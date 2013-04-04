#!/usr/bin/Rscript

timestamp <- format(Sys.time(),format='%Y-%m-%d_%H:%M:%S')
out.dir <- paste("results/",timestamp,"_inheritance/", sep="")
dir.create(out.dir, mode="0755")

data <- read.delim(pipe("tdbquery --loc=tdb/ --query=queries/inheritance.sparql --results=TSV"))
colnames(data) <- c("allele", "disease", "inheritance", "numMaintain", "numInterrupt")

#Filter out zeros
data <- data[apply(data[,c("numMaintain","numInterrupt")] > 0, 1, any),]

#Clean up SPARQL syntax
data[,"disease"] <- sapply(as.vector(data[,"disease"]), function (x) substr(x, 1,nchar(x)-43))
data[,"allele"] <- sapply(as.vector(data[,"allele"]), function (x) substr(x, 18,nchar(x)-1))

#Call edgotypes
data[,"classification"] <- apply(data[,c(4:5)], 1, function (x) {
	if (x[2] == 0) {
		"pseudoWT"
	} else if (x[1] == 0) {
		"pseudoNull"
	} else {
		"edgetic"
	}
})

#Summarize various modes into dominant and recessive 
data$inheritance <- sapply(data$inheritance, function (x) {
	if (x == "autosomal dominant" || x == "dominant" || x == "x-linked dominant") {
		"dominant"
	} else if (x == "autosomal recessive" || x == "recessive" || x == "x-linked recessive") {
		"recessive"
	} else {
		NA
	}
})
data <- na.omit(data)

#Make contingency table
contingencies <- table(data[,c("inheritance","classification")])

#Fisher's exact test
fisher.test(contingencies, alternative="greater")

#compute sums
contingencies <- rbind(contingencies,sum=apply(contingencies,2,sum))
contingencies <- cbind(contingencies,sum=apply(contingencies,1,sum))

#compute percentages
percentages <- 100 * contingencies / contingencies["sum","sum"]

#Cobine contingencies and percentages in one table
out <- array(c(contingencies,percentages),append(dim(contingencies),2))
out <- apply(out,c(1,2),function(x) paste(paste(x,collapse=" ("),"%)",sep=""))
dimnames(out) <- dimnames(contingencies)

out

file <- paste(out.dir,"inheritance_counts.txt",sep="")
write.table(out,file=file,sep="\t",quote=FALSE)




