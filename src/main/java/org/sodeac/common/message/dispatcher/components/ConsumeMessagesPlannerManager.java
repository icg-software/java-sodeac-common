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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelComponent;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemManager;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemService;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTaskContext;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelDetach;
import org.sodeac.common.message.dispatcher.components.ConsumeMessagesConsumerManager.ConsumeMessagesConsumerManagerAdapter;
import org.sodeac.common.message.dispatcher.components.ConsumeMessagesConsumerManager.MessageConsumeHelperImpl;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule.TriggerByMessageAgeMode;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;

@Component(service = { IDispatcherChannelSystemManager.class, IDispatcherChannelSystemService.class }, property = { "type=consume-messages", "role=planner" })
public class ConsumeMessagesPlannerManager implements IDispatcherChannelSystemManager, IOnChannelAttach<Object>, IOnChannelDetach<Object>, IDispatcherChannelSystemService<Object>
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    public static final IFilterItem MATCH_FILTER = IDispatcherChannelComponent.getAdapterMatchFilter(MessageConsumerFeature.MessageConsumerFeatureConfiguration.class);
    
    public static final String MANAGER_NAME = "Consume Messages Planner Manager";
    public static final String SERVICE_NAME = "Consume Messages Planner Service";
    public static final String SERVICE_ID = ConsumeMessagesPlannerManager.class.getCanonicalName() + ".Service";
    
    protected enum KeepMessagesMode
    {MessagesConsumed, MessagesProcessed, MessagesConsumedByRule, MessagesProcessedByRule}
    
    @Override
    public void configureChannelManagerPolicy(final IChannelManagerPolicy componentBindingPolicy)
    {
        componentBindingPolicy
            .addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelConfiguration(MATCH_FILTER).setName(MANAGER_NAME));
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
        // activate Consume-Message-Execute manager in parent channel
        channel.getParentChannel().getConfigurationPropertyBlock().setProperty(ConsumeMessagesConsumerManager.class.getCanonicalName(), Boolean.TRUE.toString());
        
        // planner state adapter in current channel
        ConsumeMessagesPlannerManagerAdapter plannerAdapter = new ConsumeMessagesPlannerManagerAdapter
            (
                channel.getConfigurationAdapter(MessageConsumerFeature.MessageConsumerFeatureConfiguration.class),
                channel
            );
        channel.setStateAdapter(ConsumeMessagesPlannerManagerAdapter.class, plannerAdapter);
        
        // execute state adapter in parent channel
        ConsumeMessagesConsumerManagerAdapter executeAdapter = channel.getParentChannel(Object.class).getStateAdapter
            (
                ConsumeMessagesConsumerManagerAdapter.class,
                () -> new ConsumeMessagesConsumerManagerAdapter(channel.getParentChannel(Object.class))
            );
        executeAdapter.addPlanner(plannerAdapter);
        
        // monitor already existing messages
        
        try (DequeSnapshot<IMessage<Object>> messages = channel.getParentChannel(Object.class).getMessageSnapshot())
        {
            if (!messages.isEmpty())
            {
                plannerAdapter.addAllToMonitoring(messages);
            }
        }
    }
    
    @Override
    public void onChannelDetach(final IDispatcherChannel<Object> channel)
    {
        ConsumeMessagesPlannerManagerAdapter plannerAdapter = channel.getStateAdapter(ConsumeMessagesPlannerManagerAdapter.class);
        if (plannerAdapter == null)
        {
            return;
        }
        ConsumeMessagesConsumerManagerAdapter executeAdapter = channel.getParentChannel(Object.class).getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
        if (executeAdapter == null)
        {
            return;
        }
        executeAdapter.removePlanner(plannerAdapter);
        plannerAdapter.dispose();
        
    }
    
    @Override
    public void run(final IDispatcherChannelTaskContext<Object> taskContext) throws Exception
    {
        ConsumeMessagesPlannerManagerAdapter plannerAdapter = taskContext.getChannel().getStateAdapter(ConsumeMessagesPlannerManagerAdapter.class);
        if (plannerAdapter == null)
        {
            return;
        }
        
        plannerAdapter.serviceRoutine(taskContext);
    }
    
    protected static class ConsumeMessagesPlannerManagerAdapter
    {
        public ConsumeMessagesPlannerManagerAdapter(final MessageConsumerFeature.MessageConsumerFeatureConfiguration configuration, final IDispatcherChannel<Object> channel)
        {
            super();
            Objects.requireNonNull(configuration, "no configuratrion for message consumer feature");
            Objects.requireNonNull(channel, "no consumer channel");
            
            this.lock = new ReentrantLock();
            this.monitoringPoolList = new ArrayList<>(configuration.getConsumerRuleList().size());
            
            for (final ConsumerRule consumerRule : configuration.getConsumerRuleList())
            {
                this.monitoringPoolList.add(new MessageMonitoringPool(consumerRule, channel));
            }
            this.channel = channel;
        }
        
        private ReentrantLock lock = null;
        private List<MessageMonitoringPool> monitoringPoolList = null;
        private volatile long currentReschedule = 0L;
        private IDispatcherChannel<Object> channel = null;
        private volatile boolean disposed = false;
        
        protected void removeConsumeMessageFlag(final UUID poolId, final UUID flag)
        {
            if (poolId == null)
            {
                return;
            }
            
            if (flag == null)
            {
                return;
            }
            
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    if (!poolId.equals(messageMonitoringPool.id))
                    {
                        continue;
                    }
                    
                    if (!flag.equals(messageMonitoringPool.consumeMessageId))
                    {
                        continue;
                    }
                    
                    messageMonitoringPool.consumeMessageId = null;
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected ConsumableState getConsumableState(final boolean requireMessageList)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return null;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    ConsumableState consumableState = messageMonitoringPool.getConsumableState(requireMessageList);
                    if (consumableState == null)
                    {
                        continue;
                    }
                    if (!(consumableState.isConsumable() || (consumableState.consumeMessageId != null)))
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
        
        protected void serviceRoutine(final IDispatcherChannelTaskContext<Object> taskContext)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                Long minTimestamp = 0L;
                boolean signal = false;
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    long next = messageMonitoringPool.calculateFatefulTime();
                    if (next < 0L)
                    {
                        continue;
                    }
                    if (messageMonitoringPool.consumable.booleanValue())
                    {
                        signal = true;
                        continue;
                    }
                    if (minTimestamp == 0L)
                    {
                        minTimestamp = next;
                    }
                    else if (minTimestamp.longValue() > next)
                    {
                        minTimestamp = next;
                    }
                }
                
                if (signal)
                {
                    taskContext.getChannel().getParentChannel(Object.class).signal(ConsumeMessagesConsumerManager.SIGNAL_CONSUME);
                }
                
                if (minTimestamp < 1L)
                {
                    this.currentReschedule = taskContext.getTaskControl().getExecutionTimestamp();
                    return;
                }
                else if (minTimestamp <= System.currentTimeMillis())
                {
                    this.currentReschedule = taskContext.getTaskControl().getExecutionTimestamp();
                    if (!signal)
                    {
                        taskContext.getChannel().getParentChannel(Object.class).signal(ConsumeMessagesConsumerManager.SIGNAL_CONSUME);
                    }
                }
                this.currentReschedule = minTimestamp;
                taskContext.getTaskControl().setExecutionTimestamp(this.currentReschedule, true);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected boolean checkConsumeOrReschedule()
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return false;
                }
                
                boolean consume = false;
                Long minTimestamp = 0L;
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    long next = messageMonitoringPool.calculateFatefulTime();
                    if (next < 0L)
                    {
                        continue;
                    }
                    if (messageMonitoringPool.consumable.booleanValue())
                    {
                        consume = true;
                        continue;
                    }
                    if (minTimestamp == 0)
                    {
                        minTimestamp = next;
                    }
                    if (minTimestamp.longValue() > next)
                    {
                        minTimestamp = next;
                    }
                }
                
                if (minTimestamp < 1L)
                {
                    return consume;
                }
                if (minTimestamp <= System.currentTimeMillis())
                {
                    return consume;
                }
                if (this.currentReschedule != minTimestamp) ;
                {
                    this.currentReschedule = minTimestamp;
                    this.channel.rescheduleTask(SERVICE_ID, this.currentReschedule, -1L, -1L);
                }
                return consume;
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addMessageToMonitoring(final IMessage<Object> message)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    messageMonitoringPool.addToMonitoring(message);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addMessagesToMonitoring(final DequeSnapshot<IMessage<Object>> snapshot)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    for (final IMessage<Object> message : snapshot)
                    {
                        messageMonitoringPool.addToMonitoring(message);
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addAllToMonitoring(final Collection<IMessage<Object>> messageList)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    messageList.forEach(m -> messageMonitoringPool.addToMonitoring(m));
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void removeRemovedMessages()
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    messageMonitoringPool.removeRemovedMessages();
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void setConsumeTimestamp(final long timestamp, final Set<String> members)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    messageMonitoringPool.setConsumeTimestamp(timestamp, members);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected boolean consumeMessages(final String poolAddress)
        {
            boolean match = false;
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return false;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    if (poolAddress.equals(messageMonitoringPool.consumerRule.getPoolAddress()))
                    {
                        messageMonitoringPool.consumeMessageId = UUID.randomUUID();
                        match = true;
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
            
            return match;
        }
        
        protected void updateKeepMessagesState(final UUID poolId)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (this.disposed)
                {
                    return;
                }
                
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    if (messageMonitoringPool.id.equals(poolId))
                    {
                        messageMonitoringPool.resetCache();
                    }
                    else if (messageMonitoringPool.keepMessagesMode == KeepMessagesMode.MessagesConsumed)
                    {
                        messageMonitoringPool.resetCache();
                    }
                    else if (messageMonitoringPool.keepMessagesMode == KeepMessagesMode.MessagesProcessed)
                    {
                        messageMonitoringPool.resetCache();
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected class ConsumableState
        {
            public ConsumableState(final UUID poolId)
            {
                super();
                this.poolId = poolId;
            }
            
            private boolean consumable = false;
            private Long fatefulTime = null;
            private LinkedList<IMessage<Object>> consumableList = null;
            private ConsumerRule consumerRule = null;
            private UUID poolId = null;
            private MessageConsumeHelperImpl messageConsumeHelperImpl = null;
            private KeepMessagesMode keepMessagesMode = null;
            private UUID consumeMessageId = null;
            
            protected boolean isConsumable()
            {
                return this.consumable;
            }
            
            protected Long getFatefulTime()
            {
                return this.fatefulTime;
            }
            
            protected LinkedList<IMessage<Object>> getConsumableList()
            {
                return this.consumableList;
            }
            
            protected ConsumerRule getConsumerRule()
            {
                return this.consumerRule;
            }
            
            protected MessageConsumeHelperImpl getMessageConsumeHelperImpl()
            {
                return this.messageConsumeHelperImpl;
            }
            
            protected void setMessageConsumeHelperImpl(final MessageConsumeHelperImpl messageConsumeHelperImpl)
            {
                this.messageConsumeHelperImpl = messageConsumeHelperImpl;
            }
            
            protected UUID getPoolId()
            {
                return this.poolId;
            }
            
            protected KeepMessagesMode getKeepMessagesMode()
            {
                return this.keepMessagesMode;
            }
            
            protected UUID getConsumeMessageId()
            {
                return this.consumeMessageId;
            }
            
            protected void dispose()
            {
                this.fatefulTime = null;
                this.consumableList = null;
                this.consumerRule = null;
                this.messageConsumeHelperImpl = null;
                this.poolId = null;
                this.keepMessagesMode = null;
                this.consumeMessageId = null;
            }
        }
        
        private class MessageMonitoringPool
        {
            public MessageMonitoringPool(final ConsumerRule consumerRule, final IDispatcherChannel<Object> channel)
            {
                super();
                this.id = UUID.randomUUID();
                this.messageBufferList = new LinkedList<>();
                this.messageMonitoringList = new LinkedList<>();
                this.consumerRule = consumerRule;
                this.messageChannel = channel.getParentChannel();
                
                if (this.consumerRule.getConsumeEventAgeTriggerAge() > -1)
                {
                    this.consumeAgeSensible = true;
                    this.consumeAgeTriggerTimeInMillis = consumerRule.getConsumeEventAgeTriggerUnit().toMillis(consumerRule.getConsumeEventAgeTriggerAge());
                }
                
                if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.ALL)
                {
                    this.messageAgeSensible = true;
                    this.messageAgeTriggerTimeInMillis = consumerRule.getMessageAgeTriggerUnit().toMillis(consumerRule.getMessageAgeTriggerAge());
                    
                    this.requiredConsumableCountByAge = consumerRule.getPoolMinSize();
                    if (this.requiredConsumableCountByAge < 1)
                    {
                        this.requiredConsumableCountByAge = 1;
                    }
                }
                else if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.LEAST_ONE)
                {
                    this.messageAgeSensible = true;
                    this.messageAgeTriggerTimeInMillis = consumerRule.getMessageAgeTriggerUnit().toMillis(consumerRule.getMessageAgeTriggerAge());
                    
                    this.requiredConsumableCountByAge = 1;
                }
                else if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.LEAST_X)
                {
                    if (consumerRule.getMessageAgeTriggerCount() < 1)
                    {
                        this.messageAgeSensible = false;
                    }
                    else
                    {
                        this.messageAgeSensible = true;
                        this.messageAgeTriggerTimeInMillis = consumerRule.getMessageAgeTriggerUnit().toMillis(consumerRule.getMessageAgeTriggerAge());
                        
                        this.requiredConsumableCountByAge = consumerRule.getMessageAgeTriggerCount();
                    }
                }
                else
                {
                    this.messageAgeSensible = false;
                }
                
                if (consumerRule.isKeepMessages())
                {
                    // MessageConsumerFeature.KEEP_MESSAGE_MODE_MESSAGES_CONSUMED = 1;
                    // MessageConsumerFeature.KEEP_MESSAGE_MODE_MESSAGES_PROCESSED = 2;
                    // MessageConsumerFeature.KEEP_MESSAGE_MODE_MESSAGES_CONSUMED_BY_RULE = 3;
                    // MessageConsumerFeature.KEEP_MESSAGE_MODE_MESSAGES_PROCESSED_BY_RULE = 4;
                    
                    if (consumerRule.getKeepMessagesMode() == 1)
                    {
                        this.keepMessagesMode = KeepMessagesMode.MessagesConsumed;
                    }
                    else if (consumerRule.getKeepMessagesMode() == 2)
                    {
                        this.keepMessagesMode = KeepMessagesMode.MessagesProcessed;
                    }
                    else if (consumerRule.getKeepMessagesMode() == 3)
                    {
                        this.keepMessagesMode = KeepMessagesMode.MessagesConsumedByRule;
                    }
                    else if (consumerRule.getKeepMessagesMode() == 4)
                    {
                        this.keepMessagesMode = KeepMessagesMode.MessagesProcessedByRule;
                    }
                }
                
                if (consumerRule.isConsumeEventAgeTriggerNeverMode())
                {
                    this.lastConsumeEvent = 1L;
                }
                
            }
            
            private LinkedList<IMessage<Object>> messageBufferList = null;
            private LinkedList<IMessage<Object>> messageMonitoringList = null;
            private ConsumerRule consumerRule = null;
            private IDispatcherChannel<Object> messageChannel = null;
            
            private boolean consumeAgeSensible = false;
            private boolean messageAgeSensible = false;
            private int requiredConsumableCountByAge = 0; // min value
            private long consumeAgeTriggerTimeInMillis = 0L;
            private long messageAgeTriggerTimeInMillis = 0L;
            
            private volatile long lastConsumeEvent = 0L;
            
            private volatile Boolean consumable = null;
            private volatile Long fatefulTime = null;
            private int currentConsumableCountByAge = 0;
            private KeepMessagesMode keepMessagesMode = null;
            private volatile UUID consumeMessageId = null;
            
            private UUID id = null;
            
            /**
             *
             * @return -1 for unknown, otherwise timestamp for next action ()
             */
            private long calculateFatefulTime()
            {
                long now = System.currentTimeMillis();
                if ((this.consumable != null) && this.consumable.booleanValue())
                {
                    // cached state: is consumable now;
                    return now;
                }
                
                if (this.consumeMessageId != null)
                {
                    // consume on demand
                    return now;
                }
                
                if (this.consumable == null) // cleared cache
                {
                    this.currentConsumableCountByAge = 0;
                    this.consumable = false;
                }
                if ((this.fatefulTime != null) && (this.fatefulTime.longValue() > now))
                {
                    // cached fatefulTime
                    return this.fatefulTime;
                }
                
                if (this.consumeAgeSensible)
                {
                    if (this.lastConsumeEvent == 0)
                    {
                        return -1;
                    }
                    long fatefulTimeByComsumeAge = this.lastConsumeEvent + this.consumeAgeTriggerTimeInMillis;
                    if (fatefulTimeByComsumeAge > now)
                    {
                        this.fatefulTime = fatefulTimeByComsumeAge;
                        return fatefulTimeByComsumeAge;
                    }
                }
                
                // check min pool size
                
                if (this.messageMonitoringList.size() < this.consumerRule.getPoolMinSize())
                {
                    // wait for more messages
                    this.fatefulTime = null;
                    return -1;
                }
                
                if ((this.keepMessagesMode != null) && (!this.consumeAgeSensible))
                {
                    int max = this.consumerRule.getPoolMaxSize();
                    ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator();
                    boolean unDone = false;
                    
                    if (this.keepMessagesMode == KeepMessagesMode.MessagesConsumed)
                    {
                        while ((max > 0) && itr.hasNext())
                        {
                            max--;
                            IMessage<Object> message = itr.next();
                            if (!message.isConsumed())
                            {
                                unDone = true;
                                break;
                            }
                        }
                    }
                    else if (this.keepMessagesMode == KeepMessagesMode.MessagesProcessed)
                    {
                        while ((max > 0) && itr.hasNext())
                        {
                            max--;
                            IMessage<Object> message = itr.next();
                            if (!message.isProcessed())
                            {
                                unDone = true;
                                break;
                            }
                        }
                    }
                    else if (this.keepMessagesMode == KeepMessagesMode.MessagesConsumedByRule)
                    {
                        while ((max > 0) && itr.hasNext())
                        {
                            max--;
                            IMessage<Object> message = itr.next();
                            if (!MessageConsumeHelperImpl.isConsumedByConfig(this.keepMessagesMode, message, this.id))
                            {
                                unDone = true;
                                break;
                            }
                        }
                    }
                    else if (this.keepMessagesMode == KeepMessagesMode.MessagesProcessedByRule)
                    {
                        while ((max > 0) && itr.hasNext())
                        {
                            max--;
                            IMessage<Object> message = itr.next();
                            if (!MessageConsumeHelperImpl.isProcessedByConfig(this.keepMessagesMode, message, this.id))
                            {
                                unDone = true;
                                break;
                            }
                        }
                    }
                    
                    if (!unDone)
                    {
                        // prevent to consume same messages over and over again
                        
                        this.fatefulTime = null;
                        return -1;
                    }
                }
                
                if ((!this.messageAgeSensible) || (this.requiredConsumableCountByAge <= this.currentConsumableCountByAge))
                {
                    // is consumable
                    this.fatefulTime = null;
                    this.consumable = true;
                    return now;
                }
                
                // check message age sensible
                
                if (!this.messageMonitoringList.isEmpty())
                {
                    ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator(this.messageMonitoringList.size() - this.currentConsumableCountByAge);
                    
                    long requiredCreateTimestamp = now - this.messageAgeTriggerTimeInMillis;
                    
                    Long calculatedFatefulTime = null;
                    Integer futureConsumableCount = null;
                    while (itr.hasPrevious())
                    {
                        IMessage<Object> message = itr.previous();
                        if (calculatedFatefulTime != null)
                        {
                            futureConsumableCount++;
                            calculatedFatefulTime = message.getCreateTimestamp() + this.messageAgeTriggerTimeInMillis;
                            
                            if (this.requiredConsumableCountByAge <= futureConsumableCount)
                            {
                                break;
                            }
                            continue;
                        }
                        if (message.getCreateTimestamp() <= requiredCreateTimestamp)
                        {
                            this.currentConsumableCountByAge++;
                            
                            if (this.requiredConsumableCountByAge <= this.currentConsumableCountByAge)
                            {
                                this.consumable = true;
                                this.fatefulTime = null;
                                return now;
                            }
                        }
                        else
                        {
                            futureConsumableCount = this.currentConsumableCountByAge + 1;
                            calculatedFatefulTime = message.getCreateTimestamp() + this.messageAgeTriggerTimeInMillis;
                            
                            if (this.requiredConsumableCountByAge <= futureConsumableCount)
                            {
                                break;
                            }
                        }
                    }
                    
                    if (calculatedFatefulTime != null)
                    {
                        this.fatefulTime = calculatedFatefulTime;
                        return this.fatefulTime;
                    }
                }
                
                // wait for more messages
                return -1;
            }
            
            private ConsumableState getConsumableState(final boolean requireMessageList)
            {
                ConsumableState consumableState = new ConsumableState(this.id);
                consumableState.keepMessagesMode = this.keepMessagesMode;
                consumableState.consumeMessageId = this.consumeMessageId;
                
                long fatefullTimestamp = calculateFatefulTime();
                if (fatefullTimestamp == -1)
                {
                    consumableState.consumable = false;
                    consumableState.fatefulTime = null;
                    
                    return consumableState;
                }
                
                consumableState.consumable = this.consumable;
                consumableState.fatefulTime = this.fatefulTime;
                
                if (this.consumable || (consumableState.consumeMessageId != null))
                {
                    consumableState.consumerRule = this.consumerRule;
                }
                
                if (requireMessageList)
                {
                    if (consumableState.consumeMessageId != null) // consume on demand
                    {
                        consumableState.consumableList = new LinkedList<>(this.messageMonitoringList);
                    }
                    else if (this.consumable) // consume by rule
                    {
                        consumableState.consumableList = new LinkedList<>();
                        
                        ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator(this.messageMonitoringList.size());
                        if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.ALL)
                        {
                            long requiredCreateTimestamp = System.currentTimeMillis() - this.messageAgeTriggerTimeInMillis;
                            while (itr.hasPrevious() && (consumableState.consumableList.size() < this.consumerRule.getPoolMaxSize()))
                            {
                                IMessage<Object> message = itr.previous();
                                if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.ALL)
                                {
                                    if (message.getCreateTimestamp() > requiredCreateTimestamp)
                                    {
                                        break;
                                    }
                                }
                                consumableState.consumableList.addLast(message);
                            }
                            
                        }
                        else
                        {
                            while (itr.hasPrevious() && (consumableState.consumableList.size() < this.consumerRule.getPoolMaxSize()))
                            {
                                consumableState.consumableList.addLast(itr.previous());
                            }
                        }
                    }
                }
                
                return consumableState;
            }
            
            private void resetCache()
            {
                this.consumable = null;
                this.fatefulTime = null;
                this.currentConsumableCountByAge = 0;
            }
            
            private void setConsumeTimestamp(final long timestamp, final Set<String> members)
            {
                if (!this.consumeAgeSensible)
                {
                    return;
                }
                Objects.requireNonNull(members, "group members is null");
                if (this.consumerRule.getConsumeEventAgeTriggerGroup() != null)
                {
                    if (!members.contains(this.consumerRule.getConsumeEventAgeTriggerGroup()))
                    {
                        return;
                    }
                }
                this.lastConsumeEvent = timestamp;
                resetCache();
            }
            
            private boolean addToMonitoring(final IMessage<Object> message)
            {
                if (message == null)
                {
                    return false;
                }
                if (this.consumerRule.getPoolMaxSize() < 1)
                {
                    return false;
                }
                if (message.isRemoved())
                {
                    return false;
                }
                if (message.getSequence() == null)
                {
                    return false;
                }
                if (this.messageChannel != message.getChannel())
                {
                    return false;
                }
                
                if (this.consumerRule.getPoolFilter() != null)
                {
                    if (!this.consumerRule.getPoolFilter().test(message))
                    {
                        return false;
                    }
                }
                
                long newMessageSequence = message.getSequence();
                
                try
                {
                    
                    if (this.consumerRule.getReplaceOlderMessageFilter() != null)
                    {
                        boolean removed = false;
                        
                        for (final IMessage<Object> check : this.messageMonitoringList)
                        {
                            try
                            {
                                if (!check.isRemoved())
                                {
                                    Boolean rm = this.consumerRule.getReplaceOlderMessageFilter().apply(message, check);
                                    if ((rm != null) && rm.booleanValue())
                                    {
                                        check.removeFromChannel();
                                        removed = true;
                                    }
                                }
                                else
                                {
                                    removed = true;
                                }
                            }
                            catch (Exception | Error e) { }
                        }
                        
                        if (removed)
                        {
                            ListIterator<IMessage<Object>> itr = this.messageBufferList.listIterator();
                            while (itr.hasNext())
                            {
                                if (itr.next().isRemoved())
                                {
                                    itr.remove();
                                }
                            }
                            
                            itr = this.messageMonitoringList.listIterator();
                            boolean reset = false;
                            while (itr.hasNext())
                            {
                                if (itr.next().isRemoved())
                                {
                                    itr.remove();
                                    reset = true;
                                }
                            }
                            
                            if (reset)
                            {
                                while ((!this.messageBufferList.isEmpty()) && (this.messageMonitoringList.size() < this.consumerRule.getPoolMaxSize()))
                                {
                                    this.messageMonitoringList.addFirst(this.messageBufferList.removeLast());
                                }
                                
                                this.resetCache();
                            }
                        }
                    }
                    
                    boolean bufferIsEmpty = this.messageBufferList.isEmpty();
                    boolean monitorIsEmpty = this.messageMonitoringList.isEmpty();
                    
                    if (bufferIsEmpty && monitorIsEmpty)
                    {
                        // new message is the only one => add to monitor directly
                        
                        this.messageMonitoringList.addFirst(message);
                        
                        return true;
                    }
                    
                    Long biggestExistingSequenceInPool = null;
                    if (!bufferIsEmpty)
                    {
                        biggestExistingSequenceInPool = this.messageBufferList.getFirst().getSequence();
                    }
                    else if (!monitorIsEmpty)
                    {
                        biggestExistingSequenceInPool = this.messageMonitoringList.getFirst().getSequence();
                    }
                    
                    if ((biggestExistingSequenceInPool != null) && (biggestExistingSequenceInPool.longValue() < newMessageSequence))
                    {
                        // no one of messages in pool has an sequence greater than new message
                        
                        if (bufferIsEmpty && (this.messageMonitoringList.size() < this.consumerRule.getPoolMaxSize()))
                        {
                            // no message in buffer => add to monitor directly
                            
                            this.messageMonitoringList.addFirst(message);
                            
                            return true;
                        }
                        else
                        {
                            // add to buffer
                            this.messageBufferList.addFirst(message);
                            
                            return true;
                        }
                    }
                    else
                    {
                        // search in buffer  => insert message after first and before last of buffer
                        
                        ListIterator<IMessage<Object>> itr = this.messageBufferList.listIterator();
                        while (itr.hasNext())
                        {
                            IMessage<Object> check = itr.next();
                            if (check.getSequence().longValue() <= newMessageSequence)
                            {
                                if (check.getSequence().longValue() == newMessageSequence)
                                {
                                    if (check != message)
                                    {
                                        throw new IllegalStateException("duplicated sequence found");
                                    }
                                    return true; // nothing to add
                                }
                                itr.previous();
                                itr.add(message);
                                return true;
                            }
                        }
                        
                        if (monitorIsEmpty)
                        {
                            // insert message as last of buffer
                            
                            this.messageBufferList.add(message);
                            return true;
                        }
                        
                        if (newMessageSequence > this.messageMonitoringList.getFirst().getSequence().longValue())
                        {
                            // insert message as last of buffer
                            
                            this.messageBufferList.add(message);
                            return true;
                        }
                        
                        // exceptional case
                        
                        itr = this.messageMonitoringList.listIterator();
                        while (itr.hasNext())
                        {
                            IMessage<Object> check = itr.next();
                            if (check.getSequence().longValue() <= newMessageSequence)
                            {
                                if (check.getSequence().longValue() == newMessageSequence)
                                {
                                    if (check != message)
                                    {
                                        throw new IllegalStateException("duplicated sequence found");
                                    }
                                    return true; // nothing to add
                                }
                                
                                if ((this.consumable != null) && (this.consumable.booleanValue()))
                                {
                                    // is already consumable
                                    
                                    itr.previous();
                                    itr.add(message);
                                    
                                    // remove current consumable count cache
                                    this.currentConsumableCountByAge = 0;
                                    
                                    return true;
                                }
                                
                                this.currentConsumableCountByAge = 0;
                                
                                itr.previous();
                                itr.add(message);
                                this.resetCache();
                                
                                return true;
                            }
                        }
                        
                        // insert message as last of monitor
                        
                        this.messageMonitoringList.add(message);
                        this.resetCache();
                        
                        return true;
                    }
                }
                finally
                {
                    while (this.messageMonitoringList.size() > this.consumerRule.getPoolMaxSize())
                    {
                        this.messageBufferList.addLast(this.messageMonitoringList.removeFirst());
                        this.resetCache();
                    }
                    while ((this.messageBufferList.size() > 0) && (this.messageMonitoringList.size() < this.consumerRule.getPoolMaxSize()))
                    {
                        this.messageMonitoringList.addFirst(this.messageBufferList.removeLast());
                    }
                }
            }
            
            private void removeRemovedMessages()
            {
                ListIterator<IMessage<Object>> itr = this.messageBufferList.listIterator();
                while (itr.hasNext())
                {
                    if (itr.next().isRemoved())
                    {
                        itr.remove();
                    }
                }
                
                boolean reset = false;
                itr = this.messageMonitoringList.listIterator();
                while (itr.hasNext())
                {
                    if (itr.next().isRemoved())
                    {
                        itr.remove();
                        reset = true;
                    }
                }
                
                if (reset)
                {
                    while ((!this.messageBufferList.isEmpty()) && (this.messageMonitoringList.size() < this.consumerRule.getPoolMaxSize()))
                    {
                        this.messageMonitoringList.addFirst(this.messageBufferList.removeLast());
                    }
                    
                    this.resetCache();
                }
            }
            
            private void dispose()
            {
                if (this.messageBufferList != null)
                {
                    try
                    {
                        this.messageBufferList.clear();
                    }
                    catch (Exception | Error e) { }
                    this.messageBufferList = null;
                }
                if (this.messageMonitoringList != null)
                {
                    try
                    {
                        this.messageMonitoringList.clear();
                    }
                    catch (Exception | Error e) { }
                    this.messageMonitoringList = null;
                }
                this.consumerRule = null;
                this.messageChannel = null;
                
                this.consumable = null;
                this.fatefulTime = null;
                this.keepMessagesMode = null;
                
                this.id = null;
            }
            
        }
        
        public UUID getId()
        {
            return this.getId();
        }
        
        private void dispose()
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                this.disposed = true;
                for (final MessageMonitoringPool messageMonitoringPool : this.monitoringPoolList)
                {
                    messageMonitoringPool.dispose();
                }
                
                try
                {
                    this.monitoringPoolList.clear();
                }
                catch (Exception | Error e) { }
                
                this.monitoringPoolList = null;
                this.channel = null;
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}
