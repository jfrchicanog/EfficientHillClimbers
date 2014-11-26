package neo.landscape.theory.apps.pseudoboolean.util;

public class PXParameters {
    
    private int k;
    private int r;
    private int g;
    
    public PXParameters(int k, int r, int g) {
        this.k=k;
        this.r=r;
        this.g=g;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + g;
        result = prime * result + k;
        result = prime * result + r;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PXParameters other = (PXParameters) obj;
        if (g != other.g)
            return false;
        if (k != other.k)
            return false;
        if (r != other.r)
            return false;
        return true;
    }
    public int getK() {
        return k;
    }
    public void setK(int k) {
        this.k = k;
    }
    public int getR() {
        return r;
    }
    public void setR(int r) {
        this.r = r;
    }
    public int getG() {
        return g;
    }
    public void setG(int g) {
        this.g = g;
    }
    
    

}
