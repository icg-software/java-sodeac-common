/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.api;

import org.sodeac.common.message.dispatcher.api.IPropertyBlockModifyListener.ModifyType;

public class PropertyBlockModifyItem
{
    public PropertyBlockModifyItem(final ModifyType type, final String key, final Object valueOld, final Object valueNew)
    {
        super();
        this.type = type;
        this.key = key;
        this.valueOld = valueOld;
        this.valueNew = valueNew;
    }
    
    private ModifyType type;
    private String key;
    private Object valueOld;
    private Object valueNew;
    
    public ModifyType getType()
    {
        return this.type;
    }
    
    public void setType(final ModifyType type)
    {
        this.type = type;
    }
    
    public String getKey()
    {
        return this.key;
    }
    
    public void setKey(final String key)
    {
        this.key = key;
    }
    
    public Object getValueOld()
    {
        return this.valueOld;
    }
    
    public void setValueOld(final Object valueOld)
    {
        this.valueOld = valueOld;
    }
    
    public Object getValueNew()
    {
        return this.valueNew;
    }
    
    public void setValueNew(final Object valueNew)
    {
        this.valueNew = valueNew;
    }
}
