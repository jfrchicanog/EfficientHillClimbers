package neo.landscape.theory.apps.pseudoboolean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
	
	public Map<String,List<String>> optionsProcessing(String [] args)
	{
		Map<String,List<String>> res = new HashMap<String,List<String>>();
		
		String key = "";
		List<String> aux = new ArrayList<String>();
		res.put(key, aux);
		
		for (String s: args)
		{
			if (s.charAt(0)=='-')
			{
				key = s.substring(1);
				aux = res.get(key);
				if (aux==null)
				{
					aux = new ArrayList<String>();
					res.put(key, aux);
				}
			}
			else
			{
				aux.add(s);
			}
		}
		
		return res;
	}
	
	public void showMaxsatHelp()
	{
		System.out.println("Arguments: maxsat [-h] [-i <instance>] [-r <r>] [-ma(xsat specific HC)] [-l <quality> <limits> ...] [-t <time(s)>] [-d <descents>] [-s <soft_restart>] [-se <seed>] [-tr(ace)] [-de(bug)] [-fl(ips) vs appearance]");
	}
	
	public void showNkVsMaxksatHelp()
	{
		System.out.println("Arguments: nkVSmaxksat [-h] [-n <vars>] [-k <k>] [-m <clauses>] [-f (nk compliant)] [-sh(uffle) clauses] [-r <r>] [-ma(xsat specific HC)] [-l <quality> <limits> ...] [-t <time(s)>] [-d <descents>] [-s <soft_restart>] [-se <seed>] [-tr(ace)] [-de(bug)] [-fl(ips) vs appearance]");
	}
	
	public boolean checkOneValue(Map<String,List<String>> options, String key, String name)
	{
		if (!options.containsKey(key) || options.get(key).size()==0)
		{
			System.err.println("Error: "+name+" missing");
			return false;
		}
		else if (options.get(key).size() > 1)
		{
			System.err.println("Error: Multiple "+name+": "+options.get(key));
			return false;
		}
		return true;
	}
	
	public void maxsatExperiments(String[] args) {
		if (args.length == 0)
		{
			showMaxsatHelp();
			return;
		}
		
		// else
		
		Map<String,List<String>> options = optionsProcessing(args);
		
		if (options.containsKey("h"))
		{
			showMaxsatHelp();
			return;
		}
		
		// else
		
		// Check mandatory elements
		// Check instance
		if (!checkOneValue(options, "i", "instance file")) return;
		// else
		String instance = options.get("i").get(0);
		
		// Check the radius
		if (!checkOneValue(options, "r", "radius")) return;
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
			if (!checkOneValue(options, "t", "stop time")) return;
			// else
			stop_time = Long.parseLong(options.get("t").get(0))*1000;
		}
		else
		{
			if (!checkOneValue(options, "d", "max descents")) return;
			// else
			stop_descents = Long.parseLong(options.get("d").get(0));
		}
		
		// Check optional elements
		
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
		checkQualityLimits(quality_limits);
		
		
		// Check seed
		long seed;
		if (options.containsKey("se"))
		{
			if (!checkOneValue(options, "se", "seed")) return;
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
		
		MAXSAT pbf = new MAXSAT();
		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		// Check the soft restart
		int soft_restart=-1;
		if (options.containsKey("s"))
		{
			if (!checkOneValue(options, "s", "soft restart fraction")) return;
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
		
		
		// Initialize data
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		boolean first_time=true;
		PBSolution best_solution = null;
		long best_sol_time = -1;
		long descents = 0;
		
		while (elapsed_time-init_time < stop_time && descents < stop_descents)
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
				if (debug) rball.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			descents++;
			elapsed_time = System.currentTimeMillis();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
				best_solution = new PBSolution (rball.getSolution());
				best_sol_time = elapsed_time;
			}
			
			if (trace)
			{
				ps.println("Moves: "+moves);
				ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
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
		
		ps.println("Solution: "+best_solution);
		ps.println("Quality: "+(sol_q+pbf.getTopClauses()));
		ps.println("Time: "+(best_sol_time-init_time));
		ps.println("Descents: "+descents);
		ps.println("N: "+pbf.getN());
		ps.println("M: "+pbf.getM());
		ps.println("R: " + r);
		ps.println("Top clauses: "+pbf.getTopClauses());
		ps.println("Seed: "+seed);
		ps.println("Stored scores:"+rball.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		if (flipvsapp)
		{
			int [] flips = rball.getFlipStat();
			int [][] appearsIn = pbf.getAppearsIn();
			// Show the flip vs appearance data (CSV values)
			ps.println("Flips vs appearance: CSV data below");
			ps.println("flips,appearance");
			for (int i=0; i < flips.length; i++)
			{
				ps.println(flips[i]+","+appearsIn[i].length);
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
	
	
	public void nkVSmaxksatExperiments(String[] args) {
		if (args.length == 0)
		{
			showNkVsMaxksatHelp();
			return;
		}
		
		// else
		
		Map<String,List<String>> options = optionsProcessing(args);
		
		if (options.containsKey("h"))
		{
			showNkVsMaxksatHelp();
			return;
		}
		
		// else
		
		// Check mandatory elements
		// Check variables
		if (!checkOneValue(options, "n", "variables")) return;
		// else
		String n = options.get("n").get(0);

		// Check k
		if (!checkOneValue(options, "k", "k")) return;
		// else
		String k = options.get("k").get(0);

		// Check clauses
		if (!checkOneValue(options, "m", "clauses")) return;
		// else
		String m = options.get("m").get(0);

		// Check the radius
		if (!checkOneValue(options, "r", "radius")) return;
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
			if (!checkOneValue(options, "t", "stop time")) return;
			// else
			stop_time = Long.parseLong(options.get("t").get(0))*1000;
		}
		else
		{
			if (!checkOneValue(options, "d", "max descents")) return;
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
		checkQualityLimits(quality_limits);
		
		
		// Check seed
		long seed;
		if (options.containsKey("se"))
		{
			if (!checkOneValue(options, "se", "seed")) return;
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
			if (!checkOneValue(options, "s", "soft restart fraction")) return;
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
		
		
		// Initialize data
		long init_time = System.currentTimeMillis();
		long elapsed_time = init_time;
		double sol_q = -Double.MAX_VALUE;
		boolean first_time=true;
		PBSolution best_solution = null;
		long best_sol_time = -1;
		long descents = 0;
		
		while (elapsed_time-init_time < stop_time && descents < stop_descents)
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
				if (debug) rball.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			descents++;
			elapsed_time = System.currentTimeMillis();
			
			if (final_quality > sol_q)
			{
				sol_q = final_quality;
				best_solution = new PBSolution (rball.getSolution());
				best_sol_time = elapsed_time;
			}
			
			if (trace)
			{
				ps.println("Moves: "+moves);
				ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
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
		ps.println("Stored scores:"+rball.getStoredScores());
		ps.println("Var appearance (histogram):"+appearance);
		ps.println("Var interaction (histogram):"+interactions);
		
		if (flipvsapp)
		{
			int [] flips = rball.getFlipStat();
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
	
	
	private void checkQualityLimits(double [] res)
	{
		if (res==null) return;
		
		Arrays.sort(res);
		
		for (int i=1; i < res.length; i++)
		{
			if (res[i]==res[i-1])
			{
				throw new IllegalArgumentException("Repeated value for the quality limits: "+res[i]);
			}
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
		
		checkQualityLimits(res);
		
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
			System.out.println("First argument: time | quality | maxsat | minsat | nkVsmaxksat");
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
			case "nkVsmaxksat":
				e.nkVSmaxksatExperiments(Arrays.copyOfRange(args, 1, args.length));
				break;
			
		}
		
	}

	

}
