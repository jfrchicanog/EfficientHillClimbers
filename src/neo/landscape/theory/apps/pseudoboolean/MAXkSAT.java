package neo.landscape.theory.apps.pseudoboolean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.Solution;

public class MAXkSAT extends KBoundedEpistasisPBF {

	public static final String N_STRING = "n";
	public static final String M_STRING = "m";
	public static final String K_STRING = "k";
	public static final String INSTANCE_STRING = "instance";
	
	private int [][] clauses;
	
	@Override
	public void setConfiguration(Properties prop) {
		if (prop.getProperty(INSTANCE_STRING)!=null)
		{
			loadInstance(new File(prop.getProperty(INSTANCE_STRING)));
		}
		else
		{
			int n = Integer.parseInt(N_STRING);
			int m = Integer.parseInt(M_STRING);
			int k = Integer.parseInt(K_STRING);
			generateRandomInstance(n,m,k);
		}
	}

	private void generateRandomInstance(int n, int m, int k) {
		this.n=n;
		this.m=m;
		this.k=k;
		masks = new int [m][k];
		clauses = new int [m][k];
		
		// Auxiliary array to randomly select the variables in each clause
		int [] aux = new int [n];
		for (int i=0; i < n; i++)
		{
			aux[i]=i;
		}
		
		for (int c=0; c < m ; c++)
		{
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
			
			String line;
			String [] parts;
			k=-1;
			int c=0;
			
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
					case 'c': // A comment, skip ti
						break;
					case 'p':  // Instance information
						parts = line.split(" ");
						n = Integer.parseInt(parts[2]);
						m = Integer.parseInt(parts[3]);
						masks = new int [m][];
						clauses = new int [m][];
						break;
					default: // A clause
						parts = line.split(" ");
						if (k < 0)
						{
							k=parts.length-1;
						}
						else if (k!=parts.length-1)
						{
							throw new RuntimeException ("Ths instance is not of MAX-k-SAT. Different values for k in clause "+c);
						}
						
						clauses[c] = new int[k];
						masks[c] = new int [k];
						
						for (int v=0; v < k; v++)
						{
							clauses[c][v] = Integer.parseInt(parts[v]);
							masks[c][v] = Math.abs(clauses[c][v])-1;
						}
						c++;
						
						break;
				}
			}
			
			brd.close();
		}
		catch (IOException e)
		{
			
		}
		
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
		
		return res;
	}

	@Override
	public double evaluateSubfunction(int sf, PBSolution pbs) {
		int i=0; 
		for (int v: clauses[sf])
		{
			int bit =pbs.getBit(i);
			
			if (bit>0 && v > 0 || bit ==0 && v < 0)
			{
				return 1;
			}
			i++;
				
		}
		return 0;
	}
	
	public static void main (String [] args)
	{
		if (args.length < 1)
		{
			System.out.println("Arguments: dimacs <file> [<solution>] | random <n> <m> <k> [<seed>]");
			return;
		}
		
		Properties prop = new Properties();
		
		MAXkSAT mks = new MAXkSAT();
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
				prop.setProperty(K_STRING, args[3]);
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
