#!/usr/bin/Rscript

args <- commandArgs(TRUE)

infile <- args[1]
con  <- file(infile, open = "r")

###
# Reads the contents of a FASTA file, returning a list object that
# contains the sequences indexed under their given identifiers.
# Parameters: 
# * con: file connection object
# * idIndex: index of the id element in the header of each FASTA entry (delimited by "|")
# 
readFASTA <- function(con, idIndex) {

	id <- NULL
	sequence <- NULL
	contents <- list()

	while (length(line <- readLines(con, n=1, warn=FALSE)) > 0) {
	    
		if (substr(line,1,1) == ">") {

			if (!is.null(id) && !is.null(sequence)) {
				contents[id] <- sequence
			}

			id <- strsplit(substr(line,2,nchar(line)), "|", fixed=TRUE)[idIndex]

		} else if (nchar(line) >= 0) {

			sequence <- paste(sequenc e, line, sep="")
		}
	} 

	contents
}


#Function to turn string into character array
to.char.array <- function (str) sapply(1:nchar(str), function(i) substr(str,i,i))


init.translator <- function(ctable.file="codontable.txt") {

	##
	# Creates a new codon table object
	#
	init.codon.table <- function(con) {

		nc2single <- list()
		nc2triple <- list()

		while (length(line <- readLines(con, n=1, warn=FALSE)) > 0) {
			cols <- strsplit(line,"\t")[[1]]
			aa3 <- cols[1]
			aa1 <- cols[2]
			codons <- strsplit(cols[3], "|", fixed=TRUE)[[1]]
			for (codon in codons) {
				nc2single[codon] <- aa1
				nc2triple[codon] <- aa3
			}
		}
		
		# Return single-letter code of aminoacid that is encoded by the given codon
		getSingleForCodon <- function(codon) {
			nc2single[[codon]]
		}

		structure(list(
			getSingleForCodon=getSingleForCodon
		),class="codonTable")
	}


	tryCatch({

		con <- file(ctable.file, open="r")
		codons <- init.codon.table(con)

	},
	error = function(ex) {
		cat(ex)
	},
	finally = {
		if (isOpen(con)) {
			close(con)
		}
	})

	# translates a given nucleotide sequence
	translate <- function(ncSeq) {

		paste(sapply(
			seq(1,nchar(ncSeq),3),
			function(i) codons$getSingleForCodon(substr(ncSeq,i,i+2))
		), collapse="")
	}

	list(translate=translate)	
}

translator <- init.translator()

# suggestMutations <- function(ncSeq, codons) {

# 	ncs <- c('A','C','G','T')

# 	for (i in seq(1,nchar(ncSeq),3)) {
		
# 		codon <- substr(ncSeq,i,i+2)
# 		aa <- codons$getSingleForCodon(codon)

# 		for (j in 1:3) {
# 			nc <- substr(codon,j,j)
# 			alternatives <- setdiff(ncs,nc)
# 		}
# 	}

# }




##
# Simulates mutagenic PCR experiment on a given amount of DNA molecules with 
# given sequence for the number of given PCR cycles, with the given enzyme/template ratio (etr)
mutagenesis <- function(seq, cycles=10, init.amount=100, etr=1/10, mut.rate=1/2000) {

	#Build transition matrix according to MutazymeII manual
	mut <- cbind(
		rbind(
			A=c(A=0,C=.047,G=.175,T=.285),
			C=c(A=.141,C=0,G=.041,T=.255),
			G=c(A=.255,C=.041,G=0,T=.141),
			T=c(A=.285,C=.175,G=.047,T=0)
		) * .5,
		DEL=rep(.048,4)/4,
		INS=rep(.008,4)/4
	) * 4 * mut.rate
	for (i in 1:4) {
		mut[i,i] <- 1-sum(mut[i,])
	}
	#Transform to cumulative matrix
	cmut <- cbind(mut[,1],sapply(2:ncol(mut), function(i) apply(mut[,1:i],1,sum)))
	dimnames(cmut) <- dimnames(mut)

	#potentially mutates the given nucleotide
	mutate <- function(nucleotide) {
		r <- runif(1,0,1)
		mutation <- names(which.min(which(r < cmut[nucleotide,])))
		if (mutation == "DEL") {
			""
		} else if (mutation == "INS") {
			paste(nucleotide, sample(c('A','C','G','T'),1), sep="")
		} else {
			mutation
		}
	}

	enzyme.amount <- round(init.amount * etr)

	dna <- rep(seq, init.amount)

	for (c in cycles) {
		dna <- c(dna, sapply(sample(dna, min(length(dna),enzyme.amount)), function(curr.template) {
			paste(sapply(to.char.array(seq), mutate),collapse="")
		}))
	}

	dna

}


