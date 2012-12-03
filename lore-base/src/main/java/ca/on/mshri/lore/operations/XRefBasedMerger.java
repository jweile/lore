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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.XRef;
import com.hp.hpl.jena.ontology.Individual;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRefBasedMerger {
    
    public void merge(Collection<RecordObject> selection, Authority authority, boolean allMustMatch, boolean uniqueKeys) {
        
        Map<String,Set<Individual>> index = new HashMap<String, Set<Individual>>();
        
        //for all objects in the selection
        for (RecordObject o : selection) {
            
            //get applicable keys
            List<String> keys = new ArrayList<String>();
            for (XRef xref : o.listXRefs()) {
                if (xref.getAuthority().equals(authority)) {
                    keys.add(xref.getValue());
                }
            }
            
            if (keys.isEmpty()) {
                continue;
            }
            
            if (uniqueKeys && keys.size() > 1) {
                throw new InconsistencyException("Keys of type "+authority+
                        " are not unique! Object "+
                        o+"is has multiple keys: "+keys);
            }
            
            //if all keys must match, combine them to one super-key
            if (allMustMatch) {
                String key = cons(";",keys);
                keys.clear();
                keys.add(key);
            }
            
            //collect all previously registered sets and combine them
            Set<Individual> unionSet = new HashSet<Individual>();
            for (String key : keys) {
                Set<Individual> set = index.get(key);
                if (set != null) {
                    unionSet.addAll(set);
                }
            }
            //add new member
            unionSet.add(o);
            
            //register with all keys
            for (String key : keys) {
                index.put(key,unionSet);
            }
            
            //merge
            Merger merger = new Merger();
            merger.merge(index.values());
            
        }
    }

    private String cons(String delim, List<String> ss) {
        Collections.sort(ss);
        StringBuilder b = new StringBuilder();
        for (String s : ss) {
            b.append(s).append(delim);
        }
        b.deleteCharAt(b.length()-1);
        return b.toString();
    }
        
}
