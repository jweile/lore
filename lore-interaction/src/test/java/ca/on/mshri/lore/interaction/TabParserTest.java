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
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.interaction.tabparser.TabParser;
import ca.on.mshri.lore.molecules.Protein;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TabParserTest extends TestCase {
    
    public void test() throws Exception {
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, 
                ModelFactory.createDefaultModel());
        
        Experiment exp = Experiment.createOrGet(model, "CCSB-HI1.1");
        
        OntClass physInt = model.getOntClass(PhysicalInteraction.CLASS_URI);
        
        InputStream in = new FileInputStream("src/test/resources/CCSB_HI1_updated.tsv");
        
        TabParser parser = new TabParser();
        
        parser.parse(model, in, model.ENTREZ, exp, physInt, Protein.class, true);
        
        List<Interaction> interactions = model.listIndividualsOfClass(Interaction.class, false);
        
        for (Interaction interaction : interactions) {
            List<? extends RecordObject> participants = interaction.listParticipants();
            
            System.out.println(interaction.getOntClass());
            
            for (RecordObject o : participants) {
                System.out.print("  -> ");
                System.out.println(getXRefValue(o, model.ENTREZ));
            }
            
        }
        
    }
    
    private String getXRefValue(RecordObject o, Authority a) {
        for (XRef x : o.listXRefs()) {
            if (x.getAuthority().equals(a)) {
                return x.getValue();
            }
        }
        return null;
    }
    
}
