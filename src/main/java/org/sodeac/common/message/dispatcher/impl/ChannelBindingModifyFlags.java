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
package org.sodeac.common.message.dispatcher.impl;

public class ChannelBindingModifyFlags
{
    
    protected ChannelBindingModifyFlags()
    {
        super();
    }
    
    private boolean rootSet = false;
    private boolean rootAdd = false;
    private boolean rootRemove = false;
    private boolean subSet = false;
    private boolean subAdd = false;
    private boolean subRemove = false;
    
    public void reset()
    {
        this.rootSet = false;
        this.rootAdd = false;
        this.rootRemove = false;
        this.subSet = false;
        this.subAdd = false;
        this.subRemove = false;
    }
    
    public boolean isRootSet()
    {
        return this.rootSet;
    }
    
    public void setRootSet(final boolean rootSet)
    {
        this.rootSet = rootSet;
    }
    
    public boolean isRootAdd()
    {
        return this.rootAdd;
    }
    
    public void setRootAdd(final boolean rootAdd)
    {
        this.rootAdd = rootAdd;
    }
    
    public boolean isRootRemove()
    {
        return this.rootRemove;
    }
    
    public void setRootRemove(final boolean rootRemove)
    {
        this.rootRemove = rootRemove;
    }
    
    public boolean isSubSet()
    {
        return this.subSet;
    }
    
    public void setSubSet(final boolean subSet)
    {
        this.subSet = subSet;
    }
    
    public boolean isSubAdd()
    {
        return this.subAdd;
    }
    
    public void setSubAdd(final boolean subAdd)
    {
        this.subAdd = subAdd;
    }
    
    public boolean isSubRemove()
    {
        return this.subRemove;
    }
    
    public void setSubRemove(final boolean subRemove)
    {
        this.subRemove = subRemove;
    }
}
