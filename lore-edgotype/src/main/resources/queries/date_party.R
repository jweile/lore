#!/usr/bin/Rscript

# library(GEOquery)
# library(affy)
library(hgu133a.db)

source("queries/resultio.R")
rio <- init.result.io("dateparty_hubs")

# file.exist <- function(filename) {
# 	system(paste("[ -e",filename,"]")) == 0
# }

my.colors <- c(red=rgb(1,0,0,.5), grey=rgb(.5,.5,.5,.5))

# parblack <- function() {
# 	# par(
# 	# 	bg="black",
# 	# 	fg="white",
# 	# 	col="white",
# 	# 	col.axis="white",
# 	# 	col.lab="white",
# 	# 	col.main="white",
# 	# 	col.sub="white"
# 	# )
# }

# timestamp <- format(Sys.time(),format='%Y-%m-%d_%H:%M:%S')
# out.dir <- paste("results/",timestamp,"_dateparty_hubs/", sep="")
# dir.create(out.dir, mode="0755")

# rio$draw <- function(name, f) {

# 	pdf(paste(out.dir,name,".pdf",sep=""))
# 	op <- parblack()
# 	f()
# 	par(op)
# 	dev.off()

# 	svg(paste(out.dir,name,".svg",sep=""))
# 	op <- parblack()
# 	f()
# 	par(op)
# 	dev.off()
# }

cat("Loading interactions...\n")

#Get interaction data
pw1 <- read.delim("input/RC4_Diseasome_Experiment1_dataset.csv")
pw2 <- read.delim("input/RC4_Diseasome_Experiment2_dataset.csv")

