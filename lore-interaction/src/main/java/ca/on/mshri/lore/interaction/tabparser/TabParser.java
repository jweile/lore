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
package ca.on.mshri.lore.interaction.tabparser;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.interaction.Interaction;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.molecules.MoleculesModel;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parse a tab-delimted file with two columns, interpreting each row as a pair of 
 * interactors forming an interaction. 
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TabParser extends LoreOperation {
    
//    public Parameter<InteractionModel> modelP = Parameter.make("model", InteractionModel.class);
    
    public final URLParameter srcP = new URLParameter("src");
    
    /**
     * The namespace of the identifiers listed in each column of the input file, e.g. EntrezGene.
     */
    public final Parameter<String> interactorAuthP = Parameter.make("interactorAuth", String.class);
    
    /**
     * Experiment id. Must not contain whitespaces.
     */
    public final Parameter<String> experimentP = Parameter.make("experiment", String.class);
    
    /**
     * The type of interaction, e.g. PhysicalInteraction
     */
    public final RefListParameter<OntClass> interactionTypeP = new RefListParameter("interactionType", OntClass.class);
    
    /**
     * The type of interactor, e.g. Molecule
     */
    public final RefListParameter<OntClass> interactorTypeP = new RefListParameter("interactorType", OntClass.class);
    
    /**
     * Whether or not the file contains a header line that needs to be ignored.
     */
    public final Parameter<Boolean> headerP = Parameter.make("header", Boolean.class, false);
    
    /**
     * Run
     */
    public void run() {
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        
        Authority interactorNS = Authority.createOrGet(model, getParameterValue(interactorAuthP));
        
        Experiment exp = Experiment.createOrGet(model, getParameterValue(experimentP));
        
        OntClass interactionType = ((List<OntClass>)getParameterValue(interactionTypeP).resolve(model)).get(0);
        
        OntClass interactorType = ((List<OntClass>)getParameterValue(interactorTypeP).resolve(model)).get(0);
        
        boolean header = getParameterValue(headerP);
        
        //FIXME: Needs to be infered from interactortype. Will break for non-molecule interactors!
        Class<?> moduleClass = MoleculesModel.class;
        
        InputStream in = null;
        try {
            
            in = getParameterValue(srcP).openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                if (header && lnum == 1) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                if (cols.length < 2) {
                    Logger.getLogger(TabParser.class.getName())
                            .log(Level.WARNING, "Invalid content in line #"+lnum);
                    continue;
                }
                      
                RecordObject i1 = getOrCreateInteractor(model, interactorType, moduleClass, interactorNS, cols[0]);
                RecordObject i2 = getOrCreateInteractor(model, interactorType, moduleClass, interactorNS, cols[1]);
                
                Interaction.createOrGet(model, exp, interactionType, i1, i2);
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read tab-delimited stream.",ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TabParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
        
    }
    
    private RecordObject getOrCreateInteractor(InteractionModel model, 
            OntClass interactorType, Class<?> interactorModuleClass, Authority interactorNS, String id) {
        try {
            
            String iName = interactorType.getURI().split("#")[1];
            
            //create manually, since we don't have access to the wrapper.
            Individual outInd = interactorType.createIndividual("urn:lore:"+iName+"#"+interactorNS.getAuthorityId()+":"+id);
            RecordObject out = RecordObject.fromIndividual(outInd);
            out.addXRef(interactorNS, id);
            
//            Method createOrGet = interactorType
//                    .getMethod("createOrGet", interactorModuleClass, Authority.class, String.class);
//            RecordObject out = (RecordObject) createOrGet.invoke(null, model, interactorNS, id);
            return out;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean requiresReasoner() {
        return true;
    }
    
}
