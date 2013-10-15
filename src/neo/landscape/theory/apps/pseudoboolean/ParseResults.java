package neo.landscape.theory.apps.pseudoboolean;

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

public class ParseResults {
	
	public static final class Sample implements Cloneable{
		public long time;
		public double quality;
		
		public Sample (long t, double q)
		{
			time = t;
			quality = q;
		}
		
		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		@Override
		public String toString() {
			return "Sample [time=" + time + ", quality=" + quality + "]";
		}

		
	}
	
	public void parseRBallQualityResults(File f)
	{
		
		List<Long> samples = new ArrayList<Long>();
		List<Double> sum_quality = new ArrayList<Double>();
		List<Long> sum_time = new ArrayList<Long>();
		long moves=0;
		double improvements=0;
		int run = 0;
		int move=0;
		String [] strs;
		
		try
		{
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(f));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

			String line;

			while ((line=brd.readLine())!=null)
			{
				if (line.startsWith("Moves:"))
				{
					strs = line.split(":");
					long m = Long.parseLong(strs[1].trim());
					moves += m;
				}
				else if (line.charAt(0)=='I')
				{
					strs = line.split(":");
					double imp = Double.parseDouble(strs[1].trim());
					improvements += imp;
				}
				else if (line.charAt(0)=='B')
				{
					strs = line.split(":");
					double best = Double.parseDouble(strs[1].trim());
					if (move < sum_quality.size())
					{
						sum_quality.set(move,sum_quality.get(move)+best);
					}
					else
					{
						sum_quality.add(move,best);
					}
					
				}
				else if (line.charAt(0)=='E')
				{
					strs = line.split(":");
					long time = Long.parseLong(strs[1].trim());
					
					if (move < sum_time.size())
					{
						sum_time.set(move,sum_time.get(move)+time);
					}
					else
					{
						sum_time.add(move,time);
					}
					
					if (move < samples.size())
					{
						samples.set(move,samples.get(move)+1);
					}
					else
					{
						samples.add(move,1L);
					}
					
					move++;
				}
				else if (line.charAt(0)=='N')
				{
					// End of record (one run analyzed)
					run++;
					move=0;
				}
			}
			
			System.out.println("time, quality, samples");
			
			long total_s =0;
			for (int i=0; i < samples.size(); i++)
			{
				double qty = sum_quality.get(i);
				double time = sum_time.get(i);
				long s = samples.get(i);
				
				System.out.println(time/s+", "+qty/s+", "+s);
				
				total_s += s;
			}
			
			System.err.println("Avg. moves per descent: "+((double)moves)/total_s);
			System.err.println("Avg. improvements per descent: "+improvements/total_s);
			System.err.println("Total runs: "+run);

			brd.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException (e);
		}
	}
	
	public void parseRBallQualityResultsKnownOptimum(File f)
	{
		List<List<Sample>> traces = new ArrayList<List<Sample>>();
		List<Sample> aux = new ArrayList<Sample>();
		ExactSolutionMethod<? super NKLandscapes> es = new NKLandscapesCircularDynProg();
		
		
		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it=false;
		
		String [] strs;
		try
		{
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(f));
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

	

	/**
	 * @param args
	 */
	public static void main(String[] args){
		
		if (args.length < 2)
		{
			System.out.println("Arguments: (quality|error) <file>");
			return;
		}
		
		ParseResults pr = new ParseResults();
		File file = new File(args[1]);
		
		
		switch (args[0])
		{
			case "quality":
				pr.parseRBallQualityResults(file);
				break;
			case "error":
				pr.parseRBallQualityResultsKnownOptimum(file);
				break;
			default:
				System.out.println("Unrecognized option "+args[0]);
		}
		
	}

}
