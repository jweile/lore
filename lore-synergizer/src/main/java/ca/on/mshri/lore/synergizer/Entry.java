/*
 *  Copyright (C) 2011 The Roth Lab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.on.mshri.lore.synergizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a chunk of information from the database to be transfered to the
 * triplestore. Each entry contains one gene and its synonyms.
 * @author Jochen Weile <jochenweile@gmail.com>
 */
final class Entry {
    
    public static final int QUEUE_LENGTH = 10;

    private int geneId;
    
    private List<Synonym> synonyms;
    
    private int speciesId;
    
    private int sourceId;

    public Entry() {
    }

    public Entry(int geneId, int speciesId, int sourceId) {
        this.geneId = geneId;
        this.speciesId = speciesId;
        this.sourceId = sourceId;
    }
    
    public int getGeneId() {
        return geneId;
    }

    public void setGeneId(int geneId) {
        this.geneId = geneId;
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }
    
    
    public int getSourceId() {
        return sourceId;
    }

    public List<Synonym> getSynonyms() {
        if (synonyms == null) {
            return Collections.EMPTY_LIST;
        }
        return synonyms;
    }

    public void addSynonym(Synonym s) {
        if (synonyms == null) {
            synonyms = new ArrayList<Synonym>();
        }
        synonyms.add(s);
    }
    
    static class Synonym {
        
        private String synonym;
        
        private int sourceId;
        
        private int nsId;

        public Synonym() {
        }

        public Synonym(String synonym, int sourceId, int nsId) {
            this.synonym = synonym;
            this.sourceId = sourceId;
            this.nsId = nsId;
        }
        
        public int getNsId() {
            return nsId;
        }

        public void setNsId(int nsId) {
            this.nsId = nsId;
        }

        public int getSourceId() {
            return sourceId;
        }

        public void setSourceId(int sourceId) {
            this.sourceId = sourceId;
        }

        public String getSynonym() {
            return synonym;
        }

        public void setSynonym(String synonym) {
            this.synonym = synonym;
        }
        
        
    }
}
