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
package ca.on.mshri.lore.edgotype;

import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.Counts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class EdgotypeCorrespondenceTest extends TestCase {
    
    public EdgotypeCorrespondenceTest(String testName) {
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
     * Test of diseaseSimilarities method, of class EdgotypeCorrespondence.
     */
    public void testDiseaseSimilarities() {
        
        System.out.println("diseaseSimilarities");
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, 
                ModelFactory.createDefaultModel());
        
        Property asso = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        
        Set<Allele> alleles = new HashSet<Allele>();
        Allele a1 = Allele.createOrGet(model, model.HGMD, "1");
        alleles.add(a1);
        Allele a2 = Allele.createOrGet(model, model.HGMD, "2");
        alleles.add(a2);
        Allele a3 = Allele.createOrGet(model, model.HGMD, "3");
        alleles.add(a3);
        
        Phenotype p1 = Phenotype.createOrGet(model, model.OMIM, "1");
        Phenotype p2 = Phenotype.createOrGet(model, model.OMIM, "2");
        Phenotype p3 = Phenotype.createOrGet(model, model.OMIM, "3");
        Phenotype p4 = Phenotype.createOrGet(model, model.OMIM, "4");
        Phenotype p5 = Phenotype.createOrGet(model, model.OMIM, "5");
        
        a1.addProperty(asso, p1);
        a1.addProperty(asso, p2);
        a2.addProperty(asso, p2);
        a2.addProperty(asso, p3);
        a3.addProperty(asso, p4);
        a3.addProperty(asso, p5);
        
        EdgotypeCorrespondence instance = new EdgotypeCorrespondence();
        instance.setModel(model);
        instance.init();
        
        double[] result = instance.diseaseSimilarities(alleles);
        String signature = signature(result);
        
        double[] expResult = new double[]{0.0, 0.0, 1.0/3.0};
        String expSignature = signature(expResult);
        
        assertEquals(expSignature, signature);
    }
    
    /**
     * Test of hasZero method, of class EdgotypeCorrespondence.
     */
    public void testHasZero() {
        System.out.println("hasZero");
        
        EdgotypeCorrespondence instance = new EdgotypeCorrespondence();
        
        double[] sims = new double[]{1.0, 1.0, 2.0};
        boolean result = instance.hasZero(sims);
        assertEquals(false, result);
        
        sims = new double[]{1.0, 1.0, 0.0};
        result = instance.hasZero(sims);
        assertEquals(true, result);
        
    }

    /**
     * Test of allZero method, of class EdgotypeCorrespondence.
     */
    public void testAllZero() {
        System.out.println("allZero");
        EdgotypeCorrespondence instance = new EdgotypeCorrespondence();
        
        double[] sims = new double[]{0.0, 1.0, 2.0};
        boolean result = instance.allZero(sims);
        assertEquals(false, result);
        
        sims = new double[]{0.0, 0.0, 0.0};
        result = instance.allZero(sims);
        assertEquals(true, result);
    }

    /**
     * Test of getMutationInfo method, of class EdgotypeCorrespondence.
     */
    public void testGetMutationInfo() {
        System.out.println("getMutationInfo");
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, 
                ModelFactory.createDefaultModel());
        
        Allele allele = Allele.createOrGet(model, model.HGMD, "1");
        allele.addMutation(PointMutation.createOrGet(model, allele, "G12L"));
        
        EdgotypeCorrespondence instance = new EdgotypeCorrespondence();
        instance.setModel(model);
        instance.init();
        
        String expResult = "GLY12LEU";
        String result = instance.getMutationInfo(allele);
        assertEquals(expResult, result);
    }

    /**
     * Test of getOtherProtein method, of class EdgotypeCorrespondence.
     */
    public void testGetOtherProtein() {
        System.out.println("getOtherProtein");
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, 
                ModelFactory.createDefaultModel());
        
        Experiment exp = Experiment.createOrGet(model, "Test");
        
        Protein p1 = Protein.createOrGet(model, model.ENTREZ, "1");
        Gene g1 = Gene.createOrGet(model, model.ENTREZ, "1");
        p1.setEncodingGene(g1);
        Protein p2 = Protein.createOrGet(model, model.ENTREZ, "2");
        Gene g2 = Gene.createOrGet(model, model.ENTREZ, "2");
        p2.setEncodingGene(g2);
        
        PhysicalInteraction ia = PhysicalInteraction.createOrGet(model, exp, 
                model.getOntClass(PhysicalInteraction.CLASS_URI), p1, p2);
        
        EdgotypeCorrespondence instance = new EdgotypeCorrespondence();
        instance.setModel(model);
        instance.init();
        
        String expResult = "2";
        String result = instance.getOtherProtein(ia, g1);
        assertEquals(expResult, result);
    }

    private String signature(double[] ds) {
        Counts<Double> counts = new Counts<Double>();
        for (double d : ds) {
            counts.count(d);
        }
        return counts.getSignature();
    }
}
