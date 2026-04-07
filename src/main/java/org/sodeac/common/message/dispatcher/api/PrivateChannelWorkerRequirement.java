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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Declares the necessity to use same queue worker thread for synchronized queue activities at all times. 
 * Otherwise the used worker can shared with other queues for synchronized queue activities. In both cases the exclusive access to queue resources is guaranteed.
 * 
 * <br>
 * 
 * <table border="1">
 * <tr>
 * <th>
 * Condition
 * </th>
 * <th>
 * Result
 * </th>
 * </tr>
 * <tr>
 * <td>
 * At least one {@link IDispatcherChannelComponent} claims to use private worker by {@link PrivateChannelWorkerRequirement#RequirePrivateChannelWorker}
 * </td>
 * <td>
 * Use private Worker (always)
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IDispatcherChannelComponent}s prefer to use same worker ({@link PrivateChannelWorkerRequirement#PreferPrivateChannelWorker}) then not ({@link PrivateChannelWorkerRequirement#PreferSharedChannelWorker})
 * </td>
 * <td>
 * Use private Worker (if no {@link IDispatcherChannelComponent} claims to use private worker by {@link PrivateChannelWorkerRequirement#RequirePrivateChannelWorker})
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IDispatcherChannelComponent}s prefer to use shared worker ({@link PrivateChannelWorkerRequirement#PreferSharedChannelWorker}) then not ({@link PrivateChannelWorkerRequirement#PreferPrivateChannelWorker})
 * </td>
 * <td>
 * Use shared Worker (if no {@link IDispatcherChannelComponent} claims to use private worker by {@link PrivateChannelWorkerRequirement#RequirePrivateChannelWorker})
 * </td>
 * </tr>
 * <tr>
 * <td>
 * No {@link IDispatcherChannelComponent}s has a preference or a requirement / all {@link IDispatcherChannelComponent}s declare {@link PrivateChannelWorkerRequirement#NoPreferenceOrRequirement}
 * </td>
 * <td>
 * Use shared Worker
 * </td>
 * </tr>
 * </table>
 * <br>
 * Private worker can be useful, if {@link IDispatcherChannelComponent} use thread sensitive libraries like Mozilla Rhino. 
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum PrivateChannelWorkerRequirement 
{
	
	/**
	 * Preference to use shared queue worker.
	 */
	PreferSharedChannelWorker(1),
	
	/**
	 * It does not matter if queue use private or shared queue worker
	 */
	NoPreferenceOrRequirement(2),
	
	/**
	 * Preference to use private queue worker.
	 */
	PreferPrivateChannelWorker(3),
	
	/**
	 * {@link IChannelComponent} requires a private queue worker.
	 */
	RequirePrivateChannelWorker(4);
	
	private PrivateChannelWorkerRequirement(int intValue)
	{
		this.intValue = intValue;
	}
	
	private static volatile Set<PrivateChannelWorkerRequirement> ALL = null;
	
	private int intValue;
	
	/**
	 * getter for all privateQueueWorker
	 * 
	 * @return Set of all privateQueueWorker
	 */
	public static Set<PrivateChannelWorkerRequirement> getAll()
	{
		if(PrivateChannelWorkerRequirement.ALL == null)
		{
			EnumSet<PrivateChannelWorkerRequirement> all = EnumSet.allOf(PrivateChannelWorkerRequirement.class);
			PrivateChannelWorkerRequirement.ALL = Collections.unmodifiableSet(all);
		}
		return PrivateChannelWorkerRequirement.ALL;
	}
	
	/**
	 * search privateQueueWorker enum represents by {@code value}
	 * 
	 * @param value integer value of privateQueueWorker
	 * 
	 * @return privateQueueWorker enum represents by {@code value}
	 */
	public static PrivateChannelWorkerRequirement findByInteger(int value)
	{
		for(PrivateChannelWorkerRequirement privateChannelWorker : getAll())
		{
			if(privateChannelWorker.intValue == value)
			{
				return privateChannelWorker;
			}
		}
		return null;
	}
	
	/**
	 * search privateQueueWorker enum represents by {@code name}
	 * 
	 * @param name of privateQueueWorker
	 * 
	 * @return enum represents by {@code name}
	 */
	public static PrivateChannelWorkerRequirement findByName(String name)
	{
		for(PrivateChannelWorkerRequirement privateChannelWorker : getAll())
		{
			if(privateChannelWorker.name().equalsIgnoreCase(name))
			{
				return privateChannelWorker;
			}
		}
		return null;
	}
}
