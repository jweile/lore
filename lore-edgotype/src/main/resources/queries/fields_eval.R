#!/usr/bin/Rscript

library("hash")

if (!exists("new.sequence")) {
	source("queries/libmyseq.R")
}
if (!exists("init.result.io")) {
	source("queries/resultio.R")
}


rio <- init.result.io("fieldsEval")

trans <- init.translator("input/codontable.txt")


ref.nc <- new.sequence("GCAGGTTGGGAGATGGCAAAGACATCTTCTGGTCAGAGATACTTCTTAAATCACATCGATCAGACAACAACATGG",id="YAP1_WW")
ref.aa <- trans$translate(ref.nc)

##IMPORT ALL MUTANT DATA
#########################

counts1 <- read.delim("input/variantCountsJoint_r1.txt")
counts6 <- read.delim("input/variantCountsJoint_r6.txt")

varnames <- union(rownames(counts1),rownames(counts6))
freqs1 <- counts1/sum(counts1[,1])
freqs6 <- counts6/sum(counts6[,1])

# #Draw histograms for the frequencies in round 1 and 6
# rio$draw("VariantFreqHistos", function() {
# 	hist6 <- hist(freqs6[,1],breaks=c(seq(0,.0001,.000001),.5), plot=FALSE)
# 	hist1 <- hist(freqs1[,1],breaks=c(seq(0,.0001,.000001),.5), plot=FALSE)
# 	plot(
# 		hist1$mids,
# 		hist1$density,
# 		xlim=c(0,.00003), 
# 		type='l',
# 		xlab="Variant frequencies",
# 		ylab="Density"
# 	)
# 	points(
# 		hist6$mids,
# 		hist6$density,
# 		type='l',
# 		col=2
# 	)
# 	legend("right",c("Input library","Round 6"),lty=1,col=c(1,2))
# })


freqH1 <- hash(rownames(freqs1),freqs1[,1])
freqH6 <- hash(rownames(freqs6),freqs6[,1])

cat("Calculating enrichments...\n")
progress <- 0
enrichments <- hash()
for (varname in varnames) {
	if (!has.key(varname,freqH1)) {
		e <- NA
	} else if (!has.key(varname,freqH6)) {
		e <- 0
	} else {
		e <- freqH6[[varname]]/freqH1[[varname]]
	}
	enrichments[[varname]] <- e
	progress <- progress+1
	cat("\r",100*progress/length(varnames),"%    ")
}
cat("\nDone!\n")


# #test product rule
# mutsByVar <- strsplit(varnames,",")
# products <- sapply(mutsByVar, function(muts) {
# 	if (length(muts) > 1 && all(has.key(muts,enrichments))) {
# 		prod(values(enrichments,muts))
# 	} else {
# 		NA
# 	}
# })

# enrichVsProduct <- data.frame(row.names=varnames,enrichment=values(enrichments,varnames),product=products)

# rio$draw("enrichVsProduct", function() {
# 	plot(
# 		log(na.omit(enrichVsProduct)),
# 		xlab="log(variant enrichment)",
# 		ylab="log(variant enrichment predicted by product rule)",
# 		pch=".",
# 		xlim=c(-8,8),
# 		ylim=c(-8,8)
# 	)
# 	abline(0,1,col="gray")
# }

##IMPORT SINGLE MUTANT DATA
###########################

reads1 <- sum(counts1[,1])#9958612
mat1 <- as.matrix(read.delim("mutationCounts_r1.txt",row.names=1))
reads6 <- sum(counts6[,1])#10884745
mat6 <- as.matrix(read.delim("mutationCounts_r6.txt",row.names=1))

# rio$draw("fieldsMutCoverage_r1", function() {
# 	plotMutCoverage(mat1,ref.nc$toString(),trans,
# 		main="Mutations in Fields Input Library"
# 	)
# },h=1.25)

# rio$draw("fieldsMutCoverage_r6", function() {
# 	plotMutCoverage(mat6,ref.nc$toString(),trans,
# 		main="Mutations in Fields Round 6"
# 	)
# },h=1.25)

singleFreqs1 <- mat1/reads1
singleFreqs6 <- mat6/reads6

