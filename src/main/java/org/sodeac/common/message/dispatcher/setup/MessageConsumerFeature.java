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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.components.ConsumeMessagesConsumerManager.ConsumeMessagesConsumerManagerAdapter;
import org.sodeac.common.message.dispatcher.setup.MessageConsumerFeature.ConsumerRule.TriggerByMessageAgeMode;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup.IChannelFeature;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup.IPreparedChannelFeature;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup.MessageConsumeHelper;

public class MessageConsumerFeature
{
    
    private final Integer KEEP_MESSAGE_MODE_MESSAGES_CONSUMED = 1;
    private final Integer KEEP_MESSAGE_MODE_MESSAGES_PROCESSED = 2;
    private final Integer KEEP_MESSAGE_MODE_MESSAGES_CONSUMED_BY_RULE = 3;
    private final Integer KEEP_MESSAGE_MODE_MESSAGES_PROCESSED_BY_RULE = 4;
    
    private MessageConsumerFeature()
    {
        super();
    }
    
    public static FeatureBuilder newBuilder()
    {
        return new MessageConsumerFeature().new FeatureBuilder();
    }
    
    public class FeatureBuilder
    {
        private final MessageConsumerFeatureConfiguration feature = new MessageConsumerFeatureConfiguration();
        
        public BuilderPhaseA1 inMessageMonitoringPool()
        {
            return new BuilderPhaseA1();
        }
        
