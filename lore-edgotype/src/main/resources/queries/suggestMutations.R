#!/usr/bin/Rscript

args <- commandArgs(TRUE)

infile <- args[1]
con  <- file(infile, open = "r")

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

initCodonTable <- function(con) {

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
	
	getSingleForCodon <- function(codon) {
		nc2single[[codon]]
	}

	structure(list(
		getSingleForCodon=getSingleForCodon
	),class="codonTable")
}

translate <- function(ncSeq, codons) {

	paste(sapply(
		seq(1,nchar(ncSeq),3),
		function(i) codons$getSingleForCodon(substr(ncSeq,i,i+2))
	), collapse="")
}

suggestMutations <- function(ncSeq, codons) {

	ncs <- c('A','C','G','T')

	for (i in seq(1,nchar(ncSeq),3)) {
		
		codon <- substr(ncSeq,i,i+2)
		aa <- codons$getSingleForCodon(codon)

		for (j in 1:3) {
			nc <- substr(codon,j,j)
			alternatives <- setdiff(ncs,nc)
		}
	}

}




tryCatch({

	con <- file("codontable.txt",open="r")
	codontable <- initCodonTable(con)

},
error = function(ex) {
	cat(ex)
},
finally = {
	if (isOpen(con)) {
		close(con)
	}
})




