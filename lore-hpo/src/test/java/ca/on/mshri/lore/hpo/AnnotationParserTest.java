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
package ca.on.mshri.lore.hpo;

import ca.on.mshri.lore.hpo.model.Gene;
import ca.on.mshri.lore.hpo.model.HpoOntModel;
import ca.on.mshri.lore.hpo.model.Phenotype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AnnotationParserTest extends TestCase {
    
    public AnnotationParserTest(String testName) {
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
     * Test of parse method, of class AnnotationParser.
     */
    public void testParse() throws Exception {
        
        InputStream annoStream = null;
        try {
            
            annoStream = new FileInputStream("src/test/resources/phenotype_to_genes_test.txt");
            
            HpoOntModel model = new HpoOntModel(OntModelSpec.OWL_MEM, ModelFactory.createOntologyModel());
            model.read(new FileInputStream("src/main/resources/lore-hpo.owl"),null);

            AnnotationParser instance = new AnnotationParser(model);
            instance.parse(annoStream);
            
            Property asso = model.getProperty(HpoOntModel.HPO+"isAssociatedWith");
            
            List<Gene> genes = model.listGenes();
            for (Gene gene: genes) {
                System.out.println(gene.getName());
                NodeIterator it = gene.listPropertyValues(asso);
                while (it.hasNext()) {
                    Phenotype pheno = Phenotype.fromIndividual(it.next().as(Individual.class));
                    System.out.println("  -> "+pheno.getName());
                }
            }
            
        } finally {
            annoStream.close();
        }
    }
}
