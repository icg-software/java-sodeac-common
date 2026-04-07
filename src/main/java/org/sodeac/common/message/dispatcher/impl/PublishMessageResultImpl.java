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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.common.message.dispatcher.api.IOnMessageStoreResult;
import org.sodeac.common.misc.TaskDoneNotifier;

public class PublishMessageResultImpl implements IOnMessageStoreResult
{
	private				ReentrantLock		lock						= null;
	private 			TaskDoneNotifier	resultSettedNotifier		= null;
	private volatile 	boolean 			processingIsFinished 		= false; 
	
	private volatile 	boolean 			queued 						= false;
	private volatile 	List<Throwable> 	errorList 					= null;
	private volatile 	Object 				detailResultObject 			= null;
	private volatile 	List<Object> 		detailResultObjectList 		= null;
	
	protected PublishMessageResultImpl()
	{
		super();
		this.resultSettedNotifier = new TaskDoneNotifier();
		this.lock = new ReentrantLock(true);
	}
	
	protected void waitForProcessingIsFinished()
	{
		while(! processingIsFinished)
		{
			try
			{
				this.resultSettedNotifier.await();
			}
			catch (Exception e) {}
		}
	}
	
	protected void processPhaseIsFinished()
	{
		this.processingIsFinished = true;
		this.resultSettedNotifier.countDown();
	}
	
	@Override
	public void addError(Throwable throwable)
	{
		this.lock.lock();
		try
		{
			if(this.errorList == null)
			{
				this.errorList = new ArrayList<Throwable>();
			}
			this.errorList.add(throwable);
		}
		finally 
		{
			this.lock.unlock();
		}
	}
	
	@Override
	public List<Throwable> getErrorList()
	{
		this.lock.lock();
		try
		{
			if(this.errorList == null)
			{
				return null;
			}
			return Collections.unmodifiableList(new ArrayList<Throwable>(this.errorList));
		}
		finally 
		{
			this.lock.unlock();
		}
	}
	
	@Override
	public boolean hasErrors()
	{
		if(this.errorList == null)
		{
			return false;
		}
		return ! this.errorList.isEmpty();
	}

	@Override
	public boolean isStored()
	{
		return queued;
	}

	@Override
	public void markStored()
	{
		this.queued = true;
	}

	@Override
	public Object getDetailResultObject()
	{
		return detailResultObject;
	}

	@Override
	public void setDetailResultObject(Object detailResultObject)
	{
		this.detailResultObject = detailResultObject;
	}

	@Override
	public List<Object> getDetailResultObjectList()
	{
		this.lock.lock();
		try
		{
			if(this.detailResultObjectList == null)
			{
				return null;
			}
			return Collections.unmodifiableList(new ArrayList<Object>(this.detailResultObjectList));
		}
		finally 
		{
			this.lock.unlock();
		}
	}

	@Override
	public void addDetailResultObjectList(Object detailResultObject)
	{
		this.lock.lock();
		try
		{
			if(this.detailResultObjectList == null)
			{
				this.detailResultObjectList = new ArrayList<Object>();
			}
			this.detailResultObjectList.add(detailResultObject);
		}
		finally 
		{
			this.lock.unlock();
		}
		this.detailResultObjectList = detailResultObjectList;
	}
	
	protected void dispose()
	{
		this.resultSettedNotifier = null;
		this.detailResultObject = null;
		
		if(this.detailResultObjectList != null)
		{
			try
			{
				this.detailResultObjectList.clear();
			}
			catch (Exception e) {}
		}
		
		if(this.errorList != null)
		{
			try
			{
				this.errorList.clear();
			}
			catch (Exception e) {}
		}
		
		this.detailResultObjectList = null;
		this.errorList = null;
		this.lock = null;
	}
	
}
