package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class PartitionCrossoverParser implements Process {

	private List<List<Sample>> traces;
    private int n;
    private int k;
    private long seed;
    private int q;
    private boolean seedSet;
    private long problemSeed;
    private boolean problemSeedSet;
    private boolean adjacent;
    private boolean noerror;
    
    private AveragedSample averagedSample = new AveragedSample();
    

    @Override
	public String getDescription() {
		return "Analysis of partition crossover with Scores experiment";
	}

	@Override
	public String getID() {
		return "px";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " [-noerror] <file>*";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			System.out.println(getInvocationInfo());
			return;
		}
		
		noerror = args[0].equals("-noerror");
		if (noerror) {
		    args = Arrays.copyOfRange(args, 1, args.length);
		}

		prepareAndClearStructures();
		for (String file : args) {
			computeTrace(file);
		}
		printResults();

	}

	private void prepareAndClearStructures() {
		traces = new ArrayList<List<Sample>>();
	}

	private void computeTrace(String fileName) {
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
	    
		List<Sample> trace = processFile(fileName);
		computeInstanceParametersIfNecessary(fileName);
		if (adjacent && !noerror) {
		    trace=adjustTrace(trace);
		}
		
		traces.add(trace);
	}
	
	private List<Sample> processFile(String fileName) throws IOException, FileNotFoundException,
	CloneNotSupportedException {
	    File file = new File(fileName);
	    GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
	    BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

	    List<Sample> aux = parseReaderContent(brd);

	    brd.close();
	    return aux;
	}
	
	private void computeInstanceParametersIfNecessary(String fileName) {
        if (!seedSet) {
            throw new RuntimeException(
                    "Seed not set, I cannot compute the optimum");
        }

        if (n < 0 || k < 0 || q < 0) {
            int[] nkq = parseValuesFromFileName(fileName);
            n = nkq[0];
            k = nkq[1];
            q = nkq[2];
        }
    }

    private List<Sample> adjustTrace(List<Sample> aux) {
        double optimumQuality = computeOptimum(n, k, q, problemSeedSet?problemSeed:seed);
		for (Sample s : aux) {
			s.quality = (optimumQuality - s.quality) / optimumQuality;
		}
		return aux;
    }

    
    private List<Sample> parseReaderContent(BufferedReader brd)
            throws IOException, CloneNotSupportedException {
        List<Sample> aux = new ArrayList<Sample>();

        boolean write_it=false;
        Sample last = new Sample(0, -Double.MAX_VALUE);
        
        String[] strs;
        n = -1;
		k = -1;
		q = -1;
		seed = -1;
		seedSet = false;
		problemSeed = -1;
		problemSeedSet = false;

		String line;

		// Solution quality
		// Elapsed Time
		// * comment
		// N
		// K
		// Adjacent model?
		// Q
		// R
		// Seed
		// Generation limit
		// Generation level

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
					aux.add((Sample) last.clone());
					write_it = false;
				}
			} else if (line.startsWith("N:")) {
				strs = line.split(":");
				n = Integer.parseInt(strs[1].trim());
			} else if (line.startsWith("Adjacent")) {
				strs = line.split(":");
				if (strs[1].trim().equals("true")) {
					adjacent = true;
				}
			} else if (line.charAt(0) == 'K') {
				strs = line.split(":");
				k = Integer.parseInt(strs[1].trim());
			} else if (line.charAt(0) == 'Q') {
				strs = line.split(":");
				q = Integer.parseInt(strs[1].trim());
			} else if (line.startsWith("Seed")) {
				strs = line.split(":");
				seed = Long.parseLong(strs[1].trim());
				seedSet = true;
			} else if (line.startsWith("ProblemSeed")) {
                strs = line.split(":");
                problemSeed = Long.parseLong(strs[1].trim());
                problemSeedSet = true;
            }
		}
		
		return aux;
    }

	private int[] parseValuesFromFileName(String fileName) {
		String[] strings = fileName.split("-");
		int[] nkq = new int[3];
		nkq[0] = nkq[1] = nkq[2] = -1;
		for (String str : strings) {
			switch (str.charAt(0)) {
			case 'n':
				nkq[0] = Integer.parseInt(str.substring(1));
				break;
			case 'k':
				nkq[1] = Integer.parseInt(str.substring(1));
				break;
			case 'q':
				nkq[2] = Integer.parseInt(str.substring(1));
				break;
			}
		}
		return nkq;
	}

	private double computeOptimum(int n, int k, int q, long seed) {
		// Compute the optimum,
		ExactSolutionMethod<? super NKLandscapes> es = new NKLandscapesCircularDynProg();
		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, "" + n);
		prop.setProperty(NKLandscapes.K_STRING, "" + k);
		prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		prop.setProperty(NKLandscapes.Q_STRING, "" + q);

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);

		SolutionQuality<? super NKLandscapes> sq = es.solveProblem(pbf);
		return sq.quality;
	}

	private void printResults() {
		System.out.println("time, error, samples");

		List<Sample>[] trs = traces.toArray(new List[0]);

		Sample[] past = new Sample[trs.length];
		Set<Integer> indices;
		while (!(indices = findIndicesOfNextSamplesToConsider(trs)).isEmpty()) {
			past = computeAveragedSample(trs, past, indices);
			System.out.println(averagedSample);
		}

		System.err.println("Total runs: " + trs.length);
	}

    private Sample[] computeAveragedSample(List<Sample>[] trs, Sample[] past, Set<Integer> indices) {
        past = prepareNextSamples(trs, past, indices);
        averagedSample.computeStatisticsForSelectedSamples(past);
        return past;
    }

    private Sample[] prepareNextSamples(List<Sample>[] trs, Sample[] past, Set<Integer> indices) {
        for (int ind_min : indices) {
        	// Get the samples with the same time from the list
        	// update the Sample array
        	past[ind_min] = trs[ind_min].remove(0);
        }
        return past;
    }

    private Set<Integer> findIndicesOfNextSamplesToConsider(List<Sample>[] traces) {
        averagedSample.setMinTime(Long.MAX_VALUE);
        Set<Integer> indices = new HashSet<Integer>();
        for (int i = 0; i < traces.length; i++) {
        	if (traces[i].isEmpty()) {
        		continue;
        	}

        	Sample s = traces[i].get(0);
        	if (s.time < averagedSample.getMinTime()) {
        		averagedSample.setMinTime(s.time);
        		indices.clear();
        		indices.add(i);
        	} else if (s.time == averagedSample.getMinTime()) {
        		indices.add(i);
        	}
        }
        return indices;
    }

}
