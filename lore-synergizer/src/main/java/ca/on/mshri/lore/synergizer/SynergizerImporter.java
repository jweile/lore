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
package ca.on.mshri.lore.synergizer;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import com.hp.hpl.jena.ontology.OntModelSpec;
import de.jweile.yogiutil.pipeline.Pipeline;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class SynergizerImporter extends LoreOperation {

    public final Parameter<String> userP = Parameter.make("user", String.class);
    
    public final Parameter<String> pwdP = Parameter.make("pwd", String.class);
    
    @Override
    public void run() {
        
        String user = getParameterValue(userP);
        String pwd = getParameterValue(pwdP);
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_DL_MEM, getModel());
        
        Pipeline pipeline = new Pipeline();
        pipeline.addNode(new DBReader(user,pwd));
        pipeline.addNode(new TripleStoreWriter(model));
        pipeline.start();
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
