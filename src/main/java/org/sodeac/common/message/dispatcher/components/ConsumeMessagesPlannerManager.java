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
    
    ;
    
    @Override
    public void configureChannelManagerPolicy(IChannelManagerPolicy componentBindingPolicy)
    {
        componentBindingPolicy
            .addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelConfiguration(MATCH_FILTER).setName(MANAGER_NAME));
    }
    
    @Override
    public void configureChannelServicePolicy(IChannelServicePolicy componentBindingPolicy)
    {
        componentBindingPolicy
            .addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelConfiguration(MATCH_FILTER).setName(SERVICE_NAME))
            .addConfigurationDetail(new ComponentBindingSetup.ChannelServiceConfiguration(SERVICE_ID).setName(SERVICE_NAME)
                                                                                                     .setPeriodicRepetitionIntervalMS(777 * 1080)
                                                                                                     .setStartDelayInMS(77));
    }
    
    @Override
    public void onChannelAttach(IDispatcherChannel<Object> channel)
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
    public void onChannelDetach(IDispatcherChannel<Object> channel)
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
    public void run(IDispatcherChannelTaskContext<Object> taskContext) throws Exception
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
        public ConsumeMessagesPlannerManagerAdapter(MessageConsumerFeature.MessageConsumerFeatureConfiguration configuration, IDispatcherChannel<Object> channel)
        {
            super();
            Objects.requireNonNull(configuration, "no configuratrion for message consumer feature");
            Objects.requireNonNull(channel, "no consumer channel");
            
            this.lock = new ReentrantLock();
            this.monitoringPoolList = new ArrayList<>(configuration.getConsumerRuleList().size());
            
            for (ConsumerRule consumerRule : configuration.getConsumerRuleList())
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
        
        protected void removeConsumeMessageFlag(UUID poolId, UUID flag)
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
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
        
        protected ConsumableState getConsumableState(boolean requireMessageList)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return null;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
        
        protected void serviceRoutine(IDispatcherChannelTaskContext<Object> taskContext)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                Long minTimestamp = 0L;
                boolean signal = false;
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
                if (disposed)
                {
                    return false;
                }
                
                boolean consume = false;
                Long minTimestamp = 0L;
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
                    channel.rescheduleTask(SERVICE_ID, this.currentReschedule, -1L, -1L);
                }
                return consume;
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addMessageToMonitoring(IMessage<Object> message)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
                {
                    messageMonitoringPool.addToMonitoring(message);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void addMessagesToMonitoring(DequeSnapshot<IMessage<Object>> snapshot)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
                {
                    for (IMessage<Object> message : snapshot)
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
        
        protected void addAllToMonitoring(Collection<IMessage<Object>> messageList)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
                {
                    messageMonitoringPool.removeRemovedMessages();
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected void setConsumeTimestamp(long timestamp, Set<String> members)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
                {
                    messageMonitoringPool.setConsumeTimestamp(timestamp, members);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        
        protected boolean consumeMessages(String poolAddress)
        {
            boolean match = false;
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return false;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
        
        protected void updateKeepMessagesState(UUID poolId)
        {
            Lock lock = this.lock;
            lock.lock();
            try
            {
                if (disposed)
                {
                    return;
                }
                
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
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
            public ConsumableState(UUID poolId)
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
                return consumable;
            }
            
            protected Long getFatefulTime()
            {
                return fatefulTime;
            }
            
            protected LinkedList<IMessage<Object>> getConsumableList()
            {
                return consumableList;
            }
            
            protected ConsumerRule getConsumerRule()
            {
                return consumerRule;
            }
            
            protected MessageConsumeHelperImpl getMessageConsumeHelperImpl()
            {
                return messageConsumeHelperImpl;
            }
            
            protected void setMessageConsumeHelperImpl(MessageConsumeHelperImpl messageConsumeHelperImpl)
            {
                this.messageConsumeHelperImpl = messageConsumeHelperImpl;
            }
            
            protected UUID getPoolId()
            {
                return this.poolId;
            }
            
            protected KeepMessagesMode getKeepMessagesMode()
            {
                return keepMessagesMode;
            }
            
            protected UUID getConsumeMessageId()
            {
                return consumeMessageId;
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
            public MessageMonitoringPool(ConsumerRule consumerRule, IDispatcherChannel<Object> channel)
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
                    lastConsumeEvent = 1L;
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
                if ((consumable != null) && consumable.booleanValue())
                {
                    // cached state: is consumable now;
                    return now;
                }
                
                if (this.consumeMessageId != null)
                {
                    // consume on demand
                    return now;
                }
                
                if (consumable == null) // cleared cache
                {
                    currentConsumableCountByAge = 0;
                    consumable = false;
                }
                if ((fatefulTime != null) && (fatefulTime.longValue() > now))
                {
                    // cached fatefulTime
                    return fatefulTime;
                }
                
                if (consumeAgeSensible)
                {
                    if (lastConsumeEvent == 0)
                    {
                        return -1;
                    }
                    long fatefulTimeByComsumeAge = lastConsumeEvent + consumeAgeTriggerTimeInMillis;
                    if (fatefulTimeByComsumeAge > now)
                    {
                        fatefulTime = fatefulTimeByComsumeAge;
                        return fatefulTimeByComsumeAge;
                    }
                }
                
                // check min pool size
                
                if (this.messageMonitoringList.size() < consumerRule.getPoolMinSize())
                {
                    // wait for more messages
                    fatefulTime = null;
                    return -1;
                }
                
                if ((keepMessagesMode != null) && (!consumeAgeSensible))
                {
                    int max = consumerRule.getPoolMaxSize();
                    ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator();
                    boolean unDone = false;
                    
                    if (keepMessagesMode == KeepMessagesMode.MessagesConsumed)
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
                    else if (keepMessagesMode == KeepMessagesMode.MessagesProcessed)
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
                    else if (keepMessagesMode == KeepMessagesMode.MessagesConsumedByRule)
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
                    else if (keepMessagesMode == KeepMessagesMode.MessagesProcessedByRule)
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
                        
                        fatefulTime = null;
                        return -1;
                    }
                }
                
                if ((!messageAgeSensible) || (requiredConsumableCountByAge <= currentConsumableCountByAge))
                {
                    // is consumable
                    fatefulTime = null;
                    consumable = true;
                    return now;
                }
                
                // check message age sensible
                
                if (!messageMonitoringList.isEmpty())
                {
                    ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator(this.messageMonitoringList.size() - currentConsumableCountByAge);
                    
                    long requiredCreateTimestamp = now - messageAgeTriggerTimeInMillis;
                    
                    Long calculatedFatefulTime = null;
                    Integer futureConsumableCount = null;
                    while (itr.hasPrevious())
                    {
                        IMessage<Object> message = itr.previous();
                        if (calculatedFatefulTime != null)
                        {
                            futureConsumableCount++;
                            calculatedFatefulTime = message.getCreateTimestamp() + messageAgeTriggerTimeInMillis;
                            
                            if (requiredConsumableCountByAge <= futureConsumableCount)
                            {
                                break;
                            }
                            continue;
                        }
                        if (message.getCreateTimestamp() <= requiredCreateTimestamp)
                        {
                            currentConsumableCountByAge++;
                            
                            if (requiredConsumableCountByAge <= currentConsumableCountByAge)
                            {
                                consumable = true;
                                fatefulTime = null;
                                return now;
                            }
                        }
                        else
                        {
                            futureConsumableCount = currentConsumableCountByAge + 1;
                            calculatedFatefulTime = message.getCreateTimestamp() + messageAgeTriggerTimeInMillis;
                            
                            if (requiredConsumableCountByAge <= futureConsumableCount)
                            {
                                break;
                            }
                        }
                    }
                    
                    if (calculatedFatefulTime != null)
                    {
                        fatefulTime = calculatedFatefulTime;
                        return fatefulTime;
                    }
                }
                
                // wait for more messages
                return -1;
            }
            
            private ConsumableState getConsumableState(boolean requireMessageList)
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
                
                consumableState.consumable = consumable;
                consumableState.fatefulTime = fatefulTime;
                
                if (consumable || (consumableState.consumeMessageId != null))
                {
                    consumableState.consumerRule = consumerRule;
                }
                
                if (requireMessageList)
                {
                    if (consumableState.consumeMessageId != null) // consume on demand
                    {
                        consumableState.consumableList = new LinkedList<>(this.messageMonitoringList);
                    }
                    else if (consumable) // consume by rule
                    {
                        consumableState.consumableList = new LinkedList<>();
                        
                        ListIterator<IMessage<Object>> itr = this.messageMonitoringList.listIterator(this.messageMonitoringList.size());
                        if (this.consumerRule.getMessageAgeTriggerMode() == TriggerByMessageAgeMode.ALL)
                        {
                            long requiredCreateTimestamp = System.currentTimeMillis() - messageAgeTriggerTimeInMillis;
                            while (itr.hasPrevious() && (consumableState.consumableList.size() < consumerRule.getPoolMaxSize()))
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
                            while (itr.hasPrevious() && (consumableState.consumableList.size() < consumerRule.getPoolMaxSize()))
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
            
            private void setConsumeTimestamp(long timestamp, Set<String> members)
            {
                if (!consumeAgeSensible)
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
            
            private boolean addToMonitoring(IMessage<Object> message)
            {
                if (message == null)
                {
                    return false;
                }
                if (consumerRule.getPoolMaxSize() < 1)
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
                        
                        for (IMessage<Object> check : messageMonitoringList)
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
                            ListIterator<IMessage<Object>> itr = messageBufferList.listIterator();
                            while (itr.hasNext())
                            {
                                if (itr.next().isRemoved())
                                {
                                    itr.remove();
                                }
                            }
                            
                            itr = messageMonitoringList.listIterator();
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
                                while ((!messageBufferList.isEmpty()) && (messageMonitoringList.size() < consumerRule.getPoolMaxSize()))
                                {
                                    messageMonitoringList.addFirst(messageBufferList.removeLast());
                                }
                                
                                this.resetCache();
                            }
                        }
                    }
                    
                    boolean bufferIsEmpty = messageBufferList.isEmpty();
                    boolean monitorIsEmpty = messageMonitoringList.isEmpty();
                    
                    if (bufferIsEmpty && monitorIsEmpty)
                    {
                        // new message is the only one => add to monitor directly
                        
                        messageMonitoringList.addFirst(message);
                        
                        return true;
                    }
                    
                    Long biggestExistingSequenceInPool = null;
                    if (!bufferIsEmpty)
                    {
                        biggestExistingSequenceInPool = messageBufferList.getFirst().getSequence();
                    }
                    else if (!monitorIsEmpty)
                    {
                        biggestExistingSequenceInPool = messageMonitoringList.getFirst().getSequence();
                    }
                    
                    if ((biggestExistingSequenceInPool != null) && (biggestExistingSequenceInPool.longValue() < newMessageSequence))
                    {
                        // no one of messages in pool has an sequence greater than new message
                        
                        if (bufferIsEmpty && (messageMonitoringList.size() < consumerRule.getPoolMaxSize()))
                        {
                            // no message in buffer => add to monitor directly
                            
                            messageMonitoringList.addFirst(message);
                            
                            return true;
                        }
                        else
                        {
                            // add to buffer
                            messageBufferList.addFirst(message);
                            
                            return true;
                        }
                    }
                    else
                    {
                        // search in buffer  => insert message after first and before last of buffer
                        
                        ListIterator<IMessage<Object>> itr = messageBufferList.listIterator();
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
                            
                            messageBufferList.add(message);
                            return true;
                        }
                        
                        if (newMessageSequence > messageMonitoringList.getFirst().getSequence().longValue())
                        {
                            // insert message as last of buffer
                            
                            messageBufferList.add(message);
                            return true;
                        }
                        
                        // exceptional case
                        
                        itr = messageMonitoringList.listIterator();
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
                                
                                if ((consumable != null) && (consumable.booleanValue()))
                                {
                                    // is already consumable
                                    
                                    itr.previous();
                                    itr.add(message);
                                    
                                    // remove current consumable count cache
                                    currentConsumableCountByAge = 0;
                                    
                                    return true;
                                }
                                
                                currentConsumableCountByAge = 0;
                                
                                itr.previous();
                                itr.add(message);
                                this.resetCache();
                                
                                return true;
                            }
                        }
                        
                        // insert message as last of monitor
                        
                        messageMonitoringList.add(message);
                        this.resetCache();
                        
                        return true;
                    }
                }
                finally
                {
                    while (messageMonitoringList.size() > consumerRule.getPoolMaxSize())
                    {
                        messageBufferList.addLast(messageMonitoringList.removeFirst());
                        this.resetCache();
                    }
                    while ((messageBufferList.size() > 0) && (messageMonitoringList.size() < consumerRule.getPoolMaxSize()))
                    {
                        messageMonitoringList.addFirst(messageBufferList.removeLast());
                    }
                }
            }
            
            private void removeRemovedMessages()
            {
                ListIterator<IMessage<Object>> itr = messageBufferList.listIterator();
                while (itr.hasNext())
                {
                    if (itr.next().isRemoved())
                    {
                        itr.remove();
                    }
                }
                
                boolean reset = false;
                itr = messageMonitoringList.listIterator();
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
                    while ((!messageBufferList.isEmpty()) && (messageMonitoringList.size() < consumerRule.getPoolMaxSize()))
                    {
                        messageMonitoringList.addFirst(messageBufferList.removeLast());
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
                for (MessageMonitoringPool messageMonitoringPool : monitoringPoolList)
                {
                    messageMonitoringPool.dispose();
                }
                
                try
                {
                    monitoringPoolList.clear();
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
