/*
 * Copyright (C) 2013 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.base.LoreModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import de.jweile.yogiutil.Counts;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Summary {
    
    public void printSummary(LoreModel model) {
        
        Counts<OntClass> counts = new Counts<OntClass>();
        ExtendedIterator<Individual> it = model.listIndividuals();
        while (it.hasNext()) {
            Individual i = it.next();
            counts.count(i.getOntClass());
        }
        
        System.out.println("\nSummary:");
        for (OntClass clazz : counts.getKeys()) {
            System.out.println(clazz+"\t"+counts.getCount(clazz));
        }
        System.out.println("\n");
        
    }
    
}
