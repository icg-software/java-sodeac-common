/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.service.api;

import java.util.Optional;
import java.util.UUID;

public interface IServiceChannel<T>
{
    enum ChannelType
    {
        /**
         * Stream to consume. If Consumed, the Message is removed
         */
        STREAM,
        
        /**
         * A Collection of tags. A tag (identified by equals) can only be one time in collection.
         * If a tag is supplied several times, the latest published tag replaces the previous item equals new tag.
         * If Consumed, the Message is removed
         */
        TAG,
        
        /**
         * A Single Object .
         * If Consumed, the Message is removed.
         */
        STATE
    }
    
    IServiceChannel<T> close(String reason);
    
    boolean isClosed();
    
    IChannelDescription getChannelDescription();
    
    <P extends IChannelPolicy> Optional<P> getChannelPolicy(Class<P> type);
    
    <P extends IChannelEventProcessor> Optional<P> getChannelEventProcessor(Class<P> type);
    
    IServiceConnection getConnection();
    
    interface IChannelDescription
    {
        ChannelType getChannelType();
    }
    
    interface IMessageRequest<T>
    {
        IServiceChannel<T> getChannel();
    }
    
    interface IMessageReceive<T>
    {
        T value();
        
        UUID getId();
        
        long getChannelSequence();
        
        long getConversationSequence();
        
        IServiceChannel<T> getChannel();
    }
    
    interface IChannelPolicy
    {
    }
    
    interface IChannelEvent
    {
    }
    
    interface IChannelEventProcessor
    {
    }
}
