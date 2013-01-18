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
import ca.on.mshri.lore.base.RecordObject;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ProteinDomain extends RecordObject {
    
    public static final String CLASS_URI = MoleculesModel.URI+"#ProteinDomain";
    
    protected ProteinDomain(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static ProteinDomain fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new ProteinDomain(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Molecule!");
        }
    }
    
    public void setStart(int start) {
        Property prop = getModel().getProperty(MoleculesModel.URI+"#start");
        cleanup(prop, "start value");
        
        addProperty(prop, start);
    }
    
    public Integer getStart() {
        Property prop = getModel().getProperty(MoleculesModel.URI+"#start");
        NodeIterator it = listPropertyValues(prop);
        Integer start = null;
        while (it.hasNext()) {
            if (start != null) {
                throw new InconsistencyException("Domain has more than one start attribute!");
            }
            start = it.next().asLiteral().getInt();
        }
        return start;
    }
    
    public void setEnd(int end) {
        Property prop = getModel().getProperty(MoleculesModel.URI+"#end");
        cleanup(prop, "end value");
        
        addProperty(prop, end);
    }
    
    public Integer getEnd() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#end"));
        Integer end = null;
        while (it.hasNext()) {
            if (end != null) {
                throw new InconsistencyException("Domain has more than one end attribute!");
            }
            end = it.next().asLiteral().getInt();
        }
        return end;
    }
    
    public void setProtein(Protein protein) {
        Property prop = getModel().getProperty(MoleculesModel.URI+"#domainOf");
        cleanup(prop, "protein");
        addProperty(prop, protein);
    }
    
    public Protein getProtein() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#domainOf"));
        Protein protein = null;
        while (it.hasNext()) {
            if (protein != null) {
                throw new InconsistencyException("Domain has more than one protein!");
            }
            protein = Protein.fromIndividual(it.next().as(Individual.class));
        }    
        return protein;
    }
        
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static ProteinDomain createOrGet(MoleculesModel model, Authority auth, String id) {
        ProteinDomain out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:ProteinDomain#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }

    private void cleanup(Property prop, String name) {
        
        NodeIterator it = listPropertyValues(prop);
        List<RDFNode> toRemove = new ArrayList<RDFNode>();
        while (it.hasNext()) {
            toRemove.add(it.next());
        }
        if (!toRemove.isEmpty()) {
            Logger.getLogger(ProteinDomain.class.getName())
                    .log(Level.WARNING, "Overwriting pre-existing "+name+" for domain "+getURI());
        }
        for (RDFNode node : toRemove) {
            removeProperty(prop, node);
        }
    }
}
