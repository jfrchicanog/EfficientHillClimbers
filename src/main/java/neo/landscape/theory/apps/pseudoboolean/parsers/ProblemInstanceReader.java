package neo.landscape.theory.apps.pseudoboolean.parsers;

import neo.landscape.theory.apps.efficienthc.Problem;

public interface ProblemInstanceReader<P extends Problem> {

    public P readInstance(Readable readable);
    
}
