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

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.ProteinDomain;
import ca.on.mshri.lore.operations.util.GuidGenerator;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class I3DParser extends TabDelimParser {

    public final URLParameter srcP = new URLParameter("src");
    
    public final Parameter<String> experimentP = Parameter.make("experiment", String.class);
    
    //indices
    private static final int prot1 = 0;
    private static final int prot2 = 1;
    private static final int seq_begin1 = 11;
    private static final int seq_end1 = 12;
    private static final int seq_begin2 = 18;
    private static final int seq_end2 = 19;
    
    //fields
    private InteractionModel iaModel;
    private Authority domAuth;
    private Experiment exp;
    private OntClass physInt;
    private Property involved;
    
    
    @Override
    public void run() {
        
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        domAuth = Authority.createOrGet(iaModel, "LoreProteinDomain");
        
        URL src = getParameterValue(srcP);
        if (src == null) {
            throw new IllegalArgumentException("Required parameter src!");
        }
        
        String expName = getParameterValue(experimentP);
        if (expName == null) {
            throw new IllegalArgumentException("Required parameter \"experiment\"!");
        }
        exp = Experiment.createOrGet(iaModel, expName);
        
        physInt = iaModel.getOntClass(PhysicalInteraction.CLASS_URI);
        involved = iaModel.getProperty(InteractionModel.URI+"#involvedIn");
        
        try {
            parseTabDelim(src.openStream(), 1, 22);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read "+src, ex);
        }
    }
        
    private String lastP1 = "", lastP2 ="";
    
    @Override
    protected void processRow(String[] cols) {
        
        if (cols[prot1].equals(lastP1) && cols[prot2].equals(lastP2)) {
            //skip this row, since it's a lower scoring interaction
            return;
        }
        
        Protein p1 = Protein.createOrGet(iaModel, iaModel.UNIPROT, cols[prot1]);
        Protein p2 = Protein.createOrGet(iaModel, iaModel.UNIPROT, cols[prot2]);
        
        ProteinDomain d1 = makeDomain(p1, cols, seq_begin1, seq_end1);
        ProteinDomain d2 = makeDomain(p2, cols, seq_begin2, seq_end2);
        
        PhysicalInteraction interaction = PhysicalInteraction.createOrGet(iaModel, exp, physInt, p1, p2);
        d1.addProperty(involved, interaction);
        d2.addProperty(involved, interaction);
        
        lastP1 = cols[prot1];
        lastP2 = cols[prot2];
    }

    
    private ProteinDomain makeDomain(Protein p1, String[] cols, int seq_begin, int seq_end) {
        
        int begin = Integer.parseInt(cols[seq_begin]);
        int end = Integer.parseInt(cols[seq_end]);
        
        String protId = p1.getURI().substring(9);
        
        StringBuilder b = new StringBuilder();
        b.append(protId).append(":");
        b.append(begin).append("-").append(end);
        
        ProteinDomain d = ProteinDomain.createOrGet(iaModel, domAuth, b.toString());
        d.setProtein(p1);
        d.setStart(begin);
        d.setEnd(Integer.parseInt(cols[seq_end]));
        
        return d;
    }
    
    @Override
    public boolean requiresReasoner() {
        return false;
    }
}
