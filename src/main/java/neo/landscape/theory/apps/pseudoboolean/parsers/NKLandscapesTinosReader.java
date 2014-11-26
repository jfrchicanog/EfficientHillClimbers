package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.util.Locale;
import java.util.Scanner;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class NKLandscapesTinosReader extends NKLandscapesAbstractReader{
     
    private Scanner scanner;
    
    public NKLandscapesTinosReader() {
        super();
    }
    
    @Override
    public NKLandscapes readInstance(Readable input) {
        prepareMemberVariables(input);
        parseInstance();
        return instance;
    }
    
    private void prepareMemberVariables(Readable input) {
        scanner = new Scanner(input);
        scanner.useDelimiter("\\s+");
        scanner.useLocale(Locale.US);
        prepareInstance();
    }

    private void parseInstance() {
        parseParameters();
        parseSubfunctions();
        parseConnectivityMatrix();
    }

    private void parseParameters() {
        scanner.nextLine();
        instance.setN(scanner.nextInt());
        instance.setM(instance.getN());
        instance.setK(scanner.nextInt()+1);
        instance.setQ(-1);
        scanner.nextLine();
    }
    
    private void parseSubfunctions() {
        instance.setSubfunctions(new double [instance.getM()][1 << instance.getK()]);
        scanner.nextLine();
        for (int subfunction=0; subfunction < instance.getM(); subfunction++) {
            parseSubfunctionValue(subfunction);
        }
        scanner.nextLine();
    }

    private void parseSubfunctionValue(int subfunction) {
        int twoToK = 1 << instance.getK();
        for (int row=0; row < twoToK; row++) {
            instance.getSubFunctions()[subfunction][row] = scanner.nextDouble();
        }
    }
    
    private void parseConnectivityMatrix() {
        instance.setMasks(new int [instance.getM()][instance.getK()]);
        scanner.nextLine();
        for (int subfunction=0; subfunction < instance.getM(); subfunction++) {
            parseSubfunctionSignature(subfunction);
        }
        instance.setCircular(false);
        tryToFixAdjacency();
    }

    private void parseSubfunctionSignature(int subfunction) {
        int variablesAdded = 0;
        for (int variable=0; variable < instance.getN(); variable++) {
            if (scanner.nextInt(2) == 1) {
                instance.getMasks()[subfunction][variablesAdded] = variable;
                variablesAdded++;
            }
        }
    }
    
    private boolean isAdjacentModel() {
        // This is a very simple test that is not valid for all the cases, but it is
        // for the ones we are interested now (Renato's instances)
        for (int subfunction=0; subfunction < instance.getM(); subfunction++) {
            for (int maskIndex=0; maskIndex < instance.getK(); maskIndex++) {
                if (! subfunctionPlusMaskIndexIsCongruentWithTheVariable(subfunction, maskIndex)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean subfunctionPlusMaskIndexIsCongruentWithTheVariable(int subfunction,
            int maskIndex) {
        return ((subfunction+maskIndex)%instance.getN()) == instance.getMasks(subfunction,maskIndex);
    }
    
    private void tryToFixAdjacency() {
        if (instance.isCircular()) {
            return;
        }
        
        for (int subfunction=0; subfunction < instance.getM(); subfunction++) {
            tryToFixAdjacency(subfunction);
            if (!isAdjacentSubfunction(subfunction)) {
                return;
            }
        }
        instance.setCircular(true);
    }

    private void tryToFixAdjacency(int subfunction) {
        int rotation = findRotationOfMask(instance.getMasks()[subfunction]);
        if (rotation > 0) {
            rotateVariablesForSubfunction(subfunction, rotation); 
        }
        
    }

    private boolean isAdjacentSubfunction(int subfunction) {
        return findRotationOfMask(instance.getMasks()[subfunction]) == 0;
    }
    
    private int findRotationOfMask(int[] mask) {
        int [] copyOfMask = mask.clone();
        int rotation = 0;
        while ((rotation < instance.getK()) && !isSorted(copyOfMask)) {
            rotateLeft(copyOfMask);
            rotation++;
        }
        
        if (rotation < instance.getK()) {
            return rotation;
        } else {
            return -1;
        }
    }

    private void rotateLeft(int[] mask) {
        int firstElement = mask[0];
        for (int element=1; element < mask.length; element++) {
            mask[element-1] = mask[element];
        }
        mask[mask.length-1] = firstElement;
    }

    private boolean isSorted(int[] mask) {
        int firstElement = mask[0];
        for (int element=0; element < mask.length; element++) {
            if ((element+firstElement)%instance.getN() != mask[element]) {
                return false;
            }
        }
        return true;
    }

    private void rotateVariablesForSubfunction(int subfunction, int rotation) {
        rotateMaskForSubfunction(subfunction, rotation);
        permuteSubfunctionValuesAfterRotation(subfunction, rotation);
        
    }

    private void rotateMaskForSubfunction(int subfunction, int rotation) {
        rotateLeftSeveralTimes(instance.getMasks()[subfunction], rotation);
    }

    private void rotateLeftSeveralTimes(int [] mask, int times) {
        for (int i=0; i < times; i++) {
            rotateLeft(mask);
        }
    }
    
    private void permuteSubfunctionValuesAfterRotation(int subfunction, int rotation) {
        double [] originalSubfunction = instance.getSubFunctions()[subfunction].clone();
        for (int row=0; row < originalSubfunction.length; row++) {
            int newRow = rotateRightIntegerSeveralTimes(row, instance.getK(), rotation);
            instance.getSubFunctions()[subfunction][newRow] = originalSubfunction[row];
        }
    }
    
    private int rotateRightIntegerSeveralTimes (int value, int bits, int times) {
        int result=value;
        for (int i=0; i < times; i++) {
            result = rotateRightInteger(result, bits);
        }
        return result;
    }
    
    private int rotateRightInteger(int value, int bits) {
        int result= (value >>> 1);
        if ((value & 0x01) != 0) {
            result |= 1 << (bits-1);
        }
        return result;
    }

}
