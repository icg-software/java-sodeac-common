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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTaskContext;
import org.sodeac.common.message.dispatcher.api.IPropertyBlock;
import org.sodeac.common.message.dispatcher.api.ITaskControl;

public class ChannelTaskContextImpl implements IDispatcherChannelTaskContext
{
    private IDispatcherChannel channel;
    private TaskContainer dueTask;
    private List<IDispatcherChannelTask> currentProcessedTaskList;
    private List<IDispatcherChannelTask> currentProcessedTaskListWritable;
    private List<IDispatcherChannelTask> currentProcessedTaskListReadOnly;
    private List<TaskContainer> dueTaskList;
    
    private volatile IPropertyBlock propertyBlock = null;
    private volatile String id = null;
    private volatile ITaskControl taskControl = null;
    
    protected ChannelTaskContextImpl(List<TaskContainer> dueTaskList)
    {
        super();
        this.dueTaskList = dueTaskList;
        currentProcessedTaskListWritable = new ArrayList<IDispatcherChannelTask>();
        currentProcessedTaskListReadOnly = Collections.unmodifiableList(currentProcessedTaskListWritable);
    }
    
    @Override
    public IDispatcherChannel getChannel()
    {
        return this.channel;
    }
    
    @Override
    public String getTaskId()
    {
        return this.id;
    }
    
    @Override
    public IPropertyBlock getTaskPropertyBlock()
    {
        return this.propertyBlock;
    }
    
    @Override
    public ITaskControl getTaskControl()
    {
        return this.taskControl;
    }
    
    @Override
    public List<IDispatcherChannelTask> currentProcessedTaskList()
    {
        if (currentProcessedTaskList == null)
        {
            currentProcessedTaskListWritable.clear();
            for (TaskContainer taskContainer : this.dueTaskList)
            {
                currentProcessedTaskListWritable.add(taskContainer.getTask());
            }
        }
        this.currentProcessedTaskList = this.currentProcessedTaskListReadOnly;
        return this.currentProcessedTaskList;
    }
    
    protected void setChannel(IDispatcherChannel channel)
    {
        this.channel = channel;
    }
    
    protected void resetCurrentProcessedTaskList()
    {
        this.currentProcessedTaskList = null;
        if (!this.currentProcessedTaskListWritable.isEmpty())
        {
            this.currentProcessedTaskListWritable.clear();
        }
    }
    
    protected void setDueTask(TaskContainer dueTask)
    {
        if (dueTask == null)
        {
            this.propertyBlock = null;
            this.id = null;
            this.taskControl = null;
            
            this.dueTask = null;
            
            return;
        }
        this.propertyBlock = dueTask.getPropertyBlock();
        this.id = dueTask.getId();
        this.taskControl = dueTask.getTaskControl();
        this.dueTask = dueTask;
    }
    
    protected void onTimeout()
    {
        this.dueTask = null;
    }
    
    @Override
    public void heartbeat()
    {
        try
        {
            if (dueTask != null)
            {
                dueTask.heartbeat();
            }
        }
        catch (Exception e) { }
        catch (Error e) { }
        
    }
    
    @Override
    public void setTaskState(Object taskState)
    {
        if (this.taskControl != null)
        {
            ((TaskControlImpl) this.taskControl).setTaskState(taskState);
        }
        
    }
}
