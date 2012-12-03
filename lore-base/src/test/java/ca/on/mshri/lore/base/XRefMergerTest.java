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

import ca.on.mshri.lore.operations.XRefBasedMerger;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.ArrayList;
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
        o1.addXRef(authA, "c");
        
        RecordObject.createOrGet(model, authA, "c");
        
        RecordObject.createOrGet(model, authA, "d");
        
        //before
        List<RecordObject> objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals(4, objects.size());
                
        //Perform merging
        XRefBasedMerger merger = new XRefBasedMerger();
        merger.merge(model.listIndividualsOfClass(RecordObject.class, false), authA, false, false);
                
        //after
        objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals(2, objects.size());
        
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
        o1.addXRef(authA, "a");
        
        RecordObject.createOrGet(model, authA, "c");
        
        RecordObject.createOrGet(model, authA, "d");
        
        //before
        List<RecordObject> objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals(4, objects.size());
                
        //Perform merging
        XRefBasedMerger merger = new XRefBasedMerger();
        merger.merge(model.listIndividualsOfClass(RecordObject.class, false), authA, true, false);
                
        //after
        objects = model.listIndividualsOfClass(RecordObject.class, false);
        assertEquals(3, objects.size());
        
        for (RecordObject o : objects) {
            System.out.println(o.getURI());
            for (XRef xref : o.listXRefs()) {
                System.out.println("-> "+xref.getValue());
            }
        }
    }
    
}
