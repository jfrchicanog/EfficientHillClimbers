package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.ArrayList;
import java.util.List;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallHillClimberStatistics.ProfileData;

public class RBallHillClimberStatistics {
    /* Solution info */
    public static class ProfileData {
    	public ProfileData(int radius, int moves) {
    		this.radius = radius;
    		this.moves = moves;
    	}
    
    	public int radius;
    	public int moves;
    }

    private int[] movesPerDistance;
    private List<RBallHillClimberStatistics.ProfileData> profile;
    private int[] flips;
    boolean tmpCollectFlips;
    /* Solution info */
    protected boolean collectFlips;
    /* Solution info */
    long solutionInitializationTime;
    long initTime;
    long solutionInitializationEvals;
    long solutionMoveEvals;
    long totalMoves;
    long totalSolutionInitializations;
    
    public static final String PROFILE = "profile";

    public RBallHillClimberStatistics(int rad, boolean withProfile, boolean collectFlips, int n) {
        if (withProfile) {
            profile = new ArrayList<RBallHillClimberStatistics.ProfileData>();
        }
        movesPerDistance = new int[rad + 1];
        solutionInitializationEvals = 0;
        solutionMoveEvals = 0;
        totalMoves = 0;
        totalSolutionInitializations = 0;
        
        this.collectFlips = collectFlips;
        if (collectFlips) {
        	flips = new int[n];
        }
    }

   public void reportMovement(RBallPBMove move) {
        int r = move.flipVariables.size();
    	movesPerDistance[r]++;
    	if (profile != null) {
    		RBallHillClimberStatistics.ProfileData pd;
    		if (!profile.isEmpty()) {
    			pd = profile.get(profile.size() - 1);
    		} else {
    			pd = new RBallHillClimberStatistics.ProfileData(r, 0);
    			profile.add(pd);
    		}
    
    		if (pd.radius == r) {
    			pd.moves++;
    		} else {
    			pd = new RBallHillClimberStatistics.ProfileData(r, 1);
    			profile.add(pd);
    		}
    	}
    
    }

    public List<ProfileData> getProfile() {
    	return profile;
    }

    public void resetProfile() {
    	profile = new ArrayList<ProfileData>();
    }

    public int[] getMovesPerDistance() {
    	return movesPerDistance;
    }

    public void resetMovesPerDistance() {
    	for (int i = 0; i < movesPerDistance.length; i++) {
    		movesPerDistance[i] = 0;
    	}
    }

    public int[] getFlipStat() {
    	return flips;
    }

    void increaseFlips(int bit) {
        if (collectFlips) {
        	flips[bit]++;
        }
    }

    void enableFlipsCollection() {
        collectFlips = tmpCollectFlips;
    }

    void disableFlipsCollection() {
        tmpCollectFlips = collectFlips;
    	collectFlips = false;
    }

    public long getSolutionInitTime() {
    	return solutionInitializationTime;
    }

    void stopSolutionInitTime() {
        solutionInitializationTime = System.currentTimeMillis() - initTime;
    }

    void startSolutionInitTime() {
        initTime = System.currentTimeMillis();
    }

    public long getSubfnsEvalsInSolInits() {
    	return solutionInitializationEvals;
    }

    public long getSubfnsEvalsInMoves() {
    	return solutionMoveEvals;
    }

    public long getTotalMoves() {
    	return totalMoves;
    }

    public long getTotalSolutionInits() {
    	return totalSolutionInitializations;
    }

    void increaseSolInitEvals(int val) {
        solutionInitializationEvals += val;
    }

    void increaseSolMoveEvals(int val) {
        solutionMoveEvals += val;
    }

    void increaseTotalMoves(int val) {
        totalMoves += val;
    }

    void increaseTotalSolutionInitializations(int val) {
        totalSolutionInitializations += val;
    }




}