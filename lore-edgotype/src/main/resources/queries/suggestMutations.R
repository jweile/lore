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
char.at <- function(str,i) substr(str,i,i)

library(bitops)

##
# Needleman-Wunsch algorithm
#
new.alignment <- function(s1, s2) {

	c1 <- to.char.array(s1)
	c2 <- to.char.array(s2)

	#init score matrix
	mat <- matrix(nrow=nchar(s1), ncol=nchar(s2))
	mat[1,] <- 1:nchar(s2) - (c1[1] == c2[1])
	mat[,1] <- 1:nchar(s1) - (c1[1] == c2[1])

	#init trace matrix
	trace <- matrix(0, nrow=nchar(s1), ncol=nchar(s2))
	trace[1,] <- 4
	trace[,1] <- 2

	#compute alignment matrix
	for (i in 2:nchar(s1)) {
		for (j in 2:nchar(s2)) {
			options <- c(
				rep = mat[i-1,j-1] + (c1[i] != c2[j]),
				del = mat[i-1,j] + 1,
				ins = mat[i,j-1] + 1
			)
			mat[i,j] <- min(options)

			tr.bitmasks <- 2^(which(options == min(options))-1)
			for (mask in tr.bitmasks) {
				trace[i,j] <- bitOr(trace[i,j],mask)
			}
		}
	}

	getMatrix <- function() {
		mat
	}

	getDistance <- function() {
		mat[nchar(s1),nchar(s2)]
	}

	getMutations <- function() {

		rep <- 1
		del <- 2
		ins <- 4

		muts <- vector()

		i <- nchar(s1)
		j <- nchar(s2)

		while (i > 1 && j > 1) {
			if (bitAnd(trace[i,j], rep) > 0) {
				if (c1[i] != c2[j]) {
					muts[length(muts)+1] <- paste(c1[i],i,c2[j], sep="")
				}
				i <- i-1
				j <- j-1
			} else if (bitAnd(trace[i,j], del)) {
				muts[length(muts)+1] <- paste("-",i, sep="")
				i <- i-1
			} else if (bitAnd(trace[i,j], ins)) {
				muts[length(muts)+1] <- paste("+",i, sep="")
				j <- j-1
			} else {
				stop("uninitialized trace at ",i,j)
			}
		}
		if (c1[1] != c2[1]) {
			muts[length(muts)+1] <- paste(c1[i],i,c2[j], sep="")
		}

		muts
	}

	list(
		getMatrix=getMatrix,
		getDistance=getDistance,
		getMutations=getMutations
	)

}

