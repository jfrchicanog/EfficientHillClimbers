package neo.landscape.theory.apps.pseudoboolean.px;

public interface VariableProcedence {
    
    public final static int RED=0x01;
    public final static int BLUE=0x02;
    public final static int PURPLE=0x03;
    public final static int COLOR_BIT_LENGTH = 2;

    public void markAsBlue(int variable);
    public void markAsPurple(int variable);
    public void markAsRed(int variable);
    public int getColor(int variable);

}