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
package ca.on.mshri.lore.interaction.edgotype;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliProgressBar;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class SeqDistAnalysis extends LoreOperation {

    /**
     * Parameter "outfile": Indicates the file to which the output is written.
     */
    public final Parameter<String> outfileP = Parameter.make("outfile",String.class, "seqDist.tsv");
    
    /**
     * The "PointMutation" OWL class
     */
    private OntClass pMutClass;

    /**
     * The OWL properties "affectsPositively" and "affectsNegatively",
     * which connect alleles to their affected interactions.
     */
    private Property affectsPositively, affectsNegatively;
    
    /**
     * Runs the analysis on the current dataset.
     */
    @Override
    public void run() {
        
        Logger.getLogger(SeqDistAnalysis.class.getName())
                .log(Level.INFO, "Sequence distance analysis started.");
                
        StringBuilder out = new StringBuilder();
        
        pMutClass = getModel().getOntClass(PointMutation.CLASS_URI);
        affectsPositively = getModel().getProperty(InteractionModel.URI+"#affectsPositively");
        affectsNegatively = getModel().getProperty(InteractionModel.URI+"#affectsNegatively");
        
        Logger.getLogger(SeqDistAnalysis.class.getName())
                .log(Level.INFO, "Analyzing edgotype profiles...");
                
        //get list of all genes
        List<Gene> genes = getModel().listIndividualsOfClass(Gene.class, false);
        
        CliProgressBar pb = new CliProgressBar(genes.size());
        
        for (Gene gene : genes) {
            
            //get alleles of current gene
            List<Allele> alleles = gene.listAlleles();
            
            //form all possible pairs of alleles for the current gene
            for (int i = 1; i < alleles.size(); i++) {
                
                //pair partner #1
                Allele a_i = alleles.get(i);
                //get the position of the mutation within this allele
                Integer pos_i = mutationPosition(a_i);
                //compute the edgotype profile for the current allele
                Set<String> profile_i = edgeticProfile(a_i);
                
                //skip alleles for which neither information was accessible
                if (pos_i == null || profile_i.isEmpty()) {
                    continue;
                }
                
                //iterate over partner #2
                for (int j = 0; j < i; j++) {
                    
                    //pair partner #2
                    Allele a_j = alleles.get(j);
                    //get the position of the mutation within this allele
                    Integer pos_j = mutationPosition(a_j);
                    //compute the edgotype profile for the current allele
                    Set<String> profile_j = edgeticProfile(a_j);
                    
                    //skip alleles for which neither information was accessible
                    if (pos_j == null || profile_j.isEmpty()) {
                        continue;
                    }
                    
                    //compute sequence distance between the two mutations
                    int distance = Math.abs(pos_i - pos_j);
                    
                    //get similarity between the two edgotype profiles
                    double similarity = jaccard(profile_i, profile_j);
                    
                    //output of results
                    out.append(distance)
                        .append('\t')
                        .append(similarity)
                        .append("\n");
                    
                }
            }
            
            pb.next();
        }
        
        //write output to file
        write(out.toString(), getParameterValue(outfileP));
        
    }
        
    /**
     * Retrives the position of the mutation within the given allele.
     */
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

    /**
     * Retrives the edgotype profile of the given allele.
     * This is represented as a set of strings which indicate positive or negative effects
     * on the different interactions in which the encoded gene enages.
     */
    private Set<String> edgeticProfile(Allele a_i) {
        
        Set<String> list = new HashSet<String>();
        
        NodeIterator it = a_i.listPropertyValues(affectsPositively);
        while (it.hasNext()) {
            list.add("pos:" + it.next().asResource().getURI());
        }
        
        it = a_i.listPropertyValues(affectsNegatively);
        while (it.hasNext()) {
            list.add("neg:" + it.next().asResource().getURI());
        }
        
        return list;
    }
    
    /**
     * This operation does not require reasoner support.
     */
    @Override
    public boolean requiresReasoner() {
        return false;
    }


    /**
     * Writes the given contents to a new file with the given file name.
     */
    private void write(String contents, String file) {
        
        Logger.getLogger(SeqDistAnalysis.class.getName())
            .log(Level.INFO, "Writing results to file.");
        
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


    /**
     * Computes the Jaccard coefficent for overlap between two sets.
     */
    private <T> double jaccard(Set<T> a, Set<T> b) {
        Set<T> union = new HashSet<T>(a);
        union.addAll(b);
        
        Set<T> intersection = new HashSet<T>(a);
        intersection.retainAll(b);
        
        return (double)intersection.size() / (double)union.size();
    }

    
}
