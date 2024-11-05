package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

import java.io.PrintStream;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class FourierPartitionCrossover<W extends WalshCoefficientsInterface<W>> implements CrossoverInternal {

	protected static final int VARIABLE_LIMIT = 1<<29;

	private Logger log = Logger.getLogger(this.getClass().getName());

    protected Random rnd;
	protected WalshBasedFunction<W> el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;

    protected PartitionComponent component;
	protected Set<Integer> varsInThisComponent = new HashSet<>();
    protected VariableProcedence varProcedence;


    protected long lastRuntime;
    private int numberOfComponents;

    private PrintStream ps;
	private W wcsConstrained;

	private boolean debug = false;
	private List<Set<Integer>> varsInComponents;
	private List<Double> redValues;
	private Set<Integer> termsWithOtherComponentsToo;
	private boolean numberOfOriginalWalshTermsPrinted;

	public FourierPartitionCrossover(WalshBasedFunction<W> el) {
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
		termsWithOtherComponentsToo = new HashSet<>();

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

		Set<Integer> termsInThisComponent = new HashSet<>();
		//TwoStatesIntegerSet termsInThisComponent = new TwoStatesISArrayImpl(wcsConstrained.getNumberOfIDs());

		while (toExplore.size() > 0) {
			// Take one node to explore
			int var = toExplore.remove();
			if (bfsSet.isExplored(var)) {
				continue;
			}

			wcsConstrained.getCoefficientsForVariable(var)
				.forEach(wc -> {
					// 1. itera por cada coeficiente de Walsh que toca esta variable
					final AtomicInteger inThisComponent = new AtomicInteger(0);
					final AtomicInteger toBeAssigned = new AtomicInteger(0);
					List<Integer> varToBeAssigned = new ArrayList<>();
					// 2. miro todas las variables del término
					wcsConstrained.getVarsForID(wc).forEach(otherVar -> {
						if (varsInThisComponent.contains(otherVar)) {
							inThisComponent.incrementAndGet();
						} else if (!bfsSet.isExplored(otherVar) && isNodeInReducedGraph(otherVar, blue, red)) {
							toBeAssigned.incrementAndGet();
							varToBeAssigned.add(otherVar);
						}
					});

					if ((inThisComponent.intValue() & toBeAssigned.intValue() & 0x01) != 0) {
						// 4. Si hay un número impar en el componente actual y lo que queda es impar también,
						// tomo una variable no asignada a ningún componente y se añade a este componmente
						// y para explorar
						int varsToSelect = varToBeAssigned.size();
						int selectedVar = varToBeAssigned.get(rnd.nextInt(varsToSelect));

						toExplore.add(selectedVar);
						varsInThisComponent.add(selectedVar);
						component.addVarToComponent(selectedVar);
						toBeAssigned.decrementAndGet();
						inThisComponent.incrementAndGet();
					}

					if ((inThisComponent.intValue() & 1) != 0) {
						termsInThisComponent.add(wc);
					} else {
						termsInThisComponent.remove(wc);
					}

					if (inThisComponent.intValue() == wcsConstrained.numberOfVarsForID(wc)) {
						termsWithOtherComponentsToo.remove(wc);
					} else {
						termsWithOtherComponentsToo.add(wc);
					}

				});
			bfsSet.explored(var);
		}


		double redValue = termsInThisComponent.stream()
			.mapToDouble(wc -> wcsConstrained.evaluate(wc, red))
			.sum();

		component.setRedValue(redValue);

		return component;
	}

	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    PBSolution solution = recombineInternal(blue, red);
		reportIfPossible(() -> "Recombination time:" + getLastRuntime());
	    
	    return solution;
	}

	private void reportIfPossible(Supplier<String> message) {
		if (ps!=null) {
			ps.println(message.get());
		}
	}

	public List<Set<Integer>> getVarsInComponents() {
		initializeVarsInComponents();
		return varsInComponents;
	}

	public List<Double> getRedValues() {
		initializeRedValues();
		return redValues;
	}

	private void resetDebugInformation() {
		initializeVarsInComponents();
		varsInComponents.clear();

		initializeRedValues();
		redValues.clear();

	}

	private void initializeRedValues() {
		if (redValues == null) {
			redValues = new ArrayList<>();
		}
	}

	private void initializeVarsInComponents() {
		if (varsInComponents == null) {
			varsInComponents = new ArrayList<>();
		}
	}

	@Override
	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
		if (debug) {
			resetDebugInformation();
		}

		long initTime = System.nanoTime();
        bfsSet.reset();
        
		PBSolution child = new PBSolution(red); //child, copy of red
		numberOfComponents = 0;

		if (!numberOfOriginalWalshTermsPrinted) {
			reportIfPossible(() -> "* Number of original Walsh terms: "+el.getOriginalWalshTerms().getNonzeroTerms());
			numberOfOriginalWalshTermsPrinted=true;
		}

		wcsConstrained = el.contraint(red, blue);
		termsWithOtherComponentsToo.clear();

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = bfs(node, blue, red);
			double redVal = component.getRedValue();

			if (redVal < 0 || ((redVal==0) && rnd.nextDouble() < 0.5)) {
			    for (int variable : component) {
			        child.flipBit(variable);
                    varProcedence.markAsBlue(variable);
                }
			}
			numberOfComponents++;
			// Tras este proceso tiene que cumplirse que para todos los coeficientes, solo puede haber como mucho
			// un componente que tenga un número impar de variables

			if (debug) {
				Set<Integer> varsInComponent = new HashSet<>();
				for (int variable: component) {
					varsInComponent.add(variable);
				}
				getVarsInComponents().add(varsInComponent);
				getRedValues().add(redVal);
			}
		}
		lastRuntime = System.nanoTime() - initTime;

		reportIfPossible(() -> "* Number of components: "+getNumberOfComponents());
		reportIfPossible(() -> "* Constrained Walsh terms: "+getWcsConstrained().getNonzeroTerms());
		reportIfPossible(() -> "* Walsh terms with several components: "+termsWithOtherComponentsToo.size());

		return child;
	}

	public WalshCoefficientsInterface getWcsConstrained() {
		return wcsConstrained;
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
