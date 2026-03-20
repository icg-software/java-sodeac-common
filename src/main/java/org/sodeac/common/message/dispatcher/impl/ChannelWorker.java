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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelService;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelWorker;
import org.sodeac.common.message.dispatcher.api.IMessage;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelSignal;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemove;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemoveSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnMessageStore;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreResult;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnTaskDone;
import org.sodeac.common.message.dispatcher.api.IOnTaskError;
import org.sodeac.common.message.dispatcher.api.IOnTaskTimeout;
import org.sodeac.common.message.dispatcher.api.IPeriodicChannelTask;
import org.sodeac.common.message.dispatcher.api.ITaskControl.ExecutionTimestampSource;
import org.sodeac.common.message.dispatcher.impl.TaskControlImpl.PeriodicServiceTimestampPredicate;
import org.sodeac.common.snapdeque.DequeSnapshot;

public class ChannelWorker extends Thread
{
    public static final long DEFAULT_WAIT_TIME = 108 * 108 * 108 * 7;
    public static final long FREE_TIME = 108 + 27;
    public static final long RESCHEDULE_BUFFER_TIME = 27;
    public static final long DEFAULT_SHUTDOWN_TIME = 1080 * 54;

    private long spoolTimeStamp = 0;

    private ChannelImpl<?> channel = null;
    private IDispatcherChannelWorker workerWrapper = null;
    private volatile boolean go = true;
    protected volatile boolean isUpdateNotified = false;
    protected volatile boolean isSoftUpdated = false;
    private final Object waitMonitor = new Object();

    private List<TaskContainer> dueTaskList = null;

    private volatile Long currentTimeOutTimeStamp = null;
    private volatile TaskContainer currentRunningTask = null;
    private volatile long wakeUpTimeStamp = -1;
    private volatile boolean inFreeingArea = false;

    private ChannelTaskContextImpl context = null;

    private final Logger logger = LoggerFactory.getLogger(ChannelWorker.class);

    protected ChannelWorker(final ChannelImpl<?> impl)
    {
        super();
        this.channel = impl;
        this.workerWrapper = new ChannelWorkerWrapper(this);
        this.dueTaskList = new ArrayList<TaskContainer>();
        this.context = new ChannelTaskContextImpl(this.dueTaskList);
        this.context.setChannel(this.channel);
        super.setDaemon(true);
        super.setName(ChannelWorker.class.getSimpleName() + " " + this.channel.getId());
    }

