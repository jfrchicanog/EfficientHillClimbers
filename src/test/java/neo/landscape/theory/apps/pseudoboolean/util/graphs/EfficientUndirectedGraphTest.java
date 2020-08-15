package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import junit.framework.Assert;

public class EfficientUndirectedGraphTest {

	@Test
	public void test() {
		Integer [][] data = new Integer[][]{{1,2},{0,2,9},{0,1,9},{4,6},{3,6,5,8},{4,6,8},{3,4,5,8,7},{6,8,9},{4,5,6,7,9},{7,8,1,2}};
		checkGraph(data);
	}
	
	@Test
	public void test1() {
		Integer [][] data = new Integer[][]{{2,3},{2,6},{0,1,4,5},{0,4,7},{2,3,5,7},{2,4,6,8},{1,5,8},{3,4,8},{5,6,7}};
		checkGraph(data);
	}
	
	@Test
	public void testThread() {
		Integer [][] data = new Integer[][]{{1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7},{6}};
		checkGraph(data);
	}
	
	@Test
	public void testStar() {
		Integer [][] data = new Integer[][]{{1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7,8},{6,8,9},{6,7,13},{7,10},{9,11},{10,12},{11},{8,14},{13,15},{14,16},{15}};
		checkGraph(data);
	}
	
	@Test
	public void testDisconnected() {
		Integer [][] data = new Integer[][]{{1,2},{0,2,9},{0,1,9},{4,6},{3,6,5,8},{4,6,8},{3,4,5,8,7},{6,8,9},{4,5,6,7,9},{7,8,1,2},{11},{10,12},{11,13},{12}};
		checkGraph(data);
	}
	
	@Test
	public void testCycle() {
		Integer [][] data = new Integer[][]{{7,1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7},{6,0}};
		checkGraph(data);
	}
	
	@Test
	public void testLongerCycle() {
		checkGraph(APCTestCaseBuilder.cycleGraph(20).build().getGraph());
	}

	protected void checkGraph(Integer[][] data) {
		Graph graph = new SampleGraph(data);
		checkGraph(graph);
	}
	
	protected void checkGraph(Graph graph) {
		UndirectedGraph mapBased = new MapBasedUndirectedGraph();
		UndirectedGraph efficient = new MemoryEfficientUndirectedGraph(graph.numberOfVertices(), graph.numberOfVertices(), graph.numberOfVertices());
		
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
