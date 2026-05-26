/*******************************************************************************
 * Copyright (c) 2017, 2021 Sebastian Palarus
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.common.message.MessageHeader;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreResult;
import org.sodeac.common.message.dispatcher.api.IPropertyBlock;
import org.sodeac.common.snapdeque.DequeNode;

public class MessageImpl<T> implements IMessage<T>
{
    private PublishMessageResultImpl scheduleResult = null;
    private ChannelImpl channel = null;
    private T payload = null;
    private MessageHeader messageHeader = null;
    
    private volatile PropertyBlockImpl propertyBlock = null;
    private volatile DequeNode<MessageImpl<T>> node = null;
    
    private UUID channelMessageId = null;
    private Long channelMessageTimestamp = null;
    private Long channelMessageSequence = null;
    private volatile Boolean consumed = null;
    private volatile Boolean processed = null;
    
    protected MessageImpl(final T payload, final ChannelImpl channel, final MessageHeader messageHeader)
    {
        super();
        this.payload = payload;
        this.channel = channel;
        this.messageHeader = messageHeader;
    }
    
    public DequeNode<MessageImpl<T>> getNode()
    {
        return this.node;
    }
    
    protected void setNode(final DequeNode<MessageImpl<T>> node)
    {
        if (node != null)
        {
            this.channelMessageId = node.getId();
            this.channelMessageSequence = node.getSequence();
            this.channelMessageTimestamp = node.getTimestamp();
        }
        this.node = node;
    }
    
    @Override
    public T getPayload()
    {
        return this.payload;
    }
    
    @Override
    public IOnMessageStoreResult getScheduleResultObject()
    {
        return this.scheduleResult;
    }
    
    protected void setScheduleResultObject(final PublishMessageResultImpl scheduleResult)
    {
        this.scheduleResult = scheduleResult;
    }
    
    @Override
    public UUID getId()
    {
        return this.channelMessageId;
    }
    
    @Override
    public Long getCreateTimestamp()
    {
        return this.channelMessageTimestamp;
    }
    
    @Override
    public Long getSequence()
    {
        return this.channelMessageSequence;
    }
    
    @Override
    public Object setProperty(final String key, final Object value)
    {
        if (this.propertyBlock == null)
        {
            ReentrantLock lock = this.channel.getMessageEventLock();
            lock.lock();
            try
            {
                if (this.propertyBlock == null)
                {
                    this.propertyBlock = (PropertyBlockImpl) this.channel.getDispatcher().createPropertyBlock();
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        return this.propertyBlock.setProperty(key, value);
    }
    
    @Override
    public Object getProperty(final String key)
    {
        if (this.propertyBlock == null)
        {
            return null;
        }
        
        return this.propertyBlock.getProperty(key);
    }
    
    @Override
    public MessageHeader getMessageHeader()
    {
        return this.messageHeader;
    }
    
    @Override
    public Set<String> getPropertyKeySet()
    {
        if (this.propertyBlock == null)
        {
            return PropertyBlockImpl.EMPTY_KEYSET;
        }
        return this.propertyBlock.getPropertyKeySet();
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        if (this.propertyBlock == null)
        {
            return PropertyBlockImpl.EMPTY_PROPERTIES;
        }
        return this.propertyBlock.getProperties();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <A> A getAdapter(final Class<A> adapterClass)
    {
        if (adapterClass == IPropertyBlock.class)
        {
            if (this.propertyBlock == null)
            {
                ReentrantLock lock = this.channel.getMessageEventLock();
                lock.lock();
                try
                {
                    if (this.propertyBlock == null)
                    {
                        this.propertyBlock = (PropertyBlockImpl) this.channel.getDispatcher().createPropertyBlock();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }
            return (A) this.propertyBlock;
        }
        return IMessage.super.getAdapter(adapterClass);
    }
    
    @Override
    public IDispatcherChannel<T> getChannel()
    {
        return this.channel;
    }
    
    @Override
    public void removeFromChannel()
    {
        if (this.channel != null)
        {
            this.channel.removeMessage(this);
        }
    }
    
    protected void dispose()
    {
        if (this.scheduleResult != null)
        {
            try
            {
                this.scheduleResult.dispose();
            }
            catch (final Exception e) { }
        }
        this.scheduleResult = null;
        this.channel = null;
        this.payload = null;
        if (this.messageHeader != null)
        {
            try
            {
                this.messageHeader.dispose();
            }
            catch (final Exception e) { }
            this.messageHeader = null;
        }
        if (this.propertyBlock != null)
        {
            try
            {
                this.propertyBlock.dispose();
            }
            catch (final Exception e) { }
        }
        this.propertyBlock = null;
        this.node = null;
        this.channelMessageId = null;
        this.channelMessageSequence = null;
        this.channelMessageTimestamp = null;
        this.processed = null;
        this.consumed = null;
    }
    
    @Override
    public boolean isRemoved()
    {
        DequeNode<MessageImpl<T>> n = this.node;
        if (n == null)
        {
            return false;
        }
        return !n.isLinked();
    }
    
    @Override
    public Boolean getConsumed()
    {
        return this.consumed;
    }
    
    @Override
    public void setConsumed(final Boolean consumed)
    {
        this.consumed = consumed;
    }
    
    @Override
    public Boolean getProcessed()
    {
        return this.processed;
    }
    
    @Override
    public void setProcessed(final Boolean processed)
    {
        this.processed = processed;
    }
    
}
