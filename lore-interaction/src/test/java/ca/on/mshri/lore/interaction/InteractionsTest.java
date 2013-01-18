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
package ca.on.mshri.lore.interaction;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.Publication;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.NucleotideFeature;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class InteractionsTest extends TestCase {
    
    
    public void test() {
                
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, 
                ModelFactory.createDefaultModel());
                
        final Gene g1 = Gene.createOrGet(model, model.ENTREZ, "0123");
        final Gene g2 = Gene.createOrGet(model, model.ENTREZ, "1234");
        
        Experiment exp = Experiment.createOrGet(model, "testPub01");
        exp.setPublication(Publication.createOrGet(model, model.PUBMED, "01234"));
        
        OntClass iType = model.getOntClass(InteractionModel.URI+"#SyntheticLethality");
        
        GeneticInteraction interaction = GeneticInteraction.createOrGet(model, exp, iType, g1, g2);
        System.out.println(interaction.getURI());
        
        List<NucleotideFeature> interactors = interaction.listParticipants();
        assertEquals(2,interactors.size());
        assertTrue(interactors.contains(g1));
        assertTrue(interactors.contains(g2));
        
    }
    
}
