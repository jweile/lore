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
package ca.on.mshri.lore.base.it;

import ca.on.mshri.lore.base.Authority;
import ca.on.mshri.lore.base.RecordObject;
import ca.on.mshri.lore.operations.LoreOperation;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class MockupParser extends LoreOperation{
    
    @Override
    public void run() {
                
        Authority authA = Authority.createOrGet(getModel(), "A");
        Authority authB = Authority.createOrGet(getModel(), "B");
        
        RecordObject r1 = RecordObject.createOrGet(getModel(), authA, "1");
        r1.addXRef(authB, "foo");
        RecordObject r2 = RecordObject.createOrGet(getModel(), authA, "2");
        r2.addXRef(authB, "foo");
        
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
