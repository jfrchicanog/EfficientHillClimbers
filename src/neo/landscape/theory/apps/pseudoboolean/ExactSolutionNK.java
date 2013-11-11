package neo.landscape.theory.apps.pseudoboolean;

import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.util.Seeds;

public class ExactSolutionNK {

	public static void main(String[] args) {

		if (args.length < 3)
		{
			System.out.println("Arguments: <n> <k> <q> (enum|dynp) [<seed>] (all the instances are circular)");
			return;
		}

		String n = args[0];
		String k = args[1];
		String q = args[2];
		String algorithm = args[3];
		long seed = 0;
		if (args.length >= 5)
		{
			seed = Long.parseLong(args[4]);
		}
		else
		{
			seed = Seeds.getSeed();
		}

		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		prop.setProperty(NKLandscapes.CIRCULAR_STRING,"yes");
		if (!q.equals("-"))
		{
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		ExactSolutionMethod<? super NKLandscapes> es;
		
		switch (algorithm)
		{
		case "enum":
			es = new CompleteEnumeration<NKLandscapes>();
			break;
		case "dynp":
			es = new NKLandscapesCircularDynProg();
			break;
		default:
			System.out.println("Algorithm desconocido: "+algorithm);
			return;
		}
		
		SolutionQuality<? super NKLandscapes> sq = es.solveProblem(pbf);
	
		System.out.println("Optimal solution: "+sq.solution);
		System.out.println("Optimal fitness: "+sq.quality);

	}

}