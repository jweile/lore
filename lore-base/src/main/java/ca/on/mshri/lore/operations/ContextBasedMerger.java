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

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import de.jweile.yogiutil.LazyInitMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges individuals if they have the same neighbours within a given restriction.
 * For example, if two Y2H experiment nodes report the exact same interactions and are 
 * published in the same journal it is very likely that they both are duplicates of the 
 * same experiment. These cases can be identified and merged with this algorithm.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ContextBasedMerger {
    
    /**
     * Perform the merging operation.
     * @param selection The a selection of objects on which the algorithm will run.
     * @param contextRestriction only connected objects of this type will be considered as
     * part of the context of a given type.
     */
    public void merge(Collection<? extends Individual> selection, OntClass contextRestriction) {
        
        LazyInitMap<String,Set<Individual>> index = new LazyInitMap<String, Set<Individual>>(HashSet.class);
        
        for (Individual ind : selection ) {
            
            Set<Connection> context = Connection.findConnections(ind);
            List<String> connectionKeys = new ArrayList<String>();
            for (Connection conn : context) {
                if (conn.getNeighbour().canAs(Individual.class) && 
                        conn.getNeighbour().as(Individual.class)
                        .hasOntClass(contextRestriction, false)) {
                    connectionKeys.add(conn.getPredicate().getURI() + "-"
                            + conn.getNeighbour().asResource().getURI());
                }
            }
            
            Collections.sort(connectionKeys);
            StringBuilder b = new StringBuilder();
            b.append(ind.getOntClass(true).getURI()).append("(");
            for (String key : connectionKeys) {
                b.append(key).append(",");
            }
            b.deleteCharAt(b.length()-1).append(")");
            
            String key = b.toString();
            
            index.getOrCreate(key).add(ind);
            
        }
        
        new Merger().merge(index.values());
        
    }
    
}
