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
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRefMergerTest extends TestCase {
    
    public void testChaining() {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority authA = Authority.createOrGet(model,"A");
        
        RecordObject o1 = RecordObject.createOrGet(model, authA, "a");
        o1.addXRef(authA, "b");
        
        RecordObject o2 = RecordObject.createOrGet(model, authA, "b");
        o2.addXRef(authA, "c");
        
        RecordObject.createOrGet(model, authA, "c");
        
        RecordObject.createOrGet(model, authA, "d");
        
        //before
        List<RecordObject> objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals("Wrong number of objects before merging.", 4, objects.size());
                
        //Perform merging
        XRefBasedMerger merger = new XRefBasedMerger();
        
        ResourceReferences<RecordObject> selection = merger.selectionP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+RecordObject.CLASS_URI+">}");
        ResourceReferences<Authority> authority = merger.authorityP.validate(authA.getURI());
        
        merger.setParameter(merger.selectionP, selection);
        merger.setParameter(merger.authorityP, authority);
        
        merger.setModel(model);
        merger.run();
                
        //after
        objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals("Wrong number of objects after merging.", 2, objects.size());
        
        for (RecordObject o : objects) {
            System.out.println(o.getURI());
            for (XRef xref : o.listXRefs()) {
                System.out.println("-> "+xref.getValue());
            }
        }
    }
    
    
    public void testMatchAll() {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority authA = Authority.createOrGet(model,"A");
        
        RecordObject o1 = RecordObject.createOrGet(model, authA, "a");
        o1.addXRef(authA, "b");
        
        RecordObject o2 = RecordObject.createOrGet(model, authA, "b");
        o2.addXRef(authA, "a");
        
        RecordObject.createOrGet(model, authA, "c");
        
        RecordObject.createOrGet(model, authA, "d");
        
        //before
        List<RecordObject> objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals("Wrong number of objects before merging.", 4, objects.size());
                
        //Perform merging
        XRefBasedMerger merger = new XRefBasedMerger();
        
        
        ResourceReferences<RecordObject> selection = merger.selectionP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+RecordObject.CLASS_URI+">}");
        ResourceReferences<Authority> authority = merger.authorityP.validate(authA.getURI());
        
        merger.setParameter(merger.selectionP, selection);
        merger.setParameter(merger.authorityP, authority);
        merger.setParameter(merger.allMustMatchP, true);
        
        merger.setModel(model);
        merger.run();
        
        
        //after
        objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals("Wrong number of objects after merging.", 3, objects.size());
                
        for (RecordObject o : objects) {
            System.out.println(o.getURI());
            for (XRef xref : o.listXRefs()) {
                System.out.println("-> "+xref.getValue());
            }
        }
    }
    
    public void testUnique() {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority authA = Authority.createOrGet(model,"A");
        Authority authB = Authority.createOrGet(model,"B");
        
        RecordObject o1 = RecordObject.createOrGet(model, authA, "1");
        o1.addXRef(authB, "a");
        
        RecordObject o2 = RecordObject.createOrGet(model, authA, "2");
        o2.addXRef(authB, "a");
        
        RecordObject o3 = RecordObject.createOrGet(model, authA, "3");
        o3.addXRef(authB, "a");
        
        RecordObject o4 = RecordObject.createOrGet(model, authA, "4");
        o4.addXRef(authB, "b");
        o4.addXRef(authB, "c");
        
        //before
        List<RecordObject> objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals("Wrong number of objects before merging.", 4, objects.size());
                
        //Perform merging
        XRefBasedMerger merger = new XRefBasedMerger();
        merger.setModel(model);
        
        ResourceReferences<RecordObject> selection = merger.selectionP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+RecordObject.CLASS_URI+">}");
        ResourceReferences<Authority> authority = merger.authorityP.validate(authB.getURI());
        
        merger.setParameter(merger.selectionP, selection);
        merger.setParameter(merger.authorityP, authority);
        merger.setParameter(merger.uniqueKeysP, true);
        merger.setModel(model);
        
        boolean failed = false;
        try {
            merger.run();
        } catch (InconsistencyException e) {
            failed = true;
        }
        
        assertEquals("Non-uniqueness did not trigger excption. ",true, failed);
        
    }
    
}
