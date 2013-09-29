package neo.landscape.theory.apps.pseudoboolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

public class PBSolutionTest {

	@Test
	public void testPBSolution() {
		
		for (int n : new int [] {1,5,31,32,63,64,2000})
		{
			PBSolution pbs = new PBSolution(n);
			for (int i=0; i < n; i++)
			{
				assertEquals("Not all bits set to zer at the beginning", 0, pbs.getBit(i));
			}
		}
	}

	@Test
	public void testSetBit() {
		
		Random rnd = new Random (0);
		
		for (int n : new int [] {1,5,31,32,63,64,2000})
		{
			PBSolution pbs = new PBSolution(n);
			for (int i=0; i < n; i++)
			{
				int v = rnd.nextInt(2);
				
				pbs.setBit(i, v);
				
				assertEquals("setBit of getBit not working", v, pbs.getBit(i));
			}
		}
	}


	@Test
	public void testFlipBit() {
		
		for (int n : new int [] {1,5,31,32,63,64,2000})
		{
			PBSolution pbs = new PBSolution(n);
			for (int i=0; i < n; i++)
			{
				pbs.flipBit(i);
			}
			
			for (int i=0; i < n; i++)
			{
				assertEquals("flipBit not working", 1, pbs.getBit(i));
			}
			
		}
		
	}
	
	@Test
	public void testRandomFlipBit() {
		
		Random rnd = new Random (0);
		
		for (int n : new int [] {1,5,31,32,63,64,2000})
		{
			PBSolution pbs = new PBSolution(n);
			
			for (int i=0; i < n; i++)
			{
				int v = rnd.nextInt(2);
				pbs.setBit(i,v);
				pbs.flipBit(i);
				
				assertEquals("flipBit not working", 1-v, pbs.getBit(i));
				
			}
		}
		
	}
	

}
