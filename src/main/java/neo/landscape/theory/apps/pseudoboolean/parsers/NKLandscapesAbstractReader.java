package neo.landscape.theory.apps.pseudoboolean.parsers;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public abstract class NKLandscapesAbstractReader  implements ProblemInstanceReader<NKLandscapes> {

    protected static class NKLandscapesSubclass extends NKLandscapes {
        public void setN(int n) {
            this.n=n;
        }
        
        public void setM(int m) {
            this.m=m;
        }
        
        public void setK(int k) {
            this.k=k;
        }
        
        public void setQ(int q) {
            this.q=q;
        }
        
        public void setNKModel(NKModel adjacent) {
            this.nkModel = adjacent;
        }
        
        public void setSubfunctions(double [][] subFunctions) {
            this.subFunctions=subFunctions;
        }
        
        public void setMasks(int [][] masks) {
            this.masks = masks;
        }
    }

    protected NKLandscapesSubclass instance; 
    
    public NKLandscapesAbstractReader() {
        super();
    }

    @Override
    public abstract NKLandscapes readInstance(Readable readable);

    protected void prepareInstance() {
        instance = new NKLandscapesSubclass();
    }

}