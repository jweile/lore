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
package ca.on.mshri.lore.base;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Model contents:
 * <pre>
 * 
 * RecordObject --hasXRef-> XRef --hasValue-> String
 *                               |-hasNamespace-> Namespace
 * </pre>
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class LoreModel extends OntModelImpl {

    /**
     * The Lore Base URI
     */
    static final String URI = "http://llama.mshri.on.ca/lore-base.owl";
    
    /**
     * creates a new model
     * @param spec
     * @param model 
     */
    public LoreModel(OntModelSpec spec, Model model) {
        super(spec, model);
        read(LoreModel.class.getClassLoader().getResourceAsStream("lore-base.owl"), null);
    }
    
    /**
     * creates a new lore model around an existing ontmodel.
     * @param m the ontmodel to wrap.
     * @return 
     */
    public static LoreModel fromOntModel(OntModel m) {
        OntModelImpl i = (OntModelImpl) m;
        return new LoreModel(i.getSpecification(), i);
    }
    
    /**
     * lists all currently existing record objects.
     * @return 
     */
    public List<RecordObject> listRecordObjects() {
        ExtendedIterator<? extends OntResource> it = getOntClass(RecordObject.CLASS_URI).listInstances();
        List<RecordObject> list = new ArrayList<RecordObject>();
        while (it.hasNext()) {
            list.add(RecordObject.fromIndividual(it.next().asIndividual()));
        }
        return list;
    }
    
    public List<XRef> listXRefs() {
        ExtendedIterator<? extends OntResource> it = getOntClass(XRef.CLASS_URI).listInstances();
        List<XRef> list = new ArrayList<XRef>();
        while (it.hasNext()) {
            list.add(XRef.fromIndividual(it.next().asIndividual()));
        }
        return list;
    }
    
    public List<Authority> listAuthorities() {
        ExtendedIterator<? extends OntResource> it = getOntClass(Authority.CLASS_URI).listInstances();
        List<Authority> list = new ArrayList<Authority>();
        while (it.hasNext()) {
            list.add(Authority.fromIndividual(it.next().asIndividual()));
        }
        return list;
    }
    
    
    public Authority getOrCreateAuthority(String id) {
        return Authority.create(this,id);
    }
    
    
    public RecordObject createRecordObject(Authority auth, String id) {
        return RecordObject.create(this, auth, id);
    }

    
}
