#!/usr/bin/Rscript

new.pcr.sim <- function() {
	num.cycles <- 3
	orf.len <- 1000
	mut.rate <- 1/2000

	poly.mol <- 100
	templ.mol <- 20

	sim <- function() {
		mutations <- rep(0,templ.mol)
		for (c in 1:num.cycles) {
			#sample templates
			new.mols <- sample(mutations, min(poly.mol,length(mutations))) 
			#add mutations
			new.mols <- new.mols + rpois(length(new.mols), orf.len*mut.rate)
			mutations <- c(mutations, new.mols)
		}
		mutations
	}

	set.templ.per.poly <- function(p) {
		templ.mol <<- round(p*poly.mol)
	}

	set.num.cycles <- function(n) {
		num.cycles <<- n
	}

	set.orf.len <- function(l) {
		orf.len <<- l
	}

	set.mut.rate <- function(r) {
		mut.rate <<- r
	}

	set.poly.mol <- function(m) {
		poly.mol <<- m
	}

	set.templ.mol <- function(m) {
		templ.mol <<- m
	}

	get.templ.mol <- function() {
		templ.mol
	}

	structure(list(
		sim=sim,
		set.num.cycles=set.num.cycles,
		set.orf.len=set.orf.len,
		set.mut.rate=set.mut.rate,
		set.poly.mol=set.poly.mol,
		set.templ.mol=set.templ.mol,
		set.templ.per.poly=set.templ.per.poly,
		get.templ.mol=get.templ.mol
	),class="pcrsim")
}


pcr <- new.pcr.sim()


# pcr$set.num.cycles(40)
# pcr$set.templ.mol(10)
# mutations <- pcr$sim()
# hist(mutations)	


cycle.vals <- 1:40
# mol.vals <- seq(10,200,5)
ratio.vals <- 2^seq(-3,4,.2)


usas <- NULL
amounts <- NULL
for (num.cycles in cycle.vals) {
	usa.row <- NULL
	amount.row <- NULL
	for (ratio in ratio.vals) {

		pcr$set.num.cycles(num.cycles)
		pcr$set.templ.per.poly(ratio)
		mutations <- pcr$sim()
		amount <- sum(mutations == 1)
		usability <- amount / (length(mutations) - pcr$get.templ.mol())

		usa.row <- c(usa.row,usability)
		amount.row <- c(amount.row, amount)
	}
	usas <- rbind(usas, usa.row)
	amounts <- rbind(amounts, amount.row)
}
rownames(usas) <- as.character(cycle.vals)
colnames(usas) <- as.character(ratio.vals)
rownames(amounts) <- as.character(cycle.vals)
colnames(amounts) <- as.character(ratio.vals)


paraplot <- function(data, main, col) {
	mar.orig <- (par.orig <- par(c("mar", "las", "mfrow")))$mar
    on.exit(par(par.orig))
    w <- (3 + mar.orig[2]) * par("csi") * 2.54
    layout(matrix(c(2, 1), ncol = 2), widths = c(1, lcm(w)))
    par(las = 1)
    mar <- mar.orig
    mar[4] <- mar[2]
    mar[2] <- 1
    par(mar = mar) 

    zlim <- range(data)
    levels <- seq(zlim[1], zlim[2], length = length(col))
    plot.new()
    plot.window(xlim = c(0, 1), ylim = range(levels), xaxs = "i", yaxs = "i")
    rect(0, levels[-length(levels)], 1, levels[-1], col = col,  density = NA)
    axis(4)
    box()
    mar <- mar.orig
    mar[4] <- 0
    par(mar = mar)

    par(las=0)
	image(
		data,
		axes=FALSE,
		xlab="cycles",
		ylab="Template/Enzyme ratio",
		main=main,
		col=col
	)
	axis(1, at=seq(0,1,length.out=length(rownames(data))), labels=rownames(data))
	axis(2, at=seq(0,1,length.out=length(colnames(data))), labels=colnames(data))

}

pdf("pcr_sim.pdf")
# op <- par(mfrow=c(2,2))

# plot.new()
paraplot(usas, "Efficiency", col=heat.colors(50))
paraplot(amounts, "Yield", col=heat.colors(50))

# image(
# 	usas,
# 	axes=FALSE,
# 	xlab="cycles",
# 	ylab="Template/Enzyme ratio",
# 	main="Efficiency",
# )
# axis(1, at=seq(0,1,length.out=length(rownames(usas))), labels=rownames(usas))
# axis(2, at=seq(0,1,length.out=length(colnames(usas))), labels=colnames(usas))

# image(
# 	amounts,
# 	axes=FALSE,
# 	xlab="cycles",
# 	ylab="Template/Enzyme ratio",
# 	main="Yield"
# )
# axis(1, at=seq(0,1,length.out=length(rownames(amounts))), labels=rownames(amounts))
# axis(2, at=seq(0,1,length.out=length(colnames(amounts))), labels=colnames(amounts))


# par(op)
dev.off()

