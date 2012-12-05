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

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.HashSet;
import java.util.Set;

/**
 * a class representing a connection to an adjacent node in an RDF graph.
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Connection {
    /**
     * The predicate
     */
    private Property predicate;
    /**
     * The actual neighbouring node
     */
    private RDFNode neighbour;
    /**
     * Whether the connection is outgoing or incoming.
     */
    private boolean outgoing;

    /**
     * Convenience constructor.
     */
    public Connection(Property predicate, RDFNode neighbour, boolean outgoing) {
        this.predicate = predicate;
        this.neighbour = neighbour;
        this.outgoing = outgoing;
    }

    /**
     * @return the neighbouring node.
     */
    public RDFNode getNeighbour() {
        return neighbour;
    }

    /**
     * @return whether the connection is outgoing or incoming
     */
    public boolean isOutgoing() {
        return outgoing;
    }

    /**
     * @return the predicate of the connection.
     */
    public Property getPredicate() {
        return predicate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Connection other = (Connection) obj;
        if (this.predicate != other.predicate && (this.predicate == null || !this.predicate.equals(other.predicate))) {
            return false;
        }
        if (this.neighbour != other.neighbour && (this.neighbour == null || !this.neighbour.equals(other.neighbour))) {
            return false;
        }
        if (this.outgoing != other.outgoing) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.predicate != null ? this.predicate.hashCode() : 0);
        hash = 29 * hash + (this.neighbour != null ? this.neighbour.hashCode() : 0);
        hash = 29 * hash + (this.outgoing ? 1 : 0);
        return hash;
    }
    
    /**
     * Finds all connections of a resource in the RDF graph.
     * 
     * @param r 
     * the resource
     * 
     * @return 
     * A set of <code>r</code>'s connections.
     */
    public static Set<Connection> findConnections(Resource r) {
        
        //init set
        Set<Connection> neighbours = new HashSet<Connection>();
        
        //find outgoing neighbours via API call
        StmtIterator it = r.listProperties();
        while (it.hasNext()) {
            Statement s = it.nextStatement();
            neighbours.add(new Connection(s.getPredicate(),s.getObject(), true));
        }
        
        //find incoming neighbours via SPARQL call. (unfortunately, there is no
        //API function available for this)
        QueryExecution qexec = QueryExecutionFactory
                .create("SELECT ?s ?p WHERE {?s ?p <"+r.getURI()+">}",r.getModel());
        try {
            ResultSet result = qexec.execSelect();
            while (result.hasNext()) {
                QuerySolution sol = result.next();
                RDFNode subject = sol.get("s");
                Property pred = sol.get("p").as(Property.class);
                neighbours.add(new Connection(pred, subject, false));
            }
        } finally {
            qexec.close();
        }
        
        return neighbours;
    }
    
}