# #Draw histograms of mutational coverage
# rio$draw("fieldsMutCoverage_r6", function() {
# 	hist1 <- hist(singleFreqs1,plot=FALSE,breaks=100)
# 	hist6 <- hist(singleFreqs6,plot=FALSE,breaks=100)
# 	plot(hist1$mids, hist1$intensities,col=1,type='l',
# 		xlab="Variant frequency",ylab="Frequency")
# 	points(hist6$mids, hist6$intensities,col=2,type='l')
# 	legend("right",c("Input Library","Round 6 Library"),col=c(1,2),lty='solid')
# })

singleEnrichment <- log10(singleFreqs6/singleFreqs1)

# drawEnrichmentMap <- function(enrichment) {
# 	layout(matrix(c(1,2),nrow=1),widths=c(9,1))

# 	#compute color sclae
# 	initColors <- function(minmax,resolution=20,stops=c('darkblue','white','red')) {
# 		.sf <- ceiling(max(abs(minmax)))
# 		.palette <- colorRampPalette(stops)(resolution)
# 		getColor <- function(x) {
# 			if (is.na(x)) {
# 				'gray'
# 			} else if (is.infinite(x)) {
# 				ifelse(x < 0, stops[1], stops[3])
# 			} else {
# 				.palette[round(x * 20 / .sf)+(resolution/2)]
# 			}
# 		}
# 		getPalette <- function() .palette
# 		getScale <- function() .sf
# 		list(getColor=getColor, getPalette=getPalette, getScale=getScale)
# 	}
# 	minmax <- range(enrichment,na.rm=T,finite=T)
# 	colors <- initColors(minmax)

# 	#draw heatmap
# 	op <- par(las=1,mar=c(5.1,4.1,4.1,0))
# 	plot(0,
# 		type='n',
# 		axes=FALSE,
# 		xlim=c(0,ncol(enrichment)), 
# 		ylim=c(0,21),
# 		xlab="Position",
# 		ylab="Amino acid"
# 	)
# 	for (x in 1:ncol(enrichment)) {
# 		for (y in 1:21) {
# 			col <- colors$getColor(enrichment[y,x])
# 			rect(x-1,22-y,x,21-y,col=col, lty="blank")
# 		}
# 	}
# 	axis(1, at=c(1,seq(5,ncol(enrichment),5))-.5, labels=c(1,seq(5,ncol(enrichment),5)))
# 	axis(2, at=(1:21)-.5, labels=rev(rownames(enrichment)) )
# 	par(op)

# 	#draw legend
# 	op <- par(las=1,mar=c(5.1,0,4.1,3.1))
# 	palette <- colors$getPalette()
# 	plot(0,
# 		type='n',
# 		axes=FALSE,
# 		xlim=c(0,1), 
# 		ylim=c(0,length(palette)),
# 		xlab="",
# 		ylab=""
# 	)
# 	for (i in 1:length(palette)) {
# 		rect(0,i-1,1,i,col=palette[i],lty="blank")
# 	}
# 	labels <- parse(text=paste("10^", c(-1,0,1)*colors$getScale(), sep=""))
# 	labels[2] <- 1
# 	axis(4, at=c(1,length(palette)/2,length(palette))-.5, labels=labels)
# 	mtext("Relative frequencies round 6 vs input",4,2,las=0)
# 	par(op)
# }
# rio$draw("fieldsMutCoverage_r6", function() {
# 	drawEnrichmentMap(singleEnrichment)
# })



# #compare single mutant values with joint multi-mutant values
# singlemuts <- unique(unlist(strsplit(varnames,",")))
# singleVjoint <- t(sapply(singlemuts, function(mut) {
# 	to <- substr(mut,nchar(mut),nchar(mut))
# 	pos <- as.numeric(substr(mut,2,nchar(mut)-1))
# 	if (has.key(mut,enrichments) && pos <= ncol(singleEnrichment) && to %in% rownames(singleEnrichment)) {
# 		c(single=log10(values(enrichments,mut)), joint.multi=singleEnrichment[to,pos])
# 	} else {
# 		c(NA,NA)
# 	}
# }))
# plot(singleVjoint,xlab="Single mutant LFC",ylab="Joint mutant LFC")
# abline(0,1,col="gray")text(-1,0.5,paste("R =",format(cor(na.omit(singleVjoint))[1,2],digits=2)))






