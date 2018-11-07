package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.HashSet;
import java.util.Set;

public class VariableClique {
	private Set<Integer> variables;
	private VariableClique parent;
	private int id;
	
	public VariableClique(int id) {
		this.id=id;
	}
	
	public VariableClique getParent() {
		return parent;
	}
	public void setParent(VariableClique parent) {
		this.parent = parent;
	}
	public Set<Integer> getVariables() {
		if (variables==null) {
			variables = new HashSet<>();
		}
		return variables;
	}
	public int getId() {
		return id;
	}
}