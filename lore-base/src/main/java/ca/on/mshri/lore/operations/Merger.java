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
import ca.on.mshri.lore.operations.util.Connection;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntResource;
import de.jweile.yogiutil.CliProgressBar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Merges sets of redundant individuals.
 * 
 * Internal use only. Not compatible with workflow parser, because of 
 * complex parameter type.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Merger extends LoreOperation {
    
    /**
     * a collection of sets of individuals. the members of each set will be merged.
     */
    public final Parameter<Collection> mergeSetsP = Parameter.make("mergeSets", Collection.class);
    
    /**
     * Performs the merging operation.
     * @param mergeSets a collection of sets of individuals. The members of each
     * set are considered mutually redundant and will be merged.
     */
    @Override
    public void run() {
        
        Collection<Set<Individual>> mergeSets = getParameterValue(mergeSetsP);
        
        int inCount = 0;
        
        //##Do the actual merging##
        Logger.getLogger(Merger.class.getName())
                            .log(Level.INFO, "Merging...");
        
        CliProgressBar pro = new CliProgressBar(mergeSets.size());
        
        //for each set of xref objects indexed under the same keys...
        for (Set<? extends Individual> mergeSet : mergeSets) {
            
            inCount += mergeSet.size();
            
            //if there's only one object in the set, we don't have to do anything
            if (mergeSet.size() <= 1) {
                pro.next();
                continue;
            }
            
            //otherwise...
            
            //a variable for the xref instance we will keep in the end
            Individual toKeep = null;
            //a bag for all the instances we will delete in the end
            List<Individual> toDelete = new ArrayList<Individual>();
            
            Set<Connection> connections = new HashSet<Connection>();
            
            //for each individual in the set of redundancies
            for (Individual ind : mergeSet) {
                
                //find connections in the ontology graph and store them
                connections.addAll(Connection.findConnections(ind));
                
                //save the first individual as the one we'll keep, the rest, we'll delete
                if (toKeep == null) {
                    toKeep = ind;
                } else {
                    toDelete.add(ind);
                }
            }
            
            //reconnect the neighbours to the keeper
            for (Connection connection : connections) {
                if (connection.isOutgoing()) {
                    toKeep.addProperty(connection.getPredicate(), connection.getNeighbour());
                } else {
                    OntResource nRes = connection.getNeighbour().as(OntResource.class);
                    nRes.addProperty(connection.getPredicate(), toKeep);
                }
            }
            
            //delete the rest
            for (Individual ind : toDelete) {
                ind.remove();
            }
            
            pro.next();
        }
        
        Logger.getLogger(Merger.class.getName())
                .log(Level.INFO, "Consolidated "+inCount+" individuals to "+
                mergeSets.size()+" units.");
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
