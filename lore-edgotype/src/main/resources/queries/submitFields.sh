#!/bin/bash

#First argument is the file with the forward reads
fwdFile=`echo $1`
#Second argument is the file with the reverse reads
revFile=`echo $2`
#Thrid argument is the number of jobs to create
njobs=`echo $3`

echo "Calculating work units..."
#only works on unix, but not mac, due to implementation of cut
lines=`wc -l $fwdFile|cut -f1,1 -d' '`
maxseq=$(( lines/4 ))
chunksize=$(( (lines/4)/njobs + 1 ))

echo "$maxseq work units to process." 
echo "Generating chunks..."

#split into chunks
split -l$((chunksize*4)) $fwdFile fwd
split -l$((chunksize*4)) $revFile rev
#make directory for chunks if it doesn't yet exist
mkdir -p chunks
#make sure directory is empty
rm chunks/*
#move chunks into directory
mv fwd* rev* chunks/

#iterate over chunk IDs
i=0
for chunkId in `echo chunks/fwd*|tr ' ' '\n'|cut -c11-12`; do
	let "i++"
	fwd=`echo "chunks/fwd$chunkId"`
	rev=`echo "chunks/rev$chunkId"`

	sync=n
	if [[ $((i % 40)) -eq 0 ]]; then
		sync=y
		echo "Waiting for batch to complete"
	fi
	
	echo "Starting job #$i Handle:$chunkId ..."
	qsub -N "fields_$chunkId" -wd `pwd` -sync $sync -b y Rscript ../bin/fieldsLabJoint.R $fwd $rev $chunkId
done
