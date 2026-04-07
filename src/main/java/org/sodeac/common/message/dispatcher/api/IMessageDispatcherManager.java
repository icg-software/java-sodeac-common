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
    String DEFAULT_DISPATCHER_ID = "org.sodeac.common.message.dispatcher.default";
    
    static IMessageDispatcherManager get()
    {
        return MessageDispatcherManagerImpl.get();
    }
    
    default IMessageDispatcher getDefaultDispatcher()
    {
        // shutdown-protection ?
        return getOrCreateDispatcher(DEFAULT_DISPATCHER_ID);
    }
    
    IMessageDispatcher createDispatcher(String id);
    
    IMessageDispatcher getOrCreateDispatcher(String id);
    
    IMessageDispatcher getDispatcher(String id);
}
