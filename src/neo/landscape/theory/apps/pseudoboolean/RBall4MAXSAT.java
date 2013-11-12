package neo.landscape.theory.apps.pseudoboolean;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber.SubfunctionChangeListener;


public class RBall4MAXSAT extends RBallEfficientHillClimber implements SubfunctionChangeListener{

	protected Set<Integer> unsatisfied;
	
	public RBall4MAXSAT (Properties prop)
	{
		super(prop);
		unsatisfied = new HashSet<Integer>();
		setSubfunctionChangeListener(this);
	}
	
	public RBall4MAXSAT(int r) {
		this(r,null);
	}
	
	public RBall4MAXSAT(int r, double [] quality_l)
	{
		super(r,quality_l);
		unsatisfied = new HashSet<Integer>();
		setSubfunctionChangeListener(this);
	}


	@Override
	public void valueChanged(int sf, double old_value, double new_value) {
		if (new_value == 0 && old_value != 0)
		{
			unsatisfied.add(sf);
		}
		
		if (new_value==1 && old_value==0)
		{
			unsatisfied.remove(sf);
		}
		
	}
	
	@Override
	public void softRestart(int soft_restart) {
		boolean tmp = collect_flips;
		collect_flips =false;
		
		Set<Integer> vars = new HashSet<Integer>();
		
		for (int un: unsatisfied)
		{
			int k = problem.getMaskLength(un);
			for (int v=0; v < k; v++)
			{
				vars.add(problem.getMasks(un, v));
			}
		}
		
		int n = vars.size();

		for (int v: vars)
		{
			int r = rnd.nextInt(n);
			if (r < soft_restart)
			{
				sol_quality+= mos[oneFlipScores[v]].v.improvement;
				moveOneBit(v);

				soft_restart--;
			}
			n--;
		}
		
		collect_flips =tmp;
	}

}
