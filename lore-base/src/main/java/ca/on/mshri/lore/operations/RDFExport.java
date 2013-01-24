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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class RDFExport extends LoreOperation {

    public final Parameter<String> outfileP = Parameter.make("outfile",String.class, "lore.rdf.gz");
    
    public final Parameter<Boolean> compressionP = Parameter.make("compression", Boolean.class, true);
    
    /**
     * Supported values: "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3"
     */
    public final Parameter<String> formatP = Parameter.make("format", String.class, "RDF/XML");
    
    @Override
    public void run() {
        
        OutputStream out = null;
        
        try {
            
            String outfile = getParameterValue(outfileP);
            Logger.getLogger(RDFExport.class.getName())
                    .log(Level.INFO, "Exporting to "+outfile);
            
            out = new FileOutputStream(outfile);
            if (getParameterValue(compressionP)) {
                out = new GZIPOutputStream(out);
            }
            getModel().write(out, getParameterValue(formatP));
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to export model!",ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(RDFExport.class.getName())
                            .log(Level.SEVERE, "Unable to close output stream!", ex);
                }
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
