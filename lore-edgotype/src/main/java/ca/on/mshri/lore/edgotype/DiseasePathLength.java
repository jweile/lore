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

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
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
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.IntArrayList;
import de.jweile.yogiutil.LazyInitMap;
import de.jweile.yogiutil.Pair;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks at alleles that disrupt edges and are associated with a disease; it 
 * then tests whether proteins connected via the disrupted edges have a shorter path
 * to the disease than proteins that are connected through an undisrupted edge.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class DiseasePathLength extends LoreOperation {
    
    public final Parameter<String> disruptedOutFileP = 
            Parameter.make("disruptedOutFile", String.class, "dist_disrupted.txt");
    public final Parameter<String> disruptedRandomOutFileP = 
            Parameter.make("disruptedRandomOutFile", String.class, "dist_disrupted_random.txt");
    public final Parameter<String> disruptedDegreeOutFileP = 
            Parameter.make("disruptedDegreeOutFile", String.class, "dist_disrupted_degree.txt");
    public final Parameter<String> maintainedOutFileP = 
            Parameter.make("maintainedOutFile", String.class, "dist_maintained.txt");
    public final Parameter<String> maintainedRandomOutFileP = 
            Parameter.make("maintainedRandomOutFile", String.class, "dist_maintained_random.txt");
    public final Parameter<String> maintainedDegreeOutFileP = 
            Parameter.make("maintainedDegreeOutFile", String.class, "dist_maintained_degree.txt");
    public final Parameter<String> tableOutFileP = 
            Parameter.make("tableOutFile", String.class, "dist_table.txt");
    public final Parameter<Integer> diseaseGroupMinP = 
            Parameter.make("diseaseGroupMin", Integer.class, 5);
    public final Parameter<Integer> diseaseGroupMaxP = 
            Parameter.make("diseaseGroupMax", Integer.class, 20);
    

    private Property isAssociatedWith, causes, affectsNegatively, affectsPositively, isa;
    
    private Logger logger;
    
    @Override
    public void run() {
        
        Authority ccsbMut = Authority.createOrGet(getModel(), "CCSB-Mutant");
        Authority doAuth = Authority.createOrGet(getModel(), "DO");
        
        logger = Logger.getLogger(DiseasePathLength.class.getName());
        logger.setLevel(Level.ALL);
        
        logger.log(Level.INFO, "Starting Disease Path Length Analysis...");
        
        String disruptedOutFile = getParameterValue(disruptedOutFileP);
        String maintainedOutFile = getParameterValue(maintainedOutFileP);
        String tableOutFile = getParameterValue(tableOutFileP);
        String disruptedRandomOutFile = getParameterValue(disruptedRandomOutFileP);
        String maintainedRandomOutFile = getParameterValue(maintainedRandomOutFileP);
        String disruptedDegreeOutFile = getParameterValue(disruptedDegreeOutFileP);
        String maintainedDegreeOutFile = getParameterValue(maintainedDegreeOutFileP);
        
        int groupMin = getParameterValue(diseaseGroupMinP);
        int groupMax = getParameterValue(diseaseGroupMaxP);
        
        //initialize sparql engine for pre-defined queries in this module.
        Sparql sparql = Sparql.getInstance(DiseasePathLength.class
                .getProtectionDomain().getCodeSource()
        );
        
        indexDiseaseAnnotations(sparql, groupMin, groupMax);
        
        //shortest path algorithm object
        ShortestPath shortestPath = new ShortestPath();
        
        //define SPARQL pattern for interaction proteins.
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
        isa = getModel()
                .getProperty(PhenotypeModel.URI+"#is_a");
        
        //prepare some arrays to store the results
        IntArrayList disruptedList = new IntArrayList(),
                maintainedList = new IntArrayList();
        IntArrayList disruptedDegreeList = new IntArrayList(),
                maintainedDegreeList = new IntArrayList();
        
        //textual output buffer
        StringBuilder textB = new StringBuilder();
        textB.append("Entrez_focal\tMutID\tEntrez_neighbour\tEdge_state\tDOID\tDisease_name\tNum_targets\tEntrez_target\tPath_length\tDegree\n");
        
        //create shortcut to interaction model interface
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        
        /* Later on we want to compare the results with permuted disease annotations
         * So I'm introducing a list where i can cache the protein sets.
         */
        List<Set<Protein>> allTargets = new ArrayList<Set<Protein>>();
        List<Pair<Protein>> allDisruptedOrigins = new ArrayList<Pair<Protein>>();
        List<Pair<Protein>> allMaintainedOrigins = new ArrayList<Pair<Protein>>();
        
        //get all alleles with edgotypes
        List<Allele> alleles = new ArrayList<Allele>();
        for (Individual ind : sparql.queryIndividuals(model, "getEdgotypedAlleles", "allele")) {
            alleles.add(Allele.fromIndividual(ind));
        }
        //set up progressbar calibrated to allele set
        CliProgressBar pb = new CliProgressBar(alleles.size());
       
        //iterate over all alleles in the model
        for (Allele allele : alleles) {
                        
            //get the protein that is encoded by the gene for which this allele exists
            List<Protein> proteins = Protein.listEncodedProteins(allele.getGene());
            if (proteins.isEmpty()) {
                
                logger.log(Level.WARNING, "Skipping gene without known protein: "+
                        allele.getGene().getXRefValue(model.ENTREZ));
                pb.next();
                continue;
            }
            if (proteins.size() > 1) {
                logger.log(Level.WARNING,"Gene encodes multiple proteins: "+
                        allele.getGene().getXRefValue(model.ENTREZ));
            }
            Protein protein = proteins.get(0);
            
            //get all the phenotypes of the allele
//            List<Phenotype> allelePhenotypes = phenotypeOf(allele);
            
            
            //prepare a set that will contain all proteins with the same phenotype annotation
            Set<Protein> protsOfSamePheno = new HashSet<Protein>();
            Phenotype groupPheno = findBestPhenotype(allele, sparql, groupMin, groupMax);
            if (groupPheno != null) {
                for (Gene groupGene : pheno2geneSet.get(groupPheno)) {
                    
                    List<Protein> groupProts = Protein.listEncodedProteins(groupGene);
                    if (groupProts.isEmpty()) {
                        logger.log(Level.FINE, "Group gene without known protein: "+
                                groupGene.getXRefValue(model.ENTREZ));
                        continue;
                    }
                    if (groupProts.size() > 1) {
                        logger.log(Level.WARNING,"Group gene encodes multiple proteins: "+
                                groupGene.getXRefValue(model.ENTREZ));
                    }
                    protsOfSamePheno.addAll(groupProts);
                }
            } else {
                logger.log(Level.WARNING, "No phenotype found for allele: "+
                        allele.getXRefValue(ccsbMut));
                pb.next();
                continue;
            }
            
//            //for each phenotype...
//            for (Phenotype pheno : allelePhenotypes) {
//                //get all the genes that are associated with the same phenotype
//                List<Individual> geneInds = sparql
//                        .queryIndividuals(model, 
//                        "getGenesForPhenotype", "gene", pheno.getURI(), pheno.getURI()
//                );
//                
//                //for each of these genes...
//                for (Individual geneInd : geneInds) {
//                    Gene gene = Gene.fromIndividual(geneInd);
//                    //get the encoded proteins and add them to our list.
//                    protsOfSamePheno.addAll(Protein.listEncodedProteins(gene));
//                }
//                
//            }
            
            /* we will want to make sure that the set of proteins with the same phenotype
             * does not contain our original protein, otherwise all the paths we find
             * will be of length 1
             */
            protsOfSamePheno.remove(protein);
            
            if (protsOfSamePheno.isEmpty()) {
                pb.next();
                logger.log(Level.WARNING, "Skipping singleton disease gene: "+
                        protein.getXRefValue(model.ENTREZ));
                continue;
            }
            
            
            //Iterate over interactions and sort into disruptes/maintained
            List<Protein> disruptedInteractors = new ArrayList<Protein>(), 
                    maintainedInteractors = new ArrayList<Protein>();
            for (PhysicalInteraction interaction : Interaction
                    .listInteractions(protein, PhysicalInteraction.class)) {
                List<Molecule> interactors = interaction.listParticipants();
                interactors.remove(protein);
                
                //make sure it's a binary interaction
                if (interactors.isEmpty()) {
                    logger.log(Level.WARNING, "Ignoring self-interaction");
                    continue;
                } else if (interactors.size() > 1) {
                    logger.log(Level.WARNING, "Ignoring non-binary interaction");
                    continue;
                }
                
                Protein interactor = Protein.fromIndividual(interactors.get(0));
                
                if (allele.hasProperty(affectsNegatively, interaction)) {
                    disruptedInteractors.add(interactor);
                } else if (allele.hasProperty(affectsPositively, interaction)) {
                    maintainedInteractors.add(interactor);
                } else {
                    logger.log(Level.WARNING, "Ignoring untested interaction");
                    continue;
                }
                
            }
            
            //discard quasi-nulls!
            if (maintainedInteractors.isEmpty()) {
                logger.log(Level.WARNING,"Discarding quasi-null");
                pb.next();
                continue;
            }
            //discard degree 1
            if (maintainedInteractors.size()+disruptedInteractors.size() < 2) {
                logger.log(Level.WARNING,"Discarding degree <2");
                pb.next();
                continue;
            }
            
            //iterate over all disrupted edges to measure paths
            for (Protein interactor : disruptedInteractors) {
                textB.append(protein.getXRefValue(model.ENTREZ)).append('\t');
                textB.append(allele.getXRefValue(ccsbMut)).append('\t');
                textB.append(interactor.getXRefValue(model.ENTREZ)).append('\t');
                textB.append("-\t");
                textB.append(groupPheno.getXRefValue(doAuth)).append('\t');
                textB.append(groupPheno.getLabel(null)).append('\t');
                textB.append(pheno2geneSet.get(groupPheno).size()).append('\t');

//                logger.log(Level.FINEST, "Calculating shortest path...");

                //calculate shortest path from interactor to targets
                PathNode path = shortestPath.find(interactor, protsOfSamePheno, 
                        iaPattern, Collections.singleton(protein));
                if (path != null) {
                    //add path to result set
                    disruptedList.add(path.getDistance());

                    Protein target = Protein.fromIndividual(path.getValue());
                    textB.append(target.getXRefValue(model.ENTREZ)).append('\t');
                    textB.append(path.getDistance());

                } else {
                    //if no path is found add -1 to the list to symbolize Infinity.
                    disruptedList.add(-1);
                    textB.append("\tInf");
                }

                int degree = Interaction
                        .listInteractions(interactor, PhysicalInteraction.class).size();
                disruptedDegreeList.add(degree);
                textB.append("\t").append(degree).append("\n");

                //store for random permutation
                allDisruptedOrigins.add(new Pair<Protein>(protein,interactor));
            }
            
            //iterate over maintained edges to measure paths
            for (Protein interactor : maintainedInteractors) {
                textB.append(protein.getXRefValue(model.ENTREZ)).append('\t');
                textB.append(allele.getXRefValue(ccsbMut)).append('\t');
                textB.append(interactor.getXRefValue(model.ENTREZ)).append('\t');
                textB.append("+\t");
                textB.append(groupPheno.getXRefValue(doAuth)).append('\t');
                textB.append(groupPheno.getLabel(null)).append('\t');
                textB.append(pheno2geneSet.get(groupPheno).size()).append('\t');

                logger.log(Level.FINEST, "Calculating shortest path...");

                //calculate shortest path from interactor to targets
                PathNode path = shortestPath.find(interactor, protsOfSamePheno, 
                        iaPattern, Collections.singleton(protein));
                if (path != null) {
                    //add path to result set
                    maintainedList.add(path.getDistance());

                    Protein target = Protein.fromIndividual(path.getValue());
                    textB.append(target.getXRefValue(model.ENTREZ)).append('\t');
                    textB.append(path.getDistance());

                } else {
                    //if no path is found add -1 to the list to symbolize Infinity.
                    maintainedList.add(-1);
                    textB.append("\tInf");
                }

                int degree = Interaction.listInteractions(interactor, PhysicalInteraction.class).size();
                maintainedDegreeList.add(degree);
                textB.append("\t").append(degree).append("\n");

                //store for random permutation
                allMaintainedOrigins.add(new Pair<Protein>(protein,interactor));
            }
            
//            //only if we processed at least one edge, we're going to store the targets for later.
//            boolean used = false;
//            
//            //for all the interactions of the protein...
//            for (PhysicalInteraction interaction : Interaction
//                    .listInteractions(protein, PhysicalInteraction.class)) {
//                
//                IntArrayList currentList, currentDegreeList;
//                
//                /*
//                 * If the edge is disrupted, the distance measurement will go
//                 * in the list for disrupted edges, otherwise into the list for
//                 * maintained edges.
//                 */
//                if (allele.hasProperty(affectsNegatively, interaction)) {
//                    currentList = disruptedList;
//                    currentDegreeList = disruptedDegreeList;
//                } else if (allele.hasProperty(affectsPositively, interaction)) {
//                    currentList = maintainedList;
//                    currentDegreeList = maintainedDegreeList;
//                } else {
//                    //if disruption has not been tested for, discard this interaction
//                    //(because counting them as maintained would be unfair)
////                    pb.next();
//                    continue;
//                }
//                
//                //get the other proteins involved in the interaction.
//                List<Molecule> interactors = interaction.listParticipants();
//                interactors.remove(protein);
//                
//                //make sure it's a binary interaction
//                if (interactors.size()==1) {
//                    
//                    Protein interactor = Protein.fromIndividual(interactors.get(0));
//                    
//                    textB.append(protein.getXRefValue(model.ENTREZ)).append('\t');
//                    textB.append(allele.getXRefValue(ccsbMut)).append('\t');
//                    textB.append(interactor.getXRefValue(model.ENTREZ)).append('\t');
//                    if (allele.hasProperty(affectsNegatively, interaction)) {
//                        textB.append("-\t");
//                    } else {
//                        textB.append("+\t");
//                    }
//                    
//                    
//                    logger.log(Level.FINEST, "Calculating shortest path...");
//            
//                    //calculate shortest path from interactor to targets
//                    //TODO: Disallow allelic protein in paths
//                    PathNode path = shortestPath.find(interactor, protsOfSamePheno, iaPattern);
//                    if (path != null) {
//                        //add path to result set
//                        currentList.add(path.getDistance());
//                        
//                        Protein target = Protein.fromIndividual(path.getValue());
//                        textB.append(target.getXRefValue(model.ENTREZ)).append('\t');
//                        textB.append(path.getDistance());
//                        
//                    } else {
//                        //if no path is found add -1 to the list to symbolize Infinity.
//                        currentList.add(-1);
//                        
//                        textB.append("\tInf");
//                    }
//                    
//                    int degree = Interaction.listInteractions(interactor, PhysicalInteraction.class).size();
//                    currentDegreeList.add(degree);
//                    textB.append("\t").append(degree).append("\n");
//                    
//                    //mark for storage
//                    used = true;
//                    if (currentList.equals(disruptedList)) {
//                        allDisruptedOrigins.add(interactor);
//                    } else {
//                        allMaintainedOrigins.add(interactor);
//                    }
//                    
//                    
//                } else {
//                    logger.log(Level.WARNING, interactors.isEmpty() ? 
//                            "Ignoring self-interaction: "+protein.getXRefValue(model.ENTREZ): 
//                            "Ignoring multi-interaction: "+protein.getXRefValue(model.ENTREZ));
//                }
//            }
            
            //check if the target was used. If so store it for later.
            allTargets.add(protsOfSamePheno);
            
            //update progress bar
            pb.next();
        }
        
        
        //###COMPUTE PATHS TO RANDOM DISEASES###
        logger.log(Level.INFO, "Computing permuted controls...");
        pb = new CliProgressBar(allDisruptedOrigins.size()+allMaintainedOrigins.size());
        
        Random random = new Random();
        IntArrayList disruptedRandomList = new IntArrayList();
        for (Pair<Protein> pair : allDisruptedOrigins) {
            
            for (int i=0; i<5; i++) {
                Set<Protein> randomTarget = allTargets.get(random.nextInt(allTargets.size()));

                PathNode path = shortestPath.find(pair.getB(), randomTarget, iaPattern, Collections.singleton(pair.getA()));
                if (path != null) {
                    //add path to result set
                    disruptedRandomList.add(path.getDistance());
                } else {
                    //if no path is found add -1 to the list to symbolize Infinity.
                    disruptedRandomList.add(-1);

                }
            }
            pb.next();
        }
        IntArrayList maintainedRandomList = new IntArrayList();
        for (Pair<Protein> pair: allMaintainedOrigins) {
            
            for (int i=0; i<5; i++) {
                Set<Protein> randomTarget = allTargets.get(random.nextInt(allTargets.size()));

                PathNode path = shortestPath.find(pair.getB(), randomTarget, iaPattern, Collections.singleton(pair.getA()));
                if (path != null) {
                    //add path to result set
                    maintainedRandomList.add(path.getDistance());
                } else {
                    //if no path is found add -1 to the list to symbolize Infinity.
                    maintainedRandomList.add(-1);

                }
            }
            pb.next();
        }
        
        //write results to file
        logger.log(Level.INFO, "Writing results to file.");
        
        BufferedWriter w = null;
        try {
            
            w = new BufferedWriter(new FileWriter(disruptedOutFile));
            for (int d : disruptedList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(maintainedOutFile));
            for (int d : maintainedList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(disruptedRandomOutFile));
            for (int d : disruptedRandomList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(maintainedRandomOutFile));
            for (int d : maintainedRandomList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(disruptedDegreeOutFile));
            for (int d : disruptedDegreeList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(maintainedDegreeOutFile));
            for (int d : maintainedDegreeList) {
                w.write(d+"\n");
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(tableOutFile));
            w.write(textB.toString());
            
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
    
//    private Phenotype coAnnotated(Allele source, Set<Protein> out, int cutoff, Sparql sparql) {
//        
//        //get source diseases
//        Set<Individual> srcDiseases = new HashSet<Individual>();
//        NodeIterator it = source.listPropertyValues(causes);
//        while (it.hasNext()) {
//            srcDiseases.add(it.next().as(Individual.class));
//        }
//        it = source.listPropertyValues(isAssociatedWith);
//        while (it.hasNext()) {
//            srcDiseases.add(it.next().as(Individual.class));
//        }
//        
//        for (Individual srcDis : srcDiseases) {
//            Individual currRoot = srcDis;
//            Individual lastOut = null;
//            while (currRoot != null) {
//                //find genes
//                List<Individual> inds = sparql.queryIndividuals(getModel(), "getGenesOfDiseaseGroup", "gene", currRoot.getURI());
//                for (Individual ind : inds) {
//                    out.addAll(Protein.listEncodedProteins(Gene.fromIndividual(ind)));
//                    lastOut = currRoot;
//                }
//                //check if enough
//                if (out.size() > cutoff) {
//                    return Phenotype.fromIndividual(currRoot);
//                } else {
//                    //go to next higher node
//                    Resource res = currRoot.getPropertyResourceValue(isa);
//                    currRoot = res == null ? null : res.as(Individual.class);
//                }
//            }
//            if (out.size() > 1) {
//                return Phenotype.fromIndividual(lastOut);
//            }
//        }
//        
//        return null;
//    }
    

    @Override
    public boolean requiresReasoner() {
        return false;
    }

//    private Map<Gene,Phenotype> gene2pheno;
    private LazyInitMap<Phenotype,Set<Gene>> pheno2geneSet;
    
    private void indexDiseaseAnnotations(Sparql sparql, int min, int max) {
        
        //map all phenotypes to their set of genes
        pheno2geneSet = new LazyInitMap<Phenotype,Set<Gene>>(HashSet.class);
        //map all genes to their set of phenotypes
//        LazyInitMap<Gene,Set<Phenotype>> gene2phenoSet = new LazyInitMap<Gene,Set<Phenotype>>(HashSet.class);
        
        
        //find all transitive gene->phenotype associations
        QueryExecution qexec = QueryExecutionFactory
                .create(sparql.get("disease2genes"), getModel());
        
        try {
            ResultSet r = qexec.execSelect();
            while (r.hasNext()) {
                QuerySolution sol = r.next();
                
                Phenotype pheno = Phenotype.fromIndividual(sol.getResource("disease").as(Individual.class));
                Gene gene = Gene.fromIndividual(sol.getResource("gene").as(Individual.class));
                
                pheno2geneSet.getOrCreate(pheno).add(gene);
//                gene2phenoSet.getOrCreate(gene).add(pheno);
                
            }
        } finally {
            qexec.close();
        }
        
//        //find the best phenotype for each gene
//        gene2pheno = new HashMap<Gene,Phenotype>();
//        
//        //must be between min and max cutoff
//        //choose the most specific one within those bounds.
//        for (Entry<Gene,Set<Phenotype>> entry : gene2phenoSet.entrySet()) {
//            int bestNum = Integer.MAX_VALUE;
//            Phenotype bestPheno = null;
//            for (Phenotype pheno : entry.getValue()) {
//                int num = pheno2geneSet.get(pheno).size();
//                if ((num >= min) && (num <= max) && (num < bestNum)) {
//                    bestNum = num;
//                    bestPheno = pheno;
//                }
//            }
//            if (bestPheno != null) {
//                gene2pheno.put(entry.getKey(),bestPheno);
//            }
//        }
        
        
        
    }

    private Phenotype findBestPhenotype(Allele allele, Sparql sparql, int min, int max) {
        
        int bestNum = Integer.MAX_VALUE;
        Phenotype bestPheno = null;
        
        int bestFallBackNum = -1;
        Phenotype bestFallBack = null;
                
        
        //for each phenotype associated with the allele
        for (Individual ind : sparql.queryIndividuals(getModel(), "getPhenoOfAllele", "disease", allele.getURI())) {
            Phenotype pheno = Phenotype.fromIndividual(ind);
            
            Set<Gene> genes = pheno2geneSet.get(pheno);
            
            //work-around for sparql bug
            if (genes == null) {
                for (Individual geneInd : sparql.queryIndividuals(getModel(), "getGenesForPhenotype", "gene", pheno.getURI())) {
                    pheno2geneSet.getOrCreate(pheno).add(Gene.fromIndividual(geneInd));
                }
            }
            genes = pheno2geneSet.get(pheno);
            if (genes == null) {
                logger.log(Level.SEVERE, "No genes for phenotype of allele: "+allele.getURI()+" -> "+pheno.getURI()+" : "+pheno.getLabel(null));
                continue;
            }
            
            int num = genes.size();
            if ((num >= min) && (num <= max) && (num < bestNum)) {
                bestNum = num;
                bestPheno = pheno;
            }
            
            if ((num > bestFallBackNum)) {
                bestFallBack = pheno;
                bestFallBackNum = num;
            }
            
            //for each parent of that phenotype
            for (Individual parInd : sparql.queryIndividuals(getModel(), "getParents", "parent", pheno.getURI())) {
                Phenotype parent = Phenotype.fromIndividual(parInd);
                
                num = pheno2geneSet.get(parent).size();
                if ((num >= min) && (num <= max) && (num < bestNum)) {
                    bestNum = num;
                    bestPheno = pheno;
                }
                
            }
            
        }
        
        if (bestPheno == null) {
            bestPheno = bestFallBack;
        }
        
        return bestPheno;
        
    }
    
    
}
