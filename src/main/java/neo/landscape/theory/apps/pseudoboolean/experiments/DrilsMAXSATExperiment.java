package neo.landscape.theory.apps.pseudoboolean.experiments;

public class DrilsMAXSATExperiment extends DrilsNKLandscapesExperiment {
    @Override
    public String getID() {
        return "drils-maxsat";
    }

    @Override
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new MAXSATConfigurator();
    }

}
