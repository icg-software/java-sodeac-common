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

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.sodeac.common.message.MessageHeader;
import org.sodeac.common.snapdeque.CapacityExceededException;
import org.sodeac.common.snapdeque.DequeSnapshot;

/**
 * API for channels. {@link IDispatcherChannel}s are configured by one or more {@link IDispatcherChannelManager}s.
 * All collected {@link IMessage}s can be processed by {@link IDispatcherChannelTask}s.
 *
 * @author Sebastian Palarus
 *
 */
public interface IDispatcherChannel<T>
{
    /**
     * getter for channel id
     *
     * @return id of channel
     */
    public String getId();
    
    /**
     * getter for scope name
     *
     * @return human readable name of this channel (or null if not defined)
     */
    public String getChannelName();
    
    /**
     * send a message with default header
     *
     * @param messagePayload payload of message to store in channel
     *
     * @throws CapacityExceededException is thrown if message channel exceeds max allowed message size
     */
    public default void sendMessage(T messagePayload) throws CapacityExceededException
    {
        this.sendMessage(messagePayload, null);
    }
    
    /**
     * send a message
     *
     * @param messagePayload payload of message to store in channel
     * @param messageHeader  message header properties
     *
     * @throws CapacityExceededException is thrown if message channel exceeds max allowed message size
     */
    public void sendMessage(T messagePayload, MessageHeader messageHeader) throws CapacityExceededException;
    
    /**
     * send messages
     *
     * @param messagePayloadCollection payload collection of messages to store in channel
     */
    public default void sendMessages(Collection<T> messagePayloadCollection)
    {
        sendMessages(messagePayloadCollection, null);
    }
    
    /**
     * send messages
     *
     * @param messagePayloadCollection payload collection of messages to store in channel
     * @param messageHeaderTemplate    template of message header properties
     */
    public void sendMessages(Collection<T> messagePayloadCollection, MessageHeader messageHeaderTemplate);
    
    /**
     * store a message with result
     *
     * @param messagePayload payload of message to store in channel
     *
     * @return Future of {@link IOnMessageStoreResult}
     *
     * @throws CapacityExceededException
     */
    public default Future<IOnMessageStoreResult> sendMessageWithResult(T messagePayload) throws CapacityExceededException
    {
        return this.sendMessageWithResult(messagePayload, null);
    }
    
    /**
     * store a message with result
     *
     * @param messagePayload payload of message to store in channel
     * @param messageHeader  message header properties
     *
     * @return Future of {@link IOnMessageStoreResult}
     *
     * @throws CapacityExceededException
     */
    public Future<IOnMessageStoreResult> sendMessageWithResult(T messagePayload, MessageHeader messageHeader) throws CapacityExceededException;
    
    /**
     * getter for configuration propertyblock of queue
     *
     * @return {@link IPropertyBlock} of queue for configuration details
     */
    public IPropertyBlock getConfigurationPropertyBlock();
    
    /**
     * getter for state propertyblock of queue
     *
     * @return {@link IPropertyBlock} of queue  for work state
     */
    public IPropertyBlock getStatePropertyBlock();
    
    /**
     * getter for global dispatchter service
     *
     * @return {@link IMessageDispatcher}
     */
    public IMessageDispatcher getDispatcher();
    
    /**
     * returns {@link IMessage} queued  with {@code uuid}
     *
     * @param uuid searchfilter
     *
     * @return IQueuedEvent queued with {@code uuid} or null if not present
     */
    public IMessage<T> getMessage(UUID uuid);
    
    /**
     * remove {@link IMessage} queued  with {@code uuid}
     *
     * @param uuid identifier for {@link IMessage} to remove
     *
     * @return true if {@link IMessage} was found and remove, otherwise false
     */
    public boolean removeMessage(UUID uuid);
    
    /**
     * return message snapshot
     *
     * @return snapshot for chain
     */
    public DequeSnapshot<IMessage<T>> getMessageSnapshot();
    
    /**
     * return message snapshot-poll (remove returned elements from channel)
     *
     * @return snapshot for chain
     */
    public DequeSnapshot<IMessage<T>> getMessageSnapshotPoll();
    
    /**
     * register an adapter for purpose of channel configuration
     *
     * @param adapterClass type of adapter
     * @param adapter      implementation of adapter
     *
     * @throws PropertyIsLockedException
     */
    public default <A> void setConfigurationAdapter(Class<A> adapterClass, A adapter) throws PropertyIsLockedException
    {
        getConfigurationPropertyBlock().setAdapter(adapterClass, adapter);
    }
    
    /**
     * get registered adapter for purpose of channel configuration
     *
     * @param adapterClass type of adapter
     *
     * @return registered adapter with specified adapterClass
     */
    public default <A> A getConfigurationAdapter(Class<A> adapterClass)
    {
        return getConfigurationPropertyBlock().getAdapter(adapterClass);
    }
    
