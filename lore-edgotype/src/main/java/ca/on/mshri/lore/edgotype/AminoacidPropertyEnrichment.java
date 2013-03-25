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

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Molecule;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.util.AminoacidProps;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs an enrichment analysis for edgotypes regarding
 * the aminoacid properties of the causing mutations.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class AminoacidPropertyEnrichment extends LoreOperation {
    
    public final Parameter<String> outfileP = Parameter.make("outfile", String.class, "aaprop.tsv");
    
    @Override
    public void run() {
        
        Logger.getLogger(AminoacidPropertyEnrichment.class.getName())
                .log(Level.INFO, "Computing changes in aminoacid properties...");
        
        File outfile = new File(getParameterValue(outfileP));
        
        AminoacidProps props = new AminoacidProps();
        
        InteractionModel model = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        pos = model.getProperty(InteractionModel.URI+"#affectsPositively");
        neg = model.getProperty(InteractionModel.URI+"#affectsNegatively");
        
        StringBuilder b = new StringBuilder();
        
        List<Allele> alleles = model.listIndividualsOfClass(Allele.class, true);
        
        for (Allele allele : alleles) {
            
            PointMutation mutation = getMutation(allele, model);
            Double disruptionRate = getDisruptionRate(allele);
            if (mutation == null || disruptionRate == null) {
                continue;
            }
            
            String oldAA = mutation.getFromAminoAcid();
            String newAA = mutation.getToAminoAcid();
            
            String oldCharge = props.getCharge(oldAA);
            String newCharge = props.getCharge(newAA);
            String chargeChange = !oldCharge.equals(newCharge) ?
                    oldCharge+"->"+newCharge:
                    "Same";
            
            double hydropathyChange = props.getHydropathy(newAA) - props.getHydropathy(oldAA);
            
            String oldPol = props.getPolarity(oldAA);
            String newPol = props.getPolarity(newAA);
            String polChange = !oldPol.equals(newPol) ?
                    oldPol+"->"+newPol :
                    "Same";
            
            b.append(chargeChange)
                    .append("\t")
                    .append(hydropathyChange)
                    .append("\t")
                    .append(polChange)
                    .append("\t")
                    .append(disruptionRate)
                    .append("\n");
        }
        
        
        Logger.getLogger(AminoacidPropertyEnrichment.class.getName())
                .log(Level.INFO, "Writing results to file...");
        
        BufferedWriter w = null;
        try {
            
            w = new BufferedWriter(new FileWriter(outfile));
            w.write(b.toString());
            
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write to "+outfile);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    Logger.getLogger(AminoacidPropertyEnrichment.class.getName())
                            .log(Level.WARNING, "Cannot close writer", ex);
                }
            }
        }
        
        Logger.getLogger(AminoacidPropertyEnrichment.class.getName())
                .log(Level.INFO, "Done!");
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    public PointMutation getMutation(Allele allele, InteractionModel model) {
        for (Mutation mut :allele.listMutations()) {
            if (LoreModel.hasClass(mut, model.getOntClass(PointMutation.CLASS_URI))) {
                return PointMutation.fromIndividual(mut);
            }
        }
        return null;
    }

    private Property pos, neg;
    
    private Double getDisruptionRate(Allele allele) {
        
        NodeIterator it = allele.listPropertyValues(pos);
        int posCount = 0, negCount = 0;
        while (it.hasNext()) {
            it.next();
            posCount++;
        }
        it = allele.listPropertyValues(neg);
        while (it.hasNext()) {
            it.next();
            negCount++;
        }
        if (posCount + negCount == 0) {
            return null;
        } else {
            return (double) negCount / (double) (posCount + negCount);
        }
    }
    
    private List<String> getEdgotype(Allele allele) {
        
        Protein protein = null;
        try {
            protein = Protein.listEncodedProteins(allele.getGene()).get(0);
        } catch (Exception ex) {
            return null;
        }
        
        List<String> out = new ArrayList<String>();
        
        NodeIterator it = allele.listPropertyValues(pos);
        while (it.hasNext()) {
            PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
            
            Protein target = null;
            for (Molecule participant : ia.listParticipants()) {
                if (!participant.equals(protein)) {
                    target = Protein.fromIndividual(participant);
                    out.add("pos:"+target.getURI().substring(17));
                }
            }
            
        }
        it = allele.listPropertyValues(neg);
        while (it.hasNext()) {
            PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
            
            Protein target = null;
            for (Molecule participant : ia.listParticipants()) {
                if (!participant.equals(protein)) {
                    target = Protein.fromIndividual(participant);
                    out.add("neg:"+target.getURI().substring(17));
                }
            }
            
        }
        return out;
    }
    
}
