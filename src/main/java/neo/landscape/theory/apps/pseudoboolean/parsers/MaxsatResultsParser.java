package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import neo.landscape.theory.apps.pseudoboolean.util.Sample;
import neo.landscape.theory.apps.pseudoboolean.util.StringFilter;
import neo.landscape.theory.apps.pseudoboolean.util.TabularData;
import neo.landscape.theory.apps.util.Process;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MaxsatResultsParser implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "maxsat";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID()
				+ "maxsat <html> <dir> <filter> <fraction> <als>*";
	}

	@Override
	public void execute(String[] args) {

		if (args.length < 4) {
			System.out.println(getInvocationInfo());
			return;
		}

		File html = new File(args[0]);
		File dir = new File(args[1]);
		final String filter = args[2];
		double fraction_reached = Double.parseDouble(args[3]);
		String[] algs = Arrays.copyOfRange(args, 4, args.length);

		try {
			FileInputStream fis = new FileInputStream(html);
			BufferedReader brd = new BufferedReader(new InputStreamReader(fis));

			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = brd.readLine()) != null) {
				sb.append(line + "\n");
			}

			brd.close();

			TabularData data = parseMAXSATTableResults(sb.toString(), dir,
					fraction_reached, algs);
			Map<String, Sample[]> plots = analyzeData(data, new StringFilter() {
				@Override
				public boolean accept(String str) {
					Pattern p = Pattern.compile(filter);
					Matcher m = p.matcher(str);
					return m.matches();
					// Old code
					// return !m.matches();
				}

			});

			System.out.println("algorithm, time, instances");
			for (Entry<String, Sample[]> e : plots.entrySet()) {
				for (Sample s : e.getValue()) {
					System.out.println(e.getKey() + ", " + s.time + ", "
							+ s.quality);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public TabularData parseMAXSATTableResults(String prev_algs, File dir,
			double fraction_reached, String... algs) {
		Document doc = Jsoup.parse(prev_algs);
		Elements tables = doc.select("table");
		Element table = tables.get(2);
		Elements head = table.select("thead");

		Map<String, Sample[]> results = new HashMap<String, Sample[]>();
		String[] algorithms;

		Elements headers = head.get(0).select("th");
		algorithms = new String[headers.size() - 1 + algs.length];
		int i;
		for (i = 0; i < headers.size() - 1; i++) {
			algorithms[i] = headers.get(i + 1).text();
		}

		System.arraycopy(algs, 0, algorithms, i, algs.length);

		for (Element tr : table.select("tbody").get(0).select("tr")) {
			Elements tds = tr.select("td");
			String instance = tds.get(0).text();
			Sample[] samples = new Sample[tds.size() - 1 + algs.length];
			if (results.containsKey(instance)) {
				System.out.println("Repeated instance!!: " + instance);
			} else {
				results.put(instance, samples);
			}

			for (i = 1; i < tds.size(); i++) {
				String val = tds.get(i).text();
				int ind_o = val.indexOf("O = ");
				int ind_t = val.indexOf("T = ");

				String q = val.substring(ind_o + 4, ind_t).trim();
				String t = val.substring(ind_t + 4).trim();
				int rest = t.indexOf('(');
				if (rest > 0) {
					t = t.substring(0, rest).trim();
				}

				Sample s = null;
				if (q.charAt(0) != 'N' && t.charAt(0) != 'T') {
					s = new Sample((long) (Double.parseDouble(t) * 1000),
							Integer.parseInt(q));
				}
				samples[i - 1] = s;
			}

			for (int j = 0; j < algs.length; j++) {
				File file = new File(dir, "maxsat-" + instance + "-" + algs[j]
						+ ".gz");
				samples[i - 1] = analyzeFile(file, fraction_reached);
				i++;
			}
		}

		return new TabularData(results, algorithms);
	}

	private Sample analyzeFile(File file, double fraction_reached) {

		if (!file.exists()) {
			return null;
		}

		List<List<Sample>> traces = new ArrayList<List<Sample>>();
		List<Sample> aux = new ArrayList<Sample>();
		int m = -1;
		int c = -1;

		// Take clauses from name of file
		String[] file_name = file.getName().split("\\.");
		if (file_name[0].startsWith("maxsat-")) {
			file_name[0] = file_name[0].substring(7);
		}

		Pattern pat = Pattern.compile("[Cc]([0-9]+)");
		Matcher mat = pat.matcher(file_name[0]);

		if (mat.find()) {
			c = Integer.parseInt(mat.group(1));
		}

		Sample last = new Sample(0, -Double.MAX_VALUE);
		boolean write_it = false;
		List<Double> quality_reached = new ArrayList<Double>();

		String[] strs;
		try {
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			BufferedReader brd = new BufferedReader(new InputStreamReader(gis));

			String line;
			boolean corrected = false;

			while ((line = brd.readLine()) != null) {
				if (line.charAt(0) == 'B') {
					strs = line.split(":");
					double best = Double.parseDouble(strs[1].trim());
					if (write_it = (best != last.quality)) {
						last.quality = best;
					}
				} else if (line.charAt(0) == 'E') {
					if (write_it) {
						strs = line.split(":");
						long time = Long.parseLong(strs[1].trim());
						last.time = time;
						try {
							aux.add((Sample) last.clone());
						} catch (CloneNotSupportedException e) {
							brd.close();
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				} else if (line.startsWith("Q")) {
					if (aux.isEmpty()) {
						strs = line.split(":");
						double quality = Double.parseDouble(strs[1].trim());
						write_it = true;
						last.quality = quality;
						corrected = true;
					}
				} else if (line.startsWith("T")) {
					if (write_it) {
						strs = line.split(":");
						long time = Long.parseLong(strs[1].trim());
						last.time = time;
						try {
							aux.add((Sample) last.clone());
						} catch (CloneNotSupportedException e) {
							brd.close();
							throw new RuntimeException(e);
						}
						write_it = false;
					}
				} else if (line.startsWith("M:")) {
					strs = line.split(":");
					m = Integer.parseInt(strs[1].trim());
					if (c < 0) {
						c = m;
					} else if (!corrected && c > m) {
						for (Sample s : aux) {
							s.quality += (c - m);
						}
					}
					// End of record (one run analyzed)
					traces.add(aux);
					aux = new ArrayList<Sample>();
					quality_reached.add(last.quality
							+ (corrected ? 0 : (c - m)));

					corrected = false;
					last.quality = -Double.MAX_VALUE;
					last.time = 0;
				}
			}

			brd.close();

			Collections.sort(quality_reached);

			double threshold = quality_reached.get((int) (quality_reached
					.size() * (1 - fraction_reached)));

			double time_sum = 0;
			int num_samples = 0;

			for (List<Sample> l : traces) {
				for (Sample s : l) {
					if (s.quality >= threshold) {
						time_sum += s.time;
						num_samples++;
						break;
					}
				}
			}

			/*
			 * if (num_samples != traces.size()) { System.err.println(
			 * "Something wrong!! The number of samples does not coincide"); }
			 */

			time_sum /= num_samples; // Compute the average time

			return new Sample((long) time_sum, c - threshold);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Sample[]> analyzeData(TabularData td,
			StringFilter instance_filter) {
		Map<String, Sample[]> result = new HashMap<String, Sample[]>();
		for (int i = 1; i < td.algorithms.length; i++) {
			List<Long> times = new ArrayList<Long>();
			String name = td.algorithms[i];
			for (Entry<String, Sample[]> e : td.results.entrySet()) {
				if (!instance_filter.accept(e.getKey())) {
					continue;
				}
				// else
				Sample[] s = e.getValue();
				if (s[i] != null && s[i].quality <= s[0].quality) {
					if (s[i].quality < s[0].quality) {
						System.err.println("Better solution found for "
								+ e.getKey());
					}
					times.add(s[i].time);
				}
			}

			if (times.isEmpty()) {
				result.put(name, new Sample[0]);
				continue;
			}
			// else

			Collections.sort(times);
			List<Sample> points = new ArrayList<Sample>();

			int j = 0;
			while (j < times.size()) {
				long current = times.get(j);
				while (j < times.size() && times.get(j) == current) {
					j++;
				}
				points.add(new Sample(current, j));
			}

			result.put(name, points.toArray(new Sample[0]));
		}

		return result;
	}

}
