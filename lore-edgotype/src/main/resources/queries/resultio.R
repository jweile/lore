#!/usr/bin/Rscript

init.result.io <- function(tag) {

	timestamp <- format(Sys.time(),format='%Y-%m-%d_%H:%M:%S')

	out.dir <- paste("results/",timestamp,"_",tag,"/", sep="")
	dir.create(out.dir, mode="0755")

	get.timestamp <- function() {
		timestamp
	}

	get.out.dir <- function() {
		out.dir
	}

	file.exist <- function(filename) {
		system(paste("[ -e",filename,"]")) == 0
	}

	# name: name of the plot to draw
	# f: function performing the drawing operations
	# custom.par: list of drawing parameters to use
	draw <- function(name, f, custom.par=NULL, w=1,h=1) {
		pdf(paste(out.dir,name,".pdf",sep=""),width=7*w,height=7*h)
		op <- if (!is.null(custom.par)) par(custom.par) else NULL
		f()
		if (!is.null(op)) par(op)
		dev.off()

		svg(paste(out.dir,name,".svg",sep=""),width=7*w,height=7*h)
		op <- if (!is.null(custom.par)) par(custom.par) else NULL
		f()
		if (!is.null(op)) par(op)
		dev.off()
	}

	structure(list(
		get.timestamp=get.timestamp,
		get.out.dir=get.out.dir,
		draw=draw,
		file.exist=file.exist
	),class="result.io")
}


