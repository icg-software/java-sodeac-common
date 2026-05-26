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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Criteria linker represents an ldap query and consists of multiple ldap criterias or other criteria linker.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class CriteriaLinker implements IFilterItem, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 3252972156160851293L;
    
    /**
     * constructor of criteria linker
     */
    public CriteriaLinker()
    {
        super();
        this.lock = new ReentrantLock();
    }
    
    private volatile boolean invert = false;
    private volatile LogicalOperator operator = LogicalOperator.AND;
    private Lock lock = null;
    
    private final List<IFilterItem> linkedItemList = new ArrayList<IFilterItem>();
    private List<IFilterItem> linkedItemListCopy = null;
    
    /**
     * getter for linked filter item list
     *
     * @return linked filter item list
     */
    public List<IFilterItem> getLinkedItemList()
    {
        List<IFilterItem> itemList = this.linkedItemListCopy;
        if (itemList != null)
        {
            return itemList;
        }
        this.lock.lock();
        try
        {
            itemList = this.linkedItemListCopy;
            if (itemList != null)
            {
                return itemList;
            }
            
            this.linkedItemListCopy = Collections.unmodifiableList(new ArrayList<IFilterItem>(this.linkedItemList));
            return this.linkedItemListCopy;
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    /**
     * adds new filter item to list
     *
     * @param item filter item to add
     *
     * @return criteria linker
     */
    protected CriteriaLinker addItem(final IFilterItem item)
    {
        this.lock.lock();
        try
        {
            this.linkedItemList.add(item);
            this.linkedItemListCopy = null;
        }
        finally
        {
            this.lock.unlock();
        }
        
        return this;
    }
    
    @Override
    public boolean isInvert()
    {
        return this.invert;
    }
    
    protected CriteriaLinker setInvert(final boolean invert)
    {
        this.invert = invert;
        return this;
    }
    
    /**
     * getter for link operator
     *
     * @return link operator
     */
    public LogicalOperator getOperator()
    {
        return this.operator;
    }
    
    /**
     * setter for link operator
     *
     * @param operator link operator
     *
     * @return this
     */
    protected CriteriaLinker setOperator(final LogicalOperator operator)
    {
        this.operator = operator;
        return this;
    }
    
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        
        stringBuilder.append("(");
        
        if (this.invert)
        {
            stringBuilder.append("!(");
        }
        
        stringBuilder.append(this.operator.getAbbreviation());
        
        if (this.linkedItemList != null)
        {
            for (final IFilterItem filterItem : this.linkedItemList)
            {
                stringBuilder.append(filterItem.toString());
            }
        }
        
        if (this.invert)
        {
            stringBuilder.append(")");
        }
        
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
    
    @Override
    public boolean matches(final Map<String, IMatchable> properties)
    {
        if (this.operator == LogicalOperator.OR)
        {
            for (final IFilterItem filterItem : getLinkedItemList())
            {
                if (filterItem.matches(properties))
                {
                    return !this.invert;
                }
            }
            return this.invert;
        }
        
        for (final IFilterItem filterItem : getLinkedItemList())
        {
            if (!filterItem.matches(properties))
            {
                return this.invert;
            }
        }
        return !this.invert;
    }
}