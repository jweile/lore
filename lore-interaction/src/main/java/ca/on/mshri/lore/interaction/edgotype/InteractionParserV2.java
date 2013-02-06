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
package ca.on.mshri.lore.interaction.edgotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Parses version 2 of the edgotyping initiative file format
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class InteractionParserV2 extends TabDelimParser {
        
    /**
     * source URL from where to read the data.
     */
    public URLParameter srcP = new URLParameter("src");
    
    /**
     * ID of the experiment that created the data.
     */
    public Parameter<String> expP = Parameter.make("exp", String.class);
    
    //column indices
    private static final int pwtest = 0;
    private static final int dbOrfId = 1;
    private static final int dbSymbol = 2;
    private static final int dbEntrezId = 3;
    private static final int mutId = 4;
    private static final int mutAAPos = 5;
    private static final int hgmdAcc = 6;
    private static final int degree = 7;
    private static final int adOrfId = 8;
    private static final int adSymbol = 9;
    private static final int adEntrezId  = 10;
    private static final int orfPair = 11;
    private static final int entrezPair = 12;
    private static final int score = 13;
    private static final int ltGrowth = 14;
    private static final int edgotype = 15;
    private static final int hgmdDisease = 16;

    
    //fields
    private InteractionModel iaModel;
    private Experiment exp;
    private Authority ccsbMut;
    private Authority ccsbOrf;
    private Authority hgmd;
    private OntClass physIntType;
    private Property pos;
    private Property neg;
    
    /**
     * Remember which genes were already handled.
     */
    private Map<String,String> gene2allele = new HashMap<String, String>();
        
    /**
     * Runs the parser.
     */
    public void run() {
        
        Logger.getLogger(InteractionParserV2.class.getName())
                .log(Level.INFO, "Interaction parser started");
        
        //init fields
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        exp = Experiment.createOrGet(iaModel, getParameterValue(expP));
        ccsbMut = Authority.createOrGet(iaModel, "CCSB-Mutant");
        ccsbOrf = Authority.createOrGet(iaModel, "CCSB-ORF");
        hgmd = Authority.createOrGet(iaModel, "HGMD");
        physIntType = iaModel.getOntClass(PhysicalInteraction.CLASS_URI);
        pos = iaModel.getProperty(InteractionModel.URI+"#affectsPositively");
        neg = iaModel.getProperty(InteractionModel.URI+"#affectsNegatively");
        
        //get input source
        URL url = getParameterValue(srcP);
        if (url == null) {
            throw new IllegalArgumentException("Parameter src is required!");
        }
        
        //start parsing
        try {
            parseTabDelim(url.openStream(), 2, 7);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to open "+url, ex);
        }
        
        
    }

    /**
     * Processes a row from the tab delimted file.
     * @param cols 
     */
    @Override
    protected void processRow(String[] cols) {
        
        //if DB gene or AD gene already have a different ORF, then skip this row
        if (isDuplicate(cols, dbEntrezId, dbOrfId) 
                || isDuplicate(cols, adEntrezId, adOrfId)) {
            return;
        }
        
        //Make DB gene and DB protein. Protein will be identified with the ORF id
        Gene dbGene = getOrMakeGene(cols, dbEntrezId, dbSymbol);
        Protein dbProtein = getOrMakeProtein(cols, dbGene, dbEntrezId, dbSymbol, dbOrfId);

        //Make AD gene and AD protein. as above.
        Gene adGene = getOrMakeGene(cols, adEntrezId, adSymbol);
        Protein adProtein = getOrMakeProtein(cols, adGene, adEntrezId, adSymbol, adOrfId);

        //Create the interaction between the proteins
        PhysicalInteraction interaction = PhysicalInteraction
                .createOrGet(iaModel, exp, physIntType, dbProtein, adProtein);

        //Create the allele. Major alleles have URI=OrfID.0
        //Minor alleles have URI=MutantId
        Allele dbAllele = Allele.createOrGet(iaModel, ccsbMut, 
                cols[mutId].equals("0") ? 
                cols[dbOrfId]+".0" : 
                cols[mutId]
        );
        //Connect allele to gene if not already done.
        if (dbAllele.getGene() == null || !dbAllele.getGene().equals(dbGene)) {
            dbAllele.setGene(dbGene);
        }
        //Add HGMD ID to allele if not already done.
        if (dbAllele.getXRefValue(hgmd) == null) {
            dbAllele.addXRef(hgmd, cols[hgmdAcc]);
        }
        //Add Mutation to Allele if not already done or Major allele.
        if (dbAllele.listMutations().isEmpty() && !cols[mutAAPos].equals("WT")) {
            PointMutation pmut = PointMutation
                    .createOrGet(iaModel, dbAllele, cols[mutAAPos]);
            dbAllele.addMutation(pmut);
        }

        //Connect allele to interaction regarding positive or negative influece
        Growth growth = Growth.fromKey(cols[score]);
        if (growth != Growth.UNKNOWN) {
            Property affects = growth != Growth.NEG ? pos : neg;
            dbAllele.addProperty(affects, interaction);
        }
    }

    /**
     * Get the gene object for the given IDs or make a new one if it doesn't 
     * exist yet.
     * @param cols the column array
     * @param entrezIndex the array index for the entrez id
     * @param symbolIndex the array index for the gene symbol
     * @return the gene object.
     */
    private Gene getOrMakeGene(String[] cols, int entrezIndex, int symbolIndex) {
        //get model components
        Gene gene = Gene.createOrGet(iaModel, iaModel.ENTREZ, cols[entrezIndex]);
        if (gene.getXRefValue(iaModel.HGNC) == null) {
            gene.addXRef(iaModel.HGNC, cols[symbolIndex]);
        }
        return gene;
    }

    /**
     * Get the protein object for the given ID or make a new one if it doesn't
     * exist yet. 
     * @param cols The column array.
     * @param gene Gene that encodes for this protein
     * @param entrezIndex Array index for the entrez id
     * @param symbolIndex Array index for gene symbol
     * @param orfIndex Array index for ORF ID
     * @return the protein object.
     */
    private Protein getOrMakeProtein(String[] cols, Gene gene, int entrezIndex, 
            int symbolIndex, int orfIndex) {
        
        Protein protein = Protein.createOrGet(iaModel, ccsbOrf, cols[orfIndex]);
        if (protein.getXRefValue(iaModel.HGNC) == null) {
            protein.addXRef(iaModel.HGNC, cols[symbolIndex]);
        }
        if (protein.getXRefValue(iaModel.ENTREZ) == null) {
            protein.addXRef(iaModel.ENTREZ, cols[entrezIndex]);
        }
        if (protein.getEncodingGene() == null || !protein.getEncodingGene().equals(gene)) {
            protein.setEncodingGene(gene);
        }
        return protein;
    }

    /**
     * Returns whether another ORF already exists for this gene.
     * @param cols column array
     * @param entrezIndex array index for entrez id
     * @param orfIndex array index for orf id
     * @return 
     */
    private boolean isDuplicate(String[] cols, int entrezIndex, int orfIndex) {
        
        String entrezId = cols[entrezIndex];
        String orfId = cols[orfIndex];
        
        String knownOrf = gene2allele.get(entrezId);
        if (knownOrf == null || knownOrf.equals(orfId)) {
            gene2allele.put(entrezId, orfId);
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Enum for representing different experimental results
     */
    private static enum Growth {
        //y, 0, y-,  y+,    aa
        POS,NEG,WEAK,STRONG,UNKNOWN;
        
        public static Growth fromKey(String key) {
            if (key.equals("y")) {
                return POS;
            } else if (key.equals("y-")) {
                return WEAK;
            } else if (key.equals("y+")) {
                return STRONG;
            } else if (key.equals("aa")) {
                return UNKNOWN;
            } else {
                return NEG;
            }
        }
    }

    /**
     * does not require reasoner support.
     * @return false
     */
    @Override
    public boolean requiresReasoner() {
        return false;
    }
}
