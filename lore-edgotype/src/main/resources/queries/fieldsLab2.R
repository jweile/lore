#!/usr/bin/Rscript

source("libmyseq.R")

logfile <- file("fieldsLab2.log", open="wt")
sink(logfile, type="message")


distCutoff <- 10
batchSize <- 10

trans <- init.translator()

ref.nc <- new.sequence("GCAGGTTGGGAGATGGCAAAGACATCTTCTGGTCAGAGATACTTCTTAAATCACATCGATCAGACAACAACATGG",id="YAP1_WW")
ref.aa <- trans$translate(ref.nc)

change.matrix <- matrix(0,
	nrow=21,
	ncol=nchar(ref.aa),
	dimnames=list(
		c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','*'),
		1:nchar(ref.aa)
	)
)

compute.shift <- function(map) {
	if (any(is.na(map[1,]))) {
		direction <- which(is.na(map[1,]))
		i <- 1
		while(is.na(map[i+1,direction])) i <- i+1
		if (direction == 1) -i else i
	} else {
		0
	}
}

tryCatch({

	con.in <- file("consensus.fastq",open="r")
	parser <- new.fastq.parser(con.in)

	# con.out <- file("snps.txt",open="w")
	progress <- 0
	while (length(batch <- parser$parse.next(batchSize)) > 0) {

		for (s in batch) {

			cat("\rProgress:",(progress <- progress + 1))

			al <- new.alignment(ref.nc,s)
			
			#cut into correct frame
			map <- al$getMappings()
			to.cut <- (-compute.shift(map))%%3
			s.cut <- subseq(s,to.cut+1,length(s))

			#translate and align
			s.aa <- trans$translate(s.cut)
			al.aa <- new.alignment(ref.aa, s.aa)

			#using same cutoff as the paper
			muts <- al.aa$getMutations()
			if (length(muts) == 0) {
				warning("Skipping WT")
				next
			}
			if (nrow(muts) > 3) {
				warning("Skipping excessive mutant")
				next
			}

			#remove indels
			non.indel <- apply(muts, 1, function(mut) !any(is.na(mut) | mut=='-'))
			if (!any(non.indel)) {
				next
			}
			snps <- muts[non.indel,]
			if (is.matrix(snps)) {
				for (i in 1:nrow(snps)) {
					aa <- snps[i,4]
					pos <- as.numeric(snps[i,2])
					change.matrix[aa,pos] <- change.matrix[aa,pos] + 1
				}
			} else {
				aa <- snps[4]
				pos <- as.numeric(snps[2])
				change.matrix[aa,pos] <- change.matrix[aa,pos] + 1
			}
		}

	}
	cat("\nFinished!\n")
	write.table(change.matrix, file="mutation_counts.txt", quote=FALSE, sep="\t")

},
error = function(ex) {
	traceback(ex)
},
finally = {
	if (exists("con.in") && isOpen(con.in)) {
		close(con.in)
	}
	# if (exists("con.out") && isOpen(con.out)) {
	# 	close(con)
	# }
})


