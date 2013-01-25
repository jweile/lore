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
package ca.on.mshri.lore.phenotype.operations;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliProgressBar;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class SeqDistAnalysis extends LoreOperation {

    public final Parameter<String> outfileP = Parameter.make("outfile",String.class, "seqDist.tsv");
    
    private OntClass pMutClass;
    private Property isAssociatedWith;
    
    @Override
    public void run() {
        
        Logger.getLogger(SeqDistAnalysis.class.getName()).log(Level.INFO, "Sequence distance analysis started.");
        
        StringBuilder out = new StringBuilder();
        
//        PhenotypeModel phenoModel = new PhenotypeModel(OntModelSpec.OWL_MEM, getModel());
        
        pMutClass = getModel().getOntClass(PointMutation.CLASS_URI);
        isAssociatedWith = getModel().getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        
        List<Gene> genes = getModel().listIndividualsOfClass(Gene.class, false);
        
        CliProgressBar pb = new CliProgressBar(genes.size());
        
        for (Gene gene : genes) {
            
            List<Allele> alleles = gene.listAlleles();
            
            //test all pairs of alleles
            for (int i = 1; i < alleles.size(); i++) {
                
                Allele a_i = alleles.get(i);
                Integer pos_i = mutationPosition(a_i);
                List<Phenotype> diseases_i = diseasesOf(a_i);
                
                if (pos_i == null || diseases_i.isEmpty()) {
                    continue;
                }
                
                for (int j = 0; j < i; j++) {
                    
                    Allele a_j = alleles.get(j);
                    Integer pos_j = mutationPosition(a_j);
                    List<Phenotype> diseases_j = diseasesOf(a_j);
                    
                    if (pos_j == null || diseases_j.isEmpty()) {
                        continue;
                    }
                    
                    //compute distance
                    int distance = Math.abs(pos_i - pos_j);
                    
                    //test whether they have diseases in common.
                    boolean commonDisease = haveCommonMember(diseases_i, diseases_j);
                    
                    out.append(distance).append('\t').append(commonDisease ? 1 : 0);
                    
                }
            }
            
            pb.next();
        }
        
        write(out.toString(), getParameterValue(outfileP));
        
    }
        
    
    private Integer mutationPosition(Allele allele) {
        List<Mutation> muts = allele.listMutations();
        if (muts != null && !muts.isEmpty()) {
            Mutation mut = muts.get(0);
            if (LoreModel.hasClass(mut, pMutClass)) {
                PointMutation pMut = PointMutation.fromIndividual(mut);
                return pMut.getPosition();
            }
        }
        return null;
    }

    private List<Phenotype> diseasesOf(Allele allele) {
        
        List<Phenotype> list = new ArrayList<Phenotype>();
        
        NodeIterator it = allele.listPropertyValues(isAssociatedWith);
        while (it.hasNext()) {
            list.add(Phenotype.fromIndividual(it.next().as(Individual.class)));
        }
        
        return list;
    }
    
    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private boolean haveCommonMember(List<Phenotype> diseases_i, List<Phenotype> diseases_j) {
        for (Phenotype pheno : diseases_i) {
            if (diseases_j.contains(pheno)) {
                return true;
            }
        }
        return false;
    }

    private void write(String contents, String file) {
        
        Logger.getLogger(SeqDistAnalysis.class.getName()).log(Level.INFO, "Writing results to file.");
        
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(file));
            w.write(contents);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to write to file "+file, ex);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    Logger.getLogger(SeqDistAnalysis.class.getName())
                            .log(Level.SEVERE, "Unable to close output stream", ex);
                }
            }
        }
    }

    
}
