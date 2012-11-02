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
package ca.on.mshri.lore.genome;

import ca.on.mshri.lore.base.Authority;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Gene extends NucleotideFeature {

    private static final String CLASS_URI = GenomeModel.URI+"#Gene";
    
    protected Gene(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Gene fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl)i;
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new Gene(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Gene!");
        }
    }
    
    public static Gene create(GenomeModel model, Authority auth, String id) {
        Gene out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Gene#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
    
}
