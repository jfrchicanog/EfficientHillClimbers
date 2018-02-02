package neo.landscape.theory.apps.pseudoboolean;

import org.junit.Test;

public class DrilsPXAPNKTest {

    @Test
    public void test() {
        Experiments.main(new String[]{"drils+pxap-nk","-n","20",
                "-k","3",
                "-mf","0.5",
                "-aseed","0",
                "-pseed","1",
                "-model","random",
                "-q","3",
                "-r","1",
                "-time","5",
                "-debug"});
    }
}
