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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.base.LoreModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ShortestPath {
    
    /**
     * Find the shortest path between the from node to the target node. Edges are
     * defined by the given path pattern
     * @param from originating node
     * @param to target node
     * @param pathPattern e.g. "^ia:hasParticipant/ia:hasParticipant" for interaction partners
     * @return the last node in the path. Can be used to trace back the path and to 
     * find out the length of the path.
     */
    public PathNode find(Individual from, Individual to, String pathPattern) {
        Collection<Individual> targets = new ArrayList<Individual>();
        targets.add(to);
        return find(from, targets, pathPattern);
    }
    
    /**
     * Find the shortest path between the from node to any of the target nodes. Edges are
     * defined by the given path pattern
     * @param from originating node
     * @param targets target nodes
     * @param pathPattern e.g. "^ia:hasParticipant/ia:hasParticipant" for interaction partners
     * @return the last node in the path. Can be used to trace back the path and to 
     * find out the length of the path.
     */
    public PathNode find(Individual from, Collection<? extends Individual> targets, String pathPattern) {
        
        //discovered nodes yet to examine
        PrioritySet open = new PrioritySet();
        //already examined nodes
        Set<PathNode> closed = new HashSet<PathNode>();
        
        //get a shortcut to the model
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, from.getModel());
        
        //discover the first node
        open.offer(new PathNode(null,from));
        
        //as long as there are still nodes to examine, we keep going
        while (!open.isEmpty()) {
            
            //retrive the next node with the shortest distance
            PathNode curr = open.poll();
            
            //iterate over neighbours of that node
            for (Individual neighbour : findNeighbours(model, curr.getValue(), pathPattern) ) {
                //wrap neighbour into pathnode object
                PathNode next = new PathNode(curr,neighbour);
                //check if neighbour is target node, if so, we're done
                if (targets.contains(neighbour)) {
                    return next;
                //otherwise, if the node is not already known, add it to the list to explore
                } else if (!closed.contains(next)) {
                    open.offer(next);
                }
            }
            
            //mark current node as closed
            closed.add(curr);
        }
        
        //if the target node was not previously found, there is no path
        return null;
        
    }
    
    /**
     * Convenience method for retrieving neighbouring nodes according to the edge pattern.
     */
    private Set<Individual> findNeighbours(LoreModel model, Individual in, String pattern) {
        
        Set<Individual> set = new HashSet<Individual>();
        String uri = "<"+in.getURI()+">";
        String qString = new StringBuilder()
                .append("SELECT ?neighbour WHERE {")
                .append(uri).append(' ').append(pattern).append(" ?neighbour. ")
                .append(" FILTER(?neighbour != ").append(uri).append(")}").toString();
        
//        System.out.println(qString);
        
        QueryExecution qexec = QueryExecutionFactory
                .create(qString, model);
        try {
            ResultSet r = qexec.execSelect();
            while (r.hasNext()) {
                QuerySolution sol = r.next();
                
                Individual i = sol.getResource("neighbour")
                        .as(Individual.class);
                
                set.add(i);
            }
        } finally {
            qexec.close();
        }
        
        
        return set;
    }
//
//    private Set<Individual> findNeighbours(Individual in, List<Property> properties) {
//        return _findNeighbours(in, properties, new HashSet<Individual>());
//    }
//    private Set<Individual> _findNeighbours(Individual in, List<Property> properties, Set<Individual> closed) {
//
//        closed.add(in);
//
//        List<Property> rest = new ArrayList<Property>(properties);
//        rest.remove(0);
//
//        Set<Individual> results = new HashSet<Individual>();
//
//        NodeIterator nit = in.listPropertyValues(properties.get(0));
//        while (nit.hasNext()) {
//            Individual neighbour = nit.next().as(Individual.class);
//
//            if (!closed.contains(neighbour)) {
//                if (rest.isEmpty()) {
//                    results.add(neighbour);
//                } else {
//                    results.addAll(_findNeighbours(neighbour, rest, closed));
//                }
//            }
//        }
//        return results;
//    }
//    
        
    
    public static class PathNode implements Comparable<PathNode> {
        
        private int distance;
        private PathNode pre;
        private Individual value;

        public PathNode(PathNode pre, Individual value) {
            this.pre = pre;
            this.value = value;
            distance = (pre != null) ? pre.getDistance()+1 : 0;
        }

        public int getDistance() {
            return distance;
        }

        public PathNode getPredecessor() {
            return pre;
        }

        public Individual getValue() {
            return value;
        }
        
        

        public int compareTo(PathNode t) {
            if (t.value.equals(this.value)) {
                return 0;
            } else if (this.distance < t.distance) {
                return -1;
            } else if (this.distance == t.distance) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (this.value != null ? this.value.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PathNode other = (PathNode) obj;
            if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
                return false;
            }
            return true;
        }
        
        
        
    }
    
    static class PrioritySet extends PriorityQueue<PathNode> {
        @Override
        public boolean offer(PathNode e) {
            if (contains(e)) {
                return false; 
            } else {
                return super.offer(e);
            }
        }
    }
    
    
    
}
