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

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.operations.Sparql;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.tdb.TDBFactory;
import de.jweile.yogiutil.MainWrapper;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class ImportOmim {
    
    public static void main(String[] args) {
        
        MainWrapper w = new MainWrapper() {

            @Override
            public void run(String[] args) {
                //check arguments and complain if necessary
                if (args.length < 1) {
                    throw new RuntimeException(
                            "Usage: java -jar lore-phenotype.jar <tdbLocation>"
                            );
                }
                                
                File tdbFile = new File(args[0]);
                if (!tdbFile.exists()) {
                    Logger.getLogger(ImportOmim.class.getName())
                            .log(Level.WARNING, "TDB location " + args[0] + 
                            " does not exist. Creating new database."
                            );
                }
                
                //start the import process
                new ImportOmim().run(tdbFile);
            }
            
        };
        w.setLogFileName("import-omim.log");
        w.start(args);
        
    }

    private void run(File tdbFile) {
        
        Dataset tdbSet = null;
        try {
            
            tdbSet = TDBFactory.createDataset(tdbFile.getAbsolutePath());
            
            PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_MEM, tdbSet.getDefaultModel());
            
//            OmimImporter importer = new OmimImporter();
//            importer.parse(model);
            
            query(model);
            
        } finally {
            if (tdbSet != null) {
                tdbSet.close();
            }
        }
        
    }

    private void query(PhenotypeModel model) {
        Sparql sparql = Sparql.getInstance(ImportOmim.class.getProtectionDomain().getCodeSource());
        
//        QueryExecution qry = QueryExecutionFactory.create(sparql.get("geneXRefs"), model);
//        ResultSet result = qry.execSelect();
//        while (result.hasNext()) {
//            QuerySolution sol = result.next();
//            Gene gene = Gene.fromIndividual(sol.get("gene").as(Individual.class));
//            XRef xref = XRef.fromIndividual(sol.get("xref").as(Individual.class));
////            Authority ns = Authority.fromIndividual(sol.get("ns").as(Individual.class));
////            String id = sol.get("id").asLiteral().getString();
//            System.out.println(
//                    gene.getLabel(null)+"\t"
//                    +xref.getValue()
////                    +id
//            );
//            
//        }
        
//        for (Gene gene : model.listIndividualsOfClass(Gene.class, true)) {
//            for (XRef xref : gene.listXRefs()) {
//                if (xref.getAuthority().equals(model.ENTREZ)) {
//                    System.out.println(gene.getLabel(null)+"\t"+xref.getValue());
//                }
//            }
//        }
        
        QueryExecution qry = QueryExecutionFactory.create(sparql.get("geneAlleleDisease"), model);
        ResultSet result = qry.execSelect();
        while (result.hasNext()) {
            QuerySolution sol = result.next();
            Gene gene = Gene.fromIndividual(sol.get("gene").as(Individual.class));
            String id = sol.get("id").asLiteral().getString();
            Allele allele = Allele.fromIndividual(sol.get("allele").as(Individual.class));
            Phenotype disease = Phenotype.fromIndividual(sol.get("disease").as(Individual.class));
            System.out.println(
                    gene.getLabel(null)+"\t"
                    +id+"\t"
                    +allele.getURI().substring(16) +"\t"
                    +disease.getLabel(null)
            );
            
            
        }
    }
    
}
