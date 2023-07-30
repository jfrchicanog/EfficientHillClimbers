package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.Writer;

import neo.landscape.theory.apps.efficienthc.Problem;

public interface ProblemInstanceWriter<P extends Problem> {

    public void writeInstance(P instance, Writer writer);
    
}
