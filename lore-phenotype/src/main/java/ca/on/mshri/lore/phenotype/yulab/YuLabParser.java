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
package ca.on.mshri.lore.phenotype.yulab;

import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.Sparql;
import ca.on.mshri.lore.operations.util.URLParameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import ca.on.mshri.lore.phenotype.omim.Levenshtein;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import de.jweile.yogiutil.DoubleArrayList;
import de.jweile.yogiutil.LazyInitMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class YuLabParser extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    @Override
    public void run() {
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, getModel());
        
        Sparql sparql = Sparql.getInstance(YuLabParser.class.getProtectionDomain().getCodeSource());
        
        int diseaseId = 0;
        int entrezId = 1;
        int diseaseName = 2;
        int source = 3;
        
        LazyInitMap<String,Set<String>> diseaseNames = new LazyInitMap<String, Set<String>>(HashSet.class);
        LazyInitMap<String,Set<String>> gene2diseases = new LazyInitMap<String,Set<String>>(HashSet.class);
        
        URL parameterValue = getParameterValue(srcP);
        InputStream in = null;
        try {
            in = parameterValue.openStream();
            
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                if (lnum < 5) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                
                if (cols.length < 4) {
                    Logger.getLogger(YuLabParser.class.getName())
                            .log(Level.WARNING, "Invalid line: "+lnum);
                    continue;
                }
                
                diseaseNames.getOrCreate(cols[diseaseId]).add(cols[diseaseName]);
                gene2diseases.getOrCreate(cols[entrezId]).add(cols[diseaseId]);
                
//                List<Individual> genes = sparql.queryIndividuals(model, 
//                        "getGeneWithXRef", 
//                        "gene", 
//                        model.ENTREZ.getURI(), cols[entrezId]);
//                
//                if (genes.size() != 1) {
//                    Logger.getLogger(YuLabParser.class.getName())
//                            .log(Level.WARNING, "Inconsistent contents for gene "+cols[entrezId]);
//                    continue;
//                }
//                
//                Gene gene = Gene.fromIndividual(genes.get(0));
//                
//                double totalMaxScore = Double.NEGATIVE_INFINITY;
//                Phenotype maxPheno;
//                
//                NodeIterator phenoIt = gene.listPropertyValues(model.getProperty(PhenotypeModel.URI+"#isAssociatedWith"));
//                while (phenoIt.hasNext()) {
//                    
//                    Phenotype pheno = Phenotype.fromIndividual(phenoIt.next().as(Individual.class));
//                    
//                    double maxScore = Double.NEGATIVE_INFINITY;
//                    String maxLabel;
//                    
//                    ExtendedIterator<RDFNode> labelIt = pheno.listLabels(null);
//                    while (labelIt.hasNext()) {
//                        
//                        String label = labelIt.next().asLiteral().getString();
//                        double score = Levenshtein.score(label, cols[diseaseName]);
//                        
//                        if (score > maxScore) {
//                            maxScore = score;
//                            maxLabel = label;
//                        }
//                    }
//                    
//                    if (maxScore > totalMaxScore) {
//                        totalMaxScore = maxScore;
//                        maxPheno = pheno;
//                    }
//                    
//                    /*
//                     * Thinking of it, this is actually bullshit. There may be
//                     * multiple phenotype objects from either OMIM or HGMD that 
//                     * can match this, so need to do it the other way around:
//                     * For each phenotype in Lore, which YuLab disease does it most
//                     * closely match?
//                     */
//                }
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Error reading from "+parameterValue, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(YuLabParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream", ex);
            }
        }
        
        DoubleArrayList scores = new DoubleArrayList();
        
        for (Gene gene : model.listIndividualsOfClass(Gene.class, false)) {
            
            String entrez = gene.getXRefValue(model.ENTREZ);
            
            gene2diseases.get(entrez);
            
            if (!gene2diseases.containsKey(entrez)) {
                Logger.getLogger(YuLabParser.class.getName())
                        .log(Level.WARNING, "No entries for gene "+entrez);
                continue;
            }
            
            NodeIterator phenoIt = gene.listPropertyValues(model.getProperty(PhenotypeModel.URI+"#isAssociatedWith"));
            while (phenoIt.hasNext()) {
                Phenotype pheno = Phenotype.fromIndividual(phenoIt.next().as(Individual.class));
                
                double maxScore = -1;
                String maxDisease = null;
                
                for (String disId : gene2diseases.get(entrez)) {
                    
                    double score = score(diseaseNames.get(disId), pheno);
                    if (score > maxScore) {
                        maxScore = score;
                        maxDisease = disId;
                    }
                }
                
                scores.add(maxScore);
                
            }
        }
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private double score(Set<String> disNames, Phenotype pheno) {
        double disVsPhenoScore = -1;
                    
        for (String disName : disNames) {

            double maxScore = -1;

            ExtendedIterator<RDFNode> labelIt = pheno.listLabels(null);
            while (labelIt.hasNext()) {
                String label = labelIt.next().asLiteral().getString();
                if (label.equals(disName)) {
                    maxScore = 1;
                } else {
                    double score = Levenshtein.score(label, disName);
                    if (score > maxScore) {
                        maxScore = score;
                    }
                }
            }

            if (maxScore > disVsPhenoScore) {
                disVsPhenoScore = maxScore;
            }
        }
        
        return disVsPhenoScore;
                    
    }
    
}
