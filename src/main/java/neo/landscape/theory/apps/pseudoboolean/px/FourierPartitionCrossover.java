package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class FourierPartitionCrossover<W extends WalshCoefficientsInterface<W>> implements CrossoverInternal {

	protected static final int VARIABLE_LIMIT = 1<<29;

	private Logger log = Logger.getLogger(this.getClass().getName());

    protected Random rnd;
	protected WalshBasedFunction<W> wbf;
	protected EmbeddedLandscape originalEL;
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

	private boolean randomizeTies = true;
	private int targetMinComponentSize = 1;

	public FourierPartitionCrossover(WalshBasedFunction<W> wbf, EmbeddedLandscape originaEL) {
		this.wbf = wbf;
		this.originalEL = originaEL;
		bfsSet = new TwoStatesISArrayImpl(wbf.getN());
		if (wbf.getN() > VARIABLE_LIMIT) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+VARIABLE_LIMIT);
		}
		
		rnd = new Random(Seeds.getSeed());
		subfns = new HashSet<Integer>();
		toExplore = new LinkedList<Integer>();
		ComponentAndVariableMask componentAndVariableProcedence = new ComponentAndVariableMask(wbf.getN());
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

	public boolean getRandomizeTies() {
		return randomizeTies;
	}

	public void setRandomizeTies(boolean randomizeTies) {
		this.randomizeTies = randomizeTies;
	}

	public void setTargetMinComponentSize(int targetMinComponentSize) {
		this.targetMinComponentSize = targetMinComponentSize;
	}

	public int getTargetMinComponentSize() {
		return targetMinComponentSize;
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

    protected PartitionComponent findComponent(Integer node, PBSolution blue, PBSolution red) {

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
					// 1. Iterate over each Walsh coefficient containing this variable
					final AtomicInteger inThisComponent = new AtomicInteger(0);
					final AtomicInteger toBeAssigned = new AtomicInteger(0);
					List<Integer> varToBeAssigned = new ArrayList<>();
					// 2. Look at all the variables of the Walsh term
					wcsConstrained.getVarsForID(wc).forEach(otherVar -> {
						if (varsInThisComponent.contains(otherVar)) {
							inThisComponent.incrementAndGet();
						} else if (!bfsSet.isExplored(otherVar) && isNodeInReducedGraph(otherVar, blue, red)) {
							toBeAssigned.incrementAndGet();
							varToBeAssigned.add(otherVar);
						}
					});

					if ((inThisComponent.intValue() < targetMinComponentSize) ||
						// 4a. If we do not reach the target minimum component size....
						((inThisComponent.intValue() & toBeAssigned.intValue() & 0x01) != 0)) {
						// 4b. ... or If there is an odd number in the current component and what remains to assign is odd
						// we take a variable "to be assigned" and add it to this component
						// and to explore
						if (!varToBeAssigned.isEmpty()) {
							int varsToSelect = varToBeAssigned.size();
							int selectedVar=0;
							if (varsToSelect > 1) {
								selectedVar = varToBeAssigned.get(rnd.nextInt(varsToSelect));
							}

							toExplore.add(selectedVar);
							varsInThisComponent.add(selectedVar);
							component.addVarToComponent(selectedVar);
							toBeAssigned.decrementAndGet();
							inThisComponent.incrementAndGet();
						}
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
			reportIfPossible(() -> "* Number of original Walsh terms: "+ wbf.getOriginalWalshTerms().getNonzeroTerms());
			numberOfOriginalWalshTermsPrinted=true;
		}

		wcsConstrained = wbf.contraint(red, blue);
		termsWithOtherComponentsToo.clear();

		int oneVarImproving = 0;
		int moreThanOneVarImproving = 0;
		int oneVarNonImproving = 0;
		int moreThanOneVarNonImproving = 0;

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = findComponent(node, blue, red);
			double redVal = component.getRedValue();
			int numberOfVarsInComponent = varsInThisComponent.size();

			if (redVal< 0 && numberOfVarsInComponent == 1) {
				oneVarImproving++;
			} else if (redVal < 0 && numberOfVarsInComponent > 1) {
				moreThanOneVarImproving++;
			} else if (redVal >= 0 && numberOfVarsInComponent == 1) {
				oneVarNonImproving++;
			} else if (redVal >= 0 && numberOfVarsInComponent > 1) {
				moreThanOneVarNonImproving++;
			}

			if (redVal < 0 || ((redVal==0) && randomizeTies && rnd.nextDouble() < 0.5)) {
			    for (int variable : component) {
			        child.flipBit(variable);
                    varProcedence.markAsBlue(variable);
                }
			}
			numberOfComponents++;
			// After this process, it must happen that for all the coefficients
			// at most one component has an odd number of variables

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

		final int foneVarImproving = oneVarImproving;
		final int fmoreThanOneVarImproving = moreThanOneVarImproving;
		final int foneVarNonImproving = oneVarNonImproving;
		final int fmoreThanOneVarNonImproving = moreThanOneVarNonImproving;

		reportIfPossible(() -> "* Number of components: "+getNumberOfComponents());
		reportIfPossible(() -> String.format("* Component details (OI, ON, MI, MN): %d, %d, %d, %d", foneVarImproving, foneVarNonImproving, fmoreThanOneVarImproving, fmoreThanOneVarNonImproving));
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
		return originalEL;
	}

	@Override
	public VariableProcedence getVarProcedence() {
		return varProcedence;
	}


}
