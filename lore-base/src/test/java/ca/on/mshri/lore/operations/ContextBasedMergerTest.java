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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ContextBasedMergerTest extends TestCase {
    
    public ContextBasedMergerTest(String testName) {
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

    /**
     * Test of merge method, of class ContextBasedMerger.
     */
    public void testMerge() {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority authA = Authority.createOrGet(model,"A");
        Property obsIn = model.getProperty(LoreModel.URI+"#observedIn");
        
        Experiment exp1 = Experiment.createOrGet(model, "exp1");
        Experiment exp2 = Experiment.createOrGet(model, "exp2");
        Experiment exp3 = Experiment.createOrGet(model, "exp3");
        
        RecordObject o1 = RecordObject.createOrGet(model, authA, "a");
        o1.addProperty(obsIn, exp1);
        o1.addProperty(obsIn, exp2);
        
        RecordObject o2 = RecordObject.createOrGet(model, authA, "b");
        o2.addProperty(obsIn, exp1);
        o2.addProperty(obsIn, exp2);
        
        RecordObject o3 = RecordObject.createOrGet(model, authA, "c");
        o2.addProperty(obsIn, exp3);
        RecordObject o4 = RecordObject.createOrGet(model, authA, "d");
        o2.addProperty(obsIn, exp3);
        
        assertEquals(4, model.listIndividualsOfClass(RecordObject.class, false).size());
        assertEquals(3, model.listIndividualsOfClass(Experiment.class, false).size());
        
        ContextBasedMerger instance = new ContextBasedMerger();
        
        
        
        ResourceReferences<Individual> selection = instance.selectionP.validate(
                "SELECT ?exp WHERE {?exp <"+RDF.type.getURI()+"> "
                + "<"+Experiment.CLASS_URI+">}");
        ResourceReferences<OntClass> contextRestriction = instance.contextRestrictionsP
                .validate(RecordObject.CLASS_URI);
        
        instance.setParameter(instance.selectionP, selection);
        instance.setParameter(instance.contextRestrictionsP, contextRestriction);
        instance.setModel(model);
        instance.run();
        
        assertEquals(4, model.listIndividualsOfClass(RecordObject.class, false).size());
        assertEquals(2, model.listIndividualsOfClass(Experiment.class, false).size());
        
    }
}
