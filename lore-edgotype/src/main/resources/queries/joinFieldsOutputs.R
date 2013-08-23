#!/usr/bin/Rscript

#read in all the separate matrices and sum them up.
matsum <- function(l) if (length(l)==0) 0 else l[[1]] + matsum(l[-1])
mat <- matsum(lapply(
	list.files(pattern="mutationCounts_\\w\\w\\.txt"), 
	function(infile) as.matrix(read.delim(infile, row.names=1))
))
colnames(mat) <- 1:ncol(mat)

write.table(mat, file="mutationCountsJoint.txt", quote=FALSE, sep="\t")


#read all output tables from the different cluster nodes
counts <- list()
for (infile in list.files(pattern="variantCounts_\\w\\w\\.txt")) {
	cat("Reading",infile,"\n")
	curr.counts <- as.matrix(read.delim(infile))
	for (id in rownames(curr.counts)) {
		if (is.null(counts[[id]])) {
			counts[[id]] <- curr.counts[id,1]
		} else {
			counts[[id]] <- counts[[id]] + curr.counts[id,1]
		}
	}
}
counts <- data.frame(counts=do.call(c,counts))

write.table(counts,"variantCountsJoint.txt", sep="\t", quote=FALSE)
