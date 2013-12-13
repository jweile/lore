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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.Publication;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.Species;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRefLinkerTest extends TestCase {
    
    public void test() {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Property testProperty = model.getProperty(LoreModel.URI+"#testProperty");
        
        Authority authA = Authority.createOrGet(model,"A");
        
        Publication pub1 = Publication.createOrGet(model, authA, "a");
        
        Publication pub2 = Publication.createOrGet(model, authA, "b");
        
        Species s1 = Species.createOrGet(model, authA, "a");
        
        Species s2 = Species.createOrGet(model, authA, "b");
        
        Species s3 = Species.createOrGet(model, authA, "c");
        s3.addXRef(authA, "b");
        
                
        //Perform merging
        XRefBasedLinker linker = new XRefBasedLinker();
        
        ResourceReferences<RecordObject> fromSet = linker.fromSetP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+Publication.CLASS_URI+">}");
        ResourceReferences<RecordObject> toSet = linker.fromSetP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+Species.CLASS_URI+">}");
        ResourceReferences<Authority> authority = linker.authorityP.validate(authA.getURI());
        ResourceReferences<Property> property = linker.propertyP.validate(testProperty.getURI());
        
        linker.setParameter(linker.fromSetP, fromSet);
        linker.setParameter(linker.toSetP, toSet);
        linker.setParameter(linker.authorityP, authority);
        linker.setParameter(linker.propertyP, property);
        
        linker.setModel(model);
        linker.run();
        
        //after
        NodeIterator nit = pub1.listPropertyValues(testProperty);
        List<Species> connected = new ArrayList<Species>();
        while (nit.hasNext()) {
            connected.add(Species.fromIndividual(nit.next().as(Individual.class)));
        }
        assertEquals(1,connected.size());
        assertTrue(connected.contains(s1));
        
        
        nit = pub2.listPropertyValues(testProperty);
        connected = new ArrayList<Species>();
        while (nit.hasNext()) {
            connected.add(Species.fromIndividual(nit.next().as(Individual.class)));
        }
        assertEquals(2,connected.size());
        assertTrue(connected.contains(s2));
        assertTrue(connected.contains(s3));
        
    }
    
}
