package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SumOfEmbeddedLandscapes extends EmbeddedLandscape {
	private EmbeddedLandscape [] nkLandscapes;
	
	public SumOfEmbeddedLandscapes(EmbeddedLandscape... landscapes) {
		this.nkLandscapes = landscapes;
		initializaInternalData();
	}

	private void initializaInternalData() {
		this.n = Stream.of(nkLandscapes).mapToInt(EmbeddedLandscape::getN).sum();
		this.m = Stream.of(nkLandscapes).mapToInt(EmbeddedLandscape::getM).sum();
		masks = new int[m][];
		int subfunctionCount=0;
		int variableCount=0;
		for (int l=0; l < nkLandscapes.length;l++) {
			for (int sf = 0; sf < nkLandscapes[l].getM(); sf++) {
				masks[subfunctionCount] = nkLandscapes[l].masks[sf].clone();
				for (int i=0; i < masks[subfunctionCount].length; i++) {
					masks[subfunctionCount][i] += variableCount;
				}
				subfunctionCount++;
			}
			variableCount += nkLandscapes[l].getN();
		}
	}

	@Override
	public void setConfiguration(Properties prop) {
	}

	@Override
	public double evaluateSubfunction(int sf, PBSolution pbs) {
		int l=0;
		while (sf >= nkLandscapes[l].getM()) {
			sf -= nkLandscapes[l].getM();
			l++;
		}
		return nkLandscapes[l].evaluateSubfunction(sf, pbs);
	}

	@Override
	public double evaluateSubfunction(int sf, int value) {
		int l=0;
		while (sf >= nkLandscapes[l].getM()) {
			sf -= nkLandscapes[l].getM();
			l++;
		}
		return nkLandscapes[l].evaluateSubfunction(sf, value);
	}

    public void writeTo(Writer wr) {
        for (EmbeddedLandscape els : this.nkLandscapes) {
            if (els instanceof NKLandscapes) {
                ((NKLandscapes) els).writeTo(wr);
            }
            if (els instanceof MAXSAT) {
                ((MAXSAT) els).writeTo(wr);
            }
        }
    }

    public List<Integer> getNValues() {
        return Stream.of(nkLandscapes).mapToInt(EmbeddedLandscape::getN).boxed().collect(Collectors.toList());
    }

}
