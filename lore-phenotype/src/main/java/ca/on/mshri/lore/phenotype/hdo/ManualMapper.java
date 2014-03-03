/*
 * Copyright (C) 2014 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.phenotype.hdo;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.URLParameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.LazyInitMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ManualMapper extends LoreOperation {

    public final URLParameter srcP = new URLParameter("src");
    
    @Override
    public void run() {
        
        Logger.getLogger(ManualMapper.class.getName())
                .log(Level.INFO, "Starting manual mapper");
        
        URL src = getParameterValue(srcP);
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, getModel());
        Property is_a = model.getProperty(PhenotypeModel.URI+"#is_a");
        
        int entity = 0;
        int name = 1;
        int doID = 2;
        int doName = 3;
        
        
        Map<String, Set<Phenotype>> nameIndex = indexNames(model);
        Map<String, Phenotype> doidIndex = indexDOID(model);
        
        Logger.getLogger(ManualMapper.class.getName()).log(Level.INFO, "Mapping...");
        
        BufferedReader r = null;
        
        try {
            r = new BufferedReader(new InputStreamReader(src.openStream()));
            
            String line; int lnum = 0;
            while ((line = r.readLine()) != null) {
                lnum++;
                
                if (lnum == 1) {
                    //skip title line
                    continue;
                }
                String[] cols = line.split("\t");
                
                if (cols.length < 4) {
                    Logger.getLogger(ManualMapper.class.getName())
                            .log(Level.WARNING, "Skipping broken line "+lnum);
                    continue;
                }
                
                Set<Phenotype> children = nameIndex.get(cols[name]);
                if (children == null || children.isEmpty()) {
                    Logger.getLogger(ManualMapper.class.getName())
                            .log(Level.WARNING, "No nodes for "+cols[name]);
                    continue;
                }
                if (children.size() > 1) {
                    Logger.getLogger(ManualMapper.class.getName())
                            .log(Level.WARNING, "Multiple child nodes found for "+cols[name]);
                }
                
                int offset = 0;
                while (cols.length > doName+offset) {
                    String[] doid = cols[doID+offset].split(":");
                    offset += 2;
                    Phenotype parent = doidIndex.get(doid[1]);
                    if (parent == null) {
                        Logger.getLogger(ManualMapper.class.getName()).log(Level.WARNING, "Parent not found: "+doid[1]);
                        continue;
                    }
                    for (Phenotype child : children) {
                        if (!child.equals(parent)) {
                            child.addProperty(is_a, parent);
                        }
                    }
                }
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read stream from "+src);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(ManualMapper.class.getName())
                            .log(Level.WARNING, "Unable to close stream!", ex);
                }
            }
        }
        
        Logger.getLogger(ManualMapper.class.getName()).log(Level.INFO, "Done!");
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    private Map<String,Set<Phenotype>> indexNames(PhenotypeModel model) {
        
        Logger.getLogger(ManualMapper.class.getName()).log(Level.INFO, "Indexing names...");
        
        LazyInitMap<String,Set<Phenotype>> index = new LazyInitMap<String,Set<Phenotype>>(HashSet.class);
        
        
        List<Phenotype> phenos = model.listIndividualsOfClass(Phenotype.class, true);
        CliProgressBar pb = new CliProgressBar(phenos.size());
        for (Phenotype pheno : phenos) {
            ExtendedIterator<RDFNode> labIt = pheno.listLabels(null);
            while (labIt.hasNext()) {
                String label = labIt.next().asLiteral().getString();
                index.getOrCreate(label).add(pheno);
            }
            pb.next();
        }
        
        return index;
    }

    private Map<String,Phenotype> indexDOID(PhenotypeModel model) {
        
        Logger.getLogger(ManualMapper.class.getName()).log(Level.INFO, "Indexing DOIDs...");
        
        Authority doAuth = Authority.createOrGet(model,"DO");
        
        Map<String,Phenotype> index = new HashMap<String,Phenotype>();
        
        List<Phenotype> phenos = model.listIndividualsOfClass(Phenotype.class, true);
        CliProgressBar pb = new CliProgressBar(phenos.size());
        for (Phenotype pheno : phenos) {
            for (XRef xref :pheno.listXRefs()) {
                if (xref.getAuthority().equals(doAuth)) {
                    String doid = xref.getValue();
                    index.put(doid,pheno);
                }
            }
            pb.next();
        }
        
        return index;
    }
    
}
