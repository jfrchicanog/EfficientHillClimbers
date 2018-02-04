package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.util.AveragedSample;
import neo.landscape.theory.apps.pseudoboolean.util.Sample;
import neo.landscape.theory.apps.util.Process;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BestSolutionAtTimeDRILS implements Process {
    
    public static final String TIME_ARGUMENT = "time";

	private List<Double> traces;
    
    private CommandLine commandLine;
    private Options options;
    private Long timeInMillis;
    
    private AveragedSample averagedSample = new AveragedSample();
    
    @Override
    public String getInvocationInfo() {
        HelpFormatter helpFormatter = new HelpFormatter();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
        return stringWriter.toString();
    }

    private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }
    
    private Options prepareOptions() {
        Options options = new Options();
        options.addOption(TIME_ARGUMENT, true, "time at which the best solution whould be reported (optional)");
        return options;
    }
    
    @Override
	public String getDescription() {
		return "Analysis of DRILS traces to get the best so far solution reported at a time";
	}

	@Override
	public String getID() {
		return "px-best";
	}
	
	private CommandLine parseCommandLine(String[] args) {
	    try {
	        CommandLineParser parser = new DefaultParser();
	        return parser.parse(getOptions(), args);
	    } catch (ParseException e) {
	        throw new RuntimeException (e);
	    }
	}

	@Override
	public void execute(String[] args) {
	    if (args.length == 0) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp(getID(), getOptions());
            return;
        }
        
        commandLine = parseCommandLine(args);
        timeInMillis=null;
        if (commandLine.hasOption(TIME_ARGUMENT)) {
            timeInMillis = Long.parseLong(commandLine.getOptionValue(TIME_ARGUMENT));
        }


		prepareAndClearStructures();
		for (String file : commandLine.getArgs()) {
			computeBestSoFarSolution(file);
		}
		printResults();
	}

	private void prepareAndClearStructures() {
		traces = new ArrayList<>();
	}

	private void computeBestSoFarSolution(String fileName) {
		try {
			computeTraceWithException(fileName);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void computeTraceWithException(String fileName)
			throws CloneNotSupportedException, IOException {
	    
		Double valor = processFile(fileName);
		if (valor ==null) {
		    valor = Double.NaN;
		}
		traces.add(valor);
	}
	
	private Double processFile(String fileName) throws IOException, FileNotFoundException,
	CloneNotSupportedException {
	    File file = new File(fileName);
	    GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
	    BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

	    Double aux = parseReaderContent(brd);

	    brd.close();
	    return aux;
	}
	
    private Double parseReaderContent(BufferedReader brd)
            throws IOException, CloneNotSupportedException {
        boolean write_it=false;
        Sample last = new Sample(0, -Double.MAX_VALUE);
        Sample lastOnTime=null;
        
        if (timeInMillis == null) {
            lastOnTime = last;
        }
        
        String[] strs;        
		String line;


		while ((line = brd.readLine()) != null) {
		    if (line.isEmpty()) {
		        continue;
		    }  else if (line.startsWith("Solution")) {
				strs = line.split(":");
				double quality = Double.parseDouble(strs[1].trim());
				if (write_it = (quality > last.quality)) {
					last.quality = quality;
				}
			} else if (line.startsWith("Elapsed")) {
				strs = line.split(":");
				long time = Long.parseLong(strs[1].trim());

				if (write_it) {
					last.time = time;
					write_it = false;
					if (timeInMillis != null && last.time <= timeInMillis) {
					    lastOnTime = (Sample) last.clone();
					}
				}
			}
		}
		
		if (lastOnTime != null) {
		    return lastOnTime.quality;
		} else {
		    return null;
		}
    }

	private void printResults() {
		System.out.println("quality");

		for (double val: traces) {
		    System.out.println(val);
		}
		
		System.err.println("Total samples: " + traces.size());
	}
}