######
#Simulate Y2H

#derive affinities from fields data
enrVals <- values(enrichments,varnames)
affinities <- log(enrVals) + 10

#simulate random mutations following a poisson distribution
mut.sim <- function(aa.seq, n, lambda=5) {
	aas <- c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y')
	lapply(rpois(n,lambda), function(nmuts) {
		pos <- sort(sample(1:nchar(aa.seq),nmuts))
		sapply(pos, function(p) {
			from <- substr(aa.seq,p,p)
			to <- sample(setdiff(aas,from),1)
			paste(from,p,to,sep="")
		})
	})
}

#Simulate colony picking
pickColonies <- function(pcr.variants, coloniesPicked=50*96) {

	variants <- sample(pcr.variants, coloniesPicked, replace=TRUE)
	#Filter out unusable mutants
	variants <- variants[sapply(variants, {
		function(x) length(x) > 0 && !any(x == "truncation" | x == "nonsense") && !all(x == "silent")
	})]

	#remove silent tags, order by position and remove redundancies
	variants <- lapply(variants, function(variant) {
		variant <- setdiff(variant,"silent")

		positions <- as.numeric(substr(variant,2,nchar(variant)-1))
		if (anyDuplicated(positions)) {
			variant <- rev(variant)[!duplicated(rev(positions))]
		}

		positions <- as.numeric(substr(variant,2,nchar(variant)-1))
		variant[order(positions)]
	})

	variants
}

y2hsim <- function(variantTags, enrichments, noise=.05, cycles=4, seq.reads=1000000) {

	# s=stdev, t=threshold, g=graduality
	doubling.time <- function(aff, min=2, t=8, s=noise, g=1) {
		exp(g*(t-aff)) + min + rnorm(length(aff),0,s)
	}
	growth.sim <- function(dtime, time=24, max=4000) {
		round(max/(1 + (max-1) * 2^(1-time/dtime)))
	}

	#look up affinities for tags
	affs <- sapply(variantTags, function(vartag) {
		if (has.key(vartag, enrichments)) {
			log(values(enrichments,vartag))+10
		} else {
			single.tags <- strsplit(vartag,",")[[1]]
			if (all(has.key(single.tags, enrichments))) {
				predicted <- prod(values(enrichments,single.tags))
				log(predicted)+10
			} else {
				NA
			}
		}
	})
	affs[affs < 0] <- 0

	#compute doubling time from affinity
	dtimes <- doubling.time(affs)
	dtimes.his <- rnorm(length(variantTags),2,noise)

	ncells <- growth.sim(dtimes)
	ncells.his <- growth.sim(dtimes.his)

	#pcr amplify (unbiased)
	namplicons <- ncells * 2^cycles
	namplicons.his <- ncells.his * 2^cycles
	#sequencing
	reads <- rpois(length(variantTags), seq.reads * namplicons/sum(na.omit(namplicons)))
	reads.his <- rpois(length(variantTags), seq.reads * namplicons.his/sum(na.omit(namplicons.his)))

	data.frame(
		# row.names=variantTags,
		reads=reads,
		reads.his=reads.his,
		score=log10(reads/reads.his),
		affinity=affs
	)
}



# #Generate variants
# pcr.result <- pcr.sim(ref.nc, trans, cycles=12, etr=512, mut.rate=1/500)
# variants <- pickColonies(
# 	pcr.result, 
# 	coloniesPicked=100*96
# )
# variantTags <- unique(sapply(variants, function(muts) paste(muts[muts != "silent"],collapse=",")))


# #plot mutation coverage
# rio$draw("y2hsim_pcr", function() {
# 	plotMutCoverage(mutlist2matrix(variants,nchar(ref.aa)), ref.nc, trans)
# })

# #run Y2H simulation
# y2hresult <- y2hsim(variantTags, enrichments)
# rownames(y2hresult) <- variantTags
# # y2hresult <- data.frame(row.names=variantTags,reads=reads,reads.his=reads.his,score=log(reads/reads.his),affinity=affs,ncells=ncells)

