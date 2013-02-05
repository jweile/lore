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
package ca.on.mshri.lore.interaction.edgotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.Interaction;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Molecule;
import ca.on.mshri.lore.molecules.Protein;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class EdgotypeParserV2Test extends TestCase {
    
    public EdgotypeParserV2Test(String testName) {
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
         
         InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, ModelFactory.createDefaultModel());
         
         File file = new File("src/test/resources/PW1v2.tsv");
         
         Authority ccsbMut = Authority.createOrGet(model, "CCSB-Mutant");
//         Experiment exp = Experiment.createOrGet(model, "CCSB-Edgotyping-PW1");
         Property pos = model.getProperty(InteractionModel.URI+"#affectsPositively");
         Property neg = model.getProperty(InteractionModel.URI+"#affectsNegatively");
         
         InteractionParserV2 parser = new InteractionParserV2();
//         parser.setParameter(parser.modelP, model);
         parser.setParameter(parser.srcP, parser.srcP.validate(file.toURI().toURL()));
         parser.setParameter(parser.expP, "CCSB-Edgotyping-PW1");
         parser.setModel(model);
         parser.run();
         
         
         //list contents
         for (PhysicalInteraction physInt : model.listIndividualsOfClass(PhysicalInteraction.class, false)) {
             System.out.println("Interaction");
             for (Molecule molecule : physInt.listParticipants()) {
                 Protein protein = Protein.fromIndividual(molecule);
                 System.out.println("  - "+protein.getXRefValue(model.ENTREZ));
                 Gene gene = protein.getEncodingGene();
                 for (Allele allele : gene.listAlleles()) {
                     String desc = null;
                     for (Mutation mut : allele.listMutations()) {
                         PointMutation pmut = PointMutation.fromIndividual(mut);
                         desc = pmut.getFromAminoAcid() + pmut.getPosition() + pmut.getToAminoAcid();
                         break;
                     }
                     
                     NodeIterator it = allele.listPropertyValues(pos);
                     while (it.hasNext()) {
                         Interaction affectedInt = Interaction.fromIndividual(it.next().as(Individual.class));
                         if (affectedInt.equals(physInt)) {
                             System.out.println("    - positively affected by "+allele.getXRefValue(ccsbMut)+" ("+desc+")");
                         }
                     }
                     
                     it = allele.listPropertyValues(neg);
                     while (it.hasNext()) {
                         Interaction affectedInt = Interaction.fromIndividual(it.next().as(Individual.class));
                         if (affectedInt.equals(physInt)) {
                             System.out.println("    - negatively affected by "+allele.getXRefValue(ccsbMut)+" ("+desc+")");
                         }
                     }
                 }
             }
         }
         
//         for (Allele allele : model.listIndividualsOfClass(Allele.class, true)) {
//             String aName = allele.getXRefValue(ccsbMut);
//             
//             NodeIterator it = allele.listPropertyValues(pos);
//             List<String> posInt = new ArrayList<String>();
//             while (it.hasNext()) {
//                 PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
//                 posInt.add(ia.listParticipants().toString());
//             }
//             
//             it = allele.listPropertyValues(neg);
//             List<String> negInt = new ArrayList<String>();
//             while (it.hasNext()) {
//                 PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
//                 negInt.add(ia.listParticipants().toString());
//             }
//             
//             if (!posInt.isEmpty() || !negInt.isEmpty()) {
//                 
//                 List<String> intersection = intersect(posInt, negInt);
//                 if (!intersection.isEmpty()) {
//                     throw new AssertionError("Intersection not empty for "+aName+": "+ intersection);
//                 }
//                 
//                 System.out.println(aName);
//                 for (String i : posInt) {
//                     System.out.println("  - pos: "+i);
//                 }
//                 for (String i : negInt) {
//                     System.out.println("  - neg: "+i);
//                 }
//             }
//         }
         
     }

    private List<String> intersect(List<String> posInt, List<String> negInt) {
        ArrayList<String> out = new ArrayList<String>(posInt);
        out.retainAll(negInt);
        return out;
    }
}
