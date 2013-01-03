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
package ca.on.mshri.lore.operations;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RefListParameter<T extends Resource> extends Parameter<ResourceReferences>{

    private Class<T> elementType;
    
    public RefListParameter(String id, Class<T> elementType) {
        super(id, ResourceReferences.class);
        this.elementType = elementType;
    }
    

    @Override
    public ResourceReferences<T> validate(Object value) {
        
        if (!(value instanceof String)) {
            throw new RuntimeException(getId()+" must be comma-separated list of URIs or a SPARQL query");
        }
        
        try {
            return new ResourceReferences<T>((String)value, elementType);
        } catch (Exception e) {
            throw new RuntimeException(getId()+" must be comma-separated list of URIs or a SPARQL query", e);
        }
        
    }
    
    
}
