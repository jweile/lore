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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Smith-Waterman local alignment algorithm.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class LocalAlignment {
    
    /**
     * score matrix
     */
    private int[][] mat;
    /**
     * trace matrix
     */
    private byte[][] trace;
    
    /**
     * length of a and b
     */
    private int al, bl;
    
    char[] as, bs;
        
    
    /**
     * bitmasks for trace matrix
     */
    private static final byte UP = 0x01;
    private static final byte LEFT = 0x02;
    private static final byte DIAG = 0x04;
    private String alignment;
    private int[] a2b;
    private int[] b2a;
    
    public LocalAlignment(String a, String b) {
        
        as = ("-"+a).toCharArray();
        bs = ("-"+b).toCharArray();
        
        al = as.length;
        bl = bs.length;
        
        //init matrix
        mat = new int[al][bl];
        trace = new byte[al][bl];
                
        //init first column
        for (int i = 0; i < al; i++) {
            mat[i][0] = 0;
            trace[i][0] = UP;
        }
        //init first row
        for (int j = 0; j < bl; j++) {
            mat[0][j] = 0;
            trace[0][j] = LEFT;
        }
        trace[0][0] = 0;
        
        //fill matrix
        for (int i = 1; i < al; i++) {
            for (int j = 1; j < bl; j++) {
                int ins = mat[i][j-1] + 1;//adding letter from b only
                int del = mat[i-1][j] + 1;//adding letter from a only
                int rep = (as[i] == bs[j]) ? mat[i-1][j-1] : mat[i-1][j-1]+1;//adding letter from a and b
                
                int min = min(ins,del,rep);
                mat[i][j] = min;
                
                //leave trace
                if (ins == min) trace[i][j] |= LEFT;
                if (del == min) trace[i][j] |= UP;
                if (rep == min) trace[i][j] |= DIAG;
            }
        }
        
        trace();
        
//        //print
//        for (int i = 0; i < al; i++) {
//            for (int j = 0; j < bl; j++) {
//                String number = mat[i][j]+"";
//                //spacer
//                for (int k = 0; k < 2-number.length(); k++) System.out.print(" ");
//                System.out.print(number);
//            }
//            System.out.println();
//        }
//        
//        System.out.println();
//        
//        //print
//        for (int i = 0; i < al; i++) {
//            for (int j = 0; j < bl; j++) {
//                System.out.print(" "+trace[i][j]);
//            }
//            System.out.println();
//        }
        
    }
        
    private void trace() {
        a2b = new int[al-1];
        for (int i = 0; i < a2b.length; i++) a2b[i] = -1;
        b2a = new int[bl-1];
        for (int j = 0; j < b2a.length; j++) b2a[j] = -1;
        
        List<Character> abuf = new ArrayList();
        List<Character> bbuf = new ArrayList();
        
        int i = al-1;
        int j = bl-1;
        boolean end = false;
        while (!end) {
            byte tr = trace[i][j];
            if (isSet(tr, DIAG)) {
                abuf.add(as[i]);
                bbuf.add(bs[j]);
                if (as[i] == bs[j]) {
                    a2b[i-1] = j-1;
                    b2a[j-1] = i-1;
                }
                i--;
                j--;
            } else if (isSet(tr, LEFT)) {
                abuf.add('-');
                bbuf.add(bs[j]);
                j--;
            } else if (isSet(tr, UP)) {
                abuf.add(as[i]);
                bbuf.add('-');
                i--;
            } else {
                end = true;
            }
        }
        
        Collections.reverse(abuf);
        Collections.reverse(bbuf);
        
        alignment = toString(abuf) +"\n"+ toString(bbuf);
        
        
    }

    public String getAlignment() {
        return alignment;
    }

    public int[] getA2B() {
        return a2b;
    }

    public int[] getB2A() {
        return b2a;
    }
    
    /**
     * Read bitmask value
     * @param var
     * @param mask
     * @return 
     */
    private boolean isSet(byte var, byte mask) {
        return (var & mask) > 0;
    }
    
    private static int min(int... vals) {
        int min = Integer.MAX_VALUE;
        for (int val : vals) {
            min = val < min ? val : min;
        }
        return min;
    }

    private String toString(List<Character> in) {
        char[] chars = new char[in.size()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = in.get(i);
        }
        return new String(chars);
    }
    
}
