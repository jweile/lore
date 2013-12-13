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
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliProgressBar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Links record objects originating from two different sets if they share an 
 * XRef of a given Authority. 
 * Can also be run in <code>allMustMatch</code> mode, where record objects are linked only if 
 * they share <i>all</i> XRefs from that Authority.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class XRefBasedLinker extends LoreOperation {
    
    /**
     *  The a selection of objects on which the algorithm will run.
     */
    public final RefListParameter<RecordObject> fromSetP = new RefListParameter<RecordObject>("fromSet", RecordObject.class);
    
    /**
     *  The a selection of objects on which the algorithm will run.
     */
    public final RefListParameter<RecordObject> toSetP = new RefListParameter<RecordObject>("toSet", RecordObject.class);
    
    /**
     * The XRef authority based on which equality will be determined.
     */
    public final RefListParameter<Authority> authorityP = new RefListParameter<Authority>("authority", Authority.class);
    
    /**
     * The property type used to link the entities.
     */
    public final RefListParameter<Property> propertyP = new RefListParameter<Property>("property", Property.class);
    
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
     * Perform the linking operation.
     */
    @Override
    public void run() {
        
        Logger.getLogger(XRefBasedLinker.class.getName())
                .log(Level.INFO, "XRef-based linker: Indexing...");
        
        List<RecordObject> fromSet = getParameterValue(fromSetP).resolve(getModel());
        List<RecordObject> toSet = getParameterValue(toSetP).resolve(getModel());
        Authority authority = ((ResourceReferences<Authority>)getParameterValue(authorityP)).resolve(getModel()).get(0);
        Property property = ((ResourceReferences<Property>)getParameterValue(propertyP)).resolve(getModel()).get(0);
        boolean allMustMatch = getParameterValue(allMustMatchP);
        boolean uniqueKeys = getParameterValue(uniqueKeysP);
        
        Map<String,Set<Individual>> fromIndex = new HashMap<String, Set<Individual>>();
        Map<String,Set<Individual>> toIndex = new HashMap<String, Set<Individual>>();
        
        CliProgressBar pro = new CliProgressBar(fromSet.size()+toSet.size());
        
        //index the from set
        //for all objects in the "from" selection
        for (RecordObject o : fromSet) {
            
            //get applicable keys
            List<String> keys = new ArrayList<String>();
            for (XRef xref : o.listXRefs()) {
                if (xref.getAuthority().equals(authority)) {
                    keys.add(xref.getValue());
                }
            }
            
            if (keys.isEmpty()) {
                pro.next();
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
                Set<Individual> set = fromIndex.get(key);
                if (set != null) {
                    unionSet.addAll(set);
                }
            }
            //add new member
            unionSet.add(o);
            
            //register with all keys
            for (String key : keys) {
                fromIndex.put(key,unionSet);
            }
            
            pro.next();
        }
        
        //index the "to" set
        for (RecordObject o : toSet) {
            
            //get applicable keys
            List<String> keys = new ArrayList<String>();
            for (XRef xref : o.listXRefs()) {
                if (xref.getAuthority().equals(authority)) {
                    keys.add(xref.getValue());
                }
            }
            
            if (keys.isEmpty()) {
                pro.next();
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
                Set<Individual> set = toIndex.get(key);
                if (set != null) {
                    unionSet.addAll(set);
                }
            }
            //add new member
            unionSet.add(o);
            
            //register with all keys
            for (String key : keys) {
                toIndex.put(key,unionSet);
            }
            
            pro.next();
            
        }
        
        
        Logger.getLogger(XRefBasedLinker.class.getName())
                .log(Level.INFO, "XRef-based merger: Linking...");
        pro = new CliProgressBar(fromIndex.keySet().size());
        
        int links = 0;
        
        //do the linking
        for (String key : fromIndex.keySet()) {
            
            Set<Individual> fromObjects = fromIndex.get(key);
            Set<Individual> toObjects = toIndex.get(key);
            
            if (fromObjects == null || fromObjects.isEmpty() || 
                    toObjects == null || toObjects.isEmpty()) {
                pro.next();
                continue;
            }
            
            for (Individual fromO : fromObjects) {
                for (Individual toO : toObjects) {
                    
                    if (!fromO.hasProperty(property, toO)) {
                        fromO.addProperty(property, toO);
                        links++;
                    }
                    
                }
            }
            
            pro.next();
        }
        
        
        Logger.getLogger(XRefBasedLinker.class.getName())
                .log(Level.INFO, "Successfully linked "+links+" entity pairs!");
        
        //merge
//        Merger merger = new Merger();
//        merger.setParameter(merger.mergeSetsP, index.values());
//        merger.run();
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

    @Override
    public boolean requiresReasoner() {
        return false;
    }
        
}
