package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CPBasedHyperplaneElimination {

    private static class Hyperplane {
        private int id;
        private int [] assignment;
        private final Set<Move> movesWhereValid;
        private final Set<Hyperplane> conflictingHyperplanes;

        public Hyperplane() {
            this.id = -1;
            this.assignment = null;
            this.movesWhereValid = new HashSet<>();
            this.conflictingHyperplanes = new HashSet<>();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Hyperplane that)) return false;
            if (id >= 0 && that.id >= 0) {
                return id == that.id;
            }
            return Objects.deepEquals(assignment, that.assignment);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(assignment);
        }
    }

    private static class Move {
        private final int id;
        private final int [] variablesFlipped;
        private final Set<Hyperplane> validHyperplanes;

        private Move(int i) {
            this.id = i;
            this.variablesFlipped = new int[]{i};
            this.validHyperplanes = new HashSet<>();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Move move)) return false;
            if (id >= 0 && move.id >= 0) {
                return id == move.id;
            }
            return Objects.deepEquals(variablesFlipped, move.variablesFlipped);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(variablesFlipped);
        }
    }

    private EmbeddedLandscape pbf;

    private Move [] moves;
    private Hyperplane [] hyperplanes;

    // Moves management during search
    private boolean markedMoves [];
    private int numberOfMarkedMoves;
    private Stack<Set<Integer>> markedMovesStack;

    // Used hyperplanes management during search
    private Stack<Hyperplane> hyperplanesStack;

    // Forbidden hyperplanes management during search
    private boolean [] forbiddenHyperplanes;
    private Stack<Set<Integer>> forbiddenHyperplanesStack;

    private Set<Move> remainingMoves;

    private Set<PBSolution> paretoLocalOptima;

    public CPBasedHyperplaneElimination(EmbeddedLandscape pbf) {
        this.pbf = pbf;
    }

    public Set<PBSolution> findParetoLocalOptima() {
        if (paretoLocalOptima == null) {
            initializeDataStrcutures();
            findParetoLocalOptimaRecursive();
        }
        return paretoLocalOptima;
    }

    private void initializeDataStrcutures() {
        computeMoves();
        computeValidHyperplanesForMoves();
        computeConflictingHyperplanes();

        markedMoves = new boolean[moves.length];
        numberOfMarkedMoves = 0;
        markedMovesStack = new Stack<>();

        hyperplanesStack = new Stack<>();

        forbiddenHyperplanes = new boolean[hyperplanes.length];
        forbiddenHyperplanesStack = new Stack<>();

        remainingMoves = Arrays.stream(moves).collect(Collectors.toSet());
        paretoLocalOptima = new HashSet<>();
    }

    private void computeMoves() {
        // We consider here only radius 1 Hamming ball (bit flip neighborhood)
        moves = IntStream.range(0, pbf.getN())
            .mapToObj(Move::new)
            .toArray(Move[]::new);
    }

    private void computeValidHyperplanesForMoves() {
        Map<Hyperplane, Hyperplane> validHyperplanes = new HashMap<>();
        for (int i = 0; i < moves.length; i++) {
            Move move = moves[i];
            var hyperplanes = computeValidHyperplanesForMove(move);
            for (Hyperplane h: hyperplanes) {
                if (!validHyperplanes.containsKey(h)) {
                    validHyperplanes.put(h, h);
                } else {
                    h = validHyperplanes.get(h);
                }
                move.validHyperplanes.add(h);
                h.movesWhereValid.add(move);
            }
        }

        hyperplanes = new Hyperplane[validHyperplanes.size()];
        int i=0;
        for (Hyperplane h: validHyperplanes.keySet()) {
            h.id = i;
            hyperplanes[i] = h;
            i++;
        }
    }

    private List<Hyperplane> computeValidHyperplanesForMove(Move move) {
        Set<Integer> subfunctions = new HashSet<>();
        int [][] appearsIn = pbf.getAppearsIn();
        for (int varIndex=0; varIndex < move.variablesFlipped.length; varIndex++) {
            int variable = move.variablesFlipped[varIndex];
            for (int sfIndex=0; sfIndex < appearsIn[variable].length; sfIndex++) {
                subfunctions.add(appearsIn[variable][sfIndex]);
            }
        }

        Set<Integer> variables = new HashSet<>();
        for (Integer sf: subfunctions) {
            for (int varIndex=0; varIndex < pbf.getMaskLength(sf); varIndex++) {
                variables.add(pbf.getMasks(sf, varIndex));
            }
        }

        List<Integer> sortedVariables = variables.stream().sorted().toList();
        int limit = 1 << sortedVariables.size();
        List<Hyperplane> result = new ArrayList<>();
        PBSolution sol = new PBSolution(pbf.getN());
        for (int varCombination=0; varCombination < limit; varCombination++) {
            // Compute the original value
            for (int varIndex=0; varIndex < sortedVariables.size(); varIndex++) {
                int variable = sortedVariables.get(varIndex);
                int value = (varCombination >>> varIndex) & 1;
                sol.setBit(variable, value);
            }

            double originalValue = evaluateSolutionInSubfunctions(sol, subfunctions);

            // Compute neighbor
            for (int varIndex=0; varIndex < move.variablesFlipped.length; varIndex++) {
                sol.flipBit(move.variablesFlipped[varIndex]);
            }

            double delta = evaluateSolutionInSubfunctions(sol, subfunctions) - originalValue;

            if (delta <= 0) { // FIXME: if delta=0 we can add a hyperplane with one less assigned variable
                Hyperplane hp = new Hyperplane();
                hp.assignment = new int[sortedVariables.size()];
                for (int varIndex=0; varIndex < sortedVariables.size(); varIndex++) {
                    int variable = sortedVariables.get(varIndex);
                    int value = (varCombination >>> varIndex) & 1;
                    hp.assignment[varIndex] = (variable << 1) | value;
                }
                result.add(hp);
            }
        }

        return result;
    }

    private double evaluateSolutionInSubfunctions(PBSolution sol, Set<Integer> subfunctions) {
        double originalValue = 0.0;
        for (Integer sf: subfunctions) {
             originalValue += pbf.evaluateSubFunctionFromCompleteSolution(sf, sol);
        }
        return originalValue;
    }

    private void computeConflictingHyperplanes() {
        for (int i=0; i < hyperplanes.length-1; i++) {
            for (int j=i+1; j < hyperplanes.length; j++) {
                if (hyperplanesAreConflicting(hyperplanes[i], hyperplanes[j])) {
                    hyperplanes[i].conflictingHyperplanes.add(hyperplanes[j]);
                    hyperplanes[j].conflictingHyperplanes.add(hyperplanes[i]);
                }
            }
        }
    }

    private boolean hyperplanesAreConflicting(Hyperplane hyperplane, Hyperplane otherHyperplane) {
        Map<Integer, Boolean> assignment = new HashMap<>();
        for (int i=0; i < hyperplane.assignment.length; i++) {
            int val = hyperplane.assignment[i];
            int variable = val >>> 1;
            boolean value = (val & 1) == 1;
            assignment.put(variable, value);
        }

        for (int i=0; i < otherHyperplane.assignment.length; i++) {
            int val = otherHyperplane.assignment[i];
            int variable = val >>> 1;
            boolean value = (val & 1) == 1;
            if (assignment.containsKey(variable) && assignment.get(variable) != value) {
                return true;
            }
        }

        return false;
    }

    private void findParetoLocalOptimaRecursive() {
        if (allMovesAreMarked()) {
            reportParetoLocalOptimum();
        } else {
            Move move = getBestMoveFromRemainingMovesMin();
            for (Hyperplane hyperplane: move.validHyperplanes) {
                if (!forbiddenHyperplanes[hyperplane.id]) {
                    hyperplanesStack.push(hyperplane);
                    markAllMovesCovered(hyperplane);
                    if (forbidAllConflictingHyperplanesOfHyperplane(hyperplane)) {
                        findParetoLocalOptimaRecursive();
                    }
                    unforbidAllConflictingHyperplanesOfHyperplane(hyperplane);
                    unmarkAllMovesCovered();
                    hyperplanesStack.pop();
                }
            }
        }
    }

    private void unforbidAllConflictingHyperplanesOfHyperplane(Hyperplane hyperplane) {
        Set<Integer> forbidden = forbiddenHyperplanesStack.pop();
        for (Integer hyperplaneId: forbidden) {
            forbiddenHyperplanes[hyperplaneId] = false;
        }
    }

    private boolean forbidAllConflictingHyperplanesOfHyperplane(Hyperplane hyperplane) {
        Set<Integer> forbidden = new HashSet<>();
        boolean result = true;
        for (Hyperplane conflictingHyperplane: hyperplane.conflictingHyperplanes) {
            if (!forbiddenHyperplanes[conflictingHyperplane.id]) {
                forbidden.add(conflictingHyperplane.id);
                forbiddenHyperplanes[conflictingHyperplane.id] = true;
                if (result) {
                    for (Move otherMove : conflictingHyperplane.movesWhereValid) {
                        if (!markedMoves[otherMove.id]) {
                            // FIXME: this can be optimized
                            result &= otherMove.validHyperplanes.stream().anyMatch(h -> !forbiddenHyperplanes[h.id]);
                        }
                    }
                }
            }
        }
        forbiddenHyperplanesStack.push(forbidden);
        return result;
    }

    private void markAllMovesCovered(Hyperplane hyperplane) {
        Set<Integer> marked = new HashSet<>();
        for (Move move: hyperplane.movesWhereValid) {
            if (!markedMoves[move.id]) {
                marked.add(move.id);
                markedMoves[move.id] = true;
                remainingMoves.remove(move);
                numberOfMarkedMoves++;
            }
        }
        markedMovesStack.push(marked);
    }

    private void unmarkAllMovesCovered() {
        Set<Integer> marked = markedMovesStack.pop();
        for (Integer moveId: marked) {
            markedMoves[moveId] = false;
            remainingMoves.add(moves[moveId]);
            numberOfMarkedMoves--;
        }
    }

    private void reportParetoLocalOptimum() {
        Hyperplane hyperplane = combineHyperplanesStack();
        paretoLocalOptima.addAll(getSolutionsFromHyperplane(hyperplane));
    }

    private Hyperplane combineHyperplanesStack() {
        Map<Integer, Boolean> assignment = new HashMap<>();
        for (Hyperplane hyperplane: hyperplanesStack) {
            for (int i=0; i < hyperplane.assignment.length; i++) {
                int val = hyperplane.assignment[i];
                int variable = val >>> 1;
                boolean value = (val & 1) == 1;
                if (assignment.containsKey(variable) && assignment.get(variable) != value) {
                    throw new RuntimeException("Inconsistent hyperplanes in the stack");
                }
                assignment.put(variable, value);
            }
        }
        Hyperplane result = new Hyperplane();
        result.assignment = new int[assignment.size()];
        int index=0;
        for (var entry: assignment.entrySet()) {
            result.assignment[index++] = (entry.getKey() << 1) | (entry.getValue()?1:0);
        }
        return result;
    }

    // FIXME: this can be more efficient
    private List<PBSolution> getSolutionsFromHyperplane(Hyperplane hyperplane) {
        int remaining = pbf.getN() - hyperplane.assignment.length;
        if (remaining > 63) {
            throw new RuntimeException("Too many remaining variables to generate solutions");
        }
        PBSolution template = new PBSolution(pbf.getN());
        List<Integer> remainingVariables = IntStream.range(0, pbf.getN()).boxed().collect(Collectors.toList());
        for (int i=0; i < hyperplane.assignment.length; i++) {
            int val = hyperplane.assignment[i];
            int variable = val >>> 1;
            int value = (val & 1);
            template.setBit(variable, value);
            remainingVariables.remove((Object)variable);
        }

        long limit = 1L << remaining;
        List<PBSolution> result = new ArrayList<>();
        for (long i=0; i < limit; i++) {
            PBSolution sol = new PBSolution(template);
            for (int varIndex=0; varIndex < remaining; varIndex++) {
                int variable = remainingVariables.get(varIndex);
                int value = (int)((i >>> varIndex) & 1);
                sol.setBit(variable, value);
            }
            result.add(sol);
        }
        return result;
    }

    private boolean allMovesAreMarked() {
        return numberOfMarkedMoves == markedMoves.length;
    }

    private Move getBestMoveFromRemainingMovesMin() {
        int min = Integer.MAX_VALUE;
        Move selected = null;
        for (Move move: remainingMoves) {
            int live = liveHyperPlanesForMove(move);
            if (live < min) {
                min = live;
                selected = move;
            }
        }
        return selected;
    }

    private Move getBestMoveFromRemainingMovesMax() {
        int max = Integer.MIN_VALUE;
        Move selected = null;
        for (Move move: remainingMoves) {
            int live = liveHyperPlanesForMove(move);
            if (live > max) {
                max = live;
                selected = move;
            }
        }
        return selected;
    }

    private Move getBestMoveFromRemainingMoves() {
        return remainingMoves.stream().findFirst().orElse(null);
    }

    private int liveHyperPlanesForMove(Move move) {
        int count = 0;
        for (Hyperplane hyperplane: move.validHyperplanes) {
            if (!forbiddenHyperplanes[hyperplane.id]) {
                count++;
            }
        }
        return count;
    }


}
