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
import java.net.MalformedURLException;
import java.net.URL;
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
    
    public void setOffset(int src) {
        Property offset = getModel().getProperty(MoleculesModel.URI+"#offset");
        RDFNode existing = getPropertyValue(offset);
        if (existing != null) {
            Logger.getLogger(Structure3D.class.getName())
                    .log(Level.WARNING, "Overwriting existing offset!");
            removeAll(offset);
        }
        addProperty(offset, src);
    }
    
    public Integer getOffset() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#offset"));
        Integer out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getInt();
            } else {
                throw new InconsistencyException("Structure3D "+getURI()+" should only have one offset!");
            }
        }
        return out;
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
}
