/*******************************************************************************
 * Copyright (c) 2017, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.api;

import java.util.List;

import org.sodeac.common.snapdeque.CapacityExceededException;

/**
 *
 * API for message dispatcher
 *
 * @author Sebastian Palarus
 *
 */
public interface IMessageDispatcher
{
    /**
     * queue a message to addressed queue
     *
     * @param channelId id of {@link IDispatcherChannel}
     * @param message   message to queue
     *
     * @throws ChannelNotFoundException
     * @throws CapacityExceededException
     */
    <T> void sendMessage(String channelId, T message) throws ChannelNotFoundException, CapacityExceededException;
    
    /**
     * factory-methode creating instance of {@link IPropertyBlock}
     *
     * @return instance of {@link IPropertyBlock}
     */
    IPropertyBlock createPropertyBlock();
    
    /**
     * request for all {@link IDispatcherChannel}-IDs
     *
     * @return {@link java.util.List} with queueIds
     */
    List<String> getChannelIdList();
    
    /**
     * getter to request for {@link IDispatcherChannel} with given id
     *
     * @param channelId id for queue
     *
     * @return instance of {@link IDispatcherChannel} registered with {@code queueId}
     */
    IDispatcherChannel<?> getChannel(String channelId);
    
    <T> IDispatcherChannel<T> getTypedChannel(String channelId, Class<T> messageType);
    
    /**
     * getter for propertyblock of dispatcher
     *
     * @return {@link IPropertyBlock} of dispatcher
     */
    IPropertyBlock getPropertyBlock();
    
    /**
     * getter for id of dispatcher.
     *
     * @return id of dispatcher
     */
    String getId();
    
    /**
     * Remove all workers and clean resources. After shutdown the dispatcher is not usable anymore.
     */
    void shutdown();
    
    void registerChannelManager(IDispatcherChannelManager channelManager);
    
    void registerChannelService(IDispatcherChannelService channelService);
    
    void unregisterChannelManager(IDispatcherChannelManager channelManager);
    
    void unregisterChannelService(IDispatcherChannelService channelService);
}
