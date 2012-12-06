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
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.String;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class InteractionParser {
    
    //column indices
    private static final int mut = 1;
    private static final int dbId = 4;
    private static final int adId = 6;
    private static final int gro = 12;
        
    public void parse(InteractionModel model, InputStream in) {
        
        Authority entrez = Authority.createOrGet(model, "EntrezGene");
        Authority ccsbMut = Authority.createOrGet(model, "CCSB-Mutant");
        Experiment exp = Experiment.createOrGet(model, "CCSB-Edgotyping-1.0");
        OntClass protein = model.getOntClass(Protein.CLASS_URI);
        
        Property pos = model.getProperty(InteractionModel.URI+"#affectsPositively");
        Property neg = model.getProperty(InteractionModel.URI+"#affectsNegatively");
        
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        
        
        try {
            //read file
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                //skip header line
                if (lnum < 2) {
                    continue;
                }
                
                //split line into columns
                String[] cols = line.split("\t");
                
                //skip broken lines
                if (cols.length < adId+1) {
                    Logger.getLogger(InteractionParser.class.getName())
                            .log(Level.WARNING, "Corrupt row: "+lnum);
                    continue;
                }
                
                //get model components
                Gene dbGene = Gene.createOrGet(model, entrez, cols[dbId]);
                Protein dbProtein = Protein.createOrGet(model,entrez, cols[dbId]);
                dbProtein.setEncodingGene(dbGene);
                
                Gene adGene = Gene.createOrGet(model, entrez, cols[adId]);
                Protein adProtein = Protein.createOrGet(model,entrez, cols[adId]);
                adProtein.setEncodingGene(adGene);
                
                PhysicalInteraction interaction = PhysicalInteraction.createOrGet(model, exp, protein, dbProtein, adProtein);
                
                Allele dbAllele = Allele.createOrGet(model, ccsbMut, cols[mut]);
                dbAllele.setGene(dbGene);
                
                
                //complement short lines
                String key = (cols.length < gro+1) ? "" : cols[gro];
                
                Growth growth = Growth.fromKey(key);
                
                //register allele with interaction
                if (growth != Growth.UNKNOWN) {
                    Property affects = growth != Growth.NEG ? pos : neg;
                    dbAllele.addProperty(affects, interaction);
                }
                
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read interacion data",ex);
        } finally {
            try {
                r.close();
            } catch (IOException ex) {
                Logger.getLogger(InteractionParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
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
}
