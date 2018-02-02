package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesISWithDataArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;
import neo.landscape.theory.apps.util.TwoStatesIntegerSetWithData;
import neo.landscape.theory.apps.util.TwoStatesIntegerSetWithData.DataFactory;

public class PartitionCrossoverArticulationPoints {
    
    public static enum FlippedSolution {
       BLUE, RED
    }
    
    private static class APInformation {
        double greenMinusBlueForParent;
        double yellowMinusRedForParent;
        double accumulatedDifference;
        double contributionIfBlueIsFlipped;
        double contributionIfRedIsFlipped;
    }
    
    private static class BranchDecision {
        FlippedSolution decisionForRedFlip;
        FlippedSolution decisionForBlueFlip;
        Set<Integer> biconnectedComponent;
        public BranchDecision(FlippedSolution forRed, FlippedSolution forBlue, Set<Integer> biconnectedComponent) {
            decisionForBlueFlip = forBlue;
            decisionForRedFlip = forRed;
            this.biconnectedComponent = biconnectedComponent;
        }
    }

	protected static final int VARIABLE_LIMIT = 1<<29;
	
    protected Random rnd;
	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;
    
    protected PartitionComponent component;
    protected VariableProcedence varProcedence;
    
    protected boolean debug;

    protected PrintStream ps;
    
    private int numberOfComponents;

