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

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Phenotype extends IndividualImpl {

    Phenotype(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Phenotype fromIndividual(Individual i) {
        if (!i.hasOntClass(HpoOntModel.HPO+"Phenotype")) {
            throw new ClassCastException("The given individual cannot be cast to Phenotype!");
        }
        IndividualImpl impl = (IndividualImpl)i;
        return new Phenotype(impl.asNode(), impl.getGraph());
    }

    public String getName() {
        RDFNode propertyValue = getPropertyValue(getModel().getProperty(HpoOntModel.HPO+"hasName"));
        if (propertyValue == null) {
            return null;
        } else {
            return propertyValue.asLiteral().getString();
        }
    }

    public void setName(String name) {
        //FIXME: check if one exists before adding a new one
        addProperty(getModel().getProperty(HpoOntModel.HPO+"hasName"), name);
    }
    
}
