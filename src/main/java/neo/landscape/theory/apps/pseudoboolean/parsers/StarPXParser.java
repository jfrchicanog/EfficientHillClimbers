package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.util.Process;

public class StarPXParser implements Process {
	
	public final class Sample implements Cloneable {
		public long time;
		public double components;
		public double logarithm;
		public double articulationPoints;
		public double recombinationTime;

		public Sample(long t) {
			time = t;
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		@Override
		public String toString() {
			return "Sample [time=" + time + ", values=" + new double[] {components, logarithm, articulationPoints, recombinationTime} + "]";
		}
	}
	
	public final class AveragedSample {
	    private long minTime;
	    private double components;
	    private double logarithm;
	    private double articulationPoints;
	    private double recombinationTime;
	    private int samples;

	    public long getMinTime() {
	        return minTime;
	    }

	    public void setMinTime(long min_time) {
	        this.minTime = min_time;
	    }

	    public int getSamples() {
	        return samples;
	    }

	    public void computeStatisticsForSelectedSamples(Sample[] past) {
	        samples = 0;
	        components=0;
	        logarithm=0;
	        articulationPoints=0;
	        recombinationTime=0;
	        for (Sample tmp : past) {
	        	if (tmp != null) {
	        		samples++;
	        		
	        		components += tmp.components;
	        		logarithm += tmp.logarithm;
	        		articulationPoints += tmp.articulationPoints;
	        		recombinationTime += tmp.recombinationTime;
	        	}
	        }
	        if (samples > 0) {
	        	components /= samples;
	        	logarithm /= samples;
	        	articulationPoints /= samples;
	        	recombinationTime /= samples;
	        }
	    }

	    public String toString() {
	        return getMinTime() + ", " + components + ", " + logarithm + ", " + articulationPoints + ", " + recombinationTime +", " + getSamples();
	    }

	}

	private List<List<Sample>> traces; 
    
    private AveragedSample averagedSample = new AveragedSample();
    

    @Override
	public String getDescription() {
		return "Statistics for PX, APX and DPX";
	}

	@Override
	public String getID() {
		return "star-px";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " <file>*";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			System.out.println(getInvocationInfo());
			return;
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
	
    
    private List<Sample> parseReaderContent(BufferedReader brd)
            throws IOException, CloneNotSupportedException {
        List<Sample> aux = new ArrayList<Sample>();

        Sample last = new Sample(0);
        
        String[] strs;
		String line;
		
		boolean somethingToPrint=false;

		while ((line = brd.readLine()) != null) {
		    if (line.isEmpty()) {
		        continue;
		    } else if (line.startsWith("* Number of components:")) {
				strs = line.split(":");
				last.components = Double.parseDouble(strs[1].trim());
				somethingToPrint=true;
			} else if (line.startsWith("* Logarithm")) {
				strs = line.split(":");
				last.logarithm = Double.parseDouble(strs[1].trim());
				somethingToPrint=true;
			} else if (line.startsWith("* Number of articulation points:")) {
				strs = line.split(":");
				last.articulationPoints = Double.parseDouble(strs[1].trim());
				somethingToPrint=true;
			} else if (line.startsWith("Recombination time:")) {
				strs = line.split(":");
				last.recombinationTime = Double.parseDouble(strs[1].trim());
				somethingToPrint=true;
			} else if (line.startsWith("Elapsed Time:")) {
				strs = line.split(":");
				last.time = Long.parseLong(strs[1].trim());
				if (somethingToPrint) {
					aux.add((Sample) last.clone());
					somethingToPrint=false;
				}
			}
		}		
		return aux;
    }

	private void printResults() {
		System.out.println("time, components, logExploredSolutions, articulationPoints, recombinationTime, samples");

		List<Sample>[] trs = traces.toArray(new List[0]);

		Sample[] past = new Sample[trs.length];
		Set<Integer> indices;
		Long lastTime = null; 
		while (!(indices = findIndicesOfNextSamplesToConsider(trs)).isEmpty()) {
			past = computeAveragedSample(trs, past, indices);
			if (lastTime == null || lastTime < averagedSample.minTime) {
				System.out.println(averagedSample);
				lastTime = averagedSample.minTime;
			}
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
