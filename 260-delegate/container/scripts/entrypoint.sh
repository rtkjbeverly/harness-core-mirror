#!/bin/bash

#Check for and stop if there is existing execution. That is needed in case of a container restart scenario. 

bash ./stop.sh

if [ "$?" -ne 0 ]; then
	exit 1
fi

source ./start.sh 