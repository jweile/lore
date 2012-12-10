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
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;

/**
 * An XRef is defined by its namespace and value, so xrefs with
 * euqal ns and value should have the same URI and thus not be able exist 
 * redundantly in the system.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRef extends IndividualImpl {

    public static final String CLASS_URI = LoreModel.URI+"#XRef";
    
    protected XRef(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static XRef fromIndividual(Individual i) {
        
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (impl.getOntClass() != null && 
                (impl.getOntClass().equals(thisType) || thisType.hasSubClass(impl.getOntClass(),false))) {
          
            return new XRef(impl.asNode(),impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as XRef object!");
        }
    }
    
    public Authority getAuthority() {
        
        NodeIterator it = listPropertyValues(getModel().getProperty(LoreModel.URI+"#hasAuthority"));
        Authority out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = Authority.fromIndividual(it.next().as(Individual.class));
            } else {
                throw new InconsistencyException("XRef "+getURI()+" should only have one namespace!");
            }
        }
        it.close();
        return out;
    }
    
    public String getValue() {
        
        NodeIterator it = listPropertyValues(getModel().getProperty(LoreModel.URI+"#hasValue"));
        String out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getString();
            } else {
                throw new InconsistencyException("XRef "+getURI()+" should only have one value!");
            }
        }
        it.close();
        return out;
    }
    
    public static XRef createOrGet(LoreModel model, Authority auth, String value) {
        
        XRef xref = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:XRef#"+auth.getAuthorityId()+":"+value));
        xref.addProperty(model.getProperty(LoreModel.URI+"#hasAuthority"), auth);
        xref.addLiteral(model.getProperty(LoreModel.URI+"#hasValue"), value);
        return xref;
    }
    
}
