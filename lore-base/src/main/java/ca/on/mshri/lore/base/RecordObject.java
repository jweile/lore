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
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RecordObject extends IndividualImpl {

    static final String CLASS_URI = LoreModel.URI+"#RecordObject";
    
    protected RecordObject(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static RecordObject fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new RecordObject(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as RecordObject!");
        }
    }
    
    public List<XRef> listXRefs() {
        List<XRef> list = new ArrayList<XRef>();
        NodeIterator it = listPropertyValues(getModel().getProperty(LoreModel.URI+"#hasXRef"));
        while (it.hasNext()) {
            list.add(XRef.fromIndividual(it.next().as(Individual.class)));
        }
        return list;
    }
    
    public void addXRef(XRef xref) {
        addProperty(getModel().getProperty(LoreModel.URI+"#hasXRef"), xref);
    }
    
    public XRef addXRef(Authority ns, String value) {
        XRef xref = XRef.createOrGet((LoreModel)getOntModel(), ns, value);
        addXRef(xref);
        return xref;
    }
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static RecordObject createOrGet(LoreModel model, Authority auth, String id) {
        RecordObject out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:RecordObject#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
        
}
