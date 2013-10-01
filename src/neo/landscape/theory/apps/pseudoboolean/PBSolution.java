package neo.landscape.theory.apps.pseudoboolean;

import neo.landscape.theory.apps.efficienthc.Solution;

public class PBSolution implements Solution<KBoundedEpistasisPBF> {

	private int data [];
	private int n;
	
	public PBSolution(int n)
	{
		this.n = n;
		data = new int [n/32+1];
	}
	
	public PBSolution (PBSolution other)
	{
		this.n = other.n;
		this.data = other.data.clone();
	}
	
	public void copyFrom(PBSolution other)
	{
		if (other.n != n)
		{
			n = other.n;
			data = new int [n/32+1];;
		}
		
		for (int i = 0; i < other.data.length; i++) {
			data[i] = other.data[i];
		}
	}
	
	public int getBit(int i)
	{
		return (data[i>>>5] >>> (i&0x1f))&0x1;
	}
	
	public void setBit(int i, int v)
	{
		if (getBit(i) != (v & 0x01))
		{
			flipBit(i);
		}
	}
	
	public void flipBit(int i)
	{
		data[i>>>5] ^= (1 <<  (i & 0x1f));
	}
	
	public int getN()
	{
		return n;
	}
	
	public String toString()
	{
		String str="";
		for (int i=n-1; i >= 0; i--)
		{
			str += getBit(i);
		}
		return str;
	}
}
