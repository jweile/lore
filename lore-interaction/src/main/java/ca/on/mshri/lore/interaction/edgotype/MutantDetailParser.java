/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.mshri.lore.interaction.edgotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntModelSpec;
import de.jweile.yogiutil.CliIndeterminateProgress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class MutantDetailParser extends LoreOperation {
    
    public URLParameter srcP = new URLParameter("src");
    
//    public Parameter<InteractionModel> modelP = Parameter.make("model", InteractionModel.class);
    
    public void run() {
        
        Logger.getLogger(MutantDetailParser.class.getName())
                .log(Level.INFO, "Mutant Detail Parser started.");
        
        InputStream in = null;
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        
        int mutId = 0;
        int symbol = 1;
        int entrez = 3;
        int hgmd = 4;
        int pos = 5;
        int fromCodon = 7;
        int toCodon = 8;
        
        Authority ccsbMut = Authority.createOrGet(model, "CCSB-Mutant");
        
        try {
            
            in = getParameterValue(srcP).openStream();
            BufferedReader b = new BufferedReader(new InputStreamReader(in));
            
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = b.readLine()) != null) {
                lnum++;
                
                if (lnum == 1) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                if (cols.length < toCodon+1) {
                    continue;
                }
                
                String mutDesc = cols[fromCodon]+cols[pos]+cols[toCodon];
                
                Gene gene = Gene.createOrGet(model, model.ENTREZ, cols[entrez]);
                gene.addLabel(cols[symbol], null);
                
                Allele allele = Allele.createOrGet(model, ccsbMut, cols[mutId]);
                PointMutation mut = PointMutation.createOrGet(model, allele, mutDesc);
                allele.addMutation(mut);
                
                Gene linkedGene = allele.getGene();
                if (linkedGene == null || !linkedGene.equals(gene)) {
                    Logger.getLogger(MutantDetailParser.class.getName())
                            .log(Level.WARNING, "Allele not linked: "+allele.getURI());
                    allele.setGene(gene);
                }                
                
                progress.next("Parsing");
                
            }
            progress.done();
            
        } catch (IOException ex) {
            throw new RuntimeException("Parsing failed!",ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MutantDetailParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
