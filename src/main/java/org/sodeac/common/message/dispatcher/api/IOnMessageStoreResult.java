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

import java.util.List;

/**
 * 
 * This object inform invoker of {@link IDispatcherChannel#storeMessageWithResult(Object)} (or related) about store of messages
 * The implementation {@link IOnMessageStore} / {@link IOnQueuedMessageList} has confirm process queued event, if it is in charge of this event type
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnMessageStoreResult
{
	/**
	 * marks event(s) as qeueued
	 */
	public void markStored();
	
	/**
	 * Getter for queued-state. This mark is only set, if a controller of type {@link IOnMessageStore} or {@link IOnQueuedMessageList} marks queued event !
	 * 
	 * @return true, if {@link IOnMessageStore} / {@link IOnQueuedMessageList} successfully processed queued event, otherwise false
	 */
	public boolean isStored();
	
	/**
	 * publish an error
	 * 
	 * @param throwable 
	 */
	public void addError(Throwable throwable);
	
	/**
	 * return schedule result contains errors
	 * 
	 * @return true if scheduler or worker published an error, otherwise false,
	 */
	public boolean hasErrors();
	
	/**
	 * returns all published errors
	 * 
	 * @return all published errors
	 */
	public List<Throwable> getErrorList();
	
	/**
	 * getter for detailResultObject
	 * 
	 * @return detailResultObject
	 */
	public Object getDetailResultObject();
	
	/**
	 * setter for detailResultObject
	 * 
	 * @param detailResultObject notify object for invoker
	 */
	public void setDetailResultObject(Object detailResultObject);
	
	/**
	 * return all published detail result objects
	 * 
	 * @return a list of published detail result object
	 */
	public List<Object> getDetailResultObjectList();
	
	/**
	 * publish a new detail result object
	 * 
	 * @param detailResultObject detail result object (appends to detailResultObjectList)
	 */
	public void addDetailResultObjectList(Object detailResultObject);
	
	/**
	 * Returns if this object is an dummy object (add event by queueEvent and not by queueEventWithResult)
	 * 
	 * @return true, if this object is an dummy object , otherwise false
	 */
	public default boolean isDummy()
	{
		return false;
	}
}
