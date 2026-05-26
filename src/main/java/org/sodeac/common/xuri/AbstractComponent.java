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
package org.sodeac.common.xuri;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for URI components. Components contains subcomponents and a representative expression string.
 *
 * @param <T>
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractComponent<T> implements IComponent<T>, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = -613198655700716268L;
    
    private ComponentType componentType = null;
    private String expression = null;
    
    /**
     * constructer must invoke by subclass.
     *
     * @param componentType type of component
     */
    protected AbstractComponent(final ComponentType componentType)
    {
        super();
        this.componentType = componentType;
        this.subComponents = new ArrayList<T>();
    }
    
    private List<T> subComponents = null;
    private volatile List<T> subComponentsImmutable = null;
    private volatile T first = null;
    private volatile T last = null;
    
    /**
     * Adds a subcomponent. For example: A path component (Component) should add pathsegment subcomponents
     *
     * @param subComponent
     *
     * @return
     */
    protected AbstractComponent<T> addSubComponent(final T subComponent)
    {
        this.subComponents.add(subComponent);
        this.subComponentsImmutable = null;
        if (this.first == null)
        {
            this.first = subComponent;
        }
        this.last = subComponent;
        return this;
    }
    
    /**
     * getter for subcomponents.
     *
     * @return immutable list of all subcomponents.
     */
    public List<T> getSubComponentList()
    {
        List<T> subComponentList = this.subComponentsImmutable;
        if (subComponentList == null)
        {
            subComponentList = this.subComponentsImmutable;
            if (subComponentList != null)
            {
                return subComponentList;
            }
            this.subComponentsImmutable = Collections.unmodifiableList(new ArrayList<T>(this.subComponents));
            subComponentList = this.subComponentsImmutable;
        }
        return subComponentList;
    }
    
    /**
     * getter for component type
     *
     * @return component type
     */
    public ComponentType getComponentType()
    {
        return this.componentType;
    }
    
    /**
     * getter for expression string
     *
     * @return string represents this component part of URI
     */
    public String getExpression()
    {
        return this.expression;
    }
    
    /**
     * setter for expression string
     *
     * @param expression string represents this component part of URI
     */
    protected void setExpression(final String expression)
    {
        this.expression = expression;
    }
    
    /**
     * getter for first subcomponent
     *
     * @return first subcomponent
     */
    public T getFirst()
    {
        return this.first;
    }
    
    /**
     * getter for last subcomponent
     *
     * @return last subcomponent
     */
    public T getLast()
    {
        return this.last;
    }
}
