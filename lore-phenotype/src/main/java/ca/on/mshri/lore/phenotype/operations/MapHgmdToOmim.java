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
package ca.on.mshri.lore.phenotype.operations;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.Merger;
import ca.on.mshri.lore.operations.util.Parameter;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import de.jweile.yogiutil.LazyInitMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Maps HGMD diseases to OMIM diseases using full-text matching.
 * 
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class MapHgmdToOmim extends LoreOperation {

    /**
     * score threshold for mappings. Defaults to 1.8
     */
    private double threshold;
    public final Parameter<Double> scoreThresholdP = Parameter.make("scoreThreshold", Double.class, 1.8);
    
    /**
     * file to which result table is written.
     */
    private String outFile;
    public final Parameter<String> outFileP = Parameter.make("outFile", String.class, "hgmd2omim.txt");
    
    /**
     * run operation.
     */
    @Override
    public void run() {
        
        threshold = getParameterValue(scoreThresholdP);
        outFile = getParameterValue(outFileP);
        
        Logger.getLogger(MapHgmdToOmim.class.getName())
                .log(Level.INFO, "Using threshold "+threshold);
        
        PhenotypeModel model = new PhenotypeModel(OntModelSpec.OWL_DL_MEM, getModel());
        
        //build index over OMIM
        ///////////////////////
        Logger.getLogger(MapHgmdToOmim.class.getName())
                .log(Level.INFO, "Indexing OMIM diseases...");
        Map<Long,Phenotype> doc2pheno = new HashMap<Long, Phenotype>();
        
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        Directory searchIndex = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        IndexWriter w = null;
        try {
            long id = 0;
            w = new IndexWriter(searchIndex, config);
            for (Phenotype pheno : model.listIndividualsOfClass(Phenotype.class, true)) {
                
                if (pheno.getXRefValue(model.OMIM) == null) {
                    //discard non-omim phenotype
                    continue;
                }
                                
                //iterate over phenotype names
                for (String diseaseName : listLabels(pheno)) {
                    Document doc = new Document();
                    doc.add(new TextField("name", diseaseName, Field.Store.YES));
                    doc.add(new LongField("id", id, Field.Store.YES));
                    w.addDocument(doc);
                    doc2pheno.put(id,pheno);
                    id++;
                }
                

            }
        } catch (IOException ex) {
            throw new RuntimeException("Error writing index.",ex);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    Logger.getLogger(MapHgmdToOmim.class.getName())
                            .log(Level.WARNING, "Cannot close RAM index writer", ex);
                }
            }
        }
        
        Logger.getLogger(MapHgmdToOmim.class.getName()).log(Level.INFO, "Searching HGMD diseases against index...");
        Collection<Set<Individual>> mergeSets = new ArrayList<Set<Individual>>();
        String tabOut = searchAgainstIndex(searchIndex, model, analyzer, doc2pheno, mergeSets);
        writeResults(tabOut);
        
        Merger merger = new Merger();
        merger.setModel(model);
        merger.setParameter(merger.mergeSetsP, mergeSets);
        merger.run();
        
    }
    
    /**
     * list all the labels of the given phenotype
     * @param pheno
     * @return 
     */
    private List<String> listLabels(Phenotype pheno) {
        List<String> out = new ArrayList<String>();
        
        ExtendedIterator<RDFNode> labelIt = pheno.listLabels(null); 
        while (labelIt.hasNext()) {
            String diseaseName = labelIt.next().asLiteral().getString();
            out.add(diseaseName);
        }
        return out;
    }
    
    /**
     * This operation does not require reasoner support.
     * @return false
     */
    @Override
    public boolean requiresReasoner() {
        return false;
    }

    /**
     * writes the result table to file.
     */
    private void writeResults(String tabOut) throws RuntimeException {
        //write results to file.
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFile));
            bw.write(tabOut);
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot write to file.",ioe);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(MapHgmdToOmim.class.getName())
                            .log(Level.WARNING, "Unable to close stream.", ex);
                }
            }
        }
    }

    /**
     * get all HMGD diseases and search them against the index.
     * @param searchIndex
     * @param model
     * @param analyzer
     * @param doc2pheno
     * @return 
     */
    private String searchAgainstIndex(Directory searchIndex, PhenotypeModel model, 
                StandardAnalyzer analyzer, Map<Long, Phenotype> doc2pheno,
                Collection<Set<Individual>> mergeSets) {
        
        IndexReader reader = null;
        Authority hgmdPhenoAuth = Authority.createOrGet(model, "HGMD-Disease");
        
        QueryParser queryParser = new QueryParser(Version.LUCENE_46, "name", analyzer);
        
        LazyInitMap<Phenotype,Set<Individual>> mergeIndex = new LazyInitMap<Phenotype,Set<Individual>>(HashSet.class);
        
        //output table
        StringBuilder tabOut = new StringBuilder();
        try {
            reader = DirectoryReader.open(searchIndex);
            IndexSearcher searcher = new IndexSearcher(reader);
            
            for (Phenotype pheno : model.listIndividualsOfClass(Phenotype.class, true)) {

                if (pheno.getXRefValue(hgmdPhenoAuth) == null) {
                    //discard non-omim phenotype
                    continue;
                }
                
                //this variable will contain the top-scoring hit
                ScoreDoc topHit = null;
                //for each name, find the best match
                for (String diseaseName : listLabels(pheno)) {

                    if (diseaseName == null || diseaseName.length() == 0) {
                        continue;
                    }
                    
                    //create search query
                    Query q = null;
                    try {
                        q = queryParser.parse(QueryParser.escape(diseaseName));
                    } catch (ParseException ex) {
                        Logger.getLogger(MapHgmdToOmim.class.getName())
                                .log(Level.WARNING, "Cannot query \""+diseaseName+"\"");
                        continue;
                    }
                    
                    //run search
                    TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
                    searcher.search(q, collector);
                    ScoreDoc[] hits = collector.topDocs().scoreDocs;
                    if (hits.length == 0) {
                        continue;
                    }
                    //replace top hit if current hit has better score
                    if (topHit == null || topHit.score < hits[0].score) {
                        topHit = hits[0];
                    }
                }
                
                //if there was no (or no good) match, we're done here.
                if (topHit == null || topHit.score < threshold) {
                    Logger.getLogger(MapHgmdToOmim.class.getName())
                            .log(Level.WARNING, "No hits for \""+pheno.getLabel(null) +"\"");
                    tabOut.append(listLabels(pheno).toString()).append("\tNA\tNA\n");
                    continue;
                }
                
                //otherwise get the top phenotype
                Document topDoc = searcher.doc(topHit.doc);
                Phenotype phenoHit = doc2pheno.get(Long.parseLong(topDoc.get("id")));
                if (phenoHit == null) {
                    Logger.getLogger(MapHgmdToOmim.class.getName())
                            .log(Level.SEVERE, "Unresolvable document ID!");
                    continue;
                }
                
                mergeIndex.getOrCreate(pheno).add(phenoHit);
                
                tabOut.append(listLabels(pheno).toString()).append('\t')
                      .append(topDoc.get("name")).append('\t')
                      .append(topHit.score).append('\n');
                
                
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    // reader can only be closed when there
                    // is no need to access the documents any more.
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(MapHgmdToOmim.class.getName())
                            .log(Level.WARNING, ex.getMessage(), ex);
                }
            }
        }
        
        //add the actual OMIM node to its own merge set
        for (Phenotype pheno : mergeIndex.keySet()) {
            mergeIndex.get(pheno).add(pheno);
        }
        mergeSets.addAll(mergeIndex.values());
        
        return tabOut.toString();
    }
    
}
