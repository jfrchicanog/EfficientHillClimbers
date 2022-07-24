package neo.landscape.theory.apps.pseudoboolean;

import java.util.Arrays;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.problems.PseudoBooleanFunction;

public class PBSolution implements Solution<PseudoBooleanFunction> {

	private int data[];
	private int n;

	public PBSolution(int n) {
		this.n = n;
		data = new int[n / 32 + 1];
	}

	public PBSolution(PBSolution other) {
		this.n = other.n;
		this.data = other.data.clone();
	}

	public void copyFrom(PBSolution other) {
		if (other.n != n) {
			n = other.n;
			data = new int[n / 32 + 1];
			;
		}

		for (int i = 0; i < other.data.length; i++) {
			data[i] = other.data[i];
		}
	}

	public int getBit(int i) {
		return (data[i >>> 5] >>> (i & 0x1f)) & 0x1;
	}

	public void setBit(int i, int v) {
		if (getBit(i) != (v & 0x01)) {
			flipBit(i);
		}
	}

	public void flipBit(int i) {
		data[i >>> 5] ^= (1 << (i & 0x1f));
	}

	public int getN() {
		return n;
	}

	/**
	 * The string prints the binary string in Big Endian order.
	 */
	public String toString() {
		String str = "";
		for (int i = n - 1; i >= 0; i--) {
			str += getBit(i);
		}
		return str;
	}

	public static PBSolution toPBSolution(String str) {
		PBSolution sol = new PBSolution(str.length());
		sol.parse(str);
		return sol;
	}
	
	public String toHex() {
	    String str = "";
	    for (int i=data.length-1; i >= 0; i--) {
	        str += String.format("%08x", data[i]);
	    }
	    return str;
	}
	
	public void fromHex(String hex) {
		hex = hex.trim();
		if (hex.length() <= (data.length-1)*8) {
			throw new IllegalArgumentException("Hex number nos long enough: "+hex);
		}
		
		for (int i=data.length-1; i >= 0; i--) {
			int endIndex = hex.length() - i*8;
			String val = hex.subSequence(Math.max(endIndex-8, 0), endIndex).toString();
			data[i] = parseHexInt(val);
		}
	}
	
	private int parseHexInt(String hex) {
		assert hex.length() <= 8;
		int result=0; 
		if (hex.length()==8) {
			result = Integer.parseInt(""+hex.charAt(0), 16);
			result <<= 28;
			hex = hex.substring(1);
		}
		result += Integer.parseInt(hex, 16);
		return result; 
	}

	/**
	 * This method assumes that the binary string is in Big Endian order.
	 * 
	 * @param string
	 */
	public void parse(String string) {
		if (string.length() != n) {
			throw new IllegalArgumentException(
					"This is not a string of length " + n);
		}

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < '0' || c > '1') {
				throw new IllegalArgumentException(
						"This is not a binary string. Found '" + c
								+ "' at position " + i);
			}
			// else
			setBit(n - 1 - i, c - '0');
		}

	}

	public int[] getData() {
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + n;
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
		PBSolution other = (PBSolution) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (n != other.n)
			return false;
		return true;
	}

}
