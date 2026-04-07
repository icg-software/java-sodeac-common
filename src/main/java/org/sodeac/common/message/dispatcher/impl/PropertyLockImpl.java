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

import java.util.UUID;

import org.sodeac.common.message.dispatcher.api.IPropertyLock;

public class PropertyLockImpl implements IPropertyLock
{
    private final PropertyBlockImpl block;
    private final String key;
    private final UUID pin;
    
    protected PropertyLockImpl(final PropertyBlockImpl block, final String key, final UUID pin)
    {
        super();
        this.block = block;
        this.key = key;
        this.pin = pin;
    }
    
    @Override
    public boolean unlock()
    {
        return this.block.unlockProperty(this);
    }
    
    protected PropertyBlockImpl getBlock()
    {
        return this.block;
    }
    
    protected String getKey()
    {
        return this.key;
    }
    
    protected UUID getPin()
    {
        return this.pin;
    }
    
}
