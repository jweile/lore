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
import ca.on.mshri.lore.molecules.Structure3D.SeqMap;
import ca.on.mshri.lore.molecules.util.Structure;
import ca.on.mshri.lore.molecules.util.Structure.Aminoacid;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Structure3DTest extends TestCase {
    
    public Structure3DTest(String testName) {
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
        Authority pdbAuth = Authority.createOrGet(model, "PDB");
        
        Protein protein = Protein.createOrGet(model, model.UNIPROT, "O00151");
        
        Structure3D struc = Structure3D.createOrGet(model, pdbAuth, "1x62");
        Structure3D.addStructureToObject(struc,protein);
        URL source = new File("src/test/resources/O00151-EXP-1x62_A.pdb").toURI().toURL();
        struc.setSource(source);
        
        for (Structure3D s3d : Structure3D.listStructuresOfObject(protein)) {
            Structure structure = struc.getStructureObject();
            for (String chainId : structure.getChainIDs()) {
                List<Aminoacid> chain = structure.getChain(chainId);
                System.out.println(chain.get(0).getCentroid());
            }
        }
        
    }
    
    public void testSeqMap() throws Exception {
        SeqMap m = new SeqMap();
        int[] test = new int[] {-1,-1,0,1,2,3,-1};
        m.put("test",test);
        
        String ser = m.serialize();
        SeqMap m2 = SeqMap.deserialize(ser);
        
        assertTrue(Arrays.equals(m.get("test"), m2.get("test")));
    }
}
