#!/bin/bash

i=0
#iterate over all stderr output files from the last run
for file in fields_*.e*; do

	#if the stderr file is not empty
	if [ -s $file ]; then

		#cut the tag out of the file name
		chunkId=`echo $file|cut -c8-9`

		let "i++"
		
		fwd=chunks/fwd$chunkId
		rev=chunks/rev$chunkId

		#set the qsub sync option for every 40th file
		sync=n
		if [[ $((i % 40)) -eq 0 ]]; then
			sync=y
			echo "Waiting for batch to complete"
		fi

		#delete old stderr files
		rm $file

		#start the job
		echo "Starting job #$i Handle:$chunkId ..."
		qsub -N "fields_$chunkId" -wd `pwd` -sync $sync -b y Rscript ../bin/fieldsLabJoint.R $fwd $rev $chunkId

	fi
done
