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
package org.sodeac.common.message.dispatcher.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.sodeac.common.message.MessageHeader;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelService;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcher;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelDetach;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreResult;
import org.sodeac.common.message.dispatcher.api.IPropertyBlock;
import org.sodeac.common.message.dispatcher.api.ISubChannel;
import org.sodeac.common.message.dispatcher.api.ITaskControl.ExecutionTimestampSource;
import org.sodeac.common.message.dispatcher.impl.ChannelManagerContainer.ControllerFilterObjects;
import org.sodeac.common.message.dispatcher.impl.ServiceContainer.ServiceFilterObjects;
import org.sodeac.common.message.dispatcher.impl.TaskControlImpl.RescheduleTimestampPredicate;
import org.sodeac.common.message.dispatcher.impl.TaskControlImpl.ScheduleTimestampPredicate;
import org.sodeac.common.snapdeque.CapacityExceededException;
import org.sodeac.common.snapdeque.DequeNode;
import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.snapdeque.SnapshotableDeque;

public class ChannelImpl<T> implements IDispatcherChannel<T>
{
    protected ChannelImpl(final String channelId, final MessageDispatcherImpl messageDispatcher, final ChannelImpl rootChannel, final ChannelImpl parentChannel, final String name, final Map<String, Object> configurationProperties, final Map<String, Object> stateProperties)
    {
        super();
        
        this.rootChannel = rootChannel;
        this.parentChannel = parentChannel;
        
        if (parentChannel == null)
        {
            if (rootChannel != null)
            {
                throw new IllegalStateException("(parentChannel == null) && (rootChannel != null)");
            }
            this.rootChannel = this;
        }
        
        this.name = name;
        
        this.channelId = channelId;
        this.messageDispatcher = messageDispatcher;
        
        this.genericChannelSpoolLock = new ReentrantLock();
        this.workerSpoolLock = new ReentrantLock();
        
        this.channelManagerList = new ArrayList<ChannelManagerContainer>();
        this.channelManagerIndex = new HashMap<ChannelManagerContainer, ChannelManagerContainer>();
        this.channelManagerListLock = new ReentrantReadWriteLock(true);
        this.channelManagerListReadLock = this.channelManagerListLock.readLock();
        this.channelManagerListWriteLock = this.channelManagerListLock.writeLock();
        
        this.channelServiceList = new ArrayList<ServiceContainer>();
        this.channelServiceIndex = new HashMap<ServiceContainer, ServiceContainer>();
        this.channelServiceListLock = new ReentrantReadWriteLock(true);
        this.channelServiceListReadLock = this.channelServiceListLock.readLock();
        this.channelServiceListWriteLock = this.channelServiceListLock.writeLock();
        
        this.messageQueue = new SnapshotableDeque<>(Integer.MAX_VALUE, true);
        this.newPublishedMessageQueue = new SnapshotableDeque<>();
        this.removedMessageQueue = new SnapshotableDeque<>();
        
        this.taskList = new ArrayList<TaskContainer>();
        this.taskIndex = new HashMap<String, TaskContainer>();
        this.taskListLock = new ReentrantReadWriteLock(true);
        this.taskListReadLock = this.taskListLock.readLock();
        this.taskListWriteLock = this.taskListLock.writeLock();
        
        this.channelSignalList = new SnapshotableDeque<String>();
        this.onChannelAttachList = new SnapshotableDeque<>();
        
        this.lastWorkerAction = System.currentTimeMillis();
        
        this.subChannelList = new ArrayList<SubChannelImpl>();
        this.subChannelIndex = new HashMap<UUID, SubChannelImpl>();
        this.subChannelListCopy = Collections.unmodifiableList(new ArrayList<ISubChannel<?>>());
        this.subChannelListLock = new ReentrantReadWriteLock(true);
        this.subChannelListReadLock = this.subChannelListLock.readLock();
        this.queueScopeListWriteLock = this.subChannelListLock.writeLock();
        
        this.consumeMessageHandlerListLock = new ReentrantReadWriteLock();
        this.consumeMessageHandlerListWriteLock = this.consumeMessageHandlerListLock.writeLock();
        this.consumeMessageHandlerListReadLock = this.consumeMessageHandlerListLock.readLock();
        
        this.dummyPublishMessageResult = new DummyPublishMessageResult();
        
        this.configurationPropertyBlock = messageDispatcher.createPropertyBlock();
        if (configurationProperties != null)
        {
            this.configurationPropertyBlock.setPropertyEntrySet(configurationProperties.entrySet(), false);
        }
        
        this.statePropertyBlock = messageDispatcher.createPropertyBlock();
        if (stateProperties != null)
        {
            this.statePropertyBlock.setPropertyEntrySet(stateProperties.entrySet(), false);
        }
        
        this.channelConfigurationModifyListener = new ChannelConfigurationModifyListener(this);
        this.configurationPropertyBlock.addModifyListener(this.channelConfigurationModifyListener);
        
        this.snapshotsByWorkerThread = new LinkedList<DequeSnapshot<IMessage<T>>>();
        this.sharedMessageLock = new ReentrantLock(true);
        
        this.registrationTypes = new RegistrationTypes();
    }
    
    // TODO values in TaskContainer ???
    protected static final String PROPERTY_KEY_TASK_ID = "TASK_ID";
    protected static final String PROPERTY_PERIODIC_REPETITION_INTERVAL = "PERIODIC_REPETITION_INTERVAL";
    protected static final String PROPERTY_KEY_THROWED_EXCEPTION = "THROWED_EXCEPTION";
    
    protected ChannelImpl rootChannel = null;
    protected ChannelImpl parentChannel = null;
    protected String name = null;
    protected volatile int capacity = Integer.MAX_VALUE;
    
    protected MessageDispatcherImpl messageDispatcher = null;
    protected String channelId = null;
    
    protected List<ChannelManagerContainer> channelManagerList;
    protected volatile List<ChannelManagerContainer> controllerListCopy = null;
    protected Map<ChannelManagerContainer, ChannelManagerContainer> channelManagerIndex = null;
    protected ReentrantReadWriteLock channelManagerListLock;
    protected ReadLock channelManagerListReadLock;
    protected WriteLock channelManagerListWriteLock;
    
    protected List<ServiceContainer> channelServiceList;
    protected Map<ServiceContainer, ServiceContainer> channelServiceIndex = null;
    protected volatile List<ServiceContainer> serviceListCopy = null;
    protected ReentrantReadWriteLock channelServiceListLock;
    protected ReadLock channelServiceListReadLock;
    protected WriteLock channelServiceListWriteLock;
    
    protected SnapshotableDeque<MessageImpl> messageQueue = null;
    protected SnapshotableDeque<MessageImpl> newPublishedMessageQueue = null;
    protected SnapshotableDeque<MessageImpl> removedMessageQueue = null;
    
    protected List<TaskContainer> taskList = null;
    protected Map<String, TaskContainer> taskIndex = null;
    protected ReentrantReadWriteLock taskListLock;
    protected ReadLock taskListReadLock;
    protected WriteLock taskListWriteLock;
    
    protected volatile boolean signalListUpdate = false;
    protected SnapshotableDeque<String> channelSignalList = null;
    
    protected volatile boolean onQueueAttachListUpdate = false;
    protected SnapshotableDeque<IOnChannelAttach> onChannelAttachList = null;
    
