#!/usr/bin/Rscript

space <- read.delim("input/mut_candidates/SPACE.tsv")
rownames(space) <- space[,1]

hi1 <- read.delim("input/mut_candidates/CCSB_HI1_updated.tsv")
hi2 <- read.delim("input/mut_candidates/HI2.tsv")
hi2012 <- read.delim("input/mut_candidates/HI2012.tsv")[,c(1,3)]

inSpace1 <- function(interactions) {
	filter <- apply(interactions, 1, function(row) space[as.character(row[1]),"in_space_i"] && space[as.character(row[2]),"in_space_i"])
	interactions[filter,]
}

iaUnion <- function(i1, i2) {
	do.call(rbind,strsplit(union(
		apply(i1,1, paste, collapse="-"),
		apply(i2,1, paste, collapse="-")
	),"-"))
}

hi2s1 <- inSpace1(hi2)
hi2012s1 <- inSpace1(hi2012)

hi3s1 <- iaUnion(hi2s1, hi2012s1)
hi3 <- iaUnion(hi2,hi2012)

colnames(hi3s1) <- c("entrez_gene_ida","entrez_gene_idb")
colnames(hi3) <- c("entrez_gene_ida","entrez_gene_idb")

write.table(hi3s1, "input/mut_candidates/HI3_space1.tsv", row.names=FALSE, sep="\t", quote=FALSE)
write.table(hi3, "input/mut_candidates/HI3.tsv", row.names=FALSE, sep="\t", quote=FALSE)


denseZone <- read.delim("input/mut_candidates/Samogram_n_pubs_autozone_LC_2+_13.txt")[,1]

hi3_dense <- hi3[apply(hi3, 1, function(row) all(row %in% denseZone)),]
hi3s1_dense <- hi3s1[apply(hi3s1, 1, function(row) all(row %in% denseZone)),]

write.table(hi3s1_dense, "input/mut_candidates/HI3_space1_dense.tsv", row.names=FALSE, sep="\t", quote=FALSE)
write.table(hi3_dense, "input/mut_candidates/HI3_dense.tsv", row.names=FALSE, sep="\t", quote=FALSE)
