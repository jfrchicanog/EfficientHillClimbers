package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.MultiobjectiveProblem;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class VectorMKLandscape implements MultiobjectiveProblem {
    protected EmbeddedLandscape [] functions;
    protected int n;
    
    protected VectorMKSubfunctionTranslator subfunctionsTranslator;
    protected int[][] appearsIn;
    protected int[][] interactions;
    
    protected VectorMKLandscape() {
        
    }
    
    public VectorMKLandscape (EmbeddedLandscape ... functions) {
        configureEmbeddedLandscapes(functions);
    }

    protected void configureEmbeddedLandscapes(EmbeddedLandscape... functions) {
        if (functions==null || functions.length==0) {
            throw new IllegalArgumentException("At least on objective must exist");
        }
        this.functions=functions;
        n = functions[0].getN();
        for (int i = 1; i < functions.length; i++) {
            if (functions[i].getN() != n) {
                throw new IllegalArgumentException("Not all objectives have the same variables");
            }
        }
        
        subfunctionsTranslator = new VectorMKSubfunctionTranslator(functions);
    }
    
    public int getDimension() {
        return functions.length;
    }
    
    public EmbeddedLandscape getComponent(int i) {
        return functions[i];
    }

    @Override
    public void setSeed(long seed) {
        throw new UnsupportedOperationException("This is a container problem, and cannot be configured");
    }

    @Override
    public void setConfiguration(Properties prop) {
        throw new UnsupportedOperationException("This is a container problem, and cannot be configured");
        
    }

    @Override
    public PBSolution getRandomSolution() {
        return functions[0].getRandomSolution();
    }

    @Override
    public double[] evaluate(Solution sol) {
        double [] result = new double [functions.length];
        for (int i = 0; i < functions.length; i++) {
            result[i] = functions[i].evaluate(sol);
        }
        return result;
    }

    @Override
    public BigDecimal[] evaluateArbitraryPrecision(Solution sol) {
        BigDecimal [] result = new BigDecimal [functions.length];
        for (int i = 0; i < functions.length; i++) {
            result[i] = functions[i].evaluateArbitraryPrecision(sol);
        }
        return result;
    }
    
    public int[][] getAppearsIn() {
        if (appearsIn == null) {
            computeAppearsIn();
        }

        return appearsIn;
    }

    public int[][] getInteractions() {
        if (interactions == null) {
            computeInteractions();
        }

        return interactions;
    }
    
    protected void computeInteractions() {
        interactions = new int[n][];

        for (int variable=0; variable < n; variable++) {
            interactions[variable] = computeInteractionsForVariable(variable);
        }
    }

    private int[] computeInteractionsForVariable(int variable) {
        Set<Integer> coOccurrentVariables = new HashSet<Integer>();
        for (int dimension = 0; dimension < functions.length; dimension++) {
            for (int interactingVariable: functions[dimension].getInteractions()[variable]) {
                coOccurrentVariables.add(interactingVariable);
            }
        }
        int res [] = new int [coOccurrentVariables.size()];
        int index=0; 
        for (int interactingVariable: coOccurrentVariables) {
            res[index++] = interactingVariable;
        }
        return res;
    }

    private void computeAppearsIn() {
        appearsIn = new int[n][];
        for (int variable=0; variable < n; variable++) {
            appearsIn[variable] = computeSubfunctionsForVariable(variable);
        }
    }

    private int[] computeSubfunctionsForVariable(int variable) {
        int [] result = new int[computeSubfunctionsInWhichVariableAppears(variable)];
        
        int index=0;
        for (int dimension = 0; dimension < functions.length; dimension++) {
            for (int subfunction : functions[dimension].getAppearsIn()[variable]) {
                result[index++] = getSubfunctionsTranslator().subfunctionID(dimension, subfunction);
            }
        }
        return result;
    }

    private int computeSubfunctionsInWhichVariableAppears(int variable) {
        int appearsInSize = 0;
        for (int dimension = 0; dimension < functions.length; dimension++) {
            appearsInSize += functions[dimension].getAppearsIn()[variable].length;
        }
        return appearsInSize;
    }
    
    public int getN() {
        return n;
    }
    
    public int getM() {
        return subfunctionsTranslator.getM();
    }
    
    public int getMaskLength(int sf) {
        int dimension = subfunctionsTranslator.dimensionOfSunbfunctionID(sf);
        int subfunctionInDimension = subfunctionsTranslator.subfunctionOfSubfunctionID(sf);
        return functions[dimension].getMaskLength(subfunctionInDimension);
    }

    public VectorMKSubfunctionTranslator getSubfunctionsTranslator() {
        return subfunctionsTranslator;
    }

    public int getMasks(int sf, int v) {
        int dimension = subfunctionsTranslator.dimensionOfSunbfunctionID(sf);
        int subfunctionInDimension = subfunctionsTranslator.subfunctionOfSubfunctionID(sf);
        return functions[dimension].getMasks(subfunctionInDimension, v);
    }

    public double evaluateSubfunction(int sf, PBSolution subSov) {
        int dimension = subfunctionsTranslator.dimensionOfSunbfunctionID(sf);
        int subfunctionInDimension = subfunctionsTranslator.subfunctionOfSubfunctionID(sf);
        return functions[dimension].evaluateSubfunction(subfunctionInDimension, subSov);
    }


    
    

}
