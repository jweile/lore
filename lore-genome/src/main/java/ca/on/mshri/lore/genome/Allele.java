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
package ca.on.mshri.lore.genome;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.InconsistencyException;
import ca.on.mshri.lore.base.RecordObject;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Allele extends RecordObject {
    
    public static final String CLASS_URI = GenomeModel.URI+"#Allele";
    
    protected Allele(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static Allele fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl)i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (impl.getOntClass() != null && 
                (impl.getOntClass().equals(thisType) || thisType.hasSubClass(impl.getOntClass(),false))) {
          
            return new Allele(impl.asNode(), impl.getGraph());
            
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as Allele!");
        }
    }
    
    public Gene getGene() {
        
        List<Gene> genes = new ArrayList<Gene>();
        
        String qry = "PREFIX : <http://llama.mshri.on.ca/lore-genome.owl#>\n SELECT ?gene WHERE {?gene :hasAllele <"+this.getURI()+">}";
        
        QueryExecution qexec = null;
        try {
            qexec = QueryExecutionFactory
                .create(qry,getModel());
            ResultSet result = qexec.execSelect();
            while (result.hasNext()) {
                QuerySolution sol = result.next();
                genes.add(Gene.fromIndividual(sol.get("gene").as(Individual.class)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed: "+qry, e);
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
        
        if (genes.isEmpty()) {
            return null;
        } else if (genes.size() > 1) {
            throw new InconsistencyException(getURI()+"has more than one associated Gene!");
        } else {
            return genes.get(1);
        }
        
    }
    
    public void setGene(Gene gene) {
        
        if (gene == null) {
            throw new NullPointerException();
        }
        
        Property hasAllele = getModel().getProperty(GenomeModel.URI+"#hasAllele");
        
        Gene existing = getGene();
        if (existing != null) {
            Logger.getLogger(Allele.class.getName())
                    .log(Level.WARNING, "Overwriting existing Gene association of allele "+getURI());
            existing.removeProperty(hasAllele, this);
        }
        
        gene.addProperty(hasAllele, this);
        
    }
    
    public void addMutation(Mutation m) {
        Property hasMut = getModel().getProperty(GenomeModel.URI+"#hasMutation");
        addProperty(hasMut, m);
    }
    
    public List<Mutation> listMutations() {
        List<Mutation> list = new ArrayList<Mutation>();
        Property hasMut = getModel().getProperty(GenomeModel.URI+"#hasMutation");
        NodeIterator it = listPropertyValues(hasMut);
        while (it.hasNext()) {
            list.add(Mutation.fromIndividual(it.next().as(Individual.class)));
        }
        return list;
    }
    
    public static Allele createOrGet(GenomeModel model, Authority auth, String id) {
        Allele out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:Allele#"+auth.getAuthorityId()+":"+id));
        out.addXRef(auth, id);
        return out;
    }
    
    
}
