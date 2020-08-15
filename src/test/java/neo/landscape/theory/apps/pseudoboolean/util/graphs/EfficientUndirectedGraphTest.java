package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import junit.framework.Assert;

public class EfficientUndirectedGraphTest {

	private static final Integer [][] GRAPH1 = new Integer[][]{{1,2},{0,2,9},{0,1,9},{4,6},{3,6,5,8},{4,6,8},{3,4,5,8,7},{6,8,9},{4,5,6,7,9},{7,8,1,2}};
	private static final Integer [][] GRAPH2 = new Integer[][]{{2,3},{2,6},{0,1,4,5},{0,4,7},{2,3,5,7},{2,4,6,8},{1,5,8},{3,4,8},{5,6,7}};
	private static final Integer [][] THREAD = new Integer[][]{{1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7},{6}};
	private static final Integer [][] STAR = new Integer[][]{{1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7,8},{6,8,9},{6,7,13},{7,10},{9,11},{10,12},{11},{8,14},{13,15},{14,16},{15}};
	private static final Integer [][] DISCONNECTED = new Integer[][]{{1,2},{0,2,9},{0,1,9},{4,6},{3,6,5,8},{4,6,8},{3,4,5,8,7},{6,8,9},{4,5,6,7,9},{7,8,1,2},{11},{10,12},{11,13},{12}};
	private static final Integer [][] CYCLE = new Integer[][]{{7,1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7},{6,0}};

	@Test
	public void test() {
		checkGraph(GRAPH1);
	}
	
	@Test
	public void test1() {
		checkGraph(GRAPH2);
	}
	
	@Test
	public void testThread() {
		checkGraph(THREAD);
	}
	
	@Test
	public void testStar() {
		checkGraph(STAR);
	}
	
	@Test
	public void testDisconnected() {
		checkGraph(DISCONNECTED);
	}
	
	@Test
	public void testCycle() {
		checkGraph(CYCLE);
	}
	
	@Test
	public void testLongerCycle() {
		checkGraph(APCTestCaseBuilder.cycleGraph(20).build().getGraph());
	}
	
	@Test
	public void testClear() {
		List<Integer [][]> graphList = Arrays.asList(GRAPH1, GRAPH2, THREAD, STAR, DISCONNECTED, CYCLE);
		
		for (int i=0; i < graphList.size(); i++) {
			for (int j=0; j < graphList.size(); j++) {
				checkClearGraph(new SampleGraph(graphList.get(i)), new SampleGraph(graphList.get(j)));
			}
		}
	}
	
	
	protected void checkGraph(Integer[][] data) {
		Graph graph = new SampleGraph(data);
		checkGraph(graph);
	}
	
	protected void checkClearGraph(Graph graph1, Graph graph2) {
		int maxNodes = Math.max(graph1.numberOfVertices(), graph2.numberOfVertices());
		UndirectedGraph mapBased = MapBasedUndirectedGraph.FACTORY.createGraph(maxNodes);
		UndirectedGraph efficient = MemoryEfficientUndirectedGraph.FACTORY.createGraph(maxNodes);
		
		compareGraphs(graph1, mapBased, efficient);
		
		efficient.clearGraph();
		mapBased.clearGraph();
		
		compareGraphs(graph2, mapBased, efficient);
	}
	
	protected void checkGraph(Graph graph) {
		UndirectedGraph mapBased = MapBasedUndirectedGraph.FACTORY.createGraph(graph.numberOfVertices());
		UndirectedGraph efficient = MemoryEfficientUndirectedGraph.FACTORY.createGraph(graph.numberOfVertices());
		
		compareGraphs(graph, mapBased, efficient);
	}

	protected void compareGraphs(Graph graph, UndirectedGraph mapBased, UndirectedGraph efficient) {
		for (int to=0; to < graph.numberOfVertices(); to++) {
			mapBased.addNodeToGraph(to);
			efficient.addNodeToGraph(to);
			for (int j=0; j < graph.numberOfAdjacentVertices(to); j++) {
				int from = graph.adjacentVertexNumber(to, j);
				if (from < to) {
					mapBased.addEdgeToGraph(from, to);
					efficient.addEdgeToGraph(from, to);
				}
			}
		}
		
		Assert.assertEquals("Node list is not identical", getListOfNodes(mapBased), getListOfNodes(efficient));
		
		Iterator<Integer> mapBasedNodeIterator = mapBased.getNodes().iterator();
		while (mapBasedNodeIterator.hasNext()) {
			int node = mapBasedNodeIterator.next();
			Assert.assertEquals("Adjacents of a node are not identical", 
					getAdjacentsList(mapBased, node), 
					getAdjacentsList(efficient, node));
		}
	}

	protected List<Integer> getAdjacentsList(UndirectedGraph mapBased, int node) {
		List<Integer> adjacentNodes = new ArrayList<>();
		mapBased.getAdjacent(node).forEach(adjacentNodes::add);
		return adjacentNodes;
	}

	protected List<Integer> getListOfNodes(UndirectedGraph graph) {
		List<Integer> nodes = new ArrayList<>();
		graph.getNodes().forEach(nodes::add);
		return nodes;
	}
}
