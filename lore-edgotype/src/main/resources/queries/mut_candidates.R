#!/usr/bin/Rscript

#READ CO-CRYSTAL DATA
data <- read.delim("input/mut_candidates/interactions.dat")
structures <- data[data$TYPE == "Structure",]
proteins <- union(structures$PROT1, structures$PROT2)
degrees <- sapply(proteins, function(prot) {
	sum(apply(structures[,1:2], 1, function(x) { 
		x[1]==prot || x[2]==prot 
	}))
})

#FIND HUB PROTEINS IN CO-CRYSTALS
degrees <- degrees[order(degrees,decreasing=T)]
hubs <- degrees[degrees > 5]

write.table(data.frame(prot=proteins),"to_translate.txt", row.names=FALSE, quote=FALSE)
# hub.structures <-  structures[structures[,1] %in% names(hubs) | structures[,2] %in% names(hubs),]
# to.translate <- unique(c(as.character(hub.structures$PROT1), as.character(hub.structures$PROT2)))
# write.table(data.frame(prot=to.translate),"to_translate.txt", row.names=FALSE, quote=FALSE)

#CREATE UNIPROT-ENTREZ TRANSLATOR
translator <- function() {
	synergizer <- read.delim("input/mut_candidates/synergizer_mut_candidates.tsv",skip=3, row.names=1, stringsAsFactors=FALSE)
	translator <- strsplit(synergizer[,1]," ")
	names(translator) <- rownames(synergizer)
	
	translate <- function(uniprot) {
		translator[as.character(uniprot)]
	}
	list(translate=translate)
}
transl <- translator()


#LOAD ORFEOME 8.1
trim <- function(x) gsub("(^ +)|( +$)", "", x)

orfeome.plates <- read.delim("input/mut_candidates/CCSB_HuORFeome_plates.csv",stringsAsFactors=FALSE)
orfeome.plates <- apply(orfeome.plates, 2, trim)

orfeome.plates <- orfeome.plates[apply(orfeome.plates, 1, function(row) !any(row == "NULL")),]
orfeome.ids <- unique(orfeome.plates[,"ENTREZ_GENE_ID"])



#TRANSLATE TO ENTREZ GENE IDS AND LOOK UP ORFEOME AVAILABILITY
hub.interactions <- list()
for (hub.uniprot in names(hubs)) {

	hub.entrez <- transl$translate(hub.uniprot)
	hub.entrez.instock <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% hub.entrez, "ENTREZ_GENE_ID"]

	if (length(hub.entrez.instock) > 0) {

		if (length(hub.entrez.instock) > 1) {
			warning("Discarding ",length(hub.entrez.instock)-1," alternative ORFs for ",hub.uniprot)
			hub.entrez.instock <- hub.entrez.instock[1]
		}

		ias.uniprot <- union(
			structures[structures[,1] == hub.uniprot,2], 
			structures[structures[,2] == hub.uniprot,1]
		)

		for (ia.uniprot in ias.uniprot) {

			ia.entrez <- transl$translate(ia.uniprot)
			ia.entrez.instock <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% ia.entrez, "ENTREZ_GENE_ID"]

			if (length(ia.entrez.instock) > 0) {

				if (length(ia.entrez.instock) > 1) {
					warning("Discarding ",length(ia.entrez.instock)-1," alternative ORFs for ", ia.uniprot)
					ia.entrez.instock <- ia.entrez.instock[1]
				}

				hub.interactions[[hub.entrez.instock]] <- c(hub.interactions[[hub.entrez.instock]], ia.entrez.instock)

			}
		}
	}
}

#EXTRACT POSITIONS ON PLATES
to.clone <- union(names(hub.interactions), unlist(hub.interactions))
clone.plates <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% to.clone, 1:3]
clone.plates <- clone.plates[!duplicated(clone.plates[,"ENTREZ_GENE_ID"]),]
rownames(clone.plates) <- clone.plates[,"ENTREZ_GENE_ID"]

#MAP AD POSITIONS
ad.plates <- clone.plates[names(hub.interactions),]

#MAP DB POSITIONS
db.plates <- sapply(hub.interactions, function(ias){
	clone.plates[ias,]
})



