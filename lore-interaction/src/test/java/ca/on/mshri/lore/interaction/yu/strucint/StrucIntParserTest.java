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
package ca.on.mshri.lore.interaction.yu.strucint;

import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.molecules.ProteinDomain;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class StrucIntParserTest extends TestCase {
    
    public StrucIntParserTest(String testName) {
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
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, 
                ModelFactory.createDefaultModel());
        
        StrucIntParser parser = new StrucIntParser();
        parser.setModel(model);
        parser.setParameter(
                parser.srcP, 
                new File("src/test/resources/yu_structural_interactions.csv")
                    .toURI().toURL()
        );
        
        parser.run();
        
        
        List<ProteinDomain> domains = model.listIndividualsOfClass(ProteinDomain.class, true);
        for (int i = 0; i < 10; i++) {
            ProteinDomain domain = domains.get(i);
            String entrez = domain.getProtein().getXRefValue(model.ENTREZ);
            String pfam = domain.getXRefValue(model.PFAM);
            System.out.println(entrez+"\t"+pfam+"\t"+domain.getStart()+"\t"+domain.getEnd());
        }
        
    }
}