# rio$draw("affinityVsY2HScore", function() {
# 	plot(
# 		y2hresult$affinity, y2hresult$score,
# 		xlab="Affinity",
# 		ylab="log fold-change of barcode counts"
# 	)
# })

# #find single mutants and those variants that contain these single mutants
# singlemuts <- variantTags[sapply(strsplit(variantTags,","),length) == 1]
# varTagsForMuts <- lapply(singlemuts, function(mutation) {
# 	matches <- sapply(variants, function(variant) {
# 		(length(variant)) > 1 && (mutation %in% variant)
# 	})
# 	unique(sapply(variants[matches], function(muts) paste(muts[muts != "silent"],collapse=",")))
# })
# names(varTagsForMuts) <- singlemuts

# rio$draw("singleVsMulti",function() {
# 	op <- par(mfrow=c(4,4))
# 	for (i in 2:17) {
# 		hist(y2hresult$score,breaks=50,col="gray",border=NA,xlab="Barcode LFC",main=singlemuts[i])
# 		abline(v=y2hresult[varTagsForMuts[[i]],"score"], col="red")
# 		abline(v=y2hresult[singlemuts[i],"score"],col="blue",lty=2,lwd=2)
# 		# legend("topright",c("Multi-mutant","Single mutant"),col="red",lty=c(1,2),lwd=c(1,2))
# 	}
# 	par(op)
# })


# #compare single mutant values with joint multi-mutant values
# singleVjoint <- do.call(rbind,lapply(1:length(singlemuts), function(i) {
# 	bar <- apply(y2hresult[unlist(varTagsForMuts[[i]]),c("reads","reads.his")],2,sum,na.rm=TRUE)
# 	joint.multi <- as.numeric(log(bar[1]/bar[2]))
# 	c(single=y2hresult[singlemuts[i],"score"], joint.multi=joint.multi)
# }))
# rio$draw("singleVsJoint",function() {
# 	plot(singleVjoint,xlab="Single mutant LFC",ylab="Joint mutant LFC")
# }


# #test product rule for Y2H data
# productRuleScores <- sapply(strsplit(variantTags,","), function(muts) {
# 	if (length(muts) > 1 && !any(is.na(y2hresult[muts,"score"]))) {
# 		sum(y2hresult[muts,"score"])
# 	} else {
# 		NA
# 	}
# })
# rio$draw("productRuleY2H",function() {
# 	plot(y2hresult$score,productRuleScores,xlab="Variant read LFC",ylab="Predicted variant read LFC")
# }


# #####
# #compare on binary Y2H interaction basis
# #

# singlemuts <- unique(unlist(strsplit(variantTags,",")))
# varTagsForMuts <- lapply(singlemuts, function(mutation) {
# 	matches <- sapply(variants, function(variant) {
# 		(length(variant)) > 1 && (mutation %in% variant)
# 	})
# 	unique(sapply(variants[matches], function(muts) paste(muts[muts != "silent"],collapse=",")))
# })
# names(varTagsForMuts) <- singlemuts

# scores <- sapply(varTagsForMuts, function(vars) {
# 	readsMinusPlus <- apply(y2hresult[unlist(vars),c("reads","reads.his")],2,sum,na.rm=TRUE)
# 	as.numeric(log(readsMinusPlus[1]/readsMinusPlus[2]))
# })
# names(scores) <- singlemuts


# goldStandard <- singleEnrichment >= -1
# goldStandard <- sapply(names(scores), function(mut) {
# 	to <- substr(mut,nchar(mut),nchar(mut))
# 	pos <- as.numeric(substr(mut,2,nchar(mut)-1))
# 	goldStandard[to,pos]
# })
# names(goldStandard) <- singlemuts


# drawOverlapMap <- function(goldStandard, calls, l) {
# 	op <- par(las=1)
# 	aas <- c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y')
# 	mat <- matrix("gray",
# 		nrow=20,ncol=l,
# 		dimnames=list(aas,1:l)
# 	)
# 	for (mut in names(calls)) {

# 		to <- substr(mut,nchar(mut),nchar(mut))
# 		pos <- as.numeric(substr(mut,2,nchar(mut)-1))

# 		gs <- goldStandard[mut]
# 		call <- calls[mut]

