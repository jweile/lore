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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.operations.util.Parameter;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class TestOperation extends LoreOperation {
    
    public static final Parameter<Integer> fooP = Parameter.make("foo", Integer.class);
    
    public static final Parameter<String> barP = Parameter.make("bar", String.class);
    
    public static final Parameter<Boolean> bazP = Parameter.make("baz", Boolean.class);

    @Override
    public void run() {
        System.out.println(getParameterValue(fooP) +" "+getParameterValue(barP)+" "+getParameterValue(bazP));
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
    
}
