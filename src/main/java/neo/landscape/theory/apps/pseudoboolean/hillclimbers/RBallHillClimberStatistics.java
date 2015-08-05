package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.ArrayList;
import java.util.List;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot.ProfileData;

public class RBallHillClimberStatistics {
    private int[] movesPerDistance;
    private List<ProfileData> profile;

    public RBallHillClimberStatistics(int rad, boolean constainsKey) {
        if (constainsKey) {
            profile = new ArrayList<ProfileData>();
        }
        movesPerDistance = new int[rad + 1];
        
    }

   public void reportMovement(RBallPBMove move) {
        int r = move.flipVariables.size();
    	movesPerDistance[r]++;
    	if (profile != null) {
    		ProfileData pd;
    		if (!profile.isEmpty()) {
    			pd = profile.get(profile.size() - 1);
    		} else {
    			pd = new ProfileData(r, 0);
    			profile.add(pd);
    		}
    
    		if (pd.radius == r) {
    			pd.moves++;
    		} else {
    			pd = new ProfileData(r, 1);
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




}