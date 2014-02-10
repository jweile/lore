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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.operations.Configure;
import ca.on.mshri.lore.operations.LoreOperation;
import com.hp.hpl.jena.ontology.OntModelSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Workflow {
    
    private List<LoreOperation> ops = new ArrayList<LoreOperation>();
    
    private LoreModel model;
    
    public void add(LoreOperation op) {
        ops.add(op);
    }

    public LoreModel getModel() {
        return model;
    }

    public void setModel(LoreModel model) {
        if (this.model != null) {
            throw new RuntimeException("Attempted to overwrite workflow model.");
        }
        this.model = model;
    }
    
    public void run() {
        
        Summary summary = new Summary();
        
        //run operations
        for (LoreOperation op : ops) {
            if (op.requiresReasoner()) {
                op.setModel(new LoreModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, model));
            } else {
                op.setModel(model);
            }
            op.run();
            
            String commitProp = System.getProperties().getProperty(Configure.COMMIT_KEY);
            if (commitProp == null || Boolean.parseBoolean(commitProp)) {
                if (model.supportsTransactions()) {
                    Logger.getLogger(Workflow.class.getName())
                            .log(Level.INFO, "Committing model to database");
                    model.commit();
                } else {
                    Logger.getLogger(Workflow.class.getName())
                            .log(Level.WARNING, "Cannot commit: Model does not support transactions!");
                }
            }
            
            String summaryProp = System.getProperties().getProperty(Configure.SUMMARIES_KEY);
            if (summaryProp == null || Boolean.parseBoolean(summaryProp)) {
                summary.printSummary(model);
            }
        }
        
    }
    
}
