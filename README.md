# Source code of publication submited to ECJ

This branch contains the source code of the algorithms used in the ECJ publication (under review at this moment). The publication is an extension of the EvoCOP 2019 paper, where DPX is presented. The version of the artifact used in the experiments is 0.20-DPX. A docker image contianing the artifact can be found [here](https://hub.docker.com/repository/docker/jfrchicanog/graybox). In the same link there are instructions to run the docker image.

The experiments used in the paper can be reproduced using the following commands:

* Crossover comparison (Section XX)
`java -Xmx5000m -jar EfficientHillClimbers-0.20-DPX.jar crossover -hd ${hd} -expSols 1000 -aseed ${seed} -problem nk -Pn=${n} -Pk=${k} -Pq=${q} -Pmodel=random -Ppseed=${pseed} -crossover ${crossover} -timer cpuClock`

When DPX is use another parameter is required (it also applies to the commands below): `-Xexhexp=${beta}`

* Evolutionary Algorithm for NKQ Landscapes (Section YY)
`java -Xmx5000m -jar EfficientHillClimbers-0.20-DPX.jar ea -population ${popsize} -mutationProb ${pm} ${crossover_options} ${problem_related_options} -time ${time} -timer cpuClock -aseed ${seed}`

The problem-related options are 
`-problem nk -Pn=${n} -Pk=${k} -Pq=${q} -Pmodel=random -Ppseed=${seed}`
`-problem maxsat -Pinstance=${instancefile}`

The crossover-reated options are
`-crossover ${crossover}` where ${crossover} can be apx, px, nx, ux
`-crossover dpx -Xexhexp=${beta}` for DPX.

* DRILS for NKQ Landscapes (Section ZZ)
`java -Xmx5000m -jar EfficientHillClimbers-0.20-DPX.jar drils -r 1 -mf ${perturbation_factor} ${crossover_options} ${problem_related_options} -time ${time} -timer cpuClock -aseed ${seed}`

* iDPX for NKQ Landscapes (Section WW)
`java -Xmx5000m -jar EfficientHillClimbers-0.20-DPX.jar isox -crossover dpx -Xexhexp=${beta} ${problem_related_options} -expSols ${explored_solutions} -time ${time} -timer cpuClock -aseed ${seed}`

Where ${explored_solutions} is 3 for the expreriments with ${beta}=28.
