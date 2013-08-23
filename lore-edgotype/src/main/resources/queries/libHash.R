#!/usr/bin/Rscript

new.hash <- function() {

	.e <- new.env(hash=TRUE)

	putValue <- function(key,val) {
		assign(x=key,value=val, envir=.e)
	}

	putAll <- function(named) {
		if (class(named) == "list") {
			if (is.null(names(named))) {
				stop("Argument must be named")
			}
			for (i in 1:length(named)) {
				putValue(names(named)[i],named[[i]])
			}
		} else if (any(class(named) == c("numeric","character","logical"))) {
			if (is.null(names(named))) {
				stop("Argument must be named")
			}
			for (i in 1:length(named)) {
				putValue(names(named)[i],named[i])
			}
		} else if (any(class(named) == c("matrix","data.frame"))) {
			if (is.null(rownames(named))) {
				stop("Argument must be named")
			}
			lapply(1:nrow(named), function(i) putValue(rownames(named)[i],named[i,]))
			# for (i in 1:nrow(named)) {
			# 	putValue(rownames(named)[i],named[i,])
			# }
		}
	}

	getValue <- function(key) {
		get(key,envir=.e)
	}

	getAll <- function(keys) {
		values <- list()
		for (key in keys) {
			v <- getValue(key)
			values[[length(values)+1]] <- v
		}
		names(values) <- keys
		values
	}

	keySet <- function() {
		ls(.e)
	}

	containsKey <- function(key) {
		exists(key,envir=.e)
	}
	
	structure(list(
		put=putValue, 
		putAll=putAll, 
		get=getValue,
		getAll=getAll,
		keySet=keySet,
		containsKey=containsKey
	),class="hash")
}

# hash <- new.hash()

# hash$put("a",1)
# hash$get("a")

# hash$putAll(c(a=1,b=2,c=3))
# hash$get("b")

# hash$keySet()

# hash$getAll(hash$keySet())

