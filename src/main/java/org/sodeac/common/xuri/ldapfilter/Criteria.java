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

import java.io.Serializable;
import java.util.Map;

/**
 * Represents an ldap criteria.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class Criteria implements IFilterItem, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = -5791677902733009628L;
    
    /**
     * constructor of ldap filter critera
     */
    public Criteria()
    {
        super();
    }
    
    private boolean invert = false;
    private String name = null;
    private ComparativeOperator operator = null;
    private String rawValue = null;
    private String value = null;
    
    /**
     * getter for name of ldap criteria
     *
     * @return name of ldap criteria
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * setter for name of ldap criteria
     *
     * @param name ldap criteria name
     *
     * @return criteria
     */
    protected Criteria setName(String name)
    {
        this.name = name;
        return this;
    }
    
    /**
     * getter for ldap criteria operator
     *
     * @return operator
     */
    public ComparativeOperator getOperator()
    {
        return operator;
    }
    
    /**
     * setter for ldap criteria operator
     *
     * @param operator ldap criteria operator
     *
     * @return
     */
    protected Criteria setOperator(ComparativeOperator operator)
    {
        this.operator = operator;
        return this;
    }
    
    /**
     * getter for value of ldap criteria
     *
     * @return value of ldap criteria
     */
    public String getValue()
    {
        return value;
    }
    
    /**
     * setter for value of ldap criteria
     *
     * @param value ldap criteria value
     *
     * @return criteria
     */
    protected Criteria setRawValue(String rawValue)
    {
        this.rawValue = rawValue;
        if (this.rawValue.indexOf("\\") < 0)
        {
            this.value = this.rawValue;
        }
        else
        {
            this.value = LDAPFilterExtension.decodeFromHexEscaped(this.rawValue);
        }
        return this;
    }
    
    @Override
    public boolean isInvert()
    {
        return invert;
    }
    
    protected Criteria setInvert(boolean invert)
    {
        this.invert = invert;
        return this;
    }
    
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        
        if (invert)
        {
            stringBuilder.append("!(");
        }
        
        stringBuilder.append(name);
        
        if (operator != null)
        {
            if (operator == ComparativeOperator.EQUAL)
            {
                stringBuilder.append("=");
            }
            else if (operator == ComparativeOperator.GTE)
            {
                stringBuilder.append(">=");
            }
            else if (operator == ComparativeOperator.LTE)
            {
                stringBuilder.append("<=");
            }
            else if (operator == ComparativeOperator.APPROX)
            {
                stringBuilder.append("~=");
            }
        }
        
        if (value != null)
        {
            stringBuilder.append(rawValue);
        }
        
        if (invert)
        {
            stringBuilder.append(")");
        }
        
        stringBuilder.append(")");
        
        return stringBuilder.toString();
    }
    
    @Override
    public boolean matches(Map<String, IMatchable> properties)
    {
        if (properties == null)
        {
            return invert;
        }
        IMatchable matchable = properties.get(this.name);
        if ((matchable != null) && matchable.matches(operator, this.name, this.value))
        {
            return !invert;
        }
        return invert;
    }
}
