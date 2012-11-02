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
package ca.on.mshri.lore.base;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Authority extends IndividualImpl {

    static final String CLASS_URI = LoreModel.URI+"#Authority";
    /**
     * prefix for individual uris.
     */
    static final String IND_URI_PRE = "urn:lore:Authority#";
    
    protected Authority(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Authority createOrGet(LoreModel model, String id) {
        return Authority.fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual(IND_URI_PRE+id));
    }
    
    public static Authority fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new Authority(impl.asNode(),impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Authority object!");
        }
    }
    
    public URL getURL() throws MalformedURLException {
        String string = getPropertyValue(getModel().getProperty(LoreModel.URI+"#hasURL"))
                .asLiteral().getString();
        return new URL(string);
    }
    
    public void setURL(URL url) {
        setPropertyValue(getModel().getProperty(LoreModel.URI+"#hasURL"), 
                getModel().createLiteral(url.toString()));
    }
    
    public String getAuthorityId() {
        return getURI().substring(IND_URI_PRE.length());
    }
    
}
