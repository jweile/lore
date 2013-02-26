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
package ca.on.mshri.lore.molecules.util;

import java.util.Collection;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Vector3D {

    private double x,y,z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public static Vector3D centroid(Collection<Vector3D> vecs) {
        
        Vector3D centroid = new Vector3D(0, 0, 0);
        
        for (Vector3D vec : vecs) {
            centroid.transpose(vec);
        }
        
        centroid.scale(1.0/(double)vecs.size());
        
        return centroid;
    }

    public Vector3D plus(Vector3D vec) {
        return new Vector3D(x+vec.x, y+vec.y, z+vec.z);
    }
    
    public Vector3D minus(Vector3D v) {
        return new Vector3D(x-v.x, y-v.x, z-v.z);
    }
    
    public Vector3D times(double d) {
        return new Vector3D(x*d, y*d, z*d);
    }

    private void transpose(Vector3D v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    private void scale(double d) {
        x *= d;
        y *= d;
        z *= d;
    }

    public double dotProduct(Vector3D v) {
        return x*v.x + y*v.y + z*v.z;
    }
    
    public static double euclidianNorm(Vector3D v) {
        return Math.sqrt(v.dotProduct(v));
    }
    
    public static double euclidianDistance(Vector3D v, Vector3D w) {
        return euclidianNorm(v.minus(w));
    }
            
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(');
        b.append(String.format("%.2f",x));
        b.append(',');
        b.append(String.format("%.2f",y));
        b.append(',');
        b.append(String.format("%.2f",z));
        b.append(')');
        return b.toString();
    }
    
    
}
