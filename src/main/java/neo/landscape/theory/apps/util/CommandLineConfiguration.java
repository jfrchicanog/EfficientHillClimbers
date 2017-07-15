package neo.landscape.theory.apps.util;

import java.util.Properties;

public class CommandLineConfiguration extends Properties {
    
    public CommandLineConfiguration(String [] arguments) {
        parseArguments(arguments);
    }

    private void parseArguments(String[] arguments) {
        
        String key=null;
        String value=null;
        
        for (String argument: arguments) {
            if (argument.charAt(0)=='-') {
                addParameterIfNotNull(key, value);
                key = argument.substring(1);
                value = null;
            } else {
                if (value == null) {
                    value = argument;
                } else {
                    value += " "+argument;
                }
            }
        }
        
        addParameterIfNotNull(key, value);
        
    }

    private void addParameterIfNotNull(String key, String value) {
        if (key != null) {
            setProperty(key, value);
        }
    }

}
