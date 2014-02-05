#!/usr/bin/Rscript

process <- function(con) {
	tab <- apply(do.call(rbind,strsplit(readLines(con,4),"\t")),c(1,2),as.numeric)
	cumulative <- do.call(rbind,lapply(1:4, function(i) c(sum(tab[1:i,1]),sum(tab[1:i,2]))))
	ratio <- apply(cumulative,1, function(r) r[2]/r[1])
	return(cbind(tab,cumulative,ratio))
}

infile <- file("diseaseShells.txt",open="r")
maintained <- list()
disrupted <- list()
while (length(line <- readLines(infile,1)) > 0) {
	if (substr(line,1,10) == "maintained") {
		maintained[[length(maintained)+1]] <- process(infile)
	} else if (substr(line,1,9) == "disrupted") {
		disrupted[[length(disrupted)+1]] <- process(infile)
	}
}
close(infile)

score <- function(tabs) sapply(tabs,function(tab) sum(tab[,5])/4)

hist(score(maintained),col="orange",breaks=100)

wilcox.test(score(disrupted),score(maintained),alternative="greater")

allplots <- function(tablist,outfile) {
	ndim <- ceiling(sqrt(length(tablist)))
	pdf(outfile,ndim,ndim)
	op <- par(mfrow=c(ndim,ndim))
	lapply(tablist, function(tab) {
		ip <- par(mar=rep(2,4))
		plot(
			tab[,5],
			type="l",
			ylim=c(0,1),
			ylab="hit rate",
			xlab="shell",
			col="red",
			lwd="3"
		)
		par(ip)
		NULL
	})
	par(op)
	dev.off()
}

allplots(maintained,"maintainedPlots.pdf")
allplots(disrupted,"disruptedPlots.pdf")


pdf("shellHistos.pdf",5,5)
breaks <- c(0,exp(seq(-10,0,.25)))
histM <- hist(score(maintained),breaks=breaks,freq=FALSE,plot=FALSE)
histD <- hist(score(disrupted),breaks=breaks,freq=FALSE,plot=FALSE)
mycolors <- c("steelblue1","goldenrod1")
plot(
	1:(length(breaks)-1),histM$intensities,
	col=mycolors[[1]],
	type="l",xaxt="n",xlab="log(score)",ylab="density"
)
lines(1:(length(breaks)-1),histD$intensities,col=mycolors[[2]])
axis(1, at=seq(1,(length(breaks)-1),length.out=12), labels=c("-Inf",seq(-10,0))) 
legend("right",c("maintained","disrupted"),lwd=1,col=mycolors)
dev.off()

# pdf("shellHistos.pdf",10,5)
# op <- par(mfrow=c(1,2))
# hist(score(maintained),breaks=20,freq=FALSE,col="steelblue1")
# hist(score(disrupted),breaks=20,freq=FALSE,col="goldenrod1")
# par(op)
# dev.off()








