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

import neo.landscape.theory.apps.pseudoboolean.IExperiment;
import neo.landscape.theory.apps.pseudoboolean.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBall4MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.util.Seeds;

public class MaxSATHpInitExperiment implements IExperiment {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "maxsatHpInit";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() +
				" [-h] [-i <instance>] [-r <r>] [-ma(xsat specific HC)] [-l <quality> <limits> ...] [-hp(hyperplane initialization)] [-d <descents>] [-se <seed>] [-de(bug)] [-fl(ips) vs appearance]";
	}
	
	private void showMaxsatHpInitHelp()
	{
		System.out.println(getInvocationInfo());
	}

	@Override
	public void execute(String[] args) {
		if (args.length == 0)
		{
			showMaxsatHpInitHelp();
			return;
		}
		
		// else
		
		Map<String,List<String>> options = UtilityMethods.optionsProcessing(args);
		
		if (options.containsKey("h"))
		{
			showMaxsatHpInitHelp();
			return;
		}
		
		// else
		
		// Check mandatory elements
		// Check instance
		if (!UtilityMethods.checkOneValue(options, "i", "instance file")) return;
		// else
		String instance = options.get("i").get(0);
		
		// Check the radius
		if (!UtilityMethods.checkOneValue(options, "r", "radius")) return;
		//else
		int r = Integer.parseInt(options.get("r").get(0));
		
		
		// Check the descents
		if (!UtilityMethods.checkOneValue(options, "d", "descents")) return;
		// else
		long stop_descents = Long.parseLong(options.get("d").get(0));
		
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
		
		// Check maxsat specific
		boolean maxsat_spec = options.containsKey("ma");
		
		// Check flips vs appearance plot
		boolean flipvsapp = options.containsKey("fl");
		
		// Check if we should use the hyperplane initialization
		boolean hpInit = options.containsKey("hp");
		
		// Create the problem
		
		MAXSAT pbf = new MAXSAT();
		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		prop.setProperty(MAXSAT.HYPERPLANE_INIT, hpInit?"yes":"no");
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
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
		long descents = 0;
		
		while (descents < stop_descents)
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
				if (debug) rball.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;
			
			double final_quality = rball.getSolutionQuality();
			
			descents++;
			elapsed_time = System.currentTimeMillis();
			

			ps.println("Initial quality: "+init_quality);
			ps.println("Moves: "+moves);
			ps.println("Move histogram: "+Arrays.toString(rball.getMovesPerDinstance()));
			ps.println("Final quality: "+final_quality);
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