        public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
        {
            return new BuilderPhaseB1().consumeMessage(messageConsumer);
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
        {
            FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
            return new BuilderPhaseB1().consumeMessage(messageConsumer);
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
        {
            FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
            return new BuilderPhaseB1().consumeMessage(messageConsumer);
        }
        
        public class BuilderPhaseA1
        {
            private BuilderPhaseA1()
            {
                super();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T> BuilderPhaseA2 useFilter(final Predicate<IMessage<T>> filter)
            {
                FeatureBuilder.this.feature.currentConsumerRule.poolFilter = (Predicate) filter;
                return new BuilderPhaseA2();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T> BuilderPhaseA2 useFilter(final Predicate<IMessage<T>> filter, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.poolFilter = (Predicate) filter;
                return new BuilderPhaseA2();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T> BuilderPhaseA3 useReplaceOlderMessageFilter(final BiFunction<IMessage<T>, IMessage<T>, Boolean> filter)
            {
                FeatureBuilder.this.feature.currentConsumerRule.replaceOlderMessageFilter = (BiFunction) filter;
                return new BuilderPhaseA3();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T> BuilderPhaseA3 useReplaceOlderMessageFilter(final BiFunction<IMessage<T>, IMessage<T>, Boolean> filter, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.replaceOlderMessageFilter = (BiFunction) filter;
                return new BuilderPhaseA3();
            }
            
            public BuilderPhaseA3.BuilderPhaseA4 minPoolSize(final int minSize)
            {
                return new BuilderPhaseA3().minPoolSize(minSize);
            }
            
            public BuilderPhaseB1 maxPoolSize(final int maxSize)
            {
                return new BuilderPhaseA3().maxPoolSize(maxSize);
            }
            
            public class BuilderPhaseA3
            {
                private BuilderPhaseA3()
                {
                    super();
                }
                
                public BuilderPhaseA4 minPoolSize(int minSize)
                {
                    if (minSize < 0)
                    {
                        minSize = 0;
                    }
                    FeatureBuilder.this.feature.currentConsumerRule.poolMinSize = minSize;
                    return new BuilderPhaseA4();
                }
                
                public BuilderPhaseB1 maxPoolSize(final int maxSize)
                {
                    return new BuilderPhaseA4().maxPoolSize(maxSize);
                }
                
                public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
                {
                    return new BuilderPhaseB1().consumeMessage(messageConsumer);
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                    return new BuilderPhaseB1().consumeMessage(messageConsumer);
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                    return new BuilderPhaseB1().consumeMessage(messageConsumer);
                }
                
                public class BuilderPhaseA4
                {
                    private BuilderPhaseA4()
                    {
                        super();
                    }
                    
                    public BuilderPhaseB1 maxPoolSize(int maxSize)
                    {
                        if (maxSize < 0)
                        {
                            maxSize = 0;
                        }
                        if (maxSize < FeatureBuilder.this.feature.currentConsumerRule.poolMinSize)
                        {
                            maxSize = FeatureBuilder.this.feature.currentConsumerRule.poolMinSize;
                        }
                        FeatureBuilder.this.feature.currentConsumerRule.poolMaxSize = maxSize;
                        
                        return new BuilderPhaseB1();
                    }
                    
                    public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
                    {
                        return new BuilderPhaseB1().consumeMessage(messageConsumer);
                    }
                    
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                        return new BuilderPhaseB1().consumeMessage(messageConsumer);
                    }
                    
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    public <T, H> BuilderPhaseB1.BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                        return new BuilderPhaseB1().consumeMessage(messageConsumer);
                    }
                }
            }
            
            public class BuilderPhaseA2 extends BuilderPhaseA3
            {
                private BuilderPhaseA2()
                {
                    super();
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T> BuilderPhaseA3 useReplaceOlderMessageFilter(final BiFunction<IMessage<T>, IMessage<T>, Boolean> filter)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.replaceOlderMessageFilter = (BiFunction) filter;
                    return new BuilderPhaseA3();
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T> BuilderPhaseA3 useReplaceOlderMessageFilter(final BiFunction<IMessage<T>, IMessage<T>, Boolean> filter, final Class<T> messageType)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.replaceOlderMessageFilter = (BiFunction) filter;
                    return new BuilderPhaseA3();
                }
            }
        }
        
        public class BuilderPhaseB1
        {
            private BuilderPhaseB1()
            {
                super();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                return new BuilderPhaseB2();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                return new BuilderPhaseB2();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseB2 consumeMessage(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageConsumer = (BiConsumer) messageConsumer;
                return new BuilderPhaseB2();
            }
            
            public class BuilderPhaseB2 extends BuilderPhaseC1
            {
                private BuilderPhaseB2()
                {
                    super();
                }
                
                public BuilderPhaseB3 memberOfGroup(final String group)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.groupMembers.add(group);
                    return new BuilderPhaseB3();
                }
                
                public class BuilderPhaseB3 extends BuilderPhaseC1
                {
                    private BuilderPhaseB3()
                    {
                        super();
                    }
                    
                    public BuilderPhaseB3 andMemberOfGroup(final String group)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.groupMembers.add(group);
                        return this;
                    }
                }
                
            }
        }
        
        public class BuilderPhaseC1
        {
            private BuilderPhaseC1()
            {
                super();
            }
            
            public BuilderPhaseC11 withTimeoutForEachMessage(final int timeOut)
            {
                FeatureBuilder.this.feature.currentConsumerRule.timeOut = timeOut;
                return new BuilderPhaseC11();
            }
            
            public class BuilderPhaseC11
            {
                private BuilderPhaseC11()
                {
                    super();
                }
                
                public BuilderPhaseC12 milliSeconds()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutUnit = TimeUnit.MILLISECONDS;
                    
                    return new BuilderPhaseC12();
                }
                
                public BuilderPhaseC12 seconds()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutUnit = TimeUnit.SECONDS;
                    
                    return new BuilderPhaseC12();
                }
                
                public BuilderPhaseC12 minutes()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutUnit = TimeUnit.MINUTES;
                    
                    return new BuilderPhaseC12();
                }
                
                public BuilderPhaseC12 hours()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutUnit = TimeUnit.HOURS;
                    
                    return new BuilderPhaseC12();
                }
                
                public BuilderPhaseC12 days()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutUnit = TimeUnit.DAYS;
                    
                    return new BuilderPhaseC12();
                }
                
