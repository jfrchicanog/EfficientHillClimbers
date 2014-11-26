package neo.landscape.theory.apps.pseudoboolean.parsers;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.CompleteEnumeration;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.CompleteEnumerationBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProgBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import org.junit.Ignore;
import org.junit.Test;

public class TinosFormatTest {

    private void testWithProvidedSolverMethod(ExactSolutionMethod<? super NKLandscapes> solverMethod) {
        NKLandscapesTinosReader parserObject = new NKLandscapesTinosReader();
        InputStreamReader input = new InputStreamReader(getClass().getResourceAsStream("/NK_0.dat"));
        NKLandscapes instance = parserObject.readInstance(input);
        
        SolutionQuality<? super NKLandscapes> solutionQuality = solverMethod.solveProblem(instance);
        
        PBSolution expectedSolution = new PBSolution(instance.getN());
        expectedSolution.parse("00111010101011100010");
        
        System.out.println("Optimal value:"+solutionQuality.quality);
        assertEquals(expectedSolution, solutionQuality.solution);
        
        assertEquals("The solution quality does not correspond to the solution", solutionQuality.quality, instance.evaluate(solutionQuality.solution), 0.0001);
        assertEquals("The exptected solution does not have the computes quality", instance.evaluate(expectedSolution), solutionQuality.quality, 0.0001);
        
    }
    
    @Test
    @Ignore
    public void testCompleteEnumerationBigDecimal() {
        testWithProvidedSolverMethod(new CompleteEnumerationBigDecimal<NKLandscapes>());
    }
    
    @Test
    public void testCompleteEnumerationDouble() {
        testWithProvidedSolverMethod(new CompleteEnumeration<NKLandscapes>());
    }
    
    @Test
    public void testDyamicProgrammingBigDecimal() {
        testWithProvidedSolverMethod(new NKLandscapesCircularDynProgBigDecimal());
    }
    
    @Test
    public void testDyamicProgrammingDouble() {
        testWithProvidedSolverMethod(new NKLandscapesCircularDynProg());
    }

}
