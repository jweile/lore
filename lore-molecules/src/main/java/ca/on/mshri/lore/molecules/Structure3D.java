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
package ca.on.mshri.lore.molecules;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.molecules.util.Structure;
import ca.on.mshri.lore.molecules.util.StructureCache;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.jweile.yogiutil.IntTable;
import de.jweile.yogiutil.Pair;
import de.jweile.yogiutil.Table;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Structure3D extends RecordObject {
    
    public static final String CLASS_URI = MoleculesModel.URI+"#Structure3D";
    
    protected Structure3D(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Structure3D fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new Structure3D(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Structure3D!");
        }
    }
    
    public void setSource(URL src) {
        Property source = getModel().getProperty(MoleculesModel.URI+"#source");
        RDFNode existing = getPropertyValue(source);
        if (existing != null) {
            Logger.getLogger(Structure3D.class.getName())
                    .log(Level.WARNING, "Overwriting existing source file association!");
            removeAll(source);
        }
        addProperty(source, src.toString());
    }
    
    public URL getSource() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#source"));
        String out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getString();
            } else {
                throw new InconsistencyException("Structure3D "+getURI()+" should only have one source URL!");
            }
        }
        try {
            return new URL(out);
        } catch (MalformedURLException ex) {
            throw new InconsistencyException("Structure3D "+getURI()+" has invalid source URL: "+out, ex);
        }
    }
    
    public Structure getStructureObject() {
        URL src = getSource();
        if (src != null) {
            return StructureCache.getInstance().getStructureForURL(src);
        } else {
            return null;
        }
    }
    
    public void setSeqMap(SeqMap map) {
        Property smProp = getModel().getProperty(MoleculesModel.URI+"#seqmap");
        RDFNode existing = getPropertyValue(smProp);
        if (existing != null) {
            Logger.getLogger(Structure3D.class.getName())
                    .log(Level.WARNING, "Overwriting existing SeqMap!");
            removeAll(smProp);
        }
        addProperty(smProp, map.serialize());
    }
    
    public SeqMap getSeqMap() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#seqmap"));
        String out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getString();
            } else {
                throw new InconsistencyException("Structure3D "+getURI()+" should only have one SeqMap!");
            }
        }
        return SeqMap.deserialize(out);
    }
    
    public static List<Structure3D> listStructuresOfObject(Individual obj) {
        NodeIterator it = obj.listPropertyValues(obj.getModel().getProperty(MoleculesModel.URI+"#hasStructure"));
        List<Structure3D> out = new ArrayList<Structure3D>();
        while (it.hasNext()) {
            out.add(Structure3D.fromIndividual(it.next().as(Individual.class)));
        }
        return out;
    }
    
    public static void addStructureToObject(Structure3D struc, Individual obj) {
        Property enc = obj.getModel().getProperty(MoleculesModel.URI+"#hasStructure");
        obj.addProperty(enc, struc);
    }
    
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static Structure3D createOrGet(MoleculesModel model, Authority auth, String id) {
        Structure3D out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Structure3D#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
    
    public static class SeqMap {
        
        private Table<String> ids = Table.newTable(2, String.class);
        private IntTable mapValues = new IntTable(3);
        
        private static final int queryI = 0;
        private static final int targetI = 1;
        private static final int queryStartI = 0;
        private static final int targetStartI = 1;
        private static final int lengthI = 2;
        
        public SeqMap() {
            
        }
        
        public int getNumMappings() {
            return ids.getNumRows();
        }
        
        public void addMapping(String query, String target, int queryStart, int targetStart, int length) {
            ids.addRow(new String[]{query,target});
            mapValues.addRow(new int[]{queryStart,targetStart,length});
        }
        
        public String getQueryId(int i) {
            return ids.getCell(i, queryI);
        }
        
        public String getTargetId(int i) {
            return ids.getCell(i,targetI);
        }
        
        public int getQueryStart(int i) {
            return mapValues.getCell(i, queryStartI);
        }
        
        public int getTargetStart(int i) {
            return mapValues.getCell(i, targetStartI);
        }
        
        public int getMappingLength(int i) {
            return mapValues.getCell(i, lengthI);
        }
        
        /**
         * serializes to the following form:
         * <pre>
         * "A,P0015,6,44,100;B,Q0152,2,0,142"
         * </pre>
         * That is, values of the mapping separated by commas,
         * multiple mappings separated by semicolons.
         * 
         * @return 
         */
        String serialize() {
            StringBuilder b = new StringBuilder();
            
            for (int i = 0; i < ids.getNumRows(); i++) {
                b.append(getQueryId(i)).append(",");
                b.append(getTargetId(i)).append(",");
                b.append(getQueryStart(i)).append(",");
                b.append(getTargetStart(i)).append(",");
                b.append(getMappingLength(i)).append(";");
            }
            //remove last semicolon if not empty
            if (b.length() > 0) {
                b.deleteCharAt(b.length()-1);
            }
            
            return b.toString();
        }
        
        static SeqMap deserialize(String str) {
            SeqMap sm = new SeqMap();
            
            for (String entry :str.split(";")) {
                
                String[] fields = entry.split(",");
                assert(fields.length == 5);
                
                String query = fields[0];
                String target = fields[1];
                int qStart = Integer.parseInt(fields[2]);
                int tStart = Integer.parseInt(fields[3]);
                int length = Integer.parseInt(fields[4]);
                sm.addMapping(query, target, qStart, tStart, length);
                
            }
            
            return sm;
        }

    }
}
