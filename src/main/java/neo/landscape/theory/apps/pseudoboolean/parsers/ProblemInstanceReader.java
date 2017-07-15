package neo.landscape.theory.apps.pseudoboolean.parsers;

import neo.landscape.theory.apps.efficienthc.SingleobjectiveProblem;

public interface ProblemInstanceReader<P extends SingleobjectiveProblem> {

    public P readInstance(Readable readable);
    
}
