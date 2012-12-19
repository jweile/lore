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

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.GenomeModel;
import ca.on.mshri.lore.genome.PointMutation;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.ArrayList;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AlleleMergerTest extends TestCase {
    
    public AlleleMergerTest(String testName) {
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
        
        GenomeModel model = new GenomeModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        final Gene g1 = Gene.createOrGet(model, model.ENTREZ, "1");
        
        Allele a1 = Allele.createOrGet(model, model.ENTREZ, "1.1");
        a1.setGene(g1);
        a1.addMutation(PointMutation.createOrGet(model, a1, "CGA17AGT"));
        
        Allele a2 = Allele.createOrGet(model, model.ENTREZ, "1.2");
        a2.setGene(g1);
        a2.addMutation(PointMutation.createOrGet(model, a2, "R5S"));
        
        //before
        assertEquals(2,model.listIndividualsOfClass(Allele.class, false).size());
        assertEquals(2,model.listIndividualsOfClass(PointMutation.class, false).size());
        
        AlleleMerger am = new AlleleMerger();
        am.setParameter(am.selectionP, new ArrayList<Gene>(){{
            add(g1);
        }});
        am.setParameter(am.modelP, model);
        am.run();
        
        //after
        assertEquals(1,model.listIndividualsOfClass(Allele.class, false).size());
        assertEquals(1,model.listIndividualsOfClass(PointMutation.class, false).size());
        
    }
}
