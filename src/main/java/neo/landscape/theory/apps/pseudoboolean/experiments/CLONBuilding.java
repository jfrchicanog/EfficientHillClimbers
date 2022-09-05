package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.UnionFindLong;

public class CLONBuilding implements Process {

	protected EmbeddedLandscape pbf;
	protected int r;
	protected long seed;

	protected long counterValue;
	private String clusterFileName;
	private List<String> inputFileNames;
	private UnionFindLong clusters;
	private long cnodeId;
	private String cnodeIdFile;
	
	
	private long [] histogram;
	private int [] clusterId;
	private int [] inverseClusterId;
	
	

	private long processedLocalOptima =0;

	@Override
	public String getDescription() {
		return "This process computes the compressed the CLON based on the clusters and the LON files";
	}

	@Override
	public String getID() {
		return "clon-building";
	}

	private long readNumberOfLocalOptima() {
		try (InputStream is = new FileInputStream(clusterFileName);
				GZIPInputStream gzip = new GZIPInputStream(is);
				JsonParser parser = Json.createParser(gzip);) {
			parser.next(); // Start object
			parser.next(); // keyname: clusters
			parser.next(); // number of clusters
			parser.next(); // keyname mapping
			parser.next(); // start array

			long lo = 0;
			while (parser.next() == JsonParser.Event.START_ARRAY) {
				lo++;
				parser.next();
				parser.next();
				parser.next();
			}
			return lo;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	private void loadClusterFile() {
		long los = readNumberOfLocalOptima();
		clusters = UnionFindLong.basicImplementation(los);
		for (int i=0; i < los; i++) {
			clusters.makeSet(i);
		}
		List<Long> clusterIds = new ArrayList<>();
		try (InputStream is = new FileInputStream(clusterFileName);
				GZIPInputStream gzip = new GZIPInputStream(is);
				JsonParser parser = Json.createParser(gzip);) {
			parser.next(); // Start object
			parser.next(); // keyname: clusters
			parser.next(); // number of clusters
			long nbOfClusters = parser.getLong();
			parser.next(); // keyname mapping
			parser.next(); // start array
			
			clusterId = new int [(int)nbOfClusters];
			int clusterIdIndex = 0;

			int maxClusterId = Integer.MIN_VALUE;
			while (parser.next() == JsonParser.Event.START_ARRAY) {
				parser.next(); // first index
				long index = parser.getLong();
				parser.next(); // clusterid 
				long clusterId = parser.getLong();
				clusters.union(index, clusterId);
				if (index == clusterId) {
					clusterIds.add(clusterId);
					if (clusterId > maxClusterId) {
						maxClusterId = (int)clusterId;
						this.clusterId[clusterIdIndex++] = (int)clusterId;
					}
				}
				if (parser.next() != JsonParser.Event.END_ARRAY) {
					throw new IllegalStateException("Unexpected token");
				}
			}
			if (clusterIds.size() != nbOfClusters) {
				throw new IllegalStateException("The number of clusters does not fit");
			}
			
			histogram = new long [(int)nbOfClusters];
			inverseClusterId = new int [maxClusterId+1];
			for (int i=0; i < nbOfClusters; i++) {
				inverseClusterId[clusterId[i]] = i;
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void clonBuilding() {
		loadClusterFile();
		clusterLON();
		writeOutput();
	}

	private void clusterLON() {
		inputFileNames.stream()
		.forEach(this::processFile);
	}

	private void writeOutput() {
		try (OutputStream file = new FileOutputStream(cnodeIdFile); 
			 OutputStream out = new GZIPOutputStream(file);
			 PrintWriter pw = new PrintWriter(out);) {

			pw.println("Start\tEnd\tFrequency");
			for (int i=0; i < clusterId.length; i++) {
				if (histogram[i] >  0) {
					pw.println(cnodeId+"\t"+clusterId[i]+"\t"+histogram[i]);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private void processFile(String inputFile) {

		try {
			System.out.println("Processing file: "+inputFile);
			InputStream is = new FileInputStream(inputFile);
			GZIPInputStream gzip = new GZIPInputStream (is);
			JsonParser parser = Json.createParser(gzip);
			parser.next();
			parser.getObjectStream()
			.onClose(()->parser.close())
			.forEach(e->processLocalOptima(e.getKey(), e.getValue()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void processLocalOptima(String lo, JsonValue histogramJson) {
		processedLocalOptima++;
		int solutionIndex = Integer.parseInt(lo);
		long clusterId=clusters.findSet(solutionIndex);
		if (this.cnodeId == clusterId) {
			getHistogramStream(histogramJson.asJsonObject())
			.forEach(entry->{
				long neighboringSolutionIndex = entry.getKey();
				long neighboringCluster = clusters.findSet(neighboringSolutionIndex);
				int clusterIndex = inverseClusterId[(int)neighboringCluster];
				histogram[clusterIndex] += entry.getValue(); 
			});
		}

		if ((processedLocalOptima % 10000) == 0) {
			System.out.println("Processed "+ processedLocalOptima+ " local optima in the file");
		}
	}

	private Stream<Map.Entry<Long, Long>> getHistogramStream(JsonObject object) {
		return object.entrySet().stream()
				.map(entry->new AbstractMap.SimpleEntry<Long,Long>(
						Long.parseLong(entry.getKey()),
						((JsonNumber)entry.getValue()).longValue())
						);
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " <cnodeid> <cnodeid-file.json.gz> <cluster-file.json.gz> <lon-file.json.gz>+";
	}

	public void execute(String[] args) {
		if (args.length < 1) {
			System.out.println(getInvocationInfo());
			return;
		}

		cnodeId = Long.parseLong(args[0]);
		cnodeIdFile = args[1];
		clusterFileName = args[2];

		int i=3;
		inputFileNames = new ArrayList<>();
		while (i < args.length) {
			inputFileNames.add(args[i++]);
		}

		clonBuilding();

	}
}
