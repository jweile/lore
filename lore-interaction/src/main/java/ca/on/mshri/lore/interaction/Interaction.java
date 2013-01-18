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
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Interaction extends IndividualImpl {
    
    public static final String CLASS_URI = InteractionModel.URI+"#Interaction";
    
    protected Interaction(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Interaction fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new Interaction(impl.asNode(), impl.getGraph());
            
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Interaction!");
        }
    }
    
    
    public List<Experiment> listExperiments() {
        
        List<Experiment> out = new ArrayList<Experiment>();
        
        Property observedIn = getModel().getProperty(LoreModel.URI+"#observedIn");
        NodeIterator it = listPropertyValues(observedIn);
        while (it.hasNext()) {
            Experiment participant = Experiment.fromIndividual(it.next().as(Individual.class));
            out.add(participant);
        }
                
        return out;
    }
    
    public void addExperiment(Experiment e) {
        
        Property observedIn = getModel().getProperty(LoreModel.URI+"#observedIn");
        addProperty(observedIn, e);
    }
    
    
    public List<? extends RecordObject> listParticipants() {
        List<RecordObject> out = new ArrayList<RecordObject>();
        
        Property hasParticipant = getModel().getProperty(InteractionModel.URI+"#hasParticipant");
        NodeIterator it = listPropertyValues(hasParticipant);
        while (it.hasNext()) {
            RecordObject participant = RecordObject.fromIndividual(it.next().as(Individual.class));
            out.add(participant);
        }
                
        return out;
    }
       
    
    
    /**
     * Pseudo-constructor. 
     * Warning: works under the assumption that the given type has a URI that
     * follows the syntax <code>Namespace#ID</code>. Also assumes that the participants
     * are Lore objects and thus have URIs following the syntax <code>urn:lore:Class#ID</code>.
     * 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static Interaction createOrGet(InteractionModel model, 
            Experiment e, OntClass type, RecordObject... participants) {
        
        //sort participants to ensure reciprocity of identifiers
        Arrays.sort(participants, new Comparator<RecordObject>() {
            public int compare(RecordObject t, RecordObject t1) {
                return t.getURI().compareTo(t1.getURI());
            }
        });
        
        //check that the given type is actually a subclass
        if (!LoreModel.isSubClassOf(type, model.getOntClass(CLASS_URI))) {
            throw new ConversionException(type+" is not a subclass of "+CLASS_URI);
        }
        
        String typeTag = type.getURI().split("#")[1];
        
        StringBuilder b = new StringBuilder("urn:lore:").append(typeTag).append("#(");
        for (RecordObject o : participants) {
            b.append(o.getURI().substring(9).replace('#', ':'))
                    .append("-");
        }
        b.deleteCharAt(b.length()-1);
        b.append(")");
//        if (e != null) {
//        b.append("$");
//        b.append(e.getURI());
//        }
       
        Interaction out = fromIndividual(type
                            .createIndividual(b.toString()));
        
        Property hasParticipant = model.getProperty(InteractionModel.URI+"#hasParticipant");
        for (RecordObject o : participants) {
            out.addProperty(hasParticipant, o);
        }
        if (e != null) {
            Property observedIn = model.getProperty(LoreModel.URI+"#observedIn");
            out.addProperty(observedIn, e);
        }
        
        
        return out;
    }
    
}
