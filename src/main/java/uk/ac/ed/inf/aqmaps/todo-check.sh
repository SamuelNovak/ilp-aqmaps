#!/bin/bash

for f in $(ls .)
do
    if [ $f != "todo-check.sh" ] && [ -n "$(cat $f | grep TODO)" ]
    then
	echo $f
    fi
done
