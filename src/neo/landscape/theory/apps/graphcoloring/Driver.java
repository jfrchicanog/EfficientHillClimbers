package neo.landscape.theory.apps.graphcoloring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.util.Seeds;

/**
 * 
 * @author francis
 *
 */

public class Driver {

	public void runDebugExecution(String [] args) throws Exception
	{
		if (args.length < 2)
		{
			System.out.println("Arguments: <instance> <colors> [<trials>]");
			return;
		}
		
		Properties prop = new Properties();
		prop.setProperty("instance", args[0]);
		prop.setProperty("colors", args[1]);
		
		int trials = 1;
		
		WeightedGraphColoring wgc = new WeightedGraphColoring();
		wgc.setConfiguration(prop);
		
		if (args.length > 2)
		{
			trials = Integer.parseInt(args[2]);
		}
		
		System.out.println(wgc);
		
		for (int i=0; i < trials; i++)
		{
			WGCSolution sol = wgc.getRandomSolution();
			System.out.println(sol);
			double fitness= wgc.evaluate(sol);
			System.out.println("Fitness:"+fitness);
			
			File dot = new File ("sol-init.dot");
			FileOutputStream fos = new FileOutputStream(dot);
			PrintWriter pw = new PrintWriter(fos);
			pw.println(wgc.dotLanguage(sol));
			pw.close();
			
			HillClimber<WeightedGraphColoring> hc = new EfficientHillClimber();
			hc.initialize(wgc, sol);
			
			
			int l=0;
			double imp = 0.0;
			do
			{
				dot = new File ("sol-int"+l+".dot");
				fos = new FileOutputStream(dot);
				pw = new PrintWriter(fos);
				
				WGCMove m = (WGCMove)hc.getMovement();
				WGCSolution s = (WGCSolution)hc.getSolution();
				pw.println(wgc.dotLanguage(s, m));
				pw.close();
				
				imp = hc.move();
				System.out.println("Improvement:"+imp);
				l++;
				
			} while (imp < 0);
			
			dot = new File ("sol-final.dot");
			fos = new FileOutputStream(dot);
			pw = new PrintWriter(fos);
			pw.println(wgc.dotLanguage((WGCSolution)hc.getSolution()));
			pw.close();
			
		}
	}
	

	public void runHillClimbing (String [] args)
	{
		if (args.length < 4)
		{
			System.out.println("Arguments: <algorithm (naive or efficient)> <instance (file)> <colors(int)> <moves(int)> [<seed>]");
			return;
		}
		
		String algorithm = args[0];
		String instance = args[1];
		String colors = args[2];
		int moves = Integer.parseInt(args[3]);
		long seed = Seeds.getSeed();
		
		if (args.length >= 5)
		{
			seed = Long.parseLong(args[4]);
		}
		
		HillClimber<WeightedGraphColoring> hc;
		
		switch(algorithm)
		{
			case "naive":
				hc = new NaiveHillClimber();
			break;
			
			case "efficient":
				hc = new EfficientHillClimber();
			break;
			
			default:
				throw new RuntimeException ("Algorithm "+algorithm+ " is not a recognized algorithm.");
		}
		
		Properties prop = new Properties();
		prop.setProperty("instance", instance);
		prop.setProperty("colors", colors);
		
		WeightedGraphColoring wgc = new WeightedGraphColoring();
		wgc.setConfiguration(prop);
		wgc.setSeed(seed);
		
		int n_moves=0;
		long init_time = System.currentTimeMillis();
		double best_sol = Double.MAX_VALUE;
		int restarts = 0;
		
		do
		{
			hc.initialize(wgc, wgc.getRandomSolution());
			restarts++;
			double imp = 0.0;
			do
			{
				imp = hc.move();
				if (imp < 0.0)
				{
					n_moves++;
				}
			} while (n_moves < moves && imp < 0.0);
			
			double fitness = wgc.evaluate(hc.getSolution());
			if (fitness < best_sol)
			{
				best_sol = fitness;
			}
			
		} while (n_moves < moves);
		long stop_time = System.currentTimeMillis();
		
		System.out.println("Time (ms): "+(stop_time-init_time));
		System.out.println("Best solution: "+best_sol);
		System.out.println("Restarts: "+restarts);
		System.out.println("Vertices: "+wgc.getVertices());
		System.out.println("Edges: "+wgc.getEdges());
		System.out.println("Colors: "+wgc.getColors());
		System.out.println("Max degree:"+wgc.getMaxDegree());
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception 
	{
		Driver d = new Driver();
		
		//d.runDebugExecution(args);
		d.runHillClimbing(args);
		
	}

}