    public PartitionCrossoverArticulationPoints(EmbeddedLandscape el) {
		this.el = el;
		bfsSet = new TwoStatesISArrayImpl(el.getN());
		if (el.getN() > VARIABLE_LIMIT) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+VARIABLE_LIMIT);
		}
		
		rnd = new Random(Seeds.getSeed());
		subfns = new HashSet<Integer>();
		toExplore = new LinkedList<Integer>();
		ComponentAndVariableMask componentAndVariableProcedence = new ComponentAndVariableMask(el.getN());
		component = componentAndVariableProcedence;
		varProcedence = componentAndVariableProcedence;
		
		initializeDataStructures();

	}

    public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	protected boolean isNodeInReducedGraph(int v, PBSolution blue,
			PBSolution red) {
	    if (blue.getBit(v) != red.getBit(v)) {
	        varProcedence.markAsRed(v);
	        return true;
	    } else {
	        varProcedence.markAsPurple(v);
	        return false;
	    }
	}

	/**
	 * Search for the net node in the reduced graph
	 * 
	 * @param blue
	 * @param red
	 * @return
	 */
	protected Integer nextNodeInReducedGraph(PBSolution blue, PBSolution red) {
		int v;

		while (bfsSet.hasMoreUnexplored()) {
			v = bfsSet.getNextUnexplored();
			if (isNodeInReducedGraph(v, blue, red)) {
				return v;
			} else {
				bfsSet.explored(v);
			}
		}

		return null;
	}

    private int low[];
    private int time[];
    private int parent[];
    private int degree[];
    private int globalTime;
    private TwoStatesIntegerSetWithData<APInformation> articulationPointsOfBC;
    private Set<Integer> allArticulationPoints;
    private Stack<VertexIndex> stack;
    private Stack<Edge> edgeStack;
    private Set<Set<Integer>> biconnectedComponents;
    private TwoStatesIntegerSet subfunctions;
    private TwoStatesIntegerSet variablesInBiconnectedComponents;
    
    private Map<Integer, FlippedSolution> allArticulationPointsToFlip;
    
    private Map<Integer, List<BranchDecision>> articulationPointsDecisions;
    
    private Set<Set<Integer>> partition;

    private double deltaBlueRed;

    private double deltaGreenBlue;

    private int articulationPointToFlip;
    private FlippedSolution solutionToFlip;;

    private double improvement;
    private double overAllImprovement;

    private double deltaYellowRed;
    
    static private class VertexIndex {
        private int vertex;
        private int index;
        
        VertexIndex(int v, int i) {
            vertex = v;
            index = i;
        }
        
        static VertexIndex newVertexIndex(int v, int i) {
            return new VertexIndex(v, i);
        }
    }
    
    static private class Edge {
        private int tail;
        private int head;
        
        Edge(int tail, int head) {
            this.tail =tail;
            this.head = head;
        }
        
        static Edge newEdge(int tail, int head) {
            return new Edge(tail, head);
        }
    }
    
    private void initializeDataStructures() {
        int n = el.getN();
        articulationPointsOfBC = new TwoStatesISWithDataArrayImpl<>(n);
        articulationPointsOfBC.reset();
        allArticulationPoints = new HashSet<>();
        articulationPointsOfBC.setDataFactory(new DataFactory<APInformation> () {
            @Override
            public APInformation newData() {
                return new APInformation();
            }
            @Override
            public void recycle(APInformation data) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Recycling not supported");
            }
        });
        
        low = new int[n];
        time = new int [n];
        parent = new int [n];
        degree = new int[n];
        globalTime = 0;
        stack = new Stack<>();
        edgeStack = new Stack<>();
        biconnectedComponents = new HashSet<>();
        subfunctions = new TwoStatesISArrayImpl(el.getM());
        variablesInBiconnectedComponents = new TwoStatesISArrayImpl(n);
        allArticulationPointsToFlip = new HashMap<>();
        partition = new HashSet<>();
        articulationPointsDecisions = new HashMap<>();
        
    }
    
    public void setPrintStream(PrintStream ps) {
        this.ps=ps;
    }

    protected PartitionComponent dfs(Integer root, PBSolution blue, PBSolution red) {
        component.clearComponent();
        
        parent[root] = -1;
        int rootChildren = 0;
        articulationPointsOfBC.reset();
        articulationPointsOfBC.setData(root, null);
        stack.add(VertexIndex.newVertexIndex(root, 0));
        while (!stack.isEmpty()) {
            VertexIndex vi = stack.pop();
            int v = vi.vertex;
            int i = vi.index;
            if (i == 0) {
                bfsSet.explored(v);
                time[v] = globalTime++;
                low[v] = time[v];
                component.addVarToComponent(v);
            }
            if (i < el.getInteractions()[v].length) {
                stack.add(VertexIndex.newVertexIndex(v, i+1));
                if (isNodeInReducedGraph(el.getInteractions()[v][i], blue, red)) {
                    int w = el.getInteractions()[v][i];
                    if (!bfsSet.isExplored(w)) { 
                        if (v == root) {
                            rootChildren++;
                        }
                        edgeStack.add(Edge.newEdge(v,  w));
                        stack.add(VertexIndex.newVertexIndex(w, 0));
                        parent[w] = v;

                    } else if (time[w] < time[v] && w != parent[v]) {
                        edgeStack.add(Edge.newEdge(v, w));
                        low[v] = Math.min(low[v], time[w]);
                    }
                } else {
                    bfsSet.explored(el.getInteractions()[v][i]);
                }
            } else if (!stack.isEmpty()){
                int w = v;
                vi = stack.peek();
                v = vi.vertex;
                low[v] = Math.min(low[v],low[w]);
                if (low[w] >= time[v]) {
                    if (v!=root) {
                        markInternalArticulationPoint(v);
                    }
                    if (!edgeStack.isEmpty()) {
                        deltaBlueRed = 0.0;
                        deltaGreenBlue = 0.0;
                        deltaYellowRed = 0.0;
                        
                        Set<Integer> component = new HashSet<>();
                        while (time[edgeStack.peek().tail] >= time[w]) {
                            Edge e = edgeStack.pop();
                            if (e.head != v) {
                                analizeVariableInBiconnectedComponent(blue, red, v, component, e.head);
                            }
                            if (e.tail != v) {
                                analizeVariableInBiconnectedComponent(blue, red, v, component, e.tail);
                            }
                        }
                        Edge e = edgeStack.pop();
                        if (e.head != v) {
                            analizeVariableInBiconnectedComponent(blue, red, v, component, e.head);
                        }
                        if (e.tail != v) {
                            analizeVariableInBiconnectedComponent(blue, red, v, component, e.tail);
                        }
                        
                        BranchDecision pd = new BranchDecision(null, null, component);
                        setDecisionOfBranch(v, pd);
                        
                        if (deltaBlueRed+deltaGreenBlue > 0) {
                            reportDebugInformation("Blue for blue flip in: "+component+" ("+(deltaBlueRed+deltaGreenBlue)+")");
                            articulationPointsOfBC.getData(v).contributionIfBlueIsFlipped += deltaBlueRed+deltaGreenBlue;
                            pd.decisionForBlueFlip = FlippedSolution.BLUE;
                        } else {
                            pd.decisionForBlueFlip = FlippedSolution.RED;
                        }
                        
                        if (deltaBlueRed > deltaYellowRed) {
                            ps.println("Blue for red flip in: "+component+" ("+(deltaBlueRed)+")");
                            articulationPointsOfBC.getData(v).contributionIfRedIsFlipped += deltaBlueRed;
                            pd.decisionForRedFlip = FlippedSolution.BLUE;
                        } else {
                            articulationPointsOfBC.getData(v).contributionIfRedIsFlipped += deltaYellowRed;
                            pd.decisionForRedFlip = FlippedSolution.RED;
                        }
                        
                        analyzeArticulationPointFunctions(blue, red, v, component);
   
                        biconnectedComponents.add(component);
                    }
                }
            }
        }
        

        if (rootChildren >= 2) {
            markRootArticulationPoint(root, rootChildren);
        }
        
        if (rootChildren > 0) {
            // Finish the computation of options
            double totalBlueRedDifference = articulationPointsOfBC.getData(root).accumulatedDifference;
            improvement = Math.max(0, totalBlueRedDifference);
            articulationPointToFlip = -1;

            articulationPointsOfBC.getExplored().forEach(ap->{
                if (ap != root) {
                    postProcessArticulationPoint(totalBlueRedDifference, ap);
                }
            });
            if (articulationPointsOfBC.isExplored(root)) {
                APInformation apInfo = articulationPointsOfBC.getData(root);
                updateImprovementWithAPOptions(root, apInfo);
            }
        } else {
            articulationPointToFlip = -1;
            
            double[] vals = evaluateComponent(component, blue, red);
            
            double blueVal = vals[0];
            double redVal = vals[1];
            improvement = Math.max(blueVal-redVal, 0);
        }
        
		return component;
	}

    protected void setDecisionOfBranch(int v, BranchDecision pd) {
        List<BranchDecision> list = articulationPointsDecisions.get(v);
        if (list ==null) {
            list = new ArrayList<>();
            articulationPointsDecisions.put(v,list);
        }
        list.add(pd);
    }

    protected void reportDebugInformation(String message) {
        if (debug && ps != null) {
            ps.println(message);
        }
    }

    protected void postProcessArticulationPoint(double totalBlueRedDifference, int ap) {
        APInformation apInfo = articulationPointsOfBC.getData(ap);
        double blueFlipContribution = totalBlueRedDifference-apInfo.accumulatedDifference + apInfo.greenMinusBlueForParent;
        BranchDecision branchDecision = articulationPointsDecisions.get(ap).stream()
                .filter(bd->{return bd.decisionForBlueFlip==null;})
                .findFirst()
                .get();
        if (blueFlipContribution > 0) {
            reportDebugInformation("Blue for blue flip in parent of: "+ap+" ("+(blueFlipContribution)+")");
            apInfo.contributionIfBlueIsFlipped += blueFlipContribution;
            branchDecision.decisionForBlueFlip = FlippedSolution.BLUE;
        } else {
            branchDecision.decisionForBlueFlip = FlippedSolution.RED;
        }
        
        
        double redBlueDifference = totalBlueRedDifference-apInfo.accumulatedDifference;
        if (redBlueDifference > apInfo.yellowMinusRedForParent) {
            ps.println("Blue for red flip in parent of: "+ap+" ("+(redBlueDifference)+")");
            apInfo.contributionIfRedIsFlipped += redBlueDifference;
            branchDecision.decisionForRedFlip = FlippedSolution.BLUE;
        } else {
            apInfo.contributionIfRedIsFlipped += apInfo.yellowMinusRedForParent;
            branchDecision.decisionForRedFlip = FlippedSolution.RED;
        }
        
        updateImprovementWithAPOptions(ap, apInfo);
    }

    protected void updateImprovementWithAPOptions(int ap, APInformation apInfo) {
        if (apInfo.contributionIfRedIsFlipped > improvement) {
            articulationPointToFlip = ap;
            solutionToFlip = FlippedSolution.RED;
            improvement = apInfo.contributionIfRedIsFlipped;
        }
        
        if (apInfo.contributionIfBlueIsFlipped > improvement) {
            articulationPointToFlip = ap;
            solutionToFlip = FlippedSolution.BLUE;
            improvement = apInfo.contributionIfBlueIsFlipped;
        }
    }

    protected void analyzeArticulationPointFunctions(PBSolution blue, PBSolution red, int v,
            Set<Integer> component) {
        if (!variablesInBiconnectedComponents.isExplored(v)) {
            //variablesInBiconnectedComponents.explored(v);
            component.add(v);
            for (int fns: el.getAppearsIn()[v]) {
                if (!subfunctions.isExplored(fns)) {
                    if (checkFunctionLinkedToVariable(fns, v, blue, red)) {
                        subfunctions.explored(fns);
                        
                        double blueValue = el.evaluateSubFunctionFromCompleteSolution(fns, blue);
                        double redValue = el.evaluateSubFunctionFromCompleteSolution(fns, red);
                        deltaBlueRed += (blueValue-redValue);
                        articulationPointsOfBC.getData(v).contributionIfRedIsFlipped += (blueValue-redValue); 
                    }
                }
            }
        }
        articulationPointsOfBC.getData(v).accumulatedDifference += deltaBlueRed;
    }
    
    private boolean checkFunctionLinkedToVariable(int fns, int v, PBSolution blue, PBSolution red) {
        boolean onlyV = true;
        for (int var: el.getMasks()[fns]) {
            if (var != v && isNodeInReducedGraph(var, blue, red)) {
                onlyV = false;
                break;
            }
        }
        return onlyV;
    }

    protected void analizeVariableInBiconnectedComponent(PBSolution blue, PBSolution red, int v,
            Set<Integer> component, int newVariable) {
        if (!variablesInBiconnectedComponents.isExplored(newVariable)) {
            variablesInBiconnectedComponents.explored(newVariable);
            component.add(newVariable);
            if (articulationPointsOfBC.isExplored(newVariable)) {
                deltaBlueRed += articulationPointsOfBC.getData(newVariable).accumulatedDifference;
                
                BranchDecision pd = new BranchDecision(null, null, component);
                setDecisionOfBranch(newVariable, pd);
                
            }
            
            for (int fns: el.getAppearsIn()[newVariable]) {
                if (!subfunctions.isExplored(fns)) {
                    subfunctions.explored(fns);
                    double blueValue = el.evaluateSubFunctionFromCompleteSolution(fns, blue);
                    double redValue = el.evaluateSubFunctionFromCompleteSolution(fns, red);
                    deltaBlueRed += (blueValue-redValue);
                    for (int var: el.getMasks()[fns]) {
                        if (var == v || articulationPointsOfBC.isExplored(var)) {
                            double greenValue = 
                                    el.evaluateSubFunctionFromCompleteSolutionFlippingVariable(fns, var, blue);
                            double yellowValue = 
                                    el.evaluateSubFunctionFromCompleteSolutionFlippingVariable(fns, var, red);
                            if (var == v){
                                deltaGreenBlue += greenValue - blueValue;
                                deltaYellowRed += yellowValue - redValue;
                            } else {
                                articulationPointsOfBC.getData(var).greenMinusBlueForParent += greenValue - blueValue;
                                articulationPointsOfBC.getData(var).yellowMinusRedForParent += yellowValue - redValue;
                            }
                        }
                    }
                }
            }
        }
    }

    protected void markRootArticulationPoint(Integer root, int rootChildren) {
        articulationPointsOfBC.explored(root);
        degree[root]=rootChildren;
    }

    protected void markInternalArticulationPoint(int v) {
        if (articulationPointsOfBC.isExplored(v)) {
            degree[v]++;
        } else {
            articulationPointsOfBC.explored(v);
            articulationPointsOfBC.setData(v, null);
            degree[v] = 2;
        }
    }

    protected double[] evaluateComponent(Iterable<Integer> vars, PBSolution... sol) {
		double[] res = new double[sol.length];

		subfns.clear();

		for (int variable : vars) {
		    int[] fns = el.getAppearsIn()[variable];
			for (int i = 0; i < fns.length; i++) {
				subfns.add(fns[i]);
			}
		}

		for (int i = 0; i < res.length; i++) {
			res[i] = 0.0;
			for (int fn : subfns) {
				res[i] += el.evaluateSubFunctionFromCompleteSolution(fn, sol[i]);
			}
		}

		return res;
	}

	public PBSolution recombine(PBSolution blue, PBSolution red) {

        bfsSet.reset();
        allArticulationPoints.clear();
        globalTime = 0;
        biconnectedComponents.clear();
        edgeStack.clear();
        stack.clear();
        variablesInBiconnectedComponents.reset();
        subfunctions.reset();
        overAllImprovement = 0.0;
        allArticulationPointsToFlip.clear();
        partition.clear();
        articulationPointsDecisions.clear();
        
        
        reportDebugInformation("Red solution: "+red+"("+el.evaluate(red)+")");
        reportDebugInformation("Blue solution:"+blue+"("+el.evaluate(blue)+")");
        
        
		PBSolution child = new PBSolution(red); //child, copy of red
		numberOfComponents = 0;

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = dfs(node, blue, red);
		    
		    overAllImprovement += improvement;
		    if (articulationPointToFlip >= 0) {
		        allArticulationPointsToFlip.put(articulationPointToFlip, solutionToFlip);
		    }
		    
		    articulationPointsOfBC.getExplored().forEach(allArticulationPoints::add);
		    
		    if (debug) {
		        Set<Integer> varsInComponent = new HashSet<>();
		        component.forEach(var->{
		            varsInComponent.add(var);
		        });
		        partition.add(varsInComponent);


		        if (ps!= null) {
		            printComponent(component);
		        }
		    }
		    
		    prepareOffspringSolution(blue, red, child, component);
			numberOfComponents++;
		}
		
		if (ps!=null) {
		    printBiconnectedComponents();
            printArticulationPoints();
		}

		return child;
	}

    protected void prepareOffspringSolution(PBSolution blue, PBSolution red, PBSolution child,
            PartitionComponent component) {
        if (articulationPointToFlip < 0) {
            double[] vals = evaluateComponent(component, blue, red);

            double blueVal = vals[0];
            double redVal = vals[1];

            if (blueVal > redVal || ((blueVal==redVal) && rnd.nextDouble() < 0.5)) {
                for (int variable : component) {
                    blueBitInChild(blue, child, variable);
                }
            }
        } else {
            if (FlippedSolution.BLUE.equals(solutionToFlip)) {
                // child does not need to be flipped, since it is red, 
                // and does not need to be marked as red
                for (BranchDecision bd:  articulationPointsDecisions.get(articulationPointToFlip)) {
                    if (FlippedSolution.BLUE.equals(bd.decisionForBlueFlip)) {
                        markBlueRecursivelyAvoiding(blue, child, bd.biconnectedComponent, articulationPointToFlip);
                    }
                }
            } else {
                blueBitInChild(blue, child, articulationPointToFlip);
                for (BranchDecision bd:  articulationPointsDecisions.get(articulationPointToFlip)) {
                    if (FlippedSolution.BLUE.equals(bd.decisionForRedFlip)) {
                        markBlueRecursivelyAvoiding(blue, child, bd.biconnectedComponent, articulationPointToFlip);
                    }
                }
            }
        }
    }
	
	protected void markBlueRecursivelyAvoiding(PBSolution blue, PBSolution child, Set<Integer> component, int var) {
        List<BranchDecision> toPaint = new ArrayList<>();

	    for (int variable: component) {
	        if (variable != var) {
	            blueBitInChild(blue, child, variable);
	            if (articulationPointsOfBC.isExplored(variable)) {
	                articulationPointsDecisions.get(variable).stream()
	                    .filter(bd->{return bd.biconnectedComponent != component;})
	                    .forEach(toPaint::add);
	            }
	        }
	    }
	    
	    while (!toPaint.isEmpty()) {
	        BranchDecision exploring = toPaint.remove(toPaint.size()-1);
	        for (int variable: exploring.biconnectedComponent) {
	            if (varProcedence.getColor(variable) != varProcedence.BLUE &&
	                    articulationPointsOfBC.isExplored(variable)) {
	                articulationPointsDecisions.get(variable).stream()
	                .filter(bd->{return bd != exploring;})
	                .forEach(toPaint::add);
	            }
	            blueBitInChild(blue, child, variable);
	        }

	    }
	}

    protected void blueBitInChild(PBSolution blue, PBSolution child, int variable) {
        child.setBit(variable, blue.getBit(variable));
        varProcedence.markAsBlue(variable);
    }

    public int getNumberOfComponents() {
        return numberOfComponents;
    }
    
    public int getNumberOfArticulationPoints() {
        return allArticulationPoints.size();
    }
    
    private void printComponent(PartitionComponent pc) {
        if (debug) {
            Set<Integer> componentSet = new HashSet<>();
            pc.forEach(e->componentSet.add(e));

            ps.print("{");
            for (int u: componentSet) {
                for (int v: el.getInteractions()[u]) {
                    if (u < v && componentSet.contains(v)) {
                        ps.print("("+u+","+v+"),");
                    }
                }
            }
            ps.println("}");
        }
    }
    
    private void printBiconnectedComponents() {
       reportDebugInformation(""+biconnectedComponents);
    }
    
    private void printArticulationPoints() {
        reportDebugInformation(allArticulationPoints.toString());
    }
    
    public IntStream degreeOfArticulationPoints() {
        return allArticulationPoints.stream().mapToInt(i->degree[i]);
    }
    
    public void printArticulationPointToFlipAndImprovement() {
        reportDebugInformation("Articulation Point to flip:"+allArticulationPointsToFlip);
        reportDebugInformation("Improvement: "+overAllImprovement);
    }
    
    public Map<Integer, FlippedSolution> getAllArticulationPointsToFlip() {
        return allArticulationPointsToFlip;
    }
    
    public double getOverallImprovement() {
        return overAllImprovement;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public Set<Set<Integer>> getPartition() {
        return partition;
    }
    
    public Set<Integer> getAllArticulationPoints() {
        return allArticulationPoints;
    }
}
