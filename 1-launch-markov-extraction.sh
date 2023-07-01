#!/bin/bash
set -e
set -o pipefail

# Find our own location.
BINDIR=$(dirname "$(readlink -f "$(type -P $0 || echo $0)")")

slurm_job() {
    n=$1
    k=$2
    q=$3
    c=$4
    r=$5
    seed=$6
    if [ "$c" == "adjacent" ]; then
    	circular=y
    else
    	circular=no
    fi
    JOBNAME=loma-extr-n${n}k${k}q${q}${c}r${r}s${seed}
    cat <<EOF > kk.sh
#!/usr/bin/env bash
# The name to show in queue lists for this job:
#SBATCH -J $JOBNAME
#SBATCH --array=1-$nruns
# Number of desired cpus:
#SBATCH --cpus-per-task=1

# Amount of RAM needed for this job:
#SBATCH --mem=35gb

# The time the job will be running:
#SBATCH --time=1-00:00:00

# To use GPUs you have to request them:
##SBATCH --gres=gpu:1
#SBATCH --constraint=cal

# Set output and error files
#SBATCH --error=$OUTDIR/${JOBNAME}_%J.stderr
#SBATCH --output=$OUTDIR/${JOBNAME}_%J.me.xz

# To load some software (you can show the list with 'module avail'):
module load java

COMMAND="java -jar EfficientHillClimbers-0.22.0-SNAPSHOT/EfficientHillClimbers-0.22.0-SNAPSHOT.jar lo-markov-extraction $n $k $q $circular $r $seed"
#echo \$COMMAND | xz
\$COMMAND | xz
EOF
sbatch kk.sh
rm kk.sh
}


nruns=1
LAUNCHER=slurm_job
OUTDIR="${BINDIR}/results"
mkdir -p "${OUTDIR}"


k=2
q=100
r=1
mode=random
for n in 15; do
	for k in 4; do
		for seed in 6; do 
			$LAUNCHER $n $k $q $mode $r $seed
		done
	done
done

