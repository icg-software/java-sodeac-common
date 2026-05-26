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

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.ITaskControl;

public class TaskControlImpl implements ITaskControl
{
    private volatile boolean done = false;
    private volatile boolean inTimeOut = false;
    private volatile long executionTimeStamp = 0L;
    private volatile long timeOutValue = -1; // TimeUnit.DAYS.toMillis(7);
    private volatile long heartBeatTimeOut = -1;
    
    private volatile boolean stopTaskOnTimeout = false;
    private volatile boolean inRun = false;
    private volatile ExecutionTimestampSource executionTimeStampSource = ITaskControl.ExecutionTimestampSource.SCHEDULE;
    
    private ReentrantLock executionTimestampLock = null;
    private SetTimestampRequest setTimestampRequest = null;
    private volatile Object taskState = null;
    
    protected TaskControlImpl()
    {
        super();
        this.executionTimeStamp = System.currentTimeMillis();
        this.executionTimestampLock = new ReentrantLock();
        this.setTimestampRequest = new SetTimestampRequest();
    }
    
    public void preRun()
    {
        this.inRun = true;
        this.done = true;
    }
    
    public void preRunPeriodicTask()
    {
        this.inRun = true;
    }
    
    public void postRun()
    {
        this.inRun = false;
        this.taskState = null;
    }
    
    @Override
    public boolean setDone()
    {
        boolean old = this.done;
        this.done = true;
        return old;
    }
    
    @Override
    public void timeout()
    {
        this.inTimeOut = true;
        this.done = true;
    }
    
    public void timeOutService()
    {
        this.inTimeOut = true;
    }
    
    @Override
    public boolean isInTimeout()
    {
        return this.inTimeOut;
    }
    
    @Override
    public long getExecutionTimestamp()
    {
        return this.executionTimeStamp;
    }
    
    @Override
    public ExecutionTimestampSource getExecutionTimestampSource()
    {
        return this.executionTimeStampSource;
    }
    
    public void setExecutionTimeStampSource(final ExecutionTimestampSource executionTimeStampSource)
    {
        this.executionTimeStampSource = executionTimeStampSource;
    }
    
    public long getExecutionTimeStampIntern()
    {
        return this.executionTimeStamp;
    }
    
    protected Object getTaskState()
    {
        return this.taskState;
    }
    
    protected void setTaskState(final Object taskState)
    {
        this.taskState = taskState;
    }
    
    @Override
    public boolean setExecutionTimestamp(final long executionTimeStamp, final boolean force)
    {
        this.executionTimestampLock.lock();
        try
        {
            long old = this.executionTimeStamp;
            if
            (
                (executionTimeStamp < old) ||
                force ||
                (old < System.currentTimeMillis()) ||
                (this.executionTimeStampSource == ITaskControl.ExecutionTimestampSource.SCHEDULE) ||
                (this.executionTimeStampSource == ITaskControl.ExecutionTimestampSource.WORKER) ||
                (this.executionTimeStampSource == ITaskControl.ExecutionTimestampSource.PERODIC)
            )
            {
                this.executionTimeStamp = executionTimeStamp;
                this.executionTimeStampSource = ITaskControl.ExecutionTimestampSource.WORKER;
                
                if (this.inRun)
                {
                    this.done = false;
                }
                
                return true;
            }
            
            /*
             * ignored if
             * 		not forced and
             * 		current configured execution timestamp in future and
             * 		new requested execution timestamp is later then current configured execution timestamp and
             * 		current configured execution timestamp configured by (schedule, worker or periodic service configuration)
             */
            
            return false;
        }
        finally
        {
            this.executionTimestampLock.unlock();
        }
    }
    
    public boolean setExecutionTimeStamp(final long executionTimeStamp, final ITaskControl.ExecutionTimestampSource type, final Predicate<SetTimestampRequest> predicate)
    {
        this.executionTimestampLock.lock();
        try
        {
            this.setTimestampRequest.setTimestamp(executionTimeStamp);
            if (predicate.test(this.setTimestampRequest))
            {
                this.executionTimeStamp = executionTimeStamp;
                this.executionTimeStampSource = type;
                
                return true;
            }
            
            return false;
        }
        finally
        {
            this.executionTimestampLock.unlock();
        }
        
    }
    
    @Override
    public long getTimeout()
    {
        return this.timeOutValue;
    }
    
    @Override
    public long setTimeout(final long timeOut)
    {
        long old = this.timeOutValue;
        this.timeOutValue = timeOut;
        return old;
    }
    
    @Override
    public long getHeartbeatTimeout()
    {
        return this.heartBeatTimeOut;
    }
    
    @Override
    public long setHeartbeatTimeout(final long heartBeatTimeOut)
    {
        long old = this.heartBeatTimeOut;
        this.heartBeatTimeOut = heartBeatTimeOut;
        return old;
    }
    
    @Override
    public boolean isDone()
    {
        return this.done;
    }
    
