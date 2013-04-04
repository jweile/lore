#!/usr/bin/Rscript

source("queries/graphs2.R")

dbreaks <- .05*(0:20)
densities <- function(x) {
	hist(x, breaks=dbreaks, plot=FALSE)$density
}
distance <- function(x,y) {
	if (length(x) != length(y)) {
		stop("must be of equal length")
	}
	sqrt(sum((x-y)^2))
}

##### LOAD REAL DATA #####

cat("Querying triplestore database...\n")

edgotypes <- read.delim(pipe("tdbquery --loc=tdb/ --query=queries/alleleTypes.sparql --results=TSV"))
colnames(edgotypes) <- c("entrez", "mut", "numMaintain", "numInterrupt")
edgotypes <- edgotypes[apply(edgotypes[,c("numMaintain","numInterrupt")] > 0, 1, any),]
edgotypes[,"entrez"] <- sapply(as.vector(edgotypes[,"entrez"]), function (x) substr(x, 1,nchar(x)-43))
edgotypes[,"mut"] <- sapply(as.vector(edgotypes[,"mut"]), function (x) substr(x, 37,nchar(x)-1))
edgotypes[,"numTotal"] <- apply(edgotypes, 1, function(x) sum(as.numeric(x[c(3,4)])))

subset <- edgotypes[(edgotypes$numTotal > 1),]
frac.real <- subset$numMaintain / subset$numTotal

dens.real <- densities(frac.real)


#### GENERATE SIMULATED GRAPH ####

n <- 1000

g <- new.graph()
g$pref.attachm(n)
gs <- screen(g)

models <- make.models()

# Draw degree distribution histograms
pdf("deg_dist.pdf")
my.cols <- c(red=rgb(1,0,0,.5), green=rgb(0,1,0,.5))
hist(g$deg.dist(), 
	col=my.cols['green'], 
	border=NA, 
	breaks=50,
	freq=FALSE,
	main="Degree distributions",
	xlab="Node degree"
)
hist(gs$deg.dist(), 
	col=my.cols['red'], 
	border=NA,
	freq=FALSE, 
	add=TRUE
)
legend("right",c("screened","true"),fill=my.cols)
dev.off()


##### TEST MONOMODAL MODEL #####

cat("\n\nTesting monomodal model...\n")
edgo <- make.edgotypes(g, sim=models$monomodal)

frac.sim <- edgo$frac.maintained(gs)
dens.sim <- densities(frac.sim)

d <- distance(dens.real, dens.sim)
c(distance=d)

###### PARAMETER FIT: BIMODAL MODEL ######

cat("\n\nTesting bimodal model...\n")

dist.bimodal <- data.frame()

ps <- seq(0.4, 0.9, 0.05)
qs <- seq(0.1, 0.4, 0.025)

pb <- txtProgressBar(max=length(ps)*length(qs),style=3)
progr <- 0

for (p in ps) {
	models$set.bm.p(p)
	for (q in qs) {
		models$set.bm.q(q)

		edgo <- make.edgotypes(g, sim=models$bimodal)

		frac.sim <- edgo$frac.maintained(gs)
		dens.sim <- densities(frac.sim)
		d <- distance(dens.real, dens.sim)

		dist.bimodal[as.character(p),as.character(q)] <- d

		progr <- progr + 1
		setTxtProgressBar(pb,progr)
	}
}

close(pb)

pdf("bimodal_fit.pdf")
image(
	as.matrix(dist.bimodal), 
	axes=FALSE, 
	xlab="p", ylab="q",
	main="Parameter fit: Bimodal"
	)
axis(1, at=seq(0,1,length.out=length(ps)), labels=ps)
axis(2, at=seq(0,1,length.out=length(qs)), labels=qs)
dev.off()

min.p <- names(which.min(apply(dist.bimodal, 1, min)))
min.q <- names(which.min(dist.bimodal[min.p,]))

bimodal.result <- c(
	min.p=as.numeric(min.p), 
	min.q=as.numeric(min.q), 
	best=dist.bimodal[min.p,min.q])
print(bimodal.result)



##### PARAMETER FIT: TRIMODAL MODEL #####

cat("Testing trimodal model...\n")

dist.trimodal <- data.frame()

ps <- seq(0.2, 0.5, 0.025)
qs <- seq(0.2, 0.5, 0.025)

pb <- txtProgressBar(max=length(ps)*length(qs),style=3)
progr <- 0

for (p in ps) {
	models$set.tm.p(p)
	for (q in qs) {
		models$set.tm.q(q)

		edgo <- make.edgotypes(g, sim=models$trimodal)

		frac.sim <- edgo$frac.maintained(gs)
		dens.sim <- densities(frac.sim)
		d <- distance(dens.real, dens.sim)

		dist.trimodal[as.character(p),as.character(q)] <- d

		progr <- progr + 1
		setTxtProgressBar(pb,progr)
	}
}

