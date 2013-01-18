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
package ca.on.mshri.lore.interaction.yu.strucint;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.ProteinDomain;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliIndeterminateProgress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class StrucIntParser extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    public final Parameter<String> experimentP = Parameter.make("experiment", String.class, "Wang_2012");
    
    @Override
    public void run() {
        
        Logger.getLogger(StrucIntParser.class.getName()).log(Level.INFO, "Structural interactome parser started.");
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        OntClass physInt = model.getOntClass(PhysicalInteraction.CLASS_URI);
        Authority domAuth = Authority.createOrGet(model, "LoreProteinDomain");
        
        Property involvedIn = model.getProperty(InteractionModel.URI+"#involvedIn");
        
        Experiment exp = Experiment.createOrGet(model, getParameterValue(experimentP));
        
        URL src = getParameterValue(srcP);
        
        int proteinA = 0;
        int proteinB = 1;
        int pfamA = 2;
        int startA = 3;
        int endA = 4;
        int pfamB = 5;
        int startB = 6;
        int endB = 7;
        
        InputStream in = null;
        try {
            
            in = src.openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                //skip header
                if (lnum < 6) {
                    progress.next("Parsing");
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                if (cols.length < 8) {
                    Logger.getLogger(StrucIntParser.class.getName())
                            .log(Level.WARNING, "Invalid line: "+lnum);
                    progress.next("Parsing");
                    continue;
                }
                
                Protein pA = Protein.createOrGet(model, model.ENTREZ, cols[proteinA]);
                Protein pB = Protein.createOrGet(model, model.ENTREZ, cols[proteinB]);
                
                PhysicalInteraction interaction = PhysicalInteraction.createOrGet(model, exp, physInt, pA,pB);
                
                ProteinDomain domA = processDomain(model, domAuth, cols[proteinA], 
                        cols[pfamA], pA, Integer.parseInt(cols[startA]), 
                        Integer.parseInt(cols[endA]));
                ProteinDomain domB = processDomain(model, domAuth, cols[proteinB], 
                        cols[pfamB], pB, Integer.parseInt(cols[startB]), 
                        Integer.parseInt(cols[endB]));
                
                domA.addProperty(involvedIn, interaction);
                domB.addProperty(involvedIn, interaction);
                
                progress.next("Parsing");
            }
            
            progress.done();
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read "+src, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(StrucIntParser.class.getName())
                            .log(Level.WARNING, "Unable to close stream", ex);
                }
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private ProteinDomain processDomain(InteractionModel model, Authority domAuth,  
            String entrez, String pfam, Protein protein, int start, int end) {
        
        StringBuilder id = new StringBuilder();
        id.append(entrez).append(".").append(pfam).append(":").append(start).append("-").append(end);
        
        ProteinDomain domain = ProteinDomain.createOrGet(model, domAuth, id.toString());
        
        if (domain.getProtein() != null) { 
            //it's a pre-existing domain
            if (!domain.getProtein().equals(protein)) {
                throw new InconsistencyException("Domain associated with multiple proteins!");
            } 
        } else {
            //it's a new domain
            domain.addXRef(model.PFAM, pfam);
            domain.setProtein(protein);
            domain.setStart(start);
            domain.setEnd(end);
        }
        return domain;
    }
    
}
