# Local Optima Markov Chain

This branch of the repository contains the source code (in Java) of the algorithms used to experiment with Local Optima Markov Chains (LOMAs). The reference paper is:

* Francisco Chicano, Gabriela Ochoa, Lorenzo Canonne and Bilel Derbel, "Local Optima Markov Chains: A New Tool for Landscape-aware Analysis of Algorithm Dynamics", GECCO 2023,
[https://doi.org/10.1145/3583131.3590422](https://doi.org/10.1145/3583131.3590422).

The exact version of the code used in the paper is the one tagged with [gecco2023-loma]().

The results of the experiments together with a snapshot of the source code can be found in Zenodo at [https://doi.org/10.5281/zenodo.7851465](https://doi.org/10.5281/zenodo.7851465).

## Instrunctions to build the code

This is a maven project that requires Maven (minimum version 3.6.3) and Java 1.8 (or higher). In order to build the code you just need to open a console and write
```
mvn package -DskipTests
```
As a result you will find in the target folder a file `EfficientHillClimbers-0.22.0-SNAPSHOT.jar` with the Java classes and a file `EfficientHillClimbers-0.22.0-SNAPSHOT-all.tar.gz`
with the resulting JAR and the required dependencies.

