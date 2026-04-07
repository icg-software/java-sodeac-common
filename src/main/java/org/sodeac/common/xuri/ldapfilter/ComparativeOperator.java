/*******************************************************************************
 * Copyright (c) 2016, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/

package org.sodeac.common.xuri.ldapfilter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enum of ldap operators
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public enum ComparativeOperator
{
    EQUAL(1, "="),
    GTE(2, ">="),
    LTE(3, "<="),
    APPROX(4, "~=");
    
    ComparativeOperator(final int intValue, final String abbreviation)
    {
        this.intValue = intValue;
        this.abbreviation = abbreviation;
    }
    
    private static volatile Set<ComparativeOperator> ALL = null;
    
    private final int intValue;
    private final String abbreviation;
    
    public int getIntValue()
    {
        return this.intValue;
    }
    
    public String getAbbreviation()
    {
        return this.abbreviation;
    }
    
    public static Set<ComparativeOperator> getAll()
    {
        if (ComparativeOperator.ALL == null)
        {
            EnumSet<ComparativeOperator> all = EnumSet.allOf(ComparativeOperator.class);
            ComparativeOperator.ALL = Collections.unmodifiableSet(all);
        }
        return ComparativeOperator.ALL;
    }
    
    public static ComparativeOperator findByInteger(final int value)
    {
        for (final ComparativeOperator operation : getAll())
        {
            if (operation.intValue == value)
            {
                return operation;
            }
        }
        return null;
    }
    
    public static ComparativeOperator findByAbbreviation(final String abbreviation)
    {
        for (final ComparativeOperator operation : getAll())
        {
            if (operation.abbreviation.equals(abbreviation))
            {
                return operation;
            }
        }
        return null;
    }
    
    public static ComparativeOperator findByName(final String name)
    {
        for (final ComparativeOperator operation : getAll())
        {
            if (operation.name().equalsIgnoreCase(name))
            {
                return operation;
            }
        }
        return null;
    }
}
