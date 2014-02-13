/*
 * Copyright (C) 2014 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.phenotype.hdo;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.generic.OboParser;
import ca.on.mshri.lore.generic.OboParser.Stanza;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.URLParameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class DiseaseOntologyParser extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    private PhenotypeModel phenoModel;
    
    private Property isaProp;
    
    private Map<String, Stanza> stanzas;
    private HashSet<Phenotype> closed = new HashSet<Phenotype>();
    
    @Override
    public void run() {
        
        phenoModel = new PhenotypeModel(OntModelSpec.OWL_MEM, getModel());
        isaProp = phenoModel.getProperty(PhenotypeModel.URI+"#is_a");
        
        URL src = getParameterValue(srcP);
        
        InputStream in = null;
        try {
            
            in = src.openStream();
            stanzas = new OboParser().parse(in);
            
            for (String id : stanzas.keySet()) {
                Stanza stanza = stanzas.get(id);
                switch (stanza.getStanzaType()) {
                    case HEADER:
                        processHeader(stanza);
                        break;
                    case TYPEDEF:
                        processTypeDef(stanza);
                        break;
                    case TERM:
                        processTerm(stanza);
                        break;
                    case INSTANCE:
                        //shouldn't occur
                        break;
                }
            }
            
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to read source: "+src,ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(DiseaseOntologyParser.class.getName())
                            .log(Level.WARNING, "Unable to close stream!", ex);
                }
            }
        }
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private void processHeader(Stanza header) {
        //ignore for now
    }

    private void processTypeDef(Stanza typedef) {
        //TODO
    }

    private Phenotype processTerm(Stanza term) {
        
        //get id
        String[] idsplit = splitRef(term.getID());
                
        //get or create the phenotype object
        Phenotype phenotype = Phenotype.createOrGet(phenoModel, phenoModel.DO, idsplit[1]);
        
        //if this was processed previously, we can skip this.
        if (closed.contains(phenotype)) {
            
            return phenotype;
            
        } else {
        
            //otherwise we'll add the name
            phenotype.addLabel(term.getName(),null);

            //add all the cross-references
            for (String xrefStr :term.getXRefs()) {
                String[] splitRef = splitRef(xrefStr);
                Authority auth = Authority.createOrGet(phenoModel, splitRef[0]);
                phenotype.addXRef(auth, splitRef[1]);
            }
            
            //and link to parent terms (if any)
            String isA = term.getIsA();
            if (isA != null) {
                Stanza parent = stanzas.get(isA);
                if (parent != null) {
                    Phenotype parentPheno = processTerm(parent);
                    phenotype.addProperty(isaProp, parentPheno);
                } else {
                    Logger.getLogger(DiseaseOntologyParser.class.getName())
                            .log(Level.WARNING, "Unresolved reference: "+isA);
                }

            }

            closed.add(phenotype);

            return phenotype;
            }
    }

    private String[] splitRef(String ref) {
        String[] split = ref.split(":");
        if (split.length < 2) {
            Logger.getLogger(DiseaseOntologyParser.class.getName())
                    .log(Level.WARNING, "Invalid Ref:"+ref);
        }
        return split;
    }
    
}