#READ PPI DATA FROM HI2-HI2012
hi2 <- read.delim("input/mut_candidates/HI2.tsv")
hi2012 <- read.delim("input/mut_candidates/HI2012.tsv")[,c(1,3)]
hi <- unique(rbind(hi2,hi2012))
hi <- apply(hi,2,as.character)


#ANNOTATE TABLES WITH HI2-2012
for (hub in names(db.plates)) {
	ias <- if (is.matrix(db.plates[[hub]])) 
			db.plates[[hub]][,"ENTREZ_GENE_ID"]
		else
			db.plates[[hub]]["ENTREZ_GENE_ID"]

	y2h <- sapply(ias, function(ia) {
		any((hi[,1] == hub & hi[,2] == ia) | (hi[,2] == hub & hi[,1] == ia))
	})
	db.plates[[hub]] <- if (is.matrix(db.plates[[hub]])) 
			cbind(db.plates[[hub]],HI=y2h)
		else 
			c(db.plates[[hub]],HI=y2h)
}




#ELIMINATE MEMBRANE PROTEINS
library("org.Hs.eg.db")
library("GO.db")

gene2go <- sapply(as.list(org.Hs.egGO), names)
superterms <- function(tid) {
  all.parents <- GOCCPARENTS[[tid]]
  parents <- all.parents[names(all.parents)=="is_a"]
  unique(c(parents,unlist(sapply(parents, superterms))))
}
membrane.term <- "GO:0016020"
is.membrane.protein <- function(entrezID) {
	direct.terms <- unlist(gene2go[entrezID])
	all.terms <- unlist(sapply(direct.terms, superterms))
	membrane.term %in% all.terms
}

#Warning: This takes freaking forever!
ad.plates <- cbind(ad.plates, GO.MEMBRANE=sapply(ad.plates[,"ENTREZ_GENE_ID"], is.membrane.protein))

for (hub in names(db.plates)) {
	ias <- if (is.matrix(db.plates[[hub]])) 
			db.plates[[hub]][,"ENTREZ_GENE_ID"]
		else
			db.plates[[hub]]["ENTREZ_GENE_ID"]
	mem <- sapply(ias, is.membrane.protein)
	db.plates[[hub]] <- if (is.matrix(db.plates[[hub]])) 
			cbind(db.plates[[hub]],GO.MEMBRANE=mem)
		else 
			c(db.plates[[hub]],GO.MEMBRANE=mem)
}





# # hubs.entrez <- read.delim("hubs.tsv")

# #ADJUST PROTEIN DEGREES ACCORDING TO PPI NETWORK
# accessible.interactors <- sapply(names(hubs), function(hub){
# 	hub.entrez <- transl$translate(hub)

# 	pdb.interactors <- union(
# 		structures[structures[,1] == hub,2], 
# 		structures[structures[,2] == hub,1]
# 	)
# 	pdb.interactors.entrez <- transl$translate(pdb.interactors)

# 	hi.interactors <- union(
# 		hi[hi[,1] == hub.entrez,2],
# 		hi[hi[,2] == hub.entrez,1]
# 	)

# 	# accessible.interactors <- 
# 	intersect(hi.interactors, unlist(pdb.interactors.entrez))
# 	# accessible.degree <- length(accessible.interactors)
# })

# accessible.degrees <- sapply(accessible.interactors,length)
# accessible.interactors <- accessible.interactors[order(accessible.degrees,decreasing=T)]
# accessible.degrees <- accessible.degrees[order(accessible.degrees,decreasing=T)]
# accessible.hubs <- accessible.interactors[accessible.degrees > 0]




# # #CHECK FOR COVERAGE IN ORFEOME 7.1
# # readOrfeomeIds <- function() {
# # 	con  <- file("human_orfeome71.fa", open = "r")
# # 	ids <- NULL
# # 	while (length(line <- readLines(con, n=1, warn=FALSE)) > 0) {
# # 		if (substr(line,1,1) == ">") {
# # 			id.section <- strsplit(substr(line,2,nchar(line)), "|", fixed=TRUE)[[1]][4]
# # 			id <- strsplit(id.section, " ")[[1]][2]
# # 			ids[length(ids)+1] <- id
# # 		}
# # 	}
# # 	ids
# # }
# # orfeome.ids <- readOrfeomeIds()


