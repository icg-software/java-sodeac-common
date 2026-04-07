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
        MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl("anonym-" + UUID.randomUUID().toString());
        for (IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
        {
            messageDispatcher.registerChannelManager(channelManager);
        }
        for (IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
        {
            messageDispatcher.registerChannelService(channelService);
        }
        return messageDispatcher;
    }
    
    @Override
    public IMessageDispatcher createDispatcher(String id)
    {
        lock.lock();
        try
        {
            if (this.registeredDispatcher.containsKey(id))
            {
                return null;
            }
            MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl(id);
            for (IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
            {
                messageDispatcher.registerChannelManager(channelManager);
            }
            for (IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
            {
                messageDispatcher.registerChannelService(channelService);
            }
            this.registeredDispatcher.put(id, messageDispatcher);
            return messageDispatcher;
        }
        finally
        {
            lock.unlock();
        }
    }
    
    @Override
    public IMessageDispatcher getOrCreateDispatcher(String id)
    {
        lock.lock();
        try
        {
            if (this.registeredDispatcher.containsKey(id))
            {
                return this.registeredDispatcher.get(id);
            }
            MessageDispatcherImpl messageDispatcher = new MessageDispatcherImpl(id);
            for (IDispatcherChannelSystemManager channelManager : Driver.getDriverList(IDispatcherChannelSystemManager.class, null))
            {
                messageDispatcher.registerChannelManager(channelManager);
            }
            for (IDispatcherChannelSystemService<?> channelService : Driver.getDriverList(IDispatcherChannelSystemService.class, null))
            {
                messageDispatcher.registerChannelService(channelService);
            }
            this.registeredDispatcher.put(id, messageDispatcher);
            return messageDispatcher;
        }
        finally
        {
            lock.unlock();
        }
    }
    
    public IMessageDispatcher getDispatcher(String id)
    {
        lock.lock();
        try
        {
            return this.registeredDispatcher.get(id);
        }
        finally
        {
            lock.unlock();
        }
    }
    
    protected void remove(String id)
    {
        lock.lock();
        try
        {
            this.registeredDispatcher.remove(id);
        }
        finally
        {
            lock.unlock();
        }
    }
    
    protected void shutdownAllDispatcher()
    {
        List<MessageDispatcherImpl> toRemove = null;
        lock.lock();
        try
        {
            toRemove = new ArrayList<MessageDispatcherImpl>(this.registeredDispatcher.values());
        }
        finally
        {
            lock.unlock();
        }
        
        for (MessageDispatcherImpl dispatcher : toRemove)
        {
            dispatcher.shutdown();
        }
    }
    
    protected void registerSystemChannelManager(IDispatcherChannelSystemManager channelManager)
    {
        lock.lock();
        try
        {
            for (MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.registerChannelManager(channelManager);
            }
        }
        finally
        {
            lock.unlock();
        }
    }
    
    protected void unregisterSystemChannelManager(IDispatcherChannelSystemManager channelManager)
    {
        lock.lock();
        try
        {
            for (MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.unregisterChannelManager(channelManager);
            }
        }
        finally
        {
            lock.unlock();
        }
    }
    
    protected void registerSystemChannelService(IDispatcherChannelSystemService<?> channelService)
    {
        lock.lock();
        try
        {
            for (MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.registerChannelService(channelService);
            }
        }
        finally
        {
            lock.unlock();
        }
    }
    
    protected void unregisterSystemChannelService(IDispatcherChannelSystemService<?> channelService)
    {
        lock.lock();
        try
        {
            for (MessageDispatcherImpl messageDispatcherImpl : this.registeredDispatcher.values())
            {
                messageDispatcherImpl.unregisterChannelService(channelService);
            }
        }
        finally
        {
            lock.unlock();
        }
    }
}
