/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannel.IDispatcherChannelReference;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcher;
import org.sodeac.common.message.dispatcher.api.IMessageDispatcherManager;
import org.sodeac.common.message.dispatcher.api.IOnMessageStore;
import org.sodeac.common.message.dispatcher.setup.MessageDispatcherChannelSetup;
import org.sodeac.common.misc.TaskDoneNotifier;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DispatcherTest
{
    public static final String TEST_DISPATCHER_ID = "org.sodeac.common.message.dispatcher.test";
    
    @Test
    public void test00001RawMessageStoreTest() throws InterruptedException
    {
        IMessageDispatcher dispatcher = IMessageDispatcherManager.get().getOrCreateDispatcher(UUID.randomUUID().toString());
        try
        {
            TaskDoneNotifier taskDoneNotifier = new TaskDoneNotifier();
            dispatcher.registerChannelManager(new SimpleManagerTest001(taskDoneNotifier));
            
            dispatcher.sendMessage(SimpleManagerTest001.CHANNEL_ID, "ABC");
            dispatcher.sendMessage(SimpleManagerTest001.CHANNEL_ID, "XYZ");
            
            assertFalse("correct msg stored, but should not", taskDoneNotifier.await(1, TimeUnit.SECONDS));
            
            dispatcher.sendMessage(SimpleManagerTest001.CHANNEL_ID, SimpleManagerTest001.MSG);
            
            assertTrue("correct msg not stored, but should", taskDoneNotifier.await(3, TimeUnit.SECONDS));
            
            Thread.sleep(1080);
        }
        finally
        {
            dispatcher.shutdown();
        }
        
        assertTrue("dispatcher should be stopped", ((MessageDispatcherImpl) dispatcher).isStopped());
    }
    
    private class SimpleManagerTest001 implements IDispatcherChannelManager, IOnMessageStore
    {
        public static final String CHANNEL_ID = "SimpleManagerTest001";
        public static final String MSG = "I'm a message";
        
        private TaskDoneNotifier taskDoneNotifier = null;
        
        public SimpleManagerTest001(final TaskDoneNotifier taskDoneNotifier)
        {
            super();
            this.taskDoneNotifier = taskDoneNotifier;
        }
        
        @Override
        public void configureChannelManagerPolicy(final IChannelManagerPolicy configurationPolicy)
        {
            configurationPolicy.addConfigurationDetail(new ComponentBindingSetup.BoundedByChannelId(CHANNEL_ID).setChannelMaster(true).setName("Test SimpleManagerTest001"));
        }
        
        @Override
        public void onMessageStore(final IMessage message)
        {
            if (MSG.equals(message.getPayload()))
            {
                this.taskDoneNotifier.setTaskDone();
            }
            message.removeFromChannel();
        }
    }
    
    @Test
    public void test00002CreateAndClose() throws IOException
    {
        String channelID = "test00002CreateAndClose";
        IDispatcherChannelReference channelCloser = MessageDispatcherChannelSetup.create()
                                                                                 .preparedBuilder().inManagedDispatcher(TEST_DISPATCHER_ID).underTheName("Only a test").buildChannelWithId(channelID);
        
        assertNotNull("channel should not be null", IMessageDispatcherManager.get().getOrCreateDispatcher(TEST_DISPATCHER_ID).getChannel(channelID));
        
        channelCloser.close();
        
        assertNull("channel should be null", IMessageDispatcherManager.get().getOrCreateDispatcher(TEST_DISPATCHER_ID).getChannel(channelID));
    }
}
