package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Lattice {
    private PBSolution representative;
    private int numberOfComponents;
    private int startVariableForComponent[]; // Components should be sorted in increasing order for the first variable
    private int nextVariableForComponent[]; // Variables should be sorted in increasing order


    public Lattice(PBSolution solution, Stream<Stream<Integer>> components) {
        setComponents(solution.getN(), components);
        representative = computeRepresentatitve(solution);
    }

    public <CI extends Collection<Integer>> Lattice(PBSolution solution, Collection<CI> components) {
        this(solution, components.stream().map(s -> s.stream()));
    }

    public LatticeID computeLatticeID() {
        return new LatticeID(new PBSolution (representative), computeMask());
    }

    public boolean contains(Lattice otherLattice) {
        if (otherLattice.numberOfComponents > numberOfComponents) {
            return false;
        }
        // Check if the variables fixed in this lattice are the same in the other lattice
        PBSolution mask = computeMask().flipAllVariables();
        if (!otherLattice.getRepresentative().and(mask)
            .equals(representative.and(mask))) {
            return false;
        }
        // Check that each component in the other lattice is the same in this lattice
        for (int otherComponent=0; otherComponent < otherLattice.numberOfComponents; otherComponent++) {
            int component = 0;
            for (; component < numberOfComponents; component++) {
                if (otherLattice.startVariableForComponent[otherComponent] == startVariableForComponent[component]) {
                    break;
                }
            }
            if (component == numberOfComponents) {
                return false;
            }
            int v = startVariableForComponent[component];
            int otherV = otherLattice.startVariableForComponent[otherComponent];
            for (; v >= 0 && otherV >= 0;
                   v = nextVariableForComponent[component], otherV = otherLattice.nextVariableForComponent[otherComponent]) {
                if (v != otherV) {
                    return false;
                }
            }
            if (v >= 0 || otherV >= 0) {
                return false;
            }
        }

        // and the bits in the representative are equal
        // Check that for the components in this lattice that are not in the other lattice, the bits are "compatible"
        // The two points above can be checked if computing the representative of the representative of the other lattice
        // with respect to the components sof this lattice gives the representative of this lattice. That is:
        return computeRepresentatitve(otherLattice.getRepresentative()).equals(representative);
    }



    public boolean equals(Object o) {
        if (o instanceof Lattice) {
            Lattice other = (Lattice) o;
            return numberOfComponents == other.numberOfComponents &&
                Arrays.equals(startVariableForComponent, other.startVariableForComponent) &&
                Arrays.equals(nextVariableForComponent, other.nextVariableForComponent) &&
                representative.equals(other.representative);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(representative, numberOfComponents, Arrays.hashCode(startVariableForComponent), Arrays.hashCode(nextVariableForComponent));
    }

    public Stream<PBSolution> computeAllSolutions() {
        return IntStream.range(0, 1 << numberOfComponents)
            .mapToObj(this::computeIndexedSolutionInLattice);
    }

    private PBSolution computeIndexedSolutionInLattice(int index) {
        PBSolution solution = new PBSolution(representative);
        for (int component = 0; component < numberOfComponents; component++) {
            if ((index & (1 << component)) != 0) {
                for (int v = startVariableForComponent[component]; v >= 0; v = nextVariableForComponent[component]) {
                    solution.flipBit(v);
                }
            }
        }
        return solution;
    }

    private void setComponents(int n, Stream<Stream<Integer>> components) {
        List<List<Integer>> auxiliaryLists = components
            .map(s -> s.sorted().collect(Collectors.toList()))
            .sorted((l1, l2) -> l1.get(0) - l2.get(0))
            .collect(Collectors.toList());
        numberOfComponents = auxiliaryLists.size();
        startVariableForComponent = new int[numberOfComponents];
        nextVariableForComponent = new int[n];
        for (int component=0; component < numberOfComponents; component++) {
            List<Integer> varsInComponent = auxiliaryLists.get(component);
            startVariableForComponent[component] = varsInComponent.get(0);
            for (int i = 0; i < varsInComponent.size() - 1; i++) {
                nextVariableForComponent[varsInComponent.get(i)] = varsInComponent.get(i + 1);
            }
            nextVariableForComponent[varsInComponent.get(varsInComponent.size() - 1)] = -1;
        }
    }

    private PBSolution computeRepresentatitve(PBSolution original) {
        PBSolution representativeSolution = new PBSolution(original);
        for (int component = 0; component < numberOfComponents; component++) {
            if (representativeSolution.getBit(startVariableForComponent[component]) == 0) {
                continue;
            }
            for (int v = startVariableForComponent[component]; v >= 0; v = nextVariableForComponent[component]) {
                representativeSolution.flipBit(v);
            }
        }
        return representativeSolution;
    }

    private PBSolution computeMask() {
        PBSolution mask = new PBSolution(representative.getN());
        for (int component = 0; component < numberOfComponents; component++) {
            for (int v = startVariableForComponent[component]; v >= 0; v = nextVariableForComponent[component]) {
                mask.setBit(v, 1);
            }
        }
        return mask;
    }

    public int getNumberOfComponents() {
        return numberOfComponents;
    }

    public List<Integer> variablesInComponent(int component) {
        List<Integer> result = new ArrayList<>();
        for (int v = startVariableForComponent[component]; v >= 0; v = nextVariableForComponent[component]) {
            result.add(v);
        }
        return result;
    }

    public PBSolution getRepresentative() {
        return representative;
    }
}
