package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import com.google.gson.Gson;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunctionConfigurator;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MquboConfigurator implements VectorMKLandscapeConfigurator {
    public static final String INSTANCE_ARGUMENT = "instance";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(INSTANCE_ARGUMENT, true, "file with the instance to load");
    }

    @Override
    public VectorMKLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        Properties properties = new Properties();

        Stream.of(INSTANCE_ARGUMENT)
            .forEach(clave -> WalshBasedFunctionConfigurator.moveProperty(commandLine, properties, clave));

        return configureProblem(properties, ps);
    }

    @Override
    public VectorMKLandscape configureProblem(Properties properties, PrintStream ps) {
        Gson gson = new Gson();
        String instanceFile = properties.getProperty(INSTANCE_ARGUMENT);
        try (InputStream inputStream = new FileInputStream(instanceFile);
            Reader reader = new InputStreamReader(inputStream)) {
            MquboDto mqubo = gson.fromJson(reader, MquboDto.class);
            int n = mqubo.problem.n;
            int d = mqubo.problem.terms.length;
            WalshBasedFunction<?> [] walshFunctions = new WalshBasedFunction[d];

            for (int i=0; i < d; i++) {
                var wcs = WalshCoefficientsArray.factory().create(n);
                for (MquboDto.WalshCoefficientDto wcdto: mqubo.problem.terms[i]) {
                    Set<Integer> vars = Arrays.stream(wcdto.ids).boxed().collect(Collectors.toSet());
                    wcs.addCoefficient(vars, wcdto.w);
                }
                walshFunctions[i] = new WalshBasedFunction<>(n, wcs);
            }
            return new VectorMKLandscape(walshFunctions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
