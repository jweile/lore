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
package ca.on.mshri.lore.molecules.util;

import ca.on.mshri.lore.molecules.util.PDBParser;
import ca.on.mshri.lore.molecules.util.Structure;
import ca.on.mshri.lore.molecules.util.Structure.Aminoacid;
import ca.on.mshri.lore.molecules.util.Vector3D;
import java.io.FileInputStream;
import java.io.InputStream;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PDBParserTest extends TestCase {
    
    public void test() throws Exception {
        
        InputStream in = new FileInputStream("src/test/resources/O00151-EXP-1x62_A.pdb");
        
        PDBParser parser = new PDBParser();
        Structure structure = parser.parse(in);
        
        for (String chainId : structure.getChainIDs()) {
            
            System.out.println("\nChain "+ chainId);
            System.out.println(structure.getChainSequence(chainId)+"\n");
            
            for (Aminoacid aa : structure.getChain(chainId)) {
                char symbol = aa.getSymbol();
                Vector3D centroid = aa.getCentroid();
                System.out.println(symbol+" "+centroid);
            }
        }
        
    }
    
}
