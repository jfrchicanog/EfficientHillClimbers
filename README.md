# Optimizing One Million Variable NK Landscapes by Hybridizing Deterministic Recombination and  Local Search

You can find in this repository the source code of the algorithms implemented for the paper "Optimizing One Million Variable NK Landscapes by Hybridizing Deterministic Recombination and  Local Search", by Francisco Chicano, Darrell Whitley, Gabriela Ochoa and Renato Tinós, published in GECCO 2017 (https://doi.org/10.1145/3071178.3071285).

In order to run the algorithms you need first to compile and package them into a JAR file (see instructions below). Then, you can run them with the following commands:

* For HiReLS

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar px  <options omitted>

* For DRILS

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar rball+ils+px  <options omitted>

To see the options, just run the algorithms without options. 

# Multi-objective Hamming Ball Hill Climber


You can find here the code of the hill climber published in the EvoCOP 2016 paper entitled "Efficient Hill Climber for Multi-Objective Pseudo-Boolean Optimization" by Francisco Chicano, Darrell Whitley and Renato Tinós (http://dx.doi.org/10.1007/978-3-319-30698-8_7); and in the GECCO 2016 paper entitled "Efficient Hill Climber for Constrained Pseudo-Boolean Optimization Problems" (https://doi.org/10.1145/2908812.2908869).

In order to run the Multi-Objective Hamming Ball Hill Climber of EvoCOP 2016 select the "mo-hbhc" experiment using this string as the first argument after the JAR file name:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar mo-hbhc

In order to run the Hill Climber for Constrained Problems (explained in the GECCO paper), you should select the "moc-hbhc" experiment:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar moc-hbhc

If run without any other argument, a list of all the options will appear with an explanation of their meaning.


**Note**: After submitting the camera-ready of the EvoCOP 2016 paper, a bug was found that could affect the results. In this branch the bug is fixed. The exact version used for the experiments in the EvoCOP paper is commit: https://github.com/jfrchicanog/EfficientHillClimbers/commit/213036ef593bb25617b48dcb58e81097a3437d71 .


# Instructions to build the software

The repository contains Java code shipped as a maven project. Maven 3 is needed as prerequisite. In order to build the executable JAR file open the command line and write:

mvn package

This will generate a file EfficientHillClimbers-1.0-SNAPSHOT.jar (in the target directory) with all the dependencies included. To run the JAR file write:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar

The list of options are the different experiments available (see above).

