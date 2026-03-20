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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sodeac.common.message.dispatcher.api.ChannelNotFoundException;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup.BoundedByChannelConfiguration;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup.BoundedByChannelId;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager.IChannelManagerPolicy;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelService;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelService.IChannelServicePolicy;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcher;
import org.sodeac.common.message.dispatcher.api.IOnChannelDetach;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreResult;
import org.sodeac.common.message.dispatcher.api.IOnTaskStop;
import org.sodeac.common.message.dispatcher.api.IOnTaskTimeout;
import org.sodeac.common.message.dispatcher.api.IPropertyBlock;
import org.sodeac.common.message.dispatcher.api.ISubChannel;
import org.sodeac.common.snapdeque.DequeNode;
import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.snapdeque.SnapshotableDeque;

public class MessageDispatcherImpl implements IMessageDispatcher
{
    private final Map<String, ChannelImpl<?>> channelIndex;
    private final ReentrantReadWriteLock channelIndexLock;
    private final ReadLock channelIndexReadLock;
    private final WriteLock channelIndexWriteLock;

    private final ReentrantReadWriteLock lifecycleLock;
    private final ReadLock lifecycleReadLock;
    private final WriteLock lifecycleWriteLock;

    private final SnapshotableDeque<ChannelWorker> workerPool;

    private final DispatcherGuardian dispatcherGuardian;
    private final SpooledChannelWorkerScheduler spooledChannelWorkerScheduler;

    private SnapshotableDeque<ChannelManagerContainer> managerList = null;
    private SnapshotableDeque<ServiceContainer> serviceList = null;

    private final PropertyBlockImpl propertyBlock;

    private String id = null;

    private volatile boolean activated = false;

    private ExecutorService executorService = null;

    private ConfigurationPropertyBindingRegistry configurationPropertyBindingRegistry = null;

    private Map<IDispatcherChannelManager, ChannelManagerContainer> channelManagerIndex = null;
    private Map<IDispatcherChannelService, ServiceContainer> serviceContainerIndex = null;

    private final Logger logger = LoggerFactory.getLogger(MessageDispatcherImpl.class);
    private boolean stopped = false;

    @Override
    public String getId()
    {
        return this.id;
    }

