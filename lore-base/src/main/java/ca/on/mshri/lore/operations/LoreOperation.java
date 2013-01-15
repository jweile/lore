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

import ca.on.mshri.lore.base.LoreModel;
import ca.on.mshri.lore.operations.util.Parameter;
import com.hp.hpl.jena.rdf.model.Model;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An operation on a lore model. Any classes extending this class will be able
 * to be called from a workflow. Each operation has a set of parameters, that 
 * are simply defined as fields of type Parameter.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public abstract class LoreOperation {
    
    /**
     * The values for each parameter.
     */
    private Map<Parameter<?>,Object> paramValues = new HashMap<Parameter<?>, Object>();
    
    /**
     * the model on which this operation works.
     */
    private LoreModel model;
    
    /**
     * Performs the operation.
     */
    public abstract void run();
    
    /**
     * Gets the set of all parameters defined on this operation.
     * @return 
     */
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
    
    /**
     * sets a a value for a given parameter
     * @param <T> the type of the parameter.
     * @param param the parameter.
     * @param value the value.
     * @throws IllegalArgumentException if the given parameter does not exist
     * for this operation.
     */
    public <T> void setParameter(Parameter<T> param, T value) {
        if (!getParameters().contains(param)) {
            throw new IllegalArgumentException("Undefined parameter");
        }
        
        paramValues.put(param, value);
    }
    
    /**
     * gets the value of a parameter
     * @param <T> the parameter type
     * @param param the parameter
     * @return the value
     */
    public <T> T getParameterValue(Parameter<T> param) {
        T val =  (T) paramValues.get(param);
        if (val == null) {
            val = param.getDefaultValue();
        }
        return val;
    }

    /**
     * checks whether this operation has a parameter by the name given.
     * @param name a parameter name.
     * @return 
     */
    public boolean hasParameter(String name) {
        boolean has = false;
        for (Field f : this.getClass().getFields()) {
            if (Parameter.class.isAssignableFrom(f.getType())) {
                has |= f.getName().equals(name);
            }
        }
        return has;
    }

    /**
     * gets the model
     * @return 
     */
    public LoreModel getModel() {
        return model;
    }

    /**
     * sets the model for this operation.
     * @param model 
     */
    public void setModel(LoreModel model) {
        this.model = model;
    }

    /**
     * whether or not this operation requires an active reasoner to work.
     * @return 
     */
    public abstract boolean requiresReasoner();
    
    
}
