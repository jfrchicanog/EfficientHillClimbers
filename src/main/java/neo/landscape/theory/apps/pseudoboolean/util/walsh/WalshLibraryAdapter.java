package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import com.sun.jna.Pointer;

import java.io.Closeable;
import java.io.IOException;
import java.util.OptionalInt;

public class WalshLibraryAdapter implements Closeable {

    private final WalshLibrary walshLibrary;
    private final int err;
    private OptionalInt previousMove;
    private int countForMove;

    public WalshLibraryAdapter(String filename) {
        walshLibrary = WalshLibrary.INSTANCE;
        previousMove = OptionalInt.empty();
        err = walshLibrary.computeDataStructures(filename);
        checkError();
    }

    private void checkError() {
        if (err != 0) {
            throw new IllegalStateException("Library could not load the file");
        }
    }

    public int getCountForMove(int move) {
        computeForMove(move);
        return countForMove;
    }

    public int [] getPartitionForMove(int move) {
        computeForMove(move);
        Pointer p = walshLibrary.getBestPartition();
        int [] vals = p.getIntArray(0, countForMove);
        return vals.clone();
    }

    private void computeForMove(int move) {
        checkError();
        if (previousMove.isEmpty() || previousMove.getAsInt() != move) {
            countForMove = walshLibrary.countComponentsForMove(move);
            previousMove = OptionalInt.of(move);
        }
    }

    @Override
    public void close() throws IOException {
        if (err == 0) {
            walshLibrary.freeDataStructures();
        }
    }
}
