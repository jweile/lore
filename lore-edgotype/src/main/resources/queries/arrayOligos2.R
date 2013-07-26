
oligos <- function(fl,overlap,e.M) {
	#number of alternative aminoacids
	num.aa <- 19
	#spacer length in nt
	spacer <- fl - 2*overlap

	#number of fragments per assembly
	n <- function(l) if(l <= fl) 1 else floor((l-overlap-1)/(spacer+overlap))+1
	#number of non-overlapping nc in the last oligo that are still part of the orf
	r <- function(l) if (l <= fl) l else (l-overlap-1) %% (spacer+overlap) + 1


	oligos <- sapply(c(SUMO=306,BC2L1=513,TRAF1=1506,PCNA=786), function(l) {
		#number of different mutant fragments in main assembly
		m1 <- num.aa*c((overlap+spacer)/3 - 1,  rep(spacer/3, n(l)-2), r(l)/3 - 1)
		#number of mutant fragments in alternative assembly
		m2 <- num.aa*rep(overlap/3, n(l)-1)
		#number of wildtype oligos
		w <- n(l) + n(l+overlap)
		#scaled quantities of different wildtype fragemts in main assembly
		w1 <- sapply(m1, function(m) (n(l)*m - e.M*m)/e.M)
		#scaled quantities of different wildtype fragemts in alternate assembly
		w2 <- sapply(m2, function(m) (n(l)*m - e.M*m)/e.M)

		num.oligos <- sum(m1)+sum(m2)+w
		num.oligos.scaled <- sum(m1)+sum(m2)+sum(w1)+sum(w2)

		c(single=num.oligos,scaled=num.oligos.scaled)
	})
	cbind(oligos,sum=apply(oligos,1,sum))
}

oligos(fl=150,overlap=21,e.M=2)
oligos(fl=150,overlap=42,e.M=2)
oligos(fl=150,overlap=21,e.M=4)
oligos(fl=60,overlap=21,e.M=2)
oligos(fl=60,overlap=21,e.M=4)
