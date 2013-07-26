
if (!exists("new.sequence")) {
	source("queries/libmyseq.R")
}
if (!exists("init.result.io")) {
	source("queries/resultio.R")
}

overlap <- 21
spacer <- 18
num.aa <- 20

e.M <- 2

xvals <- seq(99,2001,3)
toDraw <- t(rbind(xvals, sapply(xvals, function(l) {

	n <- floor((l-overlap)/(spacer+overlap))
	r <- (l-overlap) %% (spacer+overlap)

	m.tot <- num.aa*((overlap*2 + spacer)/3 - 1 + (n-2)*(overlap+spacer)/3 + r/3 - 1)
	w.tot <- num.aa*(((overlap*2 + spacer)/3 - 1)*(n/e.M-1) + ((n-2)*(overlap+spacer)/3)*(n/e.M-1) + (r/3 - 1)*(n/e.M-1))
	tot <- m.tot + w.tot

	c(m.tot,w.tot,tot)
})))
colnames(toDraw) <- c('orf.length','mutant.oligos','wt.oligos','total.oligos')


plot(toDraw[,'orf.length'], toDraw[,'mutant.oligos'], col="red", 
	type='l', 
	ylim=c(0,max(toDraw)),
	main="Required oligos",
	xlab="ORF length",
	ylab="#oligos"
)
lines(toDraw[,'orf.length'], toDraw[,'wt.oligos'], col="green")
lines(toDraw[,'orf.length'], toDraw[,'total.oligos'], col="black")
legend("topleft",c("mutant oligos","wt oligos","total oligos"),lty=1,col=c("red","green","black"))



#Draw probability density for number of mutations
barplot(dpois(0:10, e.M),names.arg=0:10, xlab="#mutations",ylab="prob. density")




# DNA template to use
template <- "ATGGCTGACCAACTGACTGAAGAGCAGATTGCAGAATTCAAAGAAGCTTTTTCACTATTTGACAAAGATGGTGATGGAACTATAACAACAAAGGAATTGGGAACTGTAATGAGATCTCTTGGGCAGAATCCCACAGAAGCAGAGTTACAGGACATGATTAATGAAGTAGATGCTGATGGTAATGGCACAATTGACTTCCCTGAATTTCTGACAATGATGGCAAGAAAAATGAAAGACACAGACAGTGAAGAAGAAATTAGAGAAGCATTCCGTGTGTTTGATAAGGATGGCAATGGCTATATTAGTGCTGCAGAACTTCGCCATGTGATGACAAACCTTGGAGAGAAGTTAACAGATGAAGAAGTTGATGAAATGATCAGGGAAGCAGATATTGATGGTGATGGTCAAGTAAACTATGAAGAGTTTGTACAAATGATGACAGCAAAGTGA"

source("libmyseq.R")
translator <- init.translator()

protein <- translator$translate(template)
aas <- c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y')

sample.size <- 10*96

mutations <- lapply(rpois(sample.size, e.M), function(m) {
	if (m == 0) {
		list()
	} else {
		#create m mutations
		sapply(sample(1:(nchar(protein)-1),m), function(pos) {
			from.aa <- char.at(protein,pos)
			to.aa <- sample(setdiff(aas,from.aa),1)
			paste(from.aa,pos,to.aa,sep="")
		})
	}
})



# plotMutCoverage <- function(mutations, all=FALSE) {

# 	muts <- unlist(mutations[sapply(mutations, {
# 		function(x) length(x) > 0 && !any(x == "truncation" | x == "nonsense")
# 	})])

# 	# a function that returns the i'th codon from the template
# 	codon.at <- function(dna, i) substr(dna,3*i-2, 3*i)

