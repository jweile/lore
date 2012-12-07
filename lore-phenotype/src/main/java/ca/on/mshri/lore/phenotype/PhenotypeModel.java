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
package ca.on.mshri.lore.phenotype;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.genome.GenomeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PhenotypeModel extends GenomeModel {
    
    public static final String URI = "http://llama.mshri.on.ca/lore-phenotype.owl";
    
    public final Authority HPO;
    public final Authority OMIM;

    public PhenotypeModel(OntModelSpec spec, Model model) {
        //super constructor loads dependencies, recursively
        super(spec, model);
        //read owl specs
        read(GenomeModel.class.getClassLoader().getResourceAsStream("lore-phenotype.owl"), null);
        
        HPO = Authority.createOrGet(this, "HPO");
        OMIM = Authority.createOrGet(this, "OMIM");
    }
    
//    public List<Gene> listGenes() {
//        
//        OntClass geneClass = getOntClass(HPO+"Gene");
//        ExtendedIterator<? extends OntResource> geneIt = geneClass.listInstances();
//        List<Gene> list = new ArrayList<Gene>();
//        while (geneIt.hasNext()) {
//            Gene g = Gene.fromIndividual(geneIt.next().as(Individual.class));
//            list.add(g);
//        }
//        geneIt.close();
//        return list;
//    }
//    
//    /**
//     * Returns the gene object for the given URI. <b>Warning</b>: In accordance 
//     * with JENA specifications, if none exists, a new gene object is created and 
//     * returned!
//     * 
//     * @param uri the URI of the gene object.
//     * @return a gene object for given URI.
//     */
//    public Gene getGene(String uri) {
//        Individual individual = getIndividual(uri);
//        if (individual == null) {
//            return null;
//        } else {
//            ensureClassAssignment(individual, HPO+"Gene");
//            return Gene.fromIndividual(individual);
//        }
//    }
//    
//    /**
//     * creates a new gene object with the given URI
//     * @param uri the URI to use
//     * @return the new gene.
//     */
//    public Gene createGene(String uri) {
//        Individual ind = createIndividual(uri, getOntClass(HPO+"Gene"));
//        return Gene.fromIndividual(ind);
//    }
//
//    /**
//     * Creates a phenotype with the given URI
//     * @param uri the URI for the new phenotype.
//     * @return the phenotype object.
//     */
//    public Phenotype createPhenotype(String uri) {
//        OntClass phenoClass = getOntClass(HPO+"Phenotype");
//        Individual individual = createIndividual(uri, phenoClass);
//        return Phenotype.fromIndividual(individual);
//    }
//    
//    /**
//     * Returns the phenotype object for the given URI. <b>Warning</b>: In accordance 
//     * with JENA specifications, if none exists, a new phenotype object is created and 
//     * returned!
//     * 
//     * @param uri the URI of the phenotype object.
//     * @return a phenotype object for given URI.
//     */
//    public Phenotype getPhenotype(String uri) {
//        Individual individual = getIndividual(uri);
//        if (individual == null) {
//            return null;
//        } else {
//            ensureClassAssignment(individual, HPO+"Phenotype");
//            return Phenotype.fromIndividual(individual);
//        }
//    }
//
//    /**
//     * Creates a new cross-reference object for the given namespace and value
//     * @param nsURI the namespace URI
//     * @param value the value for the xref
//     * @return the xref object.
//     */
//    public XRef createXRef(String nsURI, String value) {
//        Individual ind = createIndividual(nsURI+":"+value, getOntClass(HPO+"XRef"));
//        
//        XRef xref =  XRef.fromIndividual(ind);
//        Individual ns = getIndividual(nsURI);
//        xref.addProperty(getProperty(HPO+"fromNamespace"), ns);
//        xref.addProperty(getProperty(HPO+"hasValue"),value);
//        
//        return xref;
//    }
//    
//    /**
//     * Returns the xref object for the given URI. <b>Warning</b>: In accordance 
//     * with JENA specifications, if none exists, a new xref object is created and 
//     * returned!
//     * 
//     * @param nsURI the URI of the namespace for this xref.
//     * @param value the value of the xref.
//     * @return a xref object for given URI.
//     */
//    public XRef getXRef(String nsURI, String value) {
//        Individual ind = getIndividual(nsURI+":"+value);
//        if (ind == null) {
//            return null;
//        } else {
//            ensureClassAssignment(ind, HPO+"XRef");
//            XRef xref = XRef.fromIndividual(ind);
//
//            if (!xref.hasProperty(getProperty(HPO+"fromNamespace"))) {
//                Individual ns = getIndividual(nsURI);
//                xref.addProperty(getProperty(HPO+"fromNamespace"), ns);
//                xref.addProperty(getProperty(HPO+"hasValue"),value);
//            }
//
//            return xref;
//        }
//    }
//
//    
//    /**
//     * Checks if the individual is compatible with the given class and ensures its
//     * assignment. 
//     * @param individual the individual
//     * @param classURI the URI of the class
//     * @throws ClassCastException if the individual is incompatible with the class
//     */
//    private void ensureClassAssignment(Individual individual, String classURI) {
//        OntClass clazz = getOntClass(classURI);
//        if (!individual.hasOntClass(clazz)) {
//            OntClass foundClass = individual.getOntClass(true);
//            if (foundClass == null || 
//                    foundClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
//                //case: new (has no class or only "Thing" class)
//                individual.setOntClass(clazz);
//            } else {
//                //case: wrong, incompatible object
//                throw new ClassCastException("Object "+individual+
//                        " is not compatible with "+classURI+"!");
//            }
//        }
//    }
    
    
}
