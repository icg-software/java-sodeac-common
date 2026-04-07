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
 * An extension interface for {@link IDispatcherChannelManager} to consume notifications if a task throws an exception or an error
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnTaskError<T> extends IDispatcherChannelManager
{
	/**
	 * This methode is fired, if {@link IDispatcherChannelTask} throws an exception or an error
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param queue  queue of task which throws the exception
	 * @param task task which throws the exception
	 * @param throwable throwed exception or error
	 */
	public void onTaskError(IDispatcherChannel<T> queue,IDispatcherChannelTask<T> task, Throwable throwable);
}
