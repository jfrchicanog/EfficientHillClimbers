package neo.landscape.theory.apps.pseudoboolean;

import java.util.Properties;

public class Driver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Something wrong with n=15, k=3 and r=4 (! not set)
		
		KBoundedEpistasisPBF pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, "100");
		prop.setProperty(NKLandscapes.K_STRING, "3");
		//prop.setProperty(NKLandscapes.Q_STRING, "100");
		pbf.setSeed(1);
		
		pbf.setConfiguration(prop);
		
		int r=4;
		
		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r);
		PBSolution pbs = pbf.getRandomSolution();
		rball.initialize(pbf, pbs);
		
		rball.checkConsistency();
		double init_fitness = pbf.evaluate(pbs);
		double imp = rball.move();
		rball.checkConsistency();
		double sum = imp;
		
		double old_fit, new_fit = init_fitness;
		int j=0; 
		while (imp > 0)
		{
			old_fit=new_fit;
			new_fit = pbf.evaluate(rball.getSolution());
			if (new_fit - old_fit != imp)
			{
				System.out.println("Something wrong (old="+old_fit+", new="+new_fit+", imp="+imp+" in "+j+")");
			}
			//System.out.println("Imp:"+imp);
			imp = rball.move();
			rball.checkConsistency();
			sum += imp;
			j++;
		}
		
		double final_fitness = pbf.evaluate(rball.getSolution());
		
		System.out.println("Init fitness:"+init_fitness);
		System.out.println("Final fitness:"+final_fitness);
		System.out.println("Improvement:"+sum);
		if (sum != final_fitness-init_fitness)
		{
			System.out.println("Something wrong");
		}
		
	}

}