##
# Creates a new translator object for translating Nucleotide strings to Amino acid strings.
#
init.translator <- function(ctable.file="input/codontable.txt") {

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

		aa <- paste(sapply(
			seq(1,nchar(ncSeq),3),
			function(i) {
				a <- codons$getSingleForCodon(substr(ncSeq,i,i+2))
				if(is.null(a)) "" else a
			}
		), collapse="")

		aaseq <- to.char.array(aa)

		if (any(aaseq == "*")) {
			cutoff <- min(which(aaseq == "*"))
			paste(aaseq[1:cutoff], collapse="")
		} else {
			aa
		}
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

pcr.sim <- function(sequence, cycles=10, init.amount=100, etr=1, mut.rate=1/2000) {

	enzyme.amount <- round(init.amount * etr)

	#calculate sampling bias based on Mutazyme II bias and sequence bias
	pol.bias <- c(A=0.2675, C=0.2325, G=0.2325, T=0.2675)
	seq.bias <- table(to.char.array(sequence)) / nchar(sequence)
	bias <- pol.bias * seq.bias / sum(pol.bias * seq.bias)
	cbias <- c(bias[1],sapply(2:4, function(i) sum(bias[1:i])))
	names(cbias) <- names(bias)

	#make index of nucleotide positions
	nuc.positions <- sapply(c('A','C','G','T'), function(nuc) which(to.char.array(sequence) == nuc))

	#mutation transition matrix based on Mutazyme II
	mut <- cbind(
		rbind(
			A=c(A=0,   C=.047,G=.175,T=.285),
			C=c(A=.141,C=0,   G=.041,T=.255),
			G=c(A=.255,C=.041,G=0,   T=.141),
			T=c(A=.285,C=.175,G=.047,T=0   )
		) * .5,
		DEL=rep(.048,4)/4,
		INS=rep(.008,4)/4
	) * 4
	mut <- mut / apply(mut,1,sum)
	cmut <- cbind(mut[,1],sapply(2:ncol(mut), function(i) apply(mut[,1:i],1,sum)))
	dimnames(cmut) <- dimnames(mut)

	#seed molecule pool with templates
	pool <- list()
	for (i in 1:init.amount) pool[[i]] <- list()

	#perform PCR cycles
	for (c in 1:cycles) {

		num.reactions <- min(length(pool),enzyme.amount)
		templates <- sample(pool, num.reactions)
		num.muts <- rpois(num.reactions, nchar(sequence) * mut.rate)

		new.mutations <- sapply(num.muts, function(num.mut) {

			if (num.mut == 0) {
				return(list())
			}

			# use bias table to figure out how many of each nucleotide to pick for mutating
			to.sample <- table(sapply(1:num.mut, function(i) {
				names(which.min(which(runif(1,0,1) < cbias)))
			}))

			#pick positions to mutate
			to.mutate <- sapply(names(to.sample), function(nuc) {
				sample(nuc.positions[[nuc]], to.sample[nuc])
			})

			#implement mutations
			unlist(sapply(names(to.mutate), function(nuc) {
				sapply(to.mutate[[nuc]], function(pos) {
					#sample mutation
					to.nuc <- names(which.min(which(runif(1,0,1) < cmut[nuc,])))

					if (to.nuc == "DEL" || to.nuc == "INS") {
						return("nonsense")
					} else {

						codon.number <- floor(pos / 3) + 1
						codon.start <- 3*codon.number - 2
						from.codon <- substr(sequence,codon.start,codon.start+2)
						
						change.pos <- pos - codon.start + 1
						to.codon <- from.codon
						substr(to.codon,change.pos,change.pos) <- to.nuc

						from.aa <- translator$translate(from.codon)
						to.aa <- translator$translate(to.codon)

						if (from.aa == to.aa) {
							return("silent")
						} else if (to.aa == "*") {
							return("truncation")
						} else {
							return(paste(from.aa,codon.number,to.aa,sep=""))
						}
					}
				})
			}))

		})
		names(new.mutations) <- NULL
	
		#add mutagenized copies to pool
		pool[(length(pool)+1):(length(pool)+num.reactions)] <- sapply(1:num.reactions, function(i) {
			c(templates[[i]], new.mutations[[i]])
		})

	}

	pool[-(1:init.amount)]
}


template <- "ATGGCTGACCAACTGACTGAAGAGCAGATTGCAGAATTCAAAGAAGCTTTTTCACTATTTGACAAAGATGGTGATGGAACTATAACAACAAAGGAATTGGGAACTGTAATGAGATCTCTTGGGCAGAATCCCACAGAAGCAGAGTTACAGGACATGATTAATGAAGTAGATGCTGATGGTAATGGCACAATTGACTTCCCTGAATTTCTGACAATGATGGCAAGAAAAATGAAAGACACAGACAGTGAAGAAGAAATTAGAGAAGCATTCCGTGTGTTTGATAAGGATGGCAATGGCTATATTAGTGCTGCAGAACTTCGCCATGTGATGACAAACCTTGGAGAGAAGTTAACAGATGAAGAAGTTGATGAAATGATCAGGGAAGCAGATATTGATGGTGATGGTCAAGTAAACTATGAAGAGTTTGTACAAATGATGACAGCAAAGTGA"

pcr.result <- pcr.sim(template)
pcr.stats <- table(sapply(pcr.result, function(mutations) {
	if (any(mutations == "truncation")) {
		"truncation"
	} else if (any(mutations == "nonsense")) {
		"nonsense"
	} else {
		sum(mutations != "silent")
	}
}))





# ##
# # Simulates mutagenic PCR experiment on a given amount of DNA molecules with 
# # given sequence for the number of given PCR cycles, with the given enzyme/template ratio (etr)
# mutagenesis <- function(seq, cycles=10, init.amount=100, etr=1, mut.rate=1/2000) {

# 	#Build transition matrix according to MutazymeII manual
# 	mut <- cbind(
# 		rbind(
# 			A=c(A=0,C=.047,G=.175,T=.285),
# 			C=c(A=.141,C=0,G=.041,T=.255),
# 			G=c(A=.255,C=.041,G=0,T=.141),
# 			T=c(A=.285,C=.175,G=.047,T=0)
# 		) * .5,
# 		DEL=rep(.048,4)/4,
# 		INS=rep(.008,4)/4
# 	) * 4 * mut.rate
# 	for (i in 1:4) {
# 		mut[i,i] <- 1-sum(mut[i,])
# 	}
# 	#Transform to cumulative matrix
# 	cmut <- cbind(mut[,1],sapply(2:ncol(mut), function(i) apply(mut[,1:i],1,sum)))
# 	dimnames(cmut) <- dimnames(mut)

# 	#potentially mutates the given nucleotide
# 	mutate <- function(nucleotide) {
# 		r <- runif(1,0,1)
# 		mutation <- names(which.min(which(r < cmut[nucleotide,])))
# 		if (mutation == "DEL") {
# 			""
# 		} else if (mutation == "INS") {
# 			paste(nucleotide, sample(c('A','C','G','T'),1), sep="")
# 		} else {
# 			mutation
# 		}
# 	}

# 	enzyme.amount <- round(init.amount * etr)

# 	dna <- rep(seq, init.amount)

# 	for (c in 1:cycles) {
# 		dna <- c(dna, 
# 			sapply(sample(dna, min(length(dna),enzyme.amount)), function(curr.template) {
# 				paste(sapply(to.char.array(curr.template), mutate),collapse="")
# 			})
# 		)
# 	}

# 	names(dna) <- NULL

# 	dna[-(1:init.amount)]

# }

# classify.muts <- function(template, mutants) {
# 	sapply(mutants, function(mut) {
# 		if (nchar(template) != nchar(mut)) {
# 			"truncation"
# 		} else {
# 			sum(sapply(1:nchar(template), function(i) char.at(template,i) != char.at(mut,i)))
# 		}
# 	})
# }

# find.snps <- function(template, mut, align.cutoff=5) {
# 	if (nchar(template) == nchar(mut)) {

# 		snps <- unlist(sapply(1:nchar(template), function(i) {
# 			if (char.at(template,i) == char.at(mut,i)) NULL else paste(char.at(template,i),i,char.at(mut,i),sep="")
# 		}))

# 		# if (length(snps) > align.cutoff) {
# 		# 	al <- new.alignment(template, mut)
# 		# 	al$getMutations()
# 		# } else {
# 		# 	snps
# 		# }

# 		snps

# 	} else {

# 		al <- new.alignment(template, mut)
# 		al$getMutations()

# 	}
# }


# template <- "ATGGCTGACCAACTGACTGAAGAGCAGATTGCAGAATTCAAAGAAGCTTTTTCACTATTTGACAAAGATGGTGATGGAACTATAACAACAAAGGAATTGGGAACTGTAATGAGATCTCTTGGGCAGAATCCCACAGAAGCAGAGTTACAGGACATGATTAATGAAGTAGATGCTGATGGTAATGGCACAATTGACTTCCCTGAATTTCTGACAATGATGGCAAGAAAAATGAAAGACACAGACAGTGAAGAAGAAATTAGAGAAGCATTCCGTGTGTTTGATAAGGATGGCAATGGCTATATTAGTGCTGCAGAACTTCGCCATGTGATGACAAACCTTGGAGAGAAGTTAACAGATGAAGAAGTTGATGAAATGATCAGGGAAGCAGATATTGATGGTGATGGTCAAGTAAACTATGAAGAGTTTGTACAAATGATGACAGCAAAGTGA"
# templ.protein <- translator$translate(template)

# dna <- mutagenesis(template, init.amount=20, etr=4)

# proteins <- sapply(dna, translator$translate)

# num.prot.muts <- classify.muts(templ.protein, proteins)
# num.dna.muts <- sapply(dna, function(mut) {
# 	length(find.snps(template,mut))
# })

# op <- par(mfrow=c(1,2))
# barplot(table(num.dna.muts),xlab="Mutations",ylab="Frequency",main="DNA level")
# barplot(table(num.prot.muts),xlab="Mutations",ylab="Frequency",main="Protein level")
# par(op)

# # num.muts <- sapply(proteins[1:100], function(mutant) {
# # 	al <- new.alignment(templ.protein, mutant)
# # 	al$getDistance()
# # })


