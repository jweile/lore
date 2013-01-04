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
package ca.on.mshri.lore.cli;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.operations.util.Workflow;
import ca.on.mshri.lore.operations.util.WorkflowParser;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;
import de.jweile.yogiutil.MainWrapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Main extends MainWrapper {
    
    public static void main(String[] args) {
        
        Main m = new Main();
        m.setLogFileName("lore.log");
        m.start(args);
        
    }
    
    @Override
    public void run(String[] args) {

        if (args.length < 2) {
            usageAndDie();
        }

        String workflowPath = args[1];
        File workflowFile = new File(workflowPath);
        if (!workflowFile.exists()) {
            throw new RuntimeException(workflowPath+" does not exist!");
        }
        
        String tdbLoc = args[0];
        File tdbFile = new File(tdbLoc);

        Dataset tdbSet = null;
        InputStream in = null;
        try {
            
            in = new FileInputStream(workflowPath);
            WorkflowParser wp = new WorkflowParser();
            Workflow workflow = wp.parse(in);
            
            tdbSet = TDBFactory.createDataset(tdbFile.getAbsolutePath());
            LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, tdbSet.getDefaultModel());
            
            workflow.setModel(model);
            workflow.run();
            
            model.commit();
            
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read workflow file: "+
                    workflowFile.getAbsolutePath(), ex);
        } finally {
            if (tdbSet != null) {
                tdbSet.close();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName())
                            .log(Level.WARNING, "Cannot close stream", ex);
                }
            }
        }
        
        
    }
    
    private void usageAndDie() {
        System.err.println("Usage: java -jar lore-cli.jar <DbLocation> <Workflow>");
        System.exit(1);
    }
    
}
