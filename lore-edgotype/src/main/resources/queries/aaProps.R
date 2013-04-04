#!/usr/bin/Rscript

source("queries/resultio.R")
rio <- init.result.io("aaprops")

#Import data
data <- read.delim("aaprop.tsv",header=FALSE)
colnames(data) <- c("charge","hydropathy","polarity","disruption")

#Draw hydropathy histogram
rio$draw("hydro_hist", function() {
	hist(
		data$hydropathy,
		xlab="Change in hydropathy",
		main="",
		col="gray"
	)
})

#Draw hydropathy vs disruption plot
rio$draw("hydropathy", function() {
	plot(
		data$hydropathy,
		data$disruption,
		xlab="Change in hydropathy",
		ylab="Disruption rate",
		main="Hydropathy"
	)
})

hydro.pn <- data[data$disruption == 1, "hydropathy"]
hydro.pwe <- data[data$disruption != 1, "hydropathy"]

rio$draw("hydro_Versus",function(){
	cols <- c(rgb(0,1,0,.5),rgb(1,0,0,.5))
	hist(
		hydro.pwe,
		freq=FALSE,
		col=cols[1],
		xlab="Change in hydropathy",
		main=""
	)
	hist(
		hydro.pn,
		freq=FALSE,
		col=cols[2],
		add=TRUE
	)
	legend("topright",c("PWT/Edgetic","PN"),fill=cols)
})

cat("\n\nTesting difference of hydropathy index between PN and PWT/Edgetc\n")
t.test(hydro.pn, hydro.pwe, alternative="less")




resolution <- .05
breaks <- seq(0,1,resolution)



### POLARITY
rio$draw("pol_freq",function() {
	barplot(table(data$polarity), ylab="Frequency")
},list(las=3,mar=c(11,4,4,2)))

disruptionByPolarity <- sapply(levels(data$polarity), 
	function(p) data[(data$polarity == p), "disruption"]
)
histoByPolarity <- sapply(
	disruptionByPolarity,
	function(x) hist(x, plot=FALSE, breaks=breaks)$intensities
)
rownames(histoByPolarity) <- breaks[-1]-(resolution/2)

categories <- colnames(histoByPolarity)
fromCategories <- setdiff(unique(sapply(strsplit(categories,"->"),function(x) x[1])),"Same")

## define colors
# cols <- c("#CC52CD",
# 		"#C94A3A",
# 		"#C15185",
# 		"#6BCE50",
# 		"#8CD4A9",
# 		"#506D3B",
# 		"#4E4169",
# 		"#8CADC4",
# 		"#8877CE",
# 		"#C79F8B",
# 		"#C48A37",
# 		"#C6CD4E",
# 		"#59352E")
# names(cols) <- categories

rio$draw("polarity",function() {

	# mtext("Polarity",outer=TRUE)

	for (fromCat in fromCategories) {

		toDraw <- categories[substr(categories,1,nchar(fromCat))==fromCat | categories=="Same"]
		cols <- c("red","green","blue","black")
		names(cols) <- toDraw

		plot(0,
			xlim=c(0,1),
			ylim=c(0,max(histoByPolarity)), 
			type="n",
			xlab="Disruption rate",
			ylab="Density"
		)
		for (category in toDraw) {
			lines(
				as.numeric(rownames(histoByPolarity)), 
				histoByPolarity[,category], 
				col=cols[category]
			)
		}
		legend("top",toDraw,col=cols, lwd=1)

	}

}, list(mfrow=c(2,2)))





### CHARGE
rio$draw("charge_freq",function() {
	barplot(table(data$charge), ylab="Frequency")
},list(las=3,mar=c(11,4,4,2)))

disruptionByCharge <- sapply(levels(data$charge),
	function(c) data[(data$charge == c),"disruption"]
)
histoByCharge <- sapply(
	disruptionByCharge, 
	function(x) hist(x, plot=FALSE, breaks=breaks)$intensities
)
rownames(histoByCharge) <- breaks[-1]-(resolution/2)
categories <- colnames(histoByCharge)
fromCategories <- setdiff(unique(sapply(strsplit(categories,"->"),function(x) x[1])),"Same")


# #define colors
# cols <- c("#C9B22B",
# 		"#C953B5",
# 		"#BD4737",
# 		"#6ACC50",
# 		"#495E1E",
# 		"#5F2E53",
# 		"#6C76C4")
# names(cols) <- categories

rio$draw("charge",function() {

	for (fromCat in fromCategories) {

		toDraw <- categories[substr(categories,1,nchar(fromCat))==fromCat | categories=="Same"]
		cols <- c("red","green","black")
		names(cols) <- toDraw

		plot(0,
			xlim=c(0,1),
			ylim=c(0,max(histoByCharge)), 
			type="n",
			xlab="Disruption rate",
			ylab="Density"
		)
		for (category in toDraw) {
			lines(
				as.numeric(rownames(histoByCharge)), 
				histoByCharge[,category], 
				col=cols[category]
			)
		}
		legend("top",toDraw,col=cols, lwd=1)
	}

}, list(mfrow=c(2,2)))


polar.pvals <- sapply(
	disruptionByPolarity[1:length(disruptionByPolarity)-1], 
	function(x) ks.test(x, disruptionByPolarity$Same)$p.value
)
polar.pvals[polar.pvals < .05]


charge.pvals <- sapply(
	disruptionByCharge[1:length(disruptionByCharge)-1], 
	function(x) ks.test(x, disruptionByCharge$Same)$p.value
)
charge.pvals[charge.pvals < .05]


# cat("\n\nTesting: Distribution of disruptions by nonpolar->acidic mutations is different from silent mutations\n")

# ks.test(
# 	disruptionByPolarity$`nonpolar->acidic polar`,
# 	disruptionByPolarity$`Same`, 
# 	alternative="less"
# )



# cat("\n\nTesting: Distribution of disruptions by neutral->negative mutations is different from silent mutations\n")

# ks.test(
# 	disruptionByCharge$`neutral->negative`,
# 	disruptionByCharge$`Same`, 
# 	alternative="less"
# )
