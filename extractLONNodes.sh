#!/bin/bash
echo -e 'ID\tFitness'
grep LON\ nodes | tr , '\n' | tr : '\t' | sed -e 's/\"//g' -e 's/^.*{//g' -e 's/}//g'

