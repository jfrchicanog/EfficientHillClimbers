package neo.landscape.theory.apps.pseudoboolean.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class PartitionCrossoverResultsIntoLaTeX {

    private Map<PXParameters,AveragedSample> results=new HashMap<PXParameters,AveragedSample>();
    private int minK = Integer.MAX_VALUE;
    private int maxK = Integer.MIN_VALUE;
    private int minR = Integer.MAX_VALUE;
    private int maxR = Integer.MIN_VALUE;
    private int minG = Integer.MAX_VALUE;
    private int maxG = Integer.MIN_VALUE;
    
    private PrintWriter pw;
    
    public static void main(String[] args) throws IOException{
        new PartitionCrossoverResultsIntoLaTeX().buildLaTeXTables(args[0]);

    }
    
    public void buildLaTeXTables(String fileName) throws IOException {
        readFromReader(fileName);
        writeLaTeXDocument();
    }
    
    private void writeLaTeXDocument() {
        pw = new PrintWriter(System.out);
        writeDocumentPreamble();
        for (int k=minK; k <= maxK; k++) {
            writeTableForK(k);
        }
        writeDocumentEpilogue();
        pw.close();
    }
    
    private void writeDocumentPreamble() {
        pw.println("\\documentclass{article}");
        pw.println("\\begin{document}");
    }
    
    private void writeDocumentEpilogue() {
        pw.println("\\end{document}");
    }

    private void writeTableForK(int k) {
        writeTablePreambleWithCaption ("K="+k);
        writeTableHeader();
        for (int g=minG; g <= maxG; g++) {
            writeTableRowWithG(g, k);
        }
        writeTableEqpilogue();
    }
    
    private void writeTablePreambleWithCaption(String caption) {
        pw.println("\\begin{table}[!ht]\n"
                + "\\caption{"+caption+"}\n"
                        + "\\begin{tabular}{"+columnConfiguration(maxR-minR+2)+"}\n");
        
    }
    
    
    private String columnConfiguration(int columns) {
        String configuration = "|";
        for (int i=0; i < columns; i++) {
            configuration += "l|";
        }
        return configuration;
    }

    private void writeTableHeader() {
        pw.println("\\hline");
        pw.print(" &");
        for (int r=minR; r <= maxR; r++) {
            pw.print("r="+r);
            columnSeparator(r);
        }
        pw.println("\\hline");
        
    }
    
    private void writeTableRowWithG(int g, int k) {
        pw.print("g="+g+" & ");
        for (int r=minR; r <= maxR; r++) {
            writeCell(k, r, g);
            columnSeparator(r);
               
        }
        pw.println("\\hline");
        
    }

    private void writeCell(int k, int r, int g) {
        PXParameters pxparameters = new PXParameters(k,r,g);
        AveragedSample averagedSample = results.get(pxparameters);
        if (averagedSample != null) {
            checkAveragedSample(averagedSample);
            pw.print(averagedSample.getMinTime());
            if (averagedSample.getError() > 0.0) {
                pw.println("*");
                //pw.print(" ("+averagedSample.getError()+")");
            }
        }
    }

    private void checkAveragedSample(AveragedSample averagedSample) {
        assert averagedSample.getSamples() == 30;
        
    }

    private void columnSeparator(int r) {
        if (r < maxR) {
            pw.print(" &");
        }
        else {
            pw.println("\\\\");
        }
    }


    private void writeTableEqpilogue() {
        pw.println("\\end{tabular}\n"
                + "\\end{table}");
    }

    

    public void readFromReader(String fileName) throws IOException {
        InputStream inputStream = new FileInputStream(fileName);
        Reader reader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        
        String line=null;
        while ((line=bufferedReader.readLine())!=null) {
            parseLineAndAddToMapIfNecessary(line);
        }
                
        bufferedReader.close();
    }

    private void parseLineAndAddToMapIfNecessary(String line) {
        Scanner scanner = new Scanner(line).useDelimiter(", *").useLocale(Locale.US);
        String fileName = scanner.next();
        
        if (scanner.hasNext()) {
            AveragedSample averagedSample = new AveragedSample();
            averagedSample.setMinTime(scanner.nextLong());
            averagedSample.setError(scanner.nextDouble());
            averagedSample.setSamples(scanner.nextInt());
            
            PXParameters pxparameters = readParametersFromName(fileName);
            aggregateStatistics(pxparameters);
            results.put(pxparameters, averagedSample);
        }
        
    }
    
    private void aggregateStatistics(PXParameters pxparameters) {
        if (pxparameters.getG() < minG) {
            minG = pxparameters.getG();
        }
        
        if (pxparameters.getG() > maxG) {
            maxG = pxparameters.getG();
        }
        
        if (pxparameters.getR() < minR) {
            minR = pxparameters.getR();
        }
        
        if (pxparameters.getR() > maxR) {
            maxR = pxparameters.getR();
        }
        
        if (pxparameters.getK() < minK) {
            minK = pxparameters.getK();
        }
        
        if (pxparameters.getK() > maxK) {
            maxK = pxparameters.getK();
        }
    }

    private PXParameters readParametersFromName(String fileName) {
        Scanner scanner = new Scanner(fileName).useDelimiter("-");
        int k=-1, g=-1, r=-1;
        
        while (scanner.hasNext()) {
            String token = scanner.next();
            switch (token.charAt(0)) {
            case 'k':
                k=Integer.parseInt(token.substring(1));
                break;
            case 'g':
                g=Integer.parseInt(token.substring(1));
                break;
            case 'r':
                r=Integer.parseInt(token.substring(1));
                default:
                    break;
            }
        }
        
        return new PXParameters(k,r,g);
        
    }
    

}
