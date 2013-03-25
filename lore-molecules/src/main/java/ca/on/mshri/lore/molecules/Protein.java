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
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.genome.GenomeModel;
import ca.on.mshri.lore.operations.Sparql;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Protein extends Molecule {
    
    public static final String CLASS_URI = MoleculesModel.URI+"#Protein";
    
    protected Protein(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Protein fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl) i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new Protein(impl.asNode(), impl.getGraph());
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Molecule!");
        }
    }
    
    public Gene getEncodingGene() {
        NodeIterator it = listPropertyValues(getModel().getProperty(MoleculesModel.URI+"#encodedBy"));
        Gene out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = Gene.fromIndividual(it.next().as(Individual.class));
            } else {
                //TODO: this might cause problems?
                throw new InconsistencyException("Protein "+getURI()+" should only have one encoding gene!");
            }
        }
        return out;
    }
    
    public void setEncodingGene(Gene gene) {
        Property enc = getModel().getProperty(MoleculesModel.URI+"#encodedBy");
        Resource existing = getPropertyResourceValue(enc);
        if (existing != null) {
            Logger.getLogger(Protein.class.getName())
                    .log(Level.WARNING, "Overwriting existing encoding gene resource!");
            removeAll(enc);
        }
        addProperty(enc, gene);
    }
    
    public static List<Protein> listEncodedProteins(Gene gene) {
        
        Sparql sparql = Sparql.getInstance(Protein.class.getProtectionDomain().getCodeSource());
        List<Individual> is = sparql.queryIndividuals(gene.getModel(), "listProteins", "protein", gene.getURI());
        
        List<Protein> ps = new ArrayList<Protein>();
        for (Individual i : is) {
            ps.add(Protein.fromIndividual(i));
        }
        return ps;
    }
    
    /**
     * Pseudo-constructor. 
     * @param model
     * @param auth
     * @param id
     * @return 
     */
    public static Protein createOrGet(MoleculesModel model, Authority auth, String id) {
        Protein out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Protein#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }

    public String getSequence() {
        
        NodeIterator it = listPropertyValues(getModel().getProperty(GenomeModel.URI+"#sequence"));
        String out = null;
        while (it.hasNext()) {
            if (out == null) {
                out = it.next().asLiteral().getString();
            } else {
                //TODO: this might cause problems?
                throw new InconsistencyException("Protein "+getURI()+" should only have one sequence!");
            }
        }
        return out;
    }
    
    public void setSequence(String seq) {
        
        Property enc = getModel().getProperty(GenomeModel.URI+"#sequence");
        RDFNode existing = getPropertyValue(enc);
        if (existing != null) {
            Logger.getLogger(Protein.class.getName())
                    .log(Level.WARNING, "Overwriting existing sequence!");
            removeAll(enc);
        }
        addProperty(enc, seq);
    }
}
