package neo.landscape.theory.apps.util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;

public class PBSolutionDigest {
    private ByteBuffer temporaryStoreForSolution;
    private IntBuffer intViewOfSolutionBuffer;
    private MessageDigest digest;
    private HexBinaryAdapter hexBinaryAdapter;
    
    public PBSolutionDigest(int size) {
        try {
            temporaryStoreForSolution = ByteBuffer.allocate(size/8+4);
            intViewOfSolutionBuffer = temporaryStoreForSolution.asIntBuffer();
            digest = MessageDigest.getInstance("SHA-1");
            hexBinaryAdapter = new HexBinaryAdapter();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException (e);
        }
    }
    
    public String getHashOfSolution(RBallEfficientHillClimberSnapshot solution) {
        intViewOfSolutionBuffer.clear();
        intViewOfSolutionBuffer.put(solution.getSolution().getData());
        String hash = hexBinaryAdapter.marshal(digest.digest(temporaryStoreForSolution.array()));
        return hash.toLowerCase();
    }
}
