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
package org.sodeac.common.message.dispatcher.components;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemManager;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemService;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTaskContext;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelSignal;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemoveSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnTaskTimeout;
import org.sodeac.common.message.dispatcher.components.ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter.ConsumableState;
import org.sodeac.common.message.dispatcher.components.ConsumeMessagesPlannerManager.KeepMessagesMode;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.IPoolController;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.SpecialErrorHandlerDefinition;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup.MessageConsumeHelper;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.misc.RuntimeWrappedException;
import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.xuri.ldapfilter.FilterBuilder;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;

@Component(service = { IDispatcherChannelSystemManager.class, IDispatcherChannelSystemService.class }, property = { "type=consume-messages", "role=consumer" })
public class ConsumeMessagesConsumerManager implements IDispatcherChannelSystemManager, IOnChannelAttach<Object>, IOnTaskTimeout<Object>, IOnMessageStoreSnapshot<Object>, IOnMessageRemoveSnapshot<Object>, IOnChannelSignal<Object>, IDispatcherChannelSystemService<Object>
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    public static final IFilterItem MATCH_FILTER = FilterBuilder.andLinker().criteriaWithName(ConsumeMessagesConsumerManager.class.getCanonicalName()).eq(Boolean.TRUE.toString()).build();
    public static final String MATCH_NAME = "Consume Messages Consumer Manager";
    public static final String SERVICE_NAME = "Consume Messages Consumer Service";
    public static final String SERVICE_ID = ConsumeMessagesConsumerManager.class.getCanonicalName() + ".Service";
    public static final String SIGNAL_CONSUME = ConsumeMessagesConsumerManager.class.getCanonicalName() + ".Signal.Consume";
    
    @Override
    public void configureChannelManagerPolicy(final IChannelManagerPolicy componentBindingPolicy)
    {
        componentBindingPolicy.addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelConfiguration(MATCH_FILTER).setName(MATCH_NAME));
    }
    
    @Override
    public void configureChannelServicePolicy(final IChannelServicePolicy componentBindingPolicy)
    {
        componentBindingPolicy
            .addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelConfiguration(MATCH_FILTER).setName(SERVICE_NAME))
            .addConfigurationDetail(new ComponentBindingSetup.ChannelServiceConfiguration(SERVICE_ID).setName(SERVICE_NAME)
                                                                                                     .setPeriodicRepetitionIntervalMS(777 * 1080)
                                                                                                     .setStartDelayInMS(77));
    }
    
    @Override
    public void onChannelAttach(final IDispatcherChannel<Object> channel)
    {
        channel.getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class, () -> new ConsumeMessagesConsumerManagerAdapter(channel));
    }
    
    @Override
    public void run(final IDispatcherChannelTaskContext<Object> taskContext) throws Exception
    {
        ConsumableState consumableState = taskContext.getChannel().getStateAdapter(ConsumableState.class);
        if (consumableState == null)
        {
            return;
        }
        
        if ((consumableState.getPoolId() != null && consumableState.getConsumeMessageId() != null))
        {
            ConsumeMessagesConsumerManagerAdapter consumerAdapter = taskContext.getChannel().getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
            if (consumerAdapter != null)
            {
                consumerAdapter.removeConsumeMessageFlag(consumableState.getPoolId(), consumableState.getConsumeMessageId());
            }
        }
        
        // TODO Recycle-Concept for consumableState
        
        taskContext.setTaskState(consumableState);
        
        long now = System.currentTimeMillis();
        
        try
        {
            
            MessageConsumeHelperImpl messageConsumeHelper = new MessageConsumeHelperImpl(); // TODO Reuse ?
            messageConsumeHelper.firstMessage = false;
            messageConsumeHelper.lastMessage = false;
            messageConsumeHelper.messageList = consumableState.getConsumableList();
            messageConsumeHelper.message = null;
            messageConsumeHelper.channel = taskContext.getChannel();
            messageConsumeHelper.helper = null;
            messageConsumeHelper.taskContext = taskContext;
            messageConsumeHelper.poolId = consumableState.getPoolId();
            
            if (consumableState.getConsumerRule().isKeepMessages())
            {
                messageConsumeHelper.keepMessageMode = consumableState.getKeepMessagesMode();
            }
            
            consumableState.setMessageConsumeHelperImpl(messageConsumeHelper);
            
            if (messageConsumeHelper.messageList.isEmpty())
            {
                try
                {
                    taskContext.heartbeat();
                    consumableState.getConsumerRule().getMessageConsumer().accept(null, messageConsumeHelper);
                }
                catch (Exception | Error e)
                {
                    try
                    {
                        handleError(e, consumableState.getConsumerRule(), messageConsumeHelper);
                    }
                    catch (Exception | Error e2) { }
                }
                
                if (taskContext.getTaskControl().isInTimeout())
                {
                }
            }
            else
            {
                int index = 0;
                int lastIndex = messageConsumeHelper.messageList.size() - 1;
                
                boolean keepMessagesUpdateState = false;
                
                try
                {
                    for (final IMessage<Object> message : messageConsumeHelper.messageList)
                    {
                        
                        messageConsumeHelper.firstMessage = index == 0;
                        messageConsumeHelper.lastMessage = index == lastIndex;
                        messageConsumeHelper.message = message;
                        
                        boolean checkDone = false;
                        
                        try
                        {
                            taskContext.heartbeat();
                            
                            if (consumableState.getConsumerRule().isKeepMessages())
                            {
                                if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesConsumed)
                                {
                                    checkDone = !message.isConsumed();
                                }
                                else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesProcessed)
                                {
                                    checkDone = !message.isProcessed();
                                }
                                else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesConsumedByRule)
                                {
                                    checkDone = !MessageConsumeHelperImpl.isConsumedByConfig(messageConsumeHelper.keepMessageMode, message, messageConsumeHelper.poolId);
                                }
                                else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesProcessedByRule)
                                {
                                    checkDone = !MessageConsumeHelperImpl.isProcessedByConfig(messageConsumeHelper.keepMessageMode, message, messageConsumeHelper.poolId);
                                }
                            }
                            
                            try
                            {
                                consumableState.getConsumerRule().getMessageConsumer().accept(message, messageConsumeHelper);
                            }
                            finally
                            {
                                
                                try
                                {
                                    if (consumableState.getConsumerRule().isKeepMessages())
                                    {
                                        if (checkDone)
                                        {
                                            if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesConsumed)
                                            {
                                                keepMessagesUpdateState = true;
                                            }
                                            else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesProcessed)
                                            {
                                                if (message.isProcessed())
                                                {
                                                    keepMessagesUpdateState = true;
                                                }
                                            }
                                            else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesConsumedByRule)
                                            {
                                                keepMessagesUpdateState = true;
                                            }
                                            else if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesProcessedByRule)
                                            {
                                                if (MessageConsumeHelperImpl.isProcessedByConfig(messageConsumeHelper.keepMessageMode, message, messageConsumeHelper.poolId))
                                                {
                                                    keepMessagesUpdateState = true;
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        message.removeFromChannel();
                                    }
                                }
                                catch (Exception | Error e) { }
                                
                                try
                                {
                                    message.setConsumed(true);
                                    if (messageConsumeHelper.keepMessageMode == KeepMessagesMode.MessagesConsumedByRule)
                                    {
                                        MessageConsumeHelperImpl.setConsumedByConfig(messageConsumeHelper.keepMessageMode, message, messageConsumeHelper.poolId);
                                    }
                                }
                                catch (Exception | Error e) { }
                            }
                        }
                        catch (Exception | Error e)
                        {
                            try
                            {
                                handleError(e, consumableState.getConsumerRule(), messageConsumeHelper);
                            }
                            catch (Exception | Error e2) { }
                        }
                        
                        index++;
                        
                        if (taskContext.getTaskControl().isInTimeout())
                        {
                            return;
                        }
                    }
                }
                finally
                {
                    if (keepMessagesUpdateState)
                    {
                        ConsumeMessagesConsumerManagerAdapter consumerAdapter = taskContext.getChannel().getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
                        if (consumerAdapter != null)
                        {
                            consumerAdapter.updateKeepMessagesState(messageConsumeHelper.poolId);
                        }
                    }
                }
                
                try
                {
                    messageConsumeHelper.messageList.clear();
                }
                catch (Exception | Error e) { }
            }
            
            // TODO clear list && cache messageConsumeHandler
        }
        finally
        {
            if (!taskContext.getTaskControl().isInTimeout())
            {
                ConsumeMessagesConsumerManagerAdapter consumerAdapter = taskContext.getChannel().getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
                if (consumerAdapter != null)
                {
                    consumerAdapter.setConsumeTimestamp(now, consumableState.getConsumerRule().getGroupMembers());
                    consumerAdapter.removeRemovedMessagesFromMonitoringPools(); // reschedule planner && signal if necessary
                }
                
                taskContext.getChannel().removeStateAdapter(ConsumableState.class);
                
                consumableState.dispose();
                
            }
        }
        
    }
    
    private static void handleError(Throwable throwable, final ConsumerRule consumerRule, final MessageConsumeHelperImpl messageConsumeHelper)
    {
        if (throwable instanceof RuntimeWrappedException)
        {
            throwable = throwable.getCause();
        }
        
        for (final SpecialErrorHandlerDefinition specialErrorHandlerDefinition : consumerRule.getSpecialErrorHandler())
        {
            if (throwable.getClass() == specialErrorHandlerDefinition.getType() && (specialErrorHandlerDefinition.getHandler() != null))
            {
                specialErrorHandlerDefinition.getHandler().accept(throwable, messageConsumeHelper);
                return;
            }
        }
        if (consumerRule.getDefaultErrorHandler() != null)
        {
            consumerRule.getDefaultErrorHandler().accept(throwable, messageConsumeHelper);
        }
    }
    
    @Override
    public void onTaskTimeout(final IDispatcherChannel<Object> channel, final IDispatcherChannelTask<Object> task, final Object taskState, final Runnable interrupter)
    {
        ConsumableState consumableState = (ConsumableState) taskState;
        if (consumableState == null)
        {
            return;
        }
        
        if (consumableState.getConsumerRule().getTimeOutHandler() != null)
        {
            try
            {
                consumableState.getConsumerRule().getTimeOutHandler().accept(consumableState
                                                                                 .getMessageConsumeHelperImpl()
                                                                                 .getMessage(), consumableState
                                                                                 .getMessageConsumeHelperImpl()
                );
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
            catch (final Error e) { }
        }
        
        ConsumeMessagesConsumerManagerAdapter consumerAdapter = channel.getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
        if (consumerAdapter != null)
        {
            consumerAdapter.setConsumeTimestamp(System.currentTimeMillis(), consumableState.getConsumerRule().getGroupMembers());
            consumerAdapter.removeRemovedMessagesFromMonitoringPools(); // reschedule planner && signal if necessary
        }
        consumableState.dispose();
    }
    
    @Override
    public void onMessageRemoveSnapshot(final DequeSnapshot<IMessage<Object>> messageRemoveSnapshot)
    {
        ConsumeMessagesConsumerManagerAdapter consumerAdapter = messageRemoveSnapshot.getFirstElement().getChannel().getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
        if (consumerAdapter == null)
        {
            return;
        }
        consumerAdapter.removeRemovedMessagesFromMonitoringPools();
    }
    
    @Override
    public void onMessageStoreSnapshot(final DequeSnapshot<IMessage<Object>> messageStoreSnapshot)
    {
        ConsumeMessagesConsumerManagerAdapter consumerAdapter = messageStoreSnapshot.getFirstElement().getChannel().getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
        if (consumerAdapter == null)
        {
            return;
        }
        consumerAdapter.addMessagesToMonitoringPools(messageStoreSnapshot);
    }
    
    @Override
    public void onChannelSignal(final IDispatcherChannel<Object> channel, final String signal)
    {
        if (SIGNAL_CONSUME.equals(signal))
        {
            channel.removeStateAdapter(ConsumableState.class);
            
            ConsumeMessagesConsumerManagerAdapter consumerAdapter = channel.getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
            if (consumerAdapter == null)
            {
                return;
            }
            
            ConsumableState consumableState = consumerAdapter.getConsumableState();
            if (consumableState == null)
            {
                return;
            }
            channel.setStateAdapter(ConsumableState.class, consumableState);
            if (consumableState.getConsumerRule().getTimeOut() < 1)
            {
                channel.rescheduleTask(SERVICE_ID, System.currentTimeMillis(), -1L, -1L);
            }
            else
            {
                long heartbeatTimeout = consumableState.getConsumerRule().getTimeOutUnit().toMillis(consumableState.getConsumerRule().getTimeOut());
                channel.rescheduleTask(SERVICE_ID, System.currentTimeMillis(), -1L, heartbeatTimeout);
            }
        }
    }
    
    public static class ConsumeMessagesConsumerManagerAdapter implements IPoolController
    {
        protected ConsumeMessagesConsumerManagerAdapter(final IDispatcherChannel<Object> channel)
        {
            super();
            
            this.lock = new ReentrantReadWriteLock(true);
            this.readLock = this.lock.readLock();
            this.writeLock = this.lock.writeLock();
            this.plannerList = new LinkedList<ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter>();
            this.channel = channel;
        }
        
        private ReentrantReadWriteLock lock = null;
        private ReentrantReadWriteLock.ReadLock readLock = null;
        private ReentrantReadWriteLock.WriteLock writeLock = null;
        private LinkedList<ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter> plannerList = null;
        private IDispatcherChannel<Object> channel = null;
        
        protected ConsumableState getConsumableState()
        {
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    ConsumableState consumableState = planner.getConsumableState(true);
                    if (consumableState == null)
                    {
                        continue;
                    }
                    if (!(consumableState.isConsumable() || (consumableState.getConsumeMessageId() != null)))
                    {
                        continue;
                    }
                    return consumableState;
                }
            }
            finally
            {
                lock.unlock();
            }
            return null;
        }
        
        protected void setConsumeTimestamp(final long timestamp, final Set<String> members)
        {
            if (members == null)
            {
                return;
            }
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    planner.setConsumeTimestamp(timestamp, members);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void updateKeepMessagesState(final UUID poolId)
        {
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    planner.updateKeepMessagesState(poolId);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addMessagesToMonitoringPools(final DequeSnapshot<IMessage<Object>> snapshot)
        {
            boolean signal = false;
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    planner.addMessagesToMonitoring(snapshot);
                    
                    if (planner.checkConsumeOrReschedule())
                    {
                        if (!signal)
                        {
                            ConsumableState consumableState = planner.getConsumableState(false);
                            if ((consumableState != null) && consumableState.isConsumable())
                            {
                                signal = true;
                            }
                        }
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
            
            if (signal)
            {
                this.channel.signal(SIGNAL_CONSUME);
            }
        }
        
        protected void removeRemovedMessagesFromMonitoringPools()
        {
            boolean signal = false;
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    planner.removeRemovedMessages();
                    
                    if (planner.checkConsumeOrReschedule())
                    {
                        if (!signal)
                        {
                            ConsumableState consumableState = planner.getConsumableState(false);
                            if ((consumableState != null) && consumableState.isConsumable())
                            {
                                signal = true;
                            }
                        }
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
            
            if (signal)
            {
                this.channel.signal(SIGNAL_CONSUME);
            }
        }
        
        protected void addPlanner(final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner)
        {
            Lock lock = this.writeLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter check : this.plannerList)
                {
                    if (planner == check)
                    {
                        return;
                    }
                }
                this.plannerList.add(planner);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void removePlanner(final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner)
        {
            Lock lock = this.writeLock;
            lock.lock();
            try
            {
                ListIterator<ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter> itr = this.plannerList.listIterator();
                while (itr.hasNext())
                {
                    if (planner == itr.next())
                    {
                        itr.remove();
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void removeConsumeMessageFlag(final UUID poolId, final UUID flag)
        {
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    planner.removeConsumeMessageFlag(poolId, flag);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        @Override
        public void consumeMessages(final String poolAddress)
        {
            if (poolAddress == null)
            {
                return;
            }
            
            boolean signal = false;
            
            Lock lock = this.readLock;
            lock.lock();
            try
            {
                for (final ConsumeMessagesPlannerManager.ConsumeMessagesPlannerManagerAdapter planner : this.plannerList)
                {
                    if (planner.consumeMessages(poolAddress))
                    {
                        signal = true;
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
            
            if (signal)
            {
                this.channel.signal(SIGNAL_CONSUME);
            }
            
        }
    }
    
    protected static class MessageConsumeHelperImpl implements MessageConsumeHelper<Object, Object>
    {
        private boolean firstMessage = false;
        private boolean lastMessage = false;
        private List<IMessage<Object>> messageList = null;
        private IMessage<Object> message = null;
        private IDispatcherChannel<Object> channel = null;
        private Object helper = null;
        private IDispatcherChannelTaskContext<Object> taskContext = null;
        private KeepMessagesMode keepMessageMode = null;
        private UUID poolId = null;
        
        @Override
        public boolean isFirstMessage()
        {
            return this.firstMessage;
        }
        
        @Override
        public boolean isLastMessage()
        {
            return this.lastMessage;
        }
        
        @Override
        public int messageSize()
        {
            return this.messageList.size();
        }
        
        @Override
        public Collection<IMessage<Object>> getAllMessages()
        {
            return this.messageList;
        }
        
        @Override
        public IMessage<Object> getMessage()
        {
            return this.message;
        }
        
        @Override
        public IDispatcherChannel<Object> getChannel()
        {
            return this.channel;
        }
        
        @Override
        public Object getHelper(final Supplier<Object> supplierIfNotExist)
        {
            if (this.helper == null)
            {
                if (supplierIfNotExist != null)
                {
                    this.helper = supplierIfNotExist.get();
                }
            }
            return this.helper;
        }
        
        @Override
        public void heartbeat()
        {
            try
            {
                this.taskContext.heartbeat();
            }
            catch (final Exception e) { }
            catch (final Error e) { }
        }
        
        @Override
        public boolean isInTimeout()
        {
            return this.taskContext.getTaskControl().isInTimeout();
        }
        
        @Override
        public boolean isProcessed()
        {
            return isProcessedByConfig(this.keepMessageMode, this.message, this.poolId);
        }
        
        @Override
        public void setProcessed()
        {
            setProcessedByConfig(this.keepMessageMode, this.message, this.poolId);
        }
        
        protected static boolean isConsumedByConfig(final KeepMessagesMode keepMessageMode, final IMessage<?> message, final UUID poolId)
        {
            if (keepMessageMode == KeepMessagesMode.MessagesConsumedByRule)
            {
                if (poolId == null)
                {
                    return false;
                }
                
                Object value = message.getProperty("CONSUMED_BY_RULE_" + poolId);
                
                if (value instanceof Boolean)
                {
                    return false;
                }
                
                return ((Boolean) value).booleanValue();
            }
            
            return message.isConsumed();
        }
        
        protected static boolean setConsumedByConfig(final KeepMessagesMode keepMessageMode, final IMessage<?> message, final UUID poolId)
        {
            boolean previews = isConsumedByConfig(keepMessageMode, message, poolId);
            
            if (keepMessageMode == KeepMessagesMode.MessagesConsumedByRule)
            {
                if (poolId == null)
                {
                    return previews;
                }
                
                message.setProperty("CONSUMED_BY_RULE_" + poolId, Boolean.TRUE);
            }
            
            message.setConsumed(Boolean.TRUE);
            
            return previews;
        }
        
        protected static boolean isProcessedByConfig(final KeepMessagesMode keepMessageMode, final IMessage<?> message, final UUID poolId)
        {
            if (keepMessageMode == KeepMessagesMode.MessagesProcessedByRule)
            {
                if (poolId == null)
                {
                    return false;
                }
                
                Object value = message.getProperty("PROCESSED_BY_RULE_" + poolId);
                
                if (value instanceof Boolean)
                {
                    return false;
                }
                
                return ((Boolean) value).booleanValue();
            }
            
            return message.isProcessed();
        }
        
        protected static boolean setProcessedByConfig(final KeepMessagesMode keepMessageMode, final IMessage<?> message, final UUID poolId)
        {
            boolean previews = isProcessedByConfig(keepMessageMode, message, poolId);
            
            if (keepMessageMode == KeepMessagesMode.MessagesProcessedByRule)
            {
                if (poolId == null)
                {
                    return previews;
                }
                
                message.setProperty("PROCESSED_BY_RULE_" + poolId, Boolean.TRUE);
            }
            
            message.setProcessed(Boolean.TRUE);
            
            return previews;
        }
        
    }
}
