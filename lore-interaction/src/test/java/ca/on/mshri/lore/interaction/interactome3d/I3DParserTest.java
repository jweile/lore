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
package ca.on.mshri.lore.interaction.interactome3d;

import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.interaction.Interaction;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.ProteinDomain;
import ca.on.mshri.lore.molecules.Structure3D;
import ca.on.mshri.lore.molecules.Structure3D.SeqMap;
import ca.on.mshri.lore.operations.Sparql;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class I3DParserTest extends TestCase {
    
    public I3DParserTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        File logFile = new File("target/surefire/I3DParserTest.log");
        FileHandler fh = new FileHandler(logFile.getAbsolutePath());
        fh.setLevel(Level.ALL);
        fh.setFormatter(new SimpleFormatter());
        java.util.logging.Logger.getLogger("").addHandler(fh);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void test() throws Exception {
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
        
        File inFile = new File("src/test/resources/interactions.dat");
        File pdbDir = new File("src/test/resources/pdb/");
        
        I3DParser parser = new I3DParser();
        parser.setModel(model);
        parser.setParameter(parser.srcP, inFile.toURI().toURL());
        parser.setParameter(parser.experimentP, "Interactome3D-CCSB:PW1:PW2");
        parser.setParameter(parser.pdbLocP, pdbDir.getAbsolutePath());
        
        parser.run();
        
        
        for(Protein protein : model.listIndividualsOfClass(Protein.class, true)) {
            
            for (Structure3D s3d : Structure3D.listStructuresOfObject(protein)) {
                print(s3d.getSeqMap());
            }
            
        }
        
        
//        Sparql sparql = Sparql.getInstance(I3DParser.class.getProtectionDomain().getCodeSource());
//        
//        for (PhysicalInteraction interaction : model.listIndividualsOfClass(PhysicalInteraction.class, false)) {
//            
//            System.out.println(interaction);
//            
//            for (RecordObject o : interaction.listParticipants()) {
//                
//                Protein protein = Protein.fromIndividual(o);
//                
//                System.out.println("  -> "+protein);
//                
//                List<Individual> domains = sparql.queryIndividuals(
//                        model, 
//                        "getDomains", 
//                        "domain", 
//                        protein.getURI(), 
//                        interaction.getURI()
//                );
//                
//                for (Individual domainInd : domains) {
//                    ProteinDomain domain = ProteinDomain.fromIndividual(domainInd);
//                    System.out.println("    ->"+domain);
//                    
//                    System.out.println("      -> "+domain.getStart()+" : "+domain.getEnd());
//                }
//            }
//            
//        }
//        
        System.out.flush();
        
    }

    private void print(SeqMap seqMap) {
        for (String key : seqMap.getKeys()) {
            System.out.println(key);
            for (int i : seqMap.get(key)) {
                String num = i+"";
                for (int j = 0; j < 3-num.length(); j++) System.out.print(" ");
                System.out.print(num);
            }
            System.out.println();
        }
    }
}
