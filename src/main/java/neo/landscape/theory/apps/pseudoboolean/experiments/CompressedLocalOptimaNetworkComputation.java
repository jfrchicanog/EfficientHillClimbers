package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.UnionFind;

public class CompressedLocalOptimaNetworkComputation implements Process {
	
	public static class CompressedComponent {
		private Set<PBSolution> solutionsCombined;
		private Set<PBSolution> otherSolutionsInComponent;
		private long size;
		private double fitness;
		private Map<PBSolution, Integer> histogram;
	}
    
    protected EmbeddedLandscape pbf;
    protected int r;
    protected long seed;

    protected long counterValue;
    private String outputFileName;
    private String lonFileName;

    @Override
    public String getDescription() {
        return "This experiment computes the compressed local optima network from a previous computed LON";
    }

    @Override
    public String getID() {
        return "clon-computation";
    }

    private void clonComputation() {
        try (OutputStream file = new FileOutputStream(outputFileName); 
             OutputStream out = new GZIPOutputStream(file);
        	 JsonWriter writer = Json.createWriter(out)) {
            
            computeCLON(writer);
                    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void printHistogramForSolution(PrintWriter writer, PBSolution solution, Map<PBSolution, Long> histogram) {
    	writer.println(String.format("\"%s\": {", solution.toHex()));
    	AtomicLong written = new AtomicLong(0);
        histogram.entrySet().stream().forEach(entry -> {
            writer.print(String.format("\t\"%s\": %d", entry.getKey().toHex(), entry.getValue()));
            if (written.incrementAndGet() < histogram.size()) {
                writer.print(",");
            }
            writer.println();
            
        });
        writer.print("}");
    }
    
    
    private void computeCLON(JsonWriter writer) {
    	Map<PBSolution, Map<PBSolution, Integer>> lon = readLON();
    	Set<CompressedComponent> compressedLON = compressedLONComputation(lon);
    	writeCLON(writer, compressedLON);        
    }
    
    
    private JsonObject jsonHistogram(Map<PBSolution, Integer> histogram) {
    	JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    	histogram.forEach((solution, frequency)->{
    		objectBuilder.add(solution.toHex(), frequency);
    	});
    	return objectBuilder.build();
    }
    
    private JsonArray arrayOfSolutions(Collection<PBSolution> solutions) {
    	JsonArrayBuilder builder = Json.createArrayBuilder();
    	solutions.stream()
    		.forEach(e->builder.add(e.toHex()));
    	return builder.build();
    }

    private void writeCLON(JsonWriter writer, Set<CompressedComponent> compressedLON) {
    	JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    	compressedLON.stream().map(cc->{
    		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    		objectBuilder.add("size", cc.size);
    		objectBuilder.add("fitness", cc.fitness);
    		objectBuilder.add("solutionsCombined", arrayOfSolutions(cc.solutionsCombined));
    		objectBuilder.add("otherSolutionsInComponent", arrayOfSolutions(cc.otherSolutionsInComponent));
    		objectBuilder.add("histogram", jsonHistogram(cc.histogram));
    		return objectBuilder.build();
    	})
    	.forEach(arrayBuilder::add);
    	
    	writer.write(arrayBuilder.build());
	}

	@Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " <output.json.gz> <input.json.gz> (nk <n> <k> <q> <circular> <r> [<seed>] | maxsat <instance> <r> [<seed>])";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        outputFileName = args[0];
        lonFileName = args[1];
        args= Arrays.copyOfRange(args, 2, args.length);

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

        clonComputation();

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
    
    private Map<PBSolution, Map<PBSolution, Integer>> readLON() {
    	
    	try(InputStream is = new FileInputStream(lonFileName);
    			GZIPInputStream gzip = new GZIPInputStream (is);
    			JsonReader reader = Json.createReader(gzip)) {
    		
    		JsonObject object = reader.readObject();
    		return object.entrySet().stream()
    			.collect(Collectors.toMap(
    					entry->solutionFromString(entry.getKey())
    					, entry->{
    						JsonObject value = (JsonObject)entry.getValue();
    						return value.entrySet().stream()
    							.collect(Collectors.toMap(
    									innerEntry->solutionFromString(innerEntry.getKey()), 
    									innerEntry->{
    										JsonNumber frequency = (JsonNumber) innerEntry.getValue();
    										return frequency.intValue();
    									}));
    			}));
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}

    }
    
    private PBSolution solutionFromString(String val) {
    	PBSolution solution = new PBSolution(pbf.getN());
		solution.fromHex(val);
		return solution;
    }
    
    private Set<PBSolution> componentsToMerge(UnionFind<PBSolution> disjointSet, CompressedComponent component) {
    	return Stream.concat(component.solutionsCombined.stream(), 
    			component.otherSolutionsInComponent.stream())
    		.map(solution->disjointSet.findSetIfContained(solution))
    		.filter(optional->optional.isPresent())
    		.map(Optional::get)
    		.collect(Collectors.toSet());
    }
    
    private CompressedComponent combine(UnionFind<PBSolution> disjointSet, CompressedComponent component1, CompressedComponent component2) {
    	assert(component1.fitness==component2.fitness);
    	component1.size += component2.size;
    	component1.solutionsCombined.addAll(component2.solutionsCombined);
    	component1.otherSolutionsInComponent.addAll(component2.otherSolutionsInComponent);
    	component1.otherSolutionsInComponent.removeAll(component1.solutionsCombined);
    	component1.histogram = combineHistograms(disjointSet, 
    			component1.solutionsCombined.stream().findAny().get(), 
    			component1.histogram, 
    			component2.histogram);
    	
    	return component1;
    }
    
    private void adjustHistograms(UnionFind<PBSolution> disjoinSets, CompressedComponent component) {
    	component.histogram = adjustHistogram(disjoinSets, 
    			component.solutionsCombined.stream().findAny().get(), 
    			component.histogram.entrySet().stream());
    }
    
    private Map<PBSolution, CompressedComponent> combineAuxiliaryStoreWithComponent(UnionFind<PBSolution> disjointSet, Map<PBSolution, CompressedComponent> store, CompressedComponent component){
    	// Invariant: all solutions in the disjoint set should be in the compressed components in the map (but not in their histograms)
    	Set<PBSolution> merging = componentsToMerge(disjointSet, component);
    	Optional<PBSolution> oneSolution = merging.stream().findAny();
    	Optional<PBSolution> otherSolution = Stream.concat(component.solutionsCombined.stream(), component.otherSolutionsInComponent.stream())
    		.reduce((sol1, sol2)->{
    			disjointSet.union(sol1, sol2);
    			return sol1;
    		});
    	oneSolution.ifPresent(s->disjointSet.union(s, otherSolution.get()));
    	
    	CompressedComponent combinedComponent = Stream.concat(Stream.of(component),
    			merging.stream().map(solution->store.remove(solution)))
    		.reduce((c1,c2)->combine(disjointSet, c1, c2))
    		.get();
    	
    	store.values().stream()
    		.forEach(c->adjustHistograms(disjointSet, c));
    	
    	PBSolution newKey = disjointSet.findSet(otherSolution.get());
    	store.put(newKey, combinedComponent);
    	return store;
    }
    
    private Map<PBSolution, CompressedComponent> combineAuxiliaryStores(UnionFind<PBSolution> disjointSet, Map<PBSolution, CompressedComponent> store1,  Map<PBSolution, CompressedComponent> store2){
    	for (CompressedComponent component: store2.values()) {
    		store1 = combineAuxiliaryStoreWithComponent(disjointSet, store1, component);
    	}
    	return store1;
    }
    
    private Set<CompressedComponent> combineCLONs(Set<CompressedComponent> clon1, Set<CompressedComponent> clon2) {
    	UnionFind<PBSolution> computedSolutions = UnionFind.basicImplementation();
    	return Stream.concat(clon1.stream(), clon2.stream())
    		.reduce((Map<PBSolution, CompressedComponent>)new HashMap<PBSolution, CompressedComponent>(), 
    				(storage, component)->combineAuxiliaryStoreWithComponent(computedSolutions, storage, component), 
    				(storage1, storage2)->combineAuxiliaryStores(computedSolutions, storage1, storage2))
    		.entrySet()
    		.stream()
    		.map(e->e.getValue())
    		.collect(Collectors.toSet());
    }
    
    private CompressedComponent fromSolutionAndHistogram(PBSolution solution, Map<PBSolution, Integer> histogram) {
    	CompressedComponent component = new CompressedComponent();
    	component.fitness = pbf.evaluate(solution);
    	component.solutionsCombined = Collections.singleton(solution);
    	component.otherSolutionsInComponent = new HashSet<>();
    	component.histogram = histogram.entrySet().stream().filter(entry->{
    		if (pbf.evaluate(entry.getKey()) == component.fitness) {
    			component.otherSolutionsInComponent.add(entry.getKey());
    			return false;
    		}
    		return true;
    		})
    			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    	
    	component.size = component.otherSolutionsInComponent.size() + component.solutionsCombined.size();
    	return component;
    }
    
    private Set<CompressedComponent> compressedLONComputation (Map<PBSolution, Map<PBSolution, Integer>> lon) {
    	return lon.entrySet().stream()
    			.map(entry->Collections.singleton(fromSolutionAndHistogram(entry.getKey(), entry.getValue())))
    			.reduce(this::combineCLONs).orElse(Collections.EMPTY_SET);
    }
    
    private Map<PBSolution, Integer> adjustHistogram(UnionFind<PBSolution> unionFind, PBSolution solution, Stream<Map.Entry<PBSolution, Integer>> histogram) {
    	return histogram
    			.filter(e->!unionFind.sameSet(solution, e.getKey()))
    			.collect(Collectors.toMap(
    					e->unionFind.findSetIfContained(e.getKey()).orElse(e.getKey()),
    					e->e.getValue(),
    					(a,b)->a+b));
    }
    
    private Map<PBSolution, Integer> combineHistograms(UnionFind<PBSolution> unionFind, PBSolution solution, Map<PBSolution, Integer> h1, Map<PBSolution, Integer> h2) {
    	return adjustHistogram(unionFind, solution, Stream.concat(h1.entrySet().stream(),h2.entrySet().stream()));
    }
    

	private PBSolution representative(UnionFind<PBSolution> clusters, PBSolution localOptima) {
		if (clusters.contains(localOptima)) {
			localOptima = clusters.findSet(localOptima);
		} else {
			clusters.makeSet(localOptima);
		}
		return localOptima;
	}

}
