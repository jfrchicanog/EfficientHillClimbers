# EfficientHillClimbers
This repository contains the implementation of several efficient hill climbers for pseudo-Boolean k-bounded functions. It is a maven java project. In order to build the executable JAR file open the command line and write:

mvn package

This will generate a file EfficientHillClimbers-1.0-SNAPSHOT.jar with all the dependencies included. To run the JAR file write:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar

The list of options are the different experiments available (see below).

# Multi-objective Hamming Ball Hill Climber

This hill climber was published in the EvoCOP 2016 paper entitled "Efficient Hill Climber for Multi-Objective Pseudo-Boolean Optimization" by Francisco Chicano, Darrell Whitley and Renato Tinós (published in LNCS 9595: 88-103, http://dx.doi.org/10.1007/978-3-319-30698-8_7)

All the experiments in that paper were conducted with this software. In order to run the Multi-Objective Hamming Ball Hill Climber select the "mo-hbhc" experiment using this string as the first argument after the JAR file name:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar mo-hbhc

If run without any other argument, a list of all the options will appear with an explanation of their meaning.

Note: After submitting the camera-ready, a bug was found that fix a problem that could affect the results. In this branch the bug is fixed. The exact verions used for the experiments in the EvoCOP paper is commit: https://github.com/jfrchicanog/EfficientHillClimbers/commit/213036ef593bb25617b48dcb58e81097a3437d71 .
