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

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.XRef;
import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.Mutation;
import ca.on.mshri.lore.genome.PointMutation;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.rdf.model.Property;
import de.jweile.yogiutil.CliIndeterminateProgress;
import de.jweile.yogiutil.CliProgressBar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OmimFulltextParser {
    
    /**
     * indexes phenotypes under their various names.
     */
    private Map<String,Phenotype> phenotypeIndex = new HashMap<String, Phenotype>();
    
    /**
     * links allele URIs to variant objects corresponding to that allele.
     */
    private Map<String,Variant> allele2variant = new HashMap<String, Variant>();
    
    private Property inheritance;
        
    /**
     * Starts the parser.
     * @param model the model
     * @param in inputstream for the omim fulltext file.
     */
    public void parse(PhenotypeModel model, InputStream in) {
        
        inheritance = model.getProperty(PhenotypeModel.URI+"#inheritanceMode");
        
        Record currRecord = null;
        StringBuilder currField = null;
        String fieldType = null;
        
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        String line; int lnum = 0;
        
        CliIndeterminateProgress progress = new CliIndeterminateProgress();
        
        try {
            
            while ((line = b.readLine()) != null) {
                lnum++;
                
                if (line.trim().equals("*RECORD*")) {
                    //terminate any old field field and process any old record
                    if (currRecord != null) {
                        writeFieldToRecord(currRecord, fieldType, currField.toString());
                        processRecord(model, currRecord);
                    }
                    //open new record
                    currRecord = new Record();
                }
                
                else if (line.startsWith("*FIELD*")) {
                    //terminate any old field
                    if (currField != null) {
                        writeFieldToRecord(currRecord, fieldType, currField.toString());
                    }
                    //open new field
                    fieldType = line.substring(8);
                    currField = new StringBuilder();
                }
                
                else {
                    currField.append(line).append("\n");
                }
                
                progress.next("Parsing");
                
            }
            
            //terminate last field and process last record
            if (currRecord != null) {
                writeFieldToRecord(currRecord, fieldType, currField.toString());
                processRecord(model, currRecord);
            }
            
            progress.done();
            
        } catch (Exception ex) {
            throw new RuntimeException("Error while reading omim.txt:"+lnum, ex);
        } finally {
            try {
                b.close();
            } catch (IOException ex) {
                Logger.getLogger(OmimImporter.class.getName())
                        .log(Level.WARNING, "Unable to close stream reader.", ex);
            }
        }
        
    }
    
    /**
     * Iterates through parsed alleles and tries to link them with their associated 
     * disease entries, if necessary using inexact string matching.
     * 
     * @param model 
     */
    public void linkAlleles(PhenotypeModel model) {
        
        Property association = model.getProperty(PhenotypeModel.URI+"#isAssociatedWith");
        
        CliProgressBar pb = new CliProgressBar(allele2variant.keySet().size());
        
        for (String alleleUri : allele2variant.keySet()) {
            
            Allele allele = Allele.fromIndividual(model.getIndividual(alleleUri));
            
            Variant variantInfo = allele2variant.get(alleleUri);
            
            for (String phenoName : variantInfo.getDiseaseNames()) {
                
                Phenotype pheno = phenotypeIndex.get(phenoName);
                
                //if no entry is known for that name, try inexact search
                if (pheno == null) {
                    double bestScore = 0.0;
                    String bestHit = null;
                    for (String key : phenotypeIndex.keySet()) {
                        if (key == null || key.length() == 0) {//FIXME: this shouldn't be happening!
                            continue;
                        }
                        double score = Levenshtein.score(phenoName, key);
                        if (score > .7 && score > bestScore) {
                            bestScore = score;
                            bestHit = key;
                        }
                    }
                    
                    if (bestHit != null) {
                        pheno = phenotypeIndex.get(bestHit);
                        Logger.getLogger(OmimFulltextParser.class.getName())
                                .log(Level.WARNING, "Inexact match: Assigning \""
                                +phenoName+"\" to \""+bestHit+"\"");
                    }
                    
                }
                
                if (pheno != null) {
                    allele.addProperty(association, pheno);
                } else {
                    Logger.getLogger(OmimFulltextParser.class.getName())
                            .log(Level.WARNING, "Unknown phenotype: "+phenoName);
                }
            }
            
            pb.next();
            
        }
                
    }
    
    
    
    
    /**
     * Pattern for the id and name line
     */
    private static final Pattern tiPattern = Pattern.compile("([\\+\\*%#]?\\d{6}) ([^;]+)(;(.*))?");
    
    private void writeFieldToRecord(Record record, String fieldType, String fieldValue) {
        
        if (fieldValue != null) {
            
            //OMIM ID field
            if (fieldType.equals("NO")) {
                record.setId(fieldValue.trim());
            } 
            
            //type, id and names
            else if (fieldType.equals("TI")) { 
                
                //type
                if (fieldValue.startsWith("*") || fieldValue.startsWith("+")) {//type: gene
                    record.setType(Type.GENE);
                } else if (fieldValue.startsWith("%") || fieldValue.startsWith("#")) {//type: phenotype
                    record.setType(Type.PHENOTYPE);
                } else if (fieldValue.startsWith("^")) {//outdated record
                    record.setDeprecated(true);
                } else {//type unconfirmed phenotype
                    record.setType(Type.PHENOTYPE);
                }
                
                //names
                String[] names = fieldValue.split("\n|;;");
                Matcher tiMatcher = tiPattern.matcher(names[0].trim());
                if (tiMatcher.find() && tiMatcher.groupCount() > 1 
                        && tiMatcher.group(2) != null && tiMatcher.group(2).length() > 0) {
                    String name = tiMatcher.group(2);
                    //process primary name
                    record.addName(name);

                    if (tiMatcher.groupCount() == 4
                            && tiMatcher.group(4) != null && tiMatcher.group(4).length() > 0) {
                        name = tiMatcher.group(4);
                        //process symbolic name
                        record.addName(name);
                    }
                } else {
                    Logger.getLogger(OmimImporter.class.getName())
                            .log(Level.WARNING, names[0].trim()+" does not contain record name");
                }
                
                //process synonyms
                for (int i = 1; i < names.length; i++) {
                    String name = names[i].trim();
                    if (name.length() == 0) {
                        continue;
                    }
                    //save name
                    record.addName(name);
                }
            }
            
            //allelic variants
            else if (fieldType.equals("AV")) {
                
                boolean insideList = true;
                Variant currVariant = null;
                List<String> diseaseNames = null;
                
                for (String line : fieldValue.split("\n")) {
                    
                    //id line indicates start of new variant
                    if (line.matches("\\.\\d{4}")) {
                        if (currVariant != null) {
                            record.addVariant(currVariant);
                        }
                        currVariant = new Variant();
                        currVariant.setId(line.trim());
                        //prepare to read disease names
                        insideList = true;
                        diseaseNames = new ArrayList<String>();
                    } 
                    else {
                        if (insideList) {
                            //if we encounter an empty line, we have already read beyond the disease list
                            if (line.trim().length() == 0) {
                                //stop reading
                                insideList = false;
                                //stupidly the last entry in the list is not a disease, but the mutant description
                                String mutantDescription = diseaseNames.get(diseaseNames.size()-1);
                                String[] mdParts = mutantDescription.split(", ");
                                //so we have to remove it
                                diseaseNames.remove(diseaseNames.size()-1);
                                //then we can set the proper content for our object.
                                currVariant.setDiseaseNames(diseaseNames);
                                currVariant.setMutation(mdParts.length > 1 ? mdParts[1] : mutantDescription);
                            } else {
                                diseaseNames.add(line);
                            }
                        } 
                    }
                }
            }
            
        }
    }
    
    private static String[] postprocessName(String s) {
        String[] split = s.split(";");
        
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].replace(", INCLUDED", "").trim();
        }
        
        return split;
    }
    
    private static final Pattern inhPattern = Pattern.compile(
            "autosomal dominant|autosomal recessive|autosomal|"
            + "x-linked dominant|x-linked recessive|x-linked|"
            + "y-linked|dominant|recessive");
    
    private static String extractInheritance(String name) {
        String[] split = name.split(",");
        for (String part : split) {
            Matcher matcher = inhPattern.matcher(part.toLowerCase());
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private static final Pattern snpPattern = Pattern.compile("\\w{3}\\d+\\w{3}");
    private static final Pattern dbSnpPattern = Pattern.compile("\\(dbSNP (\\w+\\d+)\\)");
    
    private void processRecord(PhenotypeModel model, Record record) {
        
        if (record.getType() == Type.GENE) {
            
            Gene gene = Gene.createOrGet(model, model.OMIM, record.getId());
            
            //add names
            for (String name : record.getNames()) {
                gene.addLabel(name, null);
            }
            
            //add variants
            for (Variant var : record.getVariants()) {
                
                if (var.getId() == null || var.getMutation() == null) {
                    continue;
                }
                
                String id = record.getId()+var.getId();
                Allele allele = Allele.createOrGet(model, model.OMIM, id);
                allele.setGene(gene);
                
                Matcher snpMatcher = snpPattern.matcher(var.getMutation());
                if (snpMatcher.find()) {
                    String mutSignature = snpMatcher.group();
                    try {
                        PointMutation mut = PointMutation.createOrGet(model, allele, mutSignature);
                        allele.addMutation(mut);
                        Matcher xrefMatcher = dbSnpPattern.matcher(var.getMutation());
                        if (xrefMatcher.find()) {
                            XRef xref = XRef.createOrGet(model, model.DB_SNP, xrefMatcher.group(1));
                            mut.addProperty(model.getProperty(LoreModel.URI+"#hasXRef"), xref);
                        }
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(OmimFulltextParser.class.getName()).log(Level.WARNING, "Invalid mutation description!\n"+ex.getMessage());
                    }
                } 
                
                
                allele2variant.put(allele.getURI(), var);
            }
            
        } else if (record.getType() == Type.PHENOTYPE) {
            
            Phenotype pheno = Phenotype.createOrGet(model, model.OMIM, record.getId());
            
            //add names
            for (String name : record.getNames()) {
                pheno.addLabel(name, null);
                
                //index under each name
                phenotypeIndex.put(name, pheno);
            }
            
            //add inheritance modes
            for (String inh : record.getInheritance()) {
                pheno.addProperty(inheritance, inh);
            }
            
            
        }
        
        
    }
    
//    /**
//     * For sorting diseases by score
//     */
//    private static class DiseaseScore implements Comparable<DiseaseScore>{
//        
//        private String id;
//        private double score;
//
//        public DiseaseScore(String id, double score) {
//            this.id = id;
//            this.score = score;
//        }
//
//        public String getId() {
//            return id;
//        }
//
//        public double getScore() {
//            return score;
//        }
//        
//        @Override
//        public int compareTo(DiseaseScore t) {
//            //sort DECREASING by score
//            if (score < t.getScore()) {
//                return 1;
//            } else if (score > t.getScore()) {
//                return -1;
//            } else {
//                return 0;
//            }
//        }
//        
//    }
    
    
    
    private static enum Type {
        GENE, PHENOTYPE
    }
    
    private static class Record {
        
        private String id;
        
        private Type type;
        
        private List<String> names;
        
        private List<Variant> variants;
        
        private boolean deprecated = false;
        
        private List<String> inheritanceModes;
        

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public List<String> getNames() {
            if (names == null) {
                return Collections.EMPTY_LIST;
            }
            return names;
        }
        
        public void addName(String s) {
            if (names == null) {
                names = new ArrayList<String>();
            }
            for (String subname : postprocessName(s)) {
                String inheritance = extractInheritance(subname);
                if (inheritance != null) {
                    if (inheritanceModes == null) {
                        inheritanceModes = new ArrayList<String>();
                    }
                    inheritanceModes.add(inheritance);
                }
                names.add(subname);
            }
        }
        
        public List<String> getInheritance() {
            if (inheritanceModes == null) {
                return Collections.EMPTY_LIST;
            }
            return inheritanceModes;
        }
        
        public List<Variant> getVariants() {
            if (variants == null) {
                return Collections.EMPTY_LIST;
            }
            return variants;
        }
        
        public void addVariant(Variant v) {
            if (variants == null) {
                variants = new ArrayList<Variant>();
            }
            variants.add(v);
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + (this.id != null ? this.id.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Record other = (Record) obj;
            if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
                return false;
            }
            return true;
        }
        
        
        
    }
    
    private static class Variant {
        private String id;
        private List<String> diseaseNames;
        private String mutation;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getDiseaseNames() {
            if (diseaseNames == null) {
                return Collections.EMPTY_LIST;
            }
            return diseaseNames;
        }

        public void setDiseaseNames(List<String> diseaseNames) {
            List<String> extendedList = new ArrayList<String>();
            //clean up disease names first
            for (String name : diseaseNames) {
                for (String subname : postprocessName(name)) {
                    extendedList.add(subname);
                }
            }
            this.diseaseNames = extendedList;
        }

        public String getMutation() {
            return mutation;
        }

        public void setMutation(String mutation) {
            this.mutation = mutation;
        }
        
        
    }
    
}