# #DETERMINE PLATE POSITIONS FOR HI2012 ACCESSIBLE INTERACTIONS
# hub.genes <- unlist(transl$translate(names(accessible.hubs)))
# to.clone <- c(hub.genes, unlist(accessible.hubs))
# clone.plates <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% to.clone, 1:3]
# clone.plates <- clone.plates[!duplicated(clone.plates[,"ENTREZ_GENE_ID"]),]
# rownames(clone.plates) <- clone.plates[,1]

# plate.info <- NULL
# for (i in 1:length(hub.genes)) {
# 	try({
# 	plate.info <- rbind(plate.info, clone.plates[hub.genes[i],])
# 	for (gene in accessible.hubs[[i]]) {
# 		try({
# 			plate.info <- rbind(plate.info, clone.plates[gene,])
# 		})
# 	}
# 	})
# }


# #MAKE LIST OF INTERACTIONS FOR HUBS AND FILTER NON-ORFEOME
# db.ids <- sapply(transl$translate(names(hubs)), function(entrez) {
# 	existing <- entrez[entrez %in% orfeome.ids]
# 	if (!is.null(existing)) existing[1] else NULL
# })
# interactors <- sapply(names(hubs), function(hub){
# 	# hub.entrez <- transl$translate(hub)

# 	partners <- union(
# 		structures[structures[,1] == hub,2], 
# 		structures[structures[,2] == hub,1]
# 	)
# 	partners.entrez <- sapply(transl$translate(partners), function(entrez) {
# 		existing <- entrez[entrez %in% orfeome.ids]
# 		if (!is.null(existing)) existing[1] else NULL
# 	})

# 	partners.entrez[partners.entrez %in% orfeome.ids]
	
# })
# db.ids <- db.ids[!is.na(db.ids)]
# interactors <- interactors[names(db.ids)]



# #ELIMINATE MEMBRANE PROTEINS
# library("org.Hs.eg.db")
# library("GO.db")

# gene2go <- sapply(as.list(org.Hs.egGO), names)
# superterms <- function(tid) {
#   all.parents <- GOCCPARENTS[[tid]]
#   parents <- all.parents[names(all.parents)=="is_a"]
#   unique(c(parents,unlist(sapply(parents, superterms))))
# }
# membrane.term <- "GO:0016020"

# #Warning: This takes freaking forever!
# membrane.proteins <- sapply(db.ids, function(entrezID) {
# 	direct.terms <- unlist(gene2go[entrezID])
# 	all.terms <- unlist(sapply(direct.terms, superterms))
# 	membrane.term %in% all.terms
# })

# db.ids <- db.ids[!membrane.proteins]
# interactors <- interactors[names(db.ids)]

# #Warning: This takes freaking forever!
# membrane.interactors <- sapply(interactors, function(proteins) {
# 	sapply(proteins, function(entrezID) {
# 		direct.terms <- unlist(gene2go[entrezID])
# 		all.terms <- unlist(sapply(direct.terms, superterms))
# 		membrane.term %in% all.terms
# 	})
# })

# interactors <- sapply(names(interactors), function(protein) {
# 	interactors[[protein]][!membrane.interactors[[protein]]]
# })


# #REORDER BY REMAINING DEGREE
# db.ids <- db.ids[order(sapply(interactors, length), decreasing=TRUE)]
# interactors <- interactors[order(sapply(interactors, length), decreasing=TRUE)]

# #CUTOFF BY DEGREE

# db.ids <- db.ids[1:4]
# interactors <- interactors[1:4]

# to.clone <- unique(c(db.ids, unlist(interactors)))


# clone.plates <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% to.clone, 1:3]
# clone.plates <- clone.plates[!duplicated(clone.plates[,"ENTREZ_GENE_ID"]),]
# rownames(clone.plates) <- clone.plates[,"ENTREZ_GENE_ID"]

# for (i in 1:length(db.ids)) {
# 	cat("\n\nInteraction #",i,"\nDB clone: \n")
# 	print(clone.plates[db.ids[i],])

# 	cat("\nAD clones: \n")
# 	print(clone.plates[interactors[[i]],])
# }


# # sapply(transl$translate(names(filtered.interactors)), function(genes) {
# # 	genes %in% as.character(plates$ENTREZ_GENE_ID)
# # })





