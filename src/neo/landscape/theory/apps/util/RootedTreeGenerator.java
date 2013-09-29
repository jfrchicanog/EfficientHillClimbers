package neo.landscape.theory.apps.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This generator is based on the recursive algorithm of the Gang Li's Master Thesis (University of Calgary). It generates all the rooted
 * trees up to a given number of nodes. The representation used is the parent vector and the order in which they are generated is the relex order. 
 * @author francis
 *
 */

public class RootedTreeGenerator {

	public interface RootedTreeCallback {
		/**
		 * This method will be invoked by the generator when it has a tree. The array starts in 1 (the 0 position is not used). 
		 * @param par the parent representation of the tree
		 * @param p the number of nodes of the tree
		 */
		public void rootedTree (int [] par, int p);
	}


	private int [] par;
	private int n;
	private RootedTreeCallback rtc;
	
	
	/**
	 * The name and parameters of this method are the same as in the original Pascal code of the procedure by Gang Li.
	 * @param p
	 * @param s
	 * @param cL
	 */
	private void gen (int p, int s, int cL)
	{
		if (p > n)
		{
			return;
		}
		// else
		if (cL==0)
		{
			par[p] = p-1;
		}
		else if (par[p-cL] < s)
		{
			par[p] = par[s];
		}
		else
		{
			par[p] = cL + par[p-cL];
		}
		// We have a rooted tree of size p here (the first one)
		
		rtc.rootedTree(par.clone(), p); // inform
		gen(p+1,s,cL);
		while (par[p] > 1)
		{
			s = par[p];
			par[p] = par[s];
			// We have a rooted tree of size p here
			rtc.rootedTree(par.clone(), p); //inform
			gen (p+1,s,p-s);
		}
	}
	
	/**
	 * This method starts the generation of all the rooted trees up to size n.
	 * @param n the maximum size of the trees.
	 * @param rtc the callback object to inform of the Rooted Trees.
	 */
	public void generate(int n, RootedTreeCallback rtc)
	{
		this.rtc = rtc;
		this.n=n;
		par = new int [n+1];
		
		gen(1,0,0);
		
	}
	
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		if (args.length < 1)
		{
			System.out.println("Arguments: <max nodes>");
			return;
		}
		int n = Integer.parseInt(args[0]);
		final int [] histogram = new int [n+1];
		final List<String> subgraphs = new ArrayList<String>();
		final List<Integer> levels = new ArrayList<Integer>();
		
		final StringBuilder sb = new StringBuilder();
		
		sb.append("digraph RT"+n+"{\n");
		
		RootedTreeGenerator rtg = new RootedTreeGenerator();
		rtg.generate(n, new RootedTreeCallback() {
			int sub=1;
			@Override
			public void rootedTree(int[] par, int p) {
				histogram[p]++;
				
				String name ="cluster"+sub;
				
				subgraphs.add(name);
				levels.add(p);
				
				sb.append(genDotSubgraph(name,par, p));
				
				// search parent
				
				int i=levels.size()-1;
				if (i!=0)
				{
					while (levels.get(i) != p-1)
					{
						i--;
					}
					sb.append(subgraphs.get(i) + " -> " + name+";\n");
				}
				
				sub++;
			}
			
			private String genDotSubgraph(String name, int[] par, int p) {
				String str="subgraph "+name+"{\n";
				str += "node[style=filled,label=\"\",shape=circle];\n";
				str += "edge[dir=none];\n";
				
				str += name+"n1 [shape=doublecircle];\n";
				
				
				for (int i=2; i <= p; i++)
				{
					str += name+"n"+i +" -> " + name+"n"+par[i] + ";\n";
				}
				str += "}\n";
				return str;
			}
		});
		
		sb.append("}\n");
		
		
		System.out.println(Arrays.toString(histogram));
		
		File out = new File ("RT"+n+".dot");
		FileOutputStream fos = new FileOutputStream (out);
		PrintStream ps = new PrintStream (fos);
		
		ps.print(sb.toString());
		
		ps.close();

	}

}
