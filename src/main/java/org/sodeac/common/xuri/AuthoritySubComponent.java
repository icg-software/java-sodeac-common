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
 * Subcomponent of URI authority (user, password, host, port ....)
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class AuthoritySubComponent implements IExtensible, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = -8307822793728177004L;
    
    private List<IExtension<?>> extensions = null;
    private volatile List<IExtension<?>> extensionsImmutable = null;
    private String expression = null;
    private String value = null;
    private char prefixDelimiter = ':';
    private char postfixDelimiter = ':';
    
    protected AuthoritySubComponent(final String expression, final String value)
    {
        super();
        this.extensions = new ArrayList<IExtension<?>>();
        this.expression = expression;
        this.value = value;
    }
    
    /**
     * setter for expression
     *
     * @param expression
     */
    protected void setExpression(final String expression)
    {
        this.expression = expression;
    }
    
    /**
     * setter for prefix delimiter
     *
     * @param delimiter
     */
    protected void setPrefixDelimiter(final char delimiter)
    {
        this.prefixDelimiter = delimiter;
    }
    
    /**
     * getter prefix delimiter
     *
     * @return prefix delimiter
     */
    public char getPrefixDelimiter()
    {
        return this.prefixDelimiter;
    }
    
    /**
     * setter for postfix delimiter
     *
     * @param postfixDelimiter
     */
    protected void setPostfixDelimiter(final char postfixDelimiter)
    {
        this.postfixDelimiter = postfixDelimiter;
    }
    
    /**
     * getter for postfix delimiter
     *
     * @return postfix delimiter
     */
    public char getPostfixDelimiter()
    {
        return this.postfixDelimiter;
    }
    
    /**
     * Append an extension for this authority subcomponent
     *
     * @param extension
     */
    protected void addExtension(final IExtension<?> extension)
    {
        if (this.extensions == null)
        {
            this.extensions = new ArrayList<IExtension<?>>();
        }
        
        this.extensions.add(extension);
        this.extensionsImmutable = null;
    }
    
    @Override
    public IExtension<?> getExtension(final String type)
    {
        List<IExtension<?>> extensionList = getExtensionList();
        
        if ((type == null) && (!extensionList.isEmpty()))
        {
            return extensionList.get(0);
        }
        for (final IExtension<?> extension : extensionList)
        {
            if (type.equals(extension.getType()))
            {
                return extension;
            }
        }
        return null;
    }
    
    @Override
    public List<IExtension<?>> getExtensionList()
    {
        List<IExtension<?>> extensionList = this.extensionsImmutable;
        if (extensionList == null)
        {
            extensionList = this.extensionsImmutable;
            if (extensionList != null)
            {
                return extensionList;
            }
            this.extensionsImmutable = Collections.unmodifiableList(this.extensions == null ? new ArrayList<IExtension<?>>() : new ArrayList<IExtension<?>>(this.extensions));
            extensionList = this.extensionsImmutable;
        }
        return extensionList;
    }
    
    @Override
    public List<IExtension<?>> getExtensionList(final String type)
    {
        List<IExtension<?>> extensionList = new ArrayList<IExtension<?>>();
        for (final IExtension<?> extension : getExtensionList())
        {
            if (type.equals(extension.getType()))
            {
                extensionList.add(extension);
            }
        }
        return extensionList;
    }
    
    /**
     * getter for representative string for authority subcomponent (value and extensions)
     *
     * @return representative string for authority subcomponent
     */
    public String getExpression()
    {
        return this.expression;
    }
    
    /**
     * getter for authority subcomponent value
     *
     * @return value of authority subcomponent
     */
    public String getValue()
    {
        return this.value;
    }
    
}
