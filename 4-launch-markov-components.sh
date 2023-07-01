#!/usr/bin/env bash
# The name to show in queue lists for this job:
#SBATCH -J component
#SBATCH --array=1-1
# Number of desired cpus:
#SBATCH --cpus-per-task=1

# Amount of RAM needed for this job:
#SBATCH --mem=25gb

# The time the job will be running:
#SBATCH --time=1-00:00:00

# To use GPUs you have to request them:
##SBATCH --gres=gpu:1
#SBATCH --constraint=cal

# Set output and error files
#SBATCH --error=component_%J.stderr
#SBATCH --output=component_%J.stdout

# To load some software (you can show the list with 'module avail'):
module load java


for file in `find . -maxdepth 1 -name loma-trans\*tr.xz -printf "%f\n"`; do
        NAME=${file%%.tr.xz}
        TEMP=${NAME##loma-trans-}
        output=loma-comp-${TEMP}.co.xz
        java -jar EfficientHillClimbers-0.22.0-SNAPSHOT/EfficientHillClimbers-0.22.0-SNAPSHOT.jar lo-markov-components <(xz -cd $file) | xz > ${output}
done
