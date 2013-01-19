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

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.Experiment;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.ProteinDomain;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses the structural interaction network from the Yu Lab (Wang et al 2012).
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class StrucIntParser extends TabDelimParser {

    /**
     * The source URL. Required!
     */
    public final URLParameter srcP = new URLParameter("src");
    
    /**
     * A string describing the experiment (defaults to Wang_2012)
     */
    public final Parameter<String> experimentP = Parameter.make("experiment", String.class, "Wang_2012");
    
    /*
     * Shared fields
     */
    private InteractionModel iaModel;
    private OntClass physInt;
    private Authority domAuth;
    private Property involvedIn;
    private Experiment exp;
    
    /*
     * Column indices
     */
    private static final int proteinA = 0;
    private static final int proteinB = 1;
    private static final int pfamA = 2;
    private static final int startA = 3;
    private static final int endA = 4;
    private static final int pfamB = 5;
    private static final int startB = 6;
    private static final int endB = 7;
    
    @Override
    public void run() {
        
        Logger.getLogger(StrucIntParser.class.getName()).log(Level.INFO, "Structural interactome parser started.");
        
        //init fields
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        physInt = iaModel.getOntClass(PhysicalInteraction.CLASS_URI);
        domAuth = Authority.createOrGet(iaModel, "LoreProteinDomain");
        involvedIn = iaModel.getProperty(InteractionModel.URI+"#involvedIn");
        exp = Experiment.createOrGet(iaModel, getParameterValue(experimentP));
        
        //check out source URL
        URL src = getParameterValue(srcP);
        if (src == null) {
            throw new IllegalArgumentException("Missing src parameter!");
        }
        
        //start parser
        try {
            parseTabDelim(src.openStream(), 6, 8);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read "+src, ex);
        }
        
    }

    /**
     * Process tab-delimited row.
     * @param cols Array of column values for this row.
     */
    @Override
    protected void processRow(String[] cols) {
        
        //build proteins
        Protein pA = Protein.createOrGet(iaModel, iaModel.ENTREZ, cols[proteinA]);
        Protein pB = Protein.createOrGet(iaModel, iaModel.ENTREZ, cols[proteinB]);

        //build interaction between proteins
        PhysicalInteraction interaction = PhysicalInteraction.createOrGet(iaModel, exp, physInt, pA,pB);

        //build domains
        ProteinDomain domA = makeDomain(cols[proteinA], cols[pfamA], pA, 
                Integer.parseInt(cols[startA]), 
                Integer.parseInt(cols[endA]));
        ProteinDomain domB = makeDomain(cols[proteinB], cols[pfamB], pB, 
                Integer.parseInt(cols[startB]), 
                Integer.parseInt(cols[endB]));

        //link domains with interaction
        domA.addProperty(involvedIn, interaction);
        domB.addProperty(involvedIn, interaction);
        
    }

    /**
     * Builds a ProteinDomain object from the given information
     * @param entrez EntrezGene id
     * @param pfam PFam id
     * @param protein protein object to link with
     * @param start start position on protein
     * @param end end position on protein
     * @return the ProteinDomain object.
     */
    private ProteinDomain makeDomain(String entrez, String pfam, Protein protein, int start, int end) {
        
        //build identifier: ENTREZ.PFAM:START-END
        StringBuilder id = new StringBuilder();
        id.append(entrez).append(".").append(pfam).append(":").append(start).append("-").append(end);
        
        //create or retrive existing domain object
        ProteinDomain domain = ProteinDomain.createOrGet(iaModel, domAuth, id.toString());
        
        //if it's pre-existing
        if (domain.getProtein() != null) { 
            //check consistency
            if (!domain.getProtein().equals(protein)) {
                throw new InconsistencyException("Domain associated with multiple proteins!");
            } 
        } else {
            //if it's a new object, init everything
            domain.addXRef(iaModel.PFAM, pfam);
            domain.setProtein(protein);
            domain.setStart(start);
            domain.setEnd(end);
        }
        
        return domain;
    }

    
    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
