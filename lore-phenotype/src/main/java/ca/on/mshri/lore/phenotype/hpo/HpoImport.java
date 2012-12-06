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
package ca.on.mshri.lore.phenotype.hpo;

import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;
import de.jweile.yogiutil.MainWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public final class HpoImport {
    
    /**
     * Main method.
     * 
     * @param args CLI arguments: A String identifying the input PSI-MI file
     * and a String describing the path to the TDB instance.
     */
    public static void main(String[] args) {
        
        MainWrapper main = new MainWrapper() {

            @Override
            public void run(String[] args) {
                
                //check arguments and complain if necessary
                if (args.length < 1) {
                    throw new RuntimeException(
                            "Usage: java -jar lore-phenotype.jar <tdbLocation>"
                            );
                }
                                
                File tdbFile = new File(args[1]);
                if (!tdbFile.exists()) {
                    Logger.getLogger(HpoImport.class.getName())
                            .log(Level.WARNING, "TDB location " + args[1] + 
                            " does not exist. Creating new database."
                            );
                }
                
                //start the import process
                new HpoImport().run(tdbFile);
            }
        };
        
        main.setLogFileName("lore-phenotype.log");
        
        main.start(args);
    }
    
    
    /**
     * Performs the actual data import into the TDB store.
     * 
     * @param tdbFile 
     */
    public void run(File tdbFile) {
        
        //setup tdb
        Dataset tdbSet = null;
        try {
            
            tdbSet = TDBFactory.createDataset(tdbFile.getAbsolutePath());
            
            PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, tdbSet.getDefaultModel());
            
            //parse HPO OBO

            ResourceBundle hpoProps = ResourceBundle.getBundle("HPO");

            OboParser oboParser = new OboParser(model);
            InputStream oboStream = null;
            try {

                oboStream = new URL(hpoProps.getString("obo")).openStream();
                oboParser.parse(oboStream);

            } catch (IOException ex) {
                throw new RuntimeException("Unable to parse HPO OBO file!", ex);
            } finally {
                if (oboStream != null) {
                    try {
                        oboStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(HpoImport.class.getName())
                                .log(Level.WARNING, "Unable to close stream!", ex);
                    }
                }
            }

            //parse annotations

            AnnotationParser annoParser = new AnnotationParser(model);
            InputStream annoStream = null;
            try {

                annoStream = new URL(hpoProps.getString("phenotype_to_genes")).openStream();
                annoParser.parse(annoStream);

            } catch (IOException ex) {
                throw new RuntimeException("Unable to parse annotation file!", ex);
            } finally {
                if (annoStream != null) {
                    try {
                        annoStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(HpoImport.class.getName())
                                .log(Level.WARNING, "Unable to close stream!", ex);
                    }
                }
            }
        
        
        } finally {
            tdbSet.close();
        }
    }

    
}
