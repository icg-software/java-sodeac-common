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

import java.util.Map;
import java.util.UUID;

import org.sodeac.common.message.dispatcher.api.ISubChannel;

public class SubChannelImpl extends ChannelImpl implements ISubChannel
{
    private UUID scopeId;
    private boolean adoptContoller = false;
    private boolean adoptServices = false;
    
    protected SubChannelImpl(UUID scopeId, ChannelImpl rootChannel, ChannelImpl parentChannel, String scopeName, boolean adoptContoller, boolean adoptServices, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
    {
        super(parentChannel.getId() + "." + scopeId.toString(), (MessageDispatcherImpl) parentChannel.getDispatcher(), rootChannel, parentChannel, scopeName, configurationProperties, stateProperties);
        
        this.adoptContoller = adoptContoller;
        this.adoptServices = adoptServices;
        this.scopeId = scopeId;
    }
    
    @Override
    public UUID getScopeId()
    {
        return scopeId;
    }
    
    public boolean isAdoptContoller()
    {
        return adoptContoller;
    }
    
    public boolean isAdoptServices()
    {
        return adoptServices;
    }
    
}