    private void checkQueueAttach()
    {
        try
        {
            final DequeSnapshot<IOnChannelAttach> onQueueAttachSnapshot = this.channel.getOnQueueAttachList();
            if(onQueueAttachSnapshot == null)
            {
                return;
            }
            try
            {

                for (final IOnChannelAttach onQueueAttach : onQueueAttachSnapshot)
                {
                    try
                    {
                        onQueueAttach.onChannelAttach(this.channel);
                    }
                    catch (final Exception e)
                    {
                        this.logger.error("Exception on on-create() event controller", e);
                    }
                }
            }
            finally
            {
                onQueueAttachSnapshot.close();
            }
        }
        catch (final Exception e)
        {
            this.logger.error("Exception while check queueAttach", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
        final Set<IOnMessageStoreResult> scheduledResultSet = new HashSet<IOnMessageStoreResult>();
        final Set<String> signalProcessed = new HashSet<String>();
        DequeSnapshot<? extends IMessage> newMessagesSnapshot;
        DequeSnapshot<? extends IMessage> removedMessagesSnapshot;
        while (this.go)
        {
            checkQueueAttach();

            synchronized (this.waitMonitor)
            {
                this.isUpdateNotified = false;
                this.isSoftUpdated = false;
            }

            this.channel.closeWorkerSnapshots();

            try
            {
                removedMessagesSnapshot = this.channel.getRemovedMessagesSnapshot();
                try
                {
                    if((removedMessagesSnapshot != null) && (!removedMessagesSnapshot.isEmpty()))
                    {
                        checkQueueAttach();

                        for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                        {
                            try
                            {
                                if(this.go && conf.isImplementingIOnMessageRemoveSnapshot())
                                {
                                    ((IOnMessageRemoveSnapshot) conf.getChannelManager()).onMessageRemoveSnapshot(removedMessagesSnapshot);
                                }
                            }
                            catch (final Exception ignored) { }
                        }

                        for (final MessageImpl message : (DequeSnapshot<MessageImpl>) removedMessagesSnapshot)
                        {
                            this.channel.touchLastWorkerAction();
                            for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                            {
                                try
                                {
                                    if(this.go && conf.isImplementingIOnMessageRemove())
                                    {
                                        ((IOnMessageRemove) conf.getChannelManager()).onMessageRemove(message);
                                    }
                                }
                                catch (final Exception ignored) { }
                            }
                        }

                        for (final MessageImpl message : (DequeSnapshot<MessageImpl>) removedMessagesSnapshot)
                        {
                            try
                            {
                                message.dispose();
                            }
                            catch (final Exception ignored) { }
                        }
                    }
                }
                finally
                {
                    if(removedMessagesSnapshot != null)
                    {
                        try
                        {
                            removedMessagesSnapshot.close();
                        }
                        finally
                        {
                            removedMessagesSnapshot = null;
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                this.logger.error("Exception while process removedEventList", e);
            }
            catch (final Error e)
            {
                this.logger.error("Error while process removedEventList", e);
                throw e;
            }

            try
            {
                newMessagesSnapshot = this.channel.getNewScheduledEventsSnaphot();
                try
                {
                    if((newMessagesSnapshot != null) && (!newMessagesSnapshot.isEmpty()))
                    {
                        checkQueueAttach();

                        boolean onMessageStoredSingle = false;
                        boolean onMessageStoredSnapshot = false;

                        for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                        {
                            if(conf.isImplementingIOnMessageStore())
                            {
                                onMessageStoredSingle = true;
                            }
                            if(conf.isImplementingIOnMessageStoreSnapshot())
                            {
                                onMessageStoredSnapshot = true;
                            }
                        }

                        if(onMessageStoredSingle || onMessageStoredSnapshot)
                        {
                            scheduledResultSet.clear();
                            for (final IMessage<?> event : newMessagesSnapshot)
                            {
                                try
                                {
                                    scheduledResultSet.add(event.getScheduleResultObject());
                                }
                                catch (final Exception ignored) { }
                            }

                            if(onMessageStoredSnapshot)
                            {
                                this.channel.touchLastWorkerAction();
                                for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                                {
                                    if(this.go && conf.isImplementingIOnMessageStoreSnapshot())
                                    {
                                        try
                                        {
                                            ((IOnMessageStoreSnapshot) conf.getChannelManager()).onMessageStoreSnapshot(newMessagesSnapshot);
                                        }
                                        catch (final Exception ignored) { }
                                    }

                                }
                            }

                            if(onMessageStoredSingle)
                            {
                                for (final MessageImpl<?> message : (DequeSnapshot<MessageImpl<?>>) newMessagesSnapshot)
                                {
                                    this.channel.touchLastWorkerAction();
                                    for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                                    {
                                        try
                                        {
                                            if(this.go && conf.isImplementingIOnMessageStore())
                                            {
                                                ((IOnMessageStore) conf.getChannelManager()).onMessageStore(message);
                                            }
                                        }
                                        catch (final Exception e)
                                        {
                                            try
                                            {
                                                message.getScheduleResultObject().addError(e);
                                            }
                                            catch (final Exception ignored) { }
                                        }
                                        catch (final Error e)
                                        {
                                            try
                                            {
                                                message.getScheduleResultObject().addError(e);
                                            }
                                            catch (final Exception ignored) { }
                                            throw e;
                                        }
                                    }
                                }
                            }

                            for (final IOnMessageStoreResult scheduleResult : scheduledResultSet)
                            {
                                try
                                {
                                    ((PublishMessageResultImpl) scheduleResult).processPhaseIsFinished();
                                }
                                catch (final Exception ignored) { }
                            }
                            for (final MessageImpl<?> event : (DequeSnapshot<MessageImpl<?>>) newMessagesSnapshot)
                            {
                                try
                                {
                                    event.setScheduleResultObject(null);
                                }
                                catch (final Exception ignored) { }
                            }

                            scheduledResultSet.clear();
                        }
                    }
                }
                finally
                {
                    if(newMessagesSnapshot != null)
                    {
                        try
                        {
                            newMessagesSnapshot.close();
                        }
                        finally
                        {
                            newMessagesSnapshot = null;
                        }
                        scheduledResultSet.clear();
                    }
                }
            }
            catch (final Exception e)
            {
                this.logger.error("Exception while process newScheduledList", e);
            }
            catch (final Error e)
            {
                this.logger.error("Error while process newScheduledList", e);
                throw e;
            }

            try
            {
                final DequeSnapshot<String> signalSnapshot = this.channel.getSignalsSnapshot();
                try
                {
                    if((signalSnapshot != null) && (!signalSnapshot.isEmpty()))
                    {
                        checkQueueAttach();

                        signalProcessed.clear();
                        for (final String signal : signalSnapshot)
                        {
                            if(signalProcessed.contains(signal))
                            {
                                continue;
                            }
                            this.channel.touchLastWorkerAction();
                            for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                            {
                                try
                                {

                                    if(this.go && conf.isImplementingIOnChannelSignal())
                                    {
                                        ((IOnChannelSignal) conf.getChannelManager()).onChannelSignal(this.channel, signal);
                                    }
                                }
                                catch (final Exception e)
                                {
                                    this.logger.error("Exception while process signal", e);
                                }
                                catch (final Error e)
                                {
                                    this.logger.error("Error while process signal", e);
                                    throw e;
                                }
                            }

                            signalProcessed.add(signal);
                        }
                        signalProcessed.clear();
                    }
                }
                finally
                {
                    if(signalSnapshot != null)
                    {
                        try
                        {
                            signalSnapshot.close();
                        }
                        catch (final Exception ignored) { }
                    }
                }
            }
            catch (final Exception e)
            {
                this.logger.error("Exception while process signalList", e);
            }
            catch (final Error e)
            {
                this.logger.error("Error while process signalList", e);
                throw e;
            }

            this.dueTaskList.clear();
            this.channel.getDueTasks(this.dueTaskList);

            if(!this.dueTaskList.isEmpty())
            {
                checkQueueAttach();

                this.channel.touchLastWorkerAction();
                boolean taskTimeOut = false;
                for (final TaskContainer dueTask : this.dueTaskList)
                {
                    try
                    {
                        if(dueTask.getTaskControl().isDone())
                        {
                            continue;
                        }
                        if(this.go)
                        {
                            this.context.resetCurrentProcessedTaskList();

                            try
                            {
                                taskTimeOut = ((dueTask.getTaskControl().getTimeout() > 0) || (dueTask.getTaskControl().getHeartbeatTimeout() > 0));
                                this.currentRunningTask = dueTask;

                                if(dueTask.getTask() instanceof IPeriodicChannelTask)
                                {
                                    Long periodicRepetitionInterval = ((IPeriodicChannelTask) dueTask.getTask()).getPeriodicRepetitionInterval();
                                    if((periodicRepetitionInterval == null) || (periodicRepetitionInterval.longValue() < 1))
                                    {
                                        periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
                                    }
                                    dueTask.getTaskControl().setExecutionTimeStamp
                                            (
                                                    System.currentTimeMillis() + periodicRepetitionInterval,
                                                    ExecutionTimestampSource.PERODIC,
                                                    PeriodicServiceTimestampPredicate.getInstance()
                                            );
                                    dueTask.getTaskControl().preRunPeriodicTask();
                                }
                                else if(dueTask.getTask() instanceof IDispatcherChannelService)
                                {
                                    long periodicRepetitionInterval = -1L;

                                    try
                                    {
                                        if(dueTask.getPropertyBlock().getProperty(ChannelImpl.PROPERTY_PERIODIC_REPETITION_INTERVAL) != null)
                                        {
                                            final Object pri = dueTask.getPropertyBlock().getProperty(ChannelImpl.PROPERTY_PERIODIC_REPETITION_INTERVAL);
                                            if(pri instanceof String)
                                            {
                                                periodicRepetitionInterval = Long.parseLong(((String) pri).trim());
                                            }
                                            else if(pri instanceof Integer)
                                            {
                                                periodicRepetitionInterval = ((Integer) pri);
                                            }
                                            else
                                            {
                                                periodicRepetitionInterval = ((Long) pri);
                                            }
                                        }
                                    }
                                    catch (final Exception ignored) { }

                                    if(periodicRepetitionInterval < 1)
                                    {
                                        periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
                                    }
                                    dueTask.getTaskControl().setExecutionTimeStamp
                                            (
                                                    System.currentTimeMillis() + periodicRepetitionInterval,
                                                    ExecutionTimestampSource.PERODIC,
                                                    PeriodicServiceTimestampPredicate.getInstance()
                                            );
                                    dueTask.getTaskControl().preRunPeriodicTask();
                                }
                                else
                                {
                                    dueTask.getTaskControl().preRun();
                                }

                                if(taskTimeOut)
                                {
                                    if(dueTask.getTaskControl().getTimeout() > 0)
                                    {
                                        this.currentTimeOutTimeStamp = System.currentTimeMillis() + dueTask.getTaskControl().getTimeout();
                                    }
                                    this.context.setDueTask(dueTask);
                                    dueTask.heartbeat();
                                    this.channel.getMessageDispatcher().registerTimeOut(this.channel, dueTask);
                                }
                                else
                                {
                                    this.context.setDueTask(dueTask);
                                    dueTask.heartbeat();
                                }

                                //
                                //	run task or service
                                //

                                dueTask.getTask().run(this.context);

                                if(this.go)
                                {
                                    dueTask.getPropertyBlock().setProperty(ChannelImpl.PROPERTY_KEY_THROWED_EXCEPTION, null);

                                    dueTask.getTaskControl().postRun();

                                    this.currentTimeOutTimeStamp = null;
                                    this.currentRunningTask = null;
                                    if(taskTimeOut)
                                    {
                                        try
                                        {
                                            this.channel.getMessageDispatcher().unregisterTimeOut(this.channel, dueTask);
                                        }
                                        catch (final Exception e)
                                        {
                                            this.logger.error("eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask)", e);
                                        }
                                    }
                                }
                                else
                                {
                                    this.channel.closeWorkerSnapshots();
                                    return;
                                }
                            }
                            catch (final Exception e)
                            {
                                final TaskContainer runningTask = this.currentRunningTask;
                                this.currentTimeOutTimeStamp = null;
                                this.currentRunningTask = null;

                                if(runningTask != null) { runningTask.getPropertyBlock().setProperty(ChannelImpl.PROPERTY_KEY_THROWED_EXCEPTION, e); }
                                this.logger.error("Exception while process task " + dueTask.getTask(), e);

                                dueTask.getTaskControl().postRun();
                                if(taskTimeOut)
                                {
                                    try
                                    {
                                        this.channel.getMessageDispatcher().unregisterTimeOut(this.channel, dueTask);
                                    }
                                    catch (final Exception e2)
                                    {
                                        this.logger.error("eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask)", e2);
                                    }
                                }

                                if(!(dueTask.getTask() instanceof IDispatcherChannelService))
                                {
                                    dueTask.getTaskControl().setDone();
                                }

                                if(!this.go)
                                {
                                    this.channel.closeWorkerSnapshots();
                                    return;
                                }

                                try
                                {
                                    for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                                    {
                                        if(conf.isImplementingIOnTaskError())
                                        {
                                            try
                                            {
                                                ((IOnTaskError) conf.getChannelManager()).onTaskError(this.channel, dueTask.getTask(), e);
                                            }
                                            catch (final Exception ie)
                                            {
                                                this.logger.error("Error while process onTaskError " + dueTask, ie);
                                            }
                                        }
                                    }
                                }
                                catch (final Exception ie)
                                {
                                    this.logger.error("Error while process onTaskError " + dueTask, ie);
                                }

                            }
                            catch (final Error e)
                            {
                                final TaskContainer runningTask = this.currentRunningTask;
                                this.currentTimeOutTimeStamp = null;
                                this.currentRunningTask = null;

                                final Exception exc = new Exception(e.getMessage(), e);

                                if(runningTask != null) { runningTask.getPropertyBlock().setProperty(ChannelImpl.PROPERTY_KEY_THROWED_EXCEPTION, exc); }
                                this.logger.error("Error while process task " + dueTask.getTask(), e);

                                dueTask.getTaskControl().postRun();
                                if(taskTimeOut)
                                {
                                    try
                                    {
                                        this.channel.getMessageDispatcher().unregisterTimeOut(this.channel, dueTask);
                                    }
                                    catch (final Exception e2)
                                    {
                                        this.logger.error("eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask)", e2);
                                    }
                                }
                                if(!(dueTask.getTask() instanceof IDispatcherChannelService))
                                {
                                    dueTask.getTaskControl().setDone();
                                }

                                try
                                {
                                    for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                                    {
                                        if(conf.isImplementingIOnTaskError())
                                        {
                                            try
                                            {
                                                ((IOnTaskError) conf.getChannelManager()).onTaskError(this.channel, dueTask.getTask(), exc);
                                            }
                                            catch (final Exception ie)
                                            {
                                                this.logger.error("Error while process onTaskError " + dueTask, ie);
                                            }
                                        }
                                    }
                                }
                                catch (final Exception ie)
                                {
                                    this.logger.error("Error while process onTaskError " + dueTask, ie);
                                }

                                throw e;
                            }

                            this.currentTimeOutTimeStamp = null;
                            this.currentRunningTask = null;

                            if(!this.go)
                            {
                                this.channel.closeWorkerSnapshots();
                                return;
                            }

                            if(dueTask.getTaskControl().isDone())
                            {
                                for (final ChannelManagerContainer conf : this.channel.getManagerContainerList())
                                {
                                    try
                                    {
                                        if(this.go)
                                        {
                                            if(conf.isImplementingIOnTaskDone())
                                            {
                                                ((IOnTaskDone) conf.getChannelManager()).onTaskDone(this.channel, dueTask.getTask());
                                            }
                                        }
                                    }
                                    catch (final Exception ignored) { }
                                }
                            }
                        }
                    }
                    catch (final Exception e)
                    {
                        try
                        {
                            if(!(dueTask.getTask() instanceof IDispatcherChannelService))
                            {
                                dueTask.getTaskControl().setDone();
                            }
                        }
                        catch (final Exception ignored) { }
                        this.logger.error("Exception while process currentProcessedTaskList", e);
                    }

                }
            }

            this.channel.cleanDoneTasks();
            this.channel.closeWorkerSnapshots();

            try
            {
                boolean shutdownWorker = false;
                if(System.currentTimeMillis() > (this.channel.getLastWorkerAction() + DEFAULT_SHUTDOWN_TIME))
                {
                    this.inFreeingArea = true;
                    shutdownWorker = this.channel.checkWorkerShutdown(this);
                    if(!shutdownWorker)
                    {
                        this.inFreeingArea = false;
                    }
                }

                if(shutdownWorker)
                {
                    synchronized (this.waitMonitor)
                    {
                        while ((this.channel == null) && (this.go))
                        {
                            try
                            {
                                this.wakeUpTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME;
                                this.waitMonitor.wait(DEFAULT_WAIT_TIME);
                                this.wakeUpTimeStamp = -1;
                            }
                            catch (final InterruptedException e)
                            {
                                this.go = false;
                                // not this.interrupt() bc it will try to interrupt again
                                Thread.currentThread().interrupt();
                            }
                        }

                        this.inFreeingArea = false;

                        if(!this.go)
                        {
                            return;
                        }

                        this.channel.touchLastWorkerAction();

                        continue;
                    }
                }

                checkQueueAttach();

                if(this.go && this.isUpdateNotified)
                {
                    this.wakeUpTimeStamp = -1;
                    this.isUpdateNotified = false;
                    continue;
                }

                long nextRunTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME;
                try
                {
                    nextRunTimeStamp = this.channel.getNextRun();
                }
                catch (final Exception e)
                {
                    this.logger.error("Exception recalc next runtime ", e);
                }
				
				/*boolean freeWorker = false;
				long waitTime = nextRunTimeStamp - System.currentTimeMillis();
				if(waitTime > DEFAULT_WAIT_TIME)
				{
					waitTime = DEFAULT_WAIT_TIME;
				}
				if(waitTime > 0)
				{
					this.inFreeingArea = true;
					if(waitTime >= FREE_TIME)
					{
						freeWorker = this.channel.checkFreeWorker(this, nextRunTimeStamp);
					}
				}*/

                synchronized (this.waitMonitor)
                {
                    if(this.go)
                    {
                        this.wakeUpTimeStamp = -1;

                        if(this.isUpdateNotified)
                        {
                            this.isUpdateNotified = false;
                            continue;
                        }

                        long waitTime = nextRunTimeStamp - System.currentTimeMillis();
                        if(waitTime > DEFAULT_WAIT_TIME)
                        {
                            waitTime = DEFAULT_WAIT_TIME;
                        }
                        if(waitTime > 0)
                        {
                            boolean freeWorker = false;
                            if(!this.isSoftUpdated)
                            {
                                this.inFreeingArea = true;
                                if(waitTime >= FREE_TIME)
                                {
                                    freeWorker = this.channel.checkFreeWorker(this, nextRunTimeStamp);                        // TODO Problem ???
                                }
                            }
                            if(freeWorker)
                            {
                                while ((this.channel == null) && (this.go))
                                {
                                    try
                                    {
                                        this.wakeUpTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME;
                                        this.waitMonitor.wait(DEFAULT_WAIT_TIME);
                                        this.wakeUpTimeStamp = -1;
                                    }
                                    catch (final InterruptedException e)
                                    {
                                        this.go = false;
                                        Thread.currentThread().interrupt();
                                    }
                                }

                                this.inFreeingArea = false;
                            }
                            else
                            {
                                this.inFreeingArea = false;

                                try
                                {
                                    this.wakeUpTimeStamp = System.currentTimeMillis() + waitTime;
                                    this.waitMonitor.wait(waitTime);
                                    this.wakeUpTimeStamp = -1;
                                }
                                catch (final InterruptedException e)
                                {
                                    this.go = false;
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                this.logger.error("Exception while run QueueWorker", e);
            }
            catch (final Error e)
            {
                this.logger.error("Error while run QueueWorker", e);
                throw e;
            }
        }
    }

    public boolean checkTimeOut(final AtomicBoolean stop)
    {
        final TaskContainer timeOutTaskContainer = this.currentRunningTask;
        if(timeOutTaskContainer == null)
        {
            return false;
        }

        TaskControlImpl taskControl = null;
        if((taskControl = timeOutTaskContainer.getTaskControl()) == null)
        {
            return false;
        }

        // First check HeartBeat TimeOut

        boolean heartBeatTimeout = false;

        if(taskControl.getHeartbeatTimeout() > 0)
        {
            try
            {
                final long lastHeartBeat = timeOutTaskContainer.getLastHeartbeat();
                if(lastHeartBeat > 0)
                {
                    if((lastHeartBeat + taskControl.getHeartbeatTimeout()) <= System.currentTimeMillis())
                    {
                        heartBeatTimeout = true;
                    }
                }
            }
            catch (final Exception e)
            {
                this.logger.error("Exception checking heartbeat timeout", e);
            }
        }

        if(!heartBeatTimeout)
        {
            // Task TimeOut

            final Long timeOut = this.currentTimeOutTimeStamp;
            if(timeOut == null)
            {
                return false;
            }

            // check timeOut and timeOutTask again to prevent working with values don't match

            if(timeOutTaskContainer != this.currentRunningTask)
            {
                return false;
            }

            if(timeOut != this.currentTimeOutTimeStamp)
            {
                return false;
            }

            if(timeOut.longValue() > System.currentTimeMillis())
            {
                return false;
            }
        }

        final ChannelImpl<?> channel = this.channel;
        final boolean stopFlag = taskControl.getStopOnTimeoutFlag();
        final IDispatcherChannelTask<Object> task = timeOutTaskContainer.getTask();
        final Object taskState = taskControl.getTaskState();
        timeOutTaskContainer.setTaskControl(taskControl.copyForTimeout());

        this.go = false;
        this.interrupt();

        try
        {
            if(task instanceof IDispatcherChannelService)
            {
                taskControl.timeOutService();
            }
            else
            {
                taskControl.timeout();
            }
        }
        catch (final Exception ignored) { }

        for (final ChannelManagerContainer conf : channel.getManagerContainerList())
        {
            try
            {
                if(conf.getChannelManager() instanceof IOnTaskTimeout)
                {
                    try
                    {
                        ((MessageDispatcherImpl) channel.getDispatcher()).executeOnTaskTimeOut((IOnTaskTimeout) conf.getChannelManager(), channel, task, taskState, this);
                    }
                    catch (final Exception ignored) { }
                }
            }
            catch (final Exception ignored) { }
        }

        try
        {
            this.context.onTimeout();
        }
        catch (final Exception ignored) { }

        if(stopFlag)
        {
            if(Thread.currentThread() != this)
            {
                try
                {
                    stop.set(true);
                    ((MessageDispatcherImpl) channel.getDispatcher()).executeOnTaskStopExecuter(this, task);
                }
                catch (final Exception ignored) { }

            }
            else
            {
                this.logger.warn("worker not stopped: checkTimeout invoke by self");
            }
        }

        return true;
    }

    public void notifySoftUpdate()
    {
        this.isUpdateNotified = true;
        this.isSoftUpdated = true;
    }

    public void notifyUpdate(final long newRuntimeStamp)
    {
        synchronized (this.waitMonitor)
        {
            this.isUpdateNotified = true;
            this.isSoftUpdated = false;
            if(this.wakeUpTimeStamp > 0) // waits for new run
            {
                if(newRuntimeStamp <= System.currentTimeMillis()
                   || this.wakeUpTimeStamp >= newRuntimeStamp)
                {
                    this.waitMonitor.notifyAll();
                }
            }
        }
    }

    public void notifyUpdate()
    {
        synchronized (this.waitMonitor)
        {
            this.isUpdateNotified = true;
            this.isSoftUpdated = false;

            this.waitMonitor.notifyAll();
        }
    }

    public void softStopWorker()
    {
        this.go = false;
    }

    public void stopWorker()
    {
        this.go = false;
        // interrupt this thread
        this.interrupt();

        synchronized (this.waitMonitor)
        {
            this.waitMonitor.notifyAll();
        }
    }

    public ChannelImpl getMessageChannel()
    {
        return this.channel;
    }

    protected boolean setMessageChannel(final ChannelImpl channel)
    {
        if(!this.go)
        {
            return false;
        }

        if((channel != null) && (this.channel != null))
        {
            return false;
        }

        if(!this.inFreeingArea)
        {
            return false;
        }

        this.channel = channel;
        this.context.setChannel(this.channel);
        if(this.channel == null)
        {
            super.setName(ChannelWorker.class.getSimpleName() + " IDLE");
        }
        else
        {
            super.setName(ChannelWorker.class.getSimpleName() + " " + this.channel.getId());
        }

        return true;
    }

    public TaskContainer getCurrentRunningTask()
    {
        return this.currentRunningTask;
    }

    public boolean isGo()
    {
        return this.go;
    }

    public long getSpoolTimeStamp()
    {
        return this.spoolTimeStamp;
    }

    public void setSpoolTimeStamp(final long spoolTimeStamp)
    {
        this.spoolTimeStamp = spoolTimeStamp;
    }

    public IDispatcherChannelWorker getWorkerWrapper()
    {
        return this.workerWrapper;
    }
}
