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
 * wrapper object for worker thread 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IDispatcherChannelWorker
{

	/**
	 * invoke {@link java.lang.Thread#interrupt()}  on worker thread
	 */
	public void interrupt();
	
	/**
	 * get {@link IDispatcherChannel} for which the worker works
	 * 
	 * @return queue for which the worker works
	 */
	public IDispatcherChannel getChannel();
}
