package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.Process;
import neo.landscape.theory.apps.pseudoboolean.util.Sample;

public class PartitionCrossoverParser implements Process {

	private List<List<Sample>> traces;

	@Override
	public String getDescription() {
		return "Analysis of partition crossover with Scores experiment";
	}

	@Override
	public String getID() {
		return "px";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: "+getID()+ " <file>*";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1)
		{
			System.out.println(getInvocationInfo());
			return;
		}

		prepareAndClearStructures();
		for (String file: args)
		{
			processFile(file);
		}
		printResults();

	}

	private void prepareAndClearStructures() {
		traces = new ArrayList<List<Sample>>();
	}

	private void processFile(String fileName)
	{
		try
		{
			processFileWithException(fileName);
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException (e);
		}
	}

	public void processFileWithException(String fileName) throws CloneNotSupportedException, IOException
	{
		File file = new File(fileName);
		List<Sample> aux = new ArrayList<Sample>();
		
		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it=false;

		String [] strs;

		GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
		BufferedReader brd = new BufferedReader(new InputStreamReader(gis));
		int n=-1;
		int k=-1;
		int q=-1;
		long seed=-1;
		boolean seedSet = false;

		String line;

		// Solution quality
		// Elapsed Time
		// * comment
		// N
		// K
		// Adjacent model?
		// Q
		// R
		// Seed
		// Generation limit
		// Generation level

		while ((line=brd.readLine())!=null)
		{

			if (line.startsWith("So"))
			{
				strs = line.split(":");
				double quality = Double.parseDouble(strs[1].trim());
				if (write_it = (quality > last.quality))
				{
					last.quality = quality;
				}
			}
			else if (line.charAt(0)=='E')
			{
				strs = line.split(":");
				long time = Long.parseLong(strs[1].trim());

				if (write_it)
				{
					last.time = time;
					aux.add((Sample)last.clone());
					write_it = false;
				}
			}
			else if (line.charAt(0)=='N')
			{
				strs = line.split(":");
				n = Integer.parseInt(strs[1].trim());
			}
			else if (line.charAt(0)=='A')
			{
				strs = line.split(":");
				if (!strs[1].trim().equals("true"))
				{
					throw new RuntimeException("I cannot compute the optimum value for instances of NK-landscapes that are not adjacent (circular)");
				}
			}
			else if (line.charAt(0)=='K')
			{
				strs = line.split(":");
				k = Integer.parseInt(strs[1].trim());
			}
			else if (line.charAt(0)=='Q')
			{
				strs = line.split(":");
				q = Integer.parseInt(strs[1].trim());
			}
			else if (line.startsWith("Se"))
			{
				strs = line.split(":");
				seed = Long.parseLong(strs[1].trim());	
				seedSet=true;
			}
		}

		brd.close();
		
		if (!seedSet)
		{
			throw new RuntimeException("Seed not set, I cannot compute the optimum");
		}
		
		if (n < 0 || k < 0 || q < 0)
		{
			int [] nkq = parseValuesFromFileName(fileName);
			n=nkq[0];
			k=nkq[1];
			q=nkq[2];
		}

		double optimumQuality = computeOptimum(n, k, q, seed);		
		for (Sample s: aux)
		{
			s.quality = (optimumQuality-s.quality)/optimumQuality;
		}
		traces.add(aux);
	}
	
	private int[] parseValuesFromFileName(String fileName) {
		String [] strings = fileName.split("-");
		int [] nkq = new int[3];
		nkq[0] = nkq[1] = nkq[2] = -1;
		for (String str: strings)
		{
			switch (str.charAt(0))
			{
			case 'n':
				nkq[0] = Integer.parseInt(str.substring(1));
				break;
			case 'k':
				nkq[1] = Integer.parseInt(str.substring(1));
				break;
			case 'q':
				nkq[2] = Integer.parseInt(str.substring(1));
				break;
			}
		}
		return nkq;
	}

	private double computeOptimum(int n, int k, int q, long seed)
	{
		// Compute the optimum, 
		ExactSolutionMethod<? super NKLandscapes> es = new NKLandscapesCircularDynProg();
		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, ""+n);
		prop.setProperty(NKLandscapes.K_STRING, ""+k);
		prop.setProperty(NKLandscapes.CIRCULAR_STRING,"yes");
		prop.setProperty(NKLandscapes.Q_STRING, ""+q);

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);

		SolutionQuality<? super NKLandscapes> sq = es.solveProblem(pbf);
		return sq.quality;
	}
	
	private void printResults()
	{
		System.out.println("time, error, samples");
		
		List<Sample> [] trs = traces.toArray(new List[0]);
		
		Sample [] past = new Sample[trs.length];
		Set<Integer> indices = new HashSet<Integer>();
		while (true)
		{
			// Find the next sample to process
			long min_time=Long.MAX_VALUE;
			indices.clear();
			for (int i=0; i < trs.length; i++)
			{
				if (trs[i].isEmpty())
				{
					continue;
				}
				
				Sample s = trs[i].get(0);
				if (s.time < min_time)
				{
					min_time = s.time;
					indices.clear();
					indices.add(i);
				}
				else if (s.time == min_time)
				{
					indices.add(i);
				}
			}
			
			if (indices.isEmpty())
			{
				break;
			}
			
			for (int ind_min: indices)
			{
				// Get the samples with the same time from the list
				// update the Sample array
				past[ind_min] = trs[ind_min].remove(0);
			}
			
			// Compute the statistics using the sample array
			double error=0;
			int samples=0;
			for (Sample tmp: past)
			{
				if (tmp != null)
				{
					samples++;
					error += tmp.quality;
				}
			}
			error /= samples;
			System.out.println(min_time+", "+error+", "+samples);
		}
		
		System.err.println("Total runs: "+trs.length);
	}

}
