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

public class SpooledChannelWorker
{
    protected SpooledChannelWorker(final ChannelImpl channel, final long wakeupTime)
    {
        super();
        this.channel = channel;
        this.wakeupTime = wakeupTime;
    }
    
    private final ChannelImpl channel;
    private final long wakeupTime;
    private volatile boolean valid = true;
    
    public ChannelImpl getChannel()
    {
        return this.channel;
    }
    
    public long getWakeupTime()
    {
        return this.wakeupTime;
    }
    
    public boolean isValid()
    {
        return this.valid;
    }
    
    public void setValid(final boolean valid)
    {
        this.valid = valid;
    }
}
