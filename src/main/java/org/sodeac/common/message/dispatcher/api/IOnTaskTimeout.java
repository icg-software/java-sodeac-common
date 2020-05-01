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
 * An extension interface for {@link IDispatcherChannelManager} to consume notifications if a task runs in a timeout
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnTaskTimeout<T> extends IDispatcherChannelManager
{
	/**
	 * This is fired, if {@link IDispatcherChannelTask} runs in timeout.
	 * <br>
	 * Attention! This call is not synchronized by worker thread!
	 * 
	 * @param channel  queue of task runs in timeout
	 * @param task runs in timeout
	 * @param taskState
	 * @param interruptInvoke
	 */
	public void onTaskTimeout(IDispatcherChannel<T> channel, IDispatcherChannelTask<T> task, Object taskState, Runnable interrupter);
}
