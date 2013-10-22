package neo.landscape.theory.apps.pseudoboolean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
	
	public static interface StringFilter {
		public boolean accept(String str);
	}
	
	public static final class TabularData {
		public Map<String,Sample[]> results;
		public String [] algorithms;
		
		public TabularData(Map<String,Sample[]> r, String [] a)
		{
			results = r;
			algorithms= a;
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
				if (line.charAt(0)=='B')
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
	
	private Map<String,Sample[]> analyzeData(TabularData td, StringFilter instance_filter)
	{
		Map<String,Sample[]> result = new HashMap<String,Sample[]>();
		for (int i=1; i < td.algorithms.length; i++)
		{
			List<Long> times = new ArrayList<Long>();
			String name = td.algorithms[i];
			for (Entry<String,Sample[]> e: td.results.entrySet())
			{
				if (!instance_filter.accept(e.getKey()))
				{
					continue;
				}
				// else
				Sample [] s = e.getValue();
				if (s[i] != null && s[i].quality <= s[0].quality)
				{
					if (s[i].quality < s[0].quality)
					{
						System.err.println("Better solution found for "+e.getKey());
					}
					times.add(s[i].time);
				}
			}
			
			if (times.isEmpty())
			{
				result.put(name, new Sample[0]);
				continue;
			}
			// else
			
			Collections.sort(times);
			List<Sample> points = new ArrayList<Sample>();
			
			int j=0;
			while (j < times.size())
			{
				long current = times.get(j);				
				while (j < times.size() && times.get(j) == current)
				{
					j++;
				}
				points.add(new Sample(current,j));
			}
			
			result.put(name, points.toArray(new Sample[0]));
		}
		
		return result;
	}
	
	public TabularData parseMAXSATTableResults(String prev_algs, File dir, double fraction_reached, String ... algs)
	{
		Document doc = Jsoup.parse(prev_algs);
		Elements tables = doc.select("table");
		Element table = tables.get(2);
		Elements head = table.select("thead");
		
		Map<String,Sample[]> results = new HashMap<String,Sample[]>();
		String [] algorithms;
		
		Elements headers = head.get(0).select("th");
		algorithms = new String [headers.size()-1+algs.length];
		int i;
		for (i=0; i < headers.size()-1; i++)
		{
			algorithms[i] = headers.get(i+1).text();
		}
		
		System.arraycopy(algs, 0, algorithms, i, algs.length);
		
		for (Element tr : table.select("tbody").get(0).select("tr"))
		{
			Elements tds = tr.select("td");
			String instance = tds.get(0).text();
			Sample [] samples = new Sample [tds.size()-1+algs.length];
			if (results.containsKey(instance))
			{
				System.out.println("Repeated instance!!: "+instance);
			}
			else
			{
				results.put(instance, samples);
			}
			
			for (i=1; i < tds.size(); i++)
			{
				String val = tds.get(i).text();
				int ind_o = val.indexOf("O = ");
				int ind_t = val.indexOf("T = ");
				
				String q = val.substring(ind_o+4, ind_t).trim();
				String t = val.substring(ind_t+4).trim();
				int rest = t.indexOf('(');
				if (rest > 0)
				{
					t = t.substring(0,rest).trim();
				}
				
				Sample s = null;
				if (q.charAt(0)!='N' && t.charAt(0)!='T')
				{
					s = new Sample((long)(Double.parseDouble(t)*1000),Integer.parseInt(q));
				}
				samples[i-1]=s;
			}
			
			for (int j=0; j < algs.length; j++)
			{
				File file = new File(dir,"maxsat-"+instance+"-"+algs[j]+".gz");
				samples[i-1] = analyzeFile(file, fraction_reached);
				i++;
			}
		}
		
		return new TabularData(results, algorithms);
	}

	private Sample analyzeFile(File file, double fraction_reached) {
		
		if (!file.exists())
		{
			return null;
		}

		List<List<Sample>> traces = new ArrayList<List<Sample>>();
		List<Sample> aux = new ArrayList<Sample>();
		int m= -1;
		int c = -1;
		
		// Take clauses from name of file
		String [] file_name = file.getName().split("\\.");
		if (file_name[0].startsWith("maxsat-"))
		{
			file_name[0] = file_name[0].substring(7);
		}
		
		Pattern pat = Pattern.compile("[Cc]([0-9]+)");
		Matcher mat = pat.matcher(file_name[0]);
		
		if (mat.find())
		{
			c = Integer.parseInt(mat.group(1));
		}
		
		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it=false;
		List<Double> quality_reached = new ArrayList<Double>();
		
		String [] strs;
		try
		{
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));
			
			String line;
			boolean corrected=false;

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
					if (write_it)
					{
						strs = line.split(":");
						long time = Long.parseLong(strs[1].trim());
						last.time = time;
						try{
							aux.add((Sample)last.clone());
						}
						catch (CloneNotSupportedException e)
						{
							brd.close();
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				}
				else if (line.startsWith("Q"))
				{
					if (aux.isEmpty())
					{
						strs = line.split(":");
						double quality = Double.parseDouble(strs[1].trim());
						write_it = true;
						last.quality = quality;
						corrected=true;
					}
				}
				else if (line.startsWith("T"))
				{
					if (write_it)
					{
						strs = line.split(":");
						long time = Long.parseLong(strs[1].trim());
						last.time = time;
						try{
							aux.add((Sample)last.clone());
						}
						catch (CloneNotSupportedException e)
						{
							brd.close();
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				}
				else if (line.startsWith("M:"))
				{
					strs = line.split(":");
					m = Integer.parseInt(strs[1].trim());
					if (c < 0)
					{
						c = m;
					}
					else if (!corrected && c > m)
					{
						for (Sample s: aux)
						{
							s.quality += (c-m);
						}
					}
					// End of record (one run analyzed)
					traces.add(aux);
					aux = new ArrayList<Sample>();
					quality_reached.add(last.quality+(corrected?0:(c-m)));
					
					corrected = false;
					last.quality=-Double.MAX_VALUE;
					last.time=0;
				}
			}
			
			brd.close();
			
			Collections.sort(quality_reached);
			
			double threshold = quality_reached.get((int)(quality_reached.size()*(1-fraction_reached)));
			
			double time_sum=0;
			int num_samples=0;
			
			for (List<Sample> l: traces)
			{
				for (Sample s : l)
				{
					if (s.quality >= threshold)
					{
						time_sum += s.time;
						num_samples++;
						break;
					}
				}
			}
			
			/*
			if (num_samples != traces.size())
			{
				System.err.println("Something wrong!! The number of samples does not coincide");
			}
			*/
			
			time_sum /= num_samples; // Compute the average time
			
			return new Sample((long)time_sum,c-threshold);
			
			
		}
		catch (IOException e)
		{
			throw new RuntimeException (e);
		}
	}

	public void parseMaxsatResults(File html, File dir, final String filter, double fraction_reached, String ...algs) {
		// TODO Auto-generated method stub
		try
		{
			FileInputStream fis = new FileInputStream (html);
			BufferedReader brd = new BufferedReader (new InputStreamReader(fis));
			
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line=brd.readLine())!=null)
			{
				sb.append(line+"\n");
			}
			
			brd.close();
			
			TabularData data = parseMAXSATTableResults(sb.toString(), dir, fraction_reached, algs);
			Map<String,Sample[]> plots= analyzeData(data, new StringFilter(){
				@Override
				public boolean accept(String str) {
					Pattern p = Pattern.compile(filter);
					Matcher m = p.matcher(str);
					if (m.matches())
					{
						return false;
					}
					else
					{
						return true;
					}
				}
				
			});
			
			System.out.println("algorithm, time, instances");
			for (Entry<String,Sample[]> e: plots.entrySet())
			{
				for (Sample s : e.getValue())
				{
					System.out.println(e.getKey()+", "+s.time+", "+s.quality);
				}
			}

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
			System.out.println("Arguments: [(quality|error) <file> | maxsat <html> <dir> <filter> <fraction> <als>*]");
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
			case "maxsat":
				pr.parseMaxsatResults(file, new File(args[2]), args[3], Double.parseDouble(args[4]), Arrays.copyOfRange(args, 5,args.length));
				break;
			default:
				System.out.println("Unrecognized option "+args[0]);
		}
		
	}



}
