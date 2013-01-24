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

import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.URLParameter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RDFImport extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    public final Parameter<Boolean> compressionP = Parameter.make("compression", Boolean.class, true);
    
//    /**
//     * Supported values: "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3"
//     */
//    public final Parameter<String> formatP = Parameter.make("format", String.class, "RDF/XML");
    
    @Override
    public void run() {
        
        InputStream in = null;
        
        URL src = getParameterValue(srcP);
        if (src == null) {
            throw new IllegalArgumentException("Paramter src required!");
        }
        
        Logger.getLogger(RDFExport.class.getName())
                .log(Level.INFO, "Importing fron "+src);
        
        try {
            
            in = src.openStream();
            if (getParameterValue(compressionP)) {
                in = new GZIPInputStream(in);
            }
            getModel().read(in, null);
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to import model!",ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(RDFExport.class.getName())
                            .log(Level.WARNING, "Unable to close stream!", ex);
                }
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
