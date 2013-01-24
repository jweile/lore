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
import ca.on.mshri.lore.base.XRef;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ImportTranslationTableTest extends TestCase {
    
    public ImportTranslationTableTest(String testName) {
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
        
        File file = new File("src/test/resources/entrez2uniprot.txt");
        
        ImportTranslationTable itt = new ImportTranslationTable();
        itt.setModel(model);
        itt.setParameter(itt.srcP, file.toURI().toURL());
        itt.setParameter(itt.authoritesP, "EntrezGene,UNIPROT");
        itt.setParameter(itt.classP, itt.classP.validate(RecordObject.CLASS_URI));
        
        itt.run();
        
        for(RecordObject obj : model.listIndividualsOfClass(RecordObject.class, false)) {
            System.out.println(obj);
            for (XRef xref : obj.listXRefs()) {
                System.out.println("  - "+xref.getAuthority().getAuthorityId()+" : "+ xref.getValue());
            }
        }
        
    }
    
    
}
