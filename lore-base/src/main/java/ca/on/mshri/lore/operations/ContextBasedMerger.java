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

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.operations.util.Connection;
import ca.on.mshri.lore.operations.util.RefListParameter;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.LazyInitMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Merges individuals if they have the same neighbours within a given restriction.
 * For example, if two Y2H experiment nodes report the exact same interactions and are 
 * published in the same journal it is very likely that they both are duplicates of the 
 * same experiment. These cases can be identified and merged with this algorithm.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ContextBasedMerger extends LoreOperation {
    
    /**
     * A selection of individuals which will be considered for merging.
     */
    public final RefListParameter<Individual> selectionP = new RefListParameter<Individual>("selection", Individual.class);
    
    /**
     * a set of classes whose members must be the same for each potential merge group.
     * E.g. restriction=Gene,Mutation for a selection of alleles means that all 
     * genes and mutations that are connected to alleles must be the same for the allele
     * individuals to be considered redundant.
     */
    public final RefListParameter<OntClass> contextRestrictionsP = new RefListParameter<OntClass>("contextRestrictions", OntClass.class);
    /**
     * Perform the merging operation.
     * @param selection The a selection of objects on which the algorithm will run.
     * @param contextRestrictions only connected objects of these types will be considered as
     * part of the context of a given type.
     */
    @Override
    public void run() {
        
        
        Logger.getLogger(XRefBasedMerger.class.getName())
                .log(Level.INFO, "Context-based merger: Indexing...");
        
        //get parameters
        List<Individual> selection = getParameterValue(selectionP).resolve(getModel());
        OntClass[] contextRestrictions = ((List<OntClass>)getParameterValue(contextRestrictionsP)
                .resolve(getModel())).toArray(new OntClass[0]);
        
        LazyInitMap<String,Set<Individual>> index = new LazyInitMap<String, Set<Individual>>(HashSet.class);
        
        CliProgressBar pro = new CliProgressBar(selection.size());
        
        for (Individual ind : selection ) {
            
            Set<Connection> context = Connection.findConnections(ind);
            List<String> connectionKeys = new ArrayList<String>();
            for (Connection conn : context) {
                if (inRestriction(conn.getNeighbour(), contextRestrictions)) {
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
            
            pro.next();
        }
        
        Merger merger = new Merger();
        merger.setParameter(merger.mergeSetsP, index.values());
        merger.run();
        
    }
    
    private boolean inRestriction(RDFNode node, OntClass[] restriction) {
        if (!node.canAs(Individual.class) 
                || node.canAs(OntClass.class)) {//apparently classes are also individuals O_o
            return false;
        }
        Individual i = node.as(Individual.class);
        boolean in = false;
        for (OntClass clazz : restriction) {
            in |= LoreModel.hasClass(i, clazz);
        }
        return in;
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
