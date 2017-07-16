package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.Iterator;

public class ComponentAndVariableMask implements PartitionComponent, VariableProcedence {
    
    private int[] componentOfPartition;
    private int sizeOfComponent;

    public ComponentAndVariableMask(int n)  {
        componentOfPartition = new int [n];
        sizeOfComponent=0;
    }



    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.PartitionComponent#clearComponent()
     */
    @Override
    public void clearComponent() {
        sizeOfComponent = 0;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.PartitionComponent#addVarToComponent(int)
     */
    @Override
    public void addVarToComponent(int var) {
        componentOfPartition[sizeOfComponent] &= PURPLE;
        componentOfPartition[sizeOfComponent++] |= (var << COLOR_BIT_LENGTH);
    }
    
    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.PartitionComponent#iterator()
     */

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int ptr = 0;
            @Override
            public boolean hasNext() {
                return ptr < sizeOfComponent;
            }

            @Override
            public Integer next() {
                return componentOfPartition[ptr++]>>>COLOR_BIT_LENGTH;
            }
            
        };
    }
    

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.VariableProcedence#markAsBlue(int)
     */
    @Override
    public void markAsBlue(int variable) {
        componentOfPartition[variable] &= (~PURPLE);
        componentOfPartition[variable] |= BLUE;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.VariableProcedence#markAsPurple(int)
     */
    @Override
    public void markAsPurple(int variable) {
        componentOfPartition[variable] |= PURPLE;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.px.VariableProcedence#markAsRed(int)
     */
    @Override
    public void markAsRed(int variable) {
        componentOfPartition[variable] &= (~PURPLE);
        componentOfPartition[variable] |= RED;
    }



    @Override
    public int getColor(int variable) {
        return componentOfPartition[variable] & PURPLE;
    }

}