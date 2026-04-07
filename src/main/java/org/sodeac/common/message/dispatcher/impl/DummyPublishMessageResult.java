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
package org.sodeac.common.message.dispatcher.impl;

import java.util.List;

public class DummyPublishMessageResult extends PublishMessageResultImpl
{
	protected DummyPublishMessageResult()
	{
		super();
	}

	@Override
	public void markStored(){}

	@Override
	public boolean isStored()
	{
		return false;
	}

	@Override
	public void addError(Throwable throwable){}

	@Override
	public boolean hasErrors()
	{
		return false;
	}

	@Override
	public List<Throwable> getErrorList()
	{
		return null;
	}

	@Override
	public Object getDetailResultObject()
	{
		return null;
	}

	@Override
	public void setDetailResultObject(Object detailResultObject){}

	@Override
	public List<Object> getDetailResultObjectList()
	{
		return null;
	}

	@Override
	public boolean isDummy()
	{
		return true;
	}

	@Override
	public void addDetailResultObjectList(Object detailResultObject)
	{
	}

	@Override
	protected void waitForProcessingIsFinished(){}

	@Override
	protected void processPhaseIsFinished(){}

	@Override
	protected void dispose()
	{
		// do nothing
	}
	
	protected void disposeDummy()
	{
		super.dispose();
	}
	
	
	
}
