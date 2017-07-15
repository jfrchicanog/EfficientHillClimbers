# README #

This README would normally document whatever steps are necessary to get your application up and running.

### How to compile the code ###

This is a Maven project. In order to compile the code you need Maven 3 installed. 
Then, go to the root directory of the project (where the pom.xml file is) and write in the command line:

```bash
mvn -DskipTests package
```

As a result of the previous command, a new JAR file should be in the target directory

### How to run the code ###

The JAR file in the directory target is a runnable JAR file with all the dependencies included.
That is, you can move it to any other directory and it will (should) work. The main class is a selector of experiments.
It can be run with the command:

```bash
java -jar target/EfficientHillClimbers-0.1-SNAPSHOT.jar
```

You will see the list of experiments implemented. Each experiment has its own additional list of parameters. 
If you want to see this additional list of parameters simply add the name of the experiment as a command 
line argument to the previous command. For example, if you are interested in the ILS (r-ball + PX) for MAXSAT experiment write:

```bash
java -jar target/EfficientHillClimbers-0.1-SNAPSHOT.jar rball+ils-gpert+px+maxsat
```

You will se something like:
```
usage: rball+ils-gpert+px+maxsat
 -aseed <arg>      random seed for the algorithm (optional)
 -hp               use hyperplane initialization (optional)
 -instance <arg>   file with the instance to load (optional)
 -m <arg>          number of clauses (optional, required if no instance
                   given)
 -maxk <arg>       max number of literals per clause (optional, required
                   if no instance given)
 -mf <arg>         proportion of variables flipped in the perturbation
 -min              is minsat (optional)
 -n <arg>          number of variables (optional, required if no instance
                   given)
 -noHillClimb      set if don't want hill climbing after perturbation
 -pseed <arg>      problem seed (optional, required if no instance given)
 -r <arg>          radius of the Hamming Ball hill climber
 -sccsize <arg>    maximum size of the SCC in the perturbation (optional,
                   default radius+1)
 -time <arg>       execution time limit (in seconds)
```

### The ILS (r-ball+PX) for MAXSAT experiment ###

You can see in the previous section the set of parameters for the ILS for MAXSAT experiment.
There are many parameters to control almost all the details of the implementation. But some of them are optional. 
If you plan to run the algorithm over an instance saved in a file (in DIMACS format) during 10 seconds you can write:

```bash
java -jar target/EfficientHillClimbers-0.1-SNAPSHOT.jar rball+ils-gpert+px+maxsat -instance <dimacs-file.cnf> -mf 0.05 -r 1 -time 10 | gzip -cd
```

In the previous example `-mf` sets the percentage of bits flipped in a perturbation of the ILS and `-r` sets the radius in the r-ball hill climber.

The pipe to `gzip -cd` is used because the output of the ILS for MAXSAT experiment is compressed in GZIP.