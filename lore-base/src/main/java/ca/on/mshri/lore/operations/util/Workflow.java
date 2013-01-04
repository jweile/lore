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
import ca.on.mshri.lore.operations.LoreOperation;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.ArrayList;
import java.util.List;

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
        this.model = model;
    }
    
    public void run() {
        
        //create new graph
        model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        //run operations
        for (LoreOperation op : ops) {
            op.setModel(model);
            op.run();
        }
        
    }
    
}