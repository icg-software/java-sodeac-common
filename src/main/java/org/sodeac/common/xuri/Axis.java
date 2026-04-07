/*******************************************************************************
 * Copyright (c) 2016, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * @see {@link https://www.w3.org/TR/xpath/#axes}
 */

/**
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public enum Axis
{
    CHILD(1, "child"),
    DESCENDANT(2, "descendant", "//"),
    PARENT(3, "parent", ".."),
    ANCESTOR(4, "ancestor"),
    FOLLOWING_SIBLING(5, "following-sibling"),
    PRECEDING_SIBLING(6, "preceding-sibling"),
    FOLLOWING(7, "following"),
    PRECEDING(8, "preceding"),
    ATTRIBUTE(9, "attribute", "@"),
    NAMESPACE(10, "namespace"),
    SELF(11, "self", "."),
    DESCENDANT_OR_SELF(12, "descendant-or-self"),
    ANCESTOR_OR_SELF(13, "ancestor-or-self");
    
    private Axis(int intValue)
    {
        this.intValue = intValue;
    }
    
    private Axis(int intValue, String xPathAxisName)
    {
        this.intValue = intValue;
        this.xPathAxisName = xPathAxisName;
    }
    
    private Axis(int intValue, String xPathAxisName, String xPathAxisAbbreviated)
    {
        this.intValue = intValue;
        this.xPathAxisName = xPathAxisName;
        this.xPathAxisAbbreviated = xPathAxisAbbreviated;
    }
    
    private static volatile Set<Axis> ALL = null;
    
    private int intValue;
    private String xPathAxisName = null;
    private String xPathAxisAbbreviated = null;
    
    public int getIntValue()
    {
        return intValue;
    }
    
    public String getXPathAxisName()
    {
        return xPathAxisName;
    }
    
    public String getXPathAxisAbbreviated()
    {
        return xPathAxisAbbreviated;
    }
    
    public static Set<Axis> getAll()
    {
        if (Axis.ALL == null)
        {
            EnumSet<Axis> all = EnumSet.allOf(Axis.class);
            Axis.ALL = Collections.unmodifiableSet(all);
        }
        return Axis.ALL;
    }
    
    public static Axis findByInteger(int value)
    {
        for (Axis axis : getAll())
        {
            if (axis.intValue == value)
            {
                return axis;
            }
        }
        return null;
    }
    
    public static Axis findByXPathAxisName(String xPathAxisName)
    {
        for (Axis axis : getAll())
        {
            if (axis.xPathAxisName.equalsIgnoreCase(xPathAxisName))
            {
                return axis;
            }
        }
        return null;
    }
    
    public static Axis findByXPathAxisAbbreviated(String xPathAxisAbbreviated)
    {
        for (Axis axis : getAll())
        {
            if (axis.xPathAxisAbbreviated.equalsIgnoreCase(xPathAxisAbbreviated))
            {
                return axis;
            }
        }
        return null;
    }
    
    public static Axis findByName(String name)
    {
        for (Axis axis : getAll())
        {
            if (axis.name().equalsIgnoreCase(name))
            {
                return axis;
            }
        }
        return null;
    }
    
    public static Axis parseAxisType(CharSequence charSequence, int position)
    {
        return parseAxisType(charSequence, new AtomicInteger(position));
    }
    
    public static Axis parseAxisType(CharSequence charSequence, AtomicInteger position)
    {
        int startposition = position.get();
        int currentposition = startposition;
        int maxposition = charSequence.length() - 1;
        char c = charSequence.charAt(currentposition);
        
        if (c == '@')
        {
            position.incrementAndGet();
            return Axis.ATTRIBUTE;
        }
        
        if (c == '/')
        {
            position.incrementAndGet();
            return Axis.DESCENDANT;
        }
        
        if (c == '.')
        {
            position.incrementAndGet();
            currentposition++;
            
            if ((currentposition > maxposition) || (charSequence.charAt(currentposition) == '.'))
            {
                position.incrementAndGet();
                return Axis.PARENT;
            }
            return Axis.SELF;
        }
        
        while ((c == ' ') || (c == '\t'))
        {
            currentposition++;
            if (currentposition > maxposition)
            {
                return null;
            }
            c = charSequence.charAt(currentposition);
        }
        
        // match test
        
        int startmatchposition = currentposition;
        all:
        for (Axis axis : getAll())
        {
            for (int i = 0; i < axis.xPathAxisName.length(); i++)
            {
                if ((currentposition > maxposition) || (axis.xPathAxisName.charAt(i) != c))
                {
                    currentposition = startmatchposition;
                    c = charSequence.charAt(currentposition);
                    continue all;
                }
                
                currentposition++;
                c = charSequence.charAt(currentposition);
            }
            
            if (((currentposition + 1) > maxposition) || (charSequence.charAt(currentposition) != ':') || (charSequence.charAt(currentposition + 1) != ':'))
            {
                currentposition = startmatchposition;
                c = charSequence.charAt(currentposition);
                continue all;
            }
            position.set(currentposition + 1);
            return axis;
        }
        
        return null;
    }
}
