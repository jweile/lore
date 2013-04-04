#!/usr/bin/Rscript

file.exist <- function(filename) {
	system(paste("[ -e",filename,"]")) == 0
}

my.colors <- c(red=rgb(1,0,0,.5), grey=rgb(.5,.5,.5,.5))

parblack <- function() {
	# par(
	# 	bg="black",
	# 	fg="white",
	# 	col="white",
	# 	col.axis="white",
	# 	col.lab="white",
	# 	col.main="white",
	# 	col.sub="white"
	# )
}

timestamp <- format(Sys.time(),format='%Y-%m-%d_%H:%M:%S')
out.dir <- paste("results/",timestamp,"_alleleTypes/", sep="")
dir.create(out.dir, mode="0755")

draw <- function(name, f, dim=c(1,1)) {

	pdf(paste(out.dir,name,".pdf",sep=""), height=dim[1]*7, width=dim[2]*7)
	op <- par(mfrow=dim)
	f()
	par(op)
	dev.off()

	svg(paste(out.dir,name,".svg",sep=""), height=dim[1]*7, width=dim[2]*7)
	op <- par(mfrow=dim)
	f()
	par(op)
	dev.off()
}

cat("Executing query...\n")

edgotypes <- read.delim(pipe("tdbquery --loc=tdb/ --query=queries/alleleTypes.sparql --results=TSV"))
colnames(edgotypes) <- c("entrez", "mut", "numMaintain", "numInterrupt")
edgotypes <- edgotypes[apply(edgotypes[,c("numMaintain","numInterrupt")] > 0, 1, any),]
edgotypes[,"entrez"] <- sapply(as.vector(edgotypes[,"entrez"]), function (x) substr(x, 1,nchar(x)-43))
edgotypes[,"mut"] <- sapply(as.vector(edgotypes[,"mut"]), function (x) substr(x, 37,nchar(x)-1))
edgotypes[,"numTotal"] <- apply(edgotypes, 1, function(x) sum(as.numeric(x[c(3,4)])))
# edgotypes[,"classification"] <- apply(edgotypes[,c(3:5)], 1, function (x) {
# 	if (x[1] == x[3]) {
# 		"pseudoWT"
# 	} else if (x[2] == x[3]) {
# 		"pseudoNull"
# 	} else {
# 		"edgetic"
# 	}
# })


subset <- edgotypes[(edgotypes$numTotal > 1),]
share <- subset$numMaintain / subset$numTotal

draw("deg_distr_real", function() {
	hist(
		edgotypes$numTotal,
		xlab="Node degree",
		main="",
		col="orange",
		border=NA,
		freq=FALSE
	)
})

numEdgetic <- sum((subset$numInterrupt > 0) & (subset$numMaintain > 0))
numPN <- sum(subset$numMaintain == 0)
numPWT <- sum(subset$numInterrupt == 0)

out <- c(pseudo.null=numPN, edgetic=numEdgetic, pseudo.wt=numPWT)
out <- rbind(num=out,percent=100*out/nrow(subset))

outfile <- paste(out.dir,"alleleTypes.txt",sep="")
write.table(out, outfile, quote=FALSE, sep="\t")

print(out)
cat("#alleles:",sum(out[1,]),"\n")
cat("#genes",length(unique(edgotypes[,1])),"\n")


draw("fractions_real", function() {
	hist(
		share, 
		xlab="Fraction of maintained edges",
		main="",
		col="orange",
		border=NA,
		freq=FALSE,
		breaks=20
	)
	barplot(
		out["percent",],
		col="orange",
		border=NA,
		ylab="%"
	)
}, dim=c(1,2))



