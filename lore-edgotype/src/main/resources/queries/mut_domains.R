#!/usr/bin/Rscript

data <- read.delim(pipe("tdbquery --loc=output/ --query=queries/mut_domains.sparql --results=TSV"), header=TRUE)

data[,1] <- sapply(as.vector(data[,1]), function (x) substr(x, 1,nchar(x)-43))
data[,2] <- sapply(as.vector(data[,2]), function (x) substr(x, 37,nchar(x)-1))
data[,3] <- sapply(as.vector(data[,3]), function (x) x != "<http://llama.mshri.on.ca/lore-interaction.owl#affectsPositively>")
data[,4] <- sapply(as.vector(data[,4]), function (x) substr(x, 1,nchar(x)-43))

data[,"within"] <- (data[,"X.position"] > data[,"X.start"]) & (data[,"X.position"] < data[,"X.end"])

counts <- data.frame()
counts["interrupt","within"] <- sum(data[, "X.effect"] & data[, "within"])
counts["maintain","within"] <- sum((!data[, "X.effect"]) & data[, "within"])
counts["sum","within"] <- sum(counts[1:2,"within"]);

counts["interrupt","not within"] <- sum(data[, "X.effect"] & (!data[, "within"]))
counts["maintain","not within"] <- sum((!data[, "X.effect"]) & (!data[, "within"]))
counts["sum","not within"] <- sum(counts[1:2,"not within"]);

counts["interrupt","sum"] <- sum(counts["interrupt",1:2])
counts["maintain","sum"] <- sum(counts["maintain",1:2])
counts["sum","sum"] <- sum(counts["sum",1:2])

counts.percent <- signif(100 * counts/nrow(data),2)

out <- matrix(paste(as.matrix(counts), " (", as.matrix(counts.percent), "%)",sep=""), ncol=3)
colnames(out) <- colnames(counts)
rownames(out) <- rownames(counts)

out

pval <- phyper(
	counts["interrupt","within"]-1, 
	counts["sum","within"], 
	counts["sum","not within"], 
	counts["interrupt","sum"],
	lower.tail=FALSE
)
cat("\nHypergeometric test for enrichment of interruption in interfaces: P =",pval,"\n\n")



#draw heatmap
library(gplots)

hm.data <- apply(data[,c("X.effect","within")], c(1,2), as.numeric)
rownames(hm.data) <- data[,"X.mutation"]
pdf("matches.pdf",width=7,height=14)
heatmap.2(
	hm.data, 
	col=c("orange", "green"),
	Rowv=NA, Colv=NA,
	key=FALSE,
	keysize=0.1,
	trace="none"
)
dev.off()
