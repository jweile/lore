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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public abstract class LoreOperation {
    
    private Map<Parameter<?>,Object> paramValues = new HashMap<Parameter<?>, Object>();
    
    public abstract void run();
    
    public Set<Parameter<?>> getParameters() {
        Set<Parameter<?>> set = new HashSet<Parameter<?>>();
        for (Field f : this.getClass().getFields()) {
            if (Parameter.class.isAssignableFrom(f.getType())) {
                try {
                    set.add((Parameter)f.get(this));
                } catch (Exception ex) {
                    Logger.getLogger(LoreOperation.class.getName())
                            .log(Level.WARNING, "Inaccessible parameter!", ex);
                } 
            }
        }
        return set;
    }
    
    public <T> void setParameter(Parameter<T> param, T value) {
        if (!getParameters().contains(param)) {
            throw new IllegalArgumentException("Undefined parameter");
        }
        
        paramValues.put(param, value);
    }
    
    public <T> T getParameterValue(Parameter<T> param) {
        T val =  (T) paramValues.get(param);
        if (val == null) {
            val = param.getDefaultValue();
        }
        return val;
    }

    public boolean hasParameter(String name) {
        boolean has = false;
        for (Field f : this.getClass().getFields()) {
            if (Parameter.class.isAssignableFrom(f.getType())) {
                has |= f.getName().equals(name);
            }
        }
        return has;
    }
    
    public static class Parameter<T> {
        
        private String id;
        
        private Class<T> type;
        
        private T defaultValue;

        private Parameter(String id, Class<T> type) {
            this.id = id;
            this.type = type;
        }
                
        public static <T> Parameter<T> make(String id, Class<T> clazz) {
            return new Parameter<T>(id,clazz);
        }
        
        public static <T> Parameter<T> make(String id, Class<T> clazz, T defaultValue) {
            Parameter<T> p = new Parameter<T>(id,clazz);
            p.setDefaultValue(defaultValue);
            return p;
        }

        public String getId() {
            return id;
        }

        public Class<T> getType() {
            return type;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Parameter<T> other = (Parameter<T>) obj;
            if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
                return false;
            }
            if (this.type != other.type && (this.type == null || !this.type.equals(other.type))) {
                return false;
            }
            return true;
        }
        
        
    }
}
