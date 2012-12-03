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
package ca.on.mshri.lore.molecules;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.genome.Gene;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Protein extends Molecule {
    
    public static final String CLASS_URI = MoleculesModel.URI+"#Protein";
    
    protected Protein(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Protein fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (impl.getOntClass() != null && 
                (impl.getOntClass().equals(thisType) || thisType.hasSubClass(impl.getOntClass(),false))) {
            return new Protein(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Molecule!");
        }
    }
    
    public Gene getEncodingGene() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#encodedBy"));
        Gene out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = Gene.fromIndividual(it.next().as(Individual.class));
            } else {
                //TODO: this might cause problems?
                throw new InconsistencyException("Protein "+getURI()+" should only have one encoding gene!");
            }
        }
        return out;
    }
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static Protein createOrGet(LoreModel model, Authority auth, String id) {
        Protein out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Protein#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
}
