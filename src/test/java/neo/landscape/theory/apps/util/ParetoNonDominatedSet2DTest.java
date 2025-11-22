package neo.landscape.theory.apps.util;

import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2D;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class ParetoNonDominatedSet2DTest {

    @Test
    public void testCombineEqualsAddTwoSmallSets() {
        double[][] aPoints = {
                {1.0, 5.0},
                {2.0, 3.0},
                {4.0, 4.0}
        };
        double[][] bPoints = {
                {3.0, 2.0},
                {2.5, 3.5},
                {6.0, 1.0}
        };

        ParetoNonDominatedSet2D setA = buildSetFromPoints(aPoints);
        ParetoNonDominatedSet2D setB = buildSetFromPoints(bPoints);

        ParetoNonDominatedSet2D combinedViaCombine = new ParetoNonDominatedSet2D();
        ParetoNonDominatedSet2D.combine(setA, new double[]{0,0}, setB, new double[]{0,0}, combinedViaCombine);

        ParetoNonDominatedSet2D combinedViaAdd = buildSetFromPoints(aPoints);
        addPointsToSet(combinedViaAdd, bPoints);

        assertSetsEquivalent(combinedViaCombine, combinedViaAdd);
    }

    @Test
    public void testCombineEqualsAddRandomized() {
        Random rnd = new Random(12345);
        double[][] aPoints = new double[50][2];
        double[][] bPoints = new double[60][2];
        for (int i = 0; i < aPoints.length; i++) {
            aPoints[i][0] = rnd.nextDouble() * 100;
            aPoints[i][1] = rnd.nextDouble() * 100;
        }
        for (int i = 0; i < bPoints.length; i++) {
            bPoints[i][0] = rnd.nextDouble() * 100;
            bPoints[i][1] = rnd.nextDouble() * 100;
        }

        ParetoNonDominatedSet2D setA = buildSetFromPoints(aPoints);
        ParetoNonDominatedSet2D setB = buildSetFromPoints(bPoints);

        ParetoNonDominatedSet2D combinedViaCombine = new ParetoNonDominatedSet2D();
        ParetoNonDominatedSet2D.combine(setA, new double[]{0,0}, setB, new double[]{0,0}, combinedViaCombine);

        ParetoNonDominatedSet2D combinedViaAdd = buildSetFromPoints(aPoints);
        addPointsToSet(combinedViaAdd, bPoints);

        assertSetsEquivalent(combinedViaCombine, combinedViaAdd);
    }

    @Test
    public void testParetoNonDominatedSet2DVsGeneralSet() {
        Random rnd = new Random(12345);
        double[][] points = new double[50][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = rnd.nextDouble() * 100;
            points[i][1] = rnd.nextDouble() * 100;
        }

        // Create and populate ParetoNonDominatedSet2D
        ParetoNonDominatedSet2D set2D = new ParetoNonDominatedSet2D();
        for (double[] point : points) {
            set2D.addPoint(point);
        }

        // Create and populate ParetoNonDominatedSet
        ParetoNonDominatedSet setGeneral = new ParetoNonDominatedSet();
        for (double[] point : points) {
            setGeneral.addPoint(point);

        }
        // Compare sizes
        assertEquals(set2D.size(), setGeneral.size(), "Sizes differ between 2D and general Pareto sets");

        // Compare contents
        List <double[] > generalList = setGeneral.stream().sorted(ParetoNonDominatedSet2D.COMPARATOR_2D).toList();
        List <double[] > twoDList = set2D.stream().toList();
        compareLists(generalList, twoDList);

    }

    @Test
    public void performanceTest() {
        Random rnd = new Random(12345);
        double[][] points = new double[50_000][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = i;
            points[i][1] = -i;
            // points[i][0] = rnd.nextDouble() * 100;
            //points[i][1] = rnd.nextDouble() * 100;
        }

        // Create and populate ParetoNonDominatedSet2D
        ParetoNonDominatedSet2D set2D = new ParetoNonDominatedSet2D();
        long start = System.nanoTime();
        for (double[] point : points) {
            set2D.addPoint(point);
        }
        long efficientTime = System.nanoTime() - start;
        System.out.println("Effective time: " + efficientTime + " nanoseconds");

        // Create and populate ParetoNonDominatedSet
        ParetoNonDominatedSet setGeneral = new ParetoNonDominatedSet();
        start = System.nanoTime();
        for (double[] point : points) {
            setGeneral.addPoint(point);
        }
        long nonEfficientTime = System.nanoTime() - start;
        System.out.println("Non-efficient time: " + nonEfficientTime + " nanoseconds");


        assertTrue(efficientTime < nonEfficientTime, "ParetoNonDominatedSet2D should be more efficient than ParetoNonDominatedSet");
        // Compare sizes
        assertEquals(set2D.size(), setGeneral.size(), "Sizes differ between 2D and general Pareto sets");

        // Compare contents
        List <double[] > generalList = setGeneral.stream().sorted(ParetoNonDominatedSet2D.COMPARATOR_2D).toList();
        List <double[] > twoDList = set2D.stream().toList();
        compareLists(generalList, twoDList);
    }

    // --- Helpers ---

    private ParetoNonDominatedSet2D buildSetFromPoints(double[][] points) {
        ParetoNonDominatedSet2D set = new ParetoNonDominatedSet2D();
        addPointsToSet(set, points);
        return set;
    }

    private void addPointsToSet(ParetoNonDominatedSet2D set, double[][] points) {
        for (double[] p : points) {
            set.addPoint(p);
        }
    }

    private void assertSetsEquivalent(ParetoNonDominatedSet2D s1, ParetoNonDominatedSet2D s2) {
        // Comprobar tamaño si existe
        int size1 = s1.size();
        int size2 = s2.size();
        assertEquals(size1, size2, "size difiere entre combine y addPoint");

        // Al iterar sobre s1 y s2 deben salir los mismos puntos en el mismo orden
        List<double []> s1list = s1.stream().toList();
        List<double []> s2list = s2.stream().toList();
        compareLists(s1list, s2list);
    }

    private static void compareLists(List<double[]> s1list, List<double[]> s2list) {
        for (int i = 0; i < s1list.size(); i++) {
            double[] p1 = s1list.get(i);
            double[] p2 = s2list.get(i);
            assertArrayEquals(p1, p2, "Punto en posición "+i+" difiere entre combine y addPoint");
        }
    }
}
