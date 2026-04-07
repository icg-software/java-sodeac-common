/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.impl;

import org.sodeac.common.message.dispatcher.api.IDispatcherChannel;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelWorker;

public class ChannelWorkerWrapper implements IDispatcherChannelWorker
{
	private ChannelWorker worker = null;
	
	protected ChannelWorkerWrapper(ChannelWorker worker)
	{
		super();
		this.worker = worker;
	}

	@Override
	public void interrupt()
	{
		worker.interrupt();
	}

	@Override
	public IDispatcherChannel getChannel()
	{
		return worker.getMessageChannel();
	}

}