    protected volatile ChannelWorker channelWorker = null;
    protected volatile SpooledChannelWorker currentSpooledChannelWorker = null;
    protected volatile long lastWorkerAction;
    protected PropertyBlockImpl configurationPropertyBlock = null;
    protected PropertyBlockImpl statePropertyBlock = null;
    
    protected volatile boolean newScheduledListUpdate = false;
    protected volatile boolean removedEventListUpdate = false;
    protected volatile boolean firedEventListUpdate = false;
    
    protected ReentrantLock genericChannelSpoolLock = null;
    protected ReentrantLock workerSpoolLock = null;
    
    protected volatile boolean disposed = false;
    protected volatile boolean privateWorker = false;
    
    protected volatile ChannelConfigurationModifyListener channelConfigurationModifyListener = null;
    
    protected List<SubChannelImpl> subChannelList;
    protected Map<UUID, SubChannelImpl> subChannelIndex;
    protected volatile List<ISubChannel> subChannelListCopy = null;
    protected ReentrantReadWriteLock subChannelListLock;
    protected ReadLock subChannelListReadLock;
    protected WriteLock queueScopeListWriteLock;
    
    private final ReentrantReadWriteLock consumeMessageHandlerListLock;
    private final ReadLock consumeMessageHandlerListReadLock;
    private final WriteLock consumeMessageHandlerListWriteLock;
    
    protected DummyPublishMessageResult dummyPublishMessageResult = null;
    
    protected LinkedList<DequeSnapshot<IMessage<T>>> snapshotsByWorkerThread;
    
    protected ReentrantLock sharedMessageLock = null;
    
    protected volatile RegistrationTypes registrationTypes = null;
    
    @Override
    public void sendMessage(final T messagePayload, MessageHeader messageHeader)
    {
        if (this.disposed)
        {
            return;
        }
        
        if (messageHeader == null)
        {
            messageHeader = MessageHeader.newInstance()
                                         .setTimestamp(System.currentTimeMillis())
                                         .lockHeader(MessageHeader.MESSAGE_HEADER_TIMESTAMP);
        }
        
        MessageImpl message = new MessageImpl(messagePayload, this, messageHeader);
        try
        {
            DequeNode<MessageImpl> node = this.messageQueue.link(SnapshotableDeque.LinkMode.APPEND, message, n ->
                                                                 {
                                                                     message.setNode(n);
                                                                     message.setScheduleResultObject(this.dummyPublishMessageResult);
                                                                 }
            );
        }
        catch (final CapacityExceededException e)
        {
            try
            {
                message.dispose();
            }
            catch (final Exception ex) { }
            throw e;
        }
        
        if (this.registrationTypes.onQueuedMessage)
        {
            this.newPublishedMessageQueue.addLast(message);
            this.newScheduledListUpdate = true;
            this.notifyOrCreateWorker(-1);
        }
    }
    
    @Override
    public void sendMessages(final Collection<T> messagePayloadCollection, final MessageHeader messageHeaderTemplate)
    {
        if (this.disposed)
        {
            return;
        }
        
        List<MessageImpl<T>> messageList = new ArrayList<>(messagePayloadCollection.size());
        for (final T messagePayload : messagePayloadCollection)
        {
            MessageHeader messageHeader = null;
            if (messageHeaderTemplate == null)
            {
                messageHeader = MessageHeader.newInstance()
                                             .setTimestamp(System.currentTimeMillis())
                                             .lockHeader(MessageHeader.MESSAGE_HEADER_TIMESTAMP);
            }
            else
            {
                messageHeader = MessageHeader.createFrom(messageHeaderTemplate, false).setTimestamp(System.currentTimeMillis());
                MessageHeader.copyLocks(messageHeader, messageHeaderTemplate);
            }
            
            messageList.add(new MessageImpl(messagePayload, this, messageHeader));
        }
        try
        {
            this.messageQueue.linkAll(SnapshotableDeque.LinkMode.APPEND, messageList, n ->
                                      {
                                          MessageImpl message = n.getElement();
                                          message.setNode(n);
                                          message.setScheduleResultObject(this.dummyPublishMessageResult);
                                      }
            );
        }
        catch (final CapacityExceededException e)
        {
            try
            {
                for (final MessageImpl<T> message : messageList)
                {
                    message.dispose();
                }
            }
            catch (final Exception ex) { }
            throw e;
        }
        
        if (this.registrationTypes.onQueuedMessage)
        {
            this.newPublishedMessageQueue.addAll(messageList);
            this.newScheduledListUpdate = true;
            this.notifyOrCreateWorker(-1);
        }
    }
    
    @Override
    public Future<IOnMessageStoreResult> sendMessageWithResult(final T messagePayload, MessageHeader messageHeader)
    {
        if (this.disposed)
        {
            return this.messageDispatcher.createFutureOfScheduleResult(new PublishMessageResultImpl());
        }
        
        if (messageHeader == null)
        {
            messageHeader = MessageHeader.newInstance()
                                         .setTimestamp(System.currentTimeMillis())
                                         .lockHeader(MessageHeader.MESSAGE_HEADER_TIMESTAMP);
        }
        
        PublishMessageResultImpl resultImpl = new PublishMessageResultImpl();
        
        MessageImpl message = new MessageImpl(messagePayload, this, messageHeader);
        try
        {
            DequeNode<MessageImpl> node = this.messageQueue.link(SnapshotableDeque.LinkMode.APPEND, message, n ->
                                                                 {
                                                                     message.setScheduleResultObject(resultImpl);
                                                                     message.setNode(n);
                                                                 }
            );
            
        }
        catch (final CapacityExceededException e)
        {
            try
            {
                message.dispose();
            }
            catch (final Exception ex) { }
            throw e;
        }
        
        if (this.registrationTypes.onQueuedMessage)
        {
            this.newPublishedMessageQueue.addLast(message);
            this.newScheduledListUpdate = true;
            this.notifyOrCreateWorker(-1);
        }
        
        return this.messageDispatcher.createFutureOfScheduleResult(resultImpl);
    }
    
    // Controller
    
