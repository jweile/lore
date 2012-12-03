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
import ca.on.mshri.lore.base.XRef;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class GenomeAPITest extends TestCase {
    
    public GenomeAPITest(String testName) {
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
        GenomeModel model = new GenomeModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority entrez = Authority.createOrGet(model,"entrez");
        
        Gene g1 = Gene.createOrGet(model, entrez, "7001");
        Gene g2 = Gene.createOrGet(model, entrez, "7001");
        assertEquals(g1,g2);
        
        for (XRef xref : g1.listXRefs()) {
            System.out.println(xref);
            System.out.println(" -> "+xref.getAuthority());
            System.out.println(" -> "+xref.getValue());
            
        }
    }
}
