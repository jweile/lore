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
package ca.on.mshri.lore.phenotype.omim;

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OmimParserTest extends TestCase {
    
    private PhenotypeModel model;
            
    public OmimParserTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        model = new PhenotypeModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
//    public void testMim2Gene() throws Exception {
//        
//        InputStream in = new FileInputStream("src/test/resources/mim2gene.txt");
//        
//        OmimImporter parser = new OmimImporter();
//        parser.parseMim2Gene(model, in);
//    }
//    
//    
//    public void testMorbidMap() throws Exception {
//        
//        InputStream in = new FileInputStream("src/test/resources/morbidmap");
//        
//        OmimImporter parser = new OmimImporter();
//        parser.parseMorbidMap(model, in);
//    }
    
    
    public void testFulltextParser() throws Exception {
        
        InputStream in = new FileInputStream("src/test/resources/omim.txt");
        
        OmimFulltextParser parser = new OmimFulltextParser();
        parser.parse(model, in);
        parser.linkAlleles(model);
        
        for (Gene gene: model.listIndividualsOfClass(Gene.class, false)) {
            System.out.println(gene.getLabel(null));
            for (Allele allele : gene.listAlleles()) {
                System.out.println("  * "+allele.getURI().substring(16));
                for (Mutation mut : allele.listMutations()) {
                    if (mut.getOntClass(true).getURI().equals(PointMutation.CLASS_URI)) {
                        PointMutation pmut = PointMutation.fromIndividual(mut);
                        System.out.println("    -> "+pmut.getFromAminoAcid()+pmut.getPosition()+pmut.getToAminoAcid());
                    }
                }
            }
        }
    }
//    
//    public void testAll() throws Exception {
//                
//        OmimImporter parser = new OmimImporter();
//        parser.parse(model);
//        
//    }
}
