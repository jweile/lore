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

import ca.on.mshri.lore.genome.util.GeneticCode;
import ca.on.mshri.lore.base.LoreModel;
import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single nucleotide polymorphism
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class PointMutation extends Mutation {
    
    public static final String CLASS_URI = GenomeModel.URI+"#PointMutation";
    
    protected PointMutation(Node n, EnhGraph g) {
        super(n, g);
    }
    
    public static PointMutation fromIndividual(Individual i) {
        IndividualImpl impl = (IndividualImpl)i;
        OntClass thisType = i.getModel().getResource(CLASS_URI).as(OntClass.class);
                
        if (LoreModel.hasClass(i, thisType)) {
            return new PointMutation(impl.asNode(), impl.getGraph());
            
        } else {
            throw new ConversionException(i.getURI()+" cannot be cast as PointMutation!");
        }
    }
    
    /**
     * 
     * @param model
     * @param allele
     * @param mutation
     * @throws IllegalArgumentException if the mutation string is invalid
     * @return 
     */
    public static PointMutation createOrGet(GenomeModel model, Allele allele, String mutation) {
        
        String[] mutInfo = parseMutantDescription(mutation);
        String fromAA = mutInfo[0];
        int position = Integer.parseInt(mutInfo[1]);
        String toAA = mutInfo[2];
        
        PointMutation out = fromIndividual(model.getOntClass(CLASS_URI)
                .createIndividual("urn:lore:PointMutation#"+allele.getURI().substring(16)+
                ":"+fromAA+position+toAA));
        
        out.addProperty(model.getProperty(GenomeModel.URI+"#fromAA"), fromAA);
        out.addProperty(model.getProperty(GenomeModel.URI+"#position"), position);
        out.addProperty(model.getProperty(GenomeModel.URI+"#toAA"), toAA);
        
        return out;
    }
    
    public int getPosition() {
        return getPropertyValue(getModel().getProperty(GenomeModel.URI+"#position"))
                .asLiteral().getInt();
    }
    
    public String getFromAminoAcid() {
        return getPropertyValue(getModel().getProperty(GenomeModel.URI+"#fromAA"))
                .asLiteral().getString();
    }
    
    public String getToAminoAcid() {
        return getPropertyValue(getModel().getProperty(GenomeModel.URI+"#toAA"))
                .asLiteral().getString();
    }
    
    private static final Pattern dnaTriple = Pattern.compile("^([ACGT]{3})(\\d+)([ACGT]{3})$");
    private static final Pattern rnaTriple = Pattern.compile("^([ACGU]{3})(\\d+)([ACGU]{3})$");
    private static final Pattern aaTriple = Pattern.compile("^([A-z]{3})(\\d+)([A-z]{3})$");
    private static final Pattern aaSingle = Pattern.compile("^([A-z])(\\d+)([A-z])$");
    
    private static String[] parseMutantDescription(String s) {
        
        int position;
        String fromAA, toAA;
        
        String[] matches = null;
        GeneticCode c = GeneticCode.getInstance();
        
        if ((matches = matches(dnaTriple,s)) != null) {
            
            fromAA = c.toTriple(c.translate(matches[0]));
            position = Integer.parseInt(matches[1])/3;
            toAA = c.toTriple(c.translate(matches[2]));
            
        } else if ((matches = matches(rnaTriple,s)) != null) {
            
            fromAA = c.toTriple(c.translate(matches[0].replace('U', 'T')));
            position = Integer.parseInt(matches[1])/3;
            toAA = c.toTriple(c.translate(matches[2].replace('U', 'T')));
            
        } else if ((matches = matches(aaTriple,s)) != null) {
            
            if (!c.isValidAA(matches[0]) || !c.isValidAA(matches[2])) {
                throw new IllegalArgumentException("Pattern "+s+" does not contain legal AA names");
            }
            fromAA = matches[0];
            position = Integer.parseInt(matches[1]);
            toAA = matches[2];
            
        } else if ((matches = matches(aaSingle,s)) != null) {
            
            fromAA = c.toTriple(matches[0]);
            position = Integer.parseInt(matches[1]);
            toAA = c.toTriple(matches[2]);
            
        } else {
            throw new IllegalArgumentException("Pattern "+s+" does not match any known Mutation description.");
        }
        
        if (fromAA == null || toAA == null) {
            throw new IllegalArgumentException("Pattern "+s+" does not match any known Mutation description.");
        }
        
        return new String[]{fromAA, position+"", toAA};
    }
    
    private static String[] matches(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (m.find()) {
            return new String[] {
              m.group(1),  
              m.group(2),  
              m.group(3)
            };
        } else {
            return null;
        }
    }

//    private String rna2dna(String rna) {
//        return rna.replace('U', 'T');
//    }
//    
    
}
