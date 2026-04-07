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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * https://tools.ietf.org/html/rfc3986#section-3.5
 */

/**
 * Fragment component of URI.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class FragmentComponent extends AbstractComponent<NoSubComponent> implements IExtensible
{
    /**
     *
     */
    private static final long serialVersionUID = -1184594786879375170L;
    
    private String value = null;
    
    private List<IExtension<?>> extensions = null;
    private volatile List<IExtension<?>> extensionsImmutable = null;
    
    protected FragmentComponent(String value)
    {
        super(ComponentType.FRAGMENT);
        this.value = value;
    }
    
    /**
     * Append an extension for this query segment
     *
     * @param extension
     */
    protected void addExtension(IExtension<?> extension)
    {
        if (this.extensions == null)
        {
            this.extensions = new ArrayList<IExtension<?>>();
        }
        this.extensions.add(extension);
        this.extensionsImmutable = null;
    }
    
    @Override
    public IExtension<?> getExtension(String type)
    {
        List<IExtension<?>> extensionList = getExtensionList();
        
        if ((type == null) && (!extensionList.isEmpty()))
        {
            return extensionList.get(0);
        }
        for (IExtension<?> extension : extensionList)
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
        List<IExtension<?>> extensionList = extensionsImmutable;
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
    public List<IExtension<?>> getExtensionList(String type)
    {
        List<IExtension<?>> extensionList = new ArrayList<IExtension<?>>();
        for (IExtension<?> extension : getExtensionList())
        {
            if (type.equals(extension.getType()))
            {
                extensionList.add(extension);
            }
        }
        return extensionList;
    }
    
    public String getValue()
    {
        return this.value;
    }
    
}
