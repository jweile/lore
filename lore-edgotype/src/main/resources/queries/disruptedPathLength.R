maintained <- read.delim("dist_maintained.txt",header=FALSE)[,1]
disrupted <- read.delim("dist_disrupted.txt",header=FALSE)[,1]
maintainedRandom <- read.delim("dist_maintained_random.txt",header=FALSE)[,1]
disruptedRandom <- read.delim("dist_disrupted_random.txt",header=FALSE)[,1]

maintainedDegree <- read.delim("dist_maintained_degree.txt",header=FALSE)[,1]
disruptedDegree <- read.delim("dist_disrupted_degree.txt",header=FALSE)[,1]

#infinity values are encoded as "-1" in the output file
maintained[maintained < 0] <- Inf
disrupted[disrupted < 0] <- Inf
maintainedRandom[maintainedRandom < 0] <- Inf
disruptedRandom[disruptedRandom < 0] <- Inf

# means <- c(mean(maintained),mean(disrupted))
# stdev <- sqrt(c(var(maintained),var(disrupted)))

# wilcox.test(disrupted,maintained,alternative="l")
# wilcox.test(disrupted,disruptedRandom,alternative="l")
# wilcox.test(disruptedRandom,maintainedRandom,alternative="l")


library(coin)

g <- factor(c(rep("disrupted", length(disrupted)), rep("maintained", length(maintained))))
v <- c(disrupted, maintained)
wx <- wilcox_test(v ~ g, alternative="l")
wx
cat("r =",statistic(wx)/sqrt(length(disrupted)+length(maintained)),"\n")

g <- factor(c(rep("disrupted", length(disrupted)), rep("disruptedRandom", length(disruptedRandom))))
v <- c(disrupted, disruptedRandom)
wx <- wilcox_test(v ~ g, alternative="l")
wx
cat("r =",statistic(wx)/sqrt(length(disrupted)+length(maintained)),"\n")

g <- factor(c(rep("disruptedRandom", length(disruptedRandom)), rep("maintainedRandom", length(maintainedRandom))))
v <- c(disruptedRandom, maintainedRandom)
wx <- wilcox_test(v ~ g, alternative="l")
wx
cat("r =",statistic(wx)/sqrt(length(disrupted)+length(maintained)),"\n")

g <- factor(c(rep("disruptedDegree", length(disruptedDegree)), rep("maintainedDegree", length(maintainedDegree))))
v <- c(disruptedDegree, maintainedDegree)
wx <- wilcox_test(v ~ g, alternative="g")
wx
cat("r =",statistic(wx)/sqrt(length(disrupted)+length(maintained)),"\n")



tab.m <- table(maintained)
tab.d <- table(disrupted)
tab.mr <- table(maintainedRandom)
tab.dr <- table(disruptedRandom)

labels <- Reduce(union,list(names(tab.m),names(tab.d),names(tab.mr),names(tab.dr)))
labels <- labels[order(as.numeric(labels))]
tab.abs <- do.call(cbind,lapply(labels, function(name) {
	c(
		ifelse(name %in% names(tab.m), tab.m[[name]], 0), 
		ifelse(name %in% names(tab.d), tab.d[[name]], 0),
		ifelse(name %in% names(tab.mr), tab.mr[[name]], 0), 
		ifelse(name %in% names(tab.dr), tab.dr[[name]], 0)
	)
}))
tab <- do.call(cbind,lapply(labels, function(name) {
	c(
		ifelse(name %in% names(tab.m), tab.m[[name]]/length(maintained), 0), 
		ifelse(name %in% names(tab.d), tab.d[[name]]/length(disrupted), 0),
		ifelse(name %in% names(tab.mr), tab.mr[[name]]/length(maintainedRandom), 0), 
		ifelse(name %in% names(tab.dr), tab.dr[[name]]/length(disruptedRandom), 0)
	)
}))
colnames(tab) <- labels
rownames(tab) <- c("maintained","disrupted","maintained_random","disrupted_random")
colnames(tab.abs) <- labels
rownames(tab.abs) <- c("maintained","disrupted","maintained_random","disrupted_random")


fisher.tables <- lapply(0:3, function(cutoff) {
	rbind(equal=c(
			perturbed=sum(tab.abs["disrupted",as.numeric(labels) <= cutoff]),
			unperturbed=sum(tab.abs["maintained",as.numeric(labels) <= cutoff])
		),
		greater=c(
			perturbed=sum(tab.abs["disrupted",as.numeric(labels) > cutoff]),
			unperturbed=sum(tab.abs["maintained",as.numeric(labels) > cutoff])
		)
	)
})
names(fisher.tables) <- 0:3

