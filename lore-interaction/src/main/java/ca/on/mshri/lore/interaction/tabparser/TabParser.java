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
import ca.on.mshri.lore.operations.Parameter;
import com.hp.hpl.jena.ontology.OntClass;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TabParser extends LoreOperation {
    
    public Parameter<InteractionModel> modelP = Parameter.make("model", InteractionModel.class);
    
    public Parameter<InputStream> inP = Parameter.make("in", InputStream.class);
    
    public Parameter<Authority> interactorAuthP = Parameter.make("interactorAuth", Authority.class);
    
    public Parameter<Experiment> experimentP = Parameter.make("experiment", Experiment.class);
    
    public Parameter<OntClass> interactionTypeP = Parameter.make("interactionType", OntClass.class);
    
    public Parameter<Class> interactorTypeP = Parameter.make("interactorType", Class.class);
    
    public Parameter<Boolean> headerP = Parameter.make("header", Boolean.class, false);
    
    public void run() {
        
        InteractionModel model = getParameterValue(modelP);
        InputStream in = getParameterValue(inP);
        Authority interactorNS = getParameterValue(interactorAuthP);
        Experiment exp = getParameterValue(experimentP);
        OntClass interactionType = getParameterValue(interactionTypeP);
        Class<? extends RecordObject> interactorType = getParameterValue(interactorTypeP);
        boolean header = getParameterValue(headerP);
        
        //FIXME: Needs to be infered from interactortype. Will break for non-molecule interactors!
        Class<?> moduleClass = MoleculesModel.class;
        
        BufferedReader r = null;
        try {
            
            r = new BufferedReader(new InputStreamReader(in));
            
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
                if (r != null) {
                    r.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TabParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
        
    }
    
    private RecordObject getOrCreateInteractor(InteractionModel model, 
            Class<? extends RecordObject> interactorType, Class<?> interactorModuleClass, Authority interactorNS, String id) {
        try {
            Method createOrGet = interactorType
                    .getMethod("createOrGet", interactorModuleClass, Authority.class, String.class);
            RecordObject out = (RecordObject) createOrGet.invoke(null, model, interactorNS, id);
            return out;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
