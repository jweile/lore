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
package ca.on.mshri.lore.psimi;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import de.jweile.yogiutil.MainWrapper;
import de.jweile.yogiutil.pipeline.ErrorHandlingThreadPool;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biopax.paxtools.model.BioPAXLevel;
import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;


/**
 * Reads a PSI-MI file, converts it to a BioPAX ontology and adds it 
 * to a Jena TDB database.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PsimiImporter {
    
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
                if (args.length < 2) {
                    throw new RuntimeException(
                            "Usage: java -jar lore-primi.jar <psimiFile> <tdbLocation>"
                            );
                }
                
                File inFile = new File(args[0]);
                if (!inFile.exists()) {
                    throw new RuntimeException(args[0]+" does not exist!");
                }
                
                File outFile = new File(args[1]);
                if (!outFile.exists()) {
                    Logger.getLogger(PsimiImporter.class.getName())
                            .log(Level.WARNING, "TDB location " + args[1] + 
                            " does not exist. Creating new database."
                            );
                }
                
                //start the import process
                new PsimiImporter().doImport(inFile, outFile);
            }
        };
        
        main.setLogFileName("lore-psimi.log");
        
        main.start(args);
    }
    
    
    /**
     * Performs the actual import functionality.
     * 
     * @param psimiFile Input PSI-MI file.
     * @param tdbLocation Location of TDB instance.
     */
    public void doImport(final File psimiFile, final File tdbLocation) {
        
        //prepare pipes
        final PipedInputStream pin;
        final PipedOutputStream pout;
        try {
            pin = new PipedInputStream();
            pout = new PipedOutputStream(pin);
        } catch (IOException e) {
            //shouldn't happen
            throw new RuntimeException(e);
        }
               
        //define reader thread
        Runnable readerThread = new Runnable() {

            public void run() {
                //prepare PSIMI to BioPAX converter
                PSIMIBioPAXConverter converter = new PSIMIBioPAXConverter(BioPAXLevel.L3);
                InputStream in = null;
                
                try {
                    
                    //run converter
                    in = new FileInputStream(psimiFile);
                    converter.convert(in, pout);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Error reading file "+psimiFile,e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            Logger.getLogger(PsimiImporter.class.getName())
                                    .log(Level.WARNING, "Unable to close stream.", ex);
                        }
                    }
                }
            }
        };
        
        //define writer thread
        Runnable writerThread = new Runnable() {

            @Override
            public void run() {
                
                Dataset tdbSet = null;
                OntModel model = null;
                
                try {
                    //open TDB instance
                    tdbSet = TDBFactory.createDataset(tdbLocation.getAbsolutePath());
                    //access ontology model
                    model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, tdbSet.getDefaultModel());
                    //read piped input stream into model
                    model.read(pin,null);
                    
                } finally {
                    if (model != null) {
                        model.commit();
                        model.close();
                    }
                    if (tdbSet != null) {
                        tdbSet.commit();
                        tdbSet.close();
                    }
                }
            }
            
        };
        
        //execute threads in error handling thread pool
        ErrorHandlingThreadPool pool = new ErrorHandlingThreadPool(2);
        pool.submit(readerThread);
        pool.submit(writerThread);
        pool.shutdown();
        
        //wait for threads
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(PsimiImporter.class.getName())
                    .log(Level.WARNING, "Master thread interrupted!", ex);
        }
        
        //check for errors
        pool.errors();
        
    }
    
    
}