    public void checkForChannelManager(final ChannelManagerContainer controllerContainer, final ChannelBindingModifyFlags bindingModifyFlags)
    {
        boolean controllerMatch = false;
        if (controllerContainer.getBoundByIdList() != null)
        {
            for (final ComponentBindingSetup.BoundedByChannelId boundedById : controllerContainer.getBoundByIdList())
            {
                if (boundedById.getChannelId() == null)
                {
                    continue;
                }
                if (boundedById.getChannelId().isEmpty())
                {
                    continue;
                }
                if (boundedById.getChannelId().equals(this.channelId))
                {
                    controllerMatch = true;
                    break;
                }
            }
        }
        if (!controllerMatch)
        {
            if (controllerContainer.getBoundedByChannelConfigurationList() != null)
            {
                if (controllerContainer.getFilterObjectList() != null)
                {
                    for (final ControllerFilterObjects controllerFilterObjects : controllerContainer.getFilterObjectList())
                    {
                        if (controllerFilterObjects.filter == null)
                        {
                            continue;
                        }
                        try
                        {
                            if (controllerFilterObjects.filter.matches(this.configurationPropertyBlock.getMatchables()))
                            {
                                controllerMatch = true;
                                break;
                            }
                        }
                        catch (final Exception e)
                        {
                            this.messageDispatcher.logError("check queue binding for controller", e);
                        }
                    }
                }
            }
        }
        
        boolean add = false;
        boolean remove = false;
        
        if (controllerMatch)
        {
            add = setManager(controllerContainer);
        }
        else
        {
            remove = unsetController(controllerContainer);
        }
        
        if (this instanceof SubChannelImpl)
        {
            if (controllerMatch)
            {
                bindingModifyFlags.setSubSet(true);
            }
            if (add)
            {
                bindingModifyFlags.setSubAdd(true);
            }
            if (remove)
            {
                bindingModifyFlags.setSubRemove(true);
            }
        }
        else
        {
            this.subChannelListReadLock.lock();
            try
            {
                for (final SubChannelImpl scope : this.subChannelList)
                {
                    if (scope.isAdoptContoller() && controllerMatch)
                    {
                        bindingModifyFlags.setSubSet(true);
                        if (scope.setManager(controllerContainer))
                        {
                            bindingModifyFlags.setSubAdd(true);
                        }
                    }
                    else
                    {
                        scope.checkForChannelManager(controllerContainer, bindingModifyFlags);
                    }
                }
            }
            finally
            {
                this.subChannelListReadLock.unlock();
            }
            
            if (controllerMatch)
            {
                bindingModifyFlags.setRootSet(true);
            }
            if (add)
            {
                bindingModifyFlags.setRootAdd(true);
            }
            if (remove)
            {
                bindingModifyFlags.setRootRemove(true);
            }
        }
    }
    
    protected boolean setManager(final ChannelManagerContainer controllerContainer)
    {
        this.channelManagerListReadLock.lock();
        try
        {
            if (this.channelManagerIndex.get(controllerContainer) != null)
            {
                return false;
            }
        }
        finally
        {
            this.channelManagerListReadLock.unlock();
        }
        
        this.channelManagerListWriteLock.lock();
        try
        {
            if (this.channelManagerIndex.get(controllerContainer) != null)
            {
                // already registered
                return false;
            }
            
            this.channelManagerList.add(controllerContainer);
            this.channelManagerIndex.put(controllerContainer, controllerContainer);
            this.controllerListCopy = null;
            
            this.recalcRegistrationTypes();
            
            // attachEvent
            if (controllerContainer.isImplementingIOnChannelAttach())
            {
                addOnChannelAttach((IOnChannelAttach) controllerContainer.getChannelManager());
            }
            
            return true;
        }
        finally
        {
            this.channelManagerListWriteLock.unlock();
        }
    }
    
    private boolean unsetController(final ChannelManagerContainer configurationContainer)
    {
        return unsetChannelManager(configurationContainer, false);
    }
    
    protected boolean unsetChannelManager(final ChannelManagerContainer configurationContainer, final boolean unregisterInScope)
    {
        if (unregisterInScope)
        {
            this.subChannelListReadLock.lock();
            try
            {
                for (final SubChannelImpl scope : this.subChannelList)
                {
                    scope.unsetChannelManager(configurationContainer, false);
                }
            }
            finally
            {
                this.subChannelListReadLock.unlock();
            }
        }
        this.channelManagerListReadLock.lock();
        try
        {
            if (this.channelManagerIndex.get(configurationContainer) == null)
            {
                return false;
            }
        }
        finally
        {
            this.channelManagerListReadLock.unlock();
        }
        
        this.channelManagerListWriteLock.lock();
        try
        {
            ChannelManagerContainer unlinkFromQueue = this.channelManagerIndex.get(configurationContainer);
            if (unlinkFromQueue == null)
            {
                return false;
            }
            
            while (this.channelManagerList.remove(unlinkFromQueue)) { }
            this.channelManagerIndex.remove(unlinkFromQueue);
            this.controllerListCopy = null;
            
            // IOnQueueDetach
            
            if (unlinkFromQueue.isImplementingIOnChannelDetach())
            {
                this.messageDispatcher.executeOnChannelDetach((IOnChannelDetach) unlinkFromQueue.getChannelManager(), this);
            }
            
            this.recalcRegistrationTypes();
            
            return true;
        }
        finally
        {
            this.channelManagerListWriteLock.unlock();
        }
    }
    
    protected int getManagerSize()
    {
        this.channelManagerListReadLock.lock();
        try
        {
            return this.channelManagerList.size();
        }
        finally
        {
            this.channelManagerListReadLock.unlock();
        }
    }
    
    protected boolean isMastered()
    {
        this.channelManagerListReadLock.lock();
        try
        {
            for (final ChannelManagerContainer container : this.channelManagerList)
            {
                if (container.isChannelMaster())
                {
                    return true;
                }
            }
            return false;
        }
        finally
        {
            this.channelManagerListReadLock.unlock();
        }
    }
    
    // Services
    
    protected void checkForService(final ServiceContainer serviceContainer, final ChannelBindingModifyFlags bindingModifyFlags)
    {
        boolean serviceMatch = false;
        
        if (serviceContainer.getBoundByIdList() != null)
        {
            for (final ComponentBindingSetup.BoundedByChannelId boundedById : serviceContainer.getBoundByIdList())
            {
                if (boundedById.getChannelId() == null)
                {
                    continue;
                }
                if (boundedById.getChannelId().isEmpty())
                {
                    continue;
                }
                if (boundedById.getChannelId().equals(this.channelId))
                {
                    serviceMatch = true;
                    break;
                }
            }
        }
        if (!serviceMatch)
        {
            if (serviceContainer.getBoundedByChannelConfigurationList() != null)
            {
                for (final ServiceFilterObjects serviceFilterObjects : serviceContainer.getFilterObjectList())
                {
                    try
                    {
                        if (serviceFilterObjects.filter.matches(this.configurationPropertyBlock.getMatchables()))
                        {
                            serviceMatch = true;
                            break;
                        }
                    }
                    catch (final Exception e)
                    {
                        this.messageDispatcher.logError("check queue binding for service", e);
                    }
                }
            }
        }
        
        boolean add = false;
        boolean remove = false;
        
        if (serviceMatch)
        {
            add = setService(serviceContainer, true);
        }
        else
        {
            remove = unsetService(serviceContainer);
        }
        
        if (this instanceof SubChannelImpl)
        {
            if (serviceMatch)
            {
                bindingModifyFlags.setSubSet(true);
            }
            if (add)
            {
                bindingModifyFlags.setSubAdd(true);
            }
            if (remove)
            {
                bindingModifyFlags.setSubRemove(true);
            }
        }
        else
        {
            this.subChannelListReadLock.lock();
            try
            {
                for (final SubChannelImpl scope : this.subChannelList)
                {
                    if (scope.isAdoptServices() && serviceMatch)
                    {
                        bindingModifyFlags.setSubSet(true);
                        if (scope.setService(serviceContainer, true))
                        {
                            bindingModifyFlags.setSubAdd(true);
                        }
                    }
                    else
                    {
                        scope.checkForService(serviceContainer, bindingModifyFlags);
                    }
                }
            }
            finally
            {
                this.subChannelListReadLock.unlock();
            }
            
            if (serviceMatch)
            {
                bindingModifyFlags.setRootSet(true);
            }
            if (add)
            {
                bindingModifyFlags.setRootAdd(true);
            }
            if (remove)
            {
                bindingModifyFlags.setRootRemove(true);
            }
        }
    }
    
