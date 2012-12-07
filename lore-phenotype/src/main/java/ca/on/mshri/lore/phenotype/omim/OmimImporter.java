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
package ca.on.mshri.lore.phenotype.omim;

import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliIndeterminateProgress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OmimImporter {
    
    Properties resources;
    
    Map<String,Gene> genes = new HashMap<String, Gene>();
    
    Map<String,Phenotype> phenos = new HashMap<String, Phenotype>();

    public OmimImporter() {
        
        loadProperties();
        
    }
    
    public void parse(PhenotypeModel model) {
                
        parseMim2Gene(model, openStream("mim2gene"));
        
        parseMorbidMap(model, openStream("morbidmap"));
        
        OmimFulltextParser p = new OmimFulltextParser();
        p.parse(model, openStream("omim"));
        
        p.linkAlleles(model);
        
        
    }

    void parseMim2Gene(PhenotypeModel model, InputStream in) {
                
        //column indices
        int mim = 0;
        int type = 1;
        int entrez = 2;
        int symbol = 3;
        
        BufferedReader r = null;
        try {
            
            r = new BufferedReader(new InputStreamReader(in));
                        
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                
                if (lnum == 1) {
                    continue;//skip title line
                }
                
                String[] cols = line.split("\t");
                
                if (cols.length < 4) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "Malformatted line in mim2gene.txt: "+lnum);
                    continue;
                }
                
                if (cols[type].equals("phenotype")) {
                    
                    //create phenotype
                    Phenotype pheno = Phenotype.createOrGet(model, model.OMIM, cols[mim]);
                    phenos.put(cols[mim],pheno);
                    
                } else if (cols[type].equals("gene")) {
                    
                    //create gene
                    Gene gene = Gene.createOrGet(model, model.OMIM, cols[mim]);
                    genes.put(cols[mim],gene);
                    
                    if (!cols[entrez].equals("-")) {
                        gene.addXRef(model.ENTREZ, cols[entrez]);
                    }
                    
                    if (!cols[symbol].equals("-")) {
                        gene.addXRef(model.HGNC, cols[symbol]);
                        gene.addLabel(model.createLiteral(cols[symbol]));
                    }
                    
                } else if (cols[type].equals("gene/phenotype")) {
                    
                    //create phenotype
                    Phenotype pheno = Phenotype.createOrGet(model, model.OMIM, cols[mim]);
                    phenos.put(cols[mim],pheno);
                                        
                    //create gene
                    Gene gene = Gene.createOrGet(model, model.OMIM, cols[mim]);
                    genes.put(cols[mim],gene);
                    
                    if (!cols[entrez].equals("-")) {
                        gene.addXRef(model.ENTREZ, cols[entrez]);
                    }
                    
                    if (!cols[symbol].equals("-")) {
                        gene.addXRef(model.HGNC, cols[symbol]);
                        gene.addLabel(cols[symbol], null);
                    }
                    
                } else {
                    //ignore
                }
                
                progress.next("Parsing stream");
            }
            
            progress.done();
            
            
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Incorrect URL for mim2gene",ex);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read mim2gene",ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "Cannont close stream", ex);
                }
            }
        }
        
    }

    private void loadProperties() {
        resources = new Properties();
        InputStream in = OmimImporter.class.getResourceAsStream("OMIM.properties");
        try {
            resources.load(in);
            in.close();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load OMIM parser properties");
        }
    }
    
    private static final Pattern NAME_MIM_METH = Pattern.compile("^(.+), (\\d{6}) \\((\\d)\\)$");

    void parseMorbidMap(PhenotypeModel model, InputStream in) {
        
        Property association = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        
        int name_mim_meth = 0;
        int symbols = 1;
        int gene_mim = 2;
        int locus = 3;
        
        BufferedReader r = null;
        try {
                        
            r = new BufferedReader(new InputStreamReader(in));
                        
            CliIndeterminateProgress progress = new CliIndeterminateProgress();
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                String[] cols = line.split("\\|");
                
                if (cols.length < 4) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "Malformatted line in morbidmap: "+lnum);
                    continue;
                }
                
                //extract name, mim id and number
                String name = null, phenoMim = null, method = null;
                Matcher nmnMatcher = NAME_MIM_METH.matcher(cols[name_mim_meth]);
                if (nmnMatcher.find()) {
                    name = nmnMatcher.group(1);
                    phenoMim = nmnMatcher.group(2);
                    method = nmnMatcher.group(3);
                } else {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "Skipping morbidmap entry without ID in line "+lnum);
                    continue;
                }
                
                if (!method.equals("3")) {
                    Logger.getLogger(OmimImporter.class.getName()).log(Level.FINE, "Skipping entry without molecular basis");
                    continue;
                }
                
                //extract acronyms
                String[] geneSymbols = cols[symbols].split(", ");
                
                //extract gene mim id
                String geneMim = cols[gene_mim];
                
                //get gene and phenotype elements
                Gene gene = genes.get(geneMim);
                if (gene == null) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "MorbidMap references unknown gene in line "+lnum);
                    continue;
                }
                Phenotype pheno = phenos.get(phenoMim);
                if (pheno == null) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "MorbidMap references unknown phenotype in line "+lnum);
                    continue;
                }
                pheno.addLabel(name,null);
                
                //create link
                gene.addProperty(association, pheno);
                
                progress.next("Parsing stream");
            }
            
            progress.done();
            
            
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Incorrect URL for mim2gene",ex);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read mim2gene",ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, "Cannont close stream", ex);
                }
            }
        } 
    }

    private InputStream openStream(String key) {
        
        try {
            
            URL url = new URL(resources.getProperty(key));

            Logger.getLogger(OmimImporter.class.getName()).log(Level.INFO, "Connecting to "+url);

            InputStream in = url.openStream();

            return in;
            
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Incorrect URL for mim2gene",ex);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read mim2gene",ex);
        } 
            
    }
}
