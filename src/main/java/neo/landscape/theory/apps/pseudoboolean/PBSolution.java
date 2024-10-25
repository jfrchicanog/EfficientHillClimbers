package neo.landscape.theory.apps.pseudoboolean;

import java.io.Serializable;
import java.util.Arrays;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.problems.PseudoBooleanFunction;

public class PBSolution implements Solution<PseudoBooleanFunction>, Serializable{
    
    public static enum BitsOrder {BIG_ENDIAN, LITTLE_ENDIAN};

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

	public static PBSolution readFromInt(int n, int value) {
		if (n > 32) {
			throw new IllegalArgumentException("The number of bits is greater than 32");
		}
		PBSolution solution = new PBSolution(n);
		for (int i=0; i < n; i++) {
			solution.setBit(i, value & 0x01);
			value >>>= 1;
		}
		return solution;
	}
	
	public int hammingDistance(PBSolution other) {
	    if (other.n!=n) {
	        throw new IllegalArgumentException("The size of the two solutions is not the same");
	    }
	    int hamming=0;
	    for (int i = 0; i < data.length; i++) {
            int diff = (data[i] ^ other.data[i]);
            hamming += numberOfOnes(diff);
        }
	    return hamming;
	}

	private int numberOfOnes(int diff) {
	    int ones=0;
	    for(int i=0; i < 32; i++) {
	        ones += (diff &0x1);
	        diff >>>= 1;
	    }
        return ones;
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
	
	public String printReversed() {
	    String str = "";
        for (int i = 0; i < n; i++) {
            str += getBit(i);
        }
        return str;
	}

	public static PBSolution toPBSolution(String str) {
		return toPBSolution(str, BitsOrder.BIG_ENDIAN);
	}
	
	public static PBSolution toPBSolution(String str, BitsOrder order) {
        PBSolution sol = new PBSolution(str.length());
        switch (order) {
        case BIG_ENDIAN:
            sol.parseBigEndian(str);
            break;
        case LITTLE_ENDIAN:
            sol.parseLittleEndian(str);
            break;
        default:
            throw new IllegalArgumentException();
        }
        return sol;
    }

	/**
	 * This method assumes that the binary string is in Big Endian order.
	 * 
	 * @param string
	 */
	public void parseBigEndian(String string) {
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
			setBit(n-1-i, c - '0');
		}

	}
	
	/**
     * This method assumes that the binary string is in Big Endian order.
     * 
     * @param string
     */
    public void parseLittleEndian(String string) {
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
            setBit(i, c - '0');
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
