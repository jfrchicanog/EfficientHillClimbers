package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This is a memory efficient implementation of a undirected graph based on arrays for the 
 * implementation of the chordal graph generated in DPX
 * @author francis
 *
 */

public class MemoryEfficientUndirectedGraph implements UndirectedGraph {
	
	private static final int SIZE_INCREMENT = 100;
	private enum GraphState {BUILD, QUERY};
	
	public static final UndirectedGraphFactory FACTORY = new UndirectedGraphFactory() {
		@Override
		public UndirectedGraph createGraph(int maxNodes) {
			return new MemoryEfficientUndirectedGraph(maxNodes, maxNodes >> 2, maxNodes >> 1);
		}
	}; 
	
	private final Iterable<Integer> nodeIterable = new Iterable<Integer>() {
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				int index=0; 
				@Override
				public boolean hasNext() {
					return index < numberOfNodes;
				}

				@Override
				public Integer next() {
					return nodes[index++];
				}
			};
		}
	};
	
	private int [] nodeIndex;
	private int [] nodes;
	private int [] edgeEntries;
	private int [] adjacencyListIndex;
	private int numberOfNodes;
	private int numberOfEdgeEntries;
	private int queryIndex;
	private GraphState state;
	
	public MemoryEfficientUndirectedGraph(int maxNodes, int initialNodeSize, int initialEdgeSize) {
		nodeIndex = new int [maxNodes];
		nodes = new int [initialNodeSize];
		adjacencyListIndex = new int [initialNodeSize];
		edgeEntries = new int [initialEdgeSize+initialEdgeSize/2];
		
		clearGraph();
	}

	@Override
	public void clearGraph() {
		numberOfNodes = 0;
		numberOfEdgeEntries=0;
		state = GraphState.BUILD;
	}
	
	private boolean isNodeInGraph(int node) {
		int index = nodeIndex[node];
		return (index < numberOfNodes && nodes[index]==node);
	}

	@Override
	public void addNodeToGraph(int node) {
		checkBuildState();
		if (!isNodeInGraph(node)) {
			ensureCapacity();
			nodes[numberOfNodes] = node;
			nodeIndex[node]=numberOfNodes;
			adjacencyListIndex[numberOfNodes]=numberOfEdgeEntries;
			edgeEntries[numberOfEdgeEntries] = 0;
			
			numberOfEdgeEntries++;
			numberOfNodes++;
		}
	}

	private void checkBuildState() {
		if (state != GraphState.BUILD) {
			throw new IllegalStateException("Adding node to graph while not in BUILD state");
		}
	}

	private void ensureCapacity() {
		if (numberOfNodes >= nodes.length) {
			nodes = expandArray(nodes);
			adjacencyListIndex = expandArray(adjacencyListIndex);
		}
		ensureCapacityForEdgeEntries();
	}

	private void ensureCapacityForEdgeEntries() {
		if (numberOfEdgeEntries >= edgeEntries.length) {
			edgeEntries = expandArray(edgeEntries);
		}
	}
	
	private int [] expandArray(int [] array) {
		int newSize;
		if (array.length <= 1) {
			newSize=2;
		} else {
			newSize = array.length+(array.length >> 1);
		}
		return Arrays.copyOf(array, newSize);
	}
	
	@Override
	public void addEdgeToGraph(int from, int to) {
		addNodeToGraph(from);
		addNodeToGraph(to);
		if (nodes[numberOfNodes-1] != to) {
			throw new IllegalArgumentException("Edges pointing to the same node should be added consecutively");
		}
		
		ensureCapacityForEdgeEntries();
		
		edgeEntries[numberOfEdgeEntries-1] = from;
		edgeEntries[numberOfEdgeEntries++]=0;
		adjacencyListIndex[numberOfNodes-1]++;
		
		int fromIndex = nodeIndex[from];
		edgeEntries[adjacencyListIndex[fromIndex]]++;
		
	}
	
	@Override
	public Iterable<Integer> getAdjacent(Integer vertex) {
		if (state == GraphState.BUILD) {
			prepareAdjacencyList();
		}
		state = GraphState.QUERY;
		return new Iterable<Integer>() {
			final int index = nodeIndex[vertex];
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					int neighborIndex=(index==0)?queryIndex:adjacencyListIndex[index-1]+1;
					@Override
					public boolean hasNext() {
						return neighborIndex <= adjacencyListIndex[index];
					}
					@Override
					public Integer next() {
						return edgeEntries[neighborIndex++];
					}
				};
			}
		};
	}
	
	private void prepareAdjacencyList() {
		// Check array size
		int requiredSize = ((numberOfEdgeEntries-numberOfNodes) << 1) + numberOfEdgeEntries;
		if (edgeEntries.length < requiredSize) {
			edgeEntries = Arrays.copyOf(edgeEntries, requiredSize);
		}
		// Set initial position
		queryIndex=numberOfEdgeEntries;
		
		int edgeEntriesIndex=queryIndex-1;
		int oldEdgeEntriesIndex=0;
		for (int index = 0; index < numberOfNodes; index++) {
			int to = nodes[index];
			// Copy the in-edges
			for (;oldEdgeEntriesIndex < adjacencyListIndex[index]; oldEdgeEntriesIndex++) {
				int from = edgeEntries[oldEdgeEntriesIndex];
				edgeEntries[++edgeEntriesIndex] = from;
				
				int indexFrom = nodeIndex[from];
				assert indexFrom < index;
				edgeEntries[++adjacencyListIndex[indexFrom]] = to;
			}
			adjacencyListIndex[index] = edgeEntriesIndex;
			// Reserve space for out-edges
			edgeEntriesIndex += edgeEntries[oldEdgeEntriesIndex++];
		}
	}

	@Override
	public Iterable<Integer> getNodes() {
		return nodeIterable;
	}
}