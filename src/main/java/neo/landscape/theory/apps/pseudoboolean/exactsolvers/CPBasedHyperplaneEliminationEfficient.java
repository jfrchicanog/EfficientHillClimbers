package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CPBasedHyperplaneEliminationEfficient {

    private static class Hyperplane {
        private int id;
        private final Set<Move> movesWhereValid;
        private int [] conflictingHyperplanesArray;
        private int conflictingHyperplanesSize;
        private long varsMask;
        private long valueMask;

        public Hyperplane(int n) {
            this.id = -1;

            this.varsMask = 0;
            this.valueMask = 0;
            this.movesWhereValid = new HashSet<>();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Hyperplane that)) return false;
            if (id >= 0 && that.id >= 0) {
                return id == that.id;
            }
            return varsMask == that.varsMask && valueMask == that.valueMask;
        }

        @Override
        public int hashCode() {
            return Objects.hash(varsMask, valueMask);

        }

        private boolean hyperplanesAreConflicting(Hyperplane otherHyperplane) {
            long vars = varsMask & otherHyperplane.varsMask;
            long values = valueMask ^ otherHyperplane.valueMask;
            return (vars & values) != 0;
        }

        private void setVariableAndValue(int variable, int value) {
            varsMask |= (1L << variable);
            if (value == 1) {
                valueMask |= (1L << variable);
            } else {
                valueMask &= ~(1L << variable);
            }
        }
    }

    private static class Move {
        private final int id;
        private final int [] variablesFlipped;
        private final Set<Hyperplane> validHyperplanes;
        private int [] validHyperplanesArray;

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

        private void prepareArray() {
            validHyperplanesArray = validHyperplanes.stream().mapToInt(h -> h.id).toArray();
        }
    }

    private class LayeredStack {
        private int [] data;
        private int dataTop;
        private int [] startOfLayer;
        private int nbLayers;

        private LayeredStack(int dataLength, int LayerLength) {
            this.data = new int [dataLength];
            this.dataTop = 0;
            this.startOfLayer = new int [LayerLength+1];
            this.nbLayers = 0;
        }

        private void addValue (int val) {
            data[dataTop++] = val;
        }

        private void commitLayer() {
            startOfLayer[++nbLayers] = dataTop;
        }

        private void pop() {
            if (nbLayers == 0) {
                throw new RuntimeException("No layer to pop");
            }
            dataTop = startOfLayer[--nbLayers];
        }

        private int lengthOfTopLayer() {
            return startOfLayer[nbLayers] - startOfLayer[nbLayers-1];
        }

        private int getValueFromTopLayer(int index) {
            if (index > lengthOfTopLayer()) {
                throw new RuntimeException("Index out of bounds for top layer");
            }
            return data[startOfLayer[nbLayers-1] + index];
        }


    }

    private EmbeddedLandscape pbf;

    private Move [] moves;
    private Hyperplane [] hyperplanes;

    // Moves management during search
    private boolean [] markedMoves;
    private int numberOfMarkedMoves;
    private LayeredStack markedMovesStack;

    // Used hyperplanes management during search
    private int [] hyperplanesStack;
    private int hyperplanesStackSize;

    // Forbidden hyperplanes management during search
    private boolean [] forbiddenHyperplanes;
    private LayeredStack forbiddenHyperplanesStack;

    private TwoStatesIntegerSet remainingMoves;

    private Set<PBSolution> paretoLocalOptima;

    public CPBasedHyperplaneEliminationEfficient(EmbeddedLandscape pbf) {
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
        markedMovesStack = new LayeredStack(moves.length, moves.length);

        hyperplanesStack = new int [moves.length];

        forbiddenHyperplanes = new boolean[hyperplanes.length];
        forbiddenHyperplanesStack = new LayeredStack(hyperplanes.length, moves.length);

        remainingMoves = new TwoStatesISArrayImpl(moves.length);
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
            h.conflictingHyperplanesArray = new int [validHyperplanes.size()];
            i++;
        }

        for (Move move: moves) {
            move.prepareArray();
        }
    }

    private List<Hyperplane> computeValidHyperplanesForMove(Move move) {
        int n = pbf.getN();
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
                Hyperplane hp = new Hyperplane(n);
                for (int varIndex=0; varIndex < sortedVariables.size(); varIndex++) {
                    int variable = sortedVariables.get(varIndex);
                    int value = (varCombination >>> varIndex) & 1;
                    hp.setVariableAndValue(variable, value);
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
        for (int i=0; i < hyperplanes.length; i++) {
            Hyperplane hi = hyperplanes[i];
            for (int j=i+1; j < hyperplanes.length; j++) {
                Hyperplane hj = hyperplanes[j];
                if (hyperplanes[i].hyperplanesAreConflicting(hyperplanes[j])) {
                    hi.conflictingHyperplanesArray[hi.conflictingHyperplanesSize++] = hj.id;
                    hj.conflictingHyperplanesArray[hj.conflictingHyperplanesSize++] = hi.id;
                }
            }
        }
    }

    private void findParetoLocalOptimaRecursive() {
        if (allMovesAreMarked()) {
            reportParetoLocalOptimum();
        } else {
            Move move = getBestMoveFromRemainingMoves();
            for (int hyperplaneId: move.validHyperplanesArray) {
                if (!forbiddenHyperplanes[hyperplaneId]) {
                    hyperplanesStack[hyperplanesStackSize++] = hyperplaneId;
                    markAllMovesCovered(hyperplanes[hyperplaneId]);
                    if (forbidAllConflictingHyperplanesOfHyperplane(hyperplanes[hyperplaneId])) {
                        findParetoLocalOptimaRecursive();
                    }
                    unforbidAllConflictingHyperplanesOfHyperplane(hyperplanes[hyperplaneId]);
                    unmarkAllMovesCovered();
                    hyperplanesStackSize--;
                }
            }
        }
    }

    private void unforbidAllConflictingHyperplanesOfHyperplane(Hyperplane hyperplane) {
        int length = forbiddenHyperplanesStack.lengthOfTopLayer();
        for (int i=0; i < length; i++) {
            int hyperplaneId = forbiddenHyperplanesStack.getValueFromTopLayer(i);
            forbiddenHyperplanes[hyperplaneId] = false;
        }
        forbiddenHyperplanesStack.pop();
    }

    private boolean forbidAllConflictingHyperplanesOfHyperplane(Hyperplane hyperplane) {
        boolean result = true;
        for (int conflictingHyperplaneIdIndex=0; conflictingHyperplaneIdIndex < hyperplane.conflictingHyperplanesSize; conflictingHyperplaneIdIndex++) {
            int conflictingHyperplaneId = hyperplane.conflictingHyperplanesArray[conflictingHyperplaneIdIndex];
            if (!forbiddenHyperplanes[conflictingHyperplaneId]) {
                forbiddenHyperplanesStack.addValue(conflictingHyperplaneId);
                forbiddenHyperplanes[conflictingHyperplaneId] = true;
                /*
                if (result) {
                    for (Move otherMove : conflictingHyperplane.movesWhereValid) {
                        if (!markedMoves[otherMove.id]) {
                            // FIXME: this can be optimized
                            result &= otherMove.validHyperplanes.stream().anyMatch(h -> !forbiddenHyperplanes[h.id]);
                        }
                    }
                }*/
            }
        }
        forbiddenHyperplanesStack.commitLayer();
        return result;
    }

    private void markAllMovesCovered(Hyperplane hyperplane) {
        for (Move move: hyperplane.movesWhereValid) {
            if (!markedMoves[move.id]) {
                markedMovesStack.addValue(move.id);
                markedMoves[move.id] = true;
                remainingMoves.explored(move.id);
                // remainingMoves.remove(move);
                numberOfMarkedMoves++;
            }
        }
        markedMovesStack.commitLayer();
    }

    private void unmarkAllMovesCovered() {
        int length = markedMovesStack.lengthOfTopLayer();
        for (int i=0; i < length; i++) {
            int moveId = markedMovesStack.getValueFromTopLayer(i);
            markedMoves[moveId] = false;
            //remainingMoves.add(moves[moveId]);
            remainingMoves.unexplored(moveId);
            numberOfMarkedMoves--;
        }
        markedMovesStack.pop();
    }

    private void reportParetoLocalOptimum() {
        Hyperplane hyperplane = combineHyperplanesStack();
        paretoLocalOptima.addAll(getSolutionsFromHyperplane(hyperplane));
    }

    private Hyperplane combineHyperplanesStack() {
        int n = pbf.getN();
        Map<Integer, Boolean> assignment = new HashMap<>();
        for (int hp=0; hp < hyperplanesStackSize; hp++) {
            Hyperplane hyperplane = hyperplanes[hyperplanesStack[hp]];
            long vars = hyperplane.varsMask;
            while (vars != 0) {
                int variable = Long.numberOfTrailingZeros(vars);
                boolean value = ((hyperplane.valueMask >>> variable) & 1L) != 0;
                if (assignment.containsKey(variable) && assignment.get(variable) != value) {
                    throw new RuntimeException("Inconsistent hyperplanes in the stack");
                }
                assignment.put(variable, value);
                vars &= ~(1L << variable);
            }
        }
        Hyperplane result = new Hyperplane(n);
        int index=0;
        for (var entry: assignment.entrySet()) {
            result.setVariableAndValue(entry.getKey(), entry.getValue()?1:0);
        }
        return result;
    }

    // FIXME: this can be more efficient
    private List<PBSolution> getSolutionsFromHyperplane(Hyperplane hyperplane) {
        int remaining = pbf.getN() - Long.bitCount(hyperplane.varsMask);
        if (remaining > 63) {
            throw new RuntimeException("Too many remaining variables to generate solutions");
        }
        PBSolution template = new PBSolution(pbf.getN());
        List<Integer> remainingVariables = IntStream.range(0, pbf.getN()).boxed().collect(Collectors.toList());
        long vars = hyperplane.varsMask;
        while (vars != 0) {
            int variable = Long.numberOfTrailingZeros(vars);
            int value = (int)((hyperplane.valueMask >>> variable) & 1L);
            template.setBit(variable, value);
            remainingVariables.remove((Object)variable);
            vars &= ~(1L << variable);
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

    /*
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
    }*/

    private Move getBestMoveFromRemainingMoves() {
        return moves[remainingMoves.getNextUnexplored()];
    }

    private int liveHyperPlanesForMove(Move move) {
        int count = 0;
        for (int hyperplaneId: move.validHyperplanesArray) {
            if (!forbiddenHyperplanes[hyperplaneId]) {
                count++;
            }
        }
        return count;
    }


}
