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
package org.sodeac.common.message.dispatcher.api;

import org.sodeac.common.message.dispatcher.impl.MessageDispatcherManagerImpl;

public interface IMessageDispatcherManager
{
    public static final String DEFAULT_DISPATCHER_ID = "org.sodeac.common.message.dispatcher.default";
    
    public static IMessageDispatcherManager get()
    {
        return MessageDispatcherManagerImpl.get();
    }
    
    public default IMessageDispatcher getDefaultDispatcher()
    {
        // shutdown-protection ?
        return getOrCreateDispatcher(DEFAULT_DISPATCHER_ID);
    }
    
    public IMessageDispatcher createDispatcher(String id);
    
    public IMessageDispatcher getOrCreateDispatcher(String id);
    
    public IMessageDispatcher getDispatcher(String id);
}
