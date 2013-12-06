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
package ca.on.mshri.lore.phenotype.hgmd;

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.io.File;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class HgmdImporterTest extends TestCase {
    
    public HgmdImporterTest(String testName) {
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
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_DL_MEM, ModelFactory.createDefaultModel());
        Property association = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        Property causes = model.getProperty(PhenotypeModel.URI+"#isCausallyAssociatedWith");
        
        HgmdImporter importer = new HgmdImporter();
        
        File inFile = new File("src/test/resources/HGMD_2011_snip.txt");
        
        importer.setParameter(importer.srcP, inFile.toURI().toURL());
        importer.setModel(model);
        
        importer.run();
        
        //print
        for (Gene gene : model.listIndividualsOfClass(Gene.class, false)) {
            System.out.println(gene.getXRefValue(model.ENTREZ)+" ("+gene.getXRefValue(model.HGNC)+")");
            for (Allele allele : gene.listAlleles()) {
                System.out.print(" -> "+allele.getXRefValue(model.HGMD));
                
                for (Mutation mut : allele.listMutations()) {
                    PointMutation pmut = PointMutation.fromIndividual(mut);
                    System.out.println(" ("+pmut.getFromAminoAcid()+pmut.getPosition()+pmut.getToAminoAcid()+")");
                }
                
//                StmtIterator pit = allele.listProperties(association);
//                while (pit.hasNext()) {
//                    Statement statement = pit.next();
//                    
//                    Phenotype disease = Phenotype.fromIndividual(statement.getObject().as(Individual.class));
//                    
//                    if (statement.getPredicate().equals(causes)) {
//                        System.out.println("   causes: "+disease.getLabel(null));
//                    } else if (statement.getPredicate().equals(association)) {
//                        System.out.println("   associated with: "+disease.getLabel(null));
//                    } else {
//                        throw new Exception("Wrong property type!");
//                    }
//                }
                
                NodeIterator it = allele.listPropertyValues(association);
                while (it.hasNext()) {
                    Phenotype disease = Phenotype.fromIndividual(it.next().as(Individual.class));
                    System.out.println("   --> "+disease.getLabel(null));
                }
                
                it = allele.listPropertyValues(causes);
                while (it.hasNext()) {
                    Phenotype disease = Phenotype.fromIndividual(it.next().as(Individual.class));
                    System.out.println("   --> (c) "+disease.getLabel(null));
                }
            }
        }
        
        System.out.flush();
        
    }
}
