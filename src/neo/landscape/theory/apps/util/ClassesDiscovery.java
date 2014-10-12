package neo.landscape.theory.apps.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import neo.landscape.theory.apps.pseudoboolean.Process;

public class ClassesDiscovery {

	/**
	 * Private helper method
	 * 
	 * @param directory
	 *            The directory to start with
	 * @param pckgname
	 *            The package name to search for. Will be needed for getting the
	 *            Class object.
	 * @param classes
	 *            if a file isn't loaded but still is in the directory
	 * @throws ClassNotFoundException
	 */
	private static void checkDirectory(File directory, String pckgname,
	        List<Class<?>> classes) throws ClassNotFoundException {
	    File tmpDirectory;

	    if (directory.exists() && directory.isDirectory()) {
	        final String[] files = directory.list();

	        for (final String file : files) {
	            if (file.endsWith(".class")) {
	                try {
	                    classes.add(Class.forName(pckgname + '.'
	                            + file.substring(0, file.length() - 6)));
	                } catch (final NoClassDefFoundError e) {
	                    // do nothing. this class hasn't been found by the
	                    // loader, and we don't care.
	                }
	            } else if ((tmpDirectory = new File(directory, file))
	                    .isDirectory()) {
	                checkDirectory(tmpDirectory, pckgname + "." + file, classes);
	            }
	        }
	    }
	}

	/**
	 * Private helper method.
	 * 
	 * @param connection
	 *            the connection to the jar
	 * @param pckgname
	 *            the package name to search for
	 * @param classes
	 *            the current ArrayList of all classes. This method will simply
	 *            add new classes.
	 * @throws ClassNotFoundException
	 *             if a file isn't loaded but still is in the jar file
	 * @throws IOException
	 *             if it can't correctly read from the jar file.
	 */
	private static void checkJarFile(JarURLConnection connection,
	        String pckgname, List<Class<?>> classes)
	        throws ClassNotFoundException, IOException {
	    final JarFile jarFile = connection.getJarFile();
	    final Enumeration<JarEntry> entries = jarFile.entries();
	    String name;

	    for (JarEntry jarEntry = null; entries.hasMoreElements()
	            && ((jarEntry = entries.nextElement()) != null);) {
	        name = jarEntry.getName();

	        if (name.contains(".class")) {
	            name = name.substring(0, name.length() - 6).replace('/', '.');

	            if (name.contains(pckgname)) {
	                classes.add(Class.forName(name));
	            }
	        }
	    }
	}
	
	private static void exploreResources(String pckgname, Enumeration<URL> resources, List<Class<?>> classes) throws ClassNotFoundException
	{
		URLConnection connection;

        for (URL url = null; resources.hasMoreElements()
                && ((url = resources.nextElement()) != null);) {
            try {
                connection = url.openConnection();
                

                if (connection instanceof JarURLConnection) {
                    checkJarFile((JarURLConnection) connection, pckgname,
                            classes);
                } else {
                    try {
                        checkDirectory(
                                new File(URLDecoder.decode(url.getPath(),
                                        "UTF-8")), pckgname, classes);
                    } catch (final UnsupportedEncodingException ex) {
                        throw new ClassNotFoundException(
                                pckgname
                                        + " does not appear to be a valid package (Unsupported encoding)",
                                ex);
                    }
                } 
                /*else
                    throw new ClassNotFoundException(pckgname + " ("
                            + url.getPath()
                            + ") does not appear to be a valid package");*/
            } catch (final IOException ioex) {
                throw new ClassNotFoundException(
                        "IOException was thrown when trying to get all resources for "
                                + pckgname, ioex);
            }
        }
    
	}

	/**
	 * Attempts to list all the classes in the specified package as determined
	 * by the context class loader
	 * 
	 * @param pckgname
	 *            the package name to search
	 * @return a list of classes that exist within that package
	 * @throws ClassNotFoundException
	 *             if something went wrong
	 */
	public static ArrayList<Class<?>> getClassesForPackage(String pckgname)
	        throws ClassNotFoundException {
	    final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

	    try {
	        final ClassLoader cld = ClassLoader.getSystemClassLoader();

	        if (cld == null)
	            throw new ClassNotFoundException("Can't get class loader.");
	        
	        
	        Enumeration<URL> resources = cld.getResources(pckgname.replace('.', '/'));
	        if (resources.hasMoreElements())
	        {
	        	exploreResources(pckgname, resources, classes);
	        }
	        
	        resources = cld.getResources(
	        		ClassesDiscovery.class.getName().replace('.', '/')+".class");
	        if (resources.hasMoreElements())
	        {
	        	exploreResources(pckgname, resources, classes);
	        }
	        
	    } catch (final NullPointerException ex) {
	        throw new ClassNotFoundException(
	                pckgname
	                        + " does not appear to be a valid package (Null pointer exception)",
	                ex);
	    } catch (final IOException ioex) {
	        throw new ClassNotFoundException(
	                "IOException was thrown when trying to get all resources for "
	                        + pckgname, ioex);
	    }
	    
	    // Added to fix a problem with class repetition (Francisco Chicano, 12/10/2014)
	    
	    Set<Class<?>> aux = new HashSet<Class<?>>();
	    aux.addAll(classes);
	    classes.clear();
	    classes.addAll(aux);

	    return classes;
	}
	
	public static <T> List<Class<? extends T>> getClassesForPackageWithSuperclass(String pack, Class<T> clazz)
	{
		List<Class<? extends T>> res = new ArrayList<Class<? extends T>>();
		try
		{
			for (Class<?> c: getClassesForPackage(pack))
			{
				if (clazz.isAssignableFrom(c))
				{
					res.add((Class<? extends T>)c);
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			// Nothing to do
		}
		
		return res;
	}
	
	public static void main (String [] args) throws Exception
	{
		for (Class<?> c: getClassesForPackageWithSuperclass("neo.landscape.theory.apps.pseudoboolean.experiments",Process.class))
		{
			System.out.println(c.getName());
		}
	}

}
