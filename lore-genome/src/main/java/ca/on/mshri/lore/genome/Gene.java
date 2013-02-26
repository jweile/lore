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
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.LoreModel;
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
public class Gene extends NucleotideFeature {

    public static final String CLASS_URI = GenomeModel.URI+"#Gene";
    
    protected Gene(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Gene fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl)i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new Gene(impl.asNode(), impl.getGraph());
            
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Gene!");
        }
    }
    
    public static Gene createOrGet(GenomeModel model, Authority auth, String id) {
        Gene out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Gene#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
    
    public List<Allele> listAlleles() {
        List<Allele> list = new ArrayList<Allele>();
        NodeIterator it = listPropertyValues(getModel().getProperty(GenomeModel.URI+"#hasAllele"));
        while (it.hasNext()) {
            list.add(Allele.fromIndividual(it.next().as(Individual.class)));
        }
        it.close();
        return list;
    }
    
    public String getSequence() {
        
        NodeIterator it = listPropertyValues(getModel().getProperty(GenomeModel.URI+"#sequence"));
        String out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getString();
            } else {
                //TODO: this might cause problems?
                throw new InconsistencyException("Gene "+getURI()+" should only have one sequence!");
            }
        }
        return out;
    }
    
    public void setSequence(String seq) {
        
        Property enc = getModel().getProperty(GenomeModel.URI+"#sequence");
        RDFNode existing = getPropertyValue(enc);
        if (existing != null) {
            Logger.getLogger(Gene.class.getName())
                    .log(Level.WARNING, "Overwriting existing sequence!");
            removeAll(enc);
        }
        addProperty(enc, seq);
    }
    
}
