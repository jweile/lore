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
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntModelSpec;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class MutantDetailParser extends TabDelimParser {
    
    public URLParameter srcP = new URLParameter("src");
    
    //columns
    private static final int mutId = 0;
    private static final int symbol = 1;
    private static final int entrez = 3;
    private static final int hgmd = 4;
    private static final int pos = 5;
    private static final int fromCodon = 7;
    private static final int toCodon = 8;
    
    //fields
    private InteractionModel iaModel;
    private Authority ccsbMut;
    
    /**
     * Run parser
     */
    public void run() {
        
        Logger.getLogger(MutantDetailParser.class.getName())
                .log(Level.INFO, "Mutant Detail Parser started.");
        
        //get source
        URL url = getParameterValue(srcP);
        if (url == null) {
            throw new IllegalArgumentException("Parameter src is required!");
        }
        
        //init fields
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        ccsbMut = Authority.createOrGet(iaModel, "CCSB-Mutant");
        
        /*
         * start parsing, 
         * skip 1st row, 
         * ensure each row has 9 columns
         */
        try {
            parseTabDelim(url.openStream(), 1, 9);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to open "+url, ex);
        }
    }


    /**
     * Called by parseTabDelim() for each row in the table.
     * 
     * @param cols an array containing the values of each column for this row.
     */
    @Override
    protected void processRow(String[] cols) {
        
        String mutDesc = cols[fromCodon]+cols[pos]+cols[toCodon];

        Gene gene = Gene.createOrGet(iaModel, iaModel.ENTREZ, cols[entrez]);
        gene.addLabel(cols[symbol], null);

        Allele allele = Allele.createOrGet(iaModel, ccsbMut, cols[mutId]);
        PointMutation mut = PointMutation.createOrGet(iaModel, allele, mutDesc);
        allele.addMutation(mut);

        Gene linkedGene = allele.getGene();
        if (linkedGene == null || !linkedGene.equals(gene)) {
            Logger.getLogger(MutantDetailParser.class.getName())
                    .log(Level.WARNING, "Allele not linked: "+allele.getURI());
            allele.setGene(gene);
        }  
        
    }
    
    
    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
