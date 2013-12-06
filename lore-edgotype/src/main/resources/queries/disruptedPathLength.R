maintained <- read.delim("dist_maintained.txt",header=F)[,1]
disrupted <- read.delim("dist_disrupted.txt",header=F)[,1]

means <- c(mean(maintained),mean(disrupted))
stdev <- sqrt(c(var(maintained),var(disrupted)))

wilcox.test(disrupted,maintained,alternative="l")

tab.m <- table(maintained)
tab.d <- table(disrupted)

pdf("diseasePathLength.pdf")

plot(as.numeric(names(tab.d)),as.numeric(tab.d)/length(disrupted),type="l",col=2,
	xlab="path length",ylab="density",xlim=c(1,8))
points(as.numeric(names(tab.m)),as.numeric(tab.m)/length(maintained),type="l")

abline(v=median(maintained),lty="dashed")
abline(v=median(disrupted),col=2,lty="dashed")

p.d <- as.numeric(tab.d)/sum(tab.d)
se.d <- sqrt(p.d * (1-p.d) / sum(tab.d))
arrows(
	as.numeric(names(tab.d)), 
	y0=as.numeric(tab.d)/length(disrupted)+se.d/2, 
	y1=as.numeric(tab.d)/length(disrupted)-se.d/2,
	length=.03,
	angle=90,
	code=3,
	col=2
)


p.m <- as.numeric(tab.m)/sum(tab.m)
se.m <- sqrt(p.m * (1-p.m) / sum(tab.m))
arrows(
	as.numeric(names(tab.m)), 
	y0=as.numeric(tab.m)/length(maintained)+se.m/2, 
	y1=as.numeric(tab.m)/length(maintained)-se.m/2,
	length=.03,
	angle=90,
	code=3,
	col=1
)


legend("topright",c("maintained","disrupted"),col=c(1,2),lty=1)

dev.off()
