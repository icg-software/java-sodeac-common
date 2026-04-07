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
package org.sodeac.common.message.dispatcher.api;

/**
 *
 * An channel manager reacts to a wide variety of queue happenings, if it implements appropriate extension interfaces.
 *
 * @author Sebastian Palarus
 *
 */
public interface IDispatcherChannelManager extends IDispatcherChannelComponent
{
    /**
     * Configure Manager channel binding behavior.
     *
     * @param configurationPolicy
     */
    public default void configureChannelManagerPolicy(IChannelManagerPolicy componentBindingPolicy)
    {
    }
    
    /**
     * Policy container defines runtime configuration of controller.
     *
     * @author "Sebastian Palarus"
     *
     */
    public static interface IChannelManagerPolicy
    {
        /**
         *
         * @param configuration
         *
         * @return
         */
        public IChannelManagerPolicy addConfigurationDetail(ComponentBindingSetup componentBindingSetup);
    }
}
