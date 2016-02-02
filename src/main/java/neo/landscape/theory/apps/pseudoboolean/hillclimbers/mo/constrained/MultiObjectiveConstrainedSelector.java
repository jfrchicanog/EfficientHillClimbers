package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedVectorPBMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.MultiCriteriaMovesStore;

public class MultiObjectiveConstrainedSelector extends MultiObjectiveAbstractMovesSelector<VectorPBMove> {
    
    private class BucketLocation {
        public int criterion;
        public int bucket;
    }

    private static final int [] BUCKET_NUMBER = new int [] {6, 4};
    private static final int CRITERIA = 2;
    
    private static final int [] defaultBuckets = new int [CRITERIA];

    private int minImpRadius;
    private int impBucket;
    private int radius;
    private int constraintIndex;
    private Region region;
    private CurrentSolutionQuality solutionQuality;
    private double [] ySolutionQuality;
    
    private double [] weights;
    
    private ArrayBasedMoveFactory<VectorPBMove> movesFactory;
    
    public MultiObjectiveConstrainedSelector(boolean allowRandomMoves, int theRadius, 
            int constraintIndex, Random random, 
            Map<SetOfVars, Integer> perfectHash, double [] weights, 
            CurrentSolutionQuality solQ, Region reg) {
        initializeDefaultBuckets();
        
        this.region = reg;
        solutionQuality = solQ;
        this.weights = weights.clone();
        checkWeights();
        movesFactory = new ArrayBasedVectorPBMoveFactory(CRITERIA, weights.length);
        randomMoves = allowRandomMoves;
        Map<SetOfVars, Integer> map = perfectHash;
        radius = theRadius;
        this.constraintIndex = constraintIndex; 
        movesStore = createMovesStore(radius, BUCKET_NUMBER, map, random);
    }

    protected void initializeDefaultBuckets() {
        for (int criterion = 0; criterion < CRITERIA; criterion++) {
            defaultBuckets[criterion] = getBucketForUnclassifiedClass(criterion);
        }
    }
    
    private BucketLocation getLocationOfMoveClass(MoveClass moveClass) {
        switch (region) {
        case FEASIBLE:
            return getLocationOfMoveClassInFeasibleRegion(moveClass);
        case UNFEASIBLE:
            return getLocationOfMoveClassInUnfeasibleRegion(moveClass);
        }
        throw new IllegalStateException("The region is not recognized");

    }
    
    private int getBucketForUnclassifiedClass(int criterion) {
        return BUCKET_NUMBER[criterion]-1;
    }
    
    private BucketLocation getLocationOfMoveClassInUnfeasibleRegion(MoveClass moveClass) {
        BucketLocation res = new BucketLocation();
        switch (moveClass) {
        case FEASIBLE_STRONG_IMPROVING_Y:
            res.bucket = 0;
            res.criterion = 0;
            break;
        case FEASIBLE_WIMPROVING_Y:
            res.bucket = 1;
            res.criterion = 0;
            break;
        case FEASIBLE_DISIMPROVING_Y:
            res.bucket = 2;
            res.criterion = 0;
            break;
        case UNFEASIBLE:
            res.bucket = 3;
            res.criterion = 0;
            break;
        case STRONG_IMPROVING_G:
            res.bucket = 0;
            res.criterion = 1;
            break;
        case WIMPROVING_G:
            res.bucket = 1;
            res.criterion = 1;
            break;
        case DISIMPROVING_G:
            res.bucket = 2;
            res.criterion = 1;
            break;
        default: 
            throw new IllegalStateException("Move Class "+moveClass+" should not be used while exploring the unfeasible region.");
        }
        
        return res;
    }

    private BucketLocation getLocationOfMoveClassInFeasibleRegion(MoveClass moveClass) {
        BucketLocation res = new BucketLocation();
        switch (moveClass) {
        case FEASIBLE_STRONG_IMPROVING_F:
            res.bucket = 0;
            res.criterion = 0;
            break;
        case FEASIBLE_WIMPROVING_F:
            res.bucket = 1;
            res.criterion = 0;
            break;
        case FEASIBLE_DISIMPROVING_F:
            res.bucket = 2;
            res.criterion = 0;
            break;
        case WFEASIBLE:
            res.bucket = 3;
            res.criterion = 0;
            break;
        case UNFEASIBLE:
            res.bucket = 4;
            res.criterion = 0;
            break;
        case WIMPROVING_F:
            res.bucket = 0;
            res.criterion = 1;
            break;  
        case DISIMPROVING_F:
            res.bucket = 1;
            res.criterion = 1;
            break;     
        default: 
            throw new IllegalStateException("Move Class "+moveClass+" should not be used while exploring the unfeasible region.");
        }
        return res;
    }

    private void checkWeights() {
        for (double weight: weights) {
            if (weight <= 0.0) {
                throw new RuntimeException ("All components of the weight vector must be strictly positive");
            }
        }
    }

