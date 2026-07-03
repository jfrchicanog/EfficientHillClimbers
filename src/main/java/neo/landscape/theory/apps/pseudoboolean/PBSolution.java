package neo.landscape.theory.apps.pseudoboolean;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.problems.PseudoBooleanFunction;

import java.io.Serializable;
import java.util.Arrays;

public class PBSolution implements Solution<PseudoBooleanFunction>, Serializable {

    public static enum BitsOrder {BIG_ENDIAN, LITTLE_ENDIAN}

    ;

    private int data[];
    private int n;

    /**
     * Creates a new solution with n bits, where all the bits are 0.
     *
     * @param n
     */
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
        // Zero-allocation, native memory block copy
        System.arraycopy(other.data, 0, this.data, 0, other.data.length);
    }

    public int getBit(int i) {
        return (data[i >>> 5] >>> (i & 0x1f)) & 0x1;
    }

    public void setBit(int i, int v) {
        // Direct bitwise manipulation to avoid unnecessary method calls in hot loops
        if ((v & 0x01) == 1) {
            data[i >>> 5] |= (1 << (i & 0x1f));
        } else {
            data[i >>> 5] &= ~(1 << (i & 0x1f));
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
        for (int i = 0; i < n; i++) {
            solution.setBit(i, value & 0x01);
            value >>>= 1;
        }
        return solution;
    }

    public static PBSolution readFromHex(int n, String hex) {
        PBSolution solution = new PBSolution(n);
        solution.fromHex(hex);
        return solution;
    }

    public int hammingDistance(PBSolution other) {
        if (other.n != n) {
            throw new IllegalArgumentException("The size of the two solutions is not the same");
        }
        int hamming = 0;
        for (int i = 0; i < data.length; i++) {
            // Hardware-level POPCNT instruction. Replaces the slow inner for-loop.
            hamming += Integer.bitCount(data[i] ^ other.data[i]);
        }
        return hamming;
    }

    public PBSolution xor(PBSolution solution) {
        if (solution.n != n) {
            throw new IllegalArgumentException("The binary strings have different lengths");
        }

        PBSolution result = new PBSolution(n);
        for (int i = 0; i < data.length; i++) {
            result.data[i] = data[i] ^ solution.data[i];
        }
        return result;
    }

    public PBSolution and(PBSolution solution) {
        if (solution.n != n) {
            throw new IllegalArgumentException("The binary strings have different lengths");
        }

        PBSolution result = new PBSolution(n);
        for (int i = 0; i < data.length; i++) {
            result.data[i] = data[i] & solution.data[i];
        }
        return result;
    }

    public PBSolution flipAllVariables() {
        PBSolution result = new PBSolution(n);
        for (int i = 0; i < data.length; i++) {
            result.data[i] = data[i] ^ 0xffffffff;
        }
        int finalMask = 0xffffffff ^ (0xffffffff << (n % 32));
        result.data[data.length - 1] &= finalMask;

        return result;
    }

    public int getN() {
        return n;
    }

    /**
     * The string prints the binary string in Big Endian order.
     */
    @Override
    public String toString() {
        // O(N) StringBuilder replaces O(N^2) String concatenations (critical memory fix)
        StringBuilder str = new StringBuilder(n);
        for (int i = n - 1; i >= 0; i--) {
            str.append(getBit(i));
        }
        return str.toString();
    }

    public String printReversed() {
        StringBuilder str = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            str.append(getBit(i));
        }
        return str.toString();
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

    public String toHex() {
        StringBuilder str = new StringBuilder(data.length * 8);
        for (int i = data.length - 1; i >= 0; i--) {
            str.append(String.format("%08x", data[i]));
        }
        return str.toString();
    }

    public void fromHex(String hex) {
        hex = hex.trim();
        if (hex.length() <= (data.length - 1) * 8) {
            throw new IllegalArgumentException("Hex number not long enough: " + hex);
        }

        for (int i = data.length - 1; i >= 0; i--) {
            int endIndex = hex.length() - i * 8;
            String val = hex.subSequence(Math.max(endIndex - 8, 0), endIndex).toString();
            data[i] = parseHexInt(val);
        }
    }

    private int parseHexInt(String hex) {
        assert hex.length() <= 8;
        int result = 0;
        if (hex.length() == 8) {
            result = Integer.parseInt("" + hex.charAt(0), 16);
            result <<= 28;
            hex = hex.substring(1);
        }
        result += Integer.parseInt(hex, 16);
        return result;
    }

    /**
     * This method assumes that the binary string is in Big Endian order.
     * * @param string
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
            setBit(n - 1 - i, c - '0');
        }

    }

    /**
     * This method assumes that the binary string is in Big Endian order.
     * * @param string
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