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
import ca.on.mshri.lore.genome.GenomeModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class MoleculesModel extends GenomeModel {
    
    
    public static final String URI = "http://llama.mshri.on.ca/lore-molecules.owl";
    
    public final Authority PFAM = Authority.createOrGet(this, "PFAM");
    public final Authority UNIPROT = Authority.createOrGet(this, "UNIPROT");

    public MoleculesModel(OntModelSpec spec, Model model) {
        //read dependencies
        super(spec, model);
        //read owl specs
        read(GenomeModel.class.getClassLoader().getResourceAsStream("lore-molecules.owl"), null);
    }
    
    
}
