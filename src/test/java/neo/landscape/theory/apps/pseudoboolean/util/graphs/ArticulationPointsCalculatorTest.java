package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.ArticulationPointsCalculator;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.Graph;

import org.junit.Test;

public class ArticulationPointsCalculatorTest {
    
    private APCTestCase starTC = new APCTestCase() {
        {
            Integer [][] graph = {{1,2,3,4},{0},{0},{0},{0}};
            Integer [] articulationPoints = {0};
            Integer [][] biconnectedComponents = {{0,1},{0,2},{0,3},{0,4}};
            this.graph=graph;
            this.articulationPoints = articulationPoints;
            this.biconnectedComponents = biconnectedComponents;
        }
    };
    
    private APCTestCase cycleTC = new APCTestCase() {
        {
            Integer [][] graph = {{4,1},{0,2},{1,3},{2,4},{3,0}};
            Integer [] articulationPoints = {};
            Integer [][] biconnectedComponents = {{0,1,2,3,4}};
            this.graph=graph;
            this.articulationPoints = articulationPoints;
            this.biconnectedComponents = biconnectedComponents;
        }
    };
    
    private APCTestCase tarjanTC = new APCTestCase() {
        {
            Integer [][] graph = {{1,5,6},{0,2,3,4},{1,3},{1,2},{1,5},{0,4},{0,7,8},{6,8},{6,7}};
            Integer [] articulationPoints = {0,6,1};
            Integer [][] biconnectedComponents = {{0,6},{1,2,3},{6,7,8},{0,1,4,5}};
            this.graph=graph;
            this.articulationPoints = articulationPoints;
            this.biconnectedComponents = biconnectedComponents;
        }
    };
    
    private APCTestCase pointTC = new APCTestCase() {
        {
            Integer [][] graph = {{}};
            Integer [] articulationPoints = {};
            Integer [][] biconnectedComponents = {};
            this.graph=graph;
            this.articulationPoints = articulationPoints;
            this.biconnectedComponents = biconnectedComponents;
        }
    };
    
    
        
    @Test
    public void testStar() {
        testArticulationPointsCalcultorWithTestCase(starTC);
    }
    
    @Test
    public void testCycle() {
        testArticulationPointsCalcultorWithTestCase(APCTestCaseBuilder.cycleGraph(5).build());
    }
    
    @Test
    public void testTarjan() {
        testArticulationPointsCalcultorWithTestCase(tarjanTC);
    }
    
    @Test
    public void testPoint() {
        testArticulationPointsCalcultorWithTestCase(APCTestCaseBuilder.single().build());
    }
    
    @Test
    public void testLine() {
        testArticulationPointsCalcultorWithTestCase(APCTestCaseBuilder.line(4).repeat(2).build());
    }
    
    @Test
    public void testJoin() {
        testArticulationPointsCalcultorWithTestCase(APCTestCaseBuilder
                .line(4).repeat(2)
                .join(APCTestCaseBuilder.cycleGraph(5))
                .build());
    }
    
    public void testArticulationPointsCalcultorWithTestCase(APCTestCase testCase) {
        Graph graph = testCase.getGraph();
        ArticulationPointsCalculator apc = new ArticulationPointsCalculator();        
        apc.computeArticulationPoints(graph);
        
        Assert.assertEquals(testCase.getArticulationPoints(), apc.getArticulationPoints());
        Assert.assertEquals(testCase.getBiconnectedComponents(), apc.getBiconnectedComponents());
        
    }

}
