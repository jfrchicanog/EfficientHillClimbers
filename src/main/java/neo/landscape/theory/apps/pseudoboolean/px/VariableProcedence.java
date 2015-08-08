package neo.landscape.theory.apps.pseudoboolean.px;

public interface VariableProcedence {

    public void markAsBlue(int variable);
    public void markAsPurple(int variable);
    public void markAsRed(int variable);
    public int getColor(int variable);

}