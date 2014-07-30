package neo.landscape.theory.apps.pseudoboolean;

public interface IExperiment {

	public String getDescription();
	public String getID();
	public String getInvocationInfo();
	public void execute(String [] args);
	
}