    @Override
    public boolean setStopOnTimeoutFlag(final boolean value)
    {
        boolean oldValue = this.stopTaskOnTimeout;
        this.stopTaskOnTimeout = value;
        return oldValue;
    }
    
    @Override
    public boolean getStopOnTimeoutFlag()
    {
        return this.stopTaskOnTimeout;
    }
    
    public class SetTimestampRequest
    {
        private long timestamp = -1L;
        
        public long getTimestamp()
        {
            return this.timestamp;
        }
        
        private void setTimestamp(final long timestamp)
        {
            this.timestamp = timestamp;
        }
        
        public TaskControlImpl getTaskControl()
        {
            return TaskControlImpl.this;
        }
    }
    
    /**
     * Predicate to test for {@link IDispatcherChannel#rescheduleTask(String, long, long, long)}
     *
     * @author Sebastian Palarus
     *
     */
    public static class RescheduleTimestampPredicate implements Predicate<SetTimestampRequest>
    {
        public static final RescheduleTimestampPredicate INSTANCE = new TaskControlImpl.RescheduleTimestampPredicate();
        
        @Override
        public boolean test(final SetTimestampRequest setTimestampRequest)
        {
            long old = setTimestampRequest.getTaskControl().getExecutionTimestamp();
            long executionTimeStamp = setTimestampRequest.getTimestamp();
            ExecutionTimestampSource executionTimestampSource = setTimestampRequest.getTaskControl().getExecutionTimestampSource();
            
            return                                                                                        // (| ....)
                (executionTimeStamp < old) ||                                                            // earlier execution is requested
                (old < System.currentTimeMillis()) ||                                                    // last request in past
                (executionTimestampSource == ITaskControl.ExecutionTimestampSource.SCHEDULE) ||            // last request from schedule
                (executionTimestampSource == ITaskControl.ExecutionTimestampSource.RESCHEDULE)            // or reschedule
                ;
            
            /*
             * => ignore if
             * 		current execution plan requires earlier execution in the future and was requested by
             * 			trigger,
             * 			tasks control
             * 			periodic service configuration
             */
        }
        
        public static RescheduleTimestampPredicate getInstance()
        {
            return RescheduleTimestampPredicate.INSTANCE;
        }
        
    }
    
    /**
     * Predicate to test for {@link IDispatcherChannel#scheduleTask(org.sodeac.messagedispatcher.api.IQueueTask)} ...
     *
     * @author Sebastian Palarus
     *
     */
    public static class ScheduleTimestampPredicate implements Predicate<SetTimestampRequest>
    {
        public static final ScheduleTimestampPredicate INSTANCE = new TaskControlImpl.ScheduleTimestampPredicate();
        
        @Override
        public boolean test(final SetTimestampRequest setTimestampRequest)
        {
            return true;
        }
        
        public static ScheduleTimestampPredicate getInstance()
        {
            return ScheduleTimestampPredicate.INSTANCE;
        }
    }
    
    /**
     * Predicate to test for set periodic reschedule service timestamp
     *
     * @author Sebastian Palarus
     *
     */
    public static class PeriodicServiceTimestampPredicate implements Predicate<SetTimestampRequest>
    {
        public static final PeriodicServiceTimestampPredicate INSTANCE = new PeriodicServiceTimestampPredicate();
        
        @Override
        public boolean test(final SetTimestampRequest setTimestampRequest)
        {
            long old = setTimestampRequest.getTaskControl().getExecutionTimestamp();
            long executionTimeStamp = setTimestampRequest.getTimestamp();
            ExecutionTimestampSource executionTimestampSource = setTimestampRequest.getTaskControl().getExecutionTimestampSource();
            
            return !
                (
                    (old > System.currentTimeMillis()) &&
                    (executionTimestampSource == ITaskControl.ExecutionTimestampSource.RESCHEDULE) &&
                    (executionTimeStamp > 0)
                );
            
            
            /*
             * => ignore if
             * 		current execution plan is in the future and was requested by IQueue#rescheduleTask and new requested timestamp is not 0
             */
        }
        
        public static PeriodicServiceTimestampPredicate getInstance()
        {
            return PeriodicServiceTimestampPredicate.INSTANCE;
        }
        
    }
    
    protected TaskControlImpl copyForTimeout()
    {
        TaskControlImpl taskControl = new TaskControlImpl();
        taskControl.done = this.done;
        taskControl.inTimeOut = false;
        taskControl.executionTimeStamp = this.executionTimeStamp;
        taskControl.timeOutValue = this.timeOutValue;
        taskControl.heartBeatTimeOut = this.heartBeatTimeOut;
        
        taskControl.stopTaskOnTimeout = this.stopTaskOnTimeout;
        taskControl.inRun = this.inRun;
        taskControl.executionTimeStampSource = this.executionTimeStampSource;
        
        taskControl.executionTimestampLock = this.executionTimestampLock;
        taskControl.setTimestampRequest = this.setTimestampRequest;
        
        return taskControl;
    }
}
