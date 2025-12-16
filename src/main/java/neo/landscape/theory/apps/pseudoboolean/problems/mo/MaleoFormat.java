package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import java.io.IOException;
import java.io.Writer;

public class MaleoFormat implements VectorMKLandscapeWriter {
    @Override
    public void write(VectorMKLandscape landscape, Writer writer) {
        try {
            for (int dim = 0; dim < landscape.getDimension(); dim++) {
                if (dim > 0) {
                    writer.write("c -----> \n");
                }
                writer.write("c Function "+ (dim + 1) + " (separate in a different file)\n");
                writeFunction(landscape.getComponent(dim), writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFunction(EmbeddedLandscape component, Writer writer) throws IOException {
        writer.write("p MK "+component.getN()+"\n");
        for (int sub=0; sub < component.getM(); sub++) {
            writer.write("m");
            int k = component.getMaskLength(sub);
            for (int i=k-1; i >= 0; i--) {
                writer.write(" "+(component.getMasks(sub, i)));
            }
            writer.write('\n');
            for (int index = 0; index < (1 << k); index++) {
                double val = component.evaluateSubfunction(sub, index);
                writer.write(val+" ");
            }
            writer.write('\n');
        }
    }

}