                public class BuilderPhaseC12
                {
                    private BuilderPhaseC12()
                    {
                        super();
                    }
                    
                    public BuilderPhaseX1 immediately()
                    {
                        return new BuilderPhaseX1();
                    }
                    
                    public BuilderPhaseD1.BuilderPhaseD2 notBeforeTheConsumeEvent()
                    {
                        return new BuilderPhaseD1().new BuilderPhaseD2();
                    }
                    
                    public BuilderPhaseC2 notBeforeTheMessageWaitsForAtLeast(final int waitTime)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerMode = TriggerByMessageAgeMode.ALL;
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerAge = waitTime;
                        return new BuilderPhaseC2();
                    }
                    
                    public BuilderPhaseC2 notBeforeOneOfTheMessagesWaitsForAtLeast(final int waitTime)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerMode = TriggerByMessageAgeMode.LEAST_ONE;
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerAge = waitTime;
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerCount = 1;
                        return new BuilderPhaseC2();
                    }
                    
                    public BuilderPhaseC3 notBefore(final int messgeSize)
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerCount = messgeSize;
                        return new BuilderPhaseC3();
                    }
                }
            }
            
            public BuilderPhaseX1 immediately()
            {
                return new BuilderPhaseX1();
            }
            
            public BuilderPhaseD1.BuilderPhaseD2 notBeforeTheConsumeEvent()
            {
                return new BuilderPhaseD1().new BuilderPhaseD2();
            }
            
            public BuilderPhaseC2 notBeforeTheMessageWaitsForAtLeast(final int waitTime)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerMode = TriggerByMessageAgeMode.ALL;
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerAge = waitTime;
                return new BuilderPhaseC2();
            }
            
            public BuilderPhaseC2 notBeforeOneOfTheMessagesWaitsForAtLeast(final int waitTime)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerMode = TriggerByMessageAgeMode.LEAST_ONE;
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerAge = waitTime;
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerCount = 1;
                return new BuilderPhaseC2();
            }
            
            public BuilderPhaseC3 notBefore(final int messgeSize)
            {
                FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerCount = messgeSize;
                return new BuilderPhaseC3();
            }
            
            public class BuilderPhaseC2
            {
                private BuilderPhaseC2()
                {
                    super();
                }
                
                public BuilderPhaseD1 milliSeconds()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerUnit = TimeUnit.MILLISECONDS;
                    
                    return new BuilderPhaseD1();
                }
                
                public BuilderPhaseD1 seconds()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerUnit = TimeUnit.SECONDS;
                    
                    return new BuilderPhaseD1();
                }
                
                public BuilderPhaseD1 minutes()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerUnit = TimeUnit.MINUTES;
                    
                    return new BuilderPhaseD1();
                }
                
                public BuilderPhaseD1 hours()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerUnit = TimeUnit.HOURS;
                    
                    return new BuilderPhaseD1();
                }
                
                public BuilderPhaseD1 days()
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerUnit = TimeUnit.DAYS;
                    
                    return new BuilderPhaseD1();
                }
            }
            
            public class BuilderPhaseC3
            {
                private BuilderPhaseC3()
                {
                    super();
                }
                
                public BuilderPhaseC2 messagesWaitForAtLeast(final int waitTime)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerMode = TriggerByMessageAgeMode.LEAST_X;
                    FeatureBuilder.this.feature.currentConsumerRule.messageAgeTriggerAge = waitTime;
                    return new BuilderPhaseC2();
                }
            }
        }
        
        public class BuilderPhaseD1 extends BuilderPhaseX1
        {
            private BuilderPhaseD1()
            {
                super();
            }
            
            public BuilderPhaseD2 andNotBeforeTheConsumeEvent()
            {
                return new BuilderPhaseD2();
            }
            
            public class BuilderPhaseD2
            {
                private BuilderPhaseD2()
                {
                    super();
                }
                
                public BuilderPhaseD3 ofGroup(final String group)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerGroup = group;
                    return new BuilderPhaseD3();
                }
                
                public BuilderPhaseD3.BuilderPhaseD4 eitherTookPlaceNeverOrTookPlace(final int waitTime)
                {
                    return new BuilderPhaseD3().eitherTookPlaceNeverOrTookPlace(waitTime);
                }
                
                public BuilderPhaseD3.BuilderPhaseD4 tookPlace(final int waitTime)
                {
                    return new BuilderPhaseD3().tookPlace(waitTime);
                }
                
                public class BuilderPhaseD3
                {
                    private BuilderPhaseD3()
                    {
                        super();
                    }
                    
                    public BuilderPhaseD4 eitherTookPlaceNeverOrTookPlace(int waitTime)
                    {
                        if (waitTime < 0)
                        {
                            waitTime = 0;
                        }
                        
                        FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerAge = waitTime;
                        FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerNeverMode = true;
                        
                        return new BuilderPhaseD4();
                    }
                    
                    public BuilderPhaseD4 tookPlace(int waitTime)
                    {
                        if (waitTime < 0)
                        {
                            waitTime = 0;
                        }
                        
                        FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerAge = waitTime;
                        FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerNeverMode = false;
                        
                        return new BuilderPhaseD4();
                    }
                    
                    public class BuilderPhaseD4
                    {
                        private BuilderPhaseD4()
                        {
                            super();
                        }
                        
                        public BuilderPhaseX1 milliSecondsAgo()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerUnit = TimeUnit.MILLISECONDS;
                            
                            return new BuilderPhaseX1();
                        }
                        
                        public BuilderPhaseX1 secondsAgo()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerUnit = TimeUnit.SECONDS;
                            
                            return new BuilderPhaseX1();
                        }
                        
                        public BuilderPhaseX1 minutesAgo()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerUnit = TimeUnit.MINUTES;
                            
                            return new BuilderPhaseX1();
                        }
                        
                        public BuilderPhaseX1 hoursAgo()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerUnit = TimeUnit.HOURS;
                            
                            return new BuilderPhaseX1();
                        }
                        
                        public BuilderPhaseX1 daysAgo()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.consumeEventAgeTriggerUnit = TimeUnit.DAYS;
                            
                            return new BuilderPhaseX1();
                        }
                    }
                }
            }
        }
        
        public class BuilderPhaseX1 extends BuilderPhaseZ1
        {
            
            private BuilderPhaseX1()
            {
                super();
            }
            
            public <E extends Throwable, T, H> BuilderPhaseX1 onError(final Class<E> clazz, final BiConsumer<E, MessageConsumeHelper<T, H>> errorConsumer)
            {
                FeatureBuilder.this.feature.currentConsumerRule.specialErrorHandler.add(new SpecialErrorHandlerDefinition(clazz, (BiConsumer) errorConsumer));
                return this;
            }
            
            public <E extends Throwable, T, H> BuilderPhaseX1 onError(final Class<E> clazz, final BiConsumer<E, MessageConsumeHelper<T, H>> errorConsumer, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.specialErrorHandler.add(new SpecialErrorHandlerDefinition(clazz, (BiConsumer) errorConsumer));
                return this;
            }
            
            public <E extends Throwable, T, H> BuilderPhaseX1 onError(final Class<E> clazz, final BiConsumer<E, MessageConsumeHelper<T, H>> errorConsumer, final Class<T> messageType, final Class<H> helperType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.specialErrorHandler.add(new SpecialErrorHandlerDefinition(clazz, (BiConsumer) errorConsumer));
                return this;
            }
            
            public <T, H> BuilderPhaseX2 onError(final BiConsumer<Throwable, MessageConsumeHelper<T, H>> errorConsumer)
            {
                FeatureBuilder.this.feature.currentConsumerRule.defaultErrorHandler = (BiConsumer) errorConsumer;
                return new BuilderPhaseX2();
            }
            
            public <T, H> BuilderPhaseX2 onError(final BiConsumer<Throwable, MessageConsumeHelper<T, H>> errorConsumer, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.defaultErrorHandler = (BiConsumer) errorConsumer;
                return new BuilderPhaseX2();
            }
            
            public <T, H> BuilderPhaseX2 onError(final BiConsumer<Throwable, MessageConsumeHelper<T, H>> errorConsumer, final Class<T> messageType, final Class<H> helperType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.defaultErrorHandler = (BiConsumer) errorConsumer;
                return new BuilderPhaseX2();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
            {
                FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                return new BuilderPhaseZ1();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                return new BuilderPhaseZ1();
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
            {
                FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                return new BuilderPhaseZ1();
            }
            
            public class BuilderPhaseX2 extends BuilderPhaseZ1
            {
                private BuilderPhaseX2()
                {
                    super();
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                    return new BuilderPhaseZ1();
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                    return new BuilderPhaseZ1();
                }
                
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public <T, H> BuilderPhaseZ1 onTimeout(final BiConsumer<IMessage<T>, MessageConsumeHelper<T, H>> messageConsumer, final Class<T> messageType, final Class<H> helperType)
                {
                    FeatureBuilder.this.feature.currentConsumerRule.timeOutHandler = (BiConsumer) messageConsumer;
                    return new BuilderPhaseZ1();
                }
            }
        }
        
        public class BuilderPhaseZ1
        {
            
            private BuilderPhaseZ1()
            {
                super();
            }
            
            public IChannelFeature buildFeature()
            {
                return FeatureBuilder.this.feature.copy(true);
            }
            
            public FeatureBuilder or()
            {
                FeatureBuilder.this.feature.currentConsumerRule = new ConsumerRule();
                FeatureBuilder.this.feature.consumerRuleList.add(FeatureBuilder.this.feature.currentConsumerRule);
                
                return FeatureBuilder.this;
            }
            
            public BuilderPhaseZ2.BuilderPhaseZ3 butKeepMessagesInChannel()
            {
                return new BuilderPhaseZ2().new BuilderPhaseZ3();
            }
            
            public BuilderPhaseZ2 definePoolAddress(final String address)
            {
                FeatureBuilder.this.feature.currentConsumerRule.poolAddress = address;
                return new BuilderPhaseZ2();
            }
            
            public class BuilderPhaseZ2
            {
                
                private BuilderPhaseZ2()
                {
                    super();
                }
                
                public IChannelFeature buildFeature()
                {
                    return FeatureBuilder.this.feature.copy(true);
                }
                
                public FeatureBuilder or()
                {
                    FeatureBuilder.this.feature.currentConsumerRule = new ConsumerRule();
                    FeatureBuilder.this.feature.consumerRuleList.add(FeatureBuilder.this.feature.currentConsumerRule);
                    
                    return FeatureBuilder.this;
                }
                
                public BuilderPhaseZ3 butKeepMessagesInChannel()
                {
                    return new BuilderPhaseZ3();
                }
                
                public class BuilderPhaseZ3
                {
                    private BuilderPhaseZ3()
                    {
                        super();
                    }
                    
                    public BuilderPhaseZ4 markConsumedMessagesAsDone()
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.keepMessagesMode = MessageConsumerFeature.this.KEEP_MESSAGE_MODE_MESSAGES_CONSUMED;
                        return new BuilderPhaseZ4();
                    }
                    
                    public BuilderPhaseZ4 markProcessedMessagesAsDone()
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.keepMessagesMode = MessageConsumerFeature.this.KEEP_MESSAGE_MODE_MESSAGES_PROCESSED;
                        return new BuilderPhaseZ4();
                    }
                    
                    public BuilderPhaseZ4 markConsumedMessagesByRuleAsDone()
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.keepMessagesMode = MessageConsumerFeature.this.KEEP_MESSAGE_MODE_MESSAGES_CONSUMED_BY_RULE;
                        return new BuilderPhaseZ4();
                    }
                    
                    public BuilderPhaseZ4 markProcessedMessageByRulesAsDone()
                    {
                        FeatureBuilder.this.feature.currentConsumerRule.keepMessagesMode = MessageConsumerFeature.this.KEEP_MESSAGE_MODE_MESSAGES_PROCESSED_BY_RULE;
                        return new BuilderPhaseZ4();
                    }
                    
                    public class BuilderPhaseZ4
                    {
                        
                        private BuilderPhaseZ4()
                        {
                            super();
                        }
                        
                        public BuilderPhaseZ5 andYesIKnowWhatThisMeans()
                        {
                            FeatureBuilder.this.feature.currentConsumerRule.keepMessages = true;
                            return new BuilderPhaseZ5();
                        }
                        
                        public class BuilderPhaseZ5
                        {
                            
                            private BuilderPhaseZ5()
                            {
                                super();
                            }
                            
                            public IChannelFeature buildFeature()
                            {
                                return FeatureBuilder.this.feature.copy(true);
                            }
                            
                            public FeatureBuilder or()
                            {
                                FeatureBuilder.this.feature.currentConsumerRule = new ConsumerRule();
                                FeatureBuilder.this.feature.consumerRuleList.add(FeatureBuilder.this.feature.currentConsumerRule);
                                
                                return FeatureBuilder.this;
                            }
                        }
                    }
                }
            }
            
        }
    }
    
    public static class SpecialErrorHandlerDefinition
    {
        public SpecialErrorHandlerDefinition(final Class type, final BiConsumer<Throwable, MessageConsumeHelper> handler)
        {
            super();
            this.type = type;
            this.handler = handler;
        }
        
        private final Class type;
        private final BiConsumer<Throwable, MessageConsumeHelper> handler;
        
        public Class getType()
        {
            return this.type;
        }
        
        public BiConsumer<Throwable, MessageConsumeHelper> getHandler()
        {
            return this.handler;
        }
    }
    
    public static class ConsumerRule
    {
        public enum TriggerByMessageAgeMode
        {NONE, ALL, LEAST_ONE, LEAST_X}
        
        public static final String ANY_GROUP = "CONSUMER_ALL_GROUP";
        
        private final UUID id = UUID.randomUUID();
        
        // pool
        private Predicate<IMessage> poolFilter = null;
        private BiFunction<IMessage, IMessage, Boolean> replaceOlderMessageFilter = null;
        private int poolMinSize = 1;
        private int poolMaxSize = Short.MAX_VALUE;
        
        // consume event trigger
        private String consumeEventAgeTriggerGroup = null;
        private int consumeEventAgeTriggerAge = -1;
        private boolean consumeEventAgeTriggerNeverMode = false;
        private TimeUnit consumeEventAgeTriggerUnit = TimeUnit.SECONDS;
        
        // message age trigger
        private TriggerByMessageAgeMode messageAgeTriggerMode = TriggerByMessageAgeMode.NONE;
        private int messageAgeTriggerCount = -1;
        private int messageAgeTriggerAge = -1;
        private TimeUnit messageAgeTriggerUnit = TimeUnit.SECONDS;
        
        // consumer
        private BiConsumer<IMessage<?>, MessageConsumeHelper<?, ?>> messageConsumer = null;
        private Set<String> groupMembers = new HashSet<String>();
        
        // timeout
        private int timeOut = -1;
        private TimeUnit timeOutUnit = TimeUnit.SECONDS;
        private BiConsumer<IMessage<?>, MessageConsumeHelper<?, ?>> timeOutHandler = null;
        
        // error
        private BiConsumer<Throwable, MessageConsumeHelper> defaultErrorHandler = null;
        private List<SpecialErrorHandlerDefinition> specialErrorHandler = new ArrayList<>();
        
        // keep messages in channel
        private boolean keepMessages = false;
        private Integer keepMessagesMode = null;
        
        private String poolAddress = null;
        
        private ConsumerRule copy()
        {
            ConsumerRule consumerRule = new ConsumerRule();
            consumerRule.poolFilter = this.poolFilter;
            consumerRule.replaceOlderMessageFilter = this.replaceOlderMessageFilter;
            consumerRule.poolMinSize = this.poolMinSize;
            consumerRule.poolMaxSize = this.poolMaxSize;
            consumerRule.consumeEventAgeTriggerGroup = this.consumeEventAgeTriggerGroup;
            consumerRule.consumeEventAgeTriggerAge = this.consumeEventAgeTriggerAge;
            consumerRule.consumeEventAgeTriggerNeverMode = this.consumeEventAgeTriggerNeverMode;
            consumerRule.consumeEventAgeTriggerUnit = this.consumeEventAgeTriggerUnit;
            consumerRule.messageConsumer = this.messageConsumer;
            consumerRule.timeOut = this.timeOut;
            consumerRule.timeOutUnit = this.timeOutUnit;
            consumerRule.groupMembers = new HashSet<String>(this.groupMembers);
            consumerRule.messageAgeTriggerMode = this.messageAgeTriggerMode;
            consumerRule.messageAgeTriggerCount = this.messageAgeTriggerCount;
            consumerRule.messageAgeTriggerAge = this.messageAgeTriggerAge;
            consumerRule.messageAgeTriggerUnit = this.messageAgeTriggerUnit;
            consumerRule.defaultErrorHandler = this.defaultErrorHandler;
            consumerRule.specialErrorHandler = new ArrayList<>(this.specialErrorHandler);
            consumerRule.timeOutHandler = this.timeOutHandler;
            consumerRule.keepMessages = this.keepMessages;
            consumerRule.keepMessagesMode = this.keepMessagesMode;
            consumerRule.poolAddress = this.poolAddress;
            return consumerRule;
            
        }
        
        public UUID getId()
        {
            return this.id;
        }
        
        public Predicate<IMessage> getPoolFilter()
        {
            return this.poolFilter;
        }
        
        public BiFunction<IMessage, IMessage, Boolean> getReplaceOlderMessageFilter()
        {
            return this.replaceOlderMessageFilter;
        }
        
        public int getPoolMinSize()
        {
            return this.poolMinSize;
        }
        
        public int getPoolMaxSize()
        {
            return this.poolMaxSize;
        }
        
        public String getConsumeEventAgeTriggerGroup()
        {
            return this.consumeEventAgeTriggerGroup;
        }
        
        public int getConsumeEventAgeTriggerAge()
        {
            return this.consumeEventAgeTriggerAge;
        }
        
        public boolean isConsumeEventAgeTriggerNeverMode()
        {
            return this.consumeEventAgeTriggerNeverMode;
        }
        
        public TimeUnit getConsumeEventAgeTriggerUnit()
        {
            return this.consumeEventAgeTriggerUnit;
        }
        
        public BiConsumer<IMessage<?>, MessageConsumeHelper<?, ?>> getMessageConsumer()
        {
            return this.messageConsumer;
        }
        
        public Set<String> getGroupMembers()
        {
            return this.groupMembers;
        }
        
        public TriggerByMessageAgeMode getMessageAgeTriggerMode()
        {
            return this.messageAgeTriggerMode;
        }
        
        public int getMessageAgeTriggerCount()
        {
            return this.messageAgeTriggerCount;
        }
        
        public int getMessageAgeTriggerAge()
        {
            return this.messageAgeTriggerAge;
        }
        
        public TimeUnit getMessageAgeTriggerUnit()
        {
            return this.messageAgeTriggerUnit;
        }
        
        public int getTimeOut()
        {
            return this.timeOut;
        }
        
        public TimeUnit getTimeOutUnit()
        {
            return this.timeOutUnit;
        }
        
        public BiConsumer<Throwable, MessageConsumeHelper> getDefaultErrorHandler()
        {
            return this.defaultErrorHandler;
        }
        
        public List<SpecialErrorHandlerDefinition> getSpecialErrorHandler()
        {
            return this.specialErrorHandler;
        }
        
        public BiConsumer<IMessage<?>, MessageConsumeHelper<?, ?>> getTimeOutHandler()
        {
            return this.timeOutHandler;
        }
        
        public boolean isKeepMessages()
        {
            return this.keepMessages;
        }
        
        public Integer getKeepMessagesMode()
        {
            return this.keepMessagesMode;
        }
        
        public String getPoolAddress()
        {
            return this.poolAddress;
        }
        
    }
    
    public class MessageConsumerFeatureConfiguration implements IPreparedChannelFeature
    {
        private List<ConsumerRule> consumerRuleList = null;
        private ConsumerRule currentConsumerRule = null;
        
        private MessageConsumerFeatureConfiguration()
        {
            super();
            this.consumerRuleList = new ArrayList<MessageConsumerFeature.ConsumerRule>();
            this.currentConsumerRule = new ConsumerRule();
            this.consumerRuleList.add(this.currentConsumerRule);
        }
        
        private MessageConsumerFeatureConfiguration(final List<ConsumerRule> consumerRuleList, final boolean immutable)
        {
            super();
            this.consumerRuleList = new ArrayList<MessageConsumerFeature.ConsumerRule>();
            for (final ConsumerRule consumerRule : consumerRuleList)
            {
                this.consumerRuleList.add(consumerRule.copy());
            }
            if (immutable)
            {
                for (final ConsumerRule consumerRule : this.consumerRuleList)
                {
                    String privateGroup = "CONSUMER_PRIVATE_GROUP_" + consumerRule.id.toString();
                    if ((consumerRule.consumeEventAgeTriggerAge > -1) && (consumerRule.consumeEventAgeTriggerGroup == null))
                    {
                        consumerRule.consumeEventAgeTriggerGroup = privateGroup;
                    }
                    consumerRule.groupMembers.add(privateGroup);
                    consumerRule.groupMembers.add(ConsumerRule.ANY_GROUP);
                    
                    consumerRule.groupMembers = Collections.unmodifiableSet(consumerRule.groupMembers);
                    consumerRule.specialErrorHandler = Collections.unmodifiableList(consumerRule.specialErrorHandler);
                }
                this.consumerRuleList = Collections.unmodifiableList(this.consumerRuleList);
            }
        }
        
        @Override
        public void applyToChannel(final IDispatcherChannel<?> channel)
        {
            Map<String, Object> configurationProperties = new HashMap<>();
            configurationProperties.put(MessageConsumerFeatureConfiguration.class.getCanonicalName(), this);
            UUID uuid = UUID.randomUUID();
            channel.createChildScope(uuid, "Message Consumer Planner Scope " + uuid, configurationProperties, null);
        }
        
        private MessageConsumerFeatureConfiguration copy(final boolean immutable)
        {
            return new MessageConsumerFeatureConfiguration(this.consumerRuleList, immutable);
        }
        
        public List<ConsumerRule> getConsumerRuleList()
        {
            return this.consumerRuleList;
        }
        
    }
    
    public interface IPoolController
    {
        void consumeMessages(String poolAddress);
        
        static void consumeMessages(final IDispatcherChannel<?> channel, final String poolAddress)
        {
            IPoolController poolController = channel.getStateAdapter(ConsumeMessagesConsumerManagerAdapter.class);
            if (poolController != null)
            {
                poolController.consumeMessages(poolAddress);
            }
        }
    }
    
}
