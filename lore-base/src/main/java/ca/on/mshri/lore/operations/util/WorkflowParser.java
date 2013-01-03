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
package ca.on.mshri.lore.operations.util;

import ca.on.mshri.lore.operations.LoreOperation;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.codehaus.jparsec.Scanners;
import org.codehaus.jparsec.Terminals;
import org.codehaus.jparsec.Token;
import org.codehaus.jparsec.functors.Map;
import org.codehaus.jparsec.functors.Pair;
import org.codehaus.jparsec.functors.Tuple3;

/**
 *
 * @author Jochen Weile <jochenweile@gmail.com>
 */
public class WorkflowParser {
    
    
    public Workflow parse(InputStream in) {
        
        Terminals operators = Terminals.operators("=", "(", ")",",",".");
        
        //lexer 
        
        Parser<Void> ws = Scanners.WHITESPACES.or(Scanners.lineComment("#")).skipMany();
                
        Parser<?> identifierTokenizer = Terminals.Identifier.TOKENIZER;
        Parser<?> valueTokenizer = Parsers.or(
                Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER, 
                Terminals.DecimalLiteral.TOKENIZER,
                Terminals.IntegerLiteral.TOKENIZER
        );
        
        Parser<?> lexer = Parsers.or(operators.tokenizer(), identifierTokenizer, valueTokenizer);
        
        
        //syntactic parser
        
        Parser<String> identifierParser = Terminals.Identifier.PARSER;
        
        Parser<Object> valueParser = Parsers.or(
                (Parser<? extends Object>)Terminals.StringLiteral.PARSER, 
                Terminals.DecimalLiteral.PARSER.map(new org.codehaus.jparsec.functors.Map<String,Double>() {

                    public Double map(String from) {
                        return Double.parseDouble(from);
                    }
                    
                }),
                Terminals.IntegerLiteral.PARSER.map(new org.codehaus.jparsec.functors.Map<String,Integer>() {

                    public Integer map(String from) {
                        return Integer.parseInt(from);
                    }
                    
                })
        );
        
        Parser<String> methodNameParser = identifierParser.sepBy1(operators.token(".")).map(new org.codehaus.jparsec.functors.Map<List<String>,String>() {

            public String map(List<String> from) {
                StringBuilder b = new StringBuilder();
                for (String s : from) {
                    b.append(s).append('.');
                }
                if (b.length() > 0) {
                    b.deleteCharAt(b.length()-1);
                }
                return b.toString();
            }
           
        });
        
        Parser<Pair<String,Object>> parameterParser = Parsers.tuple(identifierParser, operators.token("="), valueParser)
                .map(new org.codehaus.jparsec.functors.Map<Tuple3<String, Token, Object>,Pair<String,Object>>() {

            public Pair<String, Object> map(Tuple3<String, Token, Object> from) {
                return new Pair(from.a,from.c);
            }
            
        });
        
        Parser<HashMap<String,Object>> parameterBlockParser = Parsers.between(
                operators.token("("), 
                parameterParser.sepBy(operators.token(",")), 
                operators.token(")")
        ).map(new Map<List<Pair<String, Object>>,HashMap<String,Object>>() {

            public HashMap<String, Object> map(List<Pair<String, Object>> from) {  
                HashMap<String,Object> values = new HashMap<String, Object>();
                for (Pair<String,Object> pair : from) {
                    values.put(pair.a, pair.b);
                }
                return values;
            }
            
        });
        
        Parser<Pair<String, HashMap<String,Object>>> methodCallParser = Parsers.pair(methodNameParser, parameterBlockParser);
        
        //plug lexer into parser
        Parser<List<Pair<String, HashMap<String,Object>>>> parser = methodCallParser.many().from(lexer,ws);
        
        Workflow workflow = new Workflow();
        
        //get input
        try {
            
            List<Pair<String, HashMap<String,Object>>> methods = parser.parse(new InputStreamReader(in));
            
            for (Pair<String, HashMap<String,Object>> method : methods) {
                
                LoreOperation op = getOperation(method.a);
                
                processParameters(op, method.b);
                
                workflow.add(op);
                
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Parsing workflow failed!",ex);
        }
        
        return workflow;
        
    }

    /**
     * 
     * @param className
     * @return 
     */
    private LoreOperation getOperation(String className) {
        
        try {
            Class<?> clazz = Class.forName(className);
            
            if (!LoreOperation.class.isAssignableFrom(clazz)) {
                throw new RuntimeException(className+" is not a known Lore operation");
            }
            
            LoreOperation op = (LoreOperation)clazz.getConstructor().newInstance();
            
            return op;
            
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(className+" is not a valid class name", ex);
        } catch (Exception e) {
            throw new RuntimeException(className+" could not be instantiated.", e);
        }
    }

    /**
     * 
     * @param op
     * @param b 
     */
    private void processParameters(LoreOperation op, HashMap<String,Object> paraVals) {
        
        Set<Parameter<?>> parameters = op.getParameters();
        
        Set<String> parameterNames = new HashSet<String>();
        for (Parameter<?> parameter : parameters) {
            parameterNames.add(parameter.getId());
        }
        
        for (String key : paraVals.keySet()) {
            
            if (!parameterNames.contains(key)) {
                Logger.getLogger(WorkflowParser.class.getName())
                        .log(Level.WARNING, "Parameter "+key+
                        " is not defined for operation "+
                        op.getClass().getSimpleName()+". Ignoring...");
            }
            
        }
              
        for (Parameter<?> parameter : parameters) {
            
            Object value = paraVals.get(parameter.getId());
            
            //skip empty parameters
            if (value == null) {
                continue;
            }
                        
            if (parameter.getType().equals(String.class)) {
                
                Parameter<String> p = (Parameter<String>)parameter;
                op.setParameter(p, p.validate(value));
                
            } else if (parameter.getType().equals(Integer.class)) {
                
                Parameter<Integer> p = (Parameter<Integer>)parameter;
                op.setParameter(p, p.validate(value));
                
            } else if (parameter.getType().equals(Double.class)) {
                
                Parameter<Double> p = (Parameter<Double>)parameter;
                op.setParameter(p, p.validate(value));
                
            } else if (parameter.getType().equals(Boolean.class)) {
                  
                Parameter<Boolean> p = (Parameter<Boolean>)parameter;
                op.setParameter(p, p.validate(value));
                
            } else if (parameter instanceof RefListParameter) {
                
                RefListParameter<?> p = (RefListParameter<?>) parameter;
                op.setParameter(p, p.validate(value));
                 
            } else {
                throw new RuntimeException("Unsupported parameter type: "+parameter.getType());
            }
            
        }
        
    }
}
