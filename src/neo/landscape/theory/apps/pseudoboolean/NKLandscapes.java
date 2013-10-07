package neo.landscape.theory.apps.pseudoboolean;

import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;

public class NKLandscapes extends KBoundedEpistasisPBF {

	public static final String N_STRING = "n";
	public static final String K_STRING = "k";
	public static final String Q_STRING = "q";
	public static final String CIRCULAR_STRING = "circular";
	
	private double [][] subfunctions;
	private int q;
	private boolean circular;
	
	@Override
	public void setConfiguration(Properties prop) {
		
		n = Integer.parseInt(prop.getProperty(N_STRING));
		k = Integer.parseInt(prop.getProperty(K_STRING))+1;
		m = n;
		
		
		int twoToK = 1 << k;
		q = twoToK;
		
		if (prop.getProperty(Q_STRING)!= null)
		{
			q = Integer.parseInt(prop.getProperty(Q_STRING));
		}
		
		circular = false;
		if (prop.getProperty(CIRCULAR_STRING)!= null)
		{
			if (prop.getProperty(CIRCULAR_STRING).equals("yes"))
			{
				circular = true;
			}
		}
		
		subfunctions = new double [m][twoToK];
		masks = new int [m][k];
		
		// Initialize masks and subfunctions
		
		if (circular)
		{
			initializeMasksCircular();
		}
		else
		{
			initializaMasksNonCircular();
		}
		
		// Initialize subfunctions
		initializeSubfunctions();
	}
	
	private void initializeSubfunctions()
	{
		int twoToK = 1 << k;
		for (int sf=0; sf < m; sf++)
		{
			for (int i=0; i < twoToK; i++)
			{
				subfunctions[sf][i] = rnd.nextInt(q);
			}
		}
	}
	
	private void initializeMasksCircular()
	{
		for (int sf=0; sf < m; sf++)
		{
			// Initialize masks
			masks[sf][0]=sf;
			for (int i=1; i < k; i++)
			{
				masks[sf][i] = (sf+i) % n;
			}
			
		}
	}
	
	private void initializaMasksNonCircular()
	{
		int [] aux = new int [n-1];
		for (int i=0; i < n-1; i++)
		{
			aux[i]=i;
		}
		
		for (int sf=0; sf < m; sf++)
		{
			// Initialize masks
			
			masks[sf][0]=sf;
			// Shuffle the aux array to get k-1 random values from the n-1 values.
			for (int i=0; i < k-1; i++)
			{
				int r = i+rnd.nextInt(n-1-i);
				int v = aux[i];
				aux[i] = aux[r];
				aux[r] = v;
			}
			
			// Copy the other variables into the mask
			for (int i=1; i < k; i++)
			{
				int v = aux[i-1];
				if (v == sf)
				{
					masks[sf][i] = n-1;
				}
				else
				{
					masks[sf][i] = v;
				}
			}
			
		}
	}
	
	public int getQ()
	{
		return q;
	}

	@Override
	public double evaluate(Solution sol) {
		
		PBSolution pbs = (PBSolution)sol;
		
		if (subfunctions == null)
		{
			throw new IllegalStateException("The NK-landscape has not been configured");
		}
		
		double res=0;

		for (int sf=0; sf < m; sf ++)
		{
			int index=0;
			for (int i=k-1; i >= 0; i--)
			{
				index = (index << 1) + pbs.getBit(masks[sf][i]);
			}
			res += subfunctions[sf][index];
		}

		return res;
	}

	@Override
	public double evaluateSubfunction(int sf, PBSolution pbs) {
		if (subfunctions == null)
		{
			throw new IllegalStateException("The NK-landscape has not been configured");
		}
		
		int index=0;
		for (int i=k-1; i >= 0; i--)
		{
			index = (index << 1) + pbs.getBit(i);
		}
		
		return subfunctions[sf][index];
	}
	
	public static void main (String [] args)
	{
		if (args.length < 2)
		{
			System.out.println("Arguments: <n> <k> [<seed>]");
			return;
		}
		
		String n = args[0];
		String k = args[1];
		long seed = 0;
		
		if (args.length > 2)
		{
			seed = Long.parseLong(args[2]);
		}
		
		NKLandscapes nkl = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		nkl.setSeed(seed);
		nkl.setConfiguration(prop);

		int [][] ai = nkl.getAppearsIn();
		
		int min=ai[0].length;
		int max=ai[0].length;
		double sum = ai[0].length;
		
		int [] histogram = new int [ai.length];
		
		histogram[ai[0].length]++;
		
		for (int i = 1; i < ai.length; i++) {
			
			histogram[ai[i].length]++;
			sum += ai[i].length;
			
			if (ai[i].length > max)
			{
				max = ai[i].length;
			}
			
			if (ai[i].length < min)
			{
				min = ai[i].length;
			}
		}
		
		System.out.println("Min:"+min);
		System.out.println("Max:"+max);
		System.out.println("Avg:"+sum/ai.length);
		
		int p;
		int q;
		
		for (p=0; p < ai.length && histogram[p]==0; p++);
		for (q=ai.length-1; q >= 0 && histogram[q] == 0 ; q--);
		
		System.out.println("Histogram ["+p+".."+q+"]:"+Arrays.toString(Arrays.copyOfRange(histogram, p, q+1)));
		
	}

	public boolean isCircular() {
		return circular;
	}
	
	public double [][] getSubFunctions()
	{
		return subfunctions;
	}

}
