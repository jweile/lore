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

import ca.on.mshri.lore.operations.LoreOperation;
import de.jweile.yogiutil.CliIndeterminateProgress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public abstract class TabDelimParser extends LoreOperation {
    
    /**
     * parse a tab-delimited input stream
     * @param in the input stream to parse.
     * @param skip how many lines to skip at the beginning of the file
     * @param minCols 
     */
    protected void parseTabDelim(InputStream in, int skip, int minCols) {
        
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                //skip header
                if (lnum < skip) {
                    progress.next("Parsing");
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                //check line consistency
                if (cols.length < minCols) {
                    Logger.getLogger(TabDelimParser.class.getName()).log(Level.WARNING, "Invalid line: "+lnum);
                    progress.next("Parsing");
                    continue;
                }
                
                processRow(cols);
                
                progress.next("Parsing");
            }
            
            progress.done();
            
        } catch (IOException ex) {
            throw new RuntimeException("Error reading stream", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(TabDelimParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
        
    }

    protected abstract void processRow(String[] cols);
}