fisher.tables
for (cutoff in as.character(0:3)) {
	print(fisher.test(fisher.tables[[cutoff]],alternative="greater"))
}



pdf("diseasePathLength.pdf",width=14,height=7)

op <- par(mfrow=c(1,2))

colors <- c("steelblue1","goldenrod1","steelblue4","goldenrod4")
xs <- barplot(tab,beside=TRUE,col=colors, xlab="Length of shortest path", ylab="Density",ylim=c(0,.6))
legend("right",c("maintained","disrupted","maintained random","disrupted random"),fill=colors)

median.index.m <- which(as.numeric(colnames(tab)) == median(maintained))
arrows(xs[1,median.index.m], y0=tab[1,median.index.m]+.05,y1=tab[1,median.index.m]+.03,length=.05,lwd=2,col=colors[1])

median.index.d <- which(as.numeric(colnames(tab)) == median(disrupted))
arrows(xs[2,median.index.d], y0=tab[2,median.index.d]+.05,y1=tab[2,median.index.d]+.03,length=.05,lwd=2,col=colors[2])

median.index.mr <- which(as.numeric(colnames(tab)) == median(maintainedRandom))
arrows(xs[3,median.index.mr], y0=tab[3,median.index.mr]+.05,y1=tab[3,median.index.mr]+.03,length=.05,lwd=2,col=colors[3])

median.index.dr <- which(as.numeric(colnames(tab)) == median(disruptedRandom))
arrows(xs[4,median.index.dr], y0=tab[4,median.index.dr]+.05,y1=tab[4,median.index.dr]+.03,length=.05,lwd=2,col=colors[4])

n <- c(sum(tab.m), sum(tab.d), sum(tab.mr), sum(tab.dr))
for (i in 1:4) {
	se <- sqrt(tab[i,] * (1-tab[i,]) / n[i])
	arrows(
		xs[i,],
		y0=tab[i,]+se/2, 
		y1=tab[i,]-se/2,
		length=.03,
		angle=90,
		code=3
	)
}

plot(0,type="n",xlab="Length of shortest path", ylab="Density",xlim=c(0,8), ylim=c(0,.6))
for (i in 1:4) {
	points(as.numeric(colnames(tab)),tab[i,],type="l",col=colors[i])
	se <- sqrt(tab[i,] * (1-tab[i,]) / n[i])
	arrows(
		as.numeric(colnames(tab)),
		y0=tab[i,]+se/2, 
		y1=tab[i,]-se/2,
		length=.03,
		angle=90,
		code=3,
		col=colors[i]
	)
}
legend("right",c("maintained","disrupted","maintained random","disrupted random"),lty=1,col=colors)

arrows(median(maintained), y0=tab[1,median.index.m]+.05,y1=tab[1,median.index.m]+.03,length=.05,lwd=2,col=colors[1])
arrows(median(disrupted), y0=tab[2,median.index.d]+.05,y1=tab[2,median.index.d]+.03,length=.05,lwd=2,col=colors[2])
arrows(median(maintainedRandom), y0=tab[3,median.index.mr]+.05,y1=tab[3,median.index.mr]+.03,length=.05,lwd=2,col=colors[3])
arrows(median(disruptedRandom), y0=tab[4,median.index.dr]+.05,y1=tab[4,median.index.dr]+.03,length=.05,lwd=2,col=colors[4])


par(op)

dev.off()


#ANALYZE DEGREES


tab.md <- table(maintainedDegree)
tab.dd <- table(disruptedDegree)

labels <- union(names(tab.md),names(tab.dd))
labels <- labels[order(as.numeric(labels))]
tab <- do.call(cbind,lapply(labels, function(name) {
	c(
		ifelse(name %in% names(tab.md), tab.md[[name]]/length(maintainedDegree), 0), 
		ifelse(name %in% names(tab.dd), tab.dd[[name]]/length(disruptedDegree), 0)
	)
}))
colnames(tab) <- labels
rownames(tab) <- c("maintainedDegree","disruptedDegree")


pdf("dpl_degree.pdf")
plot(0,type="n",xlab="degree of neighbour", ylab="Cumulative Distribution",xlim=c(1,200), ylim=c(0,1),log="x")
for (i in 1:2) {
	points(as.numeric(colnames(tab)),sapply(1:ncol(tab),function(j) sum(tab[i,1:j])),type="p",pch=20,col=colors[i])
}
legend("right",c("maintained","disrupted"),pch=20,col=colors)
dev.off()














