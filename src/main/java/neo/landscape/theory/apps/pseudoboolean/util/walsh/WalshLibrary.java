package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface WalshLibrary extends Library {
    WalshLibrary INSTANCE = (WalshLibrary)
        Native.load("walsh", WalshLibrary.class);

    int computeDataStructures(String filename);

    int countComponentsForMove(int move);

    Pointer getBestPartition();

    void freeDataStructures();

}
