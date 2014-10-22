package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.pseudoboolean.Process;

public class RBallQualityResultsParser implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "quality";
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

		List<Long> samples = new ArrayList<Long>();
		List<Double> sum_quality = new ArrayList<Double>();
		List<Long> sum_time = new ArrayList<Long>();
		long moves = 0;
		double improvements = 0;
		int run = 0;
		int move = 0;
		String[] strs;

		try {
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

			String line;

			while ((line = brd.readLine()) != null) {
				if (line.charAt(0) == 'B') {
					strs = line.split(":");
					double best = Double.parseDouble(strs[1].trim());
					if (move < sum_quality.size()) {
						sum_quality.set(move, sum_quality.get(move) + best);
					} else {
						sum_quality.add(move, best);
					}

				} else if (line.charAt(0) == 'E') {
					strs = line.split(":");
					long time = Long.parseLong(strs[1].trim());

					if (move < sum_time.size()) {
						sum_time.set(move, sum_time.get(move) + time);
					} else {
						sum_time.add(move, time);
					}

					if (move < samples.size()) {
						samples.set(move, samples.get(move) + 1);
					} else {
						samples.add(move, 1L);
					}

					move++;
				} else if (line.charAt(0) == 'N') {
					// End of record (one run analyzed)
					run++;
					move = 0;
				}
			}

			System.out.println("time, quality, samples");

			long total_s = 0;
			for (int i = 0; i < samples.size(); i++) {
				double qty = sum_quality.get(i);
				double time = sum_time.get(i);
				long s = samples.get(i);

				System.out.println(time / s + ", " + qty / s + ", " + s);

				total_s += s;
			}

			System.err.println("Avg. moves per descent: " + ((double) moves)
					/ total_s);
			System.err.println("Avg. improvements per descent: " + improvements
					/ total_s);
			System.err.println("Total runs: " + run);

			brd.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
