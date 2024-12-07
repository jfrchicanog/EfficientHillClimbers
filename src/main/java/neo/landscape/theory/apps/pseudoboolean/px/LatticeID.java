package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.Objects;

public class LatticeID {
    public PBSolution minimumSolution;
    public PBSolution mask;

    public LatticeID(PBSolution minimum, PBSolution mask) {
        this.minimumSolution = minimum;
        this.mask = mask;
    }

    public String toString() {
        return String.format("%s|%s", minimumSolution.toHex(), mask.toHex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimumSolution, mask);
    }

    public boolean equals(Object o) {
        if (o instanceof LatticeID) {
            LatticeID other = (LatticeID) o;
            return minimumSolution.equals(other.minimumSolution) && mask.equals(other.mask);
        }
        return false;
    }
}
