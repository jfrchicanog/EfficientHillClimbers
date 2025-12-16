package neo.landscape.theory.apps.pseudoboolean.experiments.mo;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.*;
import neo.landscape.theory.apps.util.Process;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

public class MOLocalOptima implements Process {

    private static final String PROBLEM_CHAR = "P";
    private static final String PROBLEM="problem";
    private static final String MNK_PROBLEM = "mnk";
    private static final String MQUBO = "mqubo";
    private static final String PREFIX_ARGUMENT = "prefix";
    private static final String OUTPUT_FILE_ARGUMENT = "output";
    private static final String REORDER_VARIABLES_ARGUMENT = "reorder";
    private static final String INSTANCE_OUTPUT_FILE_ARGUMENT = "instanceOutput";


    private static final long REPORT_PERIOD = 1L<<30;
    private VectorMKLandscape pbf;
    public int r=1;
    public MultiObjectiveHammingBallHillClimberSnapshot rball;
    public MultiObjectiveHammingBallHillClimberForInstanceOf rballfio;
    public long seed;
    protected String prefix="";

    protected List<VectorPBMove> [] moveBin;
    protected long counterValue;
    protected int [] variableOrder;
    protected int [] variableRank;
    //protected int [] counter;
    public List<PBSolution> localOptima;
    
    private String outputFileName;
    private VectorMKLandscapeConfigurator configurator;
    private String problem;
    private Options options;
    private PrintStream ps;
    private ByteArrayOutputStream ba;
    private String instanceOutputFileName;
    private boolean reoderVariables = false;

