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
import com.hp.hpl.jena.rdf.model.Property;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Experiment extends IndividualImpl {
    
    static final String CLASS_URI = LoreModel.URI+"#Experiment";
    
    protected Experiment(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Experiment fromIndividual(Individual i) {
        
        IndividualImpl impl = (IndividualImpl) i;
        
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new Experiment(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Experiment!");
        }
        
    }
    
    public Publication getPublication() {
        NodeIterator it = listPropertyValues(getModel().getProperty(LoreModel.URI+"#publishedIn"));
        Publication out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = Publication.fromIndividual(it.next().as(Individual.class));
            } else {
                throw new InconcistencyException("Experiment "+getURI()+" should only have one publication!");
            }
        }
        it.close();
        return out;
    }
    
    public void setPublication(Publication p) {
        
        Property pubIn = getModel().getProperty(LoreModel.URI+"#publishedIn");
        
        Publication oldP = getPublication();
        if (oldP != null) {
            Logger.getLogger(Experiment.class.getName())
                    .log(Level.WARNING, "Overwriting existing publication for experiment "+getURI());
            removeProperty(pubIn, oldP);
        }
        
        addProperty(pubIn, p);
    }
    
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static Experiment createOrGet(LoreModel model, String id) {
        Experiment out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Experiment#"+id));
        return out;
    }
}
