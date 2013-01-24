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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ExportImportTest extends TestCase {
    
    public ExportImportTest(String testName) {
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
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        RecordObject o = RecordObject.createOrGet(model, model.PUBMED, "test");
        
        File testFile = File.createTempFile("rdftest", null);
        
        RDFExport exporter = new RDFExport();
        exporter.setModel(model);
        exporter.setParameter(exporter.outfileP, testFile.getAbsolutePath());
        
        exporter.run();
        
        
        LoreModel model2 = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        RDFImport importer = new RDFImport();
        importer.setModel(model2);
        importer.setParameter(importer.srcP, testFile.toURI().toURL());
        
        importer.run();
        
        List<RecordObject> ros = model2.listIndividualsOfClass(RecordObject.class, true);
        
        assertEquals(1, ros.size());
        assertEquals(o.getURI(), ros.get(0).getURI());
    }
}
