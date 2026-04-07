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

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IServiceConnection
{
    Set<IServiceChannel.IChannelDescription> getChannelCatalog();
    
    <T> IMessageProducerEndpoint<T> openMessageProducerEndpoint(Class<T> messageClass);
    
    <T> IMessageConsumerEndpoint<T> openMessageConsumerEndpoint(Class<T> messageClass);
    
    IServiceConnection connect();
    
    IServiceConnection disconnect();
    
    boolean isConnected();
    
    IServiceConnection close();
    
    boolean isClosed();
    
    <A> A getAdapter(Class<A> adapterClass);
    
    interface IMessageConsumerEndpoint<T> extends IServiceChannel<T>
    {
        IMessageConsumerEndpoint<T> onMessageReceived(Consumer<IMessageReceive<T>> messageConsumer);
        
        IMessageConsumerEndpoint<T> setupEndpoint(Consumer<IMessageConsumerEndpoint<T>> setup);
    }
    
    interface IMessageProducerEndpoint<T> extends IServiceChannel<T>
    {
        IMessageProducerEndpoint<T> onMessageRequested(BiConsumer<IMessageRequest<T>, Consumer<T>> messageProducer);
        
        IMessageProducerEndpoint<T> setupEndpoint(Consumer<IMessageProducerEndpoint<T>> setup);
    }
}
