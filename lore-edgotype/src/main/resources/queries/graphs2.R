#/usr/bin/Rscript

new.graph <- function() {

	nodes <- vector()
	edges <- list()
	nb <- list()

	# tries to add an edge between node 'a' and 'b' to the graph
	# returns TRUE if successful and FALSE if edge already existed.
	add.edge <- function(a,b) {
		ai <- as.character(a)
		bi <- as.character(b)

		if (b %in% nb[[ai]]) {
			FALSE
		} else {
			nodes <<- union(nodes, c(a,b))
			edges[[length(edges)+1]] <<- c(a,b)
			nb[[ai]] <<- c(nb[[ai]],b)
			nb[[bi]] <<- c(nb[[bi]],a)
			TRUE
		}
	}

	# lists all nodes in the graph
	list.nodes <- function() {
		nodes
	}

	# lists all edges in the graph
	list.edges <- function() {
		edges
	}

	# returns number of nodes
	num.nodes <- function() {
		length(nodes)
	}

	# returns number of edges
	num.edges <- function() {
		length(edges)
	}

	# lists the neighbours of the node 'node'
	list.neighbours <- function(node) {
		nname <- as.character(node)
		nb[[nname]]
	}

	# returns the degree of the node 'node'
	degree <- function(node) {
		length(nb[[as.character(node)]])
	}

	#returns a vector of all node degrees in the graph
	deg.dist <- function() {
		sapply(nodes, degree)
	}

	# constructs a new scale-free graph with n nodes 
	# according to the preferential attachment model
	pref.attachm <- function(n, k=3) {

		if (n < 4) {
			stop("n must not be smaller than 4")
		}

		cat("Running preferential attachment algorithm\n")
		pb <- txtProgressBar(max=n, style=3)

		#seed
		add.edge(1,2)
		add.edge(1,3)
		add.edge(3,2)

		#grow
		for (new.node in 4:n) {
			for (i in 1:round(runif(1,1,k))) {
				r <- runif(1, 1, 2*num.edges())
				s <- 0
				for (node in nodes) {
					s <- s + degree(node)
					if (s > r) {
						add.edge(new.node, node)
						break
					}
				}
			}
			setTxtProgressBar(pb,new.node)
		}
		close(pb)

	}

	structure(list(
		add.edge=add.edge, 
		list.nodes=list.nodes,
		list.edges=list.edges, 
		num.nodes=num.nodes,
		num.edges=num.edges,
		list.neighbours=list.neighbours,
		degree=degree,
		deg.dist=deg.dist,
		pref.attachm=pref.attachm
	), class="Graph")
}

print.Graph <- function(graph) {
	if (!inherits(graph,"Graph"))
		stop("graph is not a Graph!")
	cat(
		"Graph:",
		graph$num.nodes(),"nodes,",
		graph$num.edges(),"edges\n"
	)
}


# performs a virtual screen with given sensitivity on
# a graph. returns a new graph where each edge from 
# old graph has a chance of p=sensitivity to be in the 
# new graph.
# parameter n: size of sampling space, defaults to max.
screen <- function(g, n=g$num.edges(), sensitivity=.2) {

	if (!inherits(g, "Graph")) {
		stop("g must be a Graph")
	}

	edges <- sample(g$list.edges(),n)

	g2 <- new.graph();
	for (edge in edges) {
		if (runif(1,0,1) < sensitivity) {
			g2$add.edge(edge[1], edge[2])
		}
	}
	g2
}




# Simulate edgotypes. Parameters:
# n: number of genes for which to create alleles
# k: maximal number of alleles per gene
# sim: function with parameter d, that generates
#      a random number of maintained edges for degree d
make.edgotypes <- function(graph, n=graph$num.nodes(), sim, k=5) {

	# genes: hashes the gene (node) corresponding to each allele
	genes <- list()
	# nb: hashes the neighbours for each allele
	nb <- list()

	if (!inherits(graph,"Graph")) {
		stop("graph must be a Graph object")
	}

	allele <- 0
	for (gene in sample(graph$list.nodes(),n)) {

		d <- graph$degree(gene)

		num.alleles <- runif(1,1,k)
		for (i in 1: num.alleles) {

			allele <- allele + 1
			aName <- as.character(allele)
			genes[[aName]] <- gene

			maintained <- sample(graph$list.neighbours(gene), sim(d))
			nb[[aName]] <- maintained

		}
	}

	num.alleles <- function() {
		length(genes)
	}

	# compute the fractions of maintained edges in the edgotypes
	# object compared to the graph
	frac.maintained <- function(graph, excludeSingles=TRUE) {

		l <- vector()
		for (allele in names(genes)) {
			gene <- genes[[allele]]
			d <- graph$degree(gene)
			if (d > 1 || !excludeSingles) {
				neighbours <- graph$list.neighbours(gene)
				maintained <- intersect(neighbours, nb[[allele]])
				frac.main <- length(maintained)/d
				l[length(l)+1] <- frac.main
			}
		}
		l
	}


	structure(list(
		num.alleles=num.alleles,
		frac.maintained=frac.maintained
	),class="Edgotype")
}

print.Edgotype <- function(edgo) {
	if (!inherits(edgo,"Edgotype"))
		stop("object is not an Edgotype!")
	cat(
		"Edgotypes for",
		edgo$num.alleles(),"alleles\n"
	)
}


make.models <- function() {

	bm.p <- .55
	bm.q <- .05
	tm.p <- .4
	tm.q <- .4

	set.bm.p <- function(p) {
		bm.p <<- p
	}
	set.bm.q <- function(q) {
		bm.q <<- q
	}
	set.tm.p <- function(p) {
		tm.p <<- p
	}
	set.tm.q <- function(q) {
		tm.q <<- q
	}

	monomodal <- function(d) {
		round(runif(1,0,d))
	}
	bimodal <- function(d) {
		n <- 0
		if (runif(1,0,1) < bm.p) {
			n <- d-rbinom(1, d, bm.q)
		}
		n
	}
	trimodal <- function(d) {
		if (d == 0) {
			0
		} else if (d == 1) {
			if (runif(1,0,1) > 0) {
				0
			} else {
				1
			}
		} else {
			r <- runif(1,0,1)
			if (r < tm.p) {
				0
			} else if (r > (1-tm.q)) {
				d
			} else {
				round(runif(1,1,d-1))
			}
		}
	}

	list(
		monomodal=monomodal,
		bimodal=bimodal,
		trimodal=trimodal,
		set.bm.p=set.bm.p,
		set.bm.q=set.bm.q,
		set.tm.p=set.tm.p,
		set.tm.q=set.tm.q
	)
}






# #### TEST ####

# n <- 1000

# g <- new.graph()
# g$pref.attachm(n)
# gs <- screen(g)

# models <- make.models()
# edgo <- make.edgotypes(g, sim=models$trimodal)
# fracs <- edgo$frac.maintained(g)




