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
package ca.on.mshri.lore.phenotype.hgmd;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.URLParameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliIndeterminateProgress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class HgmdImporter extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    @Override
    public void run() {
        
        Logger.getLogger(HgmdImporter.class.getName())
                .log(Level.INFO, "HGMD importer started.");
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_DL_MEM, getModel());
        Authority hgmdPhenoAuth = Authority.createOrGet(model, "HGMD-Disease");
        Property association = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        Property causes = model.getProperty(PhenotypeModel.URI+"#isCausallyAssociatedWith");
                        
        URL inURL = getParameterValue(srcP);
        InputStream in = null;
        
//        int type = 0;
        int variant_class = 1;
        int acc = 2;
//        int dbsnp = 3;
//        int genomic_coordinates_hg18 = 4;
//        int genomic_coordinates_hg19 = 5;
//        int hgvs_cdna = 6;
        int hgvs_protein = 7;
        int gene = 8;
        int disease = 9;
//        int sequence_context_hg18 = 10;
//        int sequence_context_hg19 = 11;
//        int codon_change = 12;
//        int mut_AA = 13;
//        int mut_Type = 14;
//        int codon_number = 15;
//        int intron_number = 16;
//        int site = 17;
//        int location = 18;
//        int location_reference_point = 19;
//        int author = 20;
//        int journal = 21;
//        int vol = 22;
//        int page = 23;
//        int year = 24;
//        int pmid = 25;
        int entrezid = 26;
//        int sift_score = 27;
//        int sift_prediction = 28;
//        int mutpred_score = 29;
//        int tag = 32;
        
        int lastPhenoId = 0;
        Map<String,Phenotype> phenoIndex = new HashMap<String, Phenotype>();
        
        try {
            in = inURL.openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                //skip header line
                if (lnum == 1) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                if (cols.length < 27) {
                    Logger.getLogger(HgmdImporter.class.getName())
                            .log(Level.WARNING, "Invalid line: "+lnum);
                    continue;
                }
                
                if (cols[entrezid].equals("null")) {
                    Logger.getLogger(HgmdImporter.class.getName())
                            .log(Level.WARNING, "Skipping entry without entrez id: "+lnum);
                    continue;
                }
                
                //make gene
                Gene geneEntity = Gene.createOrGet(model, model.ENTREZ, cols[entrezid]);
                geneEntity.addXRef(model.HGNC, cols[gene]);
                
                //make and link allele
                Allele alleleEntity = Allele.createOrGet(model, model.HGMD, cols[acc]);
                alleleEntity.setGene(geneEntity);
                
                //make and link disease
                Phenotype phenoEntity = phenoIndex.get(cols[disease]);
                if (phenoEntity == null) {
                    phenoEntity = Phenotype.createOrGet(model, hgmdPhenoAuth, (++lastPhenoId)+"");
                    phenoEntity.addLabel(cols[disease],null);
                    phenoIndex.put(cols[disease],phenoEntity);
                }
                
                //choose causal or loose association based on variant class
                Property prop = cols[variant_class].equals("DM") ? causes : association;
                
                alleleEntity.addProperty(prop, phenoEntity);
                geneEntity.addProperty(prop, phenoEntity);
                
                //make and link mutation
                PointMutation mutEntity = null;
                String[] split = cols[hgvs_protein].split("\\.");
                if (split.length == 3) {
                    String mutDesc = split[2];
                    try {
                        mutEntity = PointMutation.createOrGet(model, alleleEntity, mutDesc);
                        alleleEntity.addMutation(mutEntity);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(HgmdImporter.class.getName())
                                .log(Level.WARNING, "Illegal mutation description in line "+lnum+":"+mutDesc, ex);
                    }
                } else {
                    Logger.getLogger(HgmdImporter.class.getName())
                            .log(Level.WARNING, "Cannot extract mutation description in line "+lnum);
                }
                
                progress.next("Parsing");
                
            }
            
            progress.done();
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not read from URL "+inURL,ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(HgmdImporter.class.getName())
                            .log(Level.WARNING, "Unable to close stream.", ex);
                }
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
