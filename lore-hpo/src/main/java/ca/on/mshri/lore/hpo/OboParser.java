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

import ca.on.mshri.lore.hpo.model.HpoOntModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
final class OboParser {

    /**
     * the ontology model.
     */
    private HpoOntModel model;
    
    /**
     * current line number.
     */
    private int lnum = 0;
    
    /**
     * indexing map. maps IDs to terms.
     */
    private Map<String,Term> index = new HashMap<String,Term>();

    /**
     * Constructor
     * @param model ontology model.
     */
    public OboParser(HpoOntModel model) {
        this.model = model;
    }
    
    
    /**
     * Perform parsing of the obo stream
     * @param oboStream input stream from an obo document.
     */
    void parse(InputStream oboStream) {
        
        BufferedReader b = null;
        
        try {
            b = new BufferedReader(new InputStreamReader(oboStream));
            
            String line; 
            while ((line = b.readLine()) != null) {
                lnum++;
                if (line.trim().equals("[Term]")) {
                    Term t = parseTerm(b);
                    index.put(t.getId(), t);
                    for (String altId : t.getAlt_id()) {
                        index.put(altId, t);
                    }
                }
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Error reading OBO file!",ex);
        } catch (ParseException ex) {
            throw new RuntimeException("Error reading OBO file!",ex);
        } finally {
            try {
                b.close();
            } catch (IOException ex) {
                Logger.getLogger(OboParser.class.getName())
                        .log(Level.WARNING, "Unable to close stream reader", ex);
            }
        }
        
//        BufferedWriter w = null;
//        try {
//            w = new BufferedWriter(new FileWriter("hpo_graph.tsv"));
//            for (Term term : index.values()) {
//                for (String isa : term.getIs_a()) {
//                    Term superTerm = index.get(isa);
//                    w.write(superTerm.getId()+"\t"+term.getId()+"\n");
//                }
//            }
//        } catch (IOException ex) {
//            throw new RuntimeException("Cannot write file!",ex);
//        } finally {
//            try {
//                w.close();
//            } catch (IOException ex) {
//                Logger.getLogger(OboParser.class.getName())
//                        .log(Level.WARNING, "Unable to close stream!", ex);
//            }
//        }
        
    }
    
    /**
     * splits an attribute line into key and value pair.
     * @param line input line.
     * @return a String array of length 2 containing the key at index 0 and
     * the value at index 1.
     */
    private String[] splitAttribute(String line) {
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        int mode = 0;
        for (char c : line.toCharArray()) {
            if (c == ':') {
                mode = 1;
                continue;
            }
            switch(mode) {
                case 0:
                    key.append(c);
                    break;
                case 1:
                    value.append(c);
                    break;
            }
        }
        String[] out = new String[2];
        out[0] = key.toString().trim();
        out[1] = value.toString().trim();
        
        return out;
    }
    
    /**
     * splits an is_a string into id and name.
     * @param line is_a string.
     * @return a String array of length 2 containing the id at index 0 and
     * the name at index 1.
     */
    private String[] splitIsa(String line) {
        StringBuilder id = new StringBuilder();
        StringBuilder name = new StringBuilder();
        int mode = 0;
        for (char c : line.toCharArray()) {
            if (c == '!') {
                mode = 1;
                continue;
            }
            switch(mode) {
                case 0:
                    id.append(c);
                    break;
                case 1:
                    name.append(c);
                    break;
            }
        }
        String[] out = new String[2];
        out[0] = id.toString().trim();
        out[1] = name.toString().trim();
        
        return out;
    }
    
    /**
     * Parses an OBO term from given reader.
     * @param r the reader 
     * @return the parsed term
     * @throws IOException if the reader couldn't be read.
     * @throws ParseException if the content read from the reader does not conform
     * to the expected standard.
     */
    private Term parseTerm(BufferedReader r) throws IOException, ParseException {
        Term term = new Term();
        String line; 
        while ((line = r.readLine()) != null) {
            lnum++;
            if (line.trim().length()==0) {
                return term;
            } else {
                String[] keyVal = splitAttribute(line);
                if (keyVal[0].equals("id")) {
                    term.setId(keyVal[1]);
                } else if (keyVal[0].equals("name")) {
                    term.setName(keyVal[1]);
                } else if (keyVal[0].equals("def")) {
                    term.setDef(keyVal[1]);
                } else if (keyVal[0].equals("comment")) {
                    term.setComment(keyVal[1]);
                } else if (keyVal[0].equals("synonym")) {
                    term.addSynonym(keyVal[1]);
                } else if (keyVal[0].equals("xref")) {
                    term.addXref(keyVal[1]);
                } else if (keyVal[0].equals("is_a")) {
                    String[] isA = splitIsa(keyVal[1]);
                    term.addIs_a(isA[0]);
                } else if (keyVal[0].equals("alt_id")) {
                    term.addAlt_id(keyVal[1]);
                } else if (keyVal[0].equals("is_obsolete")) {
                    term.setObsolete(Boolean.parseBoolean(keyVal[1]));
                } else if (keyVal[0].equals("created_by")) {
                    //ignore
                } else if (keyVal[0].equals("creation_date")) {
                    //ignore
                } else {
                    Logger.getLogger(OboParser.class.getName())
                            .log(Level.WARNING, "Unrecognized attribute key: "+keyVal[0]);
                }
            }
        }
        throw new ParseException("Unexpected end of stream.", lnum);
    }
    
    /**
     * a private class to represent terms in an Obo ontology
     */
    private static class Term {
        /**
         * the id
         */
        private String id;
        
        /**
         * the name 
         */
        private String name;
        
        /**
         * definition
         */
        private String def;
        
        /**
         * a comment
         */
        private String comment;
        
        /**
         * a list of synonyms
         */
        private List<String> synonym;
        
        /**
         * a list of cross references
         */
        private List<String> xref;
        
        /**
         * a list of superterms to this term.
         */
        private List<String> is_a;
        
        /**
         * alternative ids for this term.
         */
        private List<String> alt_id;
        
        /**
         * marks this term as obsolete.
         */
        private boolean obsolete;

        
        
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
        public String getDef() {
            return def;
        }

        public void setDef(String def) {
            this.def = def;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public List<String> getSynonym() {
            if (synonym == null) {
                return Collections.EMPTY_LIST;
            }
            return synonym;
        }
        
        public void addSynonym(String syn) {
            if (synonym == null) {
                synonym = new ArrayList<String>();
            }
            synonym.add(syn);
        }

        public List<String> getXref() {
            if (xref == null) {
                return Collections.EMPTY_LIST;
            }
            return xref;
        }
        
        public void addXref(String x) {
            if (xref == null) {
                xref = new ArrayList<String>();
            }
            xref.add(x);
        }

        public List<String> getIs_a() {
            if (is_a == null) {
                return Collections.EMPTY_LIST;
            }
            return is_a;
        }
        
        public void addIs_a(String i) {
            if (is_a == null) {
                is_a = new ArrayList<String>();
            }
            is_a.add(i);
        }

        public List<String> getAlt_id() {
            if (alt_id == null) {
                return Collections.EMPTY_LIST;
            }
            return alt_id;
        }
        
        public void addAlt_id(String a) {
            if (alt_id == null) {
                alt_id = new ArrayList<String>();
            }
            alt_id.add(a);
        }

        public boolean isObsolete() {
            return obsolete;
        }

        public void setObsolete(boolean obsolete) {
            this.obsolete = obsolete;
        }
        
        
        
    }
    
}
