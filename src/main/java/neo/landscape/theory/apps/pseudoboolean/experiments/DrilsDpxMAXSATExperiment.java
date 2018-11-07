package neo.landscape.theory.apps.pseudoboolean.experiments;

public class DrilsDpxMAXSATExperiment extends DrilsDpxExperiment {
    @Override
    public String getID() {
        return "drils+dpx-maxsat";
    }

    @Override
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new MAXSATConfigurator();
    }
}
