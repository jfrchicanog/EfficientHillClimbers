package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.IExperiment;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossover;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaExperiment implements IExperiment {
	
	private List<PBSolution> localOptima;
	private PrintWriter nodesFile;
	private PrintWriter edgesFile;
	private Set<Integer> appearedEdges;
	
	public LocalOptimaExperiment()
	{
		localOptima = new ArrayList<>();
	}
	
	@Override
	public String getDescription() {
		return "This experiment computes the All the Local Optima of the search " +
				"space using the Efficient RBall exploration algorithm";
	}

	@Override
	public String getID() {
		return "lo";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() +
				" <n> <k> <q> <circular> <r> [<seed>]";
	}
	
	private void notifyLocalOptima(RBallEfficientHillClimber rball, NKLandscapes pbf)
	{
		double imp = rball.getMovement().getImprovement();
		if (imp <= 0.0)
		{
			PBSolution lo =  new PBSolution (rball.getSolution());
			double val=pbf.evaluate(lo);
			System.out.println(wI(localOptima.size())+": "+lo + ": "+val);
			localOptima.add(lo);
			nodesFile.println(val);
		}
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 5)
		{
			System.out.println(getInvocationInfo());
			return;
		}
		
		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		int r = Integer.parseInt(args[4]);
		long seed = 0;
		if (args.length >= 6)
		{
			seed = Long.parseLong(args[5]);
		}
		else
		{
			seed = Seeds.getSeed();
		}
		
		
		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		
		if (!q.equals("-"))
		{
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}
		
		if (circular.equals("y"))
		{
			prop.setProperty(NKLandscapes.CIRCULAR_STRING,"yes");
		}

		String file_name = "nkq-"+n+"-"+k+"-"+q+"-"+circular+"-"+r+"-"+seed;
		try
		{
			nodesFile = new PrintWriter(new FileOutputStream(file_name+".nodes"));
			nodesFile.println("FITNESS");
			edgesFile = new PrintWriter(new FileOutputStream(file_name+".edges"));
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException("I cannot open the output files");
		}
		
		appearedEdges = new HashSet<>();
		
		pbf.setSeed(seed);
		pbf.setConfiguration(prop);
		
		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r);
		PBSolution pbs = pbf.getRandomSolution();

		rball.initialize(pbf, pbs);
		
		int n_int = pbf.getN();

		long init_time = System.currentTimeMillis();
	
		notifyLocalOptima(rball,pbf);
		for (int bit: new GrayCodeBitFlipIterable(n_int))
		{
			rball.moveOneBit(bit);
			notifyLocalOptima(rball,pbf);
		}
		
		long final_time = System.currentTimeMillis();
		
		// Applying PX to all the pair of LO
		PartitionCrossover px = new PartitionCrossover(pbf);
		
		PBSolution [] los = localOptima.toArray(new PBSolution [0]);
		for (int i=0; i < los.length; i++)
		{
			for (int j=i+1; j < los.length; j++)
			{
				PBSolution res = px.recombine(los[i], los[j]);
				notifyCrossover(i,j,res);
			}
		}
		
		
		int max_app=0;
		int max_interactions=0;
		for (int i=0; i < n_int; i++)
		{
			if (pbf.getAppearsIn()[i].length > max_app)
			{
				max_app = pbf.getAppearsIn()[i].length;
			}
			
			if (pbf.getInteractions()[i].length > max_interactions)
			{
				max_interactions = pbf.getInteractions()[i].length;
			}
		}
		
		System.out.println("Problem init time: "+rball.getProblemInitTime());
		System.out.println("Solution init time: "+rball.getSolutionInitTime());
		System.out.println("Move time: "+(final_time-init_time));
		System.out.println("Stored scores:"+rball.getStoredScores());
		System.out.println("Var appearance (max):"+max_app);
		System.out.println("Var interaction (max):"+max_interactions);
		
		pbf.writeTo(new OutputStreamWriter(System.out));
		nodesFile.close();
		edgesFile.close();

	}
	
	private String wI(int i)
	{
		return ""+(i+1);
	}
	
	private int edgeID(int i,int j)
	{
		return localOptima.size()*i + j;
	}
	
	private void notifyEdge(int i, int j)
	{
		int eid = edgeID(i,j);
		if (!appearedEdges.contains(eid))
		{
			appearedEdges.add(eid);
			edgesFile.println(wI(i)+" "+wI(j)+" 1");
		}
	}

	private void notifyCrossover(int i, int j, PBSolution res) {
		int index = localOptima.indexOf(res);

		if (index < 0 || (index != i && index != j))
		{
			System.out.println("R:"+localOptima.get(i)+"(" + wI(i) +
					") x "+localOptima.get(j)+"(" + wI(j) +
					") -> "+res + (index < 0?"":"(" + wI(index) +
							")"));
			
			if (index >= 0)
			{
				notifyEdge(i, index);
				notifyEdge(j, index);
			}
		}
	}

}
