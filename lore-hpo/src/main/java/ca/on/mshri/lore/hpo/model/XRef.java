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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRef extends IndividualImpl {

    XRef(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static XRef fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        return new XRef(impl.asNode(), impl.getGraph());
    }
    
    public String getValue() {
        RDFNode propertyValue = getPropertyValue(getModel().getProperty(HpoOntModel.HPO+"hasValue"));
        if (propertyValue == null) {
            return null;
        } else {
            return propertyValue.asLiteral().getString();
        }
    }
    
    public Individual getNamespace() {
        Resource resource = getPropertyResourceValue(getModel().getProperty(HpoOntModel.HPO+"fromNamespace"));
        if (resource == null) {
            return null;
        } else {
            return resource.as(Individual.class);
        }
    }
}
