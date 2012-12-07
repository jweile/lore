/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.mshri.lore.phenotype.omim;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Levenshtein {
    
    /**
     * Computes the Levenshtein distance between the two strings using the 
     * Needleman-Wunsch algorithm and returns 1-dist(a,b)/maxlen(a,b).
     * @param a the first string
     * @param b the second string
     * @return a score between 0 and 1 indicating how well the strings match
     */
    public static double score(String a, String b) {
        
        char[] as = a.toCharArray();
        char[] bs = b.toCharArray();
        
        int[][] mat = new int[as.length][bs.length];
        
        //init top left
        mat[0][0] = as[0] == bs[0] ? 0 : 1;
        
        //init first row
        for (int i = 1; i < as.length; i++) {
            mat[i][0] = i;
        }
        //init first column
        for (int j = 1; j < bs.length; j++) {
            mat[0][j] = j;
        }
        
        //fill matrix
        for (int i = 1; i < as.length; i++) {
            for (int j = 1; j < bs.length; j++) {
                int ins = mat[i][j-1] + 1;
                int del = mat[i-1][j] + 1;
                int rep = (as[i] == bs[j]) ? mat[i-1][j-1] : mat[i-1][j-1]+1;
                mat[i][j] = min(ins,del,rep);
            }
        }
        
//        //print matrix
//        for (int i = 0; i < mat.length; i++) {
//            for (int j = 0; j < mat[0].length; j++) {
//                System.out.print(mat[i][j]+" ");
//            }
//            System.out.println();
//        }
        
        double distance = (double)mat[as.length-1][bs.length-1];
        double maxLength = (double)Math.max(as.length, bs.length);
        
        return 1.0 - (distance / maxLength);
    }
    
    private static int min(int... vals) {
        int min = Integer.MAX_VALUE;
        for (int val : vals) {
            min = val < min ? val : min;
        }
        return min;
    }
    
}