    protected MessageDispatcherImpl(final String id)
    {
        super();

        this.channelIndex = new HashMap<String, ChannelImpl<?>>();
        this.channelIndexLock = new ReentrantReadWriteLock(true);
        this.channelIndexReadLock = this.channelIndexLock.readLock();
        this.channelIndexWriteLock = this.channelIndexLock.writeLock();

        this.lifecycleLock = new ReentrantReadWriteLock(true);
        this.lifecycleReadLock = this.lifecycleLock.readLock();
        this.lifecycleWriteLock = this.lifecycleLock.writeLock();

        this.managerList = new SnapshotableDeque<ChannelManagerContainer>();
        this.serviceList = new SnapshotableDeque<ServiceContainer>();

        this.serviceContainerIndex = new HashMap<IDispatcherChannelService, ServiceContainer>();
        this.channelManagerIndex = new HashMap<IDispatcherChannelManager, ChannelManagerContainer>();

        this.workerPool = new SnapshotableDeque<ChannelWorker>();

        this.propertyBlock = createPropertyBlock();
        this.configurationPropertyBindingRegistry = new ConfigurationPropertyBindingRegistry();

        if((id != null) && (!id.isEmpty()))
        {
            this.id = id;
        }
        else
        {
            this.id = "anonym-" + UUID.randomUUID();
        }

        this.executorService = Executors.newCachedThreadPool();

        this.dispatcherGuardian = new DispatcherGuardian(this);
        this.dispatcherGuardian.start();

        this.spooledChannelWorkerScheduler = new SpooledChannelWorkerScheduler(this);
        this.spooledChannelWorkerScheduler.start();

        this.activated = true;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> void sendMessage(final String channelId, final T message)
    {
        this.lifecycleReadLock.lock();
        try
        {
            if(!this.activated)
            {
                return;
            }

            ChannelImpl channel = null;
            this.channelIndexReadLock.lock();
            try
            {
                channel = this.channelIndex.get(channelId);
            }
            finally
            {
                this.channelIndexReadLock.unlock();
            }
            if(channel == null)
            {
                throw new ChannelNotFoundException(channelId);
            }

            channel.sendMessage(message);
        }
        finally
        {
            this.lifecycleReadLock.unlock();
        }
    }

    @Override
    public List<String> getChannelIdList()
    {
        final List<String> channelIdList = new ArrayList<>();
        this.channelIndexReadLock.lock();
        try
        {
            channelIdList.addAll(this.channelIndex.keySet());
        }
        finally
        {
            this.channelIndexReadLock.unlock();
        }
        return Collections.unmodifiableList(channelIdList);
    }

    @Override
    public IDispatcherChannel<?> getChannel(final String channelId)
    {
        this.channelIndexReadLock.lock();
        try
        {
            return this.channelIndex.get(channelId);
        }
        finally
        {
            this.channelIndexReadLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IDispatcherChannel<T> getTypedChannel(final String channelId, final Class<T> messageType)
    {
        this.channelIndexReadLock.lock();
        try
        {
            return (IDispatcherChannel<T>) this.channelIndex.get(channelId);
        }
        finally
        {
            this.channelIndexReadLock.unlock();
        }
    }

    protected void registerTimeOut(final ChannelImpl<?> channel, final TaskContainer taskContainer)
    {
        this.dispatcherGuardian.registerTimeOut(channel, taskContainer);
    }

    protected void unregisterTimeOut(final ChannelImpl<?> channel, final TaskContainer taskContainer)
    {
        this.dispatcherGuardian.unregisterTimeOut(channel, taskContainer);
    }

    @Override
    public void shutdown()
    {
        this.lifecycleWriteLock.lock();

        try
        {
            try (DequeSnapshot<ChannelManagerContainer> managerSnaphot = this.managerList.createSnapshotPoll())
            {
                for (final ChannelManagerContainer managerContainer : managerSnaphot)
                {
                    try
                    {
                        this.unregisterChannelManager(managerContainer);
                    }
                    catch (final Exception e)
                    {
                        logError("Exception on unregister channel manager", e);
                    }
                    catch (final Error e)
                    {
                        logError("Exception on unregister channel manager", e);
                    }
                }
            }

            this.channelIndexReadLock.lock();
            try
            {
                for (final Entry<String, ChannelImpl<?>> entry : this.channelIndex.entrySet())
                {
                    try
                    {
                        entry.getValue().dispose();
                    }
                    catch (final Exception e)
                    {
                        logError("dispose channel on dispatcher shutdown", e);
                    }
                }
            }
            finally
            {
                this.channelIndexReadLock.unlock();
            }

            this.channelIndexWriteLock.lock();
            try
            {
                this.channelIndex.clear();
            }
            finally
            {
                this.channelIndexWriteLock.unlock();
            }

            try
            {
                this.dispatcherGuardian.stopGuardian();
            }
            catch (final Exception e) { }

            try
            {
                this.spooledChannelWorkerScheduler.stopScheduler();
            }
            catch (final Exception e) { }

            try (DequeSnapshot<ChannelWorker> snapshot = this.workerPool.createSnapshotPoll())
            {
                for (final ChannelWorker worker : snapshot)
                {
                    try
                    {
                        worker.stopWorker();
                    }
                    catch (final Exception e) { }
                }
            }
        }
        finally
        {
            this.lifecycleWriteLock.unlock();
        }

        try
        {
            this.configurationPropertyBindingRegistry.clear();
        }
        catch (final Exception e) { }

        ((MessageDispatcherManagerImpl) MessageDispatcherManagerImpl.get()).remove(this.id);

        this.stopped = true;
    }

    @Override
    public void registerChannelManager(final IDispatcherChannelManager channelManager)
    {
        final ChannelManagerPolicy channelManagerPolicy = new ChannelManagerPolicy();
        channelManager.configureChannelManagerPolicy(channelManagerPolicy);

        if(channelManagerPolicy.getConfigurationSet().isEmpty())
        {
            return;
        }

        ChannelManagerContainer channelManagerContainer = null;
        this.lifecycleReadLock.lock();
        try
        {
            List<ComponentBindingSetup.BoundedByChannelId> boundByIdList = null;
            List<ComponentBindingSetup.BoundedByChannelConfiguration> boundedByChannelConfigurationList = null;

            ComponentBindingSetup.BoundedByChannelId boundedById;
            ComponentBindingSetup.BoundedByChannelConfiguration boundedByChannelConfiguration;
            for (final ComponentBindingSetup config : channelManagerPolicy.getConfigurationSet())
            {
                if(config instanceof ComponentBindingSetup.BoundedByChannelId)
                {
                    boundedById = (ComponentBindingSetup.BoundedByChannelId) config;
                    if((boundedById.getDispatcherId() != null) && (!boundedById.getDispatcherId().equals(this.id)))
                    {
                        continue;
                    }
                    if(boundByIdList == null)
                    {
                        boundByIdList = new ArrayList<ComponentBindingSetup.BoundedByChannelId>();
                    }
                    boundByIdList.add(boundedById.copy());
                }
                if(config instanceof ComponentBindingSetup.BoundedByChannelConfiguration)
                {
                    boundedByChannelConfiguration = (ComponentBindingSetup.BoundedByChannelConfiguration) config;
                    if
                    (
                            (boundedByChannelConfiguration.getDispatcherId() != null) &&
                            (!boundedByChannelConfiguration.getDispatcherId().isEmpty()) &&
                            (!boundedByChannelConfiguration.getDispatcherId().equals(this.id))
                    )
                    {
                        continue;
                    }
                    if(boundedByChannelConfigurationList == null)
                    {
                        boundedByChannelConfigurationList = new ArrayList<ComponentBindingSetup.BoundedByChannelConfiguration>();
                    }
                    boundedByChannelConfigurationList.add(boundedByChannelConfiguration.copy());
                }
            }

            if
            (
                    ((boundByIdList == null) || boundByIdList.isEmpty()) &&
                    ((boundedByChannelConfigurationList == null) || boundedByChannelConfigurationList.isEmpty())
            )
            {
                return;
            }

            // TODO sameObject
            channelManagerContainer = this.channelManagerIndex.get(channelManager);

            if(channelManagerContainer == null)
            {
                channelManagerContainer = new ChannelManagerContainer
                        (
                                this, channelManager,
                                boundByIdList,
                                boundedByChannelConfigurationList
                        );

                this.channelManagerIndex.put(channelManager, channelManagerContainer);
                this.managerList.addLast(channelManagerContainer);

                this.configurationPropertyBindingRegistry.register(channelManagerContainer);
            }
        }
        finally
        {
            this.lifecycleReadLock.unlock();
        }
        internRegisterChannelManager(channelManagerContainer);
    }

    private boolean internRegisterChannelManager(final ChannelManagerContainer channelManagerContainer)
    {
        if(channelManagerContainer.isRegistered())
        {
            return false;
        }
        channelManagerContainer.setRegistered(true);

        if
        (
                (
                        (channelManagerContainer.getBoundByIdList() == null) ||
                        channelManagerContainer.getBoundByIdList().isEmpty()
                ) &&
                (
                        (channelManagerContainer.getBoundedByChannelConfigurationList() == null) ||
                        channelManagerContainer.getBoundedByChannelConfigurationList().isEmpty()
                )
        )
        {
            return false;
        }

        ChannelImpl<?> channel = null;

        boolean managerInUse = false;
        final ChannelBindingModifyFlags modifyFlags = new ChannelBindingModifyFlags();

        if(channelManagerContainer.getBoundByIdList() != null)
        {
            for (final BoundedByChannelId boundedChannelId : channelManagerContainer.getBoundByIdList())
            {
                if(boundedChannelId.getChannelId() == null)
                {
                    continue;
                }
                if(boundedChannelId.getChannelId().isEmpty())
                {
                    continue;
                }
                if
                (
                        (boundedChannelId.getDispatcherId() != null) &&
                        (!boundedChannelId.getDispatcherId().isEmpty()) &&
                        (!boundedChannelId.getDispatcherId().equals(this.id))
                )
                {
                    continue;
                }

                this.channelIndexReadLock.lock();
                try
                {
                    channel = this.channelIndex.get(boundedChannelId.getChannelId());
                }
                finally
                {
                    this.channelIndexReadLock.unlock();
                }

                if(channel != null)
                {
                    // set in existing channel
                    channel.setManager(channelManagerContainer);
                }
                else if((channel == null) && boundedChannelId.isChannelMaster()) // autocreate
                {
                    // create a new channel and set into
                    this.channelIndexWriteLock.lock();
                    try
                    {
                        channel = this.channelIndex.get(boundedChannelId.getChannelId());

                        if(channel == null)
                        {
                            final String name = (boundedChannelId.getName() == null || boundedChannelId.getName().isEmpty()) ?
                                    channelManagerContainer.getChannelManager().getClass().getSimpleName() :
                                    boundedChannelId.getName();

                            channel = new ChannelImpl<Object>(boundedChannelId.getChannelId(), this, null, null, name, null, null);
                            this.channelIndex.put(boundedChannelId.getChannelId(), channel);

                            try (DequeSnapshot<ServiceContainer> servicesSnapshot = this.serviceList.createSnapshot())
                            {

                                for (final ServiceContainer serviceContainer : servicesSnapshot)
                                {
                                    modifyFlags.reset();

                                    channel.checkForService(serviceContainer, modifyFlags);
                                }
                            }
                        }
                    }
                    finally
                    {
                        this.channelIndexWriteLock.unlock();
                    }

                    managerInUse = true;
                    channel.setManager(channelManagerContainer);
                } // end autocreate

            }

            if(channelManagerContainer.getBoundedByChannelConfigurationList() != null)
            {
                for (final BoundedByChannelConfiguration boundedByChannelConfiguration : channelManagerContainer.getBoundedByChannelConfigurationList())
                {
                    if(boundedByChannelConfiguration.getLdapFilter() == null)
                    {
                        continue;
                    }

                    try
                    {
                        this.channelIndexReadLock.lock();
                        try
                        {
                            for (final Entry<String, ChannelImpl<?>> entry : this.channelIndex.entrySet())
                            {
                                modifyFlags.reset();
                                entry.getValue().checkForChannelManager(channelManagerContainer, modifyFlags);
                                if(modifyFlags.isRootSet() || modifyFlags.isSubSet())
                                {
                                    managerInUse = true;
                                }
                            }
                        }
                        finally
                        {
                            this.channelIndexReadLock.unlock();
                        }
                    }
                    catch (final Exception e)
                    {
                        logError("check channel binding for manager by configuration filter", e);
                    }
                }
            }
        }

        return managerInUse;

    }

    @Override
    public void unregisterChannelManager(final IDispatcherChannelManager channelManager)
    {
        ChannelManagerContainer managerContainer = null;
        this.lifecycleReadLock.lock();
        try
        {

            managerContainer = this.channelManagerIndex.get(channelManager);

            if(managerContainer == null)
            {
                return;
            }

            try (DequeSnapshot<ChannelManagerContainer> managerSnapshot = this.managerList.createSnapshot())
            {
                DequeNode<ChannelManagerContainer> containerNode = null;
                while ((containerNode = managerSnapshot.getLinkedNode(managerContainer)) != null)
                {
                    if(containerNode != null)
                    {
                        containerNode.unlink();
                    }
                }
            }
            this.channelManagerIndex.remove(channelManager);
            this.configurationPropertyBindingRegistry.unregister(managerContainer);

        }
        finally
        {
            this.lifecycleReadLock.unlock();
        }

        this.unregisterChannelManager(managerContainer);
    }

    private boolean unregisterChannelManager(final ChannelManagerContainer channelManagerContainer)
    {
        boolean registered = false;
        List<ChannelImpl<?>> registeredOnChannelList = null;
        List<ChannelImpl<?>> channelRemoveList = null;

        this.channelIndexReadLock.lock();
        try
        {
            for (final Entry<String, ChannelImpl<?>> entry : this.channelIndex.entrySet())
            {
                if(entry.getValue().unsetChannelManager(channelManagerContainer, true))
                {
                    if(registeredOnChannelList == null)
                    {
                        registeredOnChannelList = new ArrayList<ChannelImpl<?>>();
                    }
                    registered = true;
                    registeredOnChannelList.add(entry.getValue());
                }

                if(!entry.getValue().isMastered())
                {
                    if(channelRemoveList == null)
                    {
                        channelRemoveList = new ArrayList<ChannelImpl<?>>();
                    }
                    channelRemoveList.add(entry.getValue());
                }
            }
        }
        finally
        {
            this.channelIndexReadLock.unlock();
        }

        if(channelRemoveList != null)
        {
            this.channelIndexWriteLock.lock();
            try
            {
                for (final ChannelImpl<?> channel : channelRemoveList)
                {
                    try
                    {
                        channel.dispose();
                    }
                    catch (final Exception e)
                    {
                        logError("dispose channel after remove all manager", e);
                    }

                    this.channelIndex.remove(channel.getId());
                }
            }
            finally
            {
                this.channelIndexWriteLock.unlock();
            }
        }

        return registered;
    }

    private void checkChannelManagerForChannel(final ChannelImpl<?> channel)
    {
        if(channel.getManagerSize() > 0)
        {
            return;
        }

        if(channel instanceof ISubChannel)
        {
            return;
        }

        this.channelIndexWriteLock.lock();
        try
        {
            try
            {
                channel.dispose();
            }
            catch (final Exception e)
            {
                logError("dispose channel after removed all manager", e);
            }

            this.channelIndex.remove(channel.getId());
        }
        finally
        {
            this.channelIndexWriteLock.unlock();
        }
    }

    @Override public void registerChannelService(final IDispatcherChannelService channelService)
    {
        final ChannelServicePolicy channelServicePolicy = new ChannelServicePolicy();
        channelService.configureChannelServicePolicy(channelServicePolicy);

        if(channelServicePolicy.getConfigurationSet().isEmpty())
        {
            return;
        }

        ServiceContainer serviceContainer = null;

        this.lifecycleReadLock.lock();
        try
        {
            List<ComponentBindingSetup.BoundedByChannelId> boundByIdList = null;
            List<ComponentBindingSetup.BoundedByChannelConfiguration> boundedByChannelConfigurationList = null;
            List<ComponentBindingSetup.ChannelServiceConfiguration> serviceBehaviorConfigurationList = null;

            ComponentBindingSetup.BoundedByChannelId boundedById;
            ComponentBindingSetup.BoundedByChannelConfiguration boundedByChannelConfiguration;
            ComponentBindingSetup.ChannelServiceConfiguration serviceConfiguration;
            for (final ComponentBindingSetup config : channelServicePolicy.getConfigurationSet())
            {
                if(config instanceof ComponentBindingSetup.BoundedByChannelId)
                {
                    boundedById = (ComponentBindingSetup.BoundedByChannelId) config;
                    if
                    (
                            (boundedById.getDispatcherId() != null) &&
                            (!boundedById.getDispatcherId().isEmpty()) &&
                            (!boundedById.getDispatcherId().equals(this.id))
                    )
                    {
                        continue;
                    }
                    if(boundByIdList == null)
                    {
                        boundByIdList = new ArrayList<ComponentBindingSetup.BoundedByChannelId>();
                    }
                    boundByIdList.add(boundedById.copy());
                }
                if(config instanceof ComponentBindingSetup.BoundedByChannelConfiguration)
                {
                    boundedByChannelConfiguration = (ComponentBindingSetup.BoundedByChannelConfiguration) config;
                    if
                    (
                            (boundedByChannelConfiguration.getDispatcherId() != null) &&
                            (!boundedByChannelConfiguration.getDispatcherId().isEmpty()) &&
                            (!boundedByChannelConfiguration.getDispatcherId().equals(this.id))
                    )
                    {
                        continue;
                    }
                    if(boundedByChannelConfigurationList == null)
                    {
                        boundedByChannelConfigurationList = new ArrayList<ComponentBindingSetup.BoundedByChannelConfiguration>();
                    }
                    boundedByChannelConfigurationList.add(boundedByChannelConfiguration.copy());
                }
                if(config instanceof ComponentBindingSetup.ChannelServiceConfiguration)
                {
                    serviceConfiguration = (ComponentBindingSetup.ChannelServiceConfiguration) config;
                    if(serviceBehaviorConfigurationList == null)
                    {
                        serviceBehaviorConfigurationList = new ArrayList<ComponentBindingSetup.ChannelServiceConfiguration>();
                    }
                    serviceBehaviorConfigurationList.add(serviceConfiguration.copy());
                }
            }

            if
            (
                    ((boundByIdList == null) || boundByIdList.isEmpty()) &&
                    ((boundedByChannelConfigurationList == null) || boundedByChannelConfigurationList.isEmpty())
            )
            {
                return;
            }

            // TODO sameObject
            serviceContainer = this.serviceContainerIndex.get(channelService);

            if(serviceContainer == null)
            {
                serviceContainer = new ServiceContainer
                        (
                                this,
                                boundByIdList,
                                boundedByChannelConfigurationList,
                                serviceBehaviorConfigurationList
                        );
                serviceContainer.setChannelService(channelService);

                this.serviceList.addLast(serviceContainer);
                this.serviceContainerIndex.put(channelService, serviceContainer);
                this.configurationPropertyBindingRegistry.register(serviceContainer);
            }
            internRegisterChannelService(serviceContainer);

        }
        finally
        {
            this.lifecycleReadLock.unlock();
        }
    }

    private boolean internRegisterChannelService(final ServiceContainer serviceContainer)
    {
        if(serviceContainer.isRegistered())
        {
            return false;
        }

        serviceContainer.setRegistered(true);

        if
        (
                (
                        (serviceContainer.getBoundByIdList() == null) ||
                        serviceContainer.getBoundByIdList().isEmpty()
                ) &&
                (
                        (serviceContainer.getBoundedByChannelConfigurationList() == null) ||
                        serviceContainer.getBoundedByChannelConfigurationList().isEmpty()
                )
        )
        {
            return false;
        }

        final ChannelBindingModifyFlags modifyFlags = new ChannelBindingModifyFlags();

        if(serviceContainer.getBoundByIdList() != null)
        {
            for (final ComponentBindingSetup.BoundedByChannelId boundedByChannelId : serviceContainer.getBoundByIdList())
            {
                if(boundedByChannelId.getChannelId() == null)
                {
                    continue;
                }
                if(boundedByChannelId.getChannelId().isEmpty())
                {
                    continue;
                }
                this.channelIndexReadLock.lock();
                ChannelImpl<?> channel = null;
                try
                {
                    channel = this.channelIndex.get(boundedByChannelId.getChannelId());

                }
                finally
                {
                    this.channelIndexReadLock.unlock();
                }

                if(channel != null)
                {
                    modifyFlags.reset();
                    channel.checkForService(serviceContainer, modifyFlags);
                }
            }
        }
        if(serviceContainer.getBoundedByChannelConfigurationList() != null)
        {
            this.channelIndexReadLock.lock();
            try
            {
                for (final Entry<String, ChannelImpl<?>> entry : this.channelIndex.entrySet())
                {
                    modifyFlags.reset();
                    entry.getValue().checkForService(serviceContainer, modifyFlags);
                }
            }
            finally
            {
                this.channelIndexReadLock.unlock();
            }
        }

        return true;
    }

    @Override
    public void unregisterChannelService(final IDispatcherChannelService channelService)
    {
        ServiceContainer serviceContainer = null;
        this.lifecycleReadLock.lock();
        try
        {
            serviceContainer = this.serviceContainerIndex.get(channelService);
            if(serviceContainer == null)
            {
                return;
            }

            this.serviceContainerIndex.remove(channelService);
            try (DequeSnapshot<ServiceContainer> managerSnapshot = this.serviceList.createSnapshot())
            {
                DequeNode<ServiceContainer> serviceInList = null;
                while ((serviceInList = managerSnapshot.getLinkedNode(serviceContainer)) != null)
                {
                    if(serviceInList != null)
                    {
                        serviceInList.unlink();
                    }
                }
            }

            this.configurationPropertyBindingRegistry.unregister(serviceContainer);

        }
        finally
        {
            this.lifecycleReadLock.unlock();
        }
        this.unregisterChannelService(serviceContainer);

    }

    private boolean unregisterChannelService(final ServiceContainer serviceContainer)
    {
        final boolean registered = false;
        this.channelIndexReadLock.lock();
        try
        {
            for (final Entry<String, ChannelImpl<?>> entry : this.channelIndex.entrySet())
            {
                entry.getValue().unsetService(serviceContainer, true);
            }
        }
        finally
        {
            this.channelIndexReadLock.unlock();
        }

        return registered;
    }

    @Override
    public PropertyBlockImpl createPropertyBlock()
    {
        return new PropertyBlockImpl(this);
    }

    protected boolean addToWorkerPool(final ChannelWorker worker)
    {
        if(!worker.isGo())
        {
            return false;
        }
        if(worker.getMessageChannel() != null)
        {
            return false;
        }
        worker.setSpoolTimeStamp(System.currentTimeMillis());
        this.workerPool.addFirst(worker);

        return true;
    }

    protected ChannelWorker getFromWorkerPool()
    {
        try (DequeSnapshot<ChannelWorker> snapshot = this.workerPool.createSnapshot())
        {
            for (final DequeNode<ChannelWorker> node : snapshot.nodeIterable())
            {
                if(!node.isLinked())
                {
                    continue;
                }
                final ChannelWorker foundWorker = node.getElement();
                node.unlink();

                if(!foundWorker.isGo())
                {
                    continue;
                }
                if(foundWorker.getMessageChannel() != null)
                {
                    continue;
                }
                if(!foundWorker.isAlive())
                {
                    continue;
                }
                return foundWorker;
            }
        }

        return null;
    }

    protected void checkTimeoutWorker()
    {
        final long shutdownTimeStamp = System.currentTimeMillis() - ChannelWorker.DEFAULT_SHUTDOWN_TIME;
        final LinkedList<DequeNode<ChannelWorker>> removeList = new LinkedList<DequeNode<ChannelWorker>>();
        try (DequeSnapshot<ChannelWorker> snapshot = this.workerPool.createSnapshot())
        {
            for (final DequeNode<ChannelWorker> workerNode : snapshot.nodeIterable())
            {
                final ChannelWorker worker = workerNode.getElement();
                try
                {
                    if(worker.getMessageChannel() != null)
                    {
                        removeList.add(workerNode);
                        continue;
                    }
                    if(!worker.isGo())
                    {
                        removeList.add(workerNode);
                        continue;
                    }
                    if(worker.getSpoolTimeStamp() < shutdownTimeStamp)
                    {
                        removeList.add(workerNode);
                        continue;
                    }
                }
                catch (final Exception e) { }
                catch (final Error e) { }
            }
            for (final DequeNode<ChannelWorker> remove : removeList)
            {
                try
                {
                    final ChannelWorker worker = remove.getElement();
                    remove.unlink();
                    worker.stopWorker();
                }
                catch (final Exception e) { this.logError("remove spooled worker", e); }
                catch (final Error e) { this.logError("remove spooled worker", e); }
            }
            removeList.clear();
        }
    }

    protected SpooledChannelWorker scheduleChannelWorker(final ChannelImpl<?> channel, final long wakeUpTime)
    {
        return this.spooledChannelWorkerScheduler.scheduleChannelWorker(channel, wakeUpTime);
    }

    protected void executeOnTaskTimeOut(final IOnTaskTimeout manager, final IDispatcherChannel<?> channel, final IDispatcherChannelTask task, final Object taskState, final ChannelWorker worker)
    {
        try
        {
            this.executorService.submit(new Callable<IDispatcherChannelTask>()
            {
                @Override
                public IDispatcherChannelTask call()
                {
                    try
                    {
                        manager.onTaskTimeout(channel, task, taskState, new Runnable()
                        {

                            @Override
                            public void run()
                            {
                                if(worker.isAlive())
                                {
                                    try
                                    {
                                        worker.interrupt();
                                    }
                                    catch (final Exception e) { }
                                    catch (final Error e) { }
                                }

                            }
                        });
                    }
                    catch (final Exception e) { }
                    return task;
                }
            }).get(7, TimeUnit.SECONDS);
        }
        catch (Exception | Error e) { }
    }

    public void executeOnTaskStopExecuter(final ChannelWorker worker, final IDispatcherChannelTask task)
    {
        this.executorService.execute(new Runnable()
        {
            @Override
            @SuppressWarnings("deprecation")
            public void run()
            {
                if(worker.isAlive())
                {
                    if(task instanceof IOnTaskStop)
                    {
                        long number = 0L;
                        long moreTimeUntilNow = 0L;
                        long moreTime;

                        while (worker.isAlive() && ((moreTime = ((IOnTaskStop) task).requestForMoreLifeTime(number, moreTimeUntilNow, worker.getWorkerWrapper())) > 0))
                        {
                            try
                            {
                                Thread.sleep(moreTime);
                            }
                            catch (final Exception e) { }
                            catch (final Error e) { }
                            number++;
                            moreTimeUntilNow += moreTime;
                        }
                    }

                }

                if(worker.isAlive()) { worker.interrupt(); }
            }
        });
    }

    protected void executeOnChannelDetach(final IOnChannelDetach onChannelDetach, final IDispatcherChannel<?> channel)
    {
        try
        {
            this.executorService.submit(new Callable<IDispatcherChannel<?>>()
            {
                @Override
                public IDispatcherChannel<?> call()
                {
                    try
                    {
                        onChannelDetach.onChannelDetach(channel);
                    }
                    catch (final Exception e) { }
                    return channel;
                }
            }).get(3, TimeUnit.SECONDS);
        }
        catch (final Exception e) { }
    }

    protected Future<IOnMessageStoreResult> createFutureOfScheduleResult(final PublishMessageResultImpl scheduleResult)
    {
        final Callable<IOnMessageStoreResult> call = new Callable<IOnMessageStoreResult>()
        {
            @Override
            public IOnMessageStoreResult call() throws Exception
            {
                scheduleResult.waitForProcessingIsFinished();
                return scheduleResult;
            }

        };
        return this.executorService.submit(call);
    }

    protected void onConfigurationModify(final ChannelImpl<?> channel, final String... attributes)
    {
        final ChannelBindingModifyFlags modifyFlags = new ChannelBindingModifyFlags();

        try
        {
            final Set<ChannelManagerContainer> matchedManagerContainer = this.configurationPropertyBindingRegistry.getManagerContainer(attributes);
            if(matchedManagerContainer != null)
            {
                // TODO managerListReadLock.lock(); // TODO required ?

                for (final ChannelManagerContainer managerContainer : matchedManagerContainer)
                {
                    modifyFlags.reset();

                    try
                    {
                        channel.checkForChannelManager(managerContainer, modifyFlags);
                    }
                    catch (final Exception e)
                    {
                        logError("check channel binding for manager by configuration filter on channel configuration modify", e);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            logError("check channel binding for manager by configuration filter on channel configuration modify", e);
        }

        try
        {
            final Set<ServiceContainer> matchedServiceContainer = this.configurationPropertyBindingRegistry.getServiceContainer(attributes);
            if(matchedServiceContainer != null)
            {
                // TODO serviceListReadLock.lock(); // TODO required?
                for (final ServiceContainer serviceContainer : matchedServiceContainer)
                {
                    modifyFlags.reset();
                    try
                    {
                        channel.checkForService(serviceContainer, modifyFlags);
                    }
                    catch (final Exception e)
                    {
                        logError("check channel binding for services by configuration filter on channel configuration modify", e);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            logError("check channel binding for services by configuration filter on channel configuration modify", e);
        }

        if(channel.getManagerSize() < 1)
        {
            checkChannelManagerForChannel(channel);
        }
    }

    @Override
    public IPropertyBlock getPropertyBlock()
    {
        return this.propertyBlock;
    }

    protected void logError(final String message, final Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    private class ChannelManagerPolicy implements IChannelManagerPolicy
    {
        private final Set<ComponentBindingSetup> configurationSet = new HashSet<ComponentBindingSetup>();

        @Override
        public IChannelManagerPolicy addConfigurationDetail(final ComponentBindingSetup configuration)
        {
            Objects.requireNonNull(configuration);
            this.configurationSet.add(configuration);

            return this;
        }

        private Set<ComponentBindingSetup> getConfigurationSet()
        {
            return this.configurationSet;
        }

    }

    private class ChannelServicePolicy implements IChannelServicePolicy
    {
        private final Set<ComponentBindingSetup> configurationSet = new HashSet<ComponentBindingSetup>();

        @Override
        public IChannelServicePolicy addConfigurationDetail(final ComponentBindingSetup configuration)
        {
            Objects.requireNonNull(configuration);
            this.configurationSet.add(configuration);
            return this;
        }

        private Set<ComponentBindingSetup> getConfigurationSet()
        {
            return this.configurationSet;
        }

    }

    protected boolean isStopped()
    {
        return this.stopped;
    }
}
