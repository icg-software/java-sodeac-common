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

import java.util.List;

/**
 * Context of task running
 * 
 * @author Sebastian Palarus
 *
 */
public interface IDispatcherChannelTaskContext<T>
{
	/**
	 * getter for queue
	 * 
	 * @return queue
	 */
	public IDispatcherChannel<T> getChannel();
	
	
	/**
	 * getter for task property block
	 * 
	 * @return task property block
	 */
	public IPropertyBlock getTaskPropertyBlock(); 
	
	/**
	 * getter for task control
	 * 
	 * @return task control
	 */
	public ITaskControl getTaskControl();
	
	/**
	 * getter for current processing task list
	 * 
	 * @return current processing task list
	 */
	public List<IDispatcherChannelTask<T>> currentProcessedTaskList();

	/**
	 * 
	 * @return
	 */
	public String getTaskId();
	
	/**
	 * publish healthy living state
	 */
	public void heartbeat();
	
	/**
	 * Setter for task state. task state is usable in {@link IOnTaskTimeout#onTaskTimeout(IDispatcherChannel, IDispatcherChannelTask, Object, Runnable)}. 
	 * 
	 * @param taskState
	 */
	public void setTaskState(Object taskState);
}