#extract interactions
interactions <- unique(rbind(
	pw1[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")],
	pw2[,c("ENTREZ_GENE_ID","InteractorAD_gene_id")]
))


#Get edgotypes
cat("Querying for edgotype classes...\n")

edgotypes <- read.delim(pipe("tdbquery --loc=tdb/ --query=queries/alleleTypes.sparql --results=TSV"))
colnames(edgotypes) <- c("entrez", "mut", "numMaintain", "numInterrupt")
edgotypes <- edgotypes[apply(edgotypes[,c("numMaintain","numInterrupt")] > 0, 1, any),]
edgotypes[,"entrez"] <- sapply(as.vector(edgotypes[,"entrez"]), function (x) substr(x, 1,nchar(x)-43))
edgotypes[,"mut"] <- sapply(as.vector(edgotypes[,"mut"]), function (x) substr(x, 37,nchar(x)-1))
edgotypes[,"numTotal"] <- apply(edgotypes, 1, function(x) sum(as.numeric(x[c(3,4)])))
edgotypes[,"classification"] <- apply(edgotypes[,c(3:5)], 1, function (x) {
	if (x[1] == x[3]) {
		"pseudoWT"
	} else if (x[2] == x[3]) {
		"pseudoNull"
	} else {
		"edgetic"
	}
})


#Load expression data
# if (file.exist("input/exprs.tsv")) {

	cat("Loading pre-processed expression data...\n")

	exprs <- read.delim("input/exprs.tsv")

# } else {

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
# }

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
rio$draw("correlations",function() {
	hist(
		random.pairs$corr, 
		col=my.colors["grey"],
		freq=FALSE,
		xlim=c(-1,1),
		main="Histogram of correlations",
		xlab="Pearson correlation coefficient",
		border=NA,
		breaks=40
	)
	hist(
		interactions$corr, 
		col=my.colors["red"], 
		freq=FALSE, 
		breaks=40,
		border=NA,
		add=TRUE
	)
	legend("topleft",c("Interacting pairs", "Random pairs"),fill=my.colors)
})

cat("Writing correlation table to file...\n")
write.table(interactions, file="interaction_correlations.tsv", sep="\t", row.names=FALSE, quote=FALSE)


cat("Computing mean expression correlations for each node...\n")
#Compute hubs and their average PCCs
interactors <- unique(append(interactions[,1], interactions[,2]))
nodePCCs <- NULL
for (interactor in interactors) {
	rows <- (interactions[,1] == interactor) | (interactions[,2] == interactor)
	meanPCC <- mean(interactions[rows,"corr"], na.rm=TRUE)
	nodePCCs <- rbind(nodePCCs,c(interactor, meanPCC))
}
rownames(nodePCCs) <- nodePCCs[,1]

#Compute hubs
node.degrees <- table(append(interactions[,1], interactions[,2]))
hubs <- names(which(node.degrees > 5))

cat("Drawing histogram...\n")
rio$draw("nodePCCs", function() {
	hist(
		nodePCCs[hubs,2],
		col="orange",
		border=NA,
		main="Histogram of mean PCCs for hubs",
		xlab="PCC"
	)
})


#Compute susceptibility to edgetic mutations for nodes
cat("Computing edgeticness of nodes...\n")
genes <- unique(edgotypes$entrez)
geneSusceptibility <- NULL
for (gene in genes) {
	mat <- edgotypes[edgotypes$entrez == gene,c("numMaintain","numInterrupt")] == 0
	edgetic <- !apply(mat,1,any)
	susceptibility <- sum(edgetic) / length(edgetic)
	geneSusceptibility <- rbind(geneSusceptibility, c(gene, susceptibility))
}
geneSusceptibility <- apply(geneSusceptibility, c(1,2), as.numeric)

#Combine data
pccVsEdgetic <- na.omit(merge(nodePCCs, geneSusceptibility, by=1, all=F))
rownames(pccVsEdgetic) <- pccVsEdgetic[,1]
pccVsEdgetic[,1] <- NULL
colnames(pccVsEdgetic) <- c("pcc","edgeticness")

#plot
cat("Plotting expression correlation vs edgeticness...\n")
rio$draw("pccVsEdgetic", function() {
	plot(
		pccVsEdgetic[hubs,],
		xlab="mean expression corr. with interactors",
		ylab="share of edgetic mutations",
		xlim=c(-1,1),
		col="orange"
	)
})

rio$draw("pccVsEdgetic_histo", function() {
	hist(
		na.omit(pccVsEdgetic[(pccVsEdgetic$edgeticness == 0),][hubs,"pcc"]),
		col=my.colors["grey"],
		freq=FALSE,
		xlab="Mean expression corr. with interactors",
		main="Date/Party vs Edgeticness",
		xlim=c(-1,1),
		border=NA
	)
	hist(
		na.omit(pccVsEdgetic[(pccVsEdgetic$edgeticness > 0),][hubs, "pcc"]),
		add=TRUE,
		col=my.colors["red"],
		freq=FALSE,
		border=NA
	)
	legend("topleft",c("Edgetic", "Non-edgetic"),fill=my.colors)
})

counts <- data.frame()
counts["non-edgetic","party"] <- sum((pccVsEdgetic$edgeticness == 0) & (pccVsEdgetic$pcc <= .5))
counts["edgetic","party"] <- sum((pccVsEdgetic$edgeticness > 0) & (pccVsEdgetic$pcc <= .5))
counts["sum","party"] <- sum(counts[1:2,"party"])
counts["non-edgetic","date"] <- sum((pccVsEdgetic$edgeticness == 0) & (pccVsEdgetic$pcc > .5))
counts["edgetic","date"] <- sum((pccVsEdgetic$edgeticness > 0) & (pccVsEdgetic$pcc > .5))
counts["sum", "date"] <- sum(counts[1:2,"date"])
counts["non-edgetic","sum"] <- sum(counts["non-edgetic",1:2])
counts["edgetic","sum"] <- sum(counts["edgetic",1:2])
counts["sum", "sum"] <- sum(counts[1:2,"sum"])

counts

counts/counts["sum","sum"]

pval <- phyper(
	counts["edgetic","party"]-1, 
	counts["sum","party"], 
	counts["sum","date"], 
	counts["edgetic","sum"],
	lower.tail=FALSE
)
cat("\nHypergeometric test for enrichment of edgeticness in date nodes: P =",pval,"\n\n")



#Are date edges (low correlation) more likely to be interrupted (large share of associated mutations is interupting)

cat("Querying for allele pos/neg allele counts for each interaction...\n")
alleleCounts <- read.delim(pipe("tdbquery --loc=output/ --query=queries/posNegCounts4Interactions.sparql --results=TSV"))
colnames(alleleCounts) <- c("entrezA", "entrezB", "numMaintain", "numInterrupt")
alleleCounts <- alleleCounts[apply(alleleCounts[,c("numMaintain","numInterrupt")] > 0, 1, any),]
alleleCounts[,"entrezA"] <- sapply(as.vector(alleleCounts[,"entrezA"]), function (x) substr(x, 1,nchar(x)-43))
alleleCounts[,"entrezB"] <- sapply(as.vector(alleleCounts[,"entrezB"]), function (x) substr(x, 1,nchar(x)-43))
alleleCounts[,"numTotal"] <- apply(alleleCounts, 1, function(x) sum(as.numeric(x[c(3,4)])))
alleleCounts[,"interruptionShare"] <- apply(alleleCounts, 1, function(x) as.numeric(x[4]) / as.numeric(x[5]))

cat("Drawing histogram...\n")
rio$draw("interruptionShares",function() {
	hist(
		alleleCounts$interruptionShare, 
		main="Histogram of interruption shares", 
		xlab="Share of interrupting mutations per edge"
	)
})

corrs <- data.frame(
	ia=paste(interactions[,1],interactions[,2], sep="_"),
	corr=interactions[,3]
)

interruption <- data.frame(
	ia=paste(alleleCounts[,1], alleleCounts[,2], sep="_"),
	interruptionShare=alleleCounts[,6]
)

corrsVsInterruption <- na.omit(merge(corrs, interruption, by=1))


cat("Drawing plot...\n")
rio$draw("corrsVsInterruption", function() {
	plot(
		corrsVsInterruption[,2],
		jitter(corrsVsInterruption[,3]),
		xlab="expression PCC",
		ylab="share of interrupting mutations",
		col="orange"
	)
})

cat("Drawing histograms...\n")
rio$draw("corrsVsInterruptionHist",function() {
	hist(
		corrsVsInterruption[(corrsVsInterruption[,3] == 0),2],
		col=my.colors["grey"],
		freq=FALSE,
		xlab="expression PCC",
		main="V-edges expression PCCs",
		xlim=c(-1,1),
		border=NA
	)
	hist(
		corrsVsInterruption[(corrsVsInterruption[,3] > 0),2],
		add=TRUE,
		col=my.colors["red"],
		freq=FALSE,
		border=NA
	)
	legend("topleft",c("Edges with interrupting alleles", "Edges w/o interrupting alleles"),fill=my.colors)
})

