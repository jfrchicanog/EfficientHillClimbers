package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.linkedlist.Entry;

public class MemoryEfficientEntryRBallPBMove extends RBallPBMove implements Entry<RBallPBMove> {
    private Entry<RBallPBMove> prev, next;
    
    public MemoryEfficientEntryRBallPBMove(double improvement, SetOfVars variables) {
        super(improvement, variables);
    }
    
    @Override
    public Entry<RBallPBMove> getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Entry<RBallPBMove> prev) {
        this.prev=prev;
    }

    @Override
    public Entry<RBallPBMove> getNext() {
        return next;
    }

    @Override
    public void setNext(Entry<RBallPBMove> next) {
        this.next=next;
    }

    @Override
    public RBallPBMove getV() {
        return this;
    }        
}