    protected boolean setService(final ServiceContainer serviceContainer, final boolean createOnly)
    {
        if (createOnly)
        {
            this.channelServiceListReadLock.lock();
            try
            {
                if (this.channelServiceIndex.get(serviceContainer) != null)
                {
                    return false;
                }
            }
            finally
            {
                this.channelServiceListReadLock.unlock();
            }
        }
        
        boolean reschedule = false;
        this.channelServiceListWriteLock.lock();
        try
        {
            if (this.channelServiceIndex.get(serviceContainer) != null)
            {
                if (createOnly)
                {
                    return false;
                }
                reschedule = true;
            }
            
            if (!reschedule)
            {
                this.channelServiceList.add(serviceContainer);
                this.channelServiceIndex.put(serviceContainer, serviceContainer);
                this.serviceListCopy = null;
            }
        }
        finally
        {
            this.channelServiceListWriteLock.unlock();
        }
        
        this.scheduleService(serviceContainer.getChannelService(), serviceContainer.getServiceConfiguration(), reschedule);
        
        return true;
        
    }
    
    private void scheduleService(final IDispatcherChannelService queueService, final ComponentBindingSetup.ChannelServiceConfiguration configuration, final boolean reschedule)
    {
        String serviceId = configuration.getServiceId();
        long delay = configuration.getStartDelayInMS() < 0L ? 0L : configuration.getStartDelayInMS();
        long timeout = configuration.getTimeOutInMS() < 0L ? -1L : configuration.getTimeOutInMS();
        long hbtimeout = configuration.getHeartbeatTimeOutInMS() < 0L ? -1L : configuration.getHeartbeatTimeOutInMS();
        
        try
        {
            if (reschedule)
            {
                this.rescheduleTask(serviceId, System.currentTimeMillis() + delay, timeout, hbtimeout);
                return;
            }
            IPropertyBlock servicePropertyBlock = this.messageDispatcher.createPropertyBlock();
            if (configuration.getPeriodicRepetitionIntervalMS() < 0L)
            {
                servicePropertyBlock.removeProperty(PROPERTY_PERIODIC_REPETITION_INTERVAL);
            }
            else
            {
                servicePropertyBlock.setProperty(PROPERTY_PERIODIC_REPETITION_INTERVAL, configuration.getPeriodicRepetitionIntervalMS());
            }
            
            this.scheduleTask(serviceId, queueService, servicePropertyBlock, System.currentTimeMillis() + delay, timeout, hbtimeout);
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("problems scheduling service with id " + serviceId, e);
        }
    }
    
    private boolean unsetService(final ServiceContainer serviceContainer)
    {
        return unsetService(serviceContainer, false);
    }
    
    protected boolean unsetService(final ServiceContainer serviceContainer, final boolean unregisterInScope)
    {
        if (unregisterInScope)
        {
            this.subChannelListReadLock.lock();
            try
            {
                for (final SubChannelImpl scope : this.subChannelList)
                {
                    scope.unsetService(serviceContainer, false);
                }
            }
            finally
            {
                this.subChannelListReadLock.unlock();
            }
        }
        
        this.channelServiceListReadLock.lock();
        try
        {
            if (this.channelServiceIndex.get(serviceContainer) == null)
            {
                return false;
            }
        }
        finally
        {
            this.channelServiceListReadLock.unlock();
        }
        
        this.channelServiceListWriteLock.lock();
        try
        {
            ServiceContainer toDelete = this.channelServiceIndex.get(serviceContainer);
            if (toDelete == null)
            {
                return false;
            }
            while (this.channelServiceList.remove(toDelete)) { }
            this.channelServiceIndex.remove(serviceContainer);
            this.serviceListCopy = null;
            
            this.taskListReadLock.lock();
            try
            {
                for (final Entry<String, TaskContainer> taskContainerEntry : this.taskIndex.entrySet())
                {
                    try
                    {
                        if (taskContainerEntry.getValue().getTask() == serviceContainer.getChannelService())
                        {
                            taskContainerEntry.getValue().getTaskControl().setDone();
                        }
                    }
                    catch (final Exception e)
                    {
                        this.messageDispatcher.logError("set queue service done", e);
                    }
                }
            }
            finally
            {
                this.taskListReadLock.unlock();
            }
            
            return true;
            
        }
        finally
        {
            this.channelServiceListWriteLock.unlock();
        }
    }
    
    protected int getServiceSize()
    {
        this.channelServiceListReadLock.lock();
        try
        {
            return this.channelServiceList.size();
        }
        finally
        {
            this.channelServiceListReadLock.unlock();
        }
    }
    
    @Override
    public IPropertyBlock getConfigurationPropertyBlock()
    {
        return this.configurationPropertyBlock;
    }
    
    @Override
    public IPropertyBlock getStatePropertyBlock()
    {
        return this.statePropertyBlock;
    }
    
    @Override
    public String getId()
    {
        return this.channelId;
    }
    
    protected int cleanDoneTasks()
    {
        List<TaskContainer> toRemove = null;
        this.taskListWriteLock.lock();
        try
        {
            
            for (final TaskContainer taskContainer : this.taskList)
            {
                if (taskContainer.getTaskControl().isDone())
                {
                    if (toRemove == null)
                    {
                        toRemove = new ArrayList<TaskContainer>();
                    }
                    toRemove.add(taskContainer);
                }
            }
            
            if (toRemove == null)
            {
                return 0;
            }
            
            for (final TaskContainer taskContainer : toRemove)
            {
                String id = taskContainer.getId();
                this.taskList.remove(taskContainer);
                
                TaskContainer containerById = this.taskIndex.get(id);
                if (containerById == null)
                {
                    continue;
                }
                if (containerById == taskContainer)
                {
                    this.taskIndex.remove(id);
                }
            }
            return toRemove.size();
        }
        finally
        {
            this.taskListWriteLock.unlock();
        }
    }
    
    protected long getDueTasks(final List<TaskContainer> dueTaskList)
    {
        this.taskListReadLock.lock();
        long timeStamp = System.currentTimeMillis();
        long nextRun = timeStamp + ChannelWorker.DEFAULT_WAIT_TIME;
        try
        {
            
            for (final TaskContainer taskContainer : this.taskList)
            {
                if (taskContainer.getTaskControl().isDone())
                {
                    continue;
                }
                long executionTimeStampIntern = taskContainer.getTaskControl().getExecutionTimeStampIntern();
                if (executionTimeStampIntern < nextRun)
                {
                    nextRun = executionTimeStampIntern;
                }
                
                if (executionTimeStampIntern <= timeStamp)
                {
                    dueTaskList.add(taskContainer);
                }
            }
        }
        finally
        {
            this.taskListReadLock.unlock();
        }
        
        return nextRun;
    }
    
    protected long getNextRun()
    {
        this.taskListReadLock.lock();
        long timeStamp = System.currentTimeMillis();
        long nextRun = timeStamp + ChannelWorker.DEFAULT_WAIT_TIME;
        try
        {
            
            for (final TaskContainer taskContainer : this.taskList)
            {
                if (taskContainer.getTaskControl().isDone())
                {
                    continue;
                }
                long executionTimeStampIntern = taskContainer.getTaskControl().getExecutionTimeStampIntern();
                if (executionTimeStampIntern < nextRun)
                {
                    nextRun = executionTimeStampIntern;
                }
            }
        }
        finally
        {
            this.taskListReadLock.unlock();
        }
        
        return nextRun;
    }
    
