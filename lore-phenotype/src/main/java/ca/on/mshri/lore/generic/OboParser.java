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
package ca.on.mshri.lore.generic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OboParser {
        
    private Map<String,Stanza> stanzas = new HashMap<String,Stanza>();
    
    public Map<String,Stanza> parse(InputStream in) throws IOException {
        
        BufferedReader r = null; 
        try {
            
            r = new BufferedReader(new InputStreamReader(in));
            
            Stanza currStanza = new Stanza(StanzaType.HEADER);
            
            String line = null; int lnum = 0;
            while((line = r.readLine()) != null) {
                
                line = line.trim();
                
                if (line.length() == 0) {
                    //ignore empty lines
                } else if (line.startsWith("[")) {
                    //store old stanza and create a new one
                    store(currStanza);
                    currStanza = new Stanza(StanzaType.parse(line.substring(1, line.length()-1)));
                } else {
                    //tag-value pair
                    TagValuePair tvp = parseTagValuePair(line);
                    if (tvp != null) {
                        currStanza.addTagValuePair(tvp);
                    } else {
                        Logger.getLogger(OboParser.class.getName())
                                .log(Level.WARNING, "Invalid line "+lnum+": "+line);
                    }
                }
                
                lnum++;
            }
            //process last stanza
            store(currStanza);
            
            return stanzas;
            
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(OboParser.class.getName())
                            .log(Level.WARNING, "Cannot close stream", ex);
                }
            }
        }
        
    }

    
    private final Pattern tv = Pattern.compile("\"([^\"]*)\"|!(.*)$|^(\\S+):|(\\S+)");
    
    private TagValuePair parseTagValuePair(String line) {
        
        TagValuePair tvp = new TagValuePair();
        
        Matcher matcher = tv.matcher(line);
        while (matcher.find()) {
            for (int i = 1; i < matcher.groupCount()+1; i++) {
                String match = matcher.group(i);
                if (match != null) {
                    switch (i) {
                        case 1:
                        case 4:
                            //value
                            tvp.addValue(match);
                            break;
                        case 2:
                            //comment
                            break;
                        case 3:
                            //tag
                            tvp.setTag(match);
                            break;
                    }
                }
            }
        }
        
        return tvp.validate() ? tvp : null;
    }

    private void store(Stanza currStanza) {
        String id = currStanza.getID();
        if (id == null) {
            if (currStanza.getStanzaType() == StanzaType.HEADER) {
                stanzas.put("HEADER",currStanza);
            } else {
                Logger.getLogger(OboParser.class.getName())
                        .log(Level.WARNING, "Cannot store unidentified stanza!");
            }
        } else {
            if (stanzas.containsKey(id)) {
                Logger.getLogger(OboParser.class.getName())
                        .log(Level.WARNING, "Duplicated id:"+id);
            }
            stanzas.put(id,currStanza);
        }
    }
    
    public static class Stanza {
        
        private static final String ID_TAG = "id";
        private static final String NAME_TAG = "name";
        private static final String XREF_TAG = "xref";
        private static final String ISA_TAG = "is_a";
        
        private StanzaType stanzaType;
        
        private List<TagValuePair> tvps = new ArrayList<TagValuePair>();
        
        Stanza(StanzaType type) {
            stanzaType = type;
        }
        
        public void addTagValuePair(TagValuePair tvp) {
            
            //check constraints
            if (tvp.getTag().equals(ID_TAG) && hasTag(ID_TAG)) {
                Logger.getLogger(Stanza.class.getName())
                        .log(Level.WARNING, "Stanza with more than one ID: "+tvp.getTag().equals(ID_TAG));
            }
            if (tvp.getTag().equals(NAME_TAG) && hasTag(NAME_TAG)) {
                Logger.getLogger(Stanza.class.getName())
                        .log(Level.WARNING, "Stanza with more than one name: "+tvp.getTag().equals(ID_TAG));
            }
            
            tvps.add(tvp);
        }
        
        public String getID() {
            return getSingleValue(ID_TAG);
        }
        
        public String getName() {
            return getUntokenizedSingleValue(NAME_TAG);
        }
        
        public List<String> getXRefs() {
            return getValues(XREF_TAG);
        }
        
        public String getIsA() {
            return getSingleValue(ISA_TAG);
        }
        
        public List<String> getUntokenizedValues(String key) {
            List<String> values = new ArrayList<String>();
            for (TagValuePair pair : getPairsByTag(key)) {
                values.add(cons(pair.getValues()));
            }
            return values;
        }
        
        public List<String> getValues(String key) {
            List<String> values = new ArrayList<String>();
            for (TagValuePair pair : getPairsByTag(key)) {
                List<String> inner = pair.getValues();
                if (!inner.isEmpty()) {
                    values.add(inner.get(0));
                }
            }
            return values;
        }
                
        public String getSingleValue(String key) {
            List<TagValuePair> pairs = getPairsByTag(key);
            if (pairs.isEmpty()) {
                return null;
            }
            List<String> values = getPairsByTag(key).get(0).getValues();
            if (values.isEmpty()) {
                return null;
            } 
            return values.get(0);
        }
        
        public String getUntokenizedSingleValue(String key) {
            List<TagValuePair> pairs = getPairsByTag(key);
            if (pairs.isEmpty()) {
                return null;
            }
            List<String> values = getPairsByTag(key).get(0).getValues();
            if (values.isEmpty()) {
                return null;
            } 
            return cons(values);
        }
        
        public boolean hasTag(String tag) {
            for (TagValuePair tvp : tvps) {
                if (tvp.getTag().equals(tag)) {
                    return true;
                }
            }
            return false;
        }
        
        public Set<String> getTags() {
            Set tags = new HashSet<String>();
            
            for (TagValuePair tvp : tvps) {
                tags.add(tvp.getTag());
            }
            
            return tags;
        }
        
        public List<TagValuePair> getPairsByTag(String tag) {
            List<TagValuePair> out= new ArrayList<TagValuePair>();
            
            for (TagValuePair tvp : tvps) {
                if (tvp.getTag().equals(tag)) {
                    out.add(tvp);
                }
            }
            
            return out;
        } 

        public List<TagValuePair> getTagValuePairs() {
            return tvps;
        }

        public StanzaType getStanzaType() {
            return stanzaType;
        }

        private String cons(List<String> values) {
            StringBuilder b = new StringBuilder();
            for (String value : values) {
                b.append(value).append(" ");
            }
            b.deleteCharAt(b.length()-1);
            return b.toString();
        }

        
        
    }
    
    public enum StanzaType {
        TERM,TYPEDEF,INSTANCE,HEADER;
        public static StanzaType parse(String s) {
            if (s.equals("Term")) {
                return TERM;
            } else if (s.equals("Typedef")) {
                return TYPEDEF;
            } else if (s.equals("Instance")) {
                return INSTANCE;
            } else {
                return null;
            }
        }
    }
    
    public static class TagValuePair {
        private String tag;
        private List<String> values = new ArrayList<String>();

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
        
        public void addValue(String val) {
            values.add(val);
        }

        public List<String> getValues() {
            return values;
        }
        
        public boolean validate() {
            return tag != null && !values.isEmpty();
        }
        
    }
    

}
