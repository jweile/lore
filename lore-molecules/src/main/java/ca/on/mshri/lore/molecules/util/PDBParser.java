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

import ca.on.mshri.lore.molecules.util.Structure.Aminoacid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PDBParser {
    
//COLUMNS        DATA  TYPE    FIELD        DEFINITION
//-------------------------------------------------------------------------------------
// 1 -  6        Record name   "ATOM  "
// 7 - 11        Integer       serial       Atom  serial number.
//13 - 16        Atom          name         Atom name.
//17             Character     altLoc       Alternate location indicator.
//18 - 20        Residue name  resName      Residue name.
//22             Character     chainID      Chain identifier.
//23 - 26        Integer       resSeq       Residue sequence number.
//27             AChar         iCode        Code for insertion of residues.
//31 - 38        Real(8.3)     x            Orthogonal coordinates for X in Angstroms.
//39 - 46        Real(8.3)     y            Orthogonal coordinates for Y in Angstroms.
//47 - 54        Real(8.3)     z            Orthogonal coordinates for Z in Angstroms.
//55 - 60        Real(6.2)     occupancy    Occupancy.
//61 - 66        Real(6.2)     tempFactor   Temperature  factor.
//77 - 78        LString(2)    element      Element symbol, right-justified.
//79 - 80        LString(2)    charge       Charge  on the atom.

//         1         2         3         4         5         6         7         8
//12345678901234567890123456789012345678901234567890123456789012345678901234567890
//ATOM     32  N  AARG A  -3      11.281  86.699  94.383  0.50 35.88           N  
    
    public Structure parse(InputStream in) {
        
        Structure structure = new Structure();
        
        List<Aminoacid> currChain = null;
        int lastResSeq = -1;
        Aminoacid currAA = null;
        String chainID = null;
        String lastChainId = "";
        
        AminoacidProps aaProps = new AminoacidProps();
        
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(in));
            
            String line; int lnum = 0;
            while ((line = r.readLine())!= null) {
                
                if (line.startsWith("ATOM")) {
                    
                    if (line.length() < 78) {
                        Logger.getLogger(PDBParser.class.getName())
                                .log(Level.WARNING, "Invalid line: "+lnum);
                        continue;
                    }
              
                    int serial = Integer.parseInt(line.substring(6, 11).trim());
                    String name = line.substring(12,16).trim();
                    String altLoc = line.substring(16,17).trim();
                    String resName = line.substring(17,20).trim();
                    chainID = line.substring(21,22).trim();
                    int resSeq = Integer.parseInt(line.substring(22,26).trim());
                    String iCode = line.substring(26,27).trim();
                    double x = Double.parseDouble(line.substring(30,38).trim());
                    double y = Double.parseDouble(line.substring(38,46).trim());
                    double z = Double.parseDouble(line.substring(46,54).trim());
                    double occupancy = Double.parseDouble(line.substring(54,60).trim());
                    double tempFactor = Double.parseDouble(line.substring(60,66).trim());
                    String element = line.substring(76,78).trim();
                    String charge = line.length() == 80 ? line.substring(78,80).trim() : "";
                    
                    Vector3D pos = new Vector3D(x,y,z);
                    char resSymbol = aaProps.getSingle(resName);
                    
                    if (!chainID.equals(lastChainId)) {
                        if (currChain != null) {
                            structure.addChain(lastChainId,currChain);
                        }
                        lastChainId = chainID;
                        currChain = new ArrayList<Aminoacid>();
                    }
                    
                    if (resSeq > lastResSeq) {
                        if (currAA != null) {
                            currChain.add(currAA);
                        }
                        currAA = new Aminoacid(resSymbol, resSeq);
                        lastResSeq = resSeq;
                    }
                    
                    currAA.addAtom(serial, name, pos, element);
                    
                }
                
            }
            //commit last aminoacid to chain and last chain to structure
            if (currAA != null) {
                currChain.add(currAA);
            }       
            if (currChain != null) {
                structure.addChain(chainID, currChain);
            }
                        
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read stream", ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(PDBParser.class.getName())
                            .log(Level.WARNING, "Unable to close stream", ex);
                }
            }
        }
        
        return structure;
        
    }

    
}