    @Override
    public IPropertyBlock getTaskPropertyBlock(final String id)
    {
        if (this.disposed)
        {
            return null;
        }
        this.taskListReadLock.lock();
        try
        {
            TaskContainer taskContainer = this.taskIndex.get(id);
            if (taskContainer != null)
            {
                if (!taskContainer.getTaskControl().isDone())
                {
                    return taskContainer.getPropertyBlock();
                }
            }
        }
        finally
        {
            this.taskListReadLock.unlock();
        }
        
        return null;
    }
    
    @Override
    public String scheduleTask(final IDispatcherChannelTask task)
    {
        return scheduleTask(null, task);
    }
    
    @Override
    public String scheduleTask(final String id, final IDispatcherChannelTask task)
    {
        return scheduleTask(id, task, null, -1, -1, -1);
    }
    
    @Override
    public String scheduleTask(final String id, final IDispatcherChannelTask task, final IPropertyBlock propertyBlock, final long executionTimeStamp, final long timeOutValue, final long heartBeatTimeOut)
    {
        return scheduleTask(id, task, propertyBlock, executionTimeStamp, timeOutValue, heartBeatTimeOut, false);
    }
    
    @Override
    public String scheduleTask(String id, final IDispatcherChannelTask task, IPropertyBlock propertyBlock, final long executionTimeStamp, final long timeOutValue, final long heartBeatTimeOut, final boolean stopOnTimeOut)
    {
        if (this.disposed)
        {
            return null;
        }
        
        TaskContainer taskContainer = null;
        
        this.taskListWriteLock.lock();
        try
        {
            TaskContainer toRemove = null;
            for (final TaskContainer alreadyInList : this.taskList)
            {
                if (alreadyInList.getTask() == task)
                {
                    if (alreadyInList.getTaskControl().isDone())
                    {
                        toRemove = alreadyInList;
                        break;
                    }
                    if ((id == null) || (id.isEmpty()))
                    {
                        return null;
                    }
                    return alreadyInList.getId();
                }
            }
            if (toRemove != null)
            {
                this.taskIndex.remove(toRemove.getId());
                this.taskList.remove(toRemove);
                
                toRemove = null;
            }
            
            if ((id == null) || (id.isEmpty()))
            {
                id = UUID.randomUUID().toString();
                taskContainer = new TaskContainer();
            }
            else
            {
                taskContainer = this.taskIndex.get(id);
                if (taskContainer != null)
                {
                    if (taskContainer.getTaskControl().isDone())
                    {
                        this.taskIndex.remove(taskContainer.getId());
                        this.taskList.remove(taskContainer);
                        
                        taskContainer = null;
                    }
                    else
                    {
                        return id;
                    }
                }
                
                taskContainer = new TaskContainer();
                taskContainer.setNamedTask(true);
            }
            
            if (propertyBlock == null)
            {
                propertyBlock = this.getDispatcher().createPropertyBlock();
            }
            
            TaskControlImpl taskControl = new TaskControlImpl();
            if (executionTimeStamp > 0)
            {
                taskControl.setExecutionTimeStamp(executionTimeStamp, ExecutionTimestampSource.SCHEDULE, ScheduleTimestampPredicate.getInstance());
            }
            if (heartBeatTimeOut > 0)
            {
                taskControl.setHeartbeatTimeout(heartBeatTimeOut);
            }
            if (timeOutValue > 0)
            {
                taskControl.setTimeout(timeOutValue);
            }
            
            taskControl.setStopOnTimeoutFlag(stopOnTimeOut);
            
            propertyBlock.setProperty(PROPERTY_KEY_TASK_ID, id);
            
            taskContainer.setId(id);
            taskContainer.setTask(task);
            taskContainer.setPropertyBlock(propertyBlock);
            taskContainer.setTaskControl(taskControl);
        }
        finally
        {
            this.taskListWriteLock.unlock();
        }
        
        taskContainer.getTask().configure(this, id, taskContainer.getPropertyBlock(), taskContainer.getTaskControl());
        
        this.taskListWriteLock.lock();
        try
        {
            this.taskList.add(taskContainer);
            this.taskIndex.put(id, taskContainer);
        }
        finally
        {
            this.taskListWriteLock.unlock();
        }
        notifyOrCreateWorker(executionTimeStamp);
        
        return id;
    }
    
    @Override
    public IDispatcherChannelTask rescheduleTask(final String id, final long executionTimeStamp, final long timeOutValue, final long heartBeatTimeOut)
    {
        if (this.disposed)
        {
            return null;
        }
        
        TaskContainer taskContainer = null;
        
        if ((id == null) || (id.isEmpty()))
        {
            return null;
        }
        
        this.taskListWriteLock.lock();
        try
        {
            taskContainer = this.taskIndex.get(id);
            if (taskContainer == null)
            {
                return null;
            }
            
            if (taskContainer.getTaskControl().isDone())
            {
                this.taskIndex.remove(taskContainer.getId());
                this.taskList.remove(taskContainer);
                return null;
            }
            
            TaskControlImpl taskControl = taskContainer.getTaskControl();
            
            if (heartBeatTimeOut > 0)
            {
                taskControl.setHeartbeatTimeout(heartBeatTimeOut);
            }
            if (timeOutValue > 0)
            {
                taskControl.setTimeout(timeOutValue);
            }
            
            if (executionTimeStamp > 0)
            {
                if (taskControl.setExecutionTimeStamp(executionTimeStamp, ExecutionTimestampSource.RESCHEDULE, RescheduleTimestampPredicate.getInstance()))
                {
                    this.notifyOrCreateWorker(executionTimeStamp);
                }
            }
            
            return taskContainer.getTask();
        }
        finally
        {
            this.taskListWriteLock.unlock();
        }
    }
    
    @Override
    public IDispatcherChannelTask getTask(final String id)
    {
        if (this.disposed)
        {
            return null;
        }
        
        this.taskListReadLock.lock();
        try
        {
            TaskContainer taskContainer = this.taskIndex.get(id);
            if (taskContainer != null)
            {
                if (!taskContainer.getTaskControl().isDone())
                {
                    return taskContainer.getTask();
                }
            }
        }
        finally
        {
            this.taskListReadLock.unlock();
        }
        
        return null;
    }
    
    @Override
    public IDispatcherChannelTask removeTask(final String id)
    {
        if (this.disposed)
        {
            return null;
        }
        
        this.taskListWriteLock.lock();
        try
        {
            TaskContainer taskContainer = this.taskIndex.get(id);
            if (taskContainer != null)
            {
                this.taskIndex.remove(id);
                this.taskList.remove(taskContainer);
            }
        }
        finally
        {
            this.taskListWriteLock.unlock();
        }
        
        return null;
    }
    
