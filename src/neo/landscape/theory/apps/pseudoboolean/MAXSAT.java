package neo.landscape.theory.apps.pseudoboolean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Solution;

public class MAXSAT extends EmbeddedLandscape {

	public static final String N_STRING = "n";
	public static final String M_STRING = "m";
	public static final String MAX_K_STRING = "max_k"; 
	public static final String INSTANCE_STRING = "instance";
	public static final String MIN_STRING="min";
	public static final String HYPERPLANE_INIT = "hp_init";
	
	protected int [][] clauses;
	protected int topClauses;
	protected boolean min;
	protected boolean hpInit;
	protected String instance;
	
	@Override
	public void setConfiguration(Properties prop) {
		if (prop.getProperty(INSTANCE_STRING)!=null)
		{
			instance=prop.getProperty(INSTANCE_STRING);
			loadInstance(new File(instance));
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
		
		hpInit=false;
		if (prop.getProperty(HYPERPLANE_INIT)!=null)
		{
			hpInit = prop.getProperty(HYPERPLANE_INIT).equals("yes");
		}
	}

	private void generateRandomInstance(int n, int m, int max_k) {
		this.n=n;
		this.m=m;
		//masks = new int [m][];
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
			//masks[c] = new int [k];
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
				//masks[c][v] = aux[v];
				clauses[c][v] = aux[v]+1;
				// Select a sign for the literal
				if (rnd.nextBoolean())
				{
					clauses[c][v] *= -1;
				}
			}
		}
	}

	protected void loadInstance(File file) {
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
						//masks = new int [m][];
						clauses = new int [m][];
						break;
					default: // A clause
						parts = line.split(" +");
						k=parts.length-1;
						
						clauses[c] = new int[k];
						//masks[c] = new int [k];
						
						for (int v=0; v < k; v++)
						{
							clauses[c][v] = Integer.parseInt(parts[v]);
							//masks[c][v] = Math.abs(clauses[c][v])-1;
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
			//masks=Arrays.copyOf(masks, c);
			clauses = Arrays.copyOf(clauses, c);
			m=c;
			
			brd.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e); 
		}
		
	}
	
	protected boolean clauseIsTop(int[] is) {
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
	
	protected int getVar(int sf, int i)
	{
		return Math.abs(clauses[sf][i])-1;
	}
	
	@Override
	protected void prepareStructures()
	{
		List<Integer> [] aux = new List[n];
		int max_length=0;
		
		for (int sf=0; sf < m; sf++)
		{
			if (clauses[sf].length > max_length)
			{
				max_length = clauses[sf].length;
			}
			
			for (int i=0; i < clauses[sf].length; i++)
			{
				int var = getVar(sf,i);
				if (aux[var]==null)
				{
					aux[var] = new ArrayList<Integer>();
				}
				aux[var].add(sf);
			}
		}
		
		appearsIn = new int [n][];
		for (int var=0; var < n; var++)
		{
			int size = (aux[var]==null)?0:aux[var].size();
			appearsIn[var] = new int[size];
			for (int i=0; i < size; i++)
			{
				appearsIn[var][i] = aux[var].get(i);
			}
		}
		
		interactions = new int[n][];
		
		Set<Integer> aux_inter=new HashSet<Integer>();
		for (int i=0; i < n; i++)
		{
			aux_inter.clear();
			for (int sf : appearsIn[i])
			{
				for (int var: clauses[sf])
				{
					aux_inter.add(Math.abs(var)-1);
				}
			}
			aux_inter.remove(i);
			
			interactions[i] = new int [aux_inter.size()];
			int j=0;
			for (int var: aux_inter)
			{
				interactions[i][j]=var;
				j++;
			}
		}
		
		sub = new PBSolution (max_length);
	}
	
	private void computeMasks()
	{
		masks = new int [clauses.length][];
		for (int i=0; i < masks.length; i++)
		{
			masks[i] = new int [clauses[i].length];
			for (int j=0; j < masks[i].length; j++)
			{
				masks[i][j] = Math.abs(clauses[i][j])-1;
			}
		}
	}
	
	public int [][] getMasks() {
		if (masks==null)
		{
			System.err.println("int [][] getMasks has been called, please avoid this method, it is memory costly");
			computeMasks();
		}
		return masks;
	}
	
	public int getMasks(int sf, int i)
	{
		return Math.abs(clauses[sf][i])-1;
	}
	
	@Override
	public int getMaskLength(int sf)
	{
		return clauses[sf].length;
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
	
	
	/**
	 * This method potentially uses the hyperplane initialization.
	 */
	@Override
	public PBSolution getRandomSolution() {
		if (!hpInit)
		{
			return super.getRandomSolution();
		}
		// else
		return getHyperplaneInitSol(instance, rnd.nextLong());
	}
	
	private PBSolution getHyperplaneInitSol(String instance, long seed)
	{
		PBSolution sol=null;
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec("./MaxsatHP -hyperplaneinit -seed "+seed+" "+instance);
			pr.waitFor();
			BufferedReader brd = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			sol = new PBSolution (n);
			sol.parse(brd.readLine());
			try {
				brd.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.err.println("Error running MaxsatHP (using a random solution)");
			e.printStackTrace();
			sol=super.getRandomSolution();
		} catch (InterruptedException e) {
			System.err.println("Error running MaxsatHP: interrupted (using a random solution)");
			e.printStackTrace();
			sol=super.getRandomSolution();
		}
		return sol;
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
		
		double res = mks.evaluate(pbs);
		int top = mks.getTopClauses();
		System.out.println(pbs.toString()+" : "+res+"(+"+top+")="+(res+top));
	}

	
}
