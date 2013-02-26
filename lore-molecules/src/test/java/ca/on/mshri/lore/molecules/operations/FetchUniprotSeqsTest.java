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
package ca.on.mshri.lore.molecules.operations;

import ca.on.mshri.lore.molecules.MoleculesModel;
import ca.on.mshri.lore.molecules.Protein;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class FetchUniprotSeqsTest extends TestCase {
    
    public FetchUniprotSeqsTest(String testName) {
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
        
        Protein protein = Protein.createOrGet(model, model.UNIPROT, "O00151");
        
        FetchUniprotSeqs op = new FetchUniprotSeqs();
        op.setModel(model);
        op.setParameter(op.selectionP, op.selectionP.validate(protein.getURI()));
        op.run();
        
        String sequence = protein.getSequence();
        assertNotNull(sequence);
        assertTrue(sequence.length() > 0);
        
        System.out.println(sequence);
        
    }
}
