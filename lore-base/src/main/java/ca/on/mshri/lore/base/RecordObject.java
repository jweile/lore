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
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the central component of a lore model. Most individuals in a model will
 * be RecordObjects, which are objects that have a record in a database somewhere.
 * This is expressed through the fact RecordObjects have one or more cross-references
 * (XRefs).
 * 
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RecordObject extends IndividualImpl {

    public static final String CLASS_URI = LoreModel.URI+"#RecordObject";
    
    protected RecordObject(Node n, EnhGraph g) {
        super(n, g);
    }
    
    /**
     * <p>"Casts" an individual to a RecordObject if possible.</p>
     * 
     * <p><i>Development Note: All OWL-class wrappers need to have this method. In principle this should
     * be done using the as() function as in Jena. However the required 
     * Interface-Implementation duality enforced by Jena makes that route more tedium 
     * than it's worth. The only way I can see how to do this properly is to write
     * a code autogenerator that creates these wrapper classes from given OWL files.</i></p>
     * 
     * @param i the individual to cast.
     * @return the cast RecordObject
     * @throws ConversionException if the individual is incompatible with RecordObject.
     */
    public static RecordObject fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new RecordObject(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as RecordObject!");
        }
    }
    
    /**
     * List all associatiated XRefs.
     * @return 
     */
    public List<XRef> listXRefs() {
        List<XRef> list = new ArrayList<XRef>();
        NodeIterator it = listPropertyValues(getModel().getProperty(LoreModel.URI+"#hasXRef"));
        while (it.hasNext()) {
            list.add(XRef.fromIndividual(it.next().as(Individual.class)));
        }
        it.close();
        return list;
    }
    
    /**
     * Returns the value of one of the xrefs with the given authority for this object.
     * If there are multiple xrefs of that authority associated with this object, it 
     * is not defined, which one will be returned. If none are found, null is returned.
     * @param a the authority.
     * @return 
     */
    public String getXRefValue(Authority a) {
        for (XRef xref : listXRefs()) {
            if (xref.getAuthority().equals(a)) {
                return xref.getValue();
            }
        }
        return null;
    }
    
    /**
     * Adds an XRef to this object.
     * @param xref 
     */
    public void addXRef(XRef xref) {
        addProperty(getModel().getProperty(LoreModel.URI+"#hasXRef"), xref);
    }
    
    /**
     * Adds an XRef to this object
     * @param ns
     * @param value
     * @return 
     */
    public XRef addXRef(Authority ns, String value) {
        XRef xref = XRef.createOrGet((LoreModel)getOntModel(), ns, value);
        addXRef(xref);
        return xref;
    }
    
    /**
     * Pseudo-constructor. The actual constructor is protected and must not be called
     * from the outside. This method creates a new RecordObject in the given model using
     * the given authorty and identifier to create an XRef for the new Object. The authority
     * and identifier also inform the URI that will be used for this object, thus calling
     * This method again with the same parameters will return the existing object instead of 
     * re-creating it.
     * 
     * @param model the model
     * @param auth an XRef authority
     * @param id an ID that is controlled by the given authority.
     * @return 
     */
    public static RecordObject createOrGet(LoreModel model, Authority auth, String id) {
        RecordObject out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:RecordObject#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
        
}
