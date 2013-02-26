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
package ca.on.mshri.lore.genome.util;

import ca.on.mshri.lore.genome.PointMutation;
import de.jweile.yogiutil.LazyInitMap;
import de.jweile.yogiutil.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class GeneticCode {
    
    private static GeneticCode instance;
    
    private LazyInitMap<String, List<String>> aa2nc = new LazyInitMap<String, List<String>>(ArrayList.class);
    
    private Map<String, String> nc2aa = new HashMap<String, String>();
    private Map<String, String> triple2single = new HashMap<String, String>();
    private Map<String, String> single2triple = new HashMap<String, String>();

    private GeneticCode() {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(PointMutation.class.getClassLoader().getResourceAsStream("codontable.txt")));
            String line;
            int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                String[] cols = line.split("\t");
                String triple = cols[0].toUpperCase();
                String single = cols[1].toUpperCase();
                String[] codons = cols[2].split("\\|");
                triple2single.put(triple, single);
                single2triple.put(single, triple);
                for (String codon : codons) {
                    aa2nc.getOrCreate(single).add(codon);
                    nc2aa.put(codon, single);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read codon table.", ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(PointMutation.class.getName()).log(Level.WARNING, "Failed to close stream.", ex);
                }
            }
        }
    }

    public static GeneticCode getInstance() {
        if (instance == null) {
            instance = new GeneticCode();
        }
        return instance;
    }

    public int explainingSNPs(String aa1, String aa2) {
        List<Pair<String>> candidates = new ArrayList<Pair<String>>();
        for (String codon1 : aa2nc.get(aa1)) {
            for (String codon2 : aa2nc.get(aa2)) {
                int mismatches = 0;
                for (int i = 0; i < 3; i++) {
                    if (codon1.charAt(i) != codon2.charAt(i)) {
                        mismatches++;
                    }
                }
                if (mismatches < 2) {
                    candidates.add(new Pair(codon1, codon2));
                }
            }
        }
        return candidates.size();
    }

    public Set<String> getAminoacids() {
        return aa2nc.keySet();
    }

    public String translate(String codon) {
        return nc2aa.get(codon.toUpperCase());
    }

    public String toTriple(String single) {
        return single2triple.get(single.toUpperCase());
    }
    
    public boolean isValidAA(String aa) {
        return triple2single.containsKey(aa.toUpperCase());
    }
    
}
