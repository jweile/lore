#!/usr/bin/Rscript

#READ CO-CRYSTAL DATA
data <- read.delim("interactions.dat")
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
	synergizer <- read.delim("synergizer_mut_candidates.tsv",skip=3, row.names=1, stringsAsFactors=FALSE)
	translator <- strsplit(synergizer[,1]," ")
	names(translator) <- rownames(synergizer)
	
	translate <- function(uniprot) {
		translator[as.character(uniprot)]
	}
	list(translate=translate)
}
transl <- translator()


#READ PPI DATA FROM HI2-HI2012
hi2 <- read.delim("HI2.tsv")
hi2012 <- read.delim("HI2012.tsv")[,c(1,3)]
hi <- unique(rbind(hi2,hi2012))
hi <- apply(hi,2,as.character)

# hubs.entrez <- read.delim("hubs.tsv")

#ADJUST PROTEIN DEGREES ACCORDING TO PPI NETWORK
accessible.degrees <- sapply(names(hubs), function(hub){
	hub.entrez <- transl$translate(hub)

	pdb.interactors <- union(
		structures[structures[,1] == hub,2], 
		structures[structures[,2] == hub,1]
	)
	pdb.interactors.entrez <- transl$translate(pdb.interactors)

	hi.interactors <- union(
		hi[hi[,1] == hub.entrez,2],
		hi[hi[,2] == hub.entrez,1]
	)

	accessible.interactors <- intersect(hi.interactors, unlist(pdb.interactors.entrez))
	accessible.degree <- length(accessible.interactors)
})

accessible.degrees <- accessible.degrees[order(accessible.degrees,decreasing=T)]
accessible.hubs <- accessible.degrees[accessible.degrees > 0]



# #CHECK FOR COVERAGE IN ORFEOME 7.1
# readOrfeomeIds <- function() {
# 	con  <- file("human_orfeome71.fa", open = "r")
# 	ids <- NULL
# 	while (length(line <- readLines(con, n=1, warn=FALSE)) > 0) {
# 		if (substr(line,1,1) == ">") {
# 			id.section <- strsplit(substr(line,2,nchar(line)), "|", fixed=TRUE)[[1]][4]
# 			id <- strsplit(id.section, " ")[[1]][2]
# 			ids[length(ids)+1] <- id
# 		}
# 	}
# 	ids
# }
# orfeome.ids <- readOrfeomeIds()


#LOAD ORFEOME 8.1
trim <- function(x) gsub("(^ +)|( +$)", "", x)

orfeome.plates <- read.delim("CCSB_HuORFeome_plates.csv",stringsAsFactors=FALSE)
orfeome.plates <- apply(orfeome.plates, 2, trim)

orfeome.plates <- orfeome.plates[apply(orfeome.plates, 1, function(row) !any(row == "NULL")),]
orfeome.ids <- unique(orfeome.plates[,"ENTREZ_GENE_ID"])



#MAKE LIST OF INTERACTIONS FOR HUBS AND FILTER NON-ORFEOME
db.ids <- sapply(transl$translate(names(hubs)), function(entrez) {
	existing <- entrez[entrez %in% orfeome.ids]
	if (!is.null(existing)) existing[1] else NULL
})
interactors <- sapply(names(hubs), function(hub){
	# hub.entrez <- transl$translate(hub)

	partners <- union(
		structures[structures[,1] == hub,2], 
		structures[structures[,2] == hub,1]
	)
	partners.entrez <- sapply(transl$translate(partners), function(entrez) {
		existing <- entrez[entrez %in% orfeome.ids]
		if (!is.null(existing)) existing[1] else NULL
	})

	partners.entrez[partners.entrez %in% orfeome.ids]
	
})
db.ids <- db.ids[!is.na(db.ids)]
interactors <- interactors[names(db.ids)]



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

#Warning: This takes freaking forever!
membrane.proteins <- sapply(db.ids, function(entrezID) {
	direct.terms <- unlist(gene2go[entrezID])
	all.terms <- unlist(sapply(direct.terms, superterms))
	membrane.term %in% all.terms
})

db.ids <- db.ids[!membrane.proteins]
interactors <- interactors[names(db.ids)]

#Warning: This takes freaking forever!
membrane.interactors <- sapply(interactors, function(proteins) {
	sapply(proteins, function(entrezID) {
		direct.terms <- unlist(gene2go[entrezID])
		all.terms <- unlist(sapply(direct.terms, superterms))
		membrane.term %in% all.terms
	})
})

interactors <- sapply(names(interactors), function(protein) {
	interactors[[protein]][!membrane.interactors[[protein]]]
})


#REORDER BY REMAINING DEGREE
db.ids <- db.ids[order(sapply(interactors, length), decreasing=TRUE)]
interactors <- interactors[order(sapply(interactors, length), decreasing=TRUE)]

#CUTOFF BY DEGREE

db.ids <- db.ids[1:4]
interactors <- interactors[1:4]

to.clone <- unique(c(db.ids, unlist(interactors)))


clone.plates <- orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"] %in% to.clone, 1:3]
clone.plates <- clone.plates[!duplicated(clone.plates[,"ENTREZ_GENE_ID"]),]
rownames(clone.plates) <- clone.plates[,"ENTREZ_GENE_ID"]

for (i in 1:length(db.ids)) {
	cat("\n\nInteraction #",i,"\nDB clone: \n")
	print(clone.plates[db.ids[i],])

	cat("\nAD clones: \n")
	print(clone.plates[interactors[[i]],])
}


# sapply(transl$translate(names(filtered.interactors)), function(genes) {
# 	genes %in% as.character(plates$ENTREZ_GENE_ID)
# })





