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

import java.util.Map;

import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterBuilder;

/**
 * Channel components are services bounded to any number of {@link IDispatcherChannel}s.
 * 
 * @author Sebastian Palarus *
 */
public interface IDispatcherChannelComponent
{
	public interface IDispatcherChannelComponentDriver extends IDispatcherChannelComponent,IDriver
	{
		public default int driverIsApplicableFor(Map<String,Object> properties)
		{
			return IDriver.APPLICABLE_DEFAULT;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static IFilterItem getAdapterMatchFilter(Class adapterClass)
	{
		return LDAPFilterBuilder.andLinker().criteriaWithName(adapterClass.getCanonicalName()).eq("*").build();
	}
}
