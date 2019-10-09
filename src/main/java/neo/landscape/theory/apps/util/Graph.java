package neo.landscape.theory.apps.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.gson.Gson;

public class Graph {
    private Map<String, Pair<Double, Long>> nodes;
    private Map<Triple<String, String, String>, Long> edges;
    private Gson gson;
    
    public Graph() {
        nodes = new HashMap<String, Pair<Double, Long>>();
        edges = new HashMap<Triple<String,String,String>,Long>();
        gson = new Gson();
    }
    
    
    public void addNode(String hash, Double value, Long timestamp) {
    	Pair<Double, Long> pair = Pair.of(value,  timestamp);
    	if (!nodes.containsKey(hash)) {
    		nodes.put(hash, pair);
    	}
    }
    
    public void addEdge(String from, String to, String kind, Long timestamp) {
    	Triple<String,String,String> triple = Triple.of(from, to, kind);
    	if (!edges.containsKey(triple)) {
    		edges.put(triple, timestamp);
    	}
    }
    
    public String printNodes() {
        return gson.toJson(nodes);
    }
    
    public String printEdges() {
        return gson.toJson(edges);
    }

}
