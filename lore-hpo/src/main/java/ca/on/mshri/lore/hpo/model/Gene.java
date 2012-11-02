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
package ca.on.mshri.lore.hpo.model;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Gene individual in the Jena Model.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Gene extends IndividualImpl {

    Gene(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Gene fromIndividual(Individual i) {
        if (!i.getOntClass().getURI().equals(HpoOntModel.HPO+"Gene")) {
            throw new ClassCastException("The given individual cannot be cast to Gene!");
        }
        IndividualImpl impl = (IndividualImpl)i;
        return new Gene(impl.asNode(), impl.getGraph());
    }
    
    
    public void setName(String geneName) {
        //FIXME: check if one exists before adding a new one
        addProperty(getModel().getProperty(HpoOntModel.HPO+"hasName"),geneName);
    }
    
    public String getName() {
        RDFNode propertyValue = getPropertyValue(getModel().getProperty(HpoOntModel.HPO+"hasName"));
        if (propertyValue != null) {
            return propertyValue.asLiteral().getString();
        } else {
            return null;
        }
    }

    
    //TODO: XRef functionality should better be implemented in some superclass like ControlledObject
    public void addXRef(XRef xref) {
        //FIXME: check existence first?
        Property prop = getModel().getProperty(HpoOntModel.HPO+"hasXRef");
        addProperty(prop, xref);
    }
    
    public List<XRef> listXRefs() {
        NodeIterator it = listPropertyValues(getModel().getProperty(HpoOntModel.HPO+"hasXRef"));
        List<XRef> list = new ArrayList<XRef>();
        while (it.hasNext()) {
            XRef xref = XRef.fromIndividual(it.next().as(Individual.class));
            list.add(xref);
        }
        return list;
    }
}
