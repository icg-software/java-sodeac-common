/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemManager;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemService;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcher;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcherManager;
import org.sodeac.common.misc.Driver;

public class MessageDispatcherManagerImpl implements IMessageDispatcherManager
{
    private static MessageDispatcherManagerImpl INSTANCE;
    
    private MessageDispatcherManagerImpl()
    {
        super();
        this.lock = new ReentrantLock();
        this.registeredDispatcher = new HashMap<String, MessageDispatcherImpl>();
    }
    
    private ReentrantLock lock = null;
    private Map<String, MessageDispatcherImpl> registeredDispatcher = null;
    
    public static IMessageDispatcherManager get()
    {
        MessageDispatcherManagerImpl factory = INSTANCE;
        if (factory != null)
        {
            return factory;
        }
        
        synchronized (MessageDispatcherManagerImpl.class)
        {
            factory = INSTANCE;
            if (factory == null)
            {
                INSTANCE = new MessageDispatcherManagerImpl();
                factory = INSTANCE;
            }
        }
        
        return factory;
    }
    
    protected IMessageDispatcher newUnmanagedMessageDispatcher()
    {
        MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl("anonym-" + UUID.randomUUID());
        for (final IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
        {
            messageDispatcher.registerChannelManager(channelManager);
        }
        for (final IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
        {
            messageDispatcher.registerChannelService(channelService);
        }
        return messageDispatcher;
    }
    
    @Override
    public IMessageDispatcher createDispatcher(final String id)
    {
        this.lock.lock();
        try
        {
            if (this.registeredDispatcher.containsKey(id))
            {
                return null;
            }
            MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl(id);
            for (final IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
            {
                messageDispatcher.registerChannelManager(channelManager);
            }
            for (final IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
            {
                messageDispatcher.registerChannelService(channelService);
            }
            this.registeredDispatcher.put(id, messageDispatcher);
            return messageDispatcher;
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    @Override
    public IMessageDispatcher getOrCreateDispatcher(final String id)
    {
        this.lock.lock();
        try
        {
            if (this.registeredDispatcher.containsKey(id))
            {
                return this.registeredDispatcher.get(id);
            }
            MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl(id);
            for (final IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
            {
                messageDispatcher.registerChannelManager(channelManager);
            }
            for (final IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
            {
                messageDispatcher.registerChannelService(channelService);
            }
            this.registeredDispatcher.put(id, messageDispatcher);
            return messageDispatcher;
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    @Override
    public IMessageDispatcher getDispatcher(final String id)
    {
        this.lock.lock();
        try
        {
            return this.registeredDispatcher.get(id);
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected void remove(final String id)
    {
        this.lock.lock();
        try
        {
            this.registeredDispatcher.remove(id);
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected void shutdownAllDispatcher()
    {
        List<MessageDispatcherImpl> toRemove = null;
        this.lock.lock();
        try
        {
            toRemove = new ArrayList<MessageDispatcherImpl>(this.registeredDispatcher.values());
        }
        finally
        {
            this.lock.unlock();
        }
        
        for (final MessageDispatcherImpl dispatcher : toRemove)
        {
            dispatcher.shutdown();
        }
    }
    
    protected void registerSystemChannelManager(final IDispatcherChannelSystemManager channelManager)
    {
        this.lock.lock();
        try
        {
            for (final MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.registerChannelManager(channelManager);
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected void unregisterSystemChannelManager(final IDispatcherChannelSystemManager channelManager)
    {
        this.lock.lock();
        try
        {
            for (final MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.unregisterChannelManager(channelManager);
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected void registerSystemChannelService(final IDispatcherChannelSystemService<?> channelService)
    {
        this.lock.lock();
        try
        {
            for (final MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.registerChannelService(channelService);
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected void unregisterSystemChannelService(final IDispatcherChannelSystemService<?> channelService)
    {
        this.lock.lock();
        try
        {
            for (final MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.unregisterChannelService(channelService);
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
}