# 		# mat[to,pos] <- call-gs
# 		if (!is.na(gs) && !is.na(call)) {
# 			mat[to,pos] <- if (gs) {
# 				if (call)
# 					"chartreuse3"
# 				else
# 					"black"
# 			} else {
# 				if (call)
# 					"red"
# 				else
# 					"white"
# 			}
# 		}
# 	}
# 	plot(0,
# 		type='n',
# 		axes=FALSE,
# 		xlim=c(0,ncol(mat)),
# 		ylim=c(0,nrow(mat)),
# 		xlab="Position",
# 		ylab="Amino acid"
# 	)
# 	for (x in 1:ncol(mat)) {
# 		for (y in 1:nrow(mat)) {
# 			rect(x-1,21-y,x,20-y,col=mat[y,x],border=NA)
# 		}
# 	}
# 	axis(1, at=c(1,seq(5,ncol(mat),5))-.5, labels=c(1,seq(5,ncol(mat),5)))
# 	axis(2, at=(1:20)-.5, labels=rev(rownames(mat)) )
# 	par(op)
# }

# drawOverlapMap(goldStandard,scores >= -1, nchar(ref.aa))


drawROC <- function(real, scores,draw=TRUE) {

	auc <- function(x,y) {
		( 
			x[1]*y[1] + 
			do.call(sum, lapply(1:length(x)-1, function(i) {
				(x[i+1] - x[i]) * (y[i+1] + y[i])
			}))
			+ (1-x[length(x)]) * (1+y[length(y)]) 
		) / 2
	}

	srange <- range(na.omit(scores),finite=TRUE)
	xp <- (srange[2]-srange[1])/30
	coords <- do.call(rbind,lapply(seq(srange[1]-xp,srange[2]+xp,length.out=30), function(threshold) {

		positive <- scores >= threshold
		
		fp <- positive & !real
		fn <- !positive & real

		fpr <- sum(na.omit(fp)) / sum(na.omit(!real))
		tpr <- 1-sum(na.omit(fn)) / sum(na.omit(real))

		c(fpr,tpr)
	}))

	coords <- coords[(nrow(coords):1),]

	# print(coords)

	coords[coords[,1] < 0,1] <- 0
	coords[coords[,1] > 1,1] <- 1
	coords[coords[,2] < 0,2] <- 0
	coords[coords[,2] > 1,2] <- 1

	.auc <- auc(coords[,1],coords[,2])
	if (draw) {
		plot(coords,
			type='l',
			lwd=2,col=2,
			xlab="1-specificity",ylab="sensitivity",
			xlim=c(0,1),ylim=c(0,1)
		)
		text(.5,.5,paste("AUC =",format(.auc,digits=2)))
	}

	.auc
}
# drawROC(goldStandard,scores)
# drawROC(goldStandard,singleScores,draw=TRUE)





