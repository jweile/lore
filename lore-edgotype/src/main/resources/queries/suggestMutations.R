#!/usr/bin/Rscript

# args <- commandArgs(TRUE)

# infile <- args[1]
# con  <- file(infile, open = "r")

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

		if (nchar(ncSeq) == 0) stop("translate: empty string! ",ncSeq)

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

##
# Initialize translator
#
translator <- init.translator()


##
# PCR simulation function
# sequence = original DNA sequence, should start with a start codon and end with a stop codon
# cycles = number of PCR cycles to simulate. Should be > 1, but too large numbers will affect runtime and memory usage exponentially
# init.amount = Initial amount of template molecules to use in the simulation
# etr = Enzyme-to-Template ratio. Defaults to 1/1
# mutation.rate = Mutations per bp introduced by the enzyme per replication process.
#
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

						codon.number <- floor((pos-1) / 3) + 1
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

	#return pool without original templates
	pool[-(1:init.amount)]
}




##
# Heatmap drawing function
#
hmap <- function(data, main, col=heat.colors(50)) {
	mar.orig <- (par.orig <- par(c("mar", "las", "mfrow")))$mar
    on.exit(par(par.orig))
    w <- (3 + mar.orig[2]) * par("csi") * 2.54
    layout(matrix(c(2, 1), ncol = 2), widths = c(1, lcm(w)))
    par(las = 1)
    mar <- mar.orig
    mar[4] <- mar[2]
    mar[2] <- 1
    par(mar = mar) 

    zlim <- range(data)
    levels <- seq(zlim[1], zlim[2], length = length(col))
    plot.new()
    plot.window(xlim = c(0, 1), ylim = range(levels), xaxs = "i", yaxs = "i")
    rect(0, levels[-length(levels)], 1, levels[-1], col = col,  density = NA)
    axis(4)
    box()
    mar <- mar.orig
    mar[4] <- 0
    par(mar = mar)

    par(las=0)
	image(
		data,
		axes=FALSE,
		xlab="#Cycles",
		ylab="Enzyme/Template ratio",
		main=main,
		col=col
	)
	axis(1, at=seq(0,1,length.out=length(rownames(data))), labels=rownames(data))
	axis(2, at=seq(0,1,length.out=length(colnames(data))), labels=colnames(data))

}





#####################
#####################
## START OF SCRIPT ##
#####################
#####################



# DNA template to use
template <- "ATGGCTGACCAACTGACTGAAGAGCAGATTGCAGAATTCAAAGAAGCTTTTTCACTATTTGACAAAGATGGTGATGGAACTATAACAACAAAGGAATTGGGAACTGTAATGAGATCTCTTGGGCAGAATCCCACAGAAGCAGAGTTACAGGACATGATTAATGAAGTAGATGCTGATGGTAATGGCACAATTGACTTCCCTGAATTTCTGACAATGATGGCAAGAAAAATGAAAGACACAGACAGTGAAGAAGAAATTAGAGAAGCATTCCGTGTGTTTGATAAGGATGGCAATGGCTATATTAGTGCTGCAGAACTTCGCCATGTGATGACAAACCTTGGAGAGAAGTTAACAGATGAAGAAGTTGATGAAATGATCAGGGAAGCAGATATTGATGGTGATGGTCAAGTAAACTATGAAGAGTTTGTACAAATGATGACAGCAAAGTGA"

##
# Cycle over different parameter combinations
# record yield, efficiency and share of singles in separate matrices
#
# WARNING: This takes a long time to execute!
#

cycle.vals <- 1:40
ratio.vals <- 2^seq(-3,4,.2)

efficiency.matrix <- NULL
yield.matrix <- NULL
singleness.matrix <- NULL
for (cycles in cycle.vals) {
#	cat(cycles,"\n")
	eff.row <- NULL
	yield.row <- NULL
	sing.row <- NULL
	for (etr in ratio.vals) {
		pcr.result <- pcr.sim(template,cycles=cycles,etr=etr)

		yield <- sum(sapply(pcr.result, function(mutations) {
			if (any(mutations == "truncation" | mutations == "nonsense")) {
				FALSE
			} else {
				sum(mutations != "silent") > 0
			}
		})) 
		efficiency <- yield / length(pcr.result)
		singleness <- sum(sapply(pcr.result, function(mutations) {
			sum((mutations != "truncation") & (mutations != "truncation") & (mutations != "truncation")) == 1
		})) / length(pcr.result)

		eff.row[length(eff.row)+1] <- efficiency
		yield.row[length(yield.row)+1] <- yield
		sing.row[length(sing.row)+1] <- singleness
	}
	efficiency.matrix <- rbind(efficiency.matrix, eff.row)
	yield.matrix <- rbind(yield.matrix, yield.row)
	singleness.matrix <- rbind(singleness.matrix, sing.row)
	cat(cycles,"\n")
}
rownames(efficiency.matrix) <- as.character(cycle.vals)
colnames(efficiency.matrix) <- as.character(ratio.vals)
rownames(yield.matrix) <- as.character(cycle.vals)
colnames(yield.matrix) <- as.character(ratio.vals)
rownames(singleness.matrix) <- as.character(cycle.vals)
colnames(singleness.matrix) <- as.character(ratio.vals)