    @Override
    public IMessage getMessage(final UUID id)
    {
        if (this.disposed)
        {
            return null;
        }
        
        if (id == null)
        {
            return null;
        }
        
        DequeSnapshot<MessageImpl> snapshot = this.messageQueue.createSnapshot();
        try
        {
            for (final IMessage queuedEvent : snapshot)
            {
                if (id.equals(queuedEvent.getId()))
                {
                    return queuedEvent;
                }
            }
        }
        finally
        {
            try
            {
                snapshot.close();
            }
            catch (final Exception e)
            {
                this.messageDispatcher.logError("close multichain snapshot", e);
            }
        }
        
        return null;
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DequeSnapshot<IMessage<T>> getMessageSnapshot()
    {
        if (this.disposed)
        {
            return null;
        }
        
        if (Thread.currentThread() == this.channelWorker)
        {
            DequeSnapshot snaphot = this.messageQueue.createSnapshot();
            this.snapshotsByWorkerThread.add(snaphot);
            return snaphot;
        }
        return (DequeSnapshot) this.messageQueue.createSnapshot();
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DequeSnapshot<IMessage<T>> getMessageSnapshotPoll()
    {
        if (this.disposed)
        {
            return null;
        }
        
        if (Thread.currentThread() == this.channelWorker)
        {
            DequeSnapshot snaphot = this.messageQueue.createSnapshotPoll();
            this.snapshotsByWorkerThread.add(snaphot);
            return snaphot;
        }
        return (DequeSnapshot) this.messageQueue.createSnapshotPoll();
    }
    
    public void closeWorkerSnapshots()
    {
        if (this.snapshotsByWorkerThread.isEmpty())
        {
            return;
        }
        
        try
        {
            for (final DequeSnapshot<IMessage<T>> snapshot : this.snapshotsByWorkerThread)
            {
                try
                {
                    snapshot.close();
                }
                catch (final Exception e)
                {
                    this.messageDispatcher.logError("close multichain worker snapshots", e);
                }
            }
            this.snapshotsByWorkerThread.clear();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("close multichain worker snapshots", e);
        }
    }
    
    protected boolean removeMessage(final MessageImpl message)
    {
        if (message == null)
        {
            return false;
        }
        
        DequeNode<MessageImpl> node = message.getNode();
        if (node != null)
        {
            node.unlink();
        }
        
        if (this.registrationTypes.onRemoveMessage)
        {
            this.removedMessageQueue.addLast(message);
            this.removedEventListUpdate = true;
            this.notifyOrCreateWorker(-1);
        }
        else
        {
            message.dispose();
        }
        
        return true;
    }
    
    @Override
    public boolean removeMessage(final UUID uuid)
    {
        if (this.disposed)
        {
            return false;
        }
        
        if (uuid == null)
        {
            return false;
        }
        
        MessageImpl removed = null;
        DequeSnapshot<MessageImpl> snapshot = this.messageQueue.createSnapshot();
        
        try
        {
            for (final MessageImpl event : snapshot)
            {
                if (uuid.equals(event.getId()))
                {
                    DequeNode<MessageImpl> node = event.getNode();
                    if (node != null)
                    {
                        node.unlink();
                        event.setNode(null);
                    }
                    removed = event;
                    break;
                }
            }
            if (removed == null)
            {
                return false;
            }
        }
        finally
        {
            try
            {
                snapshot.close();
            }
            catch (final Exception e)
            {
                this.messageDispatcher.logError("close deque snapshot", e);
            }
        }
        
        if (this.registrationTypes.onRemoveMessage)
        {
            this.removedMessageQueue.addLast(removed);
            this.removedEventListUpdate = true;
            this.notifyOrCreateWorker(-1);
        }
        
        return true;
    }
    
    @Override
    public boolean removeMessageList(final List<UUID> uuidList)
    {
        if (this.disposed)
        {
            return false;
        }
        
        if (uuidList == null)
        {
            return false;
        }
        
        if (uuidList.isEmpty())
        {
            return false;
        }
        
        boolean removed = false;
        List<MessageImpl> removeMessageList = null;
        if (this.registrationTypes.onRemoveMessage)
        {
            removeMessageList = new ArrayList<MessageImpl>(uuidList.size());
        }
        DequeSnapshot<MessageImpl> snapshot = this.messageQueue.createSnapshot();
        try
        {
            for (final MessageImpl message : snapshot)
            {
                for (final UUID uuid : uuidList)
                {
                    if (uuid == null)
                    {
                        continue;
                    }
                    if (uuid.equals(message.getId()))
                    {
                        removed = true;
                        DequeNode<MessageImpl> node = message.getNode();
                        if (node != null)
                        {
                            node.unlink();
                        }
                        message.setNode(null);
                        if (removeMessageList != null)
                        {
                            removeMessageList.add(message);
                        }
                    }
                }
            }
            if (!removed)
            {
                return false;
            }
        }
        finally
        {
            try
            {
                snapshot.close();
            }
            catch (final Exception e)
            {
                this.messageDispatcher.logError("close deque snapshot", e);
            }
        }
        
        if (removeMessageList != null)
        {
            this.removedMessageQueue.addAll(removeMessageList);
            this.removedEventListUpdate = true;
            this.notifyOrCreateWorker(-1);
            
            removeMessageList.clear();
        }
        
        return true;
    }
    
    protected List<ChannelManagerContainer> getManagerContainerList()
    {
        List<ChannelManagerContainer> list = this.controllerListCopy;
        if (list != null)
        {
            return list;
        }
        this.channelManagerListReadLock.lock();
        try
        {
            list = new ArrayList<ChannelManagerContainer>();
            for (final ChannelManagerContainer container : this.channelManagerList)
            {
                list.add(container);
            }
            list = Collections.unmodifiableList(list);
            this.controllerListCopy = list;
        }
        finally
        {
            this.channelManagerListReadLock.unlock();
        }
        
        return list;
    }
    
    public List<ServiceContainer> getServiceContainerList()
    {
        List<ServiceContainer> list = this.serviceListCopy;
        if (list != null)
        {
            return list;
        }
        this.channelServiceListReadLock.lock();
        try
        {
            list = new ArrayList<ServiceContainer>();
            for (final ServiceContainer service : this.channelServiceList)
            {
                list.add(service);
            }
            list = Collections.unmodifiableList(list);
            this.serviceListCopy = list;
        }
        finally
        {
            this.channelServiceListReadLock.unlock();
        }
        
        return list;
    }
    
    public boolean checkTimeOut()
    {
        ChannelWorker worker = null;
        this.workerSpoolLock.lock();
        try
        {
            worker = this.channelWorker;
        }
        finally
        {
            this.workerSpoolLock.unlock();
        }
        
        boolean timeOut = false;
        if (worker != null)
        {
            AtomicBoolean stopTask = new AtomicBoolean(false);
            try
            {
                timeOut = worker.checkTimeOut(stopTask);
                if (timeOut)
                {
                    this.workerSpoolLock.lock();
                    try
                    {
                        if (worker == this.channelWorker)
                        {
                            this.channelWorker = null;
                        }
                    }
                    finally
                    {
                        this.workerSpoolLock.unlock();
                    }
                }
            }
            catch (Exception | Error e)
            {
                this.messageDispatcher.logError("check worker timeout", e);
            }
        }
        return timeOut;
    }
    
    public void dispose()
    {
        if (this.disposed)
        {
            return;
        }
        this.disposed = true;
        
        this.queueScopeListWriteLock.lock();
        try
        {
            if (!this.subChannelList.isEmpty())
            {
                List<SubChannelImpl> scopeCopyList = new ArrayList<SubChannelImpl>(this.subChannelList);
                for (final SubChannelImpl scope : scopeCopyList)
                {
                    scope.dispose();
                }
                scopeCopyList.clear();
            }
        }
        finally
        {
            this.queueScopeListWriteLock.unlock();
        }
        
        if (this.channelConfigurationModifyListener != null)
        {
            this.configurationPropertyBlock.removeModifyListener(this.channelConfigurationModifyListener);
        }
        
        stopQueueWorker();
        
        this.closeWorkerSnapshots();
        
        if ((this instanceof SubChannelImpl) && (this.parentChannel != null))
        {
            this.parentChannel.removeScope((SubChannelImpl) this);
            
            this.channelManagerListReadLock.lock();
            try
            {
                for (final ChannelManagerContainer controllerContainer : this.channelManagerList)
                {
                    if (controllerContainer.isImplementingIOnChannelDetach())
                    {
                        this.messageDispatcher.executeOnChannelDetach(((IOnChannelDetach) controllerContainer.getChannelManager()), this);
                    }
                }
            }
            finally
            {
                this.channelManagerListReadLock.unlock();
            }
        }
        
        this.channelServiceListWriteLock.lock();
        try
        {
            this.channelServiceList.clear();
            this.channelServiceIndex.clear();
            this.serviceListCopy = null;
            
        }
        finally
        {
            this.channelServiceListWriteLock.unlock();
        }
        
        this.sharedMessageLock = null;
        
        try
        {
            this.taskListReadLock.lock();
            try
            {
                for (final Entry<String, TaskContainer> taskContainerEntry : this.taskIndex.entrySet())
                {
                    try
                    {
                        taskContainerEntry.getValue().getTaskControl().setDone();
                    }
                    catch (final Exception e)
                    {
                        this.messageDispatcher.logError("set queue task / service done", e);
                    }
                }
            }
            finally
            {
                this.taskListReadLock.unlock();
            }
        }
        catch (final Exception e) { }
        
        try
        {
            this.messageQueue.dispose();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("dispose event queue", e);
        }
        
        try
        {
            this.removedMessageQueue.dispose();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("dispose new event queue", e);
        }
        
        try
        {
            this.newPublishedMessageQueue.dispose();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("dispose new event queue", e);
        }
        
        try
        {
            this.channelSignalList.dispose();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("dispose signal queue", e);
        }
        
        try
        {
            this.onChannelAttachList.dispose();
        }
        catch (final Exception e)
        {
            this.messageDispatcher.logError("dispose channel attach queue", e);
        }
        
        this.registrationTypes = null;
        
        if (this.dummyPublishMessageResult != null)
        {
            try
            {
                this.dummyPublishMessageResult.disposeDummy();
            }
            catch (final Exception e) { }
            this.dummyPublishMessageResult = null;
        }
        
        this.rootChannel = null;
        this.parentChannel = null;
        
    }
    
    private void removeScope(final SubChannelImpl scope)
    {
        SubChannelImpl found = null;
        this.queueScopeListWriteLock.lock();
        try
        {
            UUID scopeId = scope.getScopeId();
            if (scopeId != null)
            {
                List<ISubChannel> copyList = this.subChannelListCopy;
                if (!((copyList == null) || copyList.isEmpty()))
                {
                    for (final ISubChannel subChannel : copyList)
                    {
                        if (subChannel == scope)
                        {
                            found = scope;
                            break;
                        }
                    }
                }
            }
            this.subChannelIndex.remove(scope.getScopeId());
            while (this.subChannelList.remove(scope)) { }
            this.subChannelListCopy = Collections.unmodifiableList(new ArrayList<ISubChannel>(this.subChannelList));
        }
        finally
        {
            this.queueScopeListWriteLock.unlock();
        }
        
        if (found != null)
        {
            found.dispose();
        }
    }
    
    protected void stopQueueWorker()
    {
        ChannelWorker worker = null;
        this.workerSpoolLock.lock();
        try
        {
            if (this.channelWorker != null)
            {
                worker = this.channelWorker;
                this.channelWorker = null;
                worker.softStopWorker();
            }
        }
        finally
        {
            this.workerSpoolLock.unlock();
        }
        
        if (worker != null)
        {
            worker.stopWorker();
        }
    }
    
    protected MessageDispatcherImpl getMessageDispatcher()
    {
        return this.messageDispatcher;
    }
    
    protected DequeSnapshot<String> getSignalsSnapshot()
    {
        if (!this.signalListUpdate)
        {
            return null;
        }
        
        this.signalListUpdate = false;
        return this.channelSignalList.createSnapshotPoll();
    }
    
    protected DequeSnapshot<? extends IMessage> getNewScheduledEventsSnaphot()
    {
        if (!this.newScheduledListUpdate)
        {
            return null;
        }
        
        this.newScheduledListUpdate = false;
        return this.newPublishedMessageQueue.createSnapshotPoll();
    }
    
    protected DequeSnapshot<? extends IMessage> getRemovedMessagesSnapshot()
    {
        if (!this.removedEventListUpdate)
        {
            return null;
        }
        
        this.removedEventListUpdate = false;
        return this.removedMessageQueue.createSnapshotPoll();
    }
    
    protected void notifyOrCreateWorker(final long nextRuntimeStamp)
    {
        if (this.disposed)
        {
            return;
        }
        
        boolean notify = false;
        ChannelWorker worker = null;
        
        this.workerSpoolLock.lock();
        
        try
        {
            if (this.channelWorker == null)
            {
                if (this.disposed)
                {
                    return;
                }
                
                if (this.currentSpooledChannelWorker != null)
                {
                    this.currentSpooledChannelWorker.setValid(false);
                    this.currentSpooledChannelWorker = null;
                }
                
                ChannelWorker queueWorker = this.messageDispatcher.getFromWorkerPool();
                if
                (
                    (queueWorker != null) &&
                    (queueWorker.isGo()) &&
                    (queueWorker.getMessageChannel() == null) &&
                    queueWorker.setMessageChannel(this)
                )
                {
                    notify = true;
                    this.channelWorker = queueWorker;
                }
                else
                {
                    if (queueWorker != null)
                    {
                        try
                        {
                            queueWorker.stopWorker();        // LOCK WORER.waitMonitor
                        }
                        catch (Exception | Error e)
                        {
                            this.messageDispatcher.logError("stop worker", e);
                        }
                    }
                    
                    queueWorker = new ChannelWorker(this);
                    queueWorker.start();
                    
                    this.channelWorker = queueWorker;
                }
            }
            else
            {
                notify = true;
            }
            worker = this.channelWorker;
            
            worker.notifySoftUpdate();
        }
        finally
        {
            this.workerSpoolLock.unlock();
        }
        
        if (notify)
        {
            if (nextRuntimeStamp < 1)
            {
                worker.notifyUpdate(); // LOCK WORKER.waitMonitor
            }
            else
            {
                worker.notifyUpdate(nextRuntimeStamp); // LOCK WORKER.waitMonitor
            }
        }
    }
    
    protected boolean checkFreeWorker(final ChannelWorker worker, final long nextRun)
    {
        if (worker == null)
        {
            return false;
        }
        
        if (this.privateWorker)
        {
            return false;
        }
        
        if (!worker.isGo())
        {
            return false;
        }
        
        this.workerSpoolLock.lock();
        try
        {
            if (worker != this.channelWorker)
            {
                worker.stopWorker();
                return false;
            }
            
            if (worker.isUpdateNotified || worker.isSoftUpdated)
            {
                return false;
            }
            
            if (this.newPublishedMessageQueue.size() > 0)
            {
                return false;
            }
            if (this.removedMessageQueue.size() > 0)
            {
                return false;
            }
            if (this.channelSignalList.size() > 0)
            {
                return false;
            }
            if (this.onChannelAttachList.size() > 0)
            {
                return false;
            }
            
            if (!worker.setMessageChannel(null))
            {
                return false;
            }
            
            if (this.currentSpooledChannelWorker != null)
            {
                this.currentSpooledChannelWorker.setValid(false);
            }
            this.currentSpooledChannelWorker = this.messageDispatcher.scheduleChannelWorker(this, nextRun - ChannelWorker.RESCHEDULE_BUFFER_TIME);
            this.channelWorker = null;
            this.messageDispatcher.addToWorkerPool(worker);
            return true;
        }
        finally
        {
            this.workerSpoolLock.unlock();
        }
    }
    
    protected boolean checkWorkerShutdown(final ChannelWorker worker)
    {
        if (worker == null)
        {
            return false;
        }
        
        if (this.privateWorker)
        {
            return false;
        }
        
        this.workerSpoolLock.lock();
        try
        {
            if (worker != this.channelWorker)
            {
                worker.stopWorker();
                return false;
            }
            
            if (worker.isUpdateNotified || worker.isSoftUpdated)
            {
                return false;
            }
            if (this.newPublishedMessageQueue.size() > 0)
            {
                return false;
            }
            if (this.removedMessageQueue.size() > 0)
            {
                return false;
            }
            if (this.channelSignalList.size() > 0)
            {
                return false;
            }
            if (this.onChannelAttachList.size() > 0)
            {
                return false;
            }
            
            this.taskListReadLock.lock();
            try
            {
                if (!this.taskList.isEmpty())
                {
                    return false;
                }
            }
            finally
            {
                this.taskListReadLock.unlock();
            }
            
            if (this.currentSpooledChannelWorker != null)
            {
                this.currentSpooledChannelWorker.setValid(false);
            }
            if (!worker.setMessageChannel(null))
            {
                return false;
            }
            this.messageDispatcher.addToWorkerPool(worker);
            this.channelWorker = null;
            
            return true;
        }
        finally
        {
            this.workerSpoolLock.unlock();
        }
    }
    
    public TaskContainer getCurrentRunningTask()
    {
        ChannelWorker worker = this.channelWorker;
        if (worker == null)
        {
            this.workerSpoolLock.lock();
            try
            {
                worker = this.channelWorker;
            }
            finally
            {
                this.workerSpoolLock.unlock();
            }
        }
        if (worker == null)
        {
            return null;
        }
        
        return worker.getCurrentRunningTask();
    }
    
    @Override
    public IMessageDispatcher getDispatcher()
    {
        return this.messageDispatcher;
    }
    
    @Override
    public void signal(final String signal)
    {
        if (this.disposed)
        {
            return;
        }
        
        this.channelSignalList.addLast(signal);
        this.signalListUpdate = true;
        
        if (this.registrationTypes.onSignal)
        {
            this.notifyOrCreateWorker(-1);
        }
    }
    
    public DequeSnapshot<IOnChannelAttach> getOnQueueAttachList()
    {
        if (!this.onQueueAttachListUpdate)
        {
            return null;
        }
        
        this.onQueueAttachListUpdate = false;
        return this.onChannelAttachList.createSnapshotPoll();
    }
    
    protected void addOnChannelAttach(final IOnChannelAttach onChannelAttach)
    {
        this.onQueueAttachListUpdate = true;
        this.onChannelAttachList.addLast(onChannelAttach);
        
        this.notifyOrCreateWorker(-1);
    }
    
    @Override
    public String getChannelName()
    {
        return this.name;
    }
    
    public void touchLastWorkerAction()
    {
        this.lastWorkerAction = System.currentTimeMillis();
    }
    
    public long getLastWorkerAction()
    {
        return this.lastWorkerAction;
    }
    
    public ChannelConfigurationModifyListener getQueueConfigurationModifyListener()
    {
        return this.channelConfigurationModifyListener;
    }
    
    public void setQueueConfigurationModifyListener(final ChannelConfigurationModifyListener queueConfigurationModifyListener)
    {
        this.channelConfigurationModifyListener = queueConfigurationModifyListener;
    }
    
    @Override
    public ISubChannel createChildScope(UUID scopeId, final String scopeName, final Map<String, Object> configurationProperties, final Map<String, Object> stateProperties, final boolean adoptContoller, final boolean adoptServices)
    {
        if (this.disposed)
        {
            return null;
        }
        if (scopeId == null)
        {
            scopeId = UUID.randomUUID();
        }
        
        SubChannelImpl newScope = null;
        
        this.queueScopeListWriteLock.lock();
        try
        {
            if (this.disposed)
            {
                return null;
            }
            if (this.subChannelIndex.get(scopeId) != null)
            {
                return null;
            }
            
            newScope = new SubChannelImpl(scopeId, this.rootChannel, this, scopeName, adoptContoller, adoptServices, configurationProperties, stateProperties);
            
            this.subChannelList.add(newScope);
            this.subChannelListCopy = Collections.unmodifiableList(new ArrayList<ISubChannel>(this.subChannelList));
            this.subChannelIndex.put(scopeId, newScope);
        }
        finally
        {
            this.queueScopeListWriteLock.unlock();
        }
        
        if ((configurationProperties != null) && (!configurationProperties.isEmpty()))
        {
            this.messageDispatcher.onConfigurationModify(newScope, configurationProperties.keySet().toArray(new String[configurationProperties.size()]));
        }
        
        return newScope;
    }
    
    @Override
    public List<ISubChannel> getChildScopes()
    {
        return this.subChannelListCopy;
    }
    
    @Override
    public ISubChannel getChildScope(final UUID scopeId)
    {
        if (this.disposed)
        {
            return null;
        }
        
        this.subChannelListReadLock.lock();
        try
        {
            return this.subChannelIndex.get(scopeId);
        }
        finally
        {
            this.subChannelListReadLock.unlock();
        }
    }
    
    public int getCapacity()
    {
        return this.capacity;
    }
    
    protected void setCapacity(final int eventListLimit)
    {
        this.capacity = eventListLimit;
        this.messageQueue.setCapacity(eventListLimit);
    }
    
    @Override
    public IDispatcherChannel<Object> getRootChannel()
    {
        return this.rootChannel;
    }
    
    @Override
    public IDispatcherChannel<Object> getParentChannel()
    {
        return this.parentChannel;
    }
    
    protected ReentrantLock getMessageEventLock()
    {
        return this.sharedMessageLock;
    }
    
    public void recalcRegistrationTypes()
    {
        RegistrationTypes newRegistrationTypes = new RegistrationTypes();
        
        for (final ChannelManagerContainer controllerContainer : getManagerContainerList())
        {
            if (controllerContainer.isImplementingIOnMessageStore() || controllerContainer.isImplementingIOnMessageStoreSnapshot())
            {
                newRegistrationTypes.onQueuedMessage = true;
            }
            if (controllerContainer.isImplementingIOnMessageRemove() || controllerContainer.isImplementingIOnMessageRemoveSnapshot())
            {
                newRegistrationTypes.onRemoveMessage = true;
            }
            if (controllerContainer.isImplementingIOnChannelSignal())
            {
                newRegistrationTypes.onSignal = true;
            }
        }
        
        this.registrationTypes = newRegistrationTypes;
    }
    
    @Override
    public boolean equals(final Object obj)
    {
        return this == obj;
    }
    
    public class RegistrationTypes
    {
        boolean onQueuedMessage = false;
        boolean onRemoveMessage = false;
        boolean onSignal = false;
    }
}
