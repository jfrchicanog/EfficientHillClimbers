package neo.landscape.theory.apps.pseudoboolean.parsers;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class NKLandscapesDimacsLikeReader extends NKLandscapesAbstractReader {

    private BufferedReader reader;
    private String parameterDescription = null;

    public NKLandscapesDimacsLikeReader() {
        super();
    }

    @Override
    public NKLandscapes readInstance(Readable input) {
        prepareMemberVariables(input);
        parseInstance();
        return instance;
    }

    private void prepareMemberVariables(Readable input) {
        reader = new BufferedReader((Reader) input, 1 << 16); // 64KB buffer
        prepareInstance();
    }

    private void parseInstance() {
        try {
            parseParameters();
            parseSubfunctions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String nextRelevantLine() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("c")) {
                if (line.toLowerCase().contains("next line contains")) {
                    parameterDescription = line.toLowerCase();
                }
                continue;
            }

            return line;
        }

        throw new IllegalArgumentException("Unexpected end of file");
    }

    private void parseParameters() throws IOException {
        String line = nextRelevantLine();

        if (!line.startsWith("p")) {
            throw new IllegalArgumentException(
                    "Wrong format for NK Landscape input: expecting line starting with 'p' and found: " + line);
        }

        String[] tokens = line.split("\\s+");

        int index = 2; // skip "p NK"

        int n = Integer.parseInt(tokens[index++]);
        int k = Integer.parseInt(tokens[index++]);

        int q = -1;
        int m = n;

        if (parameterDescription != null) {
            if (parameterDescription.contains("q")) {
                q = Integer.parseInt(tokens[index++]);
            }
            if (parameterDescription.contains("m") && index < tokens.length) {
                m = Integer.parseInt(tokens[index]);
            }
        }

        instance.setN(n);
        instance.setK(k + 1);
        instance.setM(m);
        instance.setQ(q);
    }

    private void parseSubfunctions() throws IOException {
        int m = instance.getM();
        int k = instance.getK();
        int twoToK = 1 << k;

        instance.setSubfunctions(new double[m][twoToK]);
        instance.setMasks(new int[m][k]);

        for (int subfunction = 0; subfunction < m; subfunction++) {
            parseSubfunction(subfunction);
        }
    }

    private void parseSubfunction(int subfunction) throws IOException {
        String line = nextRelevantLine();
        parseSubfunctionSignature(subfunction, line);

        line = nextRelevantLine();
        parseSubfunctionValue(subfunction, line);
    }

    private void parseSubfunctionValue(int subfunction, String line) {
        String[] tokens = line.split("\\s+");

        int twoToK = 1 << instance.getK();
        double[] table = instance.getSubFunctions()[subfunction];

        for (int i = 0; i < twoToK; i++) {
            table[i] = Double.parseDouble(tokens[i]);
        }
    }

    private void parseSubfunctionSignature(int subfunction, String line) {
        if (!line.startsWith("m")) {
            throw new IllegalArgumentException(
                    "Expecting mask of subfunction (line starting with 'm') and found: " + line);
        }

        String[] tokens = line.substring(1).trim().split("\\s+");
        int[] mask = instance.getMasks()[subfunction];

        for (int i = instance.getK() - 1, j = 0; i >= 0; i--, j++) {
            mask[i] = Integer.parseInt(tokens[j]);
        }
    }
}