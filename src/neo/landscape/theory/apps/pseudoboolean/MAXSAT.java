package neo.landscape.theory.apps.pseudoboolean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.Solution;

public class MAXSAT extends AdditivelyDecomposablePBF {

	public static final String N_STRING = "n";
	public static final String M_STRING = "m";
	public static final String MAX_K_STRING = "max_k"; 
	public static final String INSTANCE_STRING = "instance";
	public static final String MIN_STRING="min";
	
	private int [][] clauses;
	private int topClauses;
	private boolean min;
	
	@Override
	public void setConfiguration(Properties prop) {
		if (prop.getProperty(INSTANCE_STRING)!=null)
		{
			loadInstance(new File(prop.getProperty(INSTANCE_STRING)));
		}
		else
		{
			int n = Integer.parseInt(prop.getProperty(N_STRING));
			int m = Integer.parseInt(prop.getProperty(M_STRING));
			int max_k= 10;
			if (prop.getProperty(MAX_K_STRING) != null)
			{
				max_k = Integer.parseInt(prop.getProperty(MAX_K_STRING));
			}
			generateRandomInstance(n,m, max_k);
		}
		
		min=false;
		if (prop.getProperty(MIN_STRING)!=null)
		{
			min = prop.getProperty(MIN_STRING).equals("yes");
		}
	}

	private void generateRandomInstance(int n, int m, int max_k) {
		this.n=n;
		this.m=m;
		masks = new int [m][];
		clauses = new int [m][];
		
		// Auxiliary array to randomly select the variables in each clause
		int [] aux = new int [n];
		for (int i=0; i < n; i++)
		{
			aux[i]=i;
		}
		
		for (int c=0; c < m ; c++)
		{
			int k = rnd.nextInt(max_k)+1;
			masks[c] = new int [k];
			clauses[c] = new int [k];
			// Shuffle the aux array to get k random values from the n values.
			for (int i=0; i < k; i++)
			{
				int r = i+rnd.nextInt(n-i);
				int v = aux[i];
				aux[i] = aux[r];
				aux[r] = v;
			}
			
			for (int v=0; v < k; v++)
			{
				masks[c][v] = aux[v];
				clauses[c][v] = aux[v]+1;
				// Select a sign for the literal
				if (rnd.nextBoolean())
				{
					clauses[c][v] *= -1;
				}
			}
		}
	}

	private void loadInstance(File file) {
		// Read the DIMCAS format
		try
		{
			FileInputStream fis = new FileInputStream (file);
			BufferedReader brd = new BufferedReader(new InputStreamReader(fis));
			
			topClauses=0;
			
			String line;
			String [] parts;
			int c=0;
			int k;
			
			while ((line=brd.readLine())!=null)
			{
				line = line.trim();
				if (line.isEmpty())
				{
					continue;
				}
				// else
				switch (line.charAt(0))
				{
					case 'c': // A comment, skip it
						break;
					case 'p':  // Instance information
						parts = line.split(" +");
						n = Integer.parseInt(parts[2]);
						m = Integer.parseInt(parts[3]);
						masks = new int [m][];
						clauses = new int [m][];
						break;
					default: // A clause
						parts = line.split(" +");
						k=parts.length-1;
						
						clauses[c] = new int[k];
						masks[c] = new int [k];
						
						for (int v=0; v < k; v++)
						{
							clauses[c][v] = Integer.parseInt(parts[v]);
							masks[c][v] = Math.abs(clauses[c][v])-1;
						}
						
						if (!clauseIsTop(clauses[c]))
						{
							c++;
						}
						else
						{
							topClauses++;
						}
						break;
				}
			}
			
			// Resize the array (in case some top clauses were inserted)
			masks=Arrays.copyOf(masks, c);
			clauses = Arrays.copyOf(clauses, c);
			m=c;
			
			brd.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e); 
		}
		
	}
	
	private boolean clauseIsTop(int[] is) {
		for (int i=0; i < is.length; i++)
		{
			for (int j=i+1; j < is.length; j++)
			{
				if (is[i] == -is[j])
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public int getTopClauses()
	{
		return topClauses;
	}

	@Override
	public double evaluate(Solution sol) {
		PBSolution pbs = (PBSolution)sol;
		double res = 0;
		
		for (int c=0; c < m; c++)
		{
			for (int v: clauses[c])
			{
				int bit =pbs.getBit(Math.abs(v)-1);
				
				if (bit>0 && v > 0 || bit ==0 && v < 0)
				{
					res++;
					break;
				}
			}
		}
		
		return min?-res:res;
	}

	@Override
	public double evaluateSubfunction(int sf, PBSolution pbs) {
		int i=0; 
		for (int v: clauses[sf])
		{
			int bit =pbs.getBit(i);
			
			if (bit>0 && v > 0 || bit ==0 && v < 0)
			{
				return min?-1:1;
			}
			i++;
				
		}
		return 0;
	}
	
	public static void main (String [] args)
	{
		if (args.length < 1)
		{
			System.out.println("Arguments: dimacs <file> [<solution>] | random <n> <m> <max_k> [<seed>]");
			return;
		}
		
		Properties prop = new Properties();
		
		MAXSAT mks = new MAXSAT();
		PBSolution pbs=null;
		
		switch (args[0])
		{
			case "dimacs":
				prop.setProperty(INSTANCE_STRING, args[1]);
				mks.setConfiguration(prop);
				if (args.length > 2)
				{
					pbs = new PBSolution (mks.getN());
					pbs.parse(args[2]);
				}
				break;
			case "random":
				prop.setProperty(N_STRING, args[1]);
				prop.setProperty(M_STRING, args[2]);
				prop.setProperty(MAX_K_STRING, args[3]);
				if (args.length > 4)
				{
					long seed = Long.parseLong(args[4]);
					mks.setSeed(seed);
				}
				mks.setConfiguration(prop);
				break;
		}
		
		if (pbs==null)
		{
			pbs = mks.getRandomSolution();
		}
		
		System.out.println(pbs.toString()+" : "+mks.evaluate(pbs));
	}

	
}
