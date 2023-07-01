# Local Optima Markov Chain

This branch of the repository contains the source code (in Java) of the algorithms used to experiment with Local Optima Markov Chains (LOMAs). The reference paper is:

* Francisco Chicano, Gabriela Ochoa, Lorenzo Canonne and Bilel Derbel, "Local Optima Markov Chains: A New Tool for Landscape-aware Analysis of Algorithm Dynamics", GECCO 2023,
[https://doi.org/10.1145/3583131.3590422](https://doi.org/10.1145/3583131.3590422).

The exact version of the code used in the paper is in the commit tagged with [GECCO2023](https://github.com/jfrchicanog/EfficientHillClimbers/tree/GECCO2023).

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

When you run the code with no arguments a list of possible experiments appear. Running the code with the name of one experiment provides more information about the arguments required for that experimetns. The experiments related to LOMAs are:
* `lo-markov-extraction`: it runs Algorithm 2 in the GECCO 2023 paper to compute function `b` defined in Eq. (22) and some other information. It takes as input the parameters of the NK Landscape instance (n, k, q, the model and the seed) and the radius (r) of the hill climber used (Hamming Ball Hill Climber, see the GECCO 2014 paper referenced at [https://doi.org/10.1145/2576768.2598304](https://doi.org/10.1145/2576768.2598304) for more information)
* `lo-markov-algorithm`: it runs Algorithm 3 in the GECCO 2023 paper to compute function `c` defined in Eq. (25). It takes as input the file obtained from the `lo-markov-extraction` experiment and the algorithm to use (they include ILS and DRILS, used in the GECCO 2023 paper). The algorithms available can be seen by running this experiment without arguments.
* `lo-markov-transition`: it uses Eq. (24) of the GECCO 2023 paper to compute the transition probaiblity of the Markov Chain. It requires a file with the information computed by the `lo-markov-algorithm` experiment, a perturbation operator and the perturbation strength alpha.
* `lo-markov-transition-precise`: it does the same as the one above but uses arbitrary precision fractions to do the computation exact and avoid ill-conditioning problems that appear with floating point numbers. In addition to the parameters required by the previous experiment, this one has a flag `-double` that allows to write the final result with floating point values instead of fractions (that could be quite verbose). In any case, the intermediate results are computed using arbitrary precision fractions.
* `lo-markov-components`: it computes the comunicating components of the Markov Chain to produce the results shown in Section 4.5 of the GECCO 2023 paper. It requires a file with the information provided by `lo-markov-transition` or `lo-markov-transition-precise` (in this latter case the flag `-double` should be used because this experiment can only read floating point numbers form the input file).

## Running the experiments of the GECCO 2023 paper

We provide some scripts to run the same experiments conducted in the GECCO 2023 paper. They are shell script files that are prepsred to run in a cluster managed by the Slurm Workload manager (used by the Picasso supercomputer in the University of Malaga). The scripts are:
* `1-launch-markov-extraction.sh`: It applies the `lo-markov-extraction` experiment to the instances used in the GECCO 2023 paper. As a result it generates a set of (compressed) files with extension `me.xz` that are used as input to the next script.
* `2-launch-markov-algorithm.sh`: It applies the `lo-markov-algorithm` experiment to the `me.xz` files computed by the previous script to generate (compressed) files with extension `al.xz` that will be the input for the next script.
* `3-launch-markov-transition.sh`: It applies the `lo-markov-transition` to the `al.xz` files computed by the previous script to generate (compressed) files with extension `tr.xz`. These files already contain a lot of metrics used in visualizations and tables in the GECCO 2023 paper.
* `4-launch-markov-components.sh`: It applies the `lo-markov-components` to the `tr.xz` files computed by the previous script to generate (compressed) files with extension `co.xz`. These files contains information about the communicating compnents of the Markov Chain.

We have uploaded the `tr.xz` and the `co.xz` files produced for the GECCO 2023 paper in Zenodo, at [https://doi.org/10.5281/zenodo.7851465](https://doi.org/10.5281/zenodo.7851465).

