/*
 * Copyright (C) 2013 Department of Molecular Genetics, University of Toronto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.on.mshri.lore.edgotype;

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.Interaction;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Molecule;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.Sparql;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.ShortestPath;
import ca.on.mshri.lore.operations.util.ShortestPath.PathNode;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.LazyInitMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expands on the DiseasePathLength method to output a Cytoscape-compatible
 * file that visualizes the paths through the network.
 * 
 * Looks at alleles that disrupt edges and are associated with a disease; it 
 * then tests whether proteins connected via the disrupted edges have a shorter path
 * to the disease than proteins that are connected through an undisrupted edge.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class DiseaseShells extends LoreOperation {
    
    public final Parameter<String> tableOutFileP = 
            Parameter.make("tableOutFile", String.class, "diseaseShells.txt");

    private Property isAssociatedWith, causes, affectsNegatively, affectsPositively;
    
    @Override
    public void run() {
        
        Logger logger = Logger.getLogger(DiseaseShells.class.getName());
        logger.setLevel(Level.ALL);
        
        logger.log(Level.INFO, "Starting Disease Shell Analysis...");
        
        String tableOutFile = getParameterValue(tableOutFileP);
        
        //initialize sparql engine for pre-defined queries in this module.
        Sparql sparql = Sparql.getInstance(DiseaseShells.class
                .getProtectionDomain().getCodeSource()
        );
        
        //shortest path algorithm object
        ShortestPath shortestPath = new ShortestPath();
        
        //define SPARQL pattern for interacting proteins.
        String hpString = "<"+InteractionModel.URI+"#hasParticipant>";
        String iaPattern = "^"+hpString+"/"+hpString;
        
        
        //define some needed properties
        isAssociatedWith = getModel()
                .getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        causes = getModel()
                .getProperty(PhenotypeModel.URI+"#isCausallyAssociatedWith");
        affectsNegatively = getModel()
                .getProperty(InteractionModel.URI+"#affectsNegatively");
        affectsPositively = getModel()
                .getProperty(InteractionModel.URI+"#affectsPositively");
        
        
        //create shortcut to interaction model interface
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        
        //get all alleles in the model
        List<Allele> alleles = model.listIndividualsOfClass(Allele.class, true);
        //set up progressbar calibrated to allele set
        CliProgressBar pb = new CliProgressBar(alleles.size());
        
        
        
        BufferedWriter w = null;
        try {
            
            w = new BufferedWriter(new FileWriter(tableOutFile));
       
            //iterate over all alleles in the model
            for (Allele allele : alleles) {

                //get the protein that is encoded by the gene for which this allele exists
                List<Protein> proteins = Protein.listEncodedProteins(allele.getGene());
                if (proteins.isEmpty()) {

                    logger.log(Level.FINE, "Skipping gene without known protein.");
                    pb.next();
                    continue;
                }
                if (proteins.size() > 1) {
                    logger.log(Level.WARNING,"Gene encodes multiple proteins!");
                }
                Protein protein = proteins.get(0);

                //get all the phenotypes of the allele
                List<Phenotype> allelePhenotypes = phenotypeOf(allele);

                //prepare a set that will contain all proteins with the same phenotype annotation
                Set<Protein> protsOfSamePheno = new HashSet<Protein>();

                logger.log(Level.FINEST, "Computing set of proteins with same phenotype...");

                //for each phenotype...
                for (Phenotype pheno : allelePhenotypes) {
                    //get all the genes that are associated with the same phenotype
                    List<Individual> geneInds = sparql
                            .queryIndividuals(model, 
                            "getGenesForPhenotype", "gene", pheno.getURI(), pheno.getURI()
                    );

                    //for each of these genes...
                    for (Individual geneInd : geneInds) {
                        Gene gene = Gene.fromIndividual(geneInd);
                        //get the encoded proteins and add them to our list.
                        protsOfSamePheno.addAll(Protein.listEncodedProteins(gene));
                    }

                }
                /* we will want to make sure that the set of proteins with the same phenotype
                 * does not contain our original protein, otherwise all the paths we find
                 * will be of length 1
                 */
                protsOfSamePheno.remove(protein);

                if (protsOfSamePheno.isEmpty()) {
                    pb.next();
                    logger.log(Level.FINEST, "Skipping singleton phenotype.");
                    continue;
                }


               
                
