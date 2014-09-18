package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.IExperiment;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.util.Seeds;

public class QualityExperiment implements IExperiment {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "quality";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() +
				" <n> <k> <q> <circular> <r> <time(s)> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 6)
		{
			System.out.println(getInvocationInfo());
			return;
		}
		
		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		int r = Integer.parseInt(args[4]);
		long time =  Integer.parseInt(args[5]);
		long seed = 0;
		if (args.length >= 7)
		{
			seed = Long.parseLong(args[6]);
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
		
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream (new GZIPOutputStream (ba));
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r);
		RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf)rball.initialize(pbf);
		
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		
		while (elapsed_time-init_time < time*1000)
		{
			PBSolution pbs = pbf.getRandomSolution();
			RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot)rballf.initialize(pbs);
			double init_quality = rballs.getSolutionQuality();
			double imp;
			long moves = 0;
			
			rballs.resetMovesPerDistance();
			
			do
			{
				imp = rballs.move();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rballs.getSolutionQuality();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
			}
			
			elapsed_time = System.currentTimeMillis();
			
			
			
			ps.println("Moves: "+moves);
			ps.println("Move histogram: "+Arrays.toString(rballs.getMovesPerDinstance()));
			ps.println("Improvement: "+(final_quality-init_quality));
			ps.println("Best solution quality: "+sol_q);
			ps.println("Elapsed Time: "+(elapsed_time-init_time));
			
		}
		
		Map<Integer,Integer> appearance = new HashMap<Integer,Integer>();
		Map<Integer, Integer> interactions = new HashMap<Integer,Integer>();
		
		
		for (int i=0; i < pbf.getN(); i++)
		{
			int appears = pbf.getAppearsIn()[i].length;
			if (appearance.get(appears)==null)
			{
				appearance.put(appears, 1);
			}
			else
			{
				appearance.put (appears,appearance.get(appears)+1);
			}
				
			
			int interacts = pbf.getInteractions()[i].length;
			if (interactions.get(interacts)==null)
			{
				interactions.put(interacts, 1);
			}
			else
			{
				interactions.put(interacts, interactions.get(interacts)+1);
			}
			
		}
		
		ps.println("N: "+n);
		ps.println("M: "+n);
		ps.println("K: "+k);
		if (pbf instanceof NKLandscapes)
		{
			ps.println("Q: "+((NKLandscapes)pbf).getQ());
			ps.println("Circular: "+((NKLandscapes)pbf).isCircular());
		}
		ps.println("R: " + r);
		ps.println("Seed: "+seed);
		ps.println("Stored scores:"+rballf.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		ps.close();
		
		try {
			System.out.write(ba.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
