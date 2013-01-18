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
package ca.on.mshri.lore.interaction;

import ca.on.mshri.lore.genome.GenomeModel;
import ca.on.mshri.lore.molecules.MoleculesModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class InteractionModel extends MoleculesModel {
    
    public static final String URI = "http://llama.mshri.on.ca/lore-interaction.owl";
    
    public InteractionModel(OntModelSpec spec, Model model) {
        //read dependencies
        super(spec, model);
        //read owl specs
        read(GenomeModel.class.getClassLoader().getResourceAsStream("lore-interaction.owl"), null);
    }
    
}