    private MultiCriteriaMovesStore<VectorPBMove> createMovesStore(int radius, int [] buckets, Map<SetOfVars, Integer> minimalPerfectHash, Random rnd) {
        long seed = rnd.nextLong();
        return new ArrayBasedMovesStore<VectorPBMove>(movesFactory, radius, buckets, minimalPerfectHash, seed, defaultBuckets);
    }
    
    @Override
    public VectorPBMove selectMove(MoveClass moveClass) {
        BucketLocation bl = getLocationOfMoveClass(moveClass);
        minImpRadius = searchMinRadiusInBucket(bl);
        if (minImpRadius >= 1) {
            return determineMovement(bl.criterion, minImpRadius, bl.bucket);
        }
        return null;
    }

    private int searchMinRadiusInBucket(BucketLocation bl) {
        for (minImpRadius=1; minImpRadius <= radius; minImpRadius++) {
            if (!movesStore.isBucketEmpty(bl.criterion, minImpRadius, bl.bucket)) {
                return minImpRadius;
            }
        }
        return -1;
    }

    @Override
    public Iterable<VectorPBMove> allMoves() {
        return movesStore.iterableOverMoves();
    }

    @Override
    public void checkCorrectPositionOfMovesInSelector() {
        for (int theRadius = 1; theRadius <= radius; theRadius++) {
            for (int criterion=0; criterion < CRITERIA; criterion++) {
                assert movesStore.isBucketEmpty(criterion, theRadius, getBucketForUnclassifiedClass(criterion));
                for (int theBucket = 0; theBucket < movesStore.getNumberOfBuckets(criterion, theRadius); theBucket++) {
                    for (VectorPBMove move : movesStore.iterableOverBucket(criterion, theRadius, theBucket)) {
                        assert move.flipVariables.size() == theRadius;
                        BucketLocation bl = getLocationOfMoveClass(classifyMove(criterion, move));
                        assert bl.bucket==theBucket;
                        assert bl.criterion == criterion;
                    }
                }
            }
    	}
    }

    @Override
    public void unclassifyMove(VectorPBMove move) {
        for (int criterion=0; criterion < CRITERIA; criterion++) {
            movesStore.changeMoveBucketFIFO(criterion, move.flipVariables.size(), 
                movesStore.getBucketOfMove(criterion, move), 
                getBucketForUnclassifiedClass(criterion), 
                move);
        }
    }

    @Override
    public void assignBucketsToUnclassifiedMoves() {
        for (int rad=1; rad <= radius; rad++) {
            for (int criterion=0; criterion < CRITERIA; criterion++) {
                int unclassifiedBucket = getBucketForUnclassifiedClass(criterion);
                while (!movesStore.isBucketEmpty(criterion, rad, unclassifiedBucket)) {
                    VectorPBMove move = movesStore.getDeterministicMove(criterion, rad, unclassifiedBucket);
                    BucketLocation bl = getLocationOfMoveClass(classifyMove(criterion, move));
                    movesStore.changeMoveBucketFIFO(bl.criterion, rad, unclassifiedBucket, bl.bucket, move);
                }
            }
        }
    }

    public MoveClass classifyMove(int criterion, VectorPBMove move) {
        switch (region) {
        case FEASIBLE:
            return classifyMoveInFeasibleRegion(criterion, move);
        case UNFEASIBLE:
            return classifyMoveInUnfeasibleRegion(criterion, move);
        }
        throw new IllegalStateException("The region is not recognized");

    }

    private MoveClass classifyMoveInUnfeasibleRegion(int criterion, VectorPBMove move) {

        switch (criterion) {
        case 0:
            return classifyMoveInUnfeasibleRegionForCriterion0(move);
        case 1:
            return classifyMoveInUnfeasibleRegionForCriterion1(move);
        }
        throw new IllegalStateException("The criterion is not correct");
    }
    
    private MoveClass classifyMoveInFeasibleRegion(int criterion, VectorPBMove move) {

        switch (criterion) {
        case 0:
            return classifyMoveInFeasibleRegionForCriterion0(move);
        case 1:
            return classifyMoveInFeasibleRegionForCriterion1(move);
        }
        throw new IllegalStateException("The criterion is not correct");
    }

    private MoveClass classifyMoveInFeasibleRegionForCriterion1(VectorPBMove move) {
        MoveClass moveClass = classifyMoveAccordingToFImprovement(move);
        
        if (moveClass.equals(MoveClass.STRONG_IMPROVING_F)) {
            return MoveClass.WIMPROVING_F;
        } else {
            return moveClass;
        }
    }
    
    private MoveClass classifyMoveAccordingToFImprovement(VectorPBMove move) {
        boolean positive=false;
        boolean negative=false;
        double wScore=0.0;
        double [] improvement = move.getImprovement();
        
        for (int i = 0; i < constraintIndex; i++) {
            wScore += weights[i] * improvement[i];
            positive |= (improvement[i] > 0.0);
            negative |= (improvement[i] < 0.0);
        }
        
        if (positive && !negative) {
            return MoveClass.STRONG_IMPROVING_F;
        } else if (wScore > 0.0) {
            return MoveClass.WIMPROVING_F;
        } else {
            return MoveClass.DISIMPROVING_F;
        }
    }
    
