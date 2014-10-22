package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.Process;
import neo.landscape.theory.apps.pseudoboolean.util.Sample;

public class RBallQualityResultsTimeParser implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "quality-t";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " <file>";
	}

	@Override
	public void execute(String[] args) {

		if (args.length < 1) {
			System.out.println(getInvocationInfo());
			return;
		}

		File file = new File(args[0]);

		List<List<Sample>> traces = new ArrayList<List<Sample>>();
		List<Sample> aux = new ArrayList<Sample>();
		ExactSolutionMethod<? super NKLandscapes> es = new NKLandscapesCircularDynProg();

		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it = false;

		String[] strs;
		try {
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

			String line;

			while ((line = brd.readLine()) != null) {
				if (line.charAt(0) == 'B') {
					strs = line.split(":");
					double best = Double.parseDouble(strs[1].trim());
					if (write_it = (best != last.quality)) {
						last.quality = best;
					}
				} else if (line.charAt(0) == 'E') {
					strs = line.split(":");
					long time = Long.parseLong(strs[1].trim());

					if (write_it) {
						last.time = time;
						try {
							aux.add((Sample) last.clone());
						} catch (CloneNotSupportedException e) {
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				} else if (line.startsWith("Se")) {
					// End of record (one run analyzed)

					traces.add(aux);
					aux = new ArrayList<Sample>();

					last.quality = -Double.MAX_VALUE;
					last.time = 0;

				}
			}

			System.out.println("time, quality, samples");

			List<Sample>[] trs = traces.toArray(new List[0]);

			Sample[] past = new Sample[trs.length];
			Set<Integer> indices = new HashSet<Integer>();
			while (true) {
				// Find the next sample to process
				long min_time = Long.MAX_VALUE;
				indices.clear();
				for (int i = 0; i < trs.length; i++) {
					if (trs[i].isEmpty()) {
						continue;
					}

					Sample s = trs[i].get(0);
					if (s.time < min_time) {
						min_time = s.time;
						indices.clear();
						indices.add(i);
					} else if (s.time == min_time) {
						indices.add(i);
					}
				}

				if (indices.isEmpty()) {
					break;
				}

				for (int ind_min : indices) {
					// Get the samples with the same time from the list
					// update the Sample array
					past[ind_min] = trs[ind_min].remove(0);
				}

				// Compute the statistics using the sample array
				double qlty = 0;
				int samples = 0;
				for (Sample tmp : past) {
					if (tmp != null) {
						samples++;
						qlty += tmp.quality;
					}
				}
				qlty /= samples;
				System.out.println(min_time + ", " + qlty + ", " + samples);
				/*
				 * // Update the Sample array if this sampel is the last in its
				 * list if (trs[ind_min].isEmpty()) { past[ind_min] = null; }
				 */
			}

			System.err.println("Total runs: " + trs.length);

			brd.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
