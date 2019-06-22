package neo.landscape.theory.apps.pseudoboolean.problems;

import java.util.Properties;
import java.util.stream.Stream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

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
		int variableCount=0;
		int l=0;
		while (sf >= nkLandscapes[l].getM()) {
			sf -= nkLandscapes[l].getM();
			variableCount += nkLandscapes[l].getN();
			l++;
		}
		PBSolution subSolution = new PBSolution(nkLandscapes[l].getMaskLength(sf));
		for (int var=0; var < subSolution.getN(); var++) {
			subSolution.setBit(var, pbs.getBit(nkLandscapes[l].getMasks(sf, var)+variableCount));
		}
		
		return nkLandscapes[l].evaluateSubfunction(sf, subSolution);
	}

}
