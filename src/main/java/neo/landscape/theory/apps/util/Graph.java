package neo.landscape.theory.apps.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import com.google.gson.Gson;

public class Graph {
    private Map<String, Double> nodes;
    private Set<Triple<String, String, String>> edges;
    private Gson gson;
    
    public Graph() {
        nodes = new HashMap<String, Double>();
        edges = new HashSet<Triple<String,String,String>>();
        gson = new Gson();
    }
    
    
    public void addNode(String hash, Double value) {
        nodes.put(hash, value);
    }
    
    public void addEdge(String from, String to, String kind) {
        edges.add(Triple.of(from, to, kind));
    }
    
    public String printNodes() {
        return gson.toJson(nodes);
    }
    
    public String printEdges() {
        return gson.toJson(edges);
    }

}
