/*
 * Copyright (C) 2012 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.GenomeModel;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.LazyInitMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes a list of non-redundant genes and consolidates the associated alleles 
 * and mutations. If a gene has two alleles with equivalent mutations, the alleles
 * are merged (as well as the mutations).
 * 
 * 
 * WARNING: This assumes that the list of genes is non-redundant, i.e. has 
 * already been consolidated and merged as appropriate based on XRefs.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AlleleMerger extends LoreOperation {
    
    /**
     * List of non-redundant genes which possess the alleles to be merged.
     * 
     * WARNING: This assumes that the list of genes is non-redundant, i.e. has 
     * already been consolidated and merged as appropriate based on XRefs.
     */
    public final RefListParameter<Gene> selectionP = new RefListParameter("selection", Gene.class);
    
//    /**
//     * Genome model on containing the genes.
//     */
//    public final Parameter<GenomeModel> modelP = Parameter.make("model", GenomeModel.class);
    
    /**
     * Merges the alleles and associated Mutations belonging to the given set of genes.
     * 
     */
    @Override
    public void run() {
                
        Logger.getLogger(AlleleMerger.class.getName())
                .log(Level.INFO, "AlleleMerger: Indexing...");
        
        GenomeModel model = new GenomeModel(OntModelSpec.OWL_MEM, getModel());
        
        Collection<Gene> selection = getParameterValue(selectionP).resolve(getModel());
        
        /* ### STEP 1 ###
         * alleles are the same if they have the same gene and the same mutations,
         * so we need to merge those mutations first
         */
        Collection<Set<Individual>> mutationSetList = new ArrayList<Set<Individual>>();
        
        //we also collect a list of all alleles, so we can merge them later
        List<Allele> alleles = new ArrayList<Allele>();
        
        CliProgressBar pro = new CliProgressBar(selection.size());
        
        //for each gene
        for (Gene gene : selection) {
            
            //create an index for the gene's mutations
            LazyInitMap<String,Set<Individual>> mutIndex = new LazyInitMap<String, Set<Individual>>(HashSet.class);
            
            //for each allele of the gene
            for (Allele allele : gene.listAlleles()) {
                
                //init counter for number of mutatations
                int mutCount = 0;
                
                //for each mutant of that allele (usually there's only one)
                for (Mutation mut : allele.listMutations()) {
                    
                    //check if it's a point mutation, if so then...
                    if (mut.getOntClass(true).getURI().equals(PointMutation.CLASS_URI)) {
                        
                        //index this mutation for the current gene based on its aminoacid change signature
                        PointMutation pmut = PointMutation.fromIndividual(mut);
                        if (pmut.getFromAminoAcid() == null || pmut.getToAminoAcid() == null || pmut.getPosition() == 0) {
                            Logger.getLogger(AlleleMerger.class.getName())
                                    .log(Level.WARNING, pmut.getURI()+" has broken change signature!");
                            continue;
                        }
                        mutCount++;
                        String signature = pmut.getFromAminoAcid()+pmut.getPosition()+pmut.getToAminoAcid();
                        mutIndex.getOrCreate(signature).add(pmut);
                        
                    } else {
                        //ignore cases of mutations other than point mutations
                    }
                }
                
                //save allele in the list so they can be passed to the context-based merger later
                if (mutCount > 0) {
                    alleles.add(allele);
                }
            }
            
//            print(mutIndex);
            
            for (Set<Individual> mutationSet : mutIndex.values()) {
                if (mutationSet.size() > 1) {
                    mutationSetList.add(mutationSet);
                }
            }
            pro.next();
        }
        
        Merger merger = new Merger();
        merger.setParameter(merger.mergeSetsP, mutationSetList);
        merger.run();
        
        
        /* ### STEP 2 ###
         * Merging the actual alleles. 
         */
        
        ContextBasedMerger cbm = new ContextBasedMerger();
        cbm.setParameter(cbm.selectionP, new ResourceReferences<Allele>(alleles, Allele.class));
        cbm.setParameter(cbm.contextRestrictionsP, cbm.contextRestrictionsP.validate(
                Gene.CLASS_URI+","+Mutation.CLASS_URI)
        );
        cbm.setModel(model);
        cbm.run();
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

//    private void print(LazyInitMap<String, Set<Individual>> mutIndex) {
//        for (String key : mutIndex.keySet()) {
//            System.out.println(key);
//            for (Individual i : mutIndex.get(key)) {
//                System.out.println(" -> "+i);
//            }
//        }
//    }
}
