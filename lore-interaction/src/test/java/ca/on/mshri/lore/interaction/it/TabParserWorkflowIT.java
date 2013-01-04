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
package ca.on.mshri.lore.interaction.it;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.interaction.Interaction;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.operations.util.Workflow;
import ca.on.mshri.lore.operations.util.WorkflowParser;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TabParserWorkflowIT extends TestCase {
    
    public TabParserWorkflowIT(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void test() throws Exception {
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_DL_MEM, ModelFactory.createDefaultModel());
        
        InputStream in = new FileInputStream("src/test/resources/test_workflow");
        
        WorkflowParser wp = new WorkflowParser();
        Workflow workflow = wp.parse(in);
        
        workflow.setModel(model);
        workflow.run();
        
        List<Interaction> interactions = model.listIndividualsOfClass(Interaction.class, false);
        
        for (Interaction interaction : interactions) {
            List<? extends RecordObject> participants = interaction.listParticipants();
            
            System.out.println(interaction.getOntClass());
            
            for (RecordObject o : participants) {
                System.out.print("  -> ");
                System.out.println(getXRefValue(o, model.ENTREZ));
            }
            
        }
        
    }
    
    
    private String getXRefValue(RecordObject o, Authority a) {
        for (XRef x : o.listXRefs()) {
            if (x.getAuthority().equals(a)) {
                return x.getValue();
            }
        }
        return null;
    }
}
