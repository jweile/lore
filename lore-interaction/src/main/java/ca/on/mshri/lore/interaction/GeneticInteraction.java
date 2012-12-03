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
package ca.on.mshri.lore.interaction;

import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.genome.NucleotideFeature;
import ca.on.mshri.lore.molecules.Molecule;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class GeneticInteraction extends Interaction {
    
    static final String CLASS_URI = InteractionModel.URI+"#GeneticInteraction";
    
    protected GeneticInteraction(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static GeneticInteraction fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        if (impl.getOntClass() != null && impl.getOntClass().getURI().equals(CLASS_URI)) {
            return new GeneticInteraction(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as GeneticInteraction!");
        }
    }
    
    @Override
    public List<NucleotideFeature> listParticipants() {
        List<NucleotideFeature> out = new ArrayList<NucleotideFeature>();
        
        Property hasParticipant = getModel().getProperty(InteractionModel.URI+"#hasParticipant");
        NodeIterator it = listPropertyValues(hasParticipant);
        while (it.hasNext()) {
            NucleotideFeature participant = NucleotideFeature.fromIndividual(it.next().as(Individual.class));
            out.add(participant);
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
    //FIXME: Should be restricted to NucleotideFeatures, but won't let me :(
    public static GeneticInteraction createOrGet(LoreModel model, 
             List<? extends RecordObject> participants, Experiment e) {
        
        return fromIndividual(Interaction.createOrGet(model, participants, e));
    }
}
