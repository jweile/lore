
#Function to turn string into character array
to.char.array <- function (str) sapply(1:nchar(str), function(i) substr(str,i,i))
char.at <- function(str,i) substr(str,i,i)


new.sequence <- function(sequence, qual=NULL, id=NULL) {

	.seq <- sequence
	.qual <- qual
	.id <- id

	toString <- function() {
		.seq
	}
	getQuality <- function(is) {
		if (!is.null(.qual)) {
			.qual[is]
		} else {
			warning("This sequence has no quality track.")
			NULL
		}
	}
	getID <- function() {
		if (!is.null(.id)) {
			.id
		} else {
			warning("This sequence has no ID field.")
			NULL
		}
	}

	structure(list(
		toString=toString,
		getQuality=getQuality,
		getID=getID
	),class="yogiseq")
}
print.yogiseq <- function(s) print(paste("<YogiSeq:",s$getID(),">"))
summary.yogiseq <- function(s) c(id=s$getID(),sequence=s$toString(),phred=paste(s$getQuality(),collapse=","))
length.yogiseq <- function(s) nchar(s$toString())

reverseComplement <- function(seq) {
	if (!any(class(seq) == "yogiseq")) stop("First argument must be a YogiSeq object")
	trans <- c(A='T',C='G',G='C',T='A',N='N',R='Y',Y='R',S='S',W='W',K='M',M='K')
	revSeq <- paste(rev(sapply(to.char.array(seq$toString()), function(nc) trans[nc])),collapse="")
	revQual <- rev(seq$getQuality())
	new.sequence(revSeq,qual=revQual,id=seq$getID())
}

parseFASTQ <- function(con) {

	qualScale <- to.char.array("!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~")
	string2phred <- function(string) {
		out <- sapply(to.char.array(string), function(x) which(qualScale == x))
		names(out) <- NULL
		out+32
	}

	id <- NULL
	sequence <- NULL
	quality <- NULL
	seqMode <- TRUE
	contents <- list()

	while (length(line <- readLines(con, n=1, warn=FALSE)) > 0) {
	    
		if (substr(line,1,1) == "@") {

			#store old sequence if exists
			if (!is.null(id) && !is.null(sequence) && !is.null(quality)) {
				if (nchar(sequence) != nchar(quality)) {
					warning("Sequence and quality tracks do not match!")
				}
				contents[[length(contents)+1]] <- new.sequence(sequence,id=id,qual=string2phred(quality))
			}

			#init storage fields for new sequence
			id <- strsplit(substr(line,2,nchar(line)), " ", fixed=TRUE)[[1]][1]
			sequence <- NULL
			quality <- NULL
			seqMode <- TRUE

		} else if (substr(line,1,1) == "+") {

			seqMode <- FALSE

		} else if (nchar(line) >= 0) {
			if (seqMode) {
				sequence <- paste(sequence, line, sep="")
			} else {
				quality <- paste(quality, line,sep="")
			}
		}
	} 

	#store last unsaved sequence if exists
	if (!is.null(id) && !is.null(sequence) && !is.null(quality)) {
		if (nchar(sequence) != nchar(quality)) {
			warning("Sequence and quality tracks do not match!")
		}
		contents[[length(contents)+1]] <- new.sequence(sequence,id=id,qual=string2phred(quality))
	}


	contents
}

writeFASTQ <- function(con, seqs) {

	#function for decoding phred quality scores
	qualScale <- to.char.array("!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~")
	qual2string <- function(qual) paste(qualScale[qual-32],collapse="")

	writeLines(unlist(lapply(seqs, function(s) {
		c(
			paste("@",s$getID(),sep=""),
			s$toString(),
			"+",
			qual2string(s$getQuality())
		)
	})),con)

}


#creates a new fastq parser object
new.fastq.parser <- function(con) {

	.con <- con

	#function for decoding phred quality scores
	qualScale <- to.char.array("!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~")
	string2phred <- function(string) {
		out <- sapply(to.char.array(string), function(x) which(qualScale == x))
		names(out) <- NULL
		out+32
	}

	#function for parsing the next n entries from the open fastq file (or less if less than n remain)
	parse.next <- function(n=10) {

		contents <- list()
		i <- 0

		while ((i <- i+1) <= n && length(lines <- readLines(.con, n=4)) > 0) {

			if (length(lines) < 4 || substr(lines[1],1,1) != "@" || substr(lines[3],1,1) != "+") {
				stop("Corrupt read:\n",paste(lines,collapse="\n"))
			}

			id <- strsplit(substr(lines[1],2,nchar(lines[1])), " ", fixed=TRUE)[[1]][1]
			sequence <- lines[2]
			quality <- string2phred(lines[4])

			contents[[length(contents)+1]] <- new.sequence(sequence,id=id,qual=quality)

		}

		contents
	}

	structure(list(parse.next=parse.next),class="yogi.fastq.parser")
}



