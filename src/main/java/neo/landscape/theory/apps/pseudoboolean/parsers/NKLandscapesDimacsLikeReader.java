package neo.landscape.theory.apps.pseudoboolean.parsers;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Scanner;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class NKLandscapesDimacsLikeReader extends NKLandscapesAbstractReader{

	private Scanner scanner;

	public NKLandscapesDimacsLikeReader() {
		super();
	}

	@Override
	public NKLandscapes readInstance(Readable input) {
		prepareMemberVariables(input);
		parseInstance();
		scanner.close();
		return instance;
	}

	private void prepareMemberVariables(Readable input) {
		scanner = new Scanner(input);
		scanner.useDelimiter("\\s+");
		scanner.useLocale(Locale.US);
		prepareInstance();
	}

	private void parseInstance() {
		parseParameters();
		parseSubfunctions();
	}

	private void parseParameters() {
		String line = scanner.nextLine();
		while (line.startsWith("c")) {
			line = scanner.nextLine();
		}

		if (line.startsWith("p")) {
			try {
				MessageFormat msg = new MessageFormat("p NK {0,number,integer} {1,number,integer}");
				Object [] objs = msg.parse(line);
				Number n = (Number)objs[0];
				Number K = (Number)objs[1];
				instance.setN(n.intValue());
				instance.setK(K.intValue()+1);
				instance.setM(instance.getN());
				instance.setQ(-1);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("Wrong format for NK Landscape input: expeting line starting with 'p' and found: "+line);
		}

	}

	private void parseSubfunctions() {
		instance.setSubfunctions(new double [instance.getM()][1 << instance.getK()]);
		instance.setMasks(new int [instance.getM()][instance.getK()]);
		for (int subfunction=0; subfunction < instance.getM(); subfunction++) {
			parseSubfunction(subfunction);
		}
	}

	private void parseSubfunction(int subfunction) {
		String line = scanner.nextLine();
		while (line.startsWith("c")) {
			line = scanner.nextLine();
		}
		
		parseSubfunctionSignature(subfunction, line);
		
		line = scanner.nextLine();
		while (line.startsWith("c")) {
			line = scanner.nextLine();
		}

		parseSubfunctionValue(subfunction, line);
		
	}

	private void parseSubfunctionValue(int subfunction, String line) {
		try (Scanner scan = new Scanner(line)) {
			scan.useDelimiter("\\s+");
			scan.useLocale(Locale.US);
			int twoToK = 1 << instance.getK();
			for (int row=0; row < twoToK; row++) {
				instance.getSubFunctions()[subfunction][row] = scan.nextDouble();
			}
		}
	}

	private void parseSubfunctionSignature(int subfunction, String line) {
		if (!line.startsWith("m")) {
			throw new IllegalArgumentException("Expecting mask of subfunction (line starting with 'm') and found: "+line);
		}
		try (Scanner scanner= new Scanner(line.substring(1))) {
			scanner.useDelimiter("\\s+");
			scanner.useLocale(Locale.US);
			for (int variable = instance.getK()-1; variable >=0; variable--) {
				instance.getMasks()[subfunction][variable] = scanner.nextInt();
			}
		}
	}
	
}
