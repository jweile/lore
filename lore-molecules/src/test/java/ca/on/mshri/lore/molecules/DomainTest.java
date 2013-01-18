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

import ca.on.mshri.lore.base.Authority;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class DomainTest extends TestCase {
    
    public DomainTest(String testName) {
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
        MoleculesModel model = new MoleculesModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority pfam = Authority.createOrGet(model, "PFAM");
        
        int start = 1, end = 10;
        
        Protein protein = Protein.createOrGet(model, model.ENTREZ, "1");
        ProteinDomain domain = ProteinDomain.createOrGet(model, pfam, "1.1");
        domain.setProtein(protein);
        domain.setStart(start);
        domain.setEnd(end);
        
        assertEquals(start, domain.getStart().intValue());
        assertEquals(end, domain.getEnd().intValue());
        
        
    }
}
