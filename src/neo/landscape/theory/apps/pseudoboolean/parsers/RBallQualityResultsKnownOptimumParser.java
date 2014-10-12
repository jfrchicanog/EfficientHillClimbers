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

public class RBallQualityResultsKnownOptimumParser implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "error";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: "+getID()+" <file>";
	}

	@Override
	public void execute(String[] args) {
		
		if (args.length < 1)
		{
			System.out.println(getInvocationInfo());
			return;
		}
		
		File file = new File(args[0]);
		
		List<List<Sample>> traces = new ArrayList<List<Sample>>();
		List<Sample> aux = new ArrayList<Sample>();
		ExactSolutionMethod<? super NKLandscapes> es = new NKLandscapesCircularDynProg();
		
		
		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it=false;
		
		String [] strs;
		try
		{
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));
			int n=-1;
			int k=-1;
			int q=-1;

			String line;

			while ((line=brd.readLine())!=null)
			{
				if (line.charAt(0)=='B')
				{
					strs = line.split(":");
					double best = Double.parseDouble(strs[1].trim());
					if (write_it = (best != last.quality))
					{
						last.quality = best;
					}
				}
				else if (line.charAt(0)=='E')
				{
					strs = line.split(":");
					long time = Long.parseLong(strs[1].trim());
					
					if (write_it)
					{
						last.time = time;
						try{
							aux.add((Sample)last.clone());
						}
						catch (CloneNotSupportedException e)
						{
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				}
				else if (line.charAt(0)=='N')
				{
					strs = line.split(":");
					n = Integer.parseInt(strs[1].trim());
				}
				else if (line.charAt(0)=='C')
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
					// Compute the optimum, 
					// End of record (one run analyzed)
					NKLandscapes pbf = new NKLandscapes();
					Properties prop = new Properties();
					prop.setProperty(NKLandscapes.N_STRING, ""+n);
					prop.setProperty(NKLandscapes.K_STRING, ""+k);
					prop.setProperty(NKLandscapes.CIRCULAR_STRING,"yes");
					prop.setProperty(NKLandscapes.Q_STRING, ""+q);
					
					strs = line.split(":");
					long seed = Long.parseLong(strs[1].trim());
				
					pbf.setSeed(seed);
					pbf.setConfiguration(prop);

					
					SolutionQuality<? super NKLandscapes> sq = es.solveProblem(pbf);
					
					for (Sample s: aux)
					{
						s.quality = (sq.quality-s.quality)/sq.quality;
					}
					
					traces.add(aux);
					aux = new ArrayList<Sample>();
					
					n=k=q=-1;
					last.quality=-Double.MAX_VALUE;
					last.time=0;
					
				}
			}
			
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
				/*
				// Update the Sample array if this sampel is the last in its list
				if (trs[ind_min].isEmpty())
				{
					past[ind_min] = null;
				}*/
			}
			
			System.err.println("Total runs: "+trs.length);

			brd.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException (e);
		}

	}

}