    private MoveClass classifyMoveAccordingToFImprovementAndY(VectorPBMove move) {
        boolean positive=false;
        boolean negative=false;
        double wScore=0.0;
        double [] improvement = move.getImprovement();
        double [] f = solutionQuality.getCurrentSolutionQuality();
        
        for (int i = 0; i < constraintIndex; i++) {
            double val = (f[i] + improvement[i] - ySolutionQuality[i]);
            wScore += weights[i] * val;
            positive |= (val > 0.0);
            negative |= (val < 0.0);
        }
        
        if (positive && !negative) {
            return MoveClass.STRONG_IMPROVING_Y;
        } else if (wScore > 0.0) {
            return MoveClass.WIMPROVING_Y;
        } else {
            return MoveClass.DISIMPROVING_Y;
        }
    }
    
    private MoveClass classifyMoveAccordingToGImprovement(VectorPBMove move) {
        boolean positive=false;
        boolean negative=false;
        double wScore=0.0;
        double [] improvement = move.getImprovement();
        
        for (int i = constraintIndex; i < improvement.length; i++) {
            wScore += weights[i] * improvement[i];
            positive |= (improvement[i] > 0.0);
            negative |= (improvement[i] < 0.0);
        }
        
        if (positive && !negative) {
            return MoveClass.STRONG_IMPROVING_G;
        } else if (wScore > 0.0) {
            return MoveClass.WIMPROVING_G;
        } else {
            return MoveClass.DISIMPROVING_G;
        }
    }
    
    private MoveClass classifyMoveAccordingToFeasibility(VectorPBMove move) {
        double [] solQuality = solutionQuality.getCurrentSolutionQuality();
        double [] improvement = move.getImprovement();
        
        boolean nonnegative=true;
        double wScore=0.0;
        
        for (int i = constraintIndex; i < improvement.length; i++) {
            double val = improvement[i] + solQuality[i];
            wScore += weights[i] * val;
            nonnegative &= (val >= 0.0);
        }
        
        if (nonnegative) {
            return MoveClass.FEASIBLE;
        } else if (wScore >= 0.0) {
            return MoveClass.WFEASIBLE;
        } else {
            return MoveClass.UNFEASIBLE;
        }
    }

    private MoveClass classifyMoveInFeasibleRegionForCriterion0(
            VectorPBMove move) {

        MoveClass moveClass = classifyMoveAccordingToFeasibility(move);
        if (moveClass.equals(MoveClass.FEASIBLE)) {
            MoveClass secondMoveClass = classifyMoveAccordingToFImprovement(move);
            switch (secondMoveClass) {
            case STRONG_IMPROVING_F:
                return MoveClass.FEASIBLE_STRONG_IMPROVING_F;
            case WIMPROVING_F:
                return MoveClass.FEASIBLE_WIMPROVING_F;
            default:
                return MoveClass.FEASIBLE_DISIMPROVING_F;
            }
        } else {
            return moveClass;
        }
    }

    private MoveClass classifyMoveInUnfeasibleRegionForCriterion1(VectorPBMove move) {
        return classifyMoveAccordingToGImprovement(move);
    }

    private MoveClass classifyMoveInUnfeasibleRegionForCriterion0(
            VectorPBMove move) {
        
        MoveClass moveClass = classifyMoveAccordingToFeasibility(move);
        if (moveClass.equals(MoveClass.FEASIBLE)) {
            MoveClass secondMoveClass = classifyMoveAccordingToFImprovementAndY(move);
            switch (secondMoveClass) {
            case STRONG_IMPROVING_Y:
                return MoveClass.FEASIBLE_STRONG_IMPROVING_Y;
            case WIMPROVING_Y:
                return MoveClass.FEASIBLE_WIMPROVING_Y;
            default:
                return MoveClass.FEASIBLE_DISIMPROVING_Y;
            }
        } else {
            return MoveClass.UNFEASIBLE;
        }
    }

    @Override
    public void setWStar(double[] wstar) {
        assert wstar.length + constraintIndex == weights.length;
        for (int i = 0; i < wstar.length; i++) {
            weights[constraintIndex+i] = wstar[i];
        }
        
    }

    @Override
    public void setRegion(Region region) {
        if (this.region != region) {
            this.region = region;
            for (int r=1; r <= radius; r++) {
                for (int criterion = 0; criterion < CRITERIA; criterion++) {
                    movesStore.changeAllMovesToBucket(criterion, r, 
                            getBucketForUnclassifiedClass(criterion));
                }
            }
            assignBucketsToUnclassifiedMoves();
        }
    }

    @Override
    public void setYSolutionQuality(double[] yValue) {
        ySolutionQuality = yValue.clone();
    }

    @Override
    public void beginUpdatesForMove(VectorPBMove move) {
        // TODO: in the future we can do this more efficient if we analyze the kind of move
    }

    @Override
    public void endUpdatesForMove() {
        for (int r=1; r <= radius; r++) {
            movesStore.changeAllMovesToBucket(0, r, getBucketForUnclassifiedClass(0));
        }    
        assignBucketsToUnclassifiedMoves();
    }
}