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
package ca.on.mshri.lore.molecules.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class StructureCache {
    
    /**
     * singleton.
     */
    private static StructureCache instance;
    
    /**
     * cache index by URL
     */
    private Map<URL, Structure> index = new HashMap<URL, Structure>();

    /**
     * private constructor for singleton pattern
     */
    private StructureCache() {}
    
    /**
     * singleton.
     * @return 
     */
    public static StructureCache getInstance() {
        if (instance == null) {
            instance = new StructureCache();
        }
        return instance;
    }
    
    public Structure getStructureForURL(URL url) {
        Structure s = index.get(url);
        if (s == null) {
            s = parsePDB(url);
            index.put(url, s);
        }
        return s;
    }

    private Structure parsePDB(URL url) {
        
        InputStream in = null;
        
        try {
            in = url.openStream();
            return new PDBParser().parse(in);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot open URL: "+url, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(StructureCache.class.getName())
                            .log(Level.WARNING, "Unable to close stream!", ex);
                }
            }
        }
    }
    
}
