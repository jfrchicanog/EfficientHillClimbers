package neo.landscape.theory.apps.util;

import java.lang.management.ManagementFactory;

public class Seeds {

	public static long getPID() {
		String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		int p = nameOfRunningVM.indexOf('@');
		String pid = nameOfRunningVM.substring(0, p);
		return Long.parseLong(pid);
	}
	
	public static long getSeed()
	{
		return System.currentTimeMillis() + getPID();
	}

}
