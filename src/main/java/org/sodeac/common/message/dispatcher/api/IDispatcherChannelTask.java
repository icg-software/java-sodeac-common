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
 * A {@link IDispatcherChannelTask} acts as processor for one or more {@link IMessage}s.
 * 
 * @author Sebastian Palarus
 *
 */
@FunctionalInterface
public interface IDispatcherChannelTask<T>
{
	
	/**
	 * invoked one time at initialization of this task
	 * 
	 * @param queue parent-{@link IDispatcherChannel} 
	 * @param id registration-id of this task
	 * @param propertyBlock properties for this task
	 * @param taskControl state-handler for this task
	 */
	public default void configure(IDispatcherChannel<T> queue, String id, IPropertyBlock propertyBlock, ITaskControl taskControl) {};
	
	/**
	 * run this task, invoked by channel-worker.
	 * 
	 * @param context of task running
	 * @throws Exception
	 */
	public void run(IDispatcherChannelTaskContext<T> taskContext) throws Exception;
}
