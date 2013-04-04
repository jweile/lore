#!/usr/bin/Rscript

library(GEOquery)
# library(affy)
library(hgu133a.db)

# file.exist <- function(filename) {
# 	system(paste("[ -e",filename,"]")) == 0
# }

#Get interaction data
pw1 <- read.delim("input/RC4_Diseasome_Experiment1_dataset.csv")
pw2 <- read.delim("input/RC4_Diseasome_Experiment2_dataset.csv")

#extract interactions
interactions <- unique(rbind(
	pw1[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")],
	pw2[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")]
))

#Download expression data
cat("Downloading expression data...\n")
gds <- getGEO("GDS596")

# # Check if CEL files have previously been downloaded already
# if (!file.exist("GDS596/GDS596_RAW.tar")) {
# 	# Download raw files. That's gigabytes to download, so it'll take a while
# 	getGEOSuppFiles("GDS596")
# 	# unpack raw data files
# 	system("tar xzf GDS596/GDS596_RAW.tar")
# }

# # get list of unpacked files
# affy.files <- as.vector(read.table(pipe("ls -l *.CEL.gz"),sep=" ")[,13])
# affy.raw <- ReadAffy(filenames=affy.files, compress=TRUE)

# # Normalization and background correction with RMA
# affy.rma <- rma(affy.raw)
# affy.matrix <- exprs(affy.rma)


#extract expression values and clean up table
expr <- Table(gds)
rownames(expr) <- expr$ID_REF
expr$ID_REF <- NULL
gene.names <- expr$IDENTIFIER
expr$IDENTIFIER <- NULL
expr <- apply(expr,c(1,2), as.numeric)

#get entrez-to-probeID translator
cat("Obtaining probe ID translation table...\n")
probe2entrez <- as.list(hgu133aENTREZID)
# entrez2probe <- function(entrez) {
# 	names(which(probe2entrez == entrez))
# }

#Function for retrieving expression values for a specific entrez id
getExpr <- function(entrez) {
	probes <- names(which(probe2entrez == entrez))
	if (length(probes) > 1) {
		apply(expr[probes,], 2, mean)
	} else {
		expr[probes,]
	}
}

calculate.correlations <- function(interaction.table) {
	for (i in 1:nrow(interaction.table)) {
		db.expr <- getExpr(interaction.table[i,1])
		ad.expr <- getExpr(interaction.table[i,2])
		if ((length(db.expr) == 0) || (length(ad.expr) == 0)) {
			interaction.table[i, "corr"] <- NA
		} else {
			interaction.table[i,"corr"] <- cor(db.expr, ad.expr)
		}
	}
	interaction.table
}


#get a sample of correlations for random gene pairs for comparison
cat("Creating random samples...\n")
all.entrez <- unique(unlist(probe2entrez))
random.pairs <- data.frame(
	a=sample(all.entrez, nrow(interactions)),
	b=sample(all.entrez, nrow(interactions))
)

cat("Computing correlations...\n")
interactions <- calculate.correlations(interactions)
cat("Computing correlations on random samples...\n")
random.pairs <- calculate.correlations(random.pairs)

cat("Drawing histograms...\n")
pdf("correlations.pdf")
colors <- c(red=rgb(1,0,0,.5), grey=rgb(.5,.5,.5,.5))
hist(
	random.pairs$corr, 
	col=colors["grey"],
	freq=FALSE,
	xlim=c(-1,1),
	main="Histogram of correlations",
	xlab="Pearson correlation coefficient"
)
hist(
	interactions$corr, 
	col=colors["red"], 
	freq=FALSE, 
	add=TRUE
)
legend("topleft",c("Interacting pairs", "Random pairs"),fill=colors)
dev.off()

cat("Writing output table...\n")
write.table(interactions, file="interaction_correlations.tsv", sep="\t", row.names=FALSE, quote=FALSE)

