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
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import de.jweile.yogiutil.LazyInitMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AlleleMerger extends LoreOperation {
    
    /**
     * List of genes.
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
     * WARNING: This assumes that the list of genes is non-redundant, i.e. has 
     * already been consolidated and merged as appropriate based on XRefs.
     */
    @Override
    public void run() {
                
        GenomeModel model = new GenomeModel(OntModelSpec.OWL_MEM, getModel());
        
        Collection<Gene> selection = getParameterValue(selectionP).resolve(getModel());
        
        /* ### STEP 1 ###
         * alleles are the same if they have the same gene and the same mutations,
         * so we need to merge those mutations first
         */
        Collection<Set<Individual>> mutationSetList = new ArrayList<Set<Individual>>();
        
        //we also collect a list of all alleles, so we can merge them later
        List<Allele> alleles = new ArrayList<Allele>();
        
        for (Gene gene : selection) {
            
            LazyInitMap<String,Set<Individual>> mutIndex = new LazyInitMap<String, Set<Individual>>(HashSet.class);
            
            for (Allele allele : gene.listAlleles()) {
                
                alleles.add(allele);
                
                for (Mutation mut : allele.listMutations()) {
                    
                    if (mut.getOntClass(true).getURI().equals(PointMutation.CLASS_URI)) {
                        
                        PointMutation pmut = PointMutation.fromIndividual(mut);
                        mutIndex.getOrCreate(pmut.getFromAminoAcid()+pmut.getPosition()+pmut.getToAminoAcid()).add(pmut);
                        
                    } else {
                        //ignore for now
                    }
                }
            }
            
            for (Set<Individual> mutationSet : mutIndex.values()) {
                if (mutationSet.size() > 1) {
                    mutationSetList.add(mutationSet);
                }
            }
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
}
