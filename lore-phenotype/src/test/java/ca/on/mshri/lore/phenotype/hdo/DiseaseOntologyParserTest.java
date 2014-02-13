/*
 * Copyright (C) 2014 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.phenotype.hdo;

import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.File;
import java.net.URL;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class DiseaseOntologyParserTest extends TestCase {
    
    public DiseaseOntologyParserTest(String testName) {
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
        
        URL url = new File("src/test/resources/HumanDO.obo").toURI().toURL();
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        Property isa = model.getProperty(PhenotypeModel.URI+"#is_a");
        
        DiseaseOntologyParser parser = new DiseaseOntologyParser();
        parser.setModel(model);
        parser.setParameter(parser.srcP, url);
        parser.run();
        
        Phenotype pheno = Phenotype.createOrGet(model, model.DO, "5810");
        
        while (pheno != null) {
            String omim = pheno.getXRefValue(model.OMIM);
            if (omim != null) {
                System.out.print(omim);
                System.out.print(" - ");
            }
            System.out.println(pheno.getLabel(null));
            Resource parent = pheno.getPropertyResourceValue(isa);
            pheno = parent == null ? null : Phenotype.fromIndividual(parent.as(Individual.class));
            
        }
        
        
        
    }
}
