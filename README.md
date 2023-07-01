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

## Running experiments

If you are running the code from the project folder, you can open a console and write:
```
java -jar target/EfficientHillClimbers-0.22.0-SNAPSHOT.jar
```

If you plan to move the code to different folder or machine, you should copy the `EfficientHillClimbers-0.22.0-SNAPSHOT-all.tar.gz` file and decompress it. A new folder will appear containing the `EfficientHillClimbers-0.22.0-SNAPSHOT.jar` file and its dependencies. Then, you can run the code again as:
```
java -jar {path to EfficientHillClimbers-0.22.0-SNAPSHOT.jar}
```

When you run the code with no arguments a list of possible experiments appear. The ones relevant for LOMA experimentation are:
* `lo-markov-extraction`: it runs Algorithm 2 in the GECCO 2023 paper to compute function `b`. It takes as input the parameters of the NK Landscape instance (n, k, q, the model and the seed).
* `lo-markov-algorithm`:
* `lo-markov-transition`:
* `lo-markov-transition-precise`:
* `lo-markov-components`:

## Running the experiments of the GECCO 2023 paper

We provide some scripts to run the same experiments conducted in the GECCO 2023 paper. They are shell script files that are prepsred to run in a cluster managed by the Slurm Workload manager (used by the Picasso supercomputer in the University of Malaga). The scripts are:
* `1-launch
