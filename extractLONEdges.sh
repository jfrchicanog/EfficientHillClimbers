#!/bin/bash
echo -e 'ID-start\tID-end\tType'
grep LON\ edges | sed -e $'s/},{/\\\n/g' -e 's/^.*\[{//g' -e 's/}\]//g' -e 's/\"left\"://g' -e 's/\"middle\"://g' -e 's/\"right\"://g' -e 's/\"//g' | tr , '\t' 

