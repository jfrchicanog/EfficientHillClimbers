package neo.landscape.theory.apps.pseudoboolean;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.CompleteEnumerationBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProgBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import org.junit.Test;

public class NKLandscapesCircularDynProgBigDecimalTest {

	@Test
	public void testSolveProblem() {

		long seed = 2;

		for (int N = 3; N < 12; N++)
			for (int K = 1; K <= 2; K++) {

				NKLandscapes pbf = new NKLandscapes();
				Properties prop = new Properties();
				prop.setProperty(NKLandscapes.N_STRING, String.valueOf(N));
				prop.setProperty(NKLandscapes.K_STRING, String.valueOf(K));
				prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
				prop.setProperty(NKLandscapes.Q_STRING, ".");

				pbf.setSeed(seed);
				pbf.setConfiguration(prop);

				ExactSolutionMethod<? super NKLandscapes> es;
				es = new NKLandscapesCircularDynProgBigDecimal();

				SolutionQuality<? super NKLandscapes> sq = es.solveProblem(pbf);

				assertEquals("Error in computation of solution (N=" + N
						+ ", K=" + K + ")", pbf.evaluate(sq.solution),
						sq.quality, 0.00000001);

			}
	}
	
	@Test
    public void testCompareExactSolutionMethodsForNK() {

        long seed = 2;

        for (int N = 3; N < 12; N++)
            for (int K = 1; K <= 2; K++) {

                NKLandscapes pbf = new NKLandscapes();
                Properties prop = new Properties();
                prop.setProperty(NKLandscapes.N_STRING, String.valueOf(N));
                prop.setProperty(NKLandscapes.K_STRING, String.valueOf(K));
                prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
                prop.setProperty(NKLandscapes.Q_STRING, ".");

                pbf.setSeed(seed);
                pbf.setConfiguration(prop);

                SolutionQuality<? super NKLandscapes> dynamicProgrammingSolution = 
                        solveProblemWithExactMethod(new NKLandscapesCircularDynProgBigDecimal(), pbf);

                
                SolutionQuality<? super NKLandscapes> completeEnumerationSolution = 
                        solveProblemWithExactMethod(new CompleteEnumerationBigDecimal(), pbf);

                
                assertEquals(dynamicProgrammingSolution.quality, completeEnumerationSolution.quality, 0.00001);
                assertEquals(dynamicProgrammingSolution.solution, completeEnumerationSolution.solution);

            }
    }
	
	private SolutionQuality<? super NKLandscapes> solveProblemWithExactMethod(ExactSolutionMethod<? super NKLandscapes> es, NKLandscapes pbf) {
	    return es.solveProblem(pbf);
	}

}
