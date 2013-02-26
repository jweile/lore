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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Structure {

    private Map<String,List<Aminoacid>> chains = new HashMap<String,List<Aminoacid>>();
    
    public Set<String> getChainIDs() {
        return Collections.unmodifiableSet(chains.keySet());
    }
    
    public List<Aminoacid> getChain(String chainId) {
        return chains.get(chainId);
    }
    
    public String getChainSequence(String chainID) {
        
        List<Aminoacid> polypeptide = chains.get(chainID);
        
        if (polypeptide == null) {
            return null;
        }
        
        StringBuilder b = new StringBuilder();
        
        for (Aminoacid aa : polypeptide) {
            b.append(aa.getSymbol());
        }
        
        return b.toString();
    }

    public void addChain(String id, List<Aminoacid> polypeptide) {
        chains.put(id, polypeptide);
    }
    
    
    public static class Atom {

        private int serial;
        
        private Vector3D position;
        
        private char element;
        
        private String name;

        public Atom(int serial, String name, Vector3D position, char element) {
            this.position = position;
            this.element = element;
            this.name = name;
        }

        public int getSerial() {
            return serial;
        }

        public Vector3D getPosition() {
            return position;
        }

        public char getElement() {
            return element;
        }

        public String getName() {
            return name;
        }
        
    }
    
    public static class Aminoacid {
    
        private char symbol;

        private List<Atom> atoms;

        private int sequencePos;

        public Aminoacid(char symbol, int sequencePos) {
            this.symbol = symbol;
            this.sequencePos = sequencePos;
        }

        public void addAtom(int serial, String name, Vector3D pos, String element) {
            if (atoms == null) {
                atoms = new ArrayList<Atom>();
            }
            Atom atom = new Atom(serial, name, pos, element.charAt(0));
            atoms.add(atom);
        }

        private Vector3D centroid;

        public List<Atom> getAtoms() {
            return atoms;
        }

        public int getSequencePos() {
            return sequencePos;
        }

        public char getSymbol() {
            return symbol;
        }



        public Vector3D getCentroid() {

            if (centroid == null) {

                Vector3D ca = null;

                List<Vector3D> positions = new ArrayList<Vector3D>();
                for (Atom atom: atoms) {

                    //remove backbone
                    if (atom.getName().equals("N") || 
                            atom.getName().equals("C") ||
                            atom.getName().equals("O") ||
                            atom.getName().startsWith("HA") ||
                            atom.getName().equals("H1") ||
                            atom.getName().equals("H")) {

                        continue;

                    } else if (atom.getName().equals("CA")) {

                        //save C alpha in case of glycin without hydrogen
                        ca = atom.getPosition();
                        continue;

                    } else {

                        positions.add(atom.getPosition());

                    }
                }

                if (positions.isEmpty()) {
                    positions.add(ca);
                }

                centroid =  Vector3D.centroid(positions);
            }

            return centroid;

        }

    }

}