//                MiniNetwork net = new MiniNetwork();
                int disruptCount = 0, maintainCount = 0;
//                IntArrayList pathLengths = new IntArrayList();
//                
//                addAsTargets(protsOfSamePheno,net);

                StringBuilder b = new StringBuilder();
                
                //for all the interactions of the protein...
                for (PhysicalInteraction interaction : Interaction
                        .listInteractions(protein, PhysicalInteraction.class)) {

                    boolean disrupted = false;
                    if (allele.hasProperty(affectsNegatively, interaction)) {
                        disrupted = true;
                        disruptCount++;
                    } else if (allele.hasProperty(affectsPositively, interaction)) {
                        disrupted = false;
                        maintainCount++;
                    } else {
                        //if disruption has not been tested for, discard this interaction
                        //(because counting them as maintained would be unfair)
                        continue;
                    }

                    //get the other proteins involved in the interaction.
                    List<Molecule> interactors = interaction.listParticipants();
                    interactors.remove(protein);

                    //make sure it's a binary interaction
                    if (interactors.size() == 1) {

                        Protein interactor = Protein.fromIndividual(interactors.get(0));
                        
                        b.append('\n');
                        b.append(disrupted ? "disrupted: ":"maintained: ");
                        b.append(interactor.getXRefValue(model.ENTREZ));
                        b.append(" (").append(interactor.getXRefValue(model.HGNC)).append(")\n");
                        
                        String table = traceNeighbourhood(interactor,protein, protsOfSamePheno);
                        b.append(table).append("\n");

//                        w.append("\nPath through ").append(interactor.getXRefValue(model.ENTREZ));
//                        w.write(disrupted ? " (perturbed)\n" : " (unperturbed):\n");

                        //calculate shortest path from interactor to targets
                        
//                        PathNode path = shortestPath.find(
//                                interactor, protsOfSamePheno, 
//                                iaPattern, Collections.singleton(protein)
//                        );
//                        
//                        if (path != null) {
//
//                            processPath(path, protein, disrupted, net);
//                            pathLengths.add(path.getDistance()+1);
//                            
//                        } else {
//    //                        //if no path is found say so
//                            processNoPath(protein, interactor, disrupted, net);
////                            w.append("No path!\n");
//                            pathLengths.add(-1);
//
//                        }


                    } else {
                        logger.log(Level.FINER, interactors.isEmpty() ? 
                                "Ignoring self-interaction": 
                                "Ignoring multi-interaction");
                    }
                }
                
                
                if (disruptCount+maintainCount > 0) {
                    
                    String tables = b.toString();
                    //output allele information
                    w.append("\n\nAllele: ");
                    w.append(allele.getGene().getXRefValue(model.ENTREZ));
                    w.append(" (").append(allele.getGene().getXRefValue(model.HGNC)).append(") ");
                    for (Mutation mut : allele.listMutations()) {
                        PointMutation pmut = PointMutation.fromIndividual(mut);
                        w.append(pmut.getFromAminoAcid()).append(pmut.getPosition()+"").append(pmut.getToAminoAcid()).append(' ');
                    }
                    w.write("\nDiseases: ");
                    b = new StringBuilder();
                    for (Phenotype pheno : allelePhenotypes) {
                        b.append(pheno.getLabel(null)).append("; ");
                    }
                    b.delete(b.length()-2, b.length());
                    w.write(b.toString());

                    w.write("\nCo-annotated: ");
                    b = new StringBuilder();
                    for (Protein prot : protsOfSamePheno) {
                        b.append(prot.getXRefValue(model.ENTREZ)).append(", ");
                    }
                    b.delete(b.length()-2, b.length());
                    w.append(b.toString());

//                    w.append("\nPath lengths: ").append(pathLengths.toString());
                    String edgotype = disruptCount==0 ? "quasi-wt" : maintainCount==0 ? "quasi-null" : "edgetic";
                    w.append("\nEdgotype: ").append(edgotype).append('\n');
                    
                    w.append(tables);

//                    for (Edge edge: net.getEdges()) {
//                        String entrez = edge.getProt1().getXRefValue(model.ENTREZ);
//                        String hgnc = edge.getProt1().getXRefValue(model.HGNC);
//                        w.append(entrez).append("\t");
//                        w.append(hgnc == null ? entrez : hgnc).append("\t");
//
//                        entrez = edge.getProt2().getXRefValue(model.ENTREZ);
//                        hgnc = edge.getProt2().getXRefValue(model.HGNC);
//                        w.append(entrez).append("\t");
//                        w.append(hgnc == null ? entrez : hgnc).append("\t");
//
//                        w.append(!net.getNodeProps().containsKey(edge.getProt1()) ? 
//                                "-" : 
//                                net.getNodeProps().get(edge.getProt1()).toString()).append("\t");
//                        w.append(!net.getNodeProps().containsKey(edge.getProt2()) ? 
//                                "-" : 
//                                net.getNodeProps().get(edge.getProt2()).toString()).append("\t");
//                        w.append(!net.getEdgeProps().containsKey(edge) ? 
//                                "-" : 
//                                net.getEdgeProps().get(edge).toString()).append("\n");
//
//                    }

                }

                //update progress bar
                pb.next();
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to file",e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Unable to close stream!", ex);
                }
            }
        }
        
    }
    
    /**
     * returns the phenotypes associated with a given individual
     * @param individual
     * @return 
     */
    private List<Phenotype> phenotypeOf(Individual individual) {
        
        List<Phenotype> list = new ArrayList<Phenotype>();
        
        NodeIterator it = individual.listPropertyValues(isAssociatedWith);
        while (it.hasNext()) {
            list.add(Phenotype.fromIndividual(it.next().as(Individual.class)));
        }
        it = individual.listPropertyValues(causes);
        while (it.hasNext()) {
            list.add(Phenotype.fromIndividual(it.next().as(Individual.class)));
        }
        
        
        return list;
    }
    

    @Override
    public boolean requiresReasoner() {
        return false;
    }
