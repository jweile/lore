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

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class URLParameter extends Parameter<URL> {

    public URLParameter(String id) {
        super(id, URL.class);
    }

    @Override
    public URL validate(Object value) {
        if (value instanceof URL) {
            return (URL)value;
        } else if (value instanceof String) {
            try {
                return new URL((String)value);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(getId()+" must be valid URL.",ex);
            }
        } else {
            throw new RuntimeException(getId()+" must be valid URL.");
        }
    }
    
    
    
}
