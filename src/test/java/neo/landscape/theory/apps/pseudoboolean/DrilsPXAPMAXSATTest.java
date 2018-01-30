package neo.landscape.theory.apps.pseudoboolean;

import org.junit.Test;

public class DrilsPXAPMAXSATTest {

    @Test
    public void test() {
        Experiments.main(new String[]{"drils+pxap-maxsat","-instance",
        "/Users/francis/Documents/investigacion/experimentacion/next-descent/max-sat/MAX-SAT 2013/ms_random/max3sat/80v/s3v80c600-1.cnf",
        "-r","1",
        "-time","10",
        "-mf","0.15",
        "-aseed","0"});
    }
}
