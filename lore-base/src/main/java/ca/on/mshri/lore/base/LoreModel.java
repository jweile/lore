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

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>This is the main entry point of the lore-base module. Other modules should have 
 * a class that extends this module, that way the OWL dependencies are automatically
 * loaded when super() is called.</p>
 * 
 * <p>Currently this class also contains fields for standardized authorities. This 
 * might be moved out to another class at some point though, together with fields
 * for commonly used properties etc.</p>
 * 
 * <h4>Model contents:</h4>
 * <pre>
 * RecordObject --hasXRef-> XRef --hasValue-> String
 *                               |-hasNamespace-> Namespace
 * Experiment --publishedIn-> Publication
 * </pre>
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class LoreModel extends OntModelImpl {

    /**
     * The Lore Base URI
     */
    public static final String URI = "http://llama.mshri.on.ca/lore-base.owl";
    
    /**
     * The PubMed authority.
     */
    public final Authority PUBMED;
    
    /**
     * creates a new model
     * @param spec
     * @param model 
     */
    public LoreModel(OntModelSpec spec, Model model) {
        super(spec, model);
        read(LoreModel.class.getClassLoader().getResourceAsStream("lore-base.owl"), null);
        
        PUBMED = Authority.createOrGet(this, "PubMed");
    }
    
//    /**
//     * creates a new lore model around an existing ontmodel.
//     * @param m the ontmodel to wrap.
//     * @return 
//     */
//    public static LoreModel fromOntModel(OntModel m) {
//        OntModelImpl i = (OntModelImpl) m;
//        return new LoreModel(i.getSpecification(), i);
//    }
//    
//    /**
//     * lists all currently existing record objects.
//     * @return 
//     */
//    public List<RecordObject> listRecordObjects() {
//        ExtendedIterator<? extends OntResource> it = getOntClass(RecordObject.CLASS_URI).listInstances();
//        List<RecordObject> list = new ArrayList<RecordObject>();
//        while (it.hasNext()) {
//            list.add(RecordObject.fromIndividual(it.next().asIndividual()));
//        }
//        return list;
//    }
//    
//    public List<XRef> listXRefs() {
//        ExtendedIterator<? extends OntResource> it = getOntClass(XRef.CLASS_URI).listInstances();
//        List<XRef> list = new ArrayList<XRef>();
//        while (it.hasNext()) {
//            list.add(XRef.fromIndividual(it.next().asIndividual()));
//        }
//        return list;
//    }
//    
//    public List<Authority> listAuthorities() {
//        ExtendedIterator<? extends OntResource> it = getOntClass(Authority.CLASS_URI).listInstances();
//        List<Authority> list = new ArrayList<Authority>();
//        while (it.hasNext()) {
//            list.add(Authority.fromIndividual(it.next().asIndividual()));
//        }
//        return list;
//    }
//    
//    
//    public Authority getOrCreateAuthority(String id) {
//        return Authority.createOrGet(this,id);
//    }
//    
//    
//    public RecordObject createRecordObject(Authority auth, String id) {
//        return RecordObject.createOrGet(this, auth, id);
//    }
    
    /**
     * Retrieves all instances of the given class from the model.
     * @param <T> The class in question.
     * @param clazz The class in question.
     * @param direct Whether or not to only list direct class members or also members of subclasses.
     * @return a list of the instances.
     */
    public <T extends Individual> List<T> listIndividualsOfClass(Class<T> clazz, boolean direct) {
        
        try {
            
            String classURI = (String) clazz.getDeclaredField("CLASS_URI").get(null);
            Method fromIndividualMethod = clazz.getMethod("fromIndividual", Individual.class);
            
            ExtendedIterator<? extends OntResource> it = getOntClass(classURI).listInstances(direct);
            List<T> list = new ArrayList<T>();
            while (it.hasNext()) {
                T t = (T) fromIndividualMethod.invoke(null, it.next().asIndividual());
                list.add(t);
            }
            it.close();
            return list;
            
        } catch (Exception ex) {
            //FIXME: maybe using an abstract method for this would be better getClassURI() or something?
            throw new RuntimeException("Class wrapper is missing CLASS_URI field or fromIndividual() method. Report this as a bug!",ex);
        } 
        
    }
    
    /**
     * This is a hack to replace the functionality of Individual.hasOntClass(), because
     * the aforementioned function only works correctly if a reasoner is enabled on the 
     * model. Reasoners cause extreme computational overhead, so we don't want them to be
     * turned on all the time. Hence this replacement function that tests class membership
     * manually.
     * @param in an individual
     * @param clazz the class in question
     * @return whether or not the individual is a member of the given class.
     */
//    public static boolean hasClass(Individual in, OntClass clazz) {
//        /*
//         * FIXME: To account for multiple possible superclasses in OWL, 
//         * This should be re-written as a recursive DFS algorithm.
//         */
//        OntClass currClass = in.getOntClass();
//        while (currClass != null) {
//            if (currClass.equals(clazz)) {
//                return true;
//            } else {
//                currClass = currClass.getSuperClass();
//            }
//        }
//        return false;
//    }
    
    public static boolean hasClass(Individual in, OntClass clazz) { 
        OntClass currClass = in.getOntClass();
        return isSubClassOf(currClass, clazz);
        
    }

    
    public static boolean isSubClassOf(OntClass subClass, OntClass clazz) {
        
        if (subClass.equals(clazz)) {
            return true;
        } else {
            ExtendedIterator<OntClass> it = subClass.listSuperClasses(true);
            while (it.hasNext()) {
                if (isSubClassOf(it.next(), clazz)) {
                    return true;
                }
            }
            return false;
        }
    }
    
}
