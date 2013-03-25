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
package ca.on.mshri.lore.molecules;

import ca.on.mshri.lore.genome.Gene;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ProteinTest extends TestCase {
    
    private MoleculesModel m;
    private Protein p;
    private Gene g;
    
    public ProteinTest(String testName) {
        super(testName);
        m = new MoleculesModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        p = Protein.createOrGet(m, m.UNIPROT, "A12345");
        g = Gene.createOrGet(m, m.ENTREZ, "1234");
        p.setEncodingGene(g);
    }
    
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testGetEncodingGene() throws Exception {
        assertNotNull(p.getEncodingGene());
    }
    
    public void testListEncodedProteins() throws Exception {
        List<Protein> l = Protein.listEncodedProteins(g);
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals(p, l.get(0));
    }
}
