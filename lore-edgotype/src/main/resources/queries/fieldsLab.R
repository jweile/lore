#!/usr/bin/Rscript

source("libmyseq.R")

distCutoff <- 10
batchSize <- 10

#compute joint illumina scores from two given illumina scores
joint.phred <- function(q1,q2) {
	#compute phred score from error probability
	phred <- function(p) -10*log10(p)
	#compute error probability from phred score
	eprob <- function(phred) 10^(-phred/10)
	#compute joint error odds from two error probabilities
	joint.odds <- function(p1,p2) (1/3) * (p1/(1-p1)) * (p2/(1-p2))
	#turn odds into probability
	p <- function(o) o/(1+o)
	#do computation
	phred(p(joint.odds(eprob(q1-32),eprob(q2-32))))+32
}

cat("Estimating file size...")
maxlines <- read.table(pipe("wc -l SRR058872_1.fastq|cut -f1,1 --delim=' '"))[1,1]
maxprogress <- maxlines/4
cat("Done!\n")


tryCatch({

	con.fwd <- file("SRR058872_1.fastq",open="r")
	par.fwd <- new.fastq.parser(con.fwd)
	con.rev <- file("SRR058872_2.fastq",open="r")
	par.rev <- new.fastq.parser(con.rev)

	con.out <- file("consensus.fastq",open="w")

	cat("\nInitializing...")
	progress <- 0

	repeat {

		batch.fwd <- par.fwd$parse.next(batchSize)
		batch.rev <- par.rev$parse.next(batchSize)

		if (length(batch.fwd) != length(batch.rev)) {
			stop("Uneven retrieval from paired-end files:",length(batch.fwd),"vs",length(batch.rev))
		}
		if (length(batch.fwd) == 0 || length(batch.rev) == 0) {
			break
		}

		batch.rev <- lapply(batch.rev, reverseComplement)

		batch.out <- list()

		for (i in 1:length(batch.fwd)) {

			cat("\rProgress:",100*(progress <- progress+1)/maxprogress,"%     ")

			s1 <- batch.fwd[[i]]
			s2 <- batch.rev[[i]]

			if (s1$getID() != s2$getID()) {
				stop("Sequence IDs of pair do not match!")
			}

			al <- new.alignment(s1,s2)

			if (al$getDistance() > distCutoff) {
				warning("dist(",s1$getID(),",",s2$getID(),")=",al$getDistance(),", skipping!")
				next
			}

			map <- al$getMappings()
			qual <- cbind(s1$getQuality(map[,1]),s2$getQuality(map[,2]))

			consensus <- sapply(1:nrow(map), function(i) {

				c1 <- char.at(s1$toString(),map[i,1])
				c2 <- char.at(s2$toString(),map[i,2])

				if (is.na(c1)) {
					c(c2, qual[i,2])
				} else if (is.na(c2)) {
					c(c1, qual[i,1])
				} else if (c1==c2) {
					c(c1, joint.phred(qual[i,1],qual[i,2]))
				} else {
					if (which.max(qual[i,]) == 1) {
						c(c1, qual[i,1])
					} else {
						c(c2, qual[i,2])
					}
				}
			})
			cons.seq <- new.sequence(paste(consensus[1,],collapse=""), qual=as.numeric(consensus[2,]), id=s1$getID())
			batch.out[[length(batch.out)+1]] <- cons.seq
		}

		writeFASTQ(con.out,batch.out)

	}

	cat("\nFinished! :)")
	
},
error = function(ex) {
	traceback(ex)
},
finally = {
	if (exists("con.fwd") && isOpen(con.fwd)) {
		close(con)
	}
	if (exists("con.rev") && isOpen(con.rev)) {
		close(con)
	}
	if (exists("con.out") && isOpen(con.out)) {
		close(con)
	}
})

