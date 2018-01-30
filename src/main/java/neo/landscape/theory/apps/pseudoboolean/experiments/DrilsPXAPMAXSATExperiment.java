package neo.landscape.theory.apps.pseudoboolean.experiments;

public class DrilsPXAPMAXSATExperiment extends DrilsPXAPNKLandscapesExperiment {
    @Override
    public String getID() {
        return "drils+pxap-maxsat";
    }

    @Override
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new MAXSATConfigurator();
    }
}
