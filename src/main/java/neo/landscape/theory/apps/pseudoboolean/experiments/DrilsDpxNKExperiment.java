package neo.landscape.theory.apps.pseudoboolean.experiments;

public class DrilsDpxNKExperiment extends DrilsDpxExperiment {
    @Override
    public String getID() {
        return "drils+dpx-nk";
    }

    @Override
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new NKLandscapeConfigurator();
    }
}
