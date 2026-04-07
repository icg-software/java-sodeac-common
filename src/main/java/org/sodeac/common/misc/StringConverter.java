/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.misc;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class StringConverter
{
    private static Map<String, Function<Object, String>> toStringIndex = null;
    private static Map<String, Function<String, Object>> fromStringIndex = null;
    
    static
    {
        Map<String, Function<Object, String>> toString = new HashMap<String, Function<Object, String>>();
        Map<String, Function<String, Object>> fromString = new HashMap<String, Function<String, Object>>();
        
        toString.put(String.class.getCanonicalName(), p -> (String) p);
        fromString.put(String.class.getCanonicalName(), p -> p);
        
        toString.put(Long.class.getCanonicalName(), (Function) Converter.LongToString);
        fromString.put(Long.class.getCanonicalName(), (Function) Converter.StringToLong);
        
        toString.put(Integer.class.getCanonicalName(), (Function) Converter.IntegerToString);
        fromString.put(Integer.class.getCanonicalName(), (Function) Converter.StringToInteger);
        
        toString.put(Double.class.getCanonicalName(), (Function) Converter.DoubleToString);
        fromString.put(Double.class.getCanonicalName(), (Function) Converter.StringToDouble);
        
        toString.put(Boolean.class.getCanonicalName(), (Function) Converter.BooleanToString);
        fromString.put(Boolean.class.getCanonicalName(), (Function) Converter.StringToBoolean);
        
        toString.put(Date.class.getCanonicalName(), (Function) Converter.DateToISO8601);
        fromString.put(Date.class.getCanonicalName(), (Function) Converter.ISO8601ToDate);
        
        toString.put(UUID.class.getCanonicalName(), (Function) Converter.UUIDToString);
        fromString.put(UUID.class.getCanonicalName(), (Function) Converter.StringToUUID);
        
        toString.put(Version.class.getCanonicalName(), (Function) Converter.VersionToString);
        fromString.put(Version.class.getCanonicalName(), (Function) Converter.StringToVersion);
        
        toString.put(Class.class.getCanonicalName(), (Function) Converter.ClassToString);
        fromString.put(Class.class.getCanonicalName(), (Function) Converter.StringToClass);
        
        StringConverter.fromStringIndex = Collections.unmodifiableMap(fromString);
        StringConverter.toStringIndex = Collections.unmodifiableMap(toString);
    }
    
    public static Map<String, Function<Object, String>> toStringIndex()
    {
        return toStringIndex;
    }
    
    public static Map<String, Function<String, Object>> fromStringIndex()
    {
        return fromStringIndex;
    }
}
