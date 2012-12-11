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
package ca.on.mshri.lore.genome;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PointMutationTest extends TestCase {
    private GenomeModel model;
    private Gene gene;
    private Allele allele;
    
    public PointMutationTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        model = new GenomeModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        gene = Gene.createOrGet(model, model.ENTREZ, "0123");
        allele = Allele.createOrGet(model, model.ENTREZ, "0123.1");
        allele.setGene(gene);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void test() throws Exception {
        
        PointMutation mut = PointMutation.createOrGet(model, allele, "CGC12CAC");
        PointMutation mut2 = PointMutation.createOrGet(model, allele, "R4H");
        PointMutation mut3 = PointMutation.createOrGet(model, allele, "ASN134LYS");
        
        System.out.println(mut.getFromAminoAcid()+" "+mut.getPosition()+" "+ mut.getToAminoAcid());
        System.out.println(mut2.getFromAminoAcid()+" "+mut2.getPosition()+" "+ mut2.getToAminoAcid());
        System.out.println(mut.getURI());
        System.out.flush();
        
        assertEquals(mut, mut2);
        
        
        
    }
    
    public void testNonsense() throws Exception {
        
        boolean caught = false;
        try {
            PointMutation mut = PointMutation.createOrGet(model, allele, "CGblahC12CAC");
        } catch (IllegalArgumentException ex) {
            caught = true;
        }
        assertTrue("Did not complain about illegal arguments",caught);
        
        
        caught = false;
        try {
            PointMutation mut = PointMutation.createOrGet(model, allele, "MIF2BLA");
        } catch (IllegalArgumentException ex) {
            caught = true;
        }
        assertTrue("Did not complain about illegal arguments",caught);
        
        
        caught = false;
        try {
            PointMutation mut = PointMutation.createOrGet(model, allele, "X12B");
        } catch (IllegalArgumentException ex) {
            caught = true;
        }
        assertTrue("Did not complain about illegal arguments",caught);
        
    }
}