    private final Map<String, VectorMKLandscapeConfigurator> configurators = new HashMap<>();
    {
        configurators.put(MNK_PROBLEM, new MNKLandscapeConfigurator());
        configurators.put(MQUBO, new MquboConfigurator());
    }

    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        try {
            ps = new PrintStream(new GZIPOutputStream(ba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "This experiment computes all the Pareto Local Optima of multi-objective problems exploring the search  "
        + "space using Goldman's algorithm";
    }

    @Override
    public String getID() {
        return "molo";
    }

    private Options prepareOptions() {
        Options options = new Options();
        options.addOption(PROBLEM, true, "problem to be solved: "+configurators.keySet());
        options.addOption(PREFIX_ARGUMENT, true, "binary prefix to fix during the exploration");
        options.addOption(Option.builder(PROBLEM_CHAR)
            .numberOfArgs(2)
            .valueSeparator()
            .argName("property=value")
            .desc("properties for the problem")
            .build());
        options.addOption(OUTPUT_FILE_ARGUMENT, true, "output file");
        options.addOption(INSTANCE_OUTPUT_FILE_ARGUMENT, true, "instance output file");
        options.addOption(REORDER_VARIABLES_ARGUMENT, false, "reorder variables");

        return options;
    }

    private VectorMKLandscapeConfigurator getProblemConfigurator() {
        if (configurator==null) {
            configurator = createProblemConfigurator();
        }
        return configurator;
    }

    private VectorMKLandscapeConfigurator createProblemConfigurator() {
        VectorMKLandscapeConfigurator elc =  configurators.get(problem);
        if (elc == null) {
            throw new IllegalArgumentException("Problem "+problem+" is unknown");
        }
        return elc;
    }

    public long findLocalOptima() {
        int n = getPbf().getN();
        int index = n-1;
        long solutions=0;
        long nextSolutionsReport = solutions + REPORT_PERIOD;
        
        initializeVariableArrays();
        initializeMoveBin();
        initializeLONDataStructures();
        //counter = new int[n];
        
        preparePrefix();
        
        long localOptima = 0;
        
        int limitIndex = n-prefix.length();
        
        index = findNextIndex(index);
        if (index >= limitIndex) {
            solutions += (1L << limitIndex);
        }
        
        while (index < limitIndex) {
            index = findNextIndex(index);
            if (index < 0) {
                localOptima++;
                storeLocalOptima();
                index = 0;
            }
            solutions += (1L << index);
            if (solutions > nextSolutionsReport) {
                //System.out.println("Solutions explored: "+(double)solutions);
                //System.out.println("Local optima: "+localOptima);
                nextSolutionsReport = solutions + REPORT_PERIOD;
            }
            long previousCounterValue = counterValue;
            counterValue += (1L<<index);
            index=63-Long.numberOfLeadingZeros(counterValue & ~previousCounterValue);
            /*
            while (index < n && counter[index] == 1) {
                counter[index]=0;
                //rball.moveOneBit(index);
                index++;
            }*/
            if (index < limitIndex) { // FIXME: test if prefix works
                //counter[index]=1;
                rball.moveOneBit(variableOrder[index]);
            }
            
        }
        //System.out.println("Total solutions explored: "+solutions);
        return localOptima;
    }
    
    private void initializeLONDataStructures() {
        localOptima = new ArrayList<>();
    }

    private void storeLocalOptima() {
        localOptima.add(new PBSolution(rball.getSolution()));
    }

    private void preparePrefix() {
        int index=getPbf().getN()-1;
        for (char c: prefix.toCharArray()) {
            if (c == '1') {
                rball.moveOneBit(variableOrder[index]);
            }
            index--;
        }
    }

    private void initializeVariableArrays() {
        int n = getPbf().getN();
        if (reoderVariables) {
            Set<Integer> variablesToAdd = IntStream.range(0, n).boxed().collect(Collectors.toSet());
            List<Integer> variablesAdded = new ArrayList<>();
            int[][] interactions = getPbf().getInteractions();

            while (!variablesToAdd.isEmpty()) {
                OptionalInt minInteractions = OptionalInt.empty();
                int variable = -1;
                for (int i : variablesToAdd) {
                    int interactionCount = 1;
                    for (int v : interactions[i]) {
                        if (!variablesAdded.contains(v)) {
                            interactionCount++;
                        }
                    }
                    if (minInteractions.isEmpty() || interactionCount < minInteractions.getAsInt()) {
                        minInteractions = OptionalInt.of(interactionCount);
                        variable = i;
                    }
                }
                variablesAdded.add(variable);
                variablesToAdd.remove(variable);
                for (int v : interactions[variable]) {
                    if (!variablesAdded.contains(v)) {
                        variablesAdded.add(v);
                        variablesToAdd.remove(v);
                    }
                }
            }

            variableOrder = variablesAdded.stream().mapToInt(Integer::intValue).toArray();
        } else {
            variableOrder = IntStream.range(0, n).toArray();
        }
        /*
        variableOrder = IntStream.range(0, n)
            .boxed()
            .sorted(Comparator.comparingInt(i->interactions[i].length))
            .mapToInt(i->i).toArray();

         */
        variableRank = new int[n];
        for (int i=0; i < n; i++) {
            variableRank[variableOrder[i]]=i;
        }
    }

    private boolean improvingMove(VectorPBMove move) {
        boolean anyBetter = false;
        double [] improvements = move.getImprovement();
        for (double v: improvements) {
            if (v > 0) {
                anyBetter = true;
            } else if (v < 0) {
                return false;
            }
        }
        return anyBetter;
    }

    protected int findNextIndex(int index) {
        while (index >= 0) {
            for (VectorPBMove move : moveBin[index]) {
                if (improvingMove(move)) {
                    return index;
                }
            }
            index--;
        }
        return index;
    }
    
    protected void initializeMoveBin() {
        moveBin = new List[getPbf().getN()];
        for (int i = 0; i < moveBin.length; i++) {
            moveBin[i] = new ArrayList<>();
        }

        for (VectorPBMove move: rball.getMovesSelector().allMoves()) {
            OptionalInt min = OptionalInt.empty();

            for (int subfn : rballfio.subFunctionsAffected(move.flipVariables)) {
                int mlength = pbf.getMaskLength(subfn);
                for (int i=0; i < mlength; i++) {
                    int var = pbf.getMasks(subfn, i);
                    if (min.isEmpty() || variableRank[var] < min.getAsInt()) {
                        min = OptionalInt.of(variableRank[var]);
                    }
                }
            }

            min.ifPresent(m->{
                moveBin[m].add(move);
            });

            //System.out.println("Move "+move+" in "+min);
        }
        /*
        for (int i=0; i < moveBin.length; i++) {
            System.out.println("Bin["+i+"]: "+moveBin[i].size());
        }*/

    }

    public void prepareRBallExplorationAlgorithm() {
        Properties rballConfig = new Properties();
        rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, seed+"");

        rballfio =
            (MultiObjectiveHammingBallHillClimberForInstanceOf) new MultiObjectiveHammingBallHillClimber(rballConfig).initialize(pbf);
        PBSolution pbs = new PBSolution(getPbf().getN());

        double [] weights = new double [getPbf().getDimension()];
        Arrays.fill(weights, 1.0); // Not a proble, because the weights are not used at all
        
        rball = rballfio.initialize(weights, pbs);
        rball.setSeed(seed);
    }

    @Override
    public String getInvocationInfo() {
        HelpFormatter helpFormatter = new HelpFormatter();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
        return stringWriter.toString();
    }

    private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }

