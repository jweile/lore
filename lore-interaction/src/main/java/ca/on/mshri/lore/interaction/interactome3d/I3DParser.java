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
import ca.on.mshri.lore.genome.util.LocalAlignment;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.molecules.Structure3D;
import ca.on.mshri.lore.molecules.Structure3D.SeqMap;
import ca.on.mshri.lore.molecules.operations.FetchUniprotSeqs;
import ca.on.mshri.lore.molecules.util.Structure;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.operations.util.TabDelimParser;
import ca.on.mshri.lore.operations.util.URLParameter;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biojava3.alignment.Alignments;
import org.biojava3.alignment.Alignments.PairwiseSequenceAlignerType;
import org.biojava3.alignment.SimpleGapPenalty;
import org.biojava3.alignment.SimpleSubstitutionMatrix;
import org.biojava3.alignment.template.SequencePair;
import org.biojava3.alignment.template.SubstitutionMatrix;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AminoAcidCompound;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class I3DParser extends TabDelimParser {

    public final URLParameter srcP = new URLParameter("src");
    
    public final Parameter<String> experimentP = Parameter.make("experiment", String.class);
    
    public final Parameter<String> pdbLocP = Parameter.make("pdbLoc", String.class);
    private File pdbLoc;
    
    //indices
    private static final int prot1 = 0;
    private static final int prot2 = 1;
    private static final int pdbId = 5;
    private static final int chain1 = 7;
    private static final int seq_begin1 = 11;
    private static final int seq_end1 = 12;
    private static final int chain2 = 14;
    private static final int seq_begin2 = 18;
    private static final int seq_end2 = 19;
    private static final int fileName = 21;
    
    //fields
    private InteractionModel iaModel;
    private Authority domAuth;
    private Experiment exp;
    private OntClass physInt;
    private Property involved;
    
    
    /**
     * Run the parser.
     */
    @Override
    public void run() {
        
        iaModel = new InteractionModel(OntModelSpec.OWL_MEM, getModel());
        domAuth = Authority.createOrGet(iaModel, "LoreProteinDomain");
        
        URL src = getParameterValue(srcP);
        if (src == null) {
            throw new IllegalArgumentException("Required parameter src!");
        }
        
        pdbLoc = new File(getParameterValue(pdbLocP));
        if (!pdbLoc.exists() || !pdbLoc.isDirectory()) {
            throw new IllegalArgumentException("pdbLoc must be a valid directory name");
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
        
    /**
     * Last used protein identifiers
     */
    private String lastP1 = "", lastP2 ="";
    private FetchUniprotSeqs seqFetcher = new FetchUniprotSeqs();
    
    /**
     * process the current row.
     * @param cols 
     */
    @Override
    protected void processRow(String[] cols) {
        
        if (cols[prot1].equals(lastP1) && cols[prot2].equals(lastP2)) {
            //skip this row, since it's a lower scoring interaction
            return;
        }
        
        //proteins
        Protein p1 = Protein.createOrGet(iaModel, iaModel.UNIPROT, cols[prot1]);
        seqFetcher.fetchSequenceForProtein(p1, iaModel);
        Protein p2 = Protein.createOrGet(iaModel, iaModel.UNIPROT, cols[prot2]);
        seqFetcher.fetchSequenceForProtein(p2, iaModel);
        
        //interaction
        PhysicalInteraction interaction = PhysicalInteraction.createOrGet(iaModel, exp, physInt, p1, p2);
        
        //structure
        File pdbFile = new File(pdbLoc,cols[fileName]);
        if (pdbFile.exists()) {
                
            Structure3D structure = Structure3D.createOrGet(iaModel, domAuth, cols[fileName]);
            structure.addXRef(iaModel.PDB, cols[pdbId]);

            Structure3D.addStructureToObject(structure, interaction);
                
            try {
                structure.setSource(pdbFile.toURI().toURL());
                buildSeqMap(structure, p1, cols[prot1], cols[chain1], p2, cols[prot2], cols[chain2]);
            } catch (MalformedURLException ex) {
                Logger.getLogger(I3DParser.class.getName())
                        .log(Level.SEVERE, "Malformed URL for file.", ex);
            }
        } else {
            Logger.getLogger(I3DParser.class.getName())
                    .log(Level.WARNING, "PDB file not found: "+pdbFile);
        }
        
        //        ProteinDomain d1 = makeDomain(p1, cols, seq_begin1, seq_end1);
        //        ProteinDomain d2 = makeDomain(p2, cols, seq_begin2, seq_end2);
        
        
//        d1.addProperty(involved, interaction);
//        d2.addProperty(involved, interaction);
        
        lastP1 = cols[prot1];
        lastP2 = cols[prot2];
    }

    
//    private ProteinDomain makeDomain(Protein p1, String[] cols, int seq_begin, int seq_end) {
//        
//        int begin = Integer.parseInt(cols[seq_begin]);
//        int end = Integer.parseInt(cols[seq_end]);
//        
//        String protId = p1.getURI().substring(9);
//        
//        StringBuilder b = new StringBuilder();
//        b.append(protId).append(":");
//        b.append(begin).append("-").append(end);
//        
//        ProteinDomain d = ProteinDomain.createOrGet(iaModel, domAuth, b.toString());
//        if (d.getProtein() == null || !d.getProtein().equals(p1)) {
//            d.setProtein(p1);
//        }
//        if (d.getStart() == null || !d.getStart().equals(begin)) {
//            d.setStart(begin);
//        }
//        if (d.getEnd() == null || !d.getEnd().equals(end)) {
//            d.setEnd(end);
//        }
//        
//        return d;
//    }
    
    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
    private void buildSeqMap2(Structure struc, Protein p, String uniprot, String chain) {
        
        ProteinSequence protSeq = new ProteinSequence(p.getSequence());
        ProteinSequence strucSeq = new ProteinSequence(struc.getChainSequence(chain));
        
        SubstitutionMatrix<AminoAcidCompound> matrix = new SimpleSubstitutionMatrix<AminoAcidCompound>();
        SequencePair<ProteinSequence, AminoAcidCompound> pair = Alignments.getPairwiseAlignment(protSeq, strucSeq,
                PairwiseSequenceAlignerType.LOCAL, new SimpleGapPenalty(), matrix);
        
        int[] a2b = new int[protSeq.getLength()];
        for (int i = 0; i < a2b.length; i++) {
            a2b[i] = pair.getIndexInTargetForQueryAt(i);
        }
        
        //TODO: continue here
    }

    private void buildSeqMap(Structure3D s3d, Protein p1, String uniprot1, String chain1, 
                                Protein p2, String uniprot2, String chain2) {
        
        SeqMap map = new SeqMap();
        
        Structure struc = s3d.getStructureObject();
        
        String protSeq1 = p1.getSequence();
        String strucSeq1 = struc.getChainSequence(chain1);
        LocalAlignment align1 = new LocalAlignment(protSeq1, strucSeq1);
        Logger.getLogger(I3DParser.class.getName())
                .log(Level.FINE, "Alignment of "+uniprot1+":\n"+align1.getAlignment());
        map.put(uniprot1+"|"+chain1, align1.getA2B());
        
        String protSeq2 = p2.getSequence();
        String strucSeq2 = struc.getChainSequence(chain2);
        LocalAlignment align2 = new LocalAlignment(protSeq2, strucSeq2);
        Logger.getLogger(I3DParser.class.getName())
                .log(Level.FINE, "Alignment of "+uniprot2+":\n"+align2.getAlignment());
        map.put(uniprot2+"|"+chain2, align2.getA2B());
        
        s3d.setSeqMap(map);
    }
}