hmap(efficiency.matrix, "Efficiency")
hmap(yield.matrix, "Yield")
hmap(log2(yield.matrix+1), "Log Yield")
hmap(singleness.matrix,"Share of singles")



##
# Examine single result more closely
#

cycles <- 100
etr <- 100
pcr.result <- pcr.sim(template, cycles=cycles, etr=etr, mut.rate=1/1000)

# A function to compute the distribution of mutation counts
mut.distr <- function(mutations) table(sapply(mutations, function(mutations) {
	if (any(mutations == "truncation")) {
		"truncation"
	} else if (any(mutations == "nonsense")) {
		"nonsense"
	} else {
		sum(mutations != "silent")
	}
}))

#Draw distribution of mutations as bar plot
op <- par(las=3)
barplot(mut.distr(pcr.result),
	main=paste("SAC distribution:",cycles,"cycles, ",etr,"enzymes/template"),
	xlab="Mutations",
	ylab="Frequency"
)
par(op)



##
# Simulate Colony picking
#
sample.size <- 20*96

##
# Perform the picking process 1000 times and collect results in matrix
#
categories <- names(mut.distr(pcr.result))
pickings <- apply(t(sapply(
		sapply(1:1000,function(x) {
			mut.distr(sample(pcr.result, sample.size, replace=TRUE))
		}), 
		function(tab) tab[categories]
	)),
	c(1,2), function(element) if (is.na(element)) 0 else element
)
colnames(pickings) <- categories

##
# Draw a boxplot of distributions likely to result from picking
#
op<-par(las=3)
boxplot(pickings, col="orange",main=paste("Picked",sample.size), xlab="mutants", ylab="frequency")
par(op)



####
# Compute map of reachable and observed mutations
####

#translate DNA template to obtain protein sequence
protein <- translator$translate(template)




plotMutCoverage <- function(mutations, all=FALSE) {

	muts <- unlist(mutations[sapply(mutations, {
		function(x) length(x) > 0 && !any(x == "truncation" | x == "nonsense")
	})])

	# a function that returns the i'th codon from the template
	codon.at <- function(dna, i) substr(dna,3*i-2, 3*i)

	#initialize a matrix covering all sequence positions times all possible amino acids
	change.matrix <- matrix(NA,
		nrow=21,
		ncol=nchar(protein),
		dimnames=list(
			c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','*'),
			1:nchar(protein)
		)
	)
	# Mark the fields corresponding to the original aminoacids in the matrix, as well as all potentially reachable mutations
	for (i in 1:nchar(protein)) {
		codon <- codon.at(template,i)
		for (pos in 1:3) {
			for (nc in c('A','C','G','T')) {
				mut.codon <- codon
				substr(mut.codon,pos,pos) <- nc
				aa <- translator$translate(mut.codon)
				change.matrix[aa,i] <- 0
			}
		}
		change.matrix[char.at(protein,i),i] <- -1
	}


	# Mark mutations in the matrix
	for (sac in muts) {

		if (sac == "silent") next

		pos <- as.numeric(substr(sac,2,nchar(sac)-1))
		aa <- substr(sac,nchar(sac),nchar(sac))

		if (is.na(change.matrix[aa,pos])) change.matrix[aa,pos] <- 0
		change.matrix[aa,pos] <- change.matrix[aa,pos] + 1
	}


	op <- par(mfrow=c(2,1),mar=c(0,4.1,4.1,2.1))	
	# compute how many of the reachable mutations are observed for each position
	if (!all) {
		coverage <- (apply(change.matrix,2,function(x) sum(na.omit(x) > 0)) 
			/ apply(change.matrix,2,function(x) sum(!is.na(x))))
	} else {
		coverage <- apply(change.matrix,2,function(x) sum(na.omit(x) > 0)) / 20
	}
	# draw a bar plot for the above coverage
	barplot(coverage,
		main=paste("SAC coverage for",sample.size,"colonies"), 
		xlab="Position",
		ylab="Coverage of possible mutations", 
		ylim=c(0,1),
		border=NA,
		names.arg=NA,
		col="darkolivegreen3"
	)
	# axis(2,at=seq(0,1,.2), labels=seq(0,1,.2))


	# Compute a color gradient to represent the mutation counts
	maxVal <- max(apply(change.matrix,1,function(x) max(na.omit(x))))
	colors <- colorRampPalette(c("white", "orange"))(maxVal+1)

	### Draw the diagram
	# set drawing color to gray and use horizontal axis labels
	op <- c(op,par(fg="gray",las=1))
	par(mar=c(5.1,4.1,0,2.1))
	# create an empty plot
	plot(0,
		type='n',
		axes=FALSE,
		xlim=c(0,nchar(protein)), 
		ylim=c(0,21),
		xlab="Position",
		ylab="Amino acid"
	)
	# iterate over each matrix entry and draw the contents on the plot
	for (x in 1:nchar(protein)) {
		for (y in 1:21) {
			if (!is.na(change.matrix[y,x])) {
				if (change.matrix[y,x] > 0) {
					#observed mutations are drawn in a color shade corresponding to their count
					if (all) {
						rect(x-1,22-y,x,21-y,col=colors[change.matrix[y,x]+1], lty="blank")
					} else {
						rect(x-1,22-y,x,21-y,col=colors[change.matrix[y,x]+1], lty="dotted")
					}
				} else if (change.matrix[y,x] == -1) {
					#original amino acids are marked in gray
					rect(x-1,22-y,x,21-y,col="gray")
				} else if (!all) {
					#reachable aminoacids are marked with dotted outline
					rect(x-1,22-y,x,21-y, lty="dotted")
				}
			}
		}
	}
	# draw axes
	axis(1, at=c(1,seq(5,nchar(protein),5))-.5, labels=c(1,seq(5,nchar(protein),5)))
	axis(2, at=(1:21)-.5, labels=rev(rownames(change.matrix)) )

	par(op)
}