    private void showOptions() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(getID(), getOptions());

        try {
            Options problemOptions = new Options();
            getProblemConfigurator().prepareOptionsForProblem(problemOptions);
            helpFormatter.printHelp("Problem: "+problem, problemOptions);
        } catch (RuntimeException e) {
        }
    }

    private CommandLine parseCommandLine(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }

    public void execute(String[] args) {
        if (args.length == 0) {
            showOptions();
            return;
        }

        try {
            CommandLine commandLine = parseCommandLine(args);
            problem = commandLine.getOptionValue(PROBLEM);
            if (commandLine.hasOption(OUTPUT_FILE_ARGUMENT)) {
                outputFileName = commandLine.getOptionValue(OUTPUT_FILE_ARGUMENT);
            }
            if (commandLine.hasOption(INSTANCE_OUTPUT_FILE_ARGUMENT)) {
                instanceOutputFileName = commandLine.getOptionValue(INSTANCE_OUTPUT_FILE_ARGUMENT);
            }
            if (commandLine.hasOption(REORDER_VARIABLES_ARGUMENT)) {
                reoderVariables = true;
            }

            if (commandLine.hasOption(PREFIX_ARGUMENT)) {
                prefix = commandLine.getOptionValue(PREFIX_ARGUMENT);
                if (!checkPrefixOK()) {
                    System.err.println("The prefix must be a binary string");
                    return;
                }
            }
            pbf = getProblemConfigurator().configureProblem(
                commandLine.getOptionProperties(PROBLEM_CHAR), ps);

            System.out.println("Seed: " + seed);

            prepareRBallExplorationAlgorithm();
            long localOptima = findLocalOptima();

            System.out.println("Local optima: " + localOptima);

            if (outputFileName != null) {
                System.out.println("Writing in file " + outputFileName);
                writeLOInFile();
                System.out.println("written");
            }

            if (instanceOutputFileName != null) {
                System.out.println("Writing instance in file " + instanceOutputFileName);
                writeInstanceInFile();
                System.out.println("written");

            }

        } catch (Exception e) {
            showOptions();
            System.err.println(e.getMessage());
        }


    }

    private void writeInstanceInFile() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(instanceOutputFileName);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             Writer writer = new OutputStreamWriter(gzos))
        {
            (new MaleoFormat()).write(pbf, writer);
        }
    }

    private void writeLOInFile() {
    	try (FileOutputStream fos = new FileOutputStream(outputFileName);
    		 GZIPOutputStream gzos = new GZIPOutputStream(fos);
    		 PrintWriter writer = new PrintWriter(gzos)) 
    	{
    		localOptima.forEach(solution->{
    			writer.println(solution.toHex()+" "+Arrays.toString(getPbf().evaluate(solution)));
    		});
    		
    		
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean  checkPrefixOK() {
        return prefix.chars().allMatch(c->(c >= '0' && c <= '1'));
    }

	protected VectorMKLandscape getPbf() {
		return pbf;
	}

	public void setPbf(VectorMKLandscape pbf) {
		this.pbf = pbf;
	}

}