    /**
     * get registered adapter for purpose of channel configuration
     *
     * @param adapterClass              type of adapter
     * @param adapterFactoryIfNotExists factory to create adapter if not exists , and store with specified key
     *
     * @return registered adapter with specified adapterClass
     */
    public default <A> A getConfigurationAdapter(Class<A> adapterClass, Supplier<A> adapterFactoryIfNotExists)
    {
        return getConfigurationPropertyBlock().getAdapter(adapterClass, adapterFactoryIfNotExists);
    }
    
    /**
     * remove registered adapter for purpose of channel configuration
     *
     * @param adapterClass type of adapter
     *
     * @throws PropertyIsLockedException
     */
    public default <A> void removeConfigruationAdapter(Class<A> adapterClass) throws PropertyIsLockedException
    {
        getConfigurationPropertyBlock().removeAdapter(adapterClass);
    }
    
    /**
     * register an adapter for purpose of controlling during runtime
     *
     * @param adapterClass type of adapter
     * @param adapter      implementation of adapter
     *
     * @throws PropertyIsLockedException
     */
    public default <A> void setStateAdapter(Class<A> adapterClass, A adapter) throws PropertyIsLockedException
    {
        getStatePropertyBlock().setAdapter(adapterClass, adapter);
    }
    
    /**
     * get registered adapter for purpose of controlling during runtime
     *
     * @param adapterClass type of adapter
     *
     * @return registered adapter with specified adapterClass
     */
    public default <A> A getStateAdapter(Class<A> adapterClass)
    {
        return getStatePropertyBlock().getAdapter(adapterClass);
    }
    
    /**
     * get registered adapter for purpose of controlling during runtime
     *
     * @param adapterClass              type of adapter
     * @param adapterFactoryIfNotExists factory to create adapter if not exists , and store with specified key
     *
     * @return registered adapter with specified adapterClass
     */
    public default <A> A getStateAdapter(Class<A> adapterClass, Supplier<A> adapterFactoryIfNotExists)
    {
        return getStatePropertyBlock().getAdapter(adapterClass, adapterFactoryIfNotExists);
    }
    
    /**
     * remove registered adapter for purpose of controlling during runtime
     *
     * @param adapterClass type of adapter
     *
     * @throws PropertyIsLockedException
     */
    public default <A> void removeStateAdapter(Class<A> adapterClass) throws PropertyIsLockedException
    {
        getStatePropertyBlock().removeAdapter(adapterClass);
    }
    
    /**
     * remove list of {@link IMessage}s queued  with one of {@code uuid}s
     *
     * @param uuidList list of identifiers for {@link IMessage} to remove
     *
     * @return true if one of {@link IMessage} was found and remove, otherwise false
     */
    public boolean removeMessageList(List<UUID> uuidList);
    
    // TODO getTaskList/Index by Predicate
    
    /**
     * schedule a anonymous {@link IDispatcherChannelTask} to {@link IDispatcherChannel}
     * <p>
     * equivalent to scheduleTask(null,task, null, -1, -1, -1);
     *
     * @param task {@link IDispatcherChannelTask} to schedule
     *
     * @return generated taskid
     */
    public String scheduleTask(IDispatcherChannelTask task);
    
    /**
     * schedule a {@link IDispatcherChannelTask} to {@link IDispatcherChannel}.
     *
     * @param id   registration-id for {@link IDispatcherChannelTask} to schedule
     * @param task {@link IDispatcherChannelTask} to schedule
     *
     * @return taskid (generated, if parameter id is null)
     */
    public String scheduleTask(String id, IDispatcherChannelTask task);
    
    /**
     * schedule a {@link IDispatcherChannelTask} to {@link IDispatcherChannel}.
     *
     * @param id                 registration-id for {@link IDispatcherChannelTask} to schedule
     * @param task               {@link IDispatcherChannelTask} to schedule
     * @param propertyBlock      {@link IDispatcherChannelTask}-properties (factory in {@link IMessageDispatcher})
     * @param executionTimeStamp execution time millis
     * @param timeOutValue       timeout value in ms, before notify for timeout
     * @param heartBeatTimeOut   heartbeat-timeout value in ms, before notify for timeout
     *
     * @return taskid (generated, in parameter id is null)
     */
    public String scheduleTask(String id, IDispatcherChannelTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut);
    
    /**
     * schedule a {@link IDispatcherChannelTask} to {@link IDispatcherChannel}.
     *
     * @param id                 registration-id for {@link IDispatcherChannelTask} to schedule
     * @param task               {@link IDispatcherChannelTask} to schedule
     * @param propertyBlock      {@link IDispatcherChannelTask}-properties (factory in {@link IMessageDispatcher})
     * @param executionTimeStamp execution time millis
     * @param timeOutValue       timeout value in ms, before notify for timeout
     * @param heartBeatTimeOut   heartbeat-timeout value in ms, before notify for timeout
     * @param stopOnTimeOut      stop unlinked worker-thread on timeout. This option is NOT necessary to create new worker running other tasks. Attention: can be dangerous
     *
     * @return taskid (generated, in parameter id is null)
     */
    public String scheduleTask(String id, IDispatcherChannelTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut, boolean stopOnTimeOut);
    
