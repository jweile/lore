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
package ca.on.mshri.lore.hpo;

import ca.on.mshri.lore.hpo.model.Gene;
import ca.on.mshri.lore.hpo.model.HpoOntModel;
import ca.on.mshri.lore.hpo.model.Phenotype;
import ca.on.mshri.lore.hpo.model.XRef;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
final class AnnotationParser {

    private HpoOntModel model;
    
    private static final Pattern pattern = Pattern.compile("(.*)\\((.*)\\)");

    public AnnotationParser(HpoOntModel model) {
        this.model = model;
    }
    
    
    void parse(InputStream annoStream) {
        
        BufferedReader b = new BufferedReader(new InputStreamReader(annoStream));
        
        try {
            
            String line; int lnum = 0;
            while ((line = b.readLine()) != null) {

                lnum++;
                
                //skip comments
                if (line.startsWith("#")) {
                    continue;
                }
                
                String[] cols = line.split("\t");
                assert(cols.length == 2);
                Matcher termMatcher = pattern.matcher(cols[0]);
                assert(termMatcher.find());
                String termName = termMatcher.group(1);
                String termId = termMatcher.group(2);
                
                //get or create the phenotype
                String phenoURI = HpoOntModel.HPO+termId;
                Phenotype phenotype = model.getPhenotype(phenoURI);
                if (phenotype == null) {
                    phenotype = model.createPhenotype(phenoURI);
                }
                if (phenotype.getName() == null) {
                    Logger.getLogger(AnnotationParser.class.getName())
                            .log(Level.WARNING, "Phenotype "+termId+"unknown. Initializing...");
                    phenotype.setName(termName);
                }
                
                //get or create the genes
                String[] geneStrs = cols[1].substring(1, cols[1].length()-1).split(", ");
                for (String geneStr : geneStrs) {
                    
                    Matcher geneMatcher = pattern.matcher(geneStr);
                    assert(geneMatcher.find());
                    String geneName = geneMatcher.group(1);
                    String geneId = geneMatcher.group(2);
                    
                    String geneURI = HpoOntModel.ENTREZ+geneId;
                    Gene gene = model.getGene(geneURI);
                    if (gene == null) {
                        gene = model.createGene(geneURI);
                    }
                    gene.setName(geneName);
                    String xrefURI = HpoOntModel.HPO+"EntrezGeneId";
                    XRef xref = model.getXRef(xrefURI,geneId);
                    if (xref == null) {
                        xref = model.createXRef(xrefURI, geneId);
                    }
                    //FIXME: for now this NS individual is defined in the owl file, 
                    //but should actually be in some kind of base module.
                    gene.addXRef(xref);
                    
                    //connect the gene with the phenotype
                    gene.addProperty(model.getProperty(HpoOntModel.HPO+"isAssociatedWith"), phenotype);
                    
                }
                
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read annotation stream!");
        } finally {
            try {
                b.close();
            } catch (IOException ex) {
                Logger.getLogger(AnnotationParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream reader!", ex);
            }
        }
        
    }
    
}
