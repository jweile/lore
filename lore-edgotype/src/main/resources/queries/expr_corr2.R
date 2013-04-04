#!/usr/bin/Rscript

# library(GEOquery)
library(affy)
library(hgu133a.db)

file.exist <- function(filename) {
	system(paste("[ -e",filename,"]")) == 0
}

#Get interaction data
pw1 <- read.delim("RC4_Diseasome_Experiment1_dataset.csv")
pw2 <- read.delim("RC4_Diseasome_Experiment2_dataset.csv")

#extract interactions
interactions <- unique(rbind(
	pw1[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")],
	pw2[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")]
))

#Load expression data
if (file.exist("input/exprs.tsv")) {

	cat("Loading pre-processed expression data...\n")

	exprs <- read.delim("input/exprs.tsv")

} else {

	# cat("Processing CEL files to extract expression data...\n")

	# human.files <- read.delim("human_files.tsv", header=FALSE)
	# human.file.names <- paste("GSE1133/", as.character(human.files[,1]), ".CEL.gz",sep="")
	# affy.raw <- ReadAffy(filenames=human.file.names, compress=TRUE)

	# # Normalization and background correction with RMA
	# affy.rma <- rma(affy.raw)
	# affy.matrix <- exprs(affy.rma)

	# #Compute mean values for each tissue
	# exprs <- NULL
	# for (tissue in unique(human.files[,2])) {
	# 	arrays <- paste(as.character(human.files[(human.files[,2] == tissue),1]), ".CEL.gz", sep="")
	# 	exprs <- cbind(exprs, apply(affy.matrix[,arrays], 1, mean))
	# }
	# colnames(exprs) <- unique(human.files[,2])
	# write.table(exprs, file="input/exprs.tsv", quote=FALSE)
}

#get entrez-to-probeID translator
cat("Obtaining probe ID translation table...\n")
probe2entrez <- as.list(hgu133aENTREZID)

#Function for retrieving expression values for a specific entrez id
getExpr <- function(entrez) {
	probes <- names(which(probe2entrez == entrez))
	if (length(probes) > 1) {
		apply(exprs[probes,], 2, mean)
	} else {
		exprs[probes,]
	}
}

calculate.correlations <- function(interaction.table) {
	for (i in 1:nrow(interaction.table)) {
		db.expr <- as.numeric(getExpr(interaction.table[i,1]))
		ad.expr <- as.numeric(getExpr(interaction.table[i,2]))
		tryCatch (
			if ((length(db.expr) == 0) || (length(ad.expr) == 0)) {
				interaction.table[i, "corr"] <- NA
			} else {
				interaction.table[i,"corr"] <- cor(db.expr, ad.expr)
			},
			error = function(x) {
				print(x)
				cat(length(db.expr), " ", length(ad.expr),"\n")
			}
		)
	}
	interaction.table
}


#get a sample of correlations for random gene pairs for comparison
cat("Creating random samples...\n")
all.entrez <- unique(unlist(probe2entrez))
random.pairs <- data.frame(
	a=sample(all.entrez, 2*nrow(interactions)),
	b=sample(all.entrez, 2*nrow(interactions))
)

cat("Computing correlations...\n")
interactions <- calculate.correlations(interactions)
cat("Computing correlations on random samples...\n")
random.pairs <- calculate.correlations(random.pairs)

cat("Drawing histograms...\n")
pdf("correlations.pdf")
op <- par(
	bg="black",
	fg="white",
	col="white",
	col.axis="white",
	col.lab="white",
	col.main="white",
	col.sub="white"
)
colors <- c(red=rgb(1,0,0,.5), grey=rgb(.5,.5,.5,.5))
hist(
	random.pairs$corr, 
	col=colors["grey"],
	freq=FALSE,
	xlim=c(-1,1),
	main="Histogram of correlations",
	xlab="Pearson correlation coefficient",
	border=NA,
	breaks=40
)
hist(
	interactions$corr, 
	col=colors["red"], 
	freq=FALSE, 
	breaks=40,
	border=NA,
	add=TRUE
)
legend("topleft",c("Interacting pairs", "Random pairs"),fill=colors)
par(op)
dev.off()

cat("Writing output table...\n")
write.table(interactions, file="interaction_correlations.tsv", sep="\t", row.names=FALSE, quote=FALSE)

