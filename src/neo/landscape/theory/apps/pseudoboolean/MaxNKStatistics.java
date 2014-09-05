package neo.landscape.theory.apps.pseudoboolean;

/**
 * 
 * @author Francisco Chicano
 * @email  chicano@lcc.uma.es
 * This class should compute the probability distribution of the maximum value 
 * for an NK Landscape. However, I think the implementation is wrong.
 * It could be also the case that this computation is used only for the adjacent model.
 * Definitely, I think th distribution is built for the K=1 adjacent model.
 */

public class MaxNKStatistics {

	private int n;
	private int q;
	
	/** This is the accumulated probability distribution */
	private double [] distribution;
	
	public void setConfiguration(int n, int q)
	{
		this.n=n;
		this.q=q;
	}
	
	/**
	 * This probability distribution is valid only for K=1 adjacent NK-landscape
	 */
	public void computeDistribution()
	{
		distribution = new double [n*(q-1)+1];
		// Initialize distribution
		int i=0;
		for (; i < q; i++)
		{
			distribution[i] = (i+1.0)/q;
		}
		for (;i < distribution.length; i++)
		{
			distribution[i]=1.0;
		}
				
		// else
		
		// Repeat the process of computing sums and maxima
		for (int f=2; f <= n; f++)
		{
			for (int j=f*(q-1)-1; j>= 0; j--)
			{
				// Compute the average over the last q terms
				// This has the effect of computing the probability distirbution
				// of a sum of two random variables where one is uniformly distributed with q values
				double sum=0;
				for (int k=j; k > Math.max(-1, j-q); k--)
				{
					sum += distribution[k];
				}
				sum /= q;
				// The square has the effect of computing the probability distribution of
				// the maximum of two random variables distributed with the distribution computed
				sum *= sum;
				distribution[j]=sum;
			}
		}
		
		for (i=0; i < distribution.length; i++)
		{
			double val = distribution[i];
			distribution[i] = val*val;
		}
		
	}
	
	public double getAvg()
	{
		if (distribution==null)
		{
			computeDistribution();
		}
		
		double sum=0;
		for (int i=0; i < distribution.length; i++)
		{
			sum += distribution[i];
		}
		
		return distribution.length-sum;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2)
		{
			System.out.println("Arguments: <n> <q>");
			return;
		}
		
		MaxNKStatistics ms = new MaxNKStatistics();
		ms.setConfiguration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		System.out.println("Avg of the maximum: "+ms.getAvg());

	}

}