//
//    private void processPath(PathNode path, Protein focalProtein, boolean disrupted, MiniNetwork net) {
//        
////        StringBuilder b = new StringBuilder();
//        
////        LazyInitMap<Edge,Set<String>> edgeProps = new LazyInitMap<Edge, Set<String>>(HashSet.class);
////        LazyInitMap<Protein,Set<String>> nodeProps = new LazyInitMap<Protein, Set<String>>(HashSet.class);
////        Set<Edge> edges = new HashSet<Edge>();
//        
//        Set<Protein> pathProteins = new HashSet<Protein>();
//        Set<Protein> neighbourProteins = new HashSet<Protein>();
//        
//        int i = 0;
//        
//        //is path of length 1 (counted from focal node)?
//        boolean l1 = path.getPredecessor() == null;
//        
//        while (path.getPredecessor() != null) {
//            
//            Protein protein = Protein.fromIndividual(path.getValue());
//            Protein predProtein = Protein.fromIndividual(path.getPredecessor().getValue());
//            
//            if (i++ == 0) {
//                //end node node
//                pathProteins.add(protein);
//                pathProteins.add(predProtein);
//                
//                net.addNodeProp(protein,"end");
//                net.addEdge(predProtein, protein, "path");
//            } else {
//                //intermediate node
//                pathProteins.add(predProtein);
//                
//                net.addEdge(predProtein, protein, "path");
//            }
//            
//            //go to predecessor unless we're at the last node already
//            if (path.getPredecessor() != null) {
//                path = path.getPredecessor();
//            }
//            
//        }
//        
//        //add edge from focal protein to neighbour
//        Protein neighbourProt = Protein.fromIndividual(path.getValue());
//        pathProteins.add(focalProtein);
//        net.addEdge(focalProtein, neighbourProt, disrupted ? "disrupted":"maintained");
//        net.addNodeProp(focalProtein, "start");
//        if (l1) {
//            //if path length is 1 (i.e. 0) then nodes haven't been added yet
//            //in the loop above.
//            pathProteins.add(neighbourProt);
//            net.addNodeProp(neighbourProt,"end");
//        }
//        
//        //track down neighbours
//        for (Protein protein : pathProteins) {
//            for (Protein neighbour : getNeighbours(protein)) {
//                if (!pathProteins.contains(neighbour)) {
//                    neighbourProteins.add(neighbour);
//                    net.addEdge(protein, neighbour);
//                }
//            }
//        }
//        
//        //find edges between neighbours
//        for (Protein protein: neighbourProteins) {
//            for (Protein neighbour: getNeighbours(protein)) {
//                if (neighbourProteins.contains(neighbour)) {
//                    net.addEdge(protein,neighbour);
//                }
//            }
//        }
//
////        return b.toString();
//        
//    }
    
    private Set<Protein> getNeighbours(Protein p) {
        
        Set<Protein> neighbours = new HashSet<Protein>();
        
        for (PhysicalInteraction ia : Interaction.listInteractions(p, PhysicalInteraction.class)) {
            for (Molecule participant : ia.listParticipants()) {
                if (!participant.equals(p)) {
                    neighbours.add(Protein.fromIndividual(participant));
                }
            }
        }
        
        return neighbours;
    }

