package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class MaximumCardinalitySearchTest {

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
		TriangularizationAlgorithm ta = new TriangularizationAlgorithm(new GraphV2Adaptor(graph));
		ta.maximumCardinalitySearch();
		System.out.println("Initial label: "+ta.getInitialLabel());
		System.out.println(Arrays.toString(ta.getAlpha()));
		ta.fillIn();
		ta.cliqueTree();
		System.out.println(ta.getCliqueTree());
	}

}
