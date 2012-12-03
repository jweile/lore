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

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.hpo.model.HpoOntModel;
import ca.on.mshri.lore.hpo.model.Phenotype;
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
        
        Authority hpo = Authority.createOrGet(model, "HPO");
        Authority entrez = Authority.createOrGet(model, "Entrez");
        
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
                Phenotype phenotype = Phenotype.createOrGet(model, hpo, termId);
                if (phenotype.getName() == null) {
                    Logger.getLogger(AnnotationParser.class.getName())
                            .log(Level.WARNING, "Phenotype "+termId+" unknown. Initializing...");
                    phenotype.setName(termName);
                }
                
                //get or create the genes
                String[] geneStrs = cols[1].substring(1, cols[1].length()-1).split(", ");
                for (String geneStr : geneStrs) {
                    
                    Matcher geneMatcher = pattern.matcher(geneStr);
                    assert(geneMatcher.find());
                    String geneName = geneMatcher.group(1);
                    String geneId = geneMatcher.group(2);
                    
                    Gene gene = Gene.createOrGet(model, entrez, geneId);
//                    gene.setName(geneName);
                    
                    //connect the gene with the phenotype
                    gene.addProperty(model.getProperty(HpoOntModel.URI+"isAssociatedWith"), phenotype);
                    
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