# 	#initialize a matrix covering all sequence positions times all possible amino acids
# 	change.matrix <- matrix(NA,
# 		nrow=21,
# 		ncol=nchar(protein),
# 		dimnames=list(
# 			c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','*'),
# 			1:nchar(protein)
# 		)
# 	)
# 	# Mark the fields corresponding to the original aminoacids in the matrix, as well as all potentially reachable mutations
# 	for (i in 1:nchar(protein)) {
# 		codon <- codon.at(template,i)
# 		for (pos in 1:3) {
# 			for (nc in c('A','C','G','T')) {
# 				mut.codon <- codon
# 				substr(mut.codon,pos,pos) <- nc
# 				aa <- translator$translate(mut.codon)
# 				change.matrix[aa,i] <- 0
# 			}
# 		}
# 		change.matrix[char.at(protein,i),i] <- -1
# 	}


# 	# Mark mutations in the matrix
# 	for (sac in muts) {

# 		if (sac == "silent") next

# 		pos <- as.numeric(substr(sac,2,nchar(sac)-1))
# 		aa <- substr(sac,nchar(sac),nchar(sac))

# 		if (is.na(change.matrix[aa,pos])) change.matrix[aa,pos] <- 0
# 		change.matrix[aa,pos] <- change.matrix[aa,pos] + 1
# 	}


# 	op <- par(mfrow=c(2,1),mar=c(0,4.1,4.1,2.1))	
# 	# compute how many of the reachable mutations are observed for each position
# 	if (!all) {
# 		coverage <- (apply(change.matrix,2,function(x) sum(na.omit(x) > 0)) 
# 			/ apply(change.matrix,2,function(x) sum(!is.na(x))))
# 	} else {
# 		coverage <- apply(change.matrix,2,function(x) sum(na.omit(x) > 0)) / 20
# 	}
# 	# draw a bar plot for the above coverage
# 	barplot(coverage,
# 		main=paste("SAC coverage for",sample.size,"colonies"), 
# 		xlab="Position",
# 		ylab="Coverage of possible mutations", 
# 		ylim=c(0,1),
# 		border=NA,
# 		names.arg=NA,
# 		col="darkolivegreen3"
# 	)
# 	# axis(2,at=seq(0,1,.2), labels=seq(0,1,.2))


# 	# Compute a color gradient to represent the mutation counts
# 	maxVal <- max(apply(change.matrix,1,function(x) max(na.omit(x))))
# 	colors <- colorRampPalette(c("white", "orange"))(maxVal+1)

# 	### Draw the diagram
# 	# set drawing color to gray and use horizontal axis labels
# 	op <- c(op,par(fg="gray",las=1))
# 	par(mar=c(5.1,4.1,0,2.1))
# 	# create an empty plot
# 	plot(0,
# 		type='n',
# 		axes=FALSE,
# 		xlim=c(0,nchar(protein)), 
# 		ylim=c(0,21),
# 		xlab="Position",
# 		ylab="Amino acid"
# 	)
# 	# iterate over each matrix entry and draw the contents on the plot
# 	for (x in 1:nchar(protein)) {
# 		for (y in 1:21) {
# 			if (!is.na(change.matrix[y,x])) {
# 				if (change.matrix[y,x] > 0) {
# 					#observed mutations are drawn in a color shade corresponding to their count
# 					if (all) {
# 						rect(x-1,22-y,x,21-y,col=colors[change.matrix[y,x]+1], lty="blank")
# 					} else {
# 						rect(x-1,22-y,x,21-y,col=colors[change.matrix[y,x]+1], lty="dotted")
# 					}
# 				} else if (change.matrix[y,x] == -1) {
# 					#original amino acids are marked in gray
# 					rect(x-1,22-y,x,21-y,col="gray")
# 				} else if (!all) {
# 					#reachable aminoacids are marked with dotted outline
# 					rect(x-1,22-y,x,21-y, lty="dotted")
# 				}
# 			}
# 		}
# 	}
# 	# draw axes
# 	axis(1, at=c(1,seq(5,nchar(protein),5))-.5, labels=c(1,seq(5,nchar(protein),5)))
# 	axis(2, at=(1:21)-.5, labels=rev(rownames(change.matrix)) )

# 	par(op)
# }

plotMutCoverage(mutations,TRUE)

