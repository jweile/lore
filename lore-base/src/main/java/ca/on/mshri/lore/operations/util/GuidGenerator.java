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
package ca.on.mshri.lore.operations.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Generates global unique ids, consisting of the host name, time in nanoseconds
 * and an increasing number. 
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class GuidGenerator {
    
    private static long last = System.nanoTime();
    
    public synchronized static String generate() {
        
        try {
            
            StringBuilder b = new StringBuilder();
            
            String host = InetAddress.getLocalHost().getHostName();
            b.append(host).append(":");
            b.append(last++);
            
            return b.toString();
            
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Cannot retrieve hostname for GUID generation!",ex);
        }
        
    }
    
}
