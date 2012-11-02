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

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class BaseApiTest extends TestCase {
    
    public BaseApiTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void test() throws Exception {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority authA = model.getOrCreateAuthority("A");
        //test if duplications get ignored
        model.getOrCreateAuthority("A");
        Authority authB = model.getOrCreateAuthority("B");
        
        RecordObject r1 = model.createRecordObject(authA, "1");
        RecordObject r2 = model.createRecordObject(authA, "2");
        
        //test if duplicated xrefs get automatically merged
        XRef foo1 = r1.addXRef(authB, "foo");
        XRef foo2 = r2.addXRef(authB, "foo");
        
        assertEquals(foo1, foo2);
        
        assertEquals(2, model.listRecordObjects().size());
        assertEquals(2, model.listAuthorities().size());
        assertEquals(3, model.listXRefs().size());
        
    }
    
}
