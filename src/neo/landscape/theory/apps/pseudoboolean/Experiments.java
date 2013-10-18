package neo.landscape.theory.apps.pseudoboolean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.util.Seeds;

public class Experiments {
	
	public void qualityExperiments(String [] args)
	{
		if (args.length < 6)
		{
			System.out.println("Arguments: quality <n> <k> <q> <circular> <r> <time(s)> [<seed>]");
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
		
		
		KBoundedEpistasisPBF pbf = new NKLandscapes();
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
		
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		
		while (elapsed_time-init_time < time*1000)
		{
			PBSolution pbs = pbf.getRandomSolution();
			rball.initialize(pbf, pbs);
			double init_quality = rball.getSolutionQuality();
			double imp;
			long moves = 0;
			
			rball.resetMovesPerDistance();
			
			do
			{
				imp = rball.move();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
			}
			
			elapsed_time = System.currentTimeMillis();
			
			
			
			ps.println("Moves: "+moves);
			ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
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
		ps.println("Stored scores:"+rball.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		ps.close();
		
		try {
			System.out.write(ba.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void timeExperiments(String [] args)
	{
		if (args.length < 6)
		{
			System.out.println("Arguments: time <n> <k> <q> <circular> <r> <moves> [<seed>]");
			return;
		}
		
		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		int r = Integer.parseInt(args[4]);
		int moves =  Integer.parseInt(args[5]);
		long seed = 0;
		if (args.length >= 7)
		{
			seed = Long.parseLong(args[6]);
		}
		else
		{
			seed = Seeds.getSeed();
		}
		
		
		KBoundedEpistasisPBF pbf = new NKLandscapes();
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
		
		/*
		long all_sols = System.currentTimeMillis();
		for (int t=0; t < 100; t++)
		{
			rball.initialize(pbf, pbf.getRandomSolution());
		}*/
		
		rball.initialize(pbf, pbs);
		
		//long total_all_sols = System.currentTimeMillis()-all_sols;
		
		int n_int = pbf.getN();
		int bit = 0;
		long init_time = System.currentTimeMillis();
		
		for (int m=0; m < moves; m++)
		{
			rball.moveOneBit(bit++);
			if (bit == n_int)
			{
				bit = 0;
			}
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
		//System.out.println("Total solution init time: "+total_all_sols);
		//System.out.println("Total moves: "+rball.getTotalMoves());
		//System.out.println("Subfns evals in moves: "+rball.getSubfnsEvalsInMoves());
		//System.out.println("Total solution inits: "+rball.getTotalSolutionInits());
		//System.out.println("Subfns evals in sol inits: "+rball.getSubfnsEvalsInSolInits());
		System.out.println("Var appearance (max):"+max_app);
		System.out.println("Var interaction (max):"+max_interactions);

	}
	
	public void maxsatExperiments(String[] args) {
		if (args.length < 4)
		{
			System.out.println("Arguments: maxsat <instance> <r> {<quality>,<limits>,...} <time(s)> [<soft_restart> [<seed>]]");
			return;
		}
		
		String instance = args[0];
		int r = Integer.parseInt(args[1]);
		double [] quality_limits = parseQL(args[2]);
		long time =  Integer.parseInt(args[3]);
		long seed = 0;
		int soft_restart=-1;
		
		if (args.length >= 6)
		{
			seed = Long.parseLong(args[5]);
		}
		else
		{
			seed = Seeds.getSeed();
		}
		
		MAXSAT pbf = new MAXSAT();
		
		
		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream (new GZIPOutputStream (ba));
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		if (args.length>=5)
		{
			double sr = Double.parseDouble(args[4]);
			if (sr > 0)
			{
				soft_restart = (int) (pbf.getN() * sr);
				if (soft_restart <= 0)
				{
					soft_restart = -1;
				}
				else if (soft_restart > pbf.getN())
				{
					soft_restart = pbf.getN();
				}
			}
		}
		
		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, quality_limits);
		rball.setSeed(seed);
		
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		boolean first_time=true;
		PBSolution best_solution = null;
		
		
		while (elapsed_time-init_time < time*1000)
		{
			if (soft_restart < 0 || first_time)
			{
				PBSolution pbs = pbf.getRandomSolution();
				rball.initialize(pbf, pbs);
				first_time=false;
			}
			else
			{
				rball.softRestart(soft_restart);
			}
			
			double init_quality = rball.getSolutionQuality();
			double imp;
			long moves = 0;
			
			rball.resetMovesPerDistance();
			
			do
			{
				imp = rball.move();
				//rball.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
				best_solution = rball.getSolution();
			}
			
			elapsed_time = System.currentTimeMillis();
			
			ps.println("Moves: "+moves);
			ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
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
		
		ps.println("Solution: "+best_solution);
		ps.println("N: "+pbf.getN());
		ps.println("M: "+pbf.getM());
		ps.println("R: " + r);
		ps.println("Top clauses: "+pbf.getTopClauses());
		ps.println("Seed: "+seed);
		ps.println("Stored scores:"+rball.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		ps.close();
		
		try {
			System.out.write(ba.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
	
	private double[] parseQL(String string) {
		if (string.charAt(0)!= '{' || string.charAt(string.length()-1)!='}')
		{
			throw new IllegalArgumentException("I dont understand qualitylimits: "+string);
		}
		// else
		string = string.substring(1, string.length()-1);
		if (string.isEmpty())
		{
			return null;
		}
		// else
		
		String [] limits = string.split(",");
		double [] res = new double [limits.length];
		for (int i=0; i < res.length; i++)
		{
			res[i] = Double.parseDouble(limits[i]);
			if (res[i] <= 0)
			{
				throw new IllegalArgumentException ("The quality limits must be positive: "+res[i]);
			}
		}
		
		Arrays.sort(res);
		
		for (int i=1; i < res.length; i++)
		{
			if (res[i]==res[i-1])
			{
				throw new IllegalArgumentException("Repeated value for the quality limits: "+res[i]);
			}
		}
		
		
		return res;
	}

	public void minsatExperiments(String[] args) {
		if (args.length < 3)
		{
			System.out.println("Arguments: minsat <instance> <r> <time(s)> [<seed>]");
			return;
		}
		
		String instance = args[0];
		int r = Integer.parseInt(args[1]);
		long time =  Integer.parseInt(args[2]);
		long seed = 0;
		if (args.length >= 4)
		{
			seed = Long.parseLong(args[3]);
		}
		else
		{
			seed = Seeds.getSeed();
		}
		
		MAXSAT pbf = new MAXSAT();
		
		
		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		prop.setProperty(MAXSAT.MIN_STRING, "yes");
		
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
		
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		
		while (elapsed_time-init_time < time*1000)
		{
			PBSolution pbs = pbf.getRandomSolution();
			rball.initialize(pbf, pbs);
			double init_quality = rball.getSolutionQuality();
			double imp;
			long moves = 0;
			
			rball.resetMovesPerDistance();
			
			do
			{
				imp = rball.move();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
			}
			
			elapsed_time = System.currentTimeMillis();
			
			ps.println("Moves: "+moves);
			ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
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
		
		ps.println("N: "+pbf.getN());
		ps.println("M: "+pbf.getM());
		ps.println("R: " + r);
		ps.println("Top clauses: "+pbf.getTopClauses());
		ps.println("Seed: "+seed);
		ps.println("Stored scores:"+rball.getStoredScores());
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
		
		if (args.length < 1)
		{
			System.out.println("First argument: time | quality | maxsat | minsat");
			return;
		}
		
		Experiments e = new Experiments();
		switch (args[0])
		{
			case "time":
				e.timeExperiments(Arrays.copyOfRange(args, 1, args.length));
				break;
			case "quality":
				e.qualityExperiments(Arrays.copyOfRange(args, 1, args.length));
				break;
			case "maxsat":
				e.maxsatExperiments(Arrays.copyOfRange(args, 1, args.length));
				break;
			case "minsat":
				e.minsatExperiments(Arrays.copyOfRange(args, 1, args.length));
				break;	
			
		}
		
	}

	

}
