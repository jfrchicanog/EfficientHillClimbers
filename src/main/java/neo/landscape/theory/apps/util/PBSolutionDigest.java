package neo.landscape.theory.apps.util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;

public class PBSolutionDigest {
	public final static int MIN_SIZE = 160;
	
    private ByteBuffer temporaryStoreForSolution;
    private IntBuffer intViewOfSolutionBuffer;
    private MessageDigest digest;
    private HexBinaryAdapter hexBinaryAdapter;
    
    public PBSolutionDigest(int size) {
        try {
            temporaryStoreForSolution = ByteBuffer.allocate(size/8+4);
            intViewOfSolutionBuffer = temporaryStoreForSolution.asIntBuffer();
            if (size > MIN_SIZE) {
            	digest = MessageDigest.getInstance("SHA-1");
            }
            hexBinaryAdapter = new HexBinaryAdapter();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException (e);
        }
    }
    
    public String getHashOfSolution(RBallEfficientHillClimberSnapshot solution) {
        intViewOfSolutionBuffer.clear();
        intViewOfSolutionBuffer.put(solution.getSolution().getData());
        byte[] data = temporaryStoreForSolution.array();
        if (digest != null) {
        	data = digest.digest(data);
        }
		String hash = hexBinaryAdapter.marshal(data);
        return hash.toLowerCase();
    }
}
