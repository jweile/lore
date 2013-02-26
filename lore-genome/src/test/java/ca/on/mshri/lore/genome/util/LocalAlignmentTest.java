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
package ca.on.mshri.lore.genome.util;

import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class LocalAlignmentTest extends TestCase {
    
    public LocalAlignmentTest(String testName) {
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
        
        LocalAlignment al = new LocalAlignment(
                "GSSGSSGSIGNAQKLPMCDKCGTGIVGVFVKLRDRHRHPECYVCTDCGTNLKQKGHFFVEDQIYCEKHARERVSGPSSG", 
                "MTTQQIDLQGPGPWGFRLVGGKDFEQPLAISRVTPGSKAALANLCIGDVITAIDGENTSNMTHLEAQNRIKGCTDNLTL"
                + "TVARSEHKVWSPLVTEEGKRHPYKMNLASEPQEVLHIGSAHNRSAMPFTASPASSTTARVITNQYNNPAGLYSSENI"
                + "SNFNNALESKTAASGVEANSRPLDHAQPPSSLVIDKESEVYKMLQEKQELNEPPKQSTSFLVLQEILESEEKGDPNK"
                + "PSGFRSVKAPVTKVAASIGNAQKLPMCDKCGTGIVGVFVKLRDRHRHPECYVCTDCGTNLKQKGHFFVEDQIYCEKH"
                + "ARERVTPPEGYEVVTVFPK");
        
        System.out.println(al.getAlignment());
        
        print(al.getA2B());
        print(al.getB2A());
    }

    private void print(int[] is) {
        for (int i = 0; i < is.length; i++) {
            String num = is[i]+"";
            for (int j = 0; j < 4-num.length(); j++)  System.out.print(" ");
            System.out.print(num);
        }
        System.out.println("");
    }
}
