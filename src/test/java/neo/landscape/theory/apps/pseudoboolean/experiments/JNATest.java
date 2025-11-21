package neo.landscape.theory.apps.pseudoboolean.experiments;

import com.sun.jna.Pointer;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshLibrary;
import org.junit.Test;


/** Simple example of JNA interface mapping and usage. */
public class JNATest {

    @Test
    public void testWalsh() {
        int err = WalshLibrary.INSTANCE.computeDataStructures("./src/test/resources/function3.walsh");
        if (err != 0) {
            System.out.println("Error al cargar la instancia");
            return;
        }
        long start = System.currentTimeMillis();
        for (int i=0; i <= 32767; i++) {
            int components = WalshLibrary.INSTANCE.countComponentsForMove(i);
            Pointer ptr = WalshLibrary.INSTANCE.getBestPartition();
            int [] subsets = ptr.getIntArray(0, components);
            //System.out.println("Move: "+i+" Components: " + Arrays.toString(subsets));
        }
        WalshLibrary.INSTANCE.freeDataStructures();
        long end = System.currentTimeMillis();
        System.out.println("Tiempo total: " + (end - start) + " ms");

    }

}