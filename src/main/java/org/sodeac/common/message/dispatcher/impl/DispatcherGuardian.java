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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherGuardian extends Thread
{
    public static final long DEFAULT_WAIT_TIME = 108 * 108 * 13;
    
    protected DispatcherGuardian(MessageDispatcherImpl dispatcher)
    {
        super();
        this.eventDispatcher = eventDispatcher;
        this.taskTimeOutIndex = new HashMap<ChannelImpl<?>, TaskObservable>();
        
        this.taskTimeOutIndexLock = new ReentrantLock();
        super.setDaemon(true);
        super.setName(DispatcherGuardian.class.getSimpleName() + " " + dispatcher.getId());
    }
    
    private volatile MessageDispatcherImpl eventDispatcher = null;
    private volatile boolean go = true;
    private volatile boolean isUpdateNotified = false;
    private volatile Object waitMonitor = new Object();
    private volatile Map<ChannelImpl<?>, TaskObservable> taskTimeOutIndex = null;
    private volatile Lock taskTimeOutIndexLock;
    
    private volatile long currentWait = -1;
    
    private Logger logger = LoggerFactory.getLogger(DispatcherGuardian.class);
    
    @Override
    public void run()
    {
        long nextTimeOutTimeStamp = -1;
        List<ChannelImpl<?>> timeOutList = null;
        List<TaskObservable> removeTaskObservableList = null;
        boolean inTimeOut = false;
        
        long timeOutListLastAccess = System.currentTimeMillis();
        long removeTaskObservableAccess = System.currentTimeMillis();
        
        while (go)
        {
            nextTimeOutTimeStamp = -1;
            if (timeOutList != null)
            {
                timeOutList.clear();
            }
            if (removeTaskObservableList != null)
            {
                removeTaskObservableList.clear();
            }
            
            taskTimeOutIndexLock.lock();
            try
            {
                long currentTimeStamp = System.currentTimeMillis();
                long heartBeatTimeOut = -1;
                long lastHeartBeat = -1;
                long heartBeatTimeOutStamp = -1;
                TaskContainer task = null;
                
                Long observableTaskTimeOut = null;
                TaskContainer observableTask = null;
                ChannelImpl<?> observableChannel = null;
                
                // Task TimeOut
                
                for (TaskObservable taskObservable : this.taskTimeOutIndex.values())
                {
                    inTimeOut = false;
                    observableTaskTimeOut = taskObservable.taskTimeOut;
                    observableTask = taskObservable.task;
                    observableChannel = taskObservable.channel;
                    
                    if ((observableTask == null) || (observableChannel == null))
                    {
                        continue;
                    }
                    
                    task = observableChannel.getCurrentRunningTask(); // LOCKS CHANNEL.workerSpoolLock
                    
                    if ((task == null) || (task != observableTask))
                    {
                        if (removeTaskObservableList == null)
                        {
                            removeTaskObservableList = new LinkedList<TaskObservable>();
                        }
                        removeTaskObservableList.add(taskObservable);
                        removeTaskObservableAccess = System.currentTimeMillis();
                        continue;
                    }
                    
                    // Task Timeout
                    
                    if ((observableTaskTimeOut != null) && (observableTaskTimeOut.longValue() > 0))
                    {
                        if (observableTaskTimeOut.longValue() <= currentTimeStamp)
                        {
                            if (timeOutList == null)
                            {
                                timeOutList = new LinkedList<ChannelImpl<?>>();
                            }
                            timeOutList.add(observableChannel);
                            timeOutListLastAccess = System.currentTimeMillis();
                            inTimeOut = true;
                        }
                        else
                        {
                            if (nextTimeOutTimeStamp < 0)
                            {
                                nextTimeOutTimeStamp = observableTaskTimeOut;
                            }
                            else if (nextTimeOutTimeStamp > observableTaskTimeOut)
                            {
                                nextTimeOutTimeStamp = observableTaskTimeOut;
                            }
                        }
                    }
                    
                    if (inTimeOut)
                    {
                        continue;
                    }
                    
                    // HeartBeat Timeout
                    
                    heartBeatTimeOutStamp = -1;
                    
                    TaskControlImpl taskControl = null;
                    if ((task != null) && ((taskControl = task.getTaskControl()) != null))
                    {
                        heartBeatTimeOut = taskControl.getHeartbeatTimeout();
                        
                        if (heartBeatTimeOut > 0)
                        {
                            try
                            {
                                lastHeartBeat = task.getLastHeartbeat();
                                heartBeatTimeOutStamp = lastHeartBeat + heartBeatTimeOut;
                                if (lastHeartBeat > 0)
                                {
                                    if (heartBeatTimeOutStamp <= currentTimeStamp)
                                    {
                                        if (timeOutList == null)
                                        {
                                            timeOutList = new ArrayList<ChannelImpl<?>>();
                                        }
                                        timeOutList.add(observableChannel);
                                        timeOutListLastAccess = System.currentTimeMillis();
                                        inTimeOut = true;
                                    }
                                    else
                                    {
                                        if ((nextTimeOutTimeStamp < 0) || nextTimeOutTimeStamp > heartBeatTimeOutStamp)
                                        {
                                            nextTimeOutTimeStamp = heartBeatTimeOutStamp;
                                        }
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                logger.error("Error while check heartbeat timeout", e);
                            }
                        }
                    }
                }
                
                observableTaskTimeOut = null;
                observableTask = null;
                observableChannel = null;
                
            }
            catch (Exception | Error e)
            {
                logger.error("Error running DispatcherGuardian", e);
            }
            finally
            {
                taskTimeOutIndexLock.unlock();
            }
            
            if (timeOutList != null)
            {
                for (ChannelImpl<?> channel : timeOutList)
                {
                    channel.checkTimeOut();
                }
            }
            
            if ((removeTaskObservableList != null) && (!removeTaskObservableList.isEmpty()))
            {
                taskTimeOutIndexLock.lock();
                try
                {
                    for (TaskObservable taskObservable : removeTaskObservableList)
                    {
                        TaskObservable toRemoveObservable = this.taskTimeOutIndex.get(taskObservable.channel);
                        if (toRemoveObservable == null)
                        {
                            continue;
                        }
                        if (toRemoveObservable != taskObservable)
                        {
                            continue;
                        }
                        
                        TaskContainer task = taskObservable.channel.getCurrentRunningTask();
                        if ((task == null) || (task != taskObservable.task))
                        {
                            this.taskTimeOutIndex.remove(taskObservable.channel);
                        }
                    }
                }
                catch (Exception | Error e)
                {
                    logger.error("Error running DispatcherGuardian", e);
                }
                finally
                {
                    taskTimeOutIndexLock.unlock();
                }
            }
            
            if ((removeTaskObservableList != null) && (removeTaskObservableAccess <= (System.currentTimeMillis() - (DEFAULT_WAIT_TIME * 3))))
            {
                removeTaskObservableList.clear();
                removeTaskObservableList = null;
            }
            
            if ((timeOutList != null) && (timeOutListLastAccess <= (System.currentTimeMillis() - (DEFAULT_WAIT_TIME * 3))))
            {
                timeOutList.clear();
                timeOutList = null;
            }
            
            try
            {
                synchronized (this.waitMonitor)
                {
                    if (go)
                    {
                        if (isUpdateNotified)
                        {
                            isUpdateNotified = false;
                        }
                        else
                        {
                            long waitTime = nextTimeOutTimeStamp - System.currentTimeMillis();
                            if ((waitTime > DEFAULT_WAIT_TIME) || (nextTimeOutTimeStamp < 0))
                            {
                                waitTime = DEFAULT_WAIT_TIME;
                            }
                            if (waitTime > 0)
                            {
                                this.currentWait = System.currentTimeMillis() + waitTime;
                                waitMonitor.wait(waitTime);
                                this.currentWait = -1;
                            }
                        }
                    }
                }
            }
            catch (InterruptedException e) { }
            catch (Error | Exception e)
            {
                logger.error("Error running Dispatcher DispatcherGuardian", e);
            }
        }
    }
    
    public void registerTimeOut(ChannelImpl<?> channel, TaskContainer taskContainer)
    {
        TaskControlImpl taskControl = taskContainer.getTaskControl();
        if (taskControl == null)
        {
            return;
        }
        
        long timeOutTimeStamp = taskControl.getTimeout() < 0L ? -1 : taskControl.getTimeout() + System.currentTimeMillis();
        
        taskTimeOutIndexLock.lock();
        try
        {
            TaskObservable taskObservable = this.taskTimeOutIndex.get(channel);
            if (taskObservable == null)
            {
                taskObservable = new TaskObservable();
                taskObservable.channel = channel;
                
                if (taskControl.getTimeout() > 0)
                {
                    taskObservable.task = taskContainer;
                    taskObservable.taskTimeOut = timeOutTimeStamp;
                }
                else if (taskControl.getHeartbeatTimeout() > 0)
                {
                    taskObservable.task = taskContainer;
                    taskObservable.taskTimeOut = null;
                }
                else
                {
                    return;
                }
                
                this.taskTimeOutIndex.put(channel, taskObservable);
            }
            else
            {
                if (taskControl.getTimeout() > 0)
                {
                    taskObservable.task = taskContainer;
                    taskObservable.taskTimeOut = timeOutTimeStamp;
                }
                else if (taskControl.getHeartbeatTimeout() > 0)
                {
                    taskObservable.task = taskContainer;
                    taskObservable.taskTimeOut = null;
                }
                else
                {
                    this.taskTimeOutIndex.remove(channel);
                    return;
                }
            }
        }
        finally
        {
            taskTimeOutIndexLock.unlock();
        }
        
        boolean notify = false;
        synchronized (this.waitMonitor)
        {
            this.isUpdateNotified = true;
            if (this.currentWait > 0)
            {
                if ((timeOutTimeStamp > 0) && (timeOutTimeStamp < this.currentWait))
                {
                    notify = true;
                }
                else
                {
                    long heartBeatTimeOut = taskControl.getHeartbeatTimeout();
                    if (heartBeatTimeOut > 0)
                    {
                        try
                        {
                            long lastHeartBeat = taskContainer.getLastHeartbeat();
                            if (lastHeartBeat > 0)
                            {
                                long heartBeatTimeOutStamp = lastHeartBeat + heartBeatTimeOut;
                                if (heartBeatTimeOutStamp < this.currentWait)
                                {
                                    notify = true;
                                }
                                
                            }
                        }
                        catch (Exception e)
                        {
                            logger.error("Error while check heartbeat timeout", e);
                        }
                    }
                }
            }
            
            if (notify)
            {
                try
                {
                    waitMonitor.notify();
                }
                catch (Exception | Error e) { }
            }
        }
        
    }
    
    public void unregisterTimeOut(ChannelImpl<?> channel, TaskContainer task)
    {
        taskTimeOutIndexLock.lock();
        try
        {
            TaskObservable taskObservable = this.taskTimeOutIndex.get(channel);
            if (taskObservable != null)
            {
                if ((taskObservable.task != null) && (taskObservable.task == task))
                {
                    this.taskTimeOutIndex.remove(channel);
                    taskObservable.channel = null;
                    taskObservable.task = null;
                    taskObservable.taskTimeOut = null;
                }
            }
        }
        finally
        {
            taskTimeOutIndexLock.unlock();
        }
    }
    
    public void stopGuardian()
    {
        this.go = false;
        synchronized (this.waitMonitor)
        {
            try
            {
                this.waitMonitor.notify();
            }
            catch (Exception e)
            {
                logger.error("Error stopping DispatcherGuardian", e);
            }
        }
    }
    
    private class TaskObservable
    {
        public volatile Long taskTimeOut = null;
        public volatile TaskContainer task = null;
        public volatile ChannelImpl<?> channel = null;
    }
    
}
