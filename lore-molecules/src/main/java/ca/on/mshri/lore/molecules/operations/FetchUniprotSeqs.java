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
package ca.on.mshri.lore.molecules.operations;

import ca.on.mshri.lore.molecules.MoleculesModel;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.RefListParameter;
import ca.on.mshri.lore.operations.util.ResourceReferences;
import com.hp.hpl.jena.ontology.OntModelSpec;
import de.jweile.yogiutil.CliProgressBar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class FetchUniprotSeqs extends LoreOperation {

    public final RefListParameter<Protein> selectionP = new RefListParameter<Protein>("selection", Protein.class);
    
    @Override
    public void run() {
        MoleculesModel model = new MoleculesModel(OntModelSpec.OWL_MEM, getModel());
        
        ResourceReferences<Protein> proteinRefs = getParameterValue(selectionP);
        List<Protein> proteins = proteinRefs != null ? 
                proteinRefs.resolve(model):
                model.listIndividualsOfClass(Protein.class, false);
        
        CliProgressBar pb = new CliProgressBar(proteins.size());
        
        for (Protein protein : proteins) {
            
            fetchSequenceForProtein(protein, model);
            
            pb.next();
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private String fetchSeq(String urlString) {
        
        StringBuilder seq = new StringBuilder();
        
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            in = url.openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line; int lnum = 0; int seqNum = 0;
            while ((line = r.readLine()) != null) {
                
                if (line.startsWith(">")) {
                    if (++seqNum > 1) {
                        throw new RuntimeException("File contains more than one sequence!");
                    }
                    continue;
                    
                } else {
                    
                    seq.append(line.trim());
                    
                }
                
            }
        } catch (MalformedURLException ex) {
            //should not happen
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading stream from "+urlString, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(FetchUniprotSeqs.class.getName())
                            .log(Level.WARNING, "Unable to close stream", ex);
                }
            }
        }
        
        return seq.toString();
    }

    public void fetchSequenceForProtein(Protein protein, MoleculesModel model) {
        
        String uniprotId = protein.getXRefValue(model.UNIPROT);
        
        if (uniprotId != null && protein.getSequence() == null) {
            
            String seq = fetchSeq("http://www.uniprot.org/uniprot/"+uniprotId+".fasta");
            if (seq != null && seq.length() > 0) {
                protein.setSequence(seq);
            }
        }
    }
    
}