//    private void processNoPath(Protein protein, Protein interactor, boolean disrupted, MiniNetwork net) {
//        net.addEdge(protein, interactor, disrupted ? "disrupted":"maintained");
//        net.addNodeProp(protein, "start");
//        net.addNodeProp(interactor, "end");
//        for (Protein neighbour : getNeighbours(protein)) {
//            net.addEdge(protein, neighbour);
//        }
//        for (Protein neighbour : getNeighbours(interactor)) {
//            net.addEdge(protein, neighbour);
//        }
//    }

//    private void addAsTargets(Set<Protein> proteins, MiniNetwork net) {
//        for (Protein p : proteins) {
//            for (Protein neighbour : getNeighbours(p)) {
//                net.addEdge(p, neighbour);
//            }
//            net.addNodeProp(p, "end");
//        }
//    }

    private String traceNeighbourhood(Protein interactor, Protein protein, Set<Protein> targets) {
        
        StringBuilder b = new StringBuilder("1\t");
        
        int maxShell = 4;
        
        List<Set<Protein>> shells = new ArrayList<Set<Protein>>();
        Set<Protein> closed = new HashSet<Protein>();
        
        closed.add(protein);
        closed.add(interactor);
        
        //interactor is the first shell
        Set<Protein> set = new HashSet<Protein>();
        set.add(interactor);
        shells.add(set);
        if (targets.contains(interactor)) {
            b.append("1\n");
        } else {
            b.append("0\n");
        }
        
        int currentShell = 0;
        
        while (++currentShell < maxShell) {
            int hits = 0;
            set = new HashSet<Protein>();
            for (Protein p : shells.get(currentShell-1)) {
                for (Protein neighbour : getNeighbours(p)) {
                    if (!closed.contains(neighbour)) {
                        set.add(neighbour);
                        closed.add(neighbour);
                        if (targets.contains(neighbour)) {
                            hits++;
                        }
                    }
                }
            }
            shells.add(set);
            b.append(set.size()).append('\t').append(hits).append('\n');
        }
        
        return b.toString();
    }
    
