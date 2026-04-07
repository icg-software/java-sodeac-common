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
 * An extension interface for {@link IDispatcherChannelManager} to consume notifications if instance of {@link IDispatcherChannelManager} detach from a {@link IDispatcherChannel}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnChannelDetach<T> extends IDispatcherChannelManager
{
	/**
	 * This is fired, if {@link IDispatcherChannelManager} detach from a {@link IDispatcherChannel}
	 * <br>
	 * Attention! This call is not synchronized by worker thread!
	 * 
	 * @param channel is detach from {@link IDispatcherChannelManager}
	 */
	public void onChannelDetach(IDispatcherChannel<T> channel);
}
