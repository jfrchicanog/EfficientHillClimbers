package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.OutputStreamWriter;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.IExperiment;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaExperiment implements IExperiment {
	
	private int localOptima;
	
	@Override
	public String getDescription() {
		return "This experiment computes the All the Local Optima of the search " +
				"space using the Efficient RBall exploration algorithm";
	}

	@Override
	public String getID() {
		return "lo";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() +
				" <n> <k> <q> <circular> <r> [<seed>]";
	}
	
	private void notifyLocalOptima(RBallEfficientHillClimber rball, NKLandscapes pbf)
	{
		double imp = rball.getMovement().getImprovement();
		if (imp <= 0.0)
		{
			
			System.out.println(localOptima+": "+rball.getSolution() + ": "+pbf.evaluate(rball.getSolution()));
			localOptima++;
		}
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 5)
		{
			System.out.println(getInvocationInfo());
			return;
		}
		
		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		int r = Integer.parseInt(args[4]);
		long seed = 0;
		if (args.length >= 6)
		{
			seed = Long.parseLong(args[5]);
		}
		else
		{
			seed = Seeds.getSeed();
		}
		
		
		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		
		if (!q.equals("-"))
		{
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}
		
		if (circular.equals("y"))
		{
			prop.setProperty(NKLandscapes.CIRCULAR_STRING,"yes");
		}
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r);
		PBSolution pbs = pbf.getRandomSolution();
		
		rball.initialize(pbf, pbs);
		
		int n_int = pbf.getN();
		
		long init_time = System.currentTimeMillis();
	
		localOptima = 0;
		notifyLocalOptima(rball,pbf);
		for (int bit: new GrayCodeBitFlipIterable(n_int))
		{
			rball.moveOneBit(bit);
			notifyLocalOptima(rball,pbf);
		}
		
		long final_time = System.currentTimeMillis();
		
		int max_app=0;
		int max_interactions=0;
		for (int i=0; i < n_int; i++)
		{
			if (pbf.getAppearsIn()[i].length > max_app)
			{
				max_app = pbf.getAppearsIn()[i].length;
			}
			
			if (pbf.getInteractions()[i].length > max_interactions)
			{
				max_interactions = pbf.getInteractions()[i].length;
			}
		}
		
		System.out.println("Problem init time: "+rball.getProblemInitTime());
		System.out.println("Solution init time: "+rball.getSolutionInitTime());
		System.out.println("Move time: "+(final_time-init_time));
		System.out.println("Stored scores:"+rball.getStoredScores());
		System.out.println("Var appearance (max):"+max_app);
		System.out.println("Var interaction (max):"+max_interactions);
		
		pbf.writeTo(new OutputStreamWriter(System.out));

	}

}
