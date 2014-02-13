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
package ca.on.mshri.lore.generic;

import ca.on.mshri.lore.generic.OboParser.Stanza;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OboParserTest extends TestCase {
    
    public OboParserTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void test() throws Exception {
        Map<String, Stanza> stanzas = new OboParser().parse(new FileInputStream("src/test/resources/HumanDO.obo"));
        int i = 0;
        for (Stanza stanza : stanzas.values()) {
            if (i++ >= 10) {
                break;
            }
            System.err.println(stanza.getID());
            System.err.println(stanza.getName());
            System.err.println();
        }
    }
}
