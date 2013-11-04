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
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parse a tab-delimted file with two columns, interpreting each row as a pair of 
 * interactors forming an interaction. 
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TabParser extends TabDelimParser {
    
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
    private InteractionModel iaModel;
    private Authority interactorNS;
    private Experiment exp;
    private OntClass interactionType;
    private OntClass interactorType;
    
    /**
     * Run
     */
    public void run() {
        
        Logger.getLogger(TabParser.class.getName()).log(Level.INFO, "Starting TabParser...");
        
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        interactorNS = Authority.createOrGet(iaModel, getParameterValue(interactorAuthP));
        exp = Experiment.createOrGet(iaModel, getParameterValue(experimentP));
        interactionType = ((List<OntClass>)getParameterValue(interactionTypeP).resolve(iaModel)).get(0);
        interactorType = ((List<OntClass>)getParameterValue(interactorTypeP).resolve(iaModel)).get(0);
        
        boolean header = getParameterValue(headerP);
        
        URL url = getParameterValue(srcP);
        if (url == null) {
            throw new IllegalArgumentException("Parameter src required!");
        }
        
        try {
            parseTabDelim(url.openStream(), header ? 1 : 0, 2);
        } catch (IOException ex) {
            Logger.getLogger(TabParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    
    @Override
    protected void processRow(String[] cols) {
        
        RecordObject i1 = getOrCreateInteractor(cols[0]);
        RecordObject i2 = getOrCreateInteractor(cols[1]);

        Interaction.createOrGet(iaModel, exp, interactionType, i1, i2);
        
    }
    
    private RecordObject getOrCreateInteractor(String id) {
        
        String iName = interactorType.getURI().split("#")[1];

        //create manually, since we don't have access to the wrapper.
        Individual outInd = interactorType.createIndividual("urn:lore:"+iName+"#"+interactorNS.getAuthorityId()+":"+id);
        RecordObject out = RecordObject.fromIndividual(outInd);
        out.addXRef(interactorNS, id);

        return out;
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    
}
