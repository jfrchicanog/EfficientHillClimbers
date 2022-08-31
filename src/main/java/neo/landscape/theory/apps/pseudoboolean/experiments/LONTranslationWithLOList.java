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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
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
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.UnionFind;

public class LONTranslationWithLOList implements Process {
	
	public static class CompressedComponent {
		private Set<PBSolution> solutionsCombined;
		private Set<PBSolution> otherSolutionsInComponent;
		private double fitness;
		private Map<PBSolution, Integer> histogram;
		public long getSize() {
			return solutionsCombined.size() + otherSolutionsInComponent.size();
		}
	}
    
    protected EmbeddedLandscape pbf;
    protected int r;
    protected long seed;

    protected long counterValue;
    private String outputFileName;
    private String loFile;
    private List<String> inputFileNames;
    private PBSolution [] solutions;
    private Map<PBSolution, Long> invertMapping;

    @Override
    public String getDescription() {
        return "This process translates the computed LONS into a smaller files with the help of a mapping function for local optima";
    }

    @Override
    public String getID() {
        return "lon-translation";
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
    
    private void lonTranslation() {
        try (OutputStream file = new FileOutputStream(outputFileName); 
             OutputStream out = new GZIPOutputStream(file);
        	 JsonGenerator generator = Json.createGenerator(out)) {
            
        	loadLocalOptima();
            translateLON(generator);
                    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
 
    
    private Long longFromSolution(PBSolution solution) {
    	return invertMapping.get(solution);
    }
    
    private void translateLON(JsonGenerator generator) {
    	generator.writeStartObject();
    	inputFileNames.stream()
    			.flatMap(this::processFile)
    			.forEach(entry->{
    				generator.write(Long.toString(entry.getKey()), 
    						jsonHistogram(entry.getValue()));
    			});
    	generator.writeEnd();
    }
    
    private Stream<Map.Entry<Long, Map<Long, Integer>>> processFile(String inputFile) {
    	
    	try {
    		InputStream is = new FileInputStream(inputFile);
			GZIPInputStream gzip = new GZIPInputStream (is);
			JsonParser parser = Json.createParser(gzip);
    		parser.next();
    		return parser.getObjectStream()
    				.onClose(()->parser.close())
    				.map(translation());
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    
    private JsonObject jsonHistogram(Map<Long, Integer> histogram) {
    	JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    	histogram.forEach((solution, frequency)->{
    		objectBuilder.add(Long.toString(solution), frequency);
    	});
    	return objectBuilder.build();
    }
    
    private JsonArray arrayOfSolutions(Collection<PBSolution> solutions) {
    	JsonArrayBuilder builder = Json.createArrayBuilder();
    	solutions.stream()
    		.forEach(e->builder.add(e.toHex()));
    	return builder.build();
    }

    private void writeTranslatedLON(JsonWriter writer, Map<Long, Map<Long, Integer>> compressedLON) {
    	JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    	compressedLON.forEach((key, value) ->{
    		objectBuilder.add(Long.toString(key), jsonHistogram(value));
    	});
    	writer.write(objectBuilder.build());
	}

    private Set<CompressedComponent> readCLON(JsonArray object) {
    	return object.stream().map(JsonObject.class::cast)
    			.map(ob->{
    				CompressedComponent component = new CompressedComponent();
    				component.fitness = ob.getJsonNumber("fitness").doubleValue();
    				component.solutionsCombined = readArrayOfSolutions(ob.getJsonArray("solutionsCombined"));
    				component.otherSolutionsInComponent = readArrayOfSolutions(ob.getJsonArray("otherSolutionsInComponent"));
    				component.histogram = readHistogram(ob.getJsonObject("histogram"));
    				return component;
    			})
    			.collect(Collectors.toSet());
    }
    

	private Map<PBSolution, Integer> readHistogram(JsonObject jsonObject) {
		return jsonObject.entrySet().stream()
			.collect(Collectors.toMap(
					entry->PBSolution.readFromHex(pbf.getN(), entry.getKey()),
					entry->((JsonNumber)entry.getValue()).intValue()));
	}

	private Set<PBSolution> readArrayOfSolutions(JsonArray jsonArray) {
		return jsonArray.stream()
			.map(JsonString.class::cast)
			.map(s->PBSolution.readFromHex(pbf.getN(), s.getString()))
			.collect(Collectors.toSet());
	}

	@Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " <output-lon-file.json.gz> <lo-list.gz> <lon-file.json.gz>+ - (nk <n> <k> <q> <circular> <r> [<seed>] | maxsat <instance> <r> [<seed>])";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        outputFileName = args[0];
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

        lonTranslation();

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
    
    private Map<Long, Integer> getHistogram(JsonObject object) {
    	return object.entrySet().stream()
    			.collect(Collectors.toMap(
    					entry->longFromSolution(solutionFromString(entry.getKey())), 
    					entry->((JsonNumber)entry.getValue()).intValue()
    			));
    }
    
    private Function<? super Entry<String, JsonValue>, ? extends Entry<Long, Map<Long, Integer>>> translation() {
		return entry->{
    		return new AbstractMap.SimpleEntry(
    				longFromSolution(solutionFromString(entry.getKey())), 
    				getHistogram((JsonObject)entry.getValue()));
    		};
	}
    
    private PBSolution solutionFromString(String val) {
    	PBSolution solution = new PBSolution(pbf.getN());
		solution.fromHex(val);
		return solution;
    }
    
    private Set<PBSolution> componentsToMerge(UnionFind<PBSolution> disjointSet, CompressedComponent component) {
    	
    	Set<PBSolution> resultSet = new HashSet<>();
    	
    	for (Set<PBSolution> set: new Set[] {component.solutionsCombined, component.otherSolutionsInComponent}) {
    		for (PBSolution solution: set) {
    			if (disjointSet.contains(solution)) {
    				resultSet.add(disjointSet.findSet(solution));
    			}
    		}
    	}
    	return resultSet;
    	
    	/*
    	return Stream.concat(component.solutionsCombined.stream(), 
    			component.otherSolutionsInComponent.stream())
    		.map(solution->disjointSet.findSetIfContained(solution))
    		.filter(optional->optional.isPresent())
    		.map(Optional::get)
    		.collect(Collectors.toSet());*/
    }
    
    private CompressedComponent combine(UnionFind<PBSolution> disjointSet, CompressedComponent component1, CompressedComponent component2) {
    	assert(component1.fitness==component2.fitness);
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
    	PBSolution representative = component.solutionsCombined.stream().findAny().get();
    	Map<PBSolution, Integer> newMap = new HashMap<>();
    	for (Entry<PBSolution, Integer> entry: component.histogram.entrySet()) {
    		if (disjoinSets.contains(entry.getKey()) && disjoinSets.sameSet(entry.getKey(), representative)) {
    			// Not included
    		} else {
    			PBSolution toInsert = entry.getKey();
    			if (disjoinSets.contains(entry.getKey())) {
    				toInsert = disjoinSets.findSet(toInsert);
    			}
    			newMap.compute(toInsert, (s, i)->((i!=null)?i:0)+entry.getValue());
    		}
    	}
    	
    	component.histogram = newMap;
    }
    
    private Map<PBSolution, CompressedComponent> combineAuxiliaryStoreWithComponent(UnionFind<PBSolution> disjointSet, Map<PBSolution, CompressedComponent> store, CompressedComponent component){
    	// Invariant: all solutions in the disjoint set should be in the compressed components in the map (but not in their histograms)
    	Set<PBSolution> merging = componentsToMerge(disjointSet, component);
    	Optional<PBSolution> oneSolution = merging.stream().findAny();
    	Optional<PBSolution> otherSolution = Stream.concat(component.solutionsCombined.stream(), component.otherSolutionsInComponent.stream())
    		.peek(solution->disjointSet.makeSetIfNotContained(solution))	
    		.reduce((sol1, sol2)->{
    			disjointSet.union(sol1, sol2);
    			return sol1;
    		});
    	
    	// oneSolution.ifPresent(sol->disjointSet.union(sol, otherSolution.get()));
    	
    	CompressedComponent combinedComponent = Stream.concat(Stream.of(component),
    			merging.stream().map(solution->store.remove(solution)))
    		.reduce((c1,c2)->combine(disjointSet, c1, c2))
    		.get();
    	
    	store.values().stream()
    		.forEach(c->adjustHistograms(disjointSet, c));
    	adjustHistograms(disjointSet, combinedComponent);
    	
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
    		.map(Map.Entry::getValue)
    		.collect(Collectors.toSet());
    }
    
    private CompressedComponent fromSolutionAndHistogram(PBSolution solution, Map<PBSolution, Integer> histogram) {
    	CompressedComponent component = new CompressedComponent();
    	component.fitness = pbf.evaluate(solution);
    	component.solutionsCombined = new HashSet<>();
    	component.solutionsCombined.add(solution);
    	component.otherSolutionsInComponent = new HashSet<>();
    	component.histogram = histogram.entrySet().stream().filter(entry->{
    		if (entry.getKey().equals(solution)) {
    			return false;
    		}
    		if (pbf.evaluate(entry.getKey()) == component.fitness) {
    			component.otherSolutionsInComponent.add(entry.getKey());
    			return false;
    		}
    		return true;
    		})
    			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    	return component;
    }
    
    private Set<CompressedComponent> compressedLONComputation (Map<PBSolution, Map<PBSolution, Integer>> lon) {
    	return lon.entrySet().stream()
    			.map(entry->Collections.singleton(fromSolutionAndHistogram(entry.getKey(), entry.getValue())))
    			.reduce(this::combineCLONs).orElse(Collections.EMPTY_SET);
    }
    
    /*
    private Map<PBSolution, Integer> adjustHistogram(UnionFind<PBSolution> unionFind, PBSolution solution, Stream<Map.Entry<PBSolution, Integer>> histogram) {
    	return histogram
    			.filter(e->!unionFind.contains(e.getKey()) || !unionFind.sameSet(solution, e.getKey()))
    			.collect(Collectors.toMap(
    					e->unionFind.findSetIfContained(e.getKey()).orElse(e.getKey()),
    					e->e.getValue(),
    					(a,b)->a+b));
    }*/
    
    private Map<PBSolution, Integer> combineHistograms(UnionFind<PBSolution> unionFind, PBSolution solution, Map<PBSolution, Integer> h1, Map<PBSolution, Integer> h2) {
    	
    	PBSolution representative = solution;
    	Map<PBSolution, Integer> newMap = new HashMap<>();
    	for (Map<PBSolution, Integer> h: new Map[] {h1, h2}) {
    		for (Entry<PBSolution, Integer> entry: h.entrySet()) {
    			if (unionFind.contains(entry.getKey()) && unionFind.sameSet(entry.getKey(), representative)) {
    				// Not included
    			} else {
    				PBSolution toInsert = entry.getKey();
    				if (unionFind.contains(entry.getKey())) {
    					toInsert = unionFind.findSet(toInsert);
    				}
    				newMap.compute(toInsert, (s, i)->((i!=null)?i:0)+entry.getValue());
    			}
    		}
    	}
    	
    	return newMap;
    	
    	// return adjustHistogram(unionFind, solution, Stream.concat(h1.entrySet().stream(),h2.entrySet().stream()));
    }

}
