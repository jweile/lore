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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.interaction.tabparser.TabParser;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.operations.util.ShortestPath.PathNode;
import ca.on.mshri.lore.operations.util.ShortestPath.PrioritySet;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ShortestPathTest extends TestCase {
    
    public ShortestPathTest(String testName) {
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
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, 
                ModelFactory.createDefaultModel());
                
        File in = new File("src/test/resources/CCSB_HI1_updated.tsv");
        
        TabParser parser = new TabParser();
//        parser.setParameter(parser.modelP, model);
        parser.setParameter(parser.srcP, parser.srcP.validate(in.toURI().toURL()));
        parser.setParameter(parser.interactorAuthP, model.ENTREZ.getAuthorityId());
        parser.setParameter(parser.experimentP, "CCSB-HI1.1");
        parser.setParameter(parser.interactionTypeP, parser.interactionTypeP.validate(PhysicalInteraction.CLASS_URI));
        parser.setParameter(parser.interactorTypeP, parser.interactorTypeP.validate(Protein.CLASS_URI));
        parser.setParameter(parser.headerP, true);
        
        parser.setModel(model);
        parser.run();
        
        List<Protein> proteins = model.listIndividualsOfClass(Protein.class, false);
        
        ShortestPath sp = new ShortestPath();
        String ia = "<"+InteractionModel.URI+"#hasParticipant>";
        PathNode path = sp.find(proteins.get(0), proteins.get(4), "^"+ia+"/"+ia);
        System.out.println("Shortest path has distance "+path.getDistance());
        
        
    }
    
    public void testPrioritySet() throws Exception {
        
        LoreModel model = new LoreModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        Authority a1 = Authority.createOrGet(model, "1");
        PathNode p1 = new PathNode(null, a1);
        Authority a2 = Authority.createOrGet(model, "2");
        PathNode p2 = new PathNode(p1, a2);
        
        PrioritySet ps = new ShortestPath.PrioritySet();
        
        ps.offer(p2);
        ps.offer(p1);
        
        PathNode returned = ps.poll();
        assertEquals(p1, returned);
        
    }
}