close(pb)

pdf("trimodal_fit.pdf")
image(
	as.matrix(dist.trimodal), 
	axes=FALSE, 
	xlab="p", ylab="q",
	main="Parameter fit: Trimodal"
	)
axis(1, at=seq(0,1,length.out=length(ps)), labels=ps)
axis(2, at=seq(0,1,length.out=length(qs)), labels=qs)
dev.off()

min.p <- names(which.min(apply(dist.trimodal, 1, min)))
min.q <- names(which.min(dist.trimodal[min.p,]))
trimodal.result <- c(
	min.p=as.numeric(min.p), 
	min.q=as.numeric(min.q), 
	best=dist.trimodal[min.p,min.q]
)
print(trimodal.result)



#### RUN BEST FITS ####

#bimodal.result <- c(min.p=0.7, min.q=0.15)
#trimodal.result <- c(min.p=0.3, min.q=0.4)
#models <- make.models()

cat("Averaging best fits...\n")

models$set.bm.p(bimodal.result["min.p"])
models$set.bm.q(bimodal.result["min.q"])
models$set.tm.p(trimodal.result["min.p"])
models$set.tm.q(trimodal.result["min.q"])


monomodal.densities <- NULL
bimodal.densities <- NULL
trimodal.densities <- NULL
trimodal.shares <- NULL
for (i in 1:10) {

	cat("\nIteration",i,"\n")

	g <- new.graph()
	g$pref.attachm(n)
	gs <- screen(g)

	cat(" -> Running monomodal model...\n")
	edgo <- make.edgotypes(g, sim=models$monomodal)
	frac.sim <- edgo$frac.maintained(gs)
	monomodal.densities <- rbind(monomodal.densities, densities(frac.sim))

	cat(" -> Running bimodal model...\n")
	edgo <- make.edgotypes(g, sim=models$bimodal)
	frac.sim <- edgo$frac.maintained(gs)
	bimodal.densities <- rbind(bimodal.densities, densities(frac.sim))

	cat(" -> Running trimodal model...\n")
	edgo <- make.edgotypes(g, sim=models$trimodal)
	frac.sim <- edgo$frac.maintained(gs)
	trimodal.densities <- rbind(trimodal.densities, densities(frac.sim))

	cat(" -> Predicting...\n")
	frac.sim <- edgo$frac.maintained(g)
	counts <- c(
		pseudo.null=sum(frac.sim == 0),
		edgetic=sum(frac.sim > 0 & frac.sim < 1),
		pseudo.wt=sum(frac.sim==1)
	)
	trimodal.shares <- rbind(trimodal.shares, counts/length(frac.sim))

}

means <- rbind(
	monomodal=apply(monomodal.densities,2,mean),
	bimodal=apply(bimodal.densities,2,mean),
	trimodal=apply(trimodal.densities,2,mean),
	real=dens.real
)

stds <- sqrt(rbind(
	monomodal=apply(monomodal.densities,2,var),
	bimodal=apply(bimodal.densities,2,var),
	trimodal=apply(trimodal.densities,2,var),
	real=rep(0,length(dens.real))
))

error.bar <- function(x, y, upper, lower=upper, length=0.1,...) {
	if(length(x) != length(y) | length(y) !=length(lower) | length(lower) != length(upper))
	stop("vectors must be same length")
	arrows(x,y+upper, x, y-lower, angle=90, code=3, length=length, ...)
}

pdf("average_fits.pdf")
my.cols <- c(rgb(.3,.3,.3),rgb(.5,.5,.5),rgb(.7,.7,.7),"orange")
x <- barplot(
	means,
	beside=TRUE,
	names.arg=dbreaks[-1],
	col=my.cols,
	xlab="Fraction of maintained edges",
	ylab="Density at 0.05 interval size"
)
error.bar(x,means,stds,length=.03)
legend("topleft",row.names(means),fill=my.cols)
dev.off()


##### CALCULATE BACK to expected real values ####

real.counts <- c(
	pseudo.null=sum(frac.real == 0),
	edgetic=sum(frac.real > 0 & frac.real < 1),
	pseudo.wt=sum(frac.real == 1)
)
real.shares <- real.counts / length(frac.real)

predict.means <- apply(trimodal.shares,2,mean)
predict.stds <- sqrt(apply(trimodal.shares,2,var))

to.plot <- rbind(real.shares, predict.means)

pdf("prediction.pdf")
my.cols <- c("orange","gray")
x <- barplot(
	to.plot,
	beside=TRUE,
	ylab="Share of alleles",
	col=my.cols
)
error.bar(x, to.plot, rbind(c(0,0,0),predict.stds))
legend("topleft",c("screen","extrapolation"),fill=my.cols)
dev.off()

