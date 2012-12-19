/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.mshri.lore.interaction.edgotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.GenomeModel;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.operations.LoreOperation;
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
    
    public Parameter<InputStream> inP = Parameter.make("in", InputStream.class);
    
    public Parameter<InteractionModel> modelP = Parameter.make("model", InteractionModel.class);
    
    public void run() {
        
        InputStream in = getParameterValue(inP);
        InteractionModel model = getParameterValue(modelP);
        
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        
        int mutId = 0;
        int symbol = 1;
        int entrez = 3;
        int hgmd = 4;
        int pos = 5;
        int fromCodon = 7;
        int toCodon = 8;
        
        Authority ccsbMut = Authority.createOrGet(model, "CCSB-Mutant");
        
        try {
            
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
                            .log(Level.WARNING, "Allele not linked:"+allele.getURI());
                    allele.setGene(gene);
                }                        
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Parsing failed!",ex);
        } finally {
            try {
                b.close();
            } catch (IOException ex) {
                Logger.getLogger(MutantDetailParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
    }
    
}
