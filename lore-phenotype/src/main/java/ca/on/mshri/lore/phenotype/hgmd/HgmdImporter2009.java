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
public class HgmdImporter2009 extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    @Override
    public void run() {
        
        Logger.getLogger(HgmdImporter2009.class.getName())
                .log(Level.INFO, "HGMD importer started.");
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_DL_MEM, getModel());
        Authority hgmdPhenoAuth = Authority.createOrGet(model, "HGMD-Disease");
        Property association = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        Property causes = model.getProperty(PhenotypeModel.URI+"#isCausallyAssociatedWith");
                        
        URL inURL = getParameterValue(srcP);
        InputStream in = null;
        
        int variant_class = -1;
        int acc = -1;
        int hgvs_protein = -1;
        int gene = -1;
        int disease = -1;
        int entrezid = -1;
        
//        int variant_class = 1;
//        int acc = 2;
//        int hgvs_protein = 7;
//        int gene = 8;
//        int disease = 9;
//        int entrezid = 26;
        
        int lastPhenoId = 0;
        Map<String,Phenotype> phenoIndex = new HashMap<String, Phenotype>();
        
        int ncols = 0;
        
        try {
            in = inURL.openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                String[] cols = line.split("\t");
                
                //if header line, only extract header info
                if (lnum == 1) {
                    //set expected number of columns
                    ncols = cols.length;
                    //find columns by name
                    for (int i = 0; i < ncols; i++) {
                        if (cols[i].equals("Variant_class")) {
                            variant_class = i;
                        } else if (cols[i].equals("ACC_NUM")) {
                            acc = i;
                        } else if (cols[i].equals("HGVS_protein")) {
                            hgvs_protein = i;
                        } else if (cols[i].equals("gene")) {
                            gene = i;
                        } else if (cols[i].equals("disease")) {
                            disease = i;
                        } else if (cols[i].equals("entrezid") || cols[i].equals("Entrez_ID")) {
                            entrezid = i;
                        } 
                    }
                    if (variant_class < 0) {
                        throw new RuntimeException("Could not find column \"Variant_class\"");
                    }
                    if (acc < 0) {
                        throw new RuntimeException("Could not find column \"ACC_NUM\"");
                    }
                    if (hgvs_protein < 0) {
                        throw new RuntimeException("Could not find column \"HGVS_protein\"");
                    }
                    if (gene < 0) {
                        throw new RuntimeException("Could not find column \"gene\"");
                    }
                    if (disease < 0) {
                        throw new RuntimeException("Could not find column \"disease\"");
                    }
                    if (entrezid < 0) {
                        Logger.getLogger(HgmdImporter2009.class.getName())
                                .log(Level.WARNING, "Could not find column \"entrez id\". Indexing by gene name instead.");
                    }
                    
                    continue;
                }
                
                if (cols.length < ncols) {
                    Logger.getLogger(HgmdImporter2009.class.getName())
                            .log(Level.WARNING, "Invalid line: "+lnum);
                    continue;
                }
                
                if (entrezid >= 0 && cols[entrezid].equals("null")) {
                    Logger.getLogger(HgmdImporter2009.class.getName())
                            .log(Level.WARNING, "Skipping entry without entrez id: "+lnum);
                    continue;
                }
                
                //make gene
                Gene geneEntity = null;
                if (entrezid < 0) {
                    geneEntity = Gene.createOrGet(model, model.HGNC, cols[gene]);
                } else {
                    try {
                        Integer.parseInt(cols[entrezid]);
                    } catch (NumberFormatException e) {
                        Logger.getLogger(HgmdImporter2009.class.getName())
                            .log(Level.WARNING, "Skipping entry with invalid entrez id: "+lnum);
                        continue;
                    }
                    geneEntity = Gene.createOrGet(model, model.ENTREZ, cols[entrezid]);
                    geneEntity.addXRef(model.HGNC, cols[gene]);
                }
                
                //make and link allele
                Allele alleleEntity = Allele.createOrGet(model, model.HGMD, cols[acc]);
                alleleEntity.setGene(geneEntity);
                
                //make and link disease
                String disString = processDiseaseName(cols[disease]);
                Phenotype phenoEntity = phenoIndex.get(disString);
                if (phenoEntity == null) {
                    phenoEntity = Phenotype.createOrGet(model, hgmdPhenoAuth, (++lastPhenoId)+"");
                    phenoEntity.addLabel(disString,null);
                    phenoIndex.put(disString,phenoEntity);
                }
                
                //choose causal or loose association based on variant class
                Property prop = cols[variant_class].equals("DM") ? causes : association;
                
                alleleEntity.addProperty(prop, phenoEntity);
                geneEntity.addProperty(prop, phenoEntity);
                
                //make and link mutation
                PointMutation mutEntity = null;
                String[] split = cols[hgvs_protein].split("\\.");
                if (split.length > 1) {
                    String mutDesc = split[split.length-1];
                    try {
                        mutEntity = PointMutation.createOrGet(model, alleleEntity, mutDesc);
                        alleleEntity.addMutation(mutEntity);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(HgmdImporter2009.class.getName())
                                .log(Level.WARNING, "Illegal mutation description in line "+lnum+":"+mutDesc);
                    }
                } else {
                    Logger.getLogger(HgmdImporter2009.class.getName())
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
                    Logger.getLogger(HgmdImporter2009.class.getName())
                            .log(Level.WARNING, "Unable to close stream.", ex);
                }
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    /**
     * remove quotation marks and trailing question marks.
     * 
     * @param string
     * @return 
     */
    private String processDiseaseName(String string) {
        if (string.startsWith("\"")) {
            string = string.substring(1);
        }
        if (string.endsWith("\"")) {
            string = string.substring(0, string.length()-1);
        }
        string = string.trim();
        if (string.trim().endsWith("?")) {
            string = string.substring(0, string.length()-1);
        }
        return string.trim();
    }
    
}
