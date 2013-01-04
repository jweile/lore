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

import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.Individual;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges two or more record objects from a given collection if they share an 
 * XRef of a given Authority. 
 * Can also be run in <code>allMustMatch</code> mode, where record objects are merged only if 
 * they share <i>all</i> XRefs from that Authority.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRefBasedMerger extends LoreOperation {
    
    /**
     *  The a selection of objects on which the algorithm will run.
     */
    public final RefListParameter<RecordObject> selectionP = new RefListParameter<RecordObject>("selection", RecordObject.class);
    
    /**
     * The XRef authority based on which equality will be determined.
     */
    public final RefListParameter<Authority> authorityP = new RefListParameter<Authority>("authority", Authority.class);
    
    /**
     * If true, all XRefs from the given authority must match between
     * two objects for them to be considered equal.
     */
    public final Parameter<Boolean> allMustMatchP = Parameter.make("allMustMatch", Boolean.class, false);
    
    /**
     * If true, the occurrence of objects with multiple xrefs of the 
     * chosen authority will trigger an exception.
     */
    public final Parameter<Boolean> uniqueKeysP = Parameter.make("uniqueKeys", Boolean.class, false);
    
    /**
     * Perform the merger operation.
     */
    @Override
    public void run() {
        
        List<RecordObject> selection = getParameterValue(selectionP).resolve(getModel());
        Authority authority = ((ResourceReferences<Authority>)getParameterValue(authorityP)).resolve(getModel()).get(0);
        boolean allMustMatch = getParameterValue(allMustMatchP);
        boolean uniqueKeys = getParameterValue(uniqueKeysP);
        
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
            merger.setParameter(merger.mergeSetsP, index.values());
            merger.run();
            
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
