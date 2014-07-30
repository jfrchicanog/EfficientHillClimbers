package neo.landscape.theory.apps.pseudoboolean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.experiments.LocalOptimaExperiment;
import neo.landscape.theory.apps.pseudoboolean.experiments.UtilityMethods;
import neo.landscape.theory.apps.util.ClassesDiscovery;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Seeds;

public class Experiments {
	
	private static final String EXPERIMENTS_PACKAGE = "neo.landscape.theory.apps.pseudoboolean.experiments";
	
	private Map<String, IExperiment> exps;
	public Experiments()
	{
		exps = new HashMap<>();
		List<Class<? extends IExperiment>> res = ClassesDiscovery.getClassesForPackageWithSuperclass(EXPERIMENTS_PACKAGE, IExperiment.class);
		for (Class<? extends IExperiment> c: res)
		{
			int mod = c.getModifiers();
			if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod) && Modifier.isPublic(mod))
			{
				try
				{
					IExperiment e = c.newInstance();

					if (exps.get(e.getID())!=null)
					{
						System.out.println("Duplicate ID in experiment: class "+ c.getName() + " will not be loaded");
					}

					exps.put(e.getID(), e);
				}
				catch (Exception e)
				{
					System.out.println("Class "+c.getName()+" cannot be loaded");
				}
			}
		}

	}
	
	private void showExperimentsIDs()
	{
		String res = "First argument: ";
		for (String id: exps.keySet())
		{
			res += id+" ";
		}
		System.out.println(res);
	}
	
	public void execute(String [] args)
	{
		if (args.length < 1)
		{
			showExperimentsIDs();
			return;
		}
		
		IExperiment exp = exps.get(args[0]);
		if (exp == null)
		{
			System.out.println("Experiment ID not found, use one of the following IDs");
			showExperimentsIDs();
		}
		else
		{
			exp.execute(Arrays.copyOfRange(args, 1, args.length));
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Experiments().execute(args);
	}

}
