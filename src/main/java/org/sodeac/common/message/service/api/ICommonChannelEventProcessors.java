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

import java.util.function.Consumer;

import org.sodeac.common.message.service.api.IServiceChannel.IChannelEvent;
import org.sodeac.common.message.service.api.IServiceChannel.IChannelEventProcessor;

public interface ICommonChannelEventProcessors
{
    interface IChannelErrorProcessor extends IChannelEventProcessor
    {
        IChannelEventProcessor onChannelEvent(Consumer<IChannelError> consumer);
    }
    
    interface IChannelError extends IChannelEvent
    {
        enum ErrorType
        {ON_TRANSPORT, ON_SUPPLY, ON_CONSUME, ON_TIMEOUT}
        
        Throwable getThrowable();
        
        ErrorType getType();
        
        IServiceChannel<?> getChannel();
    }
    
    interface IChannelCloseProcessor extends IChannelEventProcessor
    {
        <T> IChannelEventProcessor onChannelEvent(Consumer<IChannelClose> consumer);
    }
    
    interface IChannelClose extends IChannelEvent
    {
        enum Actor
        {SUPPLIER, CONSUMNER}
        
        int getCountSupplier();
        
        int getCountConsumer();
        
        IServiceChannel<?> getChannel();
    }
}
