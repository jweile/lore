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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AminoacidProps {

    public AminoacidProps() {
        parse();
    }

    private static final int name = 0;
    private static final int triple = 1;
    private static final int single = 2;
    private static final int polarity = 3;
    private static final int charge = 4;
    private static final int hydropathy = 5;
    
    private Map<Character, String[]> bySingle = new HashMap<Character, String[]>();
    private Map<String, String[]> byTriple = new HashMap<String, String[]>();
    
    
    private void parse() {
        
        InputStream in = AminoacidProps.class.getClassLoader().getResourceAsStream("aa_props.tsv");
        
        try {
            
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            String line;
            
            while ((line = r.readLine()) != null) {
                
                String[] cols = line.split("\t");
                
                bySingle.put(cols[single].charAt(0), cols);
                byTriple.put(cols[triple].toUpperCase(), cols);
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Error reading aminoacid properties");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Cannot load aminoacid properties.");
                }
            }
        }
    }
    
    public String getName(char s) {
        return bySingle.get(s)[name];
    }
    
    public String getName(String t) {
        return byTriple.get(t.toUpperCase())[name];
    }
    
    public String getTriple(char s) {
        return bySingle.get(s)[triple];
    }
    
    public char getSingle(String t) {
        return byTriple.get(t.toUpperCase())[single].charAt(0);
    }
    
    public String getPolarity(char s) {
        return bySingle.get(s)[polarity];
    }
    
    public String getPolarity(String t) {
        return byTriple.get(t.toUpperCase())[polarity];
    }
    
    public String getCharge(char s) {
        return bySingle.get(s)[charge];
    }
    
    public String getCharge(String t) {
        return byTriple.get(t.toUpperCase())[charge];
    }
    
    public double getHydropathy(char s) {
        return Double.parseDouble(bySingle.get(s)[hydropathy]);
    }
    
    public double getHydropathy(String t) {
        return Double.parseDouble(byTriple.get(t.toUpperCase())[name]);
    }
}
