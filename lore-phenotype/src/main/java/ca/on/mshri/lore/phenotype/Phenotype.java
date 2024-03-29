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
package ca.on.mshri.lore.phenotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Phenotype extends RecordObject {
    
    public static final String CLASS_URI = PhenotypeModel.URI+"#Phenotype";

    protected Phenotype(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Phenotype fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl)i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new Phenotype(impl.asNode(), impl.getGraph());
            
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Phenotype!");
        }
    }
    
    public static Phenotype createOrGet(PhenotypeModel model, Authority auth, String id) {
        Phenotype out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Phenotype#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }

    public String getName() {
        RDFNode propertyValue = getPropertyValue(getModel().getProperty(PhenotypeModel.URI+"hasName"));
        if (propertyValue == null) {
            return null;
        } else {
            return propertyValue.asLiteral().getString();
        }
    }

    public void setName(String name) {
        //FIXME: check if one exists before adding a new one
        addProperty(getModel().getProperty(PhenotypeModel.URI+"hasName"), name);
    }
    
}