singleVjoint <- function(ref.aa, toPick, nmuts) {

	#generates a gold standard dataset from the enrichment data
	generate.GS <- function(ref.aa, singleLFC) {

		aas <- c('A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y')
		allSingles <- do.call(c,lapply(1:nchar(ref.aa), function(pos) {
			from <- substr(ref.aa,pos,pos)
			tos <- setdiff(aas,from)
			paste(from,pos,tos,sep="")
		}))

		gsMatrix <- singleLFC >= -1
		goldStandard <- sapply(allSingles, function(mut) {
			to <- substr(mut,nchar(mut),nchar(mut))
			pos <- as.numeric(substr(mut,2,nchar(mut)-1))
			gsMatrix[to,pos]
		})
		names(goldStandard) <- allSingles

		goldStandard
	}

	#supplement the predictions with priors to cover all the remaining possible mutants
	supplementPriors <- function(scores, mutants) {
		sapply(mutants, function(mutant) {
			if (mutant %in% names(scores) && !is.na(scores[mutant])) {
				scores[mutant]
			} else {
				mean(na.omit(scores))
			}
		})
	}


	#Generate variants
	# mutagenized <- pcr.sim(ref.nc, trans, cycles=12, etr=512, mut.rate=1/500)
	mutagenized <- mut.sim(ref.aa, 2*toPick, nmuts)

	variants <- pickColonies(mutagenized, coloniesPicked=toPick)
	variantTags <- unique(sapply(variants, function(muts) 
		paste(muts[muts != "silent"],collapse=",")
	))

	singlemuts <- variantTags[sapply(strsplit(variantTags,","),length) == 1]
	single.coverage <- length(singlemuts) / (nchar(ref.aa)*19)

	#run Y2H simulation
	y2hresult <- y2hsim(variantTags, enrichments)
	rownames(y2hresult) <- variantTags

	goldStandard <- generate.GS(ref.aa, singleEnrichment)

	singleScores <- y2hresult[singlemuts,"score"]
	names(singleScores) <- singlemuts
	singleScores <- supplementPriors(singleScores, names(goldStandard))

	single.auc <- drawROC(goldStandard,singleScores,draw=FALSE)


	#Identify all mutations
	allmuts <- unique(unlist(strsplit(variantTags,",")))
	joint.coverage <- length(allmuts) / (nchar(ref.aa) * 19)
	varTagsForMuts <- lapply(allmuts, function(mutation) {
		matches <- sapply(variants, function(variant) {
			(length(variant)) > 1 && (mutation %in% variant)
		})
		unique(sapply(variants[matches], function(muts) paste(muts[muts != "silent"],collapse=",")))
	})
	names(varTagsForMuts) <- allmuts

	jointScores <- sapply(varTagsForMuts, function(vars) {
		readsMinusPlus <- apply(y2hresult[unlist(vars),c("reads","reads.his")],2,sum,na.rm=TRUE)
		as.numeric(log(readsMinusPlus[1]/readsMinusPlus[2]))
	})
	jointScores <- supplementPriors(jointScores, names(goldStandard))

	names(jointScores) <- allmuts


	joint.auc <- drawROC(goldStandard,jointScores,draw=FALSE)

	c(
		e.mut = nmuts,
		picked = toPick,
		usable = length(variantTags),
		single = length(singlemuts),
		single.coverage = single.coverage,
		joint.coverage = joint.coverage,
		single.auc = single.auc,
		joint.auc = joint.auc
	)
}


toPick <- 96*c(1,2,5,10,50,100)
nmuts <- 1:6
results <- lapply(1:10, function(i) {
	t(apply(
		expand.grid(
			nmuts=nmuts,
			toPick=toPick
		),
		1,
		function(vals) singleVjoint(ref.aa,vals["toPick"],vals["nmuts"])
	))
})

single.aucs <- do.call(cbind,lapply(results, function(result) {
	result[,"single.auc"]
}))
single.auc.mean <- apply(single.aucs,1,mean)
single.auc.stdev <- sqrt(apply(single.aucs,1,var))

joint.aucs <- do.call(cbind,lapply(results, function(result) {
	result[,"joint.auc"]
}))
joint.auc.mean <- apply(joint.aucs,1,mean)
joint.auc.stdev <- sqrt(apply(joint.aucs,1,var))


op <- par(mfrow=c(2,3),oma=c(0,0,0,10))
for (picked in toPick) {
	rows <- results[[1]][,"picked.toPick"]==picked
	
	mids <- barplot(
		rbind(single.auc.mean[rows], joint.auc.mean[rows]),
		names.arg=nmuts,
		beside=TRUE,
		main=paste(picked,"colonies"),
		ylim=c(0,1),
		xlab="lambda mutations",
		ylab="ROC AUC",
		col=c("steelblue1","steelblue4")
	)
	abline(h=0.5, lty="dashed")
	arrows(
		mids[1,],single.auc.mean[rows]-single.auc.stdev[rows],
		mids[1,],single.auc.mean[rows]+single.auc.stdev[rows],
		angle=90,length=.02,code=3
	)
	arrows(
		mids[2,],joint.auc.mean[rows]-joint.auc.stdev[rows],
		mids[2,],joint.auc.mean[rows]+joint.auc.stdev[rows],
		angle=90,length=.02,code=3
	)
}
par(op)
op <- par(usr=c(0,1,0,1),xpd=NA)
legend("right",legend=c("single","joint"),fill=c("steelblue1","steelblue4"))
par(op)