    /**
     * reset execution plan for an existing {@link IDispatcherChannelTask}
     *
     * <p>The execution timestamp is ignored if current execution plan requires earlier execution in the future
     * and was requested by trigger, tasks control and periodic service configuration
     *
     * @param id                 registration-id of {@link IDispatcherChannelTask} in which reset execution plan
     * @param executionTimeStamp new execution time millis
     * @param timeOutValue       new timeout value in ms, before notify for timeout
     * @param heartBeatTimeOut   heartbeat-timeout value in ms, before notify for timeout
     *
     * @return affected {@link IDispatcherChannelTask} or null if not found
     */
    public IDispatcherChannelTask rescheduleTask(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut);
    
    /**
     * returns {@link IDispatcherChannelTask} scheduled under registration {@code id}
     *
     * @param id registration-id for {@link IDispatcherChannelTask}
     *
     * @return {@link IDispatcherChannelTask} scheduled under registration {@code id}
     */
    public IDispatcherChannelTask getTask(String id);
    
    /**
     * remove{@link IDispatcherChannelTask} scheduled under registration {@code id}
     *
     * @param id registration-id for {@link IDispatcherChannelTask} to remove
     *
     * @return removed {@link IDispatcherChannelTask} or null if no scheduled with {@code id} found
     */
    public IDispatcherChannelTask removeTask(String id);
    
    /**
     * returns properties of {@link IDispatcherChannelTask} scheduled under registration {@code id}
     *
     * @param id registration-id for {@link IDispatcherChannelTask}
     *
     * @return properties of {@link IDispatcherChannelTask} scheduled under registration {@code id}
     */
    public IPropertyBlock getTaskPropertyBlock(String id);
    
    /**
     * Sends a signal. All {@link IDispatcherChannelManager} manage this {@link IDispatcherChannel} and implements {@link IOnChannelSignal} will notify asynchronously by queueworker.
     *
     * @param signal
     */
    public void signal(String signal);
    
    /**
     * returns root scope
     *
     * @return global scope
     */
    public IDispatcherChannel<Object> getRootChannel();
    
    /**
     * returns root scope
     *
     * @param rootType
     *
     * @return global scope
     */
    @SuppressWarnings("unchecked")
    public default <R> IDispatcherChannel<R> getRootChannel(Class<R> rootType)
    {
        return (IDispatcherChannel<R>) getRootChannel();
    }
    
    /**
     * getter for parent scope, if exists.
     *
     * @return parentChannel or null
     */
    public IDispatcherChannel<Object> getParentChannel();
    
    /**
     * getter for parent scope, if exists.
     *
     * @param parentType
     *
     * @return parentChannel or null
     */
    @SuppressWarnings("unchecked")
    public default <P> IDispatcherChannel<P> getParentChannel(Class<P> parentType)
    {
        return (IDispatcherChannel<P>) getParentChannel();
    }
    
    /**
     * create {@link ISubChannel} for {@link IDispatcherChannel}
     *
     * @param scopeId                 unique id of scope (unique by queue) or null for auto-generation
     * @param scopeName               human readable name of scope (nullable)
     * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
     * @param stateProperties         blue print for state propertyblock of new scope (nullable)
     *
     * @return new scope, or null, if scope already exists
     */
    public default ISubChannel<?> createChildScope(UUID scopeId, String scopeName, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
    {
        return this.createChildScope(scopeId, scopeName, configurationProperties, stateProperties, false, false);
    }
    
    /**
     * create {@link ISubChannel} for {@link IDispatcherChannel}
     *
     * @param scopeId                 unique id of scope (unique by queue) or null for auto-generation
     * @param scopeName               human readable name of scope (nullable)
     * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
     * @param stateProperties         blue print for state propertyblock of new scope (nullable)
     * @param adoptContoller          keep controller of parent queue
     * @param adoptServices           keep services of parent queue
     *
     * @return new scope, or null, if scope already exists
     */
    public ISubChannel createChildScope(UUID scopeId, String scopeName, Map<String, Object> configurationProperties, Map<String, Object> stateProperties, boolean adoptContoller, boolean adoptServices);
    
    /**
     * getter for child scope list. The child scopes list is defined by virtual tree structure.
     *
     * @return immutable list of child scopes
     */
    public List<ISubChannel> getChildScopes();
    
    /**
     * returns scopelist of queue with positiv match result for {@code filter}
     *
     * @param filter match condition for configuration propertyblock
     *
     * @return scopelist of queue with positiv match result for {@code filter}
     */
    // public List<IQueueChildScope> getChildScopes(Filter filter);
    
    // TODO getChildScope by Predicate
    
    /**
     * returns scope with given {@code scopeId}
     *
     * @param scopeId id of scope to return
     *
     * @return scope with given {@code scopeId} or null, if scope not found
     */
    public ISubChannel getChildScope(UUID scopeId);
    
    public interface IDispatcherChannelReference extends Closeable
    {
        public <T> IDispatcherChannel<T> getChannel(Class<T> type);
    }
    
}
