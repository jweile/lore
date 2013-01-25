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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Creates an entity for each row in the file that carries XRefs for all the
 * entries in the row. The authorities of these XRefs will be determined by the
 * <code>authorities</code> parameter.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ImportTranslationTable extends TabDelimParser {

    /**
     * the input file/URL
     */
    public final URLParameter srcP = new URLParameter("src");
    
    /**
     * The entity type to use 
     */
    public final RefListParameter<OntClass> classP = new RefListParameter<OntClass>("class",OntClass.class);
    
    /**
     * Comma separated list of authority names in order of the columns. E.g. "EntrezGene,UNIPROT"
     */
    public final Parameter<String> authoritesP = Parameter.make("authorities", String.class);
    
    public final Parameter<Integer> skipP = Parameter.make("skip", Integer.class, 0);
    
    //fields
    private OntClass clazz;
    private List<Authority> authorities;
    private String prefix;
    
    @Override
    public void run() {
        
        //get source parameter
        URL src = getParameterValue(srcP);
        if (src == null) {
            throw new IllegalArgumentException("Parameter src required!");
        }
        
        //get class parameter
        ResourceReferences<OntClass> classParVal = (ResourceReferences<OntClass>)getParameterValue(classP);
        if (classParVal == null) {
            throw new IllegalArgumentException("Parameter class required!");
        }
        clazz = classParVal.resolve(getModel()).get(0);
        
        //complain if it's not a subtype of recordobject
        if (!LoreModel.isSubClassOf(clazz, getModel().getOntClass(RecordObject.CLASS_URI))) {
            throw new IllegalArgumentException("Parameter class must be subclass of RecordObject!");
        }
        
        //generate a prefix for member URIs
        prefix = "urn:lore:"+clazz.getURI().split("#")[1]+"#";
        
        //get and process authorities parameter
        String authoritiesStr = getParameterValue(authoritesP);
        if (authoritiesStr == null) {
            throw new IllegalArgumentException("Parameter authorites required!");
        }
        authorities = new ArrayList<Authority>();
        for (String authS : authoritiesStr.split(",")) {
            authorities.add(Authority.createOrGet(getModel(), authS.trim()));
        }
        
        int skip = getParameterValue(skipP);
        
        //start parsing
        try {
            parseTabDelim(src.openStream(), skip, authorities.size());
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read "+src, ex);
        }
        
    }

    @Override
    protected void processRow(String[] cols) {
        
        //create object
        RecordObject obj = RecordObject.fromIndividual(
                clazz.createIndividual(prefix+authorities.get(0).getAuthorityId()+":"+cols[0]));
        
        //add XRefs
        for (int i = 0; i < cols.length; i++) {
            obj.addXRef(authorities.get(i), cols[i]);
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
