#!/bin/bash
set -e
set -o pipefail

# Find our own location.
BINDIR=$(dirname "$(readlink -f "$(type -P $0 || echo $0)")")

slurm_job() {
    file=$1
    alg=$2
    NAME=${file%%_*.me.xz}
    TEMP=${NAME##loma-extr-}
    JOBNAME=loma-alg-${TEMP}-${alg}
    cat <<EOF > kk.sh
#!/usr/bin/env bash
# The name to show in queue lists for this job:
#SBATCH -J $JOBNAME
#SBATCH --array=1-$nruns
# Number of desired cpus:
#SBATCH --cpus-per-task=1

# Amount of RAM needed for this job:
#SBATCH --mem=45gb

# The time the job will be running:
#SBATCH --time=1-00:00:00

# To use GPUs you have to request them:
##SBATCH --gres=gpu:1
#SBATCH --constraint=cal

# Set output and error files
#SBATCH --error=$OUTDIR/${JOBNAME}_%J.stderr
#SBATCH --output=$OUTDIR/${JOBNAME}_%J.al.xz

# To load some software (you can show the list with 'module avail'):
module load java

java -jar EfficientHillClimbers-0.22.0-SNAPSHOT/EfficientHillClimbers-0.22.0-SNAPSHOT.jar lo-markov-algorithm <(xz -cd $OUTDIR/$file) $alg | xz
#echo \$COMMAND | xz
#\$COMMAND | xz
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
n=10
k=2
seed=0
alg=drils
for n in 10 15 20; do
	for k in `seq 2 5`; do
		for seed in `seq 0 9`; do
			for alg in ils drils; do
				$LAUNCHER `find $OUTDIR -name loma-extr-n${n}k${k}q${q}${mode}r${r}s${seed}\*.me.xz -printf "%f\n" ` $alg
			done
		done
	done
done

