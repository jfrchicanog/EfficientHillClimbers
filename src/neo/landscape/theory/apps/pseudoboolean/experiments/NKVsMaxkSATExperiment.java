package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.Process;
import neo.landscape.theory.apps.pseudoboolean.MAXkSAT;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBall4MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.util.Seeds;

public class NKVsMaxkSATExperiment implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "nkVsmaxksat";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() +
				" [-h] [-n <vars>] [-k <k>] [-m <clauses>] [-f (nk compliant)] [-sh(uffle) clauses] [-r <r>] [-ma(xsat specific HC)] [-l <quality> <limits> ...] [-t <time(s)>] [-d <descents>] [-s <soft_restart>] [-se <seed>] [-tr(ace)] [-de(bug)] [-fl(ips) vs appearance]";
	}
	
	private void showNkVsMaxksatHelp()
	{
		System.out.println(getInvocationInfo());
	}

	@Override
	public void execute(String[] args) {
		if (args.length == 0)
		{
			showNkVsMaxksatHelp();
			return;
		}
		
		// else
		
		Map<String,List<String>> options = UtilityMethods.optionsProcessing(args);
		
		if (options.containsKey("h"))
		{
			showNkVsMaxksatHelp();
			return;
		}
		
		// else
		
		// Check mandatory elements
		// Check variables
		if (!UtilityMethods.checkOneValue(options, "n", "variables")) return;
		// else
		String n = options.get("n").get(0);

		// Check k
		if (!UtilityMethods.checkOneValue(options, "k", "k")) return;
		// else
		String k = options.get("k").get(0);

		// Check clauses
		if (!UtilityMethods.checkOneValue(options, "m", "clauses")) return;
		// else
		String m = options.get("m").get(0);

		// Check the radius
		if (!UtilityMethods.checkOneValue(options, "r", "radius")) return;
		//else
		int r = Integer.parseInt(options.get("r").get(0));
		
		
		// Check the stopping criterion
		if (! (options.containsKey("t") || options.containsKey("d")))
		{
			System.err.println("Error: stopping condition not specified");
			return;
		}
		
		long stop_descents = Long.MAX_VALUE;
		long stop_time = Long.MAX_VALUE;
		
		if (options.containsKey("t"))
		{
			if (!UtilityMethods.checkOneValue(options, "t", "stop time")) return;
			// else
			stop_time = Long.parseLong(options.get("t").get(0))*1000;
		}
		else
		{
			if (!UtilityMethods.checkOneValue(options, "d", "max descents")) return;
			// else
			stop_descents = Long.parseLong(options.get("d").get(0));
		}
		
		// Check optional elements
		
		// Check NK compliant
		boolean formula = options.containsKey("f");
		
		// Check if we should shuffle the clauses
		boolean shuffle_clauses = options.containsKey("sh");
		
		// Check quality limits
		double [] quality_limits = null;
		if (options.containsKey("l") && options.get("l").size() > 0)
		{
			quality_limits = new double [options.get("l").size()];
			int i=0; 
			for (String val: options.get("l"))
			{
				quality_limits[i++] = Double.parseDouble(val);
			}
		}
		UtilityMethods.checkQualityLimits(quality_limits);
		
		
		// Check seed
		long seed;
		if (options.containsKey("se"))
		{
			if (!UtilityMethods.checkOneValue(options, "se", "seed")) return;
			seed = Long.parseLong(options.get("se").get(0));
		}
		else
		{
			seed = Seeds.getSeed();;
		}
		
		// Check debug
		boolean debug = options.containsKey("de");
		
		// Check trace
		boolean trace = options.containsKey("tr");
		
		// Check maxsat specific
		boolean maxsat_spec = options.containsKey("ma");
		
		// Check flips vs appearance plot
		boolean flipvsapp = options.containsKey("fl");
		
		// Create the problem
		
		MAXkSAT pbf = new MAXkSAT();
		Properties prop = new Properties();
		prop.setProperty(MAXkSAT.N_STRING, n);
		prop.setProperty(MAXkSAT.K_STRING, k);
		prop.setProperty(MAXkSAT.M_STRING, m);
		prop.setProperty(MAXkSAT.RANDOM_FORMULA, formula?"yes":"no");
		prop.setProperty(MAXkSAT.SHUFFLE_CLAUSES, shuffle_clauses?"yes":"no");
		
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		// Check the soft restart
		int soft_restart=-1;
		if (options.containsKey("s"))
		{
			if (!UtilityMethods.checkOneValue(options, "s", "soft restart fraction")) return;
			double sr = Double.parseDouble(options.get("s").get(0));
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
		
		// Prepare the output
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream (new GZIPOutputStream (ba));
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
		
		// Prepare the hill climber
		
		Properties rball_prop = new Properties();
		rball_prop.setProperty(RBallEfficientHillClimber.R_STRING, String.valueOf(r));
		rball_prop.setProperty(RBallEfficientHillClimber.SEED, String.valueOf(seed));
		if (flipvsapp)
		{
			rball_prop.setProperty(RBallEfficientHillClimber.FLIP_STAT, "");
		}
		if (quality_limits != null)
		{
			String str = ""+quality_limits[0];
			for (int i=1; i < quality_limits.length; i++)
			{
				str += " "+quality_limits[i];
			}
			rball_prop.setProperty(RBallEfficientHillClimber.QUALITY_LIMITS,str);
		}
		
		RBallEfficientHillClimber rball;
		if (maxsat_spec)
		{
			rball = new RBall4MAXSAT(rball_prop);
		}
		else
		{
			rball = new RBallEfficientHillClimber(rball_prop);
		}
		
		RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf)rball.initialize(pbf);
		
		// Initialize data
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		boolean first_time=true;
		PBSolution best_solution = null;
		long best_sol_time = -1;
		long descents = 0;
		
		RBallEfficientHillClimberSnapshot rballs=null;
		while (elapsed_time-init_time < stop_time && descents < stop_descents)
		{
			if (soft_restart < 0 || first_time)
			{
				PBSolution pbs = pbf.getRandomSolution();
				rballs = rballf.initialize(pbs);
				first_time=false;
			}
			else
			{
				rballs.softRestart(soft_restart);
			}
			
			double init_quality = rballs.getSolutionQuality();
			double imp;
			long moves = 0;
			
			rballs.resetMovesPerDistance();
			
			do
			{
				imp = rballs.move();
				if (debug) rballs.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rballs.getSolutionQuality();
			
			descents++;
			elapsed_time = System.currentTimeMillis();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
				best_solution = new PBSolution (rballs.getSolution());
				best_sol_time = elapsed_time;
			}
			
			if (trace)
			{
				ps.println("Moves: "+moves);
				ps.println("Move histogram: "+Arrays.toString(rballs.getMovesPerDinstance()));
				ps.println("Improvement: "+(final_quality-init_quality));
				ps.println("Best solution quality: "+sol_q);
				ps.println("Elapsed Time: "+(elapsed_time-init_time));
			}
			
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
		ps.println("K: "+k);
		ps.println("R: " + r);
		ps.println("Seed: "+seed);
		ps.println("Stored scores:"+rballf.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		if (flipvsapp)
		{
			int [] flips = rballs.getFlipStat();
			int [][] appearsIn = pbf.getAppearsIn();
			// Show the flip vs appearance data (CSV values)
			ps.println("Flips vs appearance: CSV data below");
			ps.println("flips,appearance");
			for (int i=0; i < flips.length; i++)
			{
				ps.println(appearsIn[i].length+","+flips[i]);
			}
			ps.println("CSV End");
		}
		
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
