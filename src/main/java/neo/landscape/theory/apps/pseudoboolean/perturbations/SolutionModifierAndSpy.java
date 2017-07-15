package neo.landscape.theory.apps.pseudoboolean.perturbations;

public interface SolutionModifierAndSpy extends SolutionModifier{
    public long getValuesForVariables(int ... variables);
}