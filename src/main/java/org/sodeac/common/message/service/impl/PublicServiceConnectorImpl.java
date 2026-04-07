/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.message.service.api.IServiceConnection;
import org.sodeac.common.message.service.api.IServiceConnector;
import org.sodeac.common.misc.Driver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.xuri.URI;

@Component(service=IServiceConnector.class,immediate=true)
public class PublicServiceConnectorImpl implements IServiceConnector
{
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile OSGiDriverRegistry internalBootstrapDep;
	
	private volatile IServiceConnector localConnector = null;
	
	@Override
	public int driverIsApplicableFor(Map<String, Object> properties)
	{
		if((properties == null) || properties.isEmpty())
		{
			return IDriver.APPLICABLE_DEFAULT;
		}
		return IDriver.APPLICABLE_NONE;
	}

	@Override
	public IServiceConnection lookup(URI serviceURI)
	{
		IServiceConnector localConnector = this.localConnector;
		if(localConnector == null)
		{
			Map<String,Object> properties = new HashMap<String, Object>();
			properties.put(IDriver.TYPE, IServiceConnector.TYPE_LOCAL);
			localConnector = Driver.getSingleDriver(IServiceConnector.class, properties);
			if(localConnector != null)
			{
				this.localConnector = localConnector;
			}
			else
			{
				return null;
			}
		}
		IServiceConnection serviceConnection = localConnector.lookup(serviceURI);
		return serviceConnection;
	}

}
