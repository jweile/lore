#!/usr/bin/Rscript

args <- commandArgs(TRUE)
fwdFile <- args[1]
revFile <- args[2]
jobTag <- args[3]

dir.create("output", showWarnings = FALSE)
outfile <- paste("output/mutationCounts_",jobTag,".txt",sep="")
outfile2 <- paste("output/variantCounts_",jobTag,".txt",sep="")
logfile <- file(paste("output/fieldsLab_",jobTag,".log",sep=""), open="wt")
sink(logfile, type="message")


source("../bin/libmyseq.R")

distCutoff <- 10
batchSize <- 100

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

#Function for computing consensus of two sequences
buildConsensus <- function(s1, s2, al) {
	#Get alignment map and corresponding phred tracks
	map <- al$getMappings()
	qual <- cbind(s1$getQuality(map[,1]),s2$getQuality(map[,2]))

	#for each alignment position
	s <- sapply(1:nrow(map), function(i) {

		c1 <- char.at(s1$toString(),map[i,1])
		c2 <- char.at(s2$toString(),map[i,2])

		#Choose bases over blanks
		if (is.na(c1)) {
			c(c2, qual[i,2])
		} else if (is.na(c2)) {
			c(c1, qual[i,1])
		} else if (c1==c2) {
			#Compute joint quality for matching bases
			c(c1, joint.phred(qual[i,1],qual[i,2]))
		} else {
			#For mismatches choose base with higher quality
			if (which.max(qual[i,]) == 1) {
				c(c1, qual[i,1])
			} else {
				c(c2, qual[i,2])
			}
		}
	})
	new.sequence(
		paste(s[1,],collapse=""), 
		qual=as.numeric(s[2,]), 
		id=s1$getID()
	)
}

#init translation function
trans <- init.translator("../bin/codontable.txt")

#YAP1 WW-domain reference sequence
ref.nc <- new.sequence("GCAGGTTGGGAGATGGCAAAGACATCTTCTGGTCAGAGATACTTCTTAAATCACATCGATCAGACAACAACATGG",id="YAP1_WW")
ref.aa <- trans$translate(ref.nc)

#prepare storage matrix
change.matrix <- matrix(0,
	nrow=21,
	ncol=nchar(ref.aa),
	dimnames=list(
		c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','*'),
		1:nchar(ref.aa)
	)
)
var.counts <- list()

#function for computing the shift between two aligned sequences
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

#Estimate number of entries in input files
cat("Estimating file size...")
maxlines <- read.table(pipe(paste("wc -l",fwdFile,"|cut -f1,1 --delim=' '")))[1,1]
maxprogress <- maxlines/4
cat("Done!",maxprogress,"items to process.\n")

tryCatch({

	#open files and attach parsers
	con.fwd <- file(fwdFile,open="r")
	par.fwd <- new.fastq.parser(con.fwd)
	con.rev <- file(revFile,open="r")
	par.rev <- new.fastq.parser(con.rev)

	cat("\nInitializing...")
	progress <- 0


	#Main routine:
	repeat {

		#Read a batch of sequences from each file
		batch.fwd <- par.fwd$parse.next(batchSize)
		batch.rev <- par.rev$parse.next(batchSize)

		#Check for problems
		if (length(batch.fwd) != length(batch.rev)) {
			stop("Uneven retrieval from paired-end files:",length(batch.fwd),"vs",length(batch.rev))
		}
		#Check if we're at the end of the file
		if (length(batch.fwd) == 0 || length(batch.rev) == 0) {
			break
		}

		#Compute the reverse complements for the reverse sequence batch
		batch.rev <- lapply(batch.rev, reverseComplement)

		#Iterate over batch entries
		for (i in 1:length(batch.fwd)) {

			cat("\rProgress:",100*(progress <- progress+1)/maxprogress,"%     ")

			s1 <- batch.fwd[[i]]
			s2 <- batch.rev[[i]]

			#Check for problems
			if (s1$getID() != s2$getID()) {
				stop("Sequence IDs of pair do not match!")
			}

			#Align forward and reverse reads
			al <- new.alignment(s1,s2)

			#Check for bad reads
			if (al$getDistance() > distCutoff) {
				warning("dist(",s1$getID(),",",s2$getID(),")=",al$getDistance(),", skipping!")
				next
			}

			#Build consensus and align with reference
			s <- buildConsensus(s1, s2, al)
			al.ref <- new.alignment(ref.nc,s)
			
			#cut into correct frame
			map <- al.ref$getMappings()
			to.cut <- (-compute.shift(map))%%3
			s.cut <- subseq(s,to.cut+1,length(s))

			#translate and align proteins
			s.aa <- trans$translate(s.cut)
			al.aa <- new.alignment(ref.aa, s.aa)

			muts <- al.aa$getMutations()
			if (!is.null(muts) && is.matrix(muts) && nrow(muts) > 3) {
				#using same cutoff as the paper
				warning("Skipping excessive mutant")
				next
			}

			#remove indels
			if (!is.null(muts) && nrow(muts) > 0) {
				snps <- muts[apply(muts, 1, function(mut) !any(is.na(mut) | mut=='-')),, drop=FALSE]
			} else {
				snps <- muts
			}

			# tryCatch({
				#build variant signature string
				if (!is.null(snps) && length(snps) > 0) {
					snp.sig <- paste(apply(snps,1,function(x) 
						paste(x[1],x[2],x[4],sep="")
					),collapse=',')
				} else {
					snp.sig <- "WildType"
				}
				#and increase counter for signature
				if (is.null(var.counts[[snp.sig]])) {
					var.counts[[snp.sig]] <- 0
				}
				var.counts[[snp.sig]] <- var.counts[[snp.sig]]+1
			# }, error=function(e) {
			# 	print(muts)
			# 	print(snps)
			# 	stop(e)
			# })

			#Skip WT
			if (length(snps) == 0) {
				warning("Skipping WT")
				next
			}

			#enter snps in count matrix
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

		#update output file after each batch
		write.table(change.matrix, file=outfile, quote=FALSE, sep="\t")
		write.table(
			data.frame(do.call(c,var.counts)), file=outfile2, 
			quote=FALSE, sep="\t"
		)
	}

	cat("\nFinished! :)\n")
	
},
error = function(ex) {
	traceback(ex)
},
finally = {
	if (exists("con.fwd") && isOpen(con.fwd)) {
		close(con.fwd)
	}
	if (exists("con.rev") && isOpen(con.rev)) {
		close(con.rev)
	}
})

