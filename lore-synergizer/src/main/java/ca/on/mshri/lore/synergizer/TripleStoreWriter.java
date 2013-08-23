/*
 *  Copyright (C) 2011 The Roth Lab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.on.mshri.lore.synergizer;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.base.Species;
import ca.on.mshri.lore.base.XRef;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import de.jweile.yogiutil.pipeline.EndNode;
import java.util.Map;

/**
 * Writes gene entries to the triplestore.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
final class TripleStoreWriter extends EndNode<Entry> {

    private LoreModel model;
    
    private ObjectProperty fromSpeciesProp;
    
    private Map<Integer,Authority> id2authority;
    private Map<Integer,Species> id2species;
    
    
    private long lastObject = 0;
    private long lastXref = 0;
    private Authority synAuth;
    
    public TripleStoreWriter(LoreModel model) {
        super("TripleStore Writer");
        this.model=model;
    }

    /**
     * Set up the triplestore and the ontology model therein. Add all the basic
     * metadata to it.
     */
    @Override
    protected void before() {
        
        fromSpeciesProp = model.getObjectProperty(LoreModel.URI+"#fromSpecies");
        
        id2authority = new IndividualPopulator<Authority>(model, Authority.class).run("namespaces");
        id2species = new IndividualPopulator<Species>(model, Species.class).run("species");
        
        synAuth = Authority.createOrGet(model, "Synergizer");
        
    }

    @Override
    public Void process(Entry in) {
        
        RecordObject object = RecordObject.createOrGet(model, synAuth, in.getGeneId()+"");
        
        Species species = id2species.get(in.getSpeciesId());
        object.addProperty(fromSpeciesProp, species);
        
        for (Entry.Synonym syn : in.getSynonyms()) {
            
            Authority auth = id2authority.get(syn.getNsId());
            object.addXRef(auth, syn.getSynonym());
            
        }
        
        return null;
    }
    
    private Class<? extends Individual> inferType(XRef xref) {
        
        String authorityId = xref.getAuthority().getAuthorityId();
        
        return null;
    }
    
    
//    @Override
//    protected void after() {
//        
//    }
//    
    

}
