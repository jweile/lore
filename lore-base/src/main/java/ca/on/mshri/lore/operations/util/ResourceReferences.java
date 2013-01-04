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

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ResourceReferences<T extends Resource> {
        
    private List<URI> uriList = null;
    
    private Query query = null;
    
    private Class<T> type;

    public ResourceReferences(String descriptor, Class<T> type) {
        preProcess(descriptor);
        this.type = type;
    }
    
    public ResourceReferences(Collection<? extends T> elements, Class<T> type) {
        uriList = new ArrayList<URI>();
        for (T element : elements) {
            uriList.add(URI.create(element.getURI()));
        }
        this.type = type;
    }
    
    public List<T> resolve(Model model) {
        
        List<T> out = new ArrayList<T>();
        
        if (uriList != null) {
            for (URI uri : uriList) {
                Resource resource = model.getResource(uri.toString());
                out.add(attemptCast(resource));
            }
        } else { //if (query != null) always true, because of preProcess()
            QueryExecution qExec = QueryExecutionFactory.create(query, model);
            ResultSet result = qExec.execSelect();
            while (result.hasNext()) {
                QuerySolution sol = result.next();
                Resource resource = sol.getResource(sol.varNames().next());
                out.add(attemptCast(resource));
            }
        }
        
        return out;
    }

    private void preProcess(String descriptor) {
        
        //try reading the 
        try {
            List<URI> uris = new ArrayList<URI>();
            for (String s : descriptor.split(",")) {
                uris.add(URI.create(s.trim()));
            }
            uriList = uris;
        } catch (IllegalArgumentException e) {
            //ignore
        }
        
        
        String msg = null;
        if (uriList == null) {
            try {
                query = QueryFactory.create(descriptor);
            } catch (QueryException e) {
                msg = e.getMessage();
            }
        }
        
        if (query == null && uriList == null) {
            throw new RuntimeException("ResourceList descriptor must be either "
                    + "comma-separated list of URIs or a SPARQL query.\n"+msg);
        }
        
    }

    private T attemptCast(Resource resource) {
        
        if (resource.canAs(Individual.class)) {
            Individual ind = resource.as(Individual.class);
            try {
                Method method = type.getMethod("fromIndividual", Individual.class);
                return (T) method.invoke(null, ind);
            } catch (Exception e) {
                //ignore
            }
        }
        
        if (resource.canAs(type)) {
            return resource.as(type);
        }
        
        throw new RuntimeException(resource.getURI()+" cannot be cast as "+type);
    }

    @Override
    public String toString() {
        if (uriList != null) {
            return uriList.toString();
        } else {
            return query.toString();
        }
    }
    
    
}
