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

/*
 * @see {@link https://tools.ietf.org/html/rfc3986#section-3}
 */

/**
 * Enum of URI - components types.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public enum ComponentType
{
    SCHEME(1, "scheme"),
    AUTHORITY(2, "authority"),
    PATH(3, "path"),
    QUERY(4, "query"),
    FRAGMENT(5, "fragment");
    
    private ComponentType(int intValue, String name)
    {
        this.intValue = intValue;
        this.name = name;
    }
    
    private static volatile Set<ComponentType> ALL = null;
    
    private int intValue;
    private String name = null;
    
    public int getIntValue()
    {
        return this.intValue;
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public static Set<ComponentType> getAll()
    {
        if (ComponentType.ALL == null)
        {
            EnumSet<ComponentType> all = EnumSet.allOf(ComponentType.class);
            ComponentType.ALL = Collections.unmodifiableSet(all);
        }
        return ComponentType.ALL;
    }
    
    public static ComponentType findByInteger(int value)
    {
        for (ComponentType component : getAll())
        {
            if (component.intValue == value)
            {
                return component;
            }
        }
        return null;
    }
    
    public static ComponentType findByName(String name)
    {
        for (ComponentType component : getAll())
        {
            if (component.name.equalsIgnoreCase(name))
            {
                return component;
            }
        }
        return null;
    }
}
