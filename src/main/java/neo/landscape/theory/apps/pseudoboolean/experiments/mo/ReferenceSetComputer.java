package neo.landscape.theory.apps.pseudoboolean.experiments.mo;

import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;

import java.io.File;
import java.util.Scanner;
import java.util.stream.Stream;

public class ReferenceSetComputer implements Process {
    private boolean integerFlag = false;
    private int objectives;

    @Override
    public String getDescription() {
        return "Reference set computer for multi-objective problems (assuming maximization).";
    }

    @Override
    public String getID() {
        return "refset";
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: [-i (integer flag) | ..] <file1> <file2> ... <fileN>\n";
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.out.println(getInvocationInfo());
            return;
        }

        if (args[0].equals("--")) {
            integerFlag = false;
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            args = newArgs;
        } else if (args[0].equals("-i")) {
            integerFlag = true;
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            args = newArgs;
        }

        computeReferenceSet(args);
    }

    private void computeReferenceSet(String[] args) {
        ParetoNonDominatedSet paretoNonDominatedSet = new ParetoNonDominatedSet();
        paretoNonDominatedSet.setSeparator("\t");
        paretoNonDominatedSet.setPrintSummary(false);
        objectives = -1;
        for (String fileName : args) {
            try (Scanner scanner = new Scanner(new File(fileName))) {
                boolean first_line = true;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (first_line) {
                        String [] aux = line.split("\t");
                        if (objectives < 0) {
                            objectives = aux.length;
                        }
                        first_line = false;
                    } else {
                        double [] values = Stream.of(line.split("\t"))
                                .mapToDouble(Double::parseDouble)
                                .map(d -> integerFlag?Math.round(d):d)
                                .toArray();
                        if (values.length != objectives) {
                            throw new IllegalArgumentException("Inconsistent number of objectives in file " + fileName);
                        }
                        paretoNonDominatedSet.reportSolutionToArchive(values, objectives);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println(resultHeading());
        System.out.println(paretoNonDominatedSet.printArchive());
    }

    private String resultHeading() {
        StringBuilder sb = new StringBuilder();
        for (int i=1; i <= objectives; i++) {
            sb.append("Obj").append(i);
            if (i < objectives) {
                sb.append("\t");
            }
        }
        return sb.toString();
    }
}
