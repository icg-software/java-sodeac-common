/*******************************************************************************
 * Copyright (c) 2020, 2021 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel.IDispatcherChannelReference;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcher;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcherManager;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.misc.TaskDoneNotifier;

public class MessageDispatcherChannelSetup
{
    private MessageDispatcherChannelSetup()
    {
        super();
    }
    
    public static MessageDispatcherChannelBuilder create()
    {
        return new MessageDispatcherChannelBuilder();
    }
    
    public static class MessageDispatcherChannelBuilder
    {
        private MessageDispatcherChannelBuilder()
        {
            super();
            this.featureList = new ArrayList<MessageDispatcherChannelSetup.IPreparedChannelFeature>();
        }
        
        private List<IPreparedChannelFeature> featureList = null;
        
        public PreparedMessageDispatcherChannelBuilder preparedBuilder()
        {
            return new PreparedMessageDispatcherChannelBuilder(this.featureList);
        }
        
        public MessageDispatcherChannelBuilder addFeature(final IChannelFeature channelFeature)
        {
            this.featureList.add((IPreparedChannelFeature) channelFeature);
            return this;
        }
        
        public static class PreparedMessageDispatcherChannelBuilder
        {
            private PreparedMessageDispatcherChannelBuilder(final List<IPreparedChannelFeature> featureList)
            {
                super();
                this.featureList = new ArrayList<>(featureList);
            }
            
            private List<IPreparedChannelFeature> featureList = null;
            
            public InDispatcher inDispatcher(final Supplier<IMessageDispatcher> dispatcherSupplier)
            {
                Objects.requireNonNull(dispatcherSupplier, "dispatcher supplier is null");
                return new InDispatcher(dispatcherSupplier);
            }
            
            public InDispatcher inDispatcher(final IMessageDispatcher dispatcher)
            {
                Objects.requireNonNull(dispatcher, "dispatcher is null");
                return new InDispatcher(new ConplierBean<IMessageDispatcher>(dispatcher));
            }
            
            public InDispatcher inManagedDispatcher(final String dispatcherId)
            {
                Objects.requireNonNull(dispatcherId, "dispatcher id is null");
                if (dispatcherId.isEmpty())
                {
                    throw new IllegalStateException("dispatcher is is empty");
                }
                return new InDispatcher(new ConplierBean<IMessageDispatcher>(IMessageDispatcherManager.get().getOrCreateDispatcher(dispatcherId)));
            }
            
            public class InDispatcher
            {
                private InDispatcher(final Supplier<IMessageDispatcher> dispatcherSupplier)
                {
                    super();
                    this.dispatcherSupplier = dispatcherSupplier;
                }
                
                private Supplier<IMessageDispatcher> dispatcherSupplier = null;
                
                public WithName underTheName(String channelName)
                {
                    if (channelName == null)
                    {
                        channelName = "";
                    }
                    return new WithName(channelName);
                }
                
                public IDispatcherChannelReference buildChannelWithId(final String channelId)
                {
                    return new WithName("").buildChannelWithId(channelId);
                }
                
                public class WithName
                {
                    public WithName(final String name)
                    {
                        super();
                        this.name = name;
                    }
                    
                    private String name = null;
                    
                    public IDispatcherChannelReference buildChannelWithId(final String channelId)
                    {
                        IMessageDispatcher dispatcher = InDispatcher.this.dispatcherSupplier.get();
                        Objects.requireNonNull(dispatcher, "dispatcher could not be supplied");
                        if (dispatcher.getChannel(channelId) != null)
                        {
                            throw new IllegalStateException("channel " + channelId + " already exists");
                        }
                        TaskDoneNotifier setupDone = new TaskDoneNotifier();
                        ChannelMasterManager channelMasterManager = new ChannelMasterManager(channelId, this.name, dispatcher, PreparedMessageDispatcherChannelBuilder.this.featureList, setupDone);
                        dispatcher.registerChannelManager(channelMasterManager);
                        try
                        {
                            setupDone.await(7, TimeUnit.SECONDS);
                        }
                        catch (final Exception e) { }
                        return channelMasterManager;
                    }
                }
                
            }
        }
    }
    
    public interface IChannelFeature
    {
    
    }
    
    public interface IPreparedChannelFeature extends IChannelFeature
    {
        void applyToChannel(IDispatcherChannel<?> channel);
    }
    
    @SuppressWarnings("rawtypes")
    protected static class ChannelMasterManager implements IDispatcherChannelManager, IOnChannelAttach, IDispatcherChannelReference
    {
        private final String id;
        private final String name;
        private final IMessageDispatcher dispatcher;
        private List<IPreparedChannelFeature> featureList;
        private TaskDoneNotifier taskDoneNotifier;
        
        protected ChannelMasterManager(final String id, final String name, final IMessageDispatcher dispatcher, final List<IPreparedChannelFeature> featureList, final TaskDoneNotifier taskDoneNotifier)
        {
            super();
            this.id = id;
            this.name = name;
            this.dispatcher = dispatcher;
            this.featureList = featureList;
            this.taskDoneNotifier = taskDoneNotifier;
        }
        
        @Override
        public void configureChannelManagerPolicy(final IChannelManagerPolicy configurationPolicy)
        {
            configurationPolicy.addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelId(this.id).setChannelMaster(true).setName(this.name));
        }
        
        @Override
        public void close() throws IOException
        {
            this.dispatcher.unregisterChannelManager(this);
        }
        
        @Override
        public void onChannelAttach(final IDispatcherChannel channel)
        {
            for (final IPreparedChannelFeature channelFeature : this.featureList)
            {
                try
                {
                    channelFeature.applyToChannel(channel);
                }
                catch (final Exception e) { }
                catch (final Error e) { }
            }
            
            this.taskDoneNotifier.setTaskDone();
            this.taskDoneNotifier = null;
            this.featureList = null;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> IDispatcherChannel<T> getChannel(final Class<T> type)
        {
            return (IDispatcherChannel<T>) this.dispatcher.getChannel(this.id);
        }
    }
    
    public interface MessageConsumeHelper<T, H>
    {
        boolean isFirstMessage();
        
        boolean isLastMessage();
        
        int messageSize();
        
        Collection<IMessage<T>> getAllMessages();
        
        IMessage<T> getMessage();
        
        IDispatcherChannel<T> getChannel();
        
        H getHelper(Supplier<H> supplierIfNotExist);
        
        void heartbeat();
        
        boolean isInTimeout();
        
        boolean isProcessed();
        
        void setProcessed();
        
    }
}
