package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.linkedlist.Entry;
import neo.landscape.theory.apps.util.linkedlist.EntryFactory;

public class MemoryEfficientEntryFactoryRBallPBMove implements EntryFactory<RBallPBMove> {

    @Override
    public Entry<RBallPBMove> getEntry(RBallPBMove t) {
        if (t instanceof MemoryEfficientEntryRBallPBMove) {
            return (MemoryEfficientEntryRBallPBMove)t;
        } else {
            MemoryEfficientEntryRBallPBMove move = new MemoryEfficientEntryRBallPBMove(t.improvement, t.flipVariables);
            return move;
        }
    }

}
