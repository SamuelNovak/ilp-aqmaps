#!/bin/bash

for f in $(ls ilp-results); do if [ -n "$(cat ilp-results/$f | grep aaaaaa)" ]; then echo $f; fi; done