# Simulate colony picking and obtain list of mutations in those colonies
pcr.sample <- sample(pcr.result, sample.size, replace=TRUE)
plotMutCoverage(pcr.sample)


# # a function that returns the i'th codon from the template
# codon.at <- function(dna, i) substr(dna,3*i-2, 3*i)

# #initialize a matrix covering all sequence positions times all possible amino acids
# change.matrix <- matrix(NA,
# 	nrow=21,
# 	ncol=nchar(protein),
# 	dimnames=list(
# 		c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','*'),
# 		1:nchar(protein)
# 	)
# )
# # Mark the fields corresponding to the original aminoacids in the matrix, as well as all potentially reachable mutations
# for (i in 1:nchar(protein)) {
# 	codon <- codon.at(template,i)
# 	for (pos in 1:3) {
# 		for (nc in c('A','C','G','T')) {
# 			mut.codon <- codon
# 			substr(mut.codon,pos,pos) <- nc
# 			aa <- translator$translate(mut.codon)
# 			change.matrix[aa,i] <- 0
# 		}
# 	}
# 	change.matrix[char.at(protein,i),i] <- -1
# }

# # Simulate colony picking and obtain list of mutations in those colonies
# pcr.sample <- sample(pcr.result, sample.size, replace=TRUE)
# pcr.sample.sacs <- unlist(pcr.sample[sapply(pcr.sample, {
# 	function(x) length(x) > 0 && !any(x == "truncation" | x == "nonsense")
# })])

# # Mark mutations in the matrix
# for (sac in pcr.sample.sacs) {

# 	if (sac == "silent") next

# 	pos <- as.numeric(substr(sac,2,nchar(sac)-1))
# 	aa <- substr(sac,nchar(sac),nchar(sac))
# 	change.matrix[aa,pos] <- change.matrix[aa,pos] + 1
# }

# # Compute a color gradient to represent the mutation counts
# maxVal <- max(apply(change.matrix,1,function(x) max(na.omit(x))))
# colors <- colorRampPalette(c("white", "orange"))(maxVal+1)

# ### Draw the diagram
# # set drawing color to gray and use horizontal axis labels
# op <- par(fg="gray",las=1)
# # create an empty plot
# plot(0,
# 	type='n',
# 	axes=FALSE,
# 	xlim=c(0,nchar(protein)), 
# 	ylim=c(0,21),
# 	xlab="Position",
# 	ylab="Amino acid",
# 	main=paste("SAC coverage for",sample.size,"colonies")
# )
# # iterate over each matrix entry and draw the contents on the plot
# for (x in 1:nchar(protein)) {
# 	for (y in 1:21) {
# 		if (!is.na(change.matrix[y,x])) {
# 			if (change.matrix[y,x] > 0) {
# 				#observed mutations are drawn in a color shade corresponding to their count
# 				rect(x-1,22-y,x,21-y,col=colors[change.matrix[y,x]+1], lty="dotted")
# 			} else if (change.matrix[y,x] == -1) {
# 				#original amino acids are marked in gray
# 				rect(x-1,22-y,x,21-y,col="gray")
# 			} else {
# 				#reachable aminoacids are marked with dotted outline
# 				rect(x-1,22-y,x,21-y, lty="dotted")
# 			}
# 		}
# 	}
# }
# # draw axes
# axis(1, at=(1:nchar(protein))-.5, labels=1:nchar(protein))
# axis(2, at=(1:21)-.5, labels=rev(rownames(change.matrix)) )
# par(op)


# ##
# # General mutation coverage for each position
# #

# # compute how many of the reachable mutations are observed for each position
# coverage <- (apply(change.matrix,2,function(x) sum(na.omit(x) > 0)) 
# 	/ apply(change.matrix,2,function(x) sum(!is.na(x))))
# # draw a bar plot for the above coverage
# barplot(coverage,
# 	main=paste("SAC coverage for",sample.size,"colonies"), 
# 	xlab="Position",
# 	ylab="Coverage of possible mutations", 
# 	ylim=c(0,1),
# 	border=NA
# )





