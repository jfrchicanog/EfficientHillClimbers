package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.UnionFindLong;

public class LONClusterComputation implements Process {

    protected EmbeddedLandscape pbf;
    protected int r;
    protected long seed;

    protected long counterValue;
    private String clusterOutputFileName;
    private String loFile;
    private List<String> inputFileNames;
    private PBSolution [] solutions;
    private Map<PBSolution, Long> invertMapping;
    private UnionFindLong clusters;

    @Override
    public String getDescription() {
        return "This process computes the compressed nodes of the CLON based on the different LON files and the list of local optima";
    }

    @Override
    public String getID() {
        return "lon-clustering";
    }

    private void loadLocalOptima() {
    	try (InputStream is = new FileInputStream(loFile);
    		 GZIPInputStream gzip = new GZIPInputStream(is);
    		 Reader reader = new InputStreamReader(gzip);
    		 BufferedReader breader = new BufferedReader(reader);) {
    		List<PBSolution> solutions = breader.lines()
    			.filter(line->!line.trim().isEmpty())
    			.map(line->PBSolution.readFromHex(pbf.getN(), line))
    			.collect(Collectors.toList());
    		this.solutions = solutions.toArray(new PBSolution[0]);
    	}
    	catch (IOException e) {
			throw new RuntimeException(e);
		}
    	invertMapping = new HashMap<>();
    	for (int i=0; i < solutions.length; i++) {
    		invertMapping.put(solutions[i], (long)i);
    	}
    }
    
    private void lonClustering() {
    	loadLocalOptima();
    	prepareClusterDataStructure();
    	clusterLON();
    	writeOutput();
    }

    private void prepareClusterDataStructure() {
    	clusters = UnionFindLong.basicImplementation(solutions.length);
    	for (int i=0; i < solutions.length; i++) {
    		clusters.makeSet(i);
    	}
	}

	private Long longFromSolution(PBSolution solution) {
    	return invertMapping.get(solution);
    }
    
    private void clusterLON() {
    	inputFileNames.stream()
    			.forEach(this::processFile);
    }
    
    private void writeOutput() {
    	try (OutputStream file = new FileOutputStream(clusterOutputFileName); 
    			OutputStream out = new GZIPOutputStream(file);
    			JsonGenerator generator = Json.createGenerator(out)) {

    		generator.writeStartObject();
    		writeNumberOfClusters(generator);
    		writeMapping(generator);
    		generator.writeEnd();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }

	private void writeMapping(JsonGenerator generator) {
		generator.writeKey("mapping");
		generator.writeStartArray();
		for (long lo = 0; lo < solutions.length; ++lo) {
			generator.writeStartArray();
			generator.write(lo);
			generator.write(clusters.findSet(lo));
			generator.writeEnd();
		}
		generator.writeEnd();
	}

	private void writeNumberOfClusters(JsonGenerator generator) {
		generator.writeKey("clusters");
		generator.write(clusters.getNumberOfSets());
	}
    
    private void processFile(String inputFile) {
    	
    	try {
    		InputStream is = new FileInputStream(inputFile);
			GZIPInputStream gzip = new GZIPInputStream (is);
			JsonParser parser = Json.createParser(gzip);
    		parser.next();
    		parser.getObjectStream()
    				.onClose(()->parser.close())
    				.forEach(e->processLocalOptima(e.getKey(), e.getValue()));
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    private void processLocalOptima(String lo, JsonValue histogramJson) {
    	int solutionIndex = Integer.parseInt(lo);
    	PBSolution solution = solutions[solutionIndex];
    	double fitness = pbf.evaluate(solution);
    	getHistogramStream(histogramJson.asJsonObject())
    		.forEach(entry->{
    			long neighboringSolutionIndex = entry.getKey();
    			PBSolution neighbor = solutions[(int)neighboringSolutionIndex];
    			if (fitness == pbf.evaluate(neighbor)) {
    				clusters.union(neighboringSolutionIndex, solutionIndex);
    			}
    		});
    }
    
    private Function<Entry<String, JsonValue>, Entry<Long, Map<Long, Integer>>> translation() {
		return entry->{
    		return new AbstractMap.SimpleEntry<Long,Map<Long,Integer>>(
    				longFromSolution(solutionFromString(entry.getKey())), 
    				getHistogram((JsonObject)entry.getValue()));
    		};
	}

    
    private JsonObject jsonHistogram(Map<Long, Integer> histogram) {
    	JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    	histogram.forEach((solution, frequency)->{
    		objectBuilder.add(Long.toString(solution), frequency);
    	});
    	return objectBuilder.build();
    }
    
    private Map<Long, Integer> getHistogram(JsonObject object) {
    	return object.entrySet().stream()
    			.collect(Collectors.toMap(
    					entry->longFromSolution(solutionFromString(entry.getKey())), 
    					entry->((JsonNumber)entry.getValue()).intValue()
    			));
    }
    
    private Stream<Map.Entry<Long, Integer>> getHistogramStream(JsonObject object) {
    	return object.entrySet().stream()
    			.map(entry->new AbstractMap.SimpleEntry<Long,Integer>(
    					Long.parseLong(entry.getKey()),
    					((JsonNumber)entry.getValue()).intValue())
    			);
    }
    
    private PBSolution solutionFromString(String val) {
    	PBSolution solution = new PBSolution(pbf.getN());
		solution.fromHex(val);
		return solution;
    }

	@Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " <output-cluster-file.json.gz> <lo-list.gz> <lon-file.json.gz>+ - (nk <n> <k> <q> <circular> <r> [<seed>] | maxsat <instance> <r> [<seed>])";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        clusterOutputFileName = args[0];
        loFile = args[1];
        
        int i=2;
        inputFileNames = new ArrayList<>();
        while (i < args.length && !args[i].equals("-")) {
        	inputFileNames.add(args[i++]);
        }
        if (!args[i].equals("-")) {
        	System.err.println("I did not find the the end of the input file list: aborting");
        	return;
        }
        
        args= Arrays.copyOfRange(args, i+1, args.length);

        if ("nk".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureNKInstance(args);
        } else if ("maxsat".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureMaxsatInstance(args);
        }
        
        if (pbf == null) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        System.out.println("Seed: "+seed);

        lonClustering();

        if (pbf instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }
    }

	private EmbeddedLandscape configureNKInstance(String[] args) {
        String n = args[0];
        String k = args[1];
        String q = args[2];
        String circular = args[3];
        r = Integer.parseInt(args[4]);
        seed = 0;
        if (args.length >= 6) {
            seed = Long.parseLong(args[5]);
        } else {
            seed = Seeds.getSeed();
        }
        
        return createNKInstance(n, k, q, circular);
    }
    
    private EmbeddedLandscape configureMaxsatInstance(String [] args) {
        String instance = args[0];
        r = Integer.parseInt(args[1]);
        seed = 0;
        if (args.length >= 3) {
            seed = Long.parseLong(args[2]);
        } else {
            seed = Seeds.getSeed();
        }
        
        Properties prop = new Properties();
        prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
        MAXSAT maxsat = new MAXSAT();
        maxsat.setConfiguration(prop);
        return maxsat;
    }

    private void reportNKInstanceToStandardOutput() {
        ((NKLandscapes)pbf).writeTo(new OutputStreamWriter(System.out));
    }
    
    private EmbeddedLandscape createNKInstance(String n, String k, String q, String circular) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);

        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
        }

        if (circular.equals("y")) {
            prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        }

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        
        return pbf;
    }
}
