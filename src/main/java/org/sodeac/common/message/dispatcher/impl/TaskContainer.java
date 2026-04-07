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

import org.sodeac.common.message.dispatcher.api.IDispatcherChannelTask;
import org.sodeac.common.message.dispatcher.api.IPropertyBlock;

public class TaskContainer
{
    protected TaskContainer()
    {
        super();
    }
    
    private IDispatcherChannelTask task;
    private String id;
    private IPropertyBlock properties;
    private volatile TaskControlImpl taskControl;
    private boolean namedTask = false;
    private volatile long lastHeartbeat = -1L;
    
    public IDispatcherChannelTask getTask()
    {
        return this.task;
    }
    
    public void setTask(final IDispatcherChannelTask task)
    {
        this.task = task;
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public void setId(final String id)
    {
        this.id = id;
    }
    
    public IPropertyBlock getPropertyBlock()
    {
        return this.properties;
    }
    
    public void setPropertyBlock(final IPropertyBlock properties)
    {
        this.properties = properties;
    }
    
    public TaskControlImpl getTaskControl()
    {
        return this.taskControl;
    }
    
    public void setTaskControl(final TaskControlImpl taskControl)
    {
        this.taskControl = taskControl;
    }
    
    public boolean isNamedTask()
    {
        return this.namedTask;
    }
    
    public void setNamedTask(final boolean namedTask)
    {
        this.namedTask = namedTask;
    }
    
    public void heartbeat()
    {
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public long getLastHeartbeat()
    {
        return this.lastHeartbeat;
    }
}
