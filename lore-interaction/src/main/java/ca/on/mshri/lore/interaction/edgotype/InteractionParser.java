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
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class InteractionParser extends TabDelimParser {
        
    public URLParameter srcP = new URLParameter("src");
    
    public Parameter<String> expP = Parameter.make("exp", String.class);
    
    //column indices
    private static final int mut = 1;
    private static final int dbId = 4;
    private static final int adId = 6;
    private static final int gro = 12;
    
    //fields
    private InteractionModel iaModel;
    private Experiment exp;
    private Authority ccsbMut;
    private OntClass physIntType;
    private Property pos;
    private Property neg;
        
    public void run() {
        
        Logger.getLogger(InteractionParser.class.getName())
                .log(Level.INFO, "Interaction parser started");
        
        //init fields
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        exp = Experiment.createOrGet(iaModel, getParameterValue(expP));
        ccsbMut = Authority.createOrGet(iaModel, "CCSB-Mutant");
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

    @Override
    protected void processRow(String[] cols) {
        
        //get model components
        Gene dbGene = Gene.createOrGet(iaModel, iaModel.ENTREZ, cols[dbId]);
        Protein dbProtein = Protein.createOrGet(iaModel, iaModel.ENTREZ, cols[dbId]);
        if (dbProtein.getEncodingGene() == null || !dbProtein.getEncodingGene().equals(dbGene)) {
            dbProtein.setEncodingGene(dbGene);
        }

        Gene adGene = Gene.createOrGet(iaModel, iaModel.ENTREZ, cols[adId]);
        Protein adProtein = Protein.createOrGet(iaModel, iaModel.ENTREZ, cols[adId]);
        if (adProtein.getEncodingGene() == null || !adProtein.getEncodingGene().equals(adGene)) {
            adProtein.setEncodingGene(adGene);
        }

        PhysicalInteraction interaction = PhysicalInteraction.createOrGet(iaModel, exp, physIntType, dbProtein, adProtein);

        Allele dbAllele = Allele.createOrGet(iaModel, ccsbMut, 
                cols[mut].equals("0") ? 
                cols[dbId]+"."+cols[mut] : 
                cols[mut]
        );
        if (dbAllele.getGene() == null || !dbAllele.getGene().equals(dbGene)) {
            dbAllele.setGene(dbGene);
        }


        //complement short lines
        String key = (cols.length < gro+1) ? "" : cols[gro];

        Growth growth = Growth.fromKey(key);

        //register allele with interaction
        if (growth != Growth.UNKNOWN) {
            Property affects = growth != Growth.NEG ? pos : neg;
            dbAllele.addProperty(affects, interaction);
        }
    }
    
    private static enum Growth {
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

    @Override
    public boolean requiresReasoner() {
        return false;
    }
}
