package neo.landscape.theory.apps.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CommandLineConfigurationTest {

    @Test
    public void test() {
        CommandLineConfiguration clc = new CommandLineConfiguration("-n 4 -m 4 5 -moves 6".split(" "));
        Assertions.assertEquals("4", clc.getProperty("n"));
        Assertions.assertEquals("4 5", clc.getProperty("m"));
        Assertions.assertEquals("6", clc.getProperty("moves"));
    }

}
