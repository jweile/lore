/*
 *  Copyright (C) 2011 The Roth Lab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.on.mshri.lore.synergizer;

import ca.on.mshri.lore.base.LoreModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Populates triplestore with individuals of a certain type according to the 
 * contents of a given file.
 * @author Jochen Weile <jochenweile@gmail.com>
 */
class IndividualPopulator<T extends Individual> {

    /**
     * The class to which the individuals will belong.
     */
    private Class<T> clazz;
    
    /**
     * The ontology model from the triplestore.
     */
    private OntModel model;
    
    /**
     * Constructor.
     * 
     * @param model
     * The ontology model from the triplestore.
     * 
     * @param clazz 
     * The class to which the individuals will belong.
     */
    public IndividualPopulator(OntModel model, Class<T> clazz) {
        this.clazz = clazz;
        this.model = model;
    }

    /**
     * Populates the ontology model with individuals from the given file
     * 
     * @param file 
     * a tab-delim file containing the individual ids and names
     * 
     * @return 
     * a map with the individuals by ID.
     */
    Map<Integer, T> run(String file) {
        
        Map<Integer,T> map = new HashMap<Integer, T>();
        
        InputStream in = IndividualPopulator.class.getClassLoader().getResourceAsStream(file+".tsv");
        if (in == null) {
            try {
               in = new FileInputStream(new File(new File(new File(new File("src"),"main"),"resources"),file+".tsv"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unable to find resource file",e);
            }
        }
        
//        String typeName = clazz.getURI().split("#")[1];
        
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        try {
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                if (line.length() == 0) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                int id;
                
                try {
                    id = Integer.parseInt(cols[0]);
                } catch (NumberFormatException e) {
                    Logger.getLogger(IndividualPopulator.class.getName())
                        .log(Level.WARNING, "Formatting error in file "+
                            file
                            +".tsv in line "+lnum, e);
                    continue;
                }
                
                
//                Individual ind = model.createIndividual("urn:lore:"+typeName+"#"+cols[1],clazz);
                try {
                    
//                    Class<T> type = null;
                    Method cog = clazz.getMethod("createOrGet", LoreModel.class, String.class);
                    T ind = (T)cog.invoke(null, model, cols[1]);
                    
                    map.put(id, ind);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                
                
            }
            
            return map;
            
        } catch (IOException ex) {
            throw new RuntimeException("Error trying to read file "+file+".tsv");
        } finally {
            try {
                r.close();
            } catch (IOException ex) {
                Logger.getLogger(IndividualPopulator.class.getName())
                        .log(Level.WARNING, "Unable to close input stream!", ex);
            }
        }
    }
    
}
