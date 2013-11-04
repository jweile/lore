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
import de.jweile.yogiutil.IntArrayList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public final Parameter<String> maintainedOutFileP = 
            Parameter.make("maintainedOutFile", String.class, "dist_maintained.txt");

    private Property isAssociatedWith, affectsNegatively, affectsPositively;
    
    @Override
    public void run() {
        
        String disruptedOutFile = getParameterValue(disruptedOutFileP);
        String maintainedOutFile = getParameterValue(maintainedOutFileP);
        
        //initialize sparql engine for pre-defined queries in this module.
        Sparql sparql = Sparql.getInstance(DiseasePathLength.class
                .getProtectionDomain().getCodeSource()
        );
        
        //shortest path algorithm object
        ShortestPath shortestPath = new ShortestPath();
        
        //define SPARQL pattern for interaction proteins.
        String hpString = "<"+InteractionModel.URI+"#hasParticipant>";
        String iaPattern = "^"+hpString+"/"+hpString;
        
        
        //define some needed properties
        isAssociatedWith = getModel()
                .getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        affectsNegatively = getModel()
                .getProperty(InteractionModel.URI+"#affectsNegatively");
        affectsPositively = getModel()
                .getProperty(InteractionModel.URI+"#affectsPositively");
        
        IntArrayList disruptedList = new IntArrayList(),
                maintainedList = new IntArrayList();
        
        //create shortcut to interaction model interface
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        
        //get all alleles in the model
        List<Allele> alleles = model.listIndividualsOfClass(Allele.class, true);
        //set up progressbar calibrated to allele set
        CliProgressBar pb = new CliProgressBar(alleles.size());
       
        //iterate over all alleles in the model
        for (Allele allele : alleles) {
            
            //get the protein that is encoded by the gene for which this allele exists
            Protein protein = Protein.listEncodedProteins(allele.getGene()).get(0);
            
            //get all the phenotypes of the allele
            List<Phenotype> allelePhenotypes = phenotypeOf(allele);
            
            //prepare a set that will contain all proteins with the same phenotype annotation
            Set<Protein> protsOfSamePheno = new HashSet<Protein>();
            
            //for each phenotype...
            for (Phenotype pheno : allelePhenotypes) {
                //get all the genes that are associated with the same phenotype
                List<Individual> geneInds = sparql
                        .queryIndividuals(model, 
                        "getGenesForPhenotype", "gene", pheno.getURI()
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
             * will be of length
             */
            protsOfSamePheno.remove(protein);
            
            
            //for all the interactions of the protein...
            for (PhysicalInteraction interaction : Interaction
                    .listInteractions(protein, PhysicalInteraction.class)) {
                
                IntArrayList currentList;
                
                /*
                 * If the edge is disrupted, the distance measurement will go
                 * in the list for disrupted edges, otherwise into the list for
                 * maintained edges.
                 */
                if (allele.hasProperty(affectsNegatively, interaction)) {
                    currentList = disruptedList;
                } else if (allele.hasProperty(affectsPositively, interaction)) {
                    currentList = maintainedList;
                } else {
                    //if disruption has not been tested for, discard this interaction
                    //(because counting them as maintained would be unfair)
                    pb.next();
                    continue;
                }
                
                //get the other proteins involved in the interaction.
                List<Molecule> interactors = interaction.listParticipants();
                interactors.remove(protein);
                
                //make sure it's a binary interaction
                if (interactors.size()==1) {
                    
                    Protein interactor = Protein.fromIndividual(interactors.get(0));
                    
                    //calculate shortest path from interactor to 
                    PathNode path = shortestPath.find(interactor, protsOfSamePheno, iaPattern);
                    if (path != null) {
                        //add path to result set
                        currentList.add(path.getDistance());
                    }
                    
                } else {
//                    Logger.getLogger(DiseasePathLength.class.getName())
//                            .log(Level.INFO, interactors.isEmpty() ? 
//                            "Ignoring self-interaction": 
//                            "Ignoring multi-interaction");
                }
            }
            
            //update progress bar
            pb.next();
        }
        
        //write results to file
        BufferedWriter w = null;
        try {
            
            w = new BufferedWriter(new FileWriter(disruptedOutFile));
            
            for (int d : disruptedList) {
                w.write(d);
                w.write('\n');
            }
            
            w.close();
            
            w = new BufferedWriter(new FileWriter(maintainedOutFile));
            
            for (int d : maintainedList) {
                w.write(d);
                w.write('\n');
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to file",e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    Logger.getLogger(DiseasePathLength.class.getName())
                            .log(Level.WARNING, "Unable to close stream!", ex);
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
        
        return list;
    }
    

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
