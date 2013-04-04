#!/usr/bin/Rscript

# Analyze disease data

source("queries/resultio.R")
rio <- init.result.io("seqdist")

data <- read.delim("seqDist_disease.tsv",header=FALSE)

# parblack <- function() par(
# 	# bg="black",
# 	# fg="white",
# 	# col="white",
# 	# col.axis="white",
# 	# col.lab="white",
# 	# col.main="white",
# 	# col.sub="white"
# )

# timestamp <- format(Sys.time(),format='%Y-%m-%d_%H:%M:%S')
# out.dir <- paste("results/",timestamp,"_seqdist/", sep="")
# dir.create(out.dir, mode="0755")

# draw <- function(name, f) {

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

s0 <- sum(data[,2] == 0)
s1 <- sum(data[,2] == 1)

#print numbers on common / not common diseases
cat("no common diseases: ",s0, ", common diseases: ",s1,"\n")

cat("Max distance: ",max(data[,1]),"\n")


colors <- c(
	rgb(1, 0, 0,0.5),#Transparent red
	rgb(0, 1, 0,0.5) #Transparent green
)
breaks <- (0:700)*50
cutoff <- 3000

common <- data[(data[,2] == 1),1]
non.common <- data[(data[,2] == 0),1]

common.dens <- hist(
	common, 
	freq=FALSE, 
	breaks=breaks,
	plot=FALSE
)$intensities

non.common.dens <- hist(
	non.common, 
	freq=FALSE, 
	breaks=breaks,
	plot=FALSE
)$intensities

#plot histograms
rio$draw("seqDist_disease", function() {
	plot(NULL,
		xlim=c(0,cutoff),
		ylim=c(0,max(c(common.dens,non.common.dens))),
		main="Common diseases vs distance",
		xlab="Distance between mutations",
		ylab="Relative frequency of pairs"
	)
	lines(breaks[-length(breaks)], common.dens, col="orange")
	lines(breaks[-length(breaks)], non.common.dens, col="gray")
	legend("topright",c("No common disease", "Common diseases"),col=c("gray","orange"),lwd=1)
})

ks.test(common, non.common, alternative="less")

####
#Analyze edogtyping data

data <- read.delim("seqDist_edgotype.tsv", header=FALSE)

rio$draw("seqDist_edgotype", function() {
	plot(
		data[,1], jitter(data[,2], amount=.01),
		main="Edgotype similarity vs distance",
		xlab="Sequence distance between mutations",
		ylab="Edgotype similarity (Jaccard)",
		col="orange"
	)
})

eq <- data[(data[,2] == 1),1]
neq <- data[(data[,2] != 1),1]

# common.dens <- hist(
# 	common, 
# 	freq=FALSE, 
# 	breaks=breaks,
# 	plot=FALSE
# )$intensities

# non.common.dens <- hist(
# 	non.common, 
# 	freq=FALSE, 
# 	breaks=breaks,
# 	plot=FALSE
# )$intensities

breaks <- seq(0,550,50)

eq.dens <- hist(
	eq, 
	freq=FALSE,
	breaks=breaks,
	plot=FALSE
)$intensities
neq.dens <- hist(
	neq,
	freq=FALSE,
	breaks=breaks,
	plot=FALSE
)$intensities
	

rio$draw("seqDist_edgotype2", function() {
	plot(
		NULL,
		xlim=c(0,max(breaks)),
		ylim=c(0,max(c(eq.dens,neq.dens))),
		main="Edgotype similarity vs distance",
		xlab="Distance between mutations",
		ylab="Relative frequency of pairs"
	)
	lines(breaks[-length(breaks)],eq.dens, col="orange")
	lines(breaks[-length(breaks)], neq.dens, col="gray")
	legend("topright",c("Different edgotypes", "Equal edgotypes"),col=c("gray","orange"),lwd=1)
})

ks.test(eq,neq, alternative="less")

cat("equal edgotypes: ",sum(data[,2] == 1), ", non-equal edgotypes: ",sum(data[,2] != 1),"\n")
