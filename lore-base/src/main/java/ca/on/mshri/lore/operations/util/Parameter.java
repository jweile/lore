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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class Parameter<T> {
    private String id;
    private Class<T> type;
    private T defaultValue;

    protected Parameter(String id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    public static <T> Parameter<T> make(String id, Class<T> clazz) {
        return new Parameter<T>(id, clazz);
    }

    public static <T> Parameter<T> make(String id, Class<T> clazz, T defaultValue) {
        Parameter<T> p = new Parameter<T>(id, clazz);
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
    
    
    public T validate(Object value) {
        
        if (getType().equals(String.class)) {
                
            if (value instanceof String) {
                return (T)value;
            } else {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be a String. Coercing...");
                return (T)value.toString();
            }

        } else if (getType().equals(Integer.class)) {

            if (value instanceof Integer) {
                return (T) value;
            } else if (value instanceof Number) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be an Integer. Coercing...");
                return (T)(Integer)((Number)value).intValue();
            } else if (value instanceof String) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be an Integer. Coercing...");
                return (T)(Integer)Integer.parseInt((String)value);
            } else {
                throw new RuntimeException("Parameter "+getId()+" must be an integer number.");
            }

        } else if (getType().equals(Double.class)) {

            if (value instanceof Double) {
                return (T) value;
            } else if (value instanceof Number) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be a Decimal number. Coercing...");
                return (T)(Double)((Number)value).doubleValue();
            } else if (value instanceof String) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be a Decimal number. Coercing...");
                return (T)(Double)Double.parseDouble((String)value);
            } else {
                throw new RuntimeException("Parameter "+getId()+" must be a decimal number.");
            }

        } else if (getType().equals(Boolean.class)) {

            if (value instanceof Boolean) {
                return (T) value;
            } else if (value instanceof String) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+getId()+
                        " should be a Boolean. Coercing...");
                return (T) (Boolean) Boolean.parseBoolean((String)value);
            } else {
                throw new RuntimeException("Parameter "+getId()+" must be a boolean value.");
            }

        } else {
            throw new UnsupportedOperationException("Unsupported parameter type: "+getType());
        }
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
