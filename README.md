# EfficientHillClimbers
This repository contains the implementation of several efficient hill climbers for pseudo-Boolean k-bounded functions. It is a maven java project. In order to build the executable JAR file open the command line and write:

mvn package

This will generate a file EfficientHillClimbers-1.0-SNAPSHOT.jar with all the dependencies included. To run the JAR file write:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar

The list of options are the different experiments available (see below).

# Multi-objective Hamming Ball Hill Climber

This branch contains the code of the hill climber published in the EvoCOP 2016 paper entitled "Efficient Hill Climber for Multi-Objective Pseudo-Boolean Optimization" by Francisco Chicano, Darrell Whitley and Renato Tinós (http://dx.doi.org/10.1007/978-3-319-30698-8_7); and in the GECCO 2016 paper entitled "Efficient Hill Climber for Constrained Pseudo-Boolean Optimization Problems" (to appear).

In order to run the Multi-Objective Hamming Ball Hill Climber of EvoCOP 2016 select the "mo-hbhc" experiment using this string as the first argument after the JAR file name:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar mo-hbhc

In order to run the Hill Climber for Constrained Problems (explained in the GECCO paper), you should select the "moc-hbhc" experiment:

java -jar EfficientHillClimbers-1.0-SNAPSHOT.jar moc-hbhc

If run without any other argument, a list of all the options will appear with an explanation of their meaning.
