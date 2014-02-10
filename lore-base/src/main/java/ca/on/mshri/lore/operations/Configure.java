/*
 * Copyright (C) 2014 Department of Molecular Genetics, University of Toronto
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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.operations.util.Parameter;
import java.util.Properties;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Configure extends LoreOperation {

    /**
     * print summaries after each workflow operation?
     */
    public static final String SUMMARIES_KEY = "lore.summaries";
    public final Parameter<Boolean> summariesP = Parameter.make("summaries", Boolean.class, true);
    /**
     * commit to database after each operation?
     */
    public static final String COMMIT_KEY = "lore.commit";
    public final Parameter<Boolean> commitP = Parameter.make("commit", Boolean.class, true);
    
    @Override
    public void run() {
        Properties p = System.getProperties();
        p.setProperty(SUMMARIES_KEY, getParameterValue(summariesP)+"");
        p.setProperty(COMMIT_KEY, getParameterValue(commitP)+"");
    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }
        
}
