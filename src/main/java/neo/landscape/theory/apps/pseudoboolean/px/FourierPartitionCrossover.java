package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshConstraint;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FourierPartitionCrossover implements CrossoverInternal {

	protected static final int VARIABLE_LIMIT = 1<<29;

	private Logger log = Logger.getLogger(this.getClass().getName());

    protected Random rnd;
	protected WalshBasedFunction el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;

    protected PartitionComponent component;
	protected Set<Integer> varsInThisComponent = new HashSet<>();
    protected VariableProcedence varProcedence;

	protected Map<Integer, List<WalshCoefficient>> wcByVariable = new HashMap<>();


    protected long lastRuntime;
    private int numberOfComponents;

    private PrintStream ps;
	private WalshCoefficients wcsConstrained;

	private boolean debug = false;

	public FourierPartitionCrossover(WalshBasedFunction el) {
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

	}

    public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean getDebug() {
		return debug;
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

    protected PartitionComponent bfs(Integer node, PBSolution blue, PBSolution red) {

		toExplore.clear();
		component.clearComponent();
		varsInThisComponent.clear();
		
		toExplore.add(node);
		component.addVarToComponent(node);
		varsInThisComponent.add(node);

		Set<WalshCoefficient> termsInThisComponent = new HashSet<>();

		while (toExplore.size() > 0) {
			// Take one node to explore
			int var = toExplore.remove();
			if (bfsSet.isExplored(var)) {
				continue;
			}

			wcByVariable.getOrDefault(var, (List<WalshCoefficient>)Collections.EMPTY_LIST).forEach(wc -> {
				// 1. itera por cada coeficiente de Walsh que toca esta variable
				int inThisComponent = 0;
				int toBeAssigned = 0;
				Integer lastVarToBeAssigned=null;
				// 2. miro las otras variables
				for (int otherVar: wc.variables) {
					if (varsInThisComponent.contains(otherVar)) {
						inThisComponent++;
					} else if (!bfsSet.isExplored(otherVar) && isNodeInReducedGraph(otherVar, blue, red)) {
						toBeAssigned++;
						lastVarToBeAssigned = otherVar; // TODO: añadir a lista para seleccionar de forma aleatoria
					}
				}
				if ((inThisComponent & toBeAssigned & 0x01) != 0) {
					// 4. Si hay un número impar en el componente actual y lo que queda es impar también,
					// tomo una variable no asignada a ningún componente y se añade a este componmente
					// y para explorar
					toExplore.add(lastVarToBeAssigned);
					varsInThisComponent.add(lastVarToBeAssigned);
					component.addVarToComponent(lastVarToBeAssigned);
					toBeAssigned--;
					inThisComponent++;
				}

				if ((inThisComponent & 1) != 0) {
					termsInThisComponent.add(wc);
				} else {
					termsInThisComponent.remove(wc);
				}
			});

			bfsSet.explored(var);
		}

		double redValue = termsInThisComponent.stream()
			.mapToDouble(wc -> wc.evaluate(red))
			.sum();

		component.setRedValue(redValue);

		return component;
	}

	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    PBSolution solution = recombineInternal(blue, red);
		reportIfPossible("Recombination time:" + getLastRuntime());
	    
	    return solution;
	}

	private void reportIfPossible(String message) {
		if (ps!=null) {
			ps.println(message);
		}
	}
	
	@Override
	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
		Set<Set<Integer>> varsInComponents = new HashSet<>();
		long initTime = System.nanoTime();
        bfsSet.reset();
        
		PBSolution child = new PBSolution(red); //child, copy of red
		numberOfComponents = 0;

		wcsConstrained = el.contraint(red, blue);
		wcByVariable.clear();
		wcsConstrained.stream().forEach(wc->{
			wc.variables.forEach(var -> {
				wcByVariable.computeIfAbsent(var,(v)-> new ArrayList<>());
				wcByVariable.computeIfPresent(var, (v,l)-> {
					l.add(wc);
					return l;
				});
			});
		});

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = bfs(node, blue, red);
			double redVal = component.getRedValue();

			if (redVal < 0 || ((redVal==0) && rnd.nextDouble() < 0.5)) {
			    for (int variable : component) {
			        child.setBit(variable, blue.getBit(variable));
                    varProcedence.markAsBlue(variable);
                }
			}
			numberOfComponents++;
			// Tras este proceso tiene que cumplirse que para todos los coeficientes, solo puede haber como mucho
			// un componente que tenga un número impar de variables

			Set<Integer> varsInComponent = new HashSet<>();
			for (int variable: component) {
				varsInComponent.add(variable);
			}
			varsInComponents.add(varsInComponent);

		}
		lastRuntime = System.nanoTime() - initTime;

		if (debug) {
			checkVariablesInComponents(varsInComponents);
		}

		reportIfPossible("* Number of components: "+getNumberOfComponents());

		return child;
	}

	private void checkVariablesInComponents(Set<Set<Integer>> varsInComponents) {
		wcsConstrained.stream()
			.forEach(wc->{
				long [] oddCounts = varsInComponents.stream().mapToLong(varSet ->
					varSet.stream().filter(wc.variables::contains).count()
				)
					.filter(l-> (l&1)!=0).toArray();

				if (oddCounts.length > 1) {
					throw new AssertionError("There are more than one odd count in component. Walsh variables "
					+wc.variables+".\n"+"Counts: "+Arrays.toString(oddCounts));
				}

		});
	}

	public int getNumberOfComponents() {
        return numberOfComponents;
    }
    
    public long getLastRuntime() {
        return lastRuntime;
    }
    
	public void setPrintStream(PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public EmbeddedLandscape getEmbddedLandscape() {
		return el;
	}

	@Override
	public VariableProcedence getVarProcedence() {
		return varProcedence;
	}


}