//    private static class MiniNetwork {
//        
//        private LazyInitMap<Edge, Set<String>> edgeProps = new LazyInitMap<Edge, Set<String>>(TreeSet.class);
//        private LazyInitMap<Protein, Set<String>> nodeProps = new LazyInitMap<Protein, Set<String>>(TreeSet.class);
//        private Set<Edge> edges = new HashSet<Edge>();
//        
//        public Edge addEdge(Protein a, Protein b) {
//            Edge e = new Edge(a,b);
//            edges.add(e);
//            return e;
//        }
//        
//        public void addEdge(Protein a, Protein b, String prop) {
//            edgeProps.getOrCreate(addEdge(a,b)).add(prop);
//        }
//        
//        public void addNodeProp(Protein p, String prop) {
//            nodeProps.getOrCreate(p).add(prop);
//        }
//
//        public LazyInitMap<Edge, Set<String>> getEdgeProps() {
//            return edgeProps;
//        }
//
//        public LazyInitMap<Protein, Set<String>> getNodeProps() {
//            return nodeProps;
//        }
//
//        public Set<Edge> getEdges() {
//            return edges;
//        }
//        
//        
//
//    }
//    
//    private static class Edge {
//        
//        private Protein prot1, prot2;
//        
//        Edge(Protein p1, Protein p2) {
//            if (p1.getURI().compareTo(p2.getURI()) > 0) {
//                prot1 = p2;
//                prot2 = p1;
//            } else {
//                prot1 = p1;
//                prot2 = p2;
//            }
//        }
//        
//        public Protein getProt1() {
//            return prot1;
//        }
//
//        public Protein getProt2() {
//            return prot2;
//        }
//
//        @Override
//        public int hashCode() {
//            int hash = 7;
//            hash = 97 * hash + (this.prot1 != null ? this.prot1.hashCode() : 0);
//            hash = 97 * hash + (this.prot2 != null ? this.prot2.hashCode() : 0);
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final Edge other = (Edge) obj;
//            if (this.prot1 != other.prot1 && (this.prot1 == null || !this.prot1.equals(other.prot1))) {
//                return false;
//            }
//            if (this.prot2 != other.prot2 && (this.prot2 == null || !this.prot2.equals(other.prot2))) {
//                return false;
//            }
//            return true;
//        }
//        
//        
//        
//    }
//    
//    private static class InteractionItem {
//        
//        private Protein prot1, prot2;
//        private String status1, status2, statusE;
//        
//        InteractionItem(Protein p1, Protein p2) {
//            if (p1.getURI().compareTo(p2.getURI()) > 0) {
//                prot1 = p2;
//                prot2 = p1;
//            } else {
//                prot1 = p1;
//                prot2 = p2;
//            }
//        }
//
//        public InteractionItem(Protein prot1, Protein prot2, 
//                String status1, String status2, String statusE) {
//            this(prot1,prot2);
//            this.status1 = status1;
//            this.status2 = status2;
//            this.statusE = statusE;
//        }
//        
//
//        public String getStatus1() {
//            return status1;
//        }
//
//        public void setStatus1(String status1) {
//            this.status1 = status1;
//        }
//
//        public String getStatus2() {
//            return status2;
//        }
//
//        public void setStatus2(String status2) {
//            this.status2 = status2;
//        }
//
//        public String getStatusE() {
//            return statusE;
//        }
//
//        public void setStatusE(String statusE) {
//            this.statusE = statusE;
//        }
//
//        public Protein getProt1() {
//            return prot1;
//        }
//
//        public Protein getProt2() {
//            return prot2;
//        }
//        
//        public String print(InteractionModel model) {
//            StringBuilder b = new StringBuilder();
//            
//            b.append(prot1.getXRefValue(model.ENTREZ)).append('\t');
//            b.append(prot2.getXRefValue(model.ENTREZ)).append('\t');
//            b.append(prot1.getXRefValue(model.HGNC)).append('\t');
//            b.append(prot2.getXRefValue(model.HGNC)).append('\t');
//            b.append(status1 == null ? "" : status1).append('\t');
//            b.append(status2 == null ? "" : status2).append('\t');
//            b.append(statusE == null ? "" : statusE).append('\n');
//            
//            return b.toString();
//        }
//
//        @Override
//        public int hashCode() {
//            int hash = 3;
//            hash = 37 * hash + (this.prot1 != null ? this.prot1.hashCode() : 0);
//            hash = 37 * hash + (this.prot2 != null ? this.prot2.hashCode() : 0);
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final InteractionItem other = (InteractionItem) obj;
//            if (this.prot1 != other.prot1 && (this.prot1 == null || !this.prot1.equals(other.prot1))) {
//                return false;
//            }
//            if (this.prot2 != other.prot2 && (this.prot2 == null || !this.prot2.equals(other.prot2))) {
//                return false;
//            }
//            return true;
//        }
//        
//        
//        
//        
//    }
}
