package neo.landscape.theory.apps.pseudoboolean;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neo.landscape.theory.apps.util.ClassesDiscovery;

public class ProcessManager {
	
	private Map<String, Process> processes;
	public ProcessManager(String processPackage)
	{
		processes = new HashMap<String, Process>();
		List<Class<? extends Process>> res = ClassesDiscovery.getClassesForPackageWithSuperclass(processPackage, Process.class);
		for (Class<? extends Process> c: res)
		{
			int mod = c.getModifiers();
			if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod) && Modifier.isPublic(mod))
			{
				try
				{
					Process e = c.newInstance();

					if (processes.get(e.getID())!=null)
					{
						System.out.println("Duplicate ID in package "+processPackage+": class "+ c.getName() + " will not be loaded");
					}

					processes.put(e.getID(), e);
				}
				catch (Exception e)
				{
					System.out.println("Class "+c.getName()+" cannot be loaded");
				}
			}
		}

	}
	
	private void showProcessesIDs()
	{
		String res = "First argument: ";
		for (String id: processes.keySet())
		{
			res += id+" ";
		}
		System.out.println(res);
	}
	
	public void execute(String [] args)
	{
		if (args.length < 1)
		{
			showProcessesIDs();
			return;
		}
		
		Process exp = processes.get(args[0]);
		if (exp == null)
		{
			System.out.println("Process ID not found, use one of the following IDs");
			showProcessesIDs();
		}
		else
		{
			exp.execute(Arrays.copyOfRange(args, 1, args.length));
		}

	}

}
