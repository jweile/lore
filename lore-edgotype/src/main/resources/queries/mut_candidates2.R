#!/urs/bin/Rscript

source("queries/synergizer.R")
syn <- new.synergizer()

hub.cutoff <- 25

#READ PPI DATA FROM HI2-HI2012
hi2 <- read.delim("input/mut_candidates/HI2.tsv")
hi2012 <- read.delim("input/mut_candidates/HI2012.tsv")[,c(1,3)]
hi <- unique(rbind(hi2,hi2012))
hi <- apply(hi,2,as.character)

#find hubs
hi.nodes <- union(hi[,1],hi[,2])
hi.interactors <- lapply(hi.nodes, function(node) union(hi[hi[,1]==node,2],hi[hi[,2]==node,1]))
names(hi.interactors) <- hi.nodes

hi.degrees <- sapply(hi.interactors,length)
names(hi.degrees) <- hi.nodes

hi.hubs <- hi.nodes[hi.degrees > hub.cutoff]
hi.hubs <- hi.hubs[order(hi.degrees[hi.hubs],decreasing=TRUE)]




#LOAD ORFEOME 8.1
trim <- function(x) gsub("(^ +)|( +$)", "", x)

orfeome.plates <- read.delim("input/mut_candidates/CCSB_HuORFeome_plates.csv",stringsAsFactors=FALSE)
orfeome.plates <- apply(orfeome.plates, 2, trim)

orfeome.plates <- orfeome.plates[apply(orfeome.plates, 1, function(row) !any(row == "NULL")),]
orfeome.ids <- unique(orfeome.plates[,"ENTREZ_GENE_ID"])


#FILTER HI HUBS FOR PRESENCE IN ORFEOME

hubs <- hi.hubs[hi.hubs %in% orfeome.ids]
interactors <- lapply(hubs, function(node) {
	ia <- hi.interactors[[node]]
	ia[ia %in% orfeome.ids]
})
names(interactors) <- hubs
degrees <- sapply(interactors,length)
names(degrees) <- hubs
hubs <- hubs[order(degrees[hubs],decreasing=TRUE)]



#READ CO-CRYSTAL DATA
ia3d <- read.delim("input/mut_candidates/interactions.dat")
ia3d.cocrys <- ia3d[ia3d$TYPE == "Structure",c("PROT1","PROT2")]
ia3d.cocrys.tags <- apply(ia3d.cocrys, 1, paste, collapse=',')
# ia3d.nodes.uniprot <- union(ia3d.cocrys[,1],ia3d.cocrys[,2])

#translate ids with synergizer
hubs.uniprot <- syn$translate("ncbi","Homo sapiens","entrezgene","uniprot",hubs)
#look for existing cocrystal structures
hasStructure <- lapply(hubs, function(hub.entrez) {
	#get the uniprot id for the hub
	hub.uni <- hubs.uniprot[[hub.entrez]]
	#translate the hub's interactors to uniprot
	ia.uni <- syn$translate("ncbi","Homo sapiens","entrezgene","uniprot",interactors[[hub.entrez]])
	#return for each interaction whether it has a structure in ia3d
	sapply(ia.uni, function(x) any(c(paste(hub.uni,x,sep=","),paste(x,hub.uni,sep=",")) %in% ia3d.cocrys.tags) )
})
names(hasStructure) <- hubs

struc.hubs <- hubs[sapply(hasStructure,any)]

ads <- lapply(struc.hubs, function(node) {
	orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"]==node,c("ENTREZ_GENE_ID","SRC_PLATE","SRC_POS")]
})
dbs <- lapply(struc.hubs, function(node) {
	out <- do.call(rbind,lapply(interactors[[node]], function(ia){
		orfeome.plates[orfeome.plates[,"ENTREZ_GENE_ID"]==ia,c("ENTREZ_GENE_ID","SRC_PLATE","SRC_POS")]
	}))
	cbind(out, hasStructure[[node]][out[,1]])
})
names(dbs) <- struc.hubs