#alignment algorithm requires bitwise operations
library(bitops)

##
# Needleman-Wunsch global alignment algorithm
#
new.alignment <- function(s1, s2) {

	if (any(class(s1)=="yogiseq")) {
		c1 <- c("$",to.char.array(s1$toString()))
	} else {
		c1 <- c("$",to.char.array(s1))
	}
	if (any(class(s2)=="yogiseq")) {
		c2 <- c("$",to.char.array(s2$toString()))
	} else {
		c2 <- c("$",to.char.array(s2))
	}

	#init score matrix
	mat <- matrix(nrow=length(c1), ncol=length(c2))
	mat[1,] <- 1:length(c2) - (c1[1] == c2[1])
	mat[,1] <- 1:length(c1) - (c1[1] == c2[1])

	#init trace matrix
	trace <- matrix(0, nrow=length(c1), ncol=length(c2))
	trace[1,] <- 4
	trace[,1] <- 2
	trace[1,1] <- 0

	#compute alignment matrix
	for (i in 2:length(c1)) {
		for (j in 2:length(c2)) {
			options <- c(
				rep = mat[i-1,j-1] + (c1[i] != c2[j]),
				del = mat[i-1,j] + 1,
				ins = mat[i,j-1] + 1
			)
			mat[i,j] <- min(options)

			tr.bitmasks <- 2^(which(options == min(options))-1)
			for (mask in tr.bitmasks) {
				trace[i,j] <- bitOr(trace[i,j],mask)
			}
		}
	}

	getMatrix <- function() {
		mat
	}

	getDistance <- function() {
		mat[length(c1),length(c2)]
	}

	.mutations <- NULL
	.mapping <- NULL
	run.trace <- function() {

		rep <- 1
		del <- 2
		ins <- 4

		muts <- list()
		map <- list()

		i <- length(c1)
		j <- length(c2)

		while (i > 1 || j > 1) {
			if (bitAnd(trace[i,j], rep) > 0) {
				if (c1[i] != c2[j]) {
					muts[[length(muts)+1]] <- c(c1[i], i-1, j-1, c2[j])
				}
				map[[length(map)+1]] <- c(i-1, j-1)
				i <- i-1
				j <- j-1
			} else if (bitAnd(trace[i,j], del)) {
				muts[[length(muts)+1]] <- c(c1[i], i-1, j-1, "-")
				map[[length(map)+1]] <- c(i-1, NA)
				i <- i-1
			} else if (bitAnd(trace[i,j], ins)) {
				muts[[length(muts)+1]] <- c("-", i-1, j-1, c2[j])
				map[[length(map)+1]] <- c(NA, j-1)
				j <- j-1
			} else {
				stop("uninitialized trace at ",i,j)
			}
		}
		# if (c1[1] != c2[1]) {
		# 	muts[[length(muts)+1]] <- c(c1[1],i,c2[1])
		# }
		.mapping <<- do.call(rbind,rev(map))
		.mutations <<- do.call(rbind,rev(muts))
	}

	getMutations <- function() {
		if (is.null(.mutations)) run.trace()
		.mutations
	}

	getMappings <- function() {
		if (is.null(.mapping)) run.trace()
		.mapping
	}

	structure(list(
		getMatrix=getMatrix,
		getDistance=getDistance,
		getMutations=getMutations,
		getMappings=getMappings
	),class="yogialign")

}

# processFile <- function(file,f) {
# 	tryCatch({
# 		con <- file(file, open="r")
# 		f(con)
# 	},
# 	error = function(ex) {
# 		traceback(ex)
# 	},
# 	finally = {
# 		if (exists("con") && isOpen(con)) {
# 			close(con)
# 		}
# 	})
# }

# test1 <- NULL
# processFile("test1.fastq",function(con) {
# 	test1 <<- parseFASTQ(con)
# })

# test2 <- NULL
# processFile("test2.fastq",function(con) {
# 	test2 <<- parseFASTQ(con)
# })







