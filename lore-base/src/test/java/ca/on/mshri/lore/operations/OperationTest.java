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
package ca.on.mshri.lore.operations;

import ca.on.mshri.lore.operations.util.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class OperationTest extends TestCase {
    
    public OperationTest(String testName) {
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
        
        LoreOperationImpl o = new LoreOperationImpl();
        
        Set<Parameter<?>> params = o.getParameters();
        assertEquals(2,params.size());
        for (Parameter<?> param : params) {
            System.out.println(param.getId()+" : "+param.getType());
        }
        
        o.setParameter(o.nameP, "example");
        
        List<Integer> list = new ArrayList<Integer>() {{
            add(3);
            add(5);
        }};
        o.setParameter(o.listP, list);
        
        o.run();
        
    }

    private static class LoreOperationImpl extends LoreOperation {

        public final Parameter<String> nameP = Parameter.make("name", String.class);
        public final Parameter<List> listP = Parameter.make("list", List.class);

        @Override            
        public void run() {
            
            String name = getParameterValue(nameP);
            List<Integer> list = getParameterValue(listP);
            
            System.out.println(name);
            for (int i : list) {
                System.out.println(i);
            }
            
        }
    }
}
