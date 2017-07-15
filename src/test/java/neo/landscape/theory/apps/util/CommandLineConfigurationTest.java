package neo.landscape.theory.apps.util;

import org.junit.Assert;
import org.junit.Test;

public class CommandLineConfigurationTest {

    @Test
    public void test() {
        CommandLineConfiguration clc = new CommandLineConfiguration("-n 4 -m 4 5 -moves 6".split(" "));
        Assert.assertEquals("4", clc.getProperty("n"));
        Assert.assertEquals("4 5", clc.getProperty("m"));
        Assert.assertEquals("6", clc.getProperty("moves"));
    }

}
