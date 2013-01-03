/*
 * Copyright (C) 2013 Department of Molecular Genetics, University of Toronto
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

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ResourceReferencesTest extends TestCase {
    
    private LoreModel model;
    
    public ResourceReferencesTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        RecordObject.createOrGet(model, model.PUBMED, "1");
        RecordObject.createOrGet(model, model.PUBMED, "2");
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testUriList() {
        
        String desc = "urn:lore:RecordObject#PubMed:1,urn:lore:RecordObject#PubMed:1";
        
        ResourceReferences<RecordObject> rl = new ResourceReferences<RecordObject>(desc, RecordObject.class);
        List<RecordObject> list = rl.resolve(model);
        
        assertEquals(2, list.size());
        
    }
    
    public void testSparql() {
        
        String desc = "SELECT ?res WHERE {?res <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://llama.mshri.on.ca/lore-base.owl#RecordObject>}";
        
        ResourceReferences<RecordObject> rl = new ResourceReferences<RecordObject>(desc, RecordObject.class);
        List<RecordObject> list = rl.resolve(model);
        
        assertEquals(2, list.size());
    }
    
    public void testMalformed() {
        
        String desc = "SELECT ?res WHERE {?res <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
        
        boolean failed = false;
        try {
            ResourceReferences<RecordObject> rl = new ResourceReferences<RecordObject>(desc, RecordObject.class);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            System.out.flush();
            failed = true;
        }
        
        assertTrue(failed);
    }
}
