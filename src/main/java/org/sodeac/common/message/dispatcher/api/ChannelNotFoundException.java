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
package org.sodeac.common.message.dispatcher.api;

/**
 * 
 * @author Sebastian Palarus
 *
 */
public class ChannelNotFoundException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9068625039710702255L;
	
	private String queueId = null;

	public ChannelNotFoundException(String queueId)
	{
		super();
		this.queueId = queueId;
	}

	public ChannelNotFoundException(String queueId,String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
		this.queueId = queueId;
	}

	public ChannelNotFoundException(String queueId, String message, Throwable cause)
	{
		super(message, cause);
		this.queueId = queueId;
	}

	public ChannelNotFoundException(String queueId,String message)
	{
		super(message);
		this.queueId = queueId;
	}

	public ChannelNotFoundException(String queueId,Throwable cause)
	{
		super(cause);
		this.queueId = queueId;
	}

	public String getQueueId()
	{
		return queueId;
	}
	
}
