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
package ca.on.mshri.lore.genome;

import ca.on.mshri.lore.genome.util.GeneticCode;
import junit.framework.TestCase;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class GeneticCodeTest extends TestCase {
    
    public void test() throws Exception {
        
        GeneticCode codons = GeneticCode.getInstance();
        
        System.out.print("\t");
        for (String aa : codons.getAminoacids()) {
            System.out.print(aa+"\t");
        }
        System.out.println();
        for (String aa1 : codons.getAminoacids()) {
            System.out.print(aa1+"\t");
            for (String aa2 : codons.getAminoacids()) {
                int num = codons.explainingSNPs(aa1, aa2);
                System.out.print((num == 0 ? "." : num) +"\t");
            }
            System.out.println();
        }
        
    }
    
}
