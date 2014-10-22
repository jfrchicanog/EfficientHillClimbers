package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilityMethods {

	public static Map<String, List<String>> optionsProcessing(String[] args) {
		Map<String, List<String>> res = new HashMap<String, List<String>>();

		String key = "";
		List<String> aux = new ArrayList<String>();
		res.put(key, aux);

		for (String s : args) {
			if (s.charAt(0) == '-') {
				key = s.substring(1);
				aux = res.get(key);
				if (aux == null) {
					aux = new ArrayList<String>();
					res.put(key, aux);
				}
			} else {
				aux.add(s);
			}
		}

		return res;
	}

	public static boolean checkOneValue(Map<String, List<String>> options,
			String key, String name) {
		if (!options.containsKey(key) || options.get(key).size() == 0) {
			System.err.println("Error: " + name + " missing");
			return false;
		} else if (options.get(key).size() > 1) {
			System.err.println("Error: Multiple " + name + ": "
					+ options.get(key));
			return false;
		}
		return true;
	}

	public static void checkQualityLimits(double[] res) {
		if (res == null)
			return;

		Arrays.sort(res);

		for (int i = 1; i < res.length; i++) {
			if (res[i] == res[i - 1]) {
				throw new IllegalArgumentException(
						"Repeated value for the quality limits: " + res[i]);
			}
		}
	}

	public static double[] parseQL(String string) {
		if (string.charAt(0) != '{'
				|| string.charAt(string.length() - 1) != '}') {
			throw new IllegalArgumentException(
					"I dont understand qualitylimits: " + string);
		}
		// else
		string = string.substring(1, string.length() - 1);
		if (string.isEmpty()) {
			return null;
		}
		// else

		String[] limits = string.split(",");
		double[] res = new double[limits.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = Double.parseDouble(limits[i]);
			if (res[i] <= 0) {
				throw new IllegalArgumentException(
						"The quality limits must be positive: " + res[i]);
			}
		}

		checkQualityLimits(res);

		return res;
	}

}
