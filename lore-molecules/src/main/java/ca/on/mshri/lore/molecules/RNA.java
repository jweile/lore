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
import ca.on.mshri.lore.base.LoreModel;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RNA extends Molecule {
    
    static final String CLASS_URI = MoleculesModel.URI+"#RNA";
    
    protected RNA(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static RNA fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new RNA(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as RNA!");
        }
    }
    
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static RNA createOrGet(LoreModel model, Authority auth, String id) {
        RNA out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:RNA#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
}
