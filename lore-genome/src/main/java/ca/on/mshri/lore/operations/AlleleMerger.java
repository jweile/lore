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
import com.hp.hpl.jena.ontology.Individual;
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
public class AlleleMerger {
    
    /**
     * Merges the alleles and associated Mutations belonging to the given set of genes.
     * 
     * WARNING: This assumes that the list of genes is non-redundant, i.e. has 
     * already been consolidated and merged as appropriate based on XRefs.
     * @param selection 
     */
    public void merge(Collection<Gene> selection, GenomeModel model) {
                
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
        
        new Merger().merge(mutationSetList);
        
        
        /* ### STEP 2 ###
         * Merging the actual alleles. 
         */
        
        new ContextBasedMerger().merge(alleles, 
                model.getOntClass(Gene.CLASS_URI), 
                model.getOntClass(Mutation.CLASS_URI)
        );
        
    }
}
