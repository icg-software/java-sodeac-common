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
package org.sodeac.common.message.dispatcher.impl;

import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemManager;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemService;
import org.sodeac.common.misc.OSGiDriverRegistry;

@Component(immediate=true,service=MessageDispatcherManagerComponent.class)
public class MessageDispatcherManagerComponent
{
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile OSGiDriverRegistry driverRegistry;
	
	private static HandleManagerUpdates handleManagerUpdates = new HandleManagerUpdates();
	private static HandleServiceUpdates handleServiceUpdates = new HandleServiceUpdates();
	
	@Activate
	public void activate()
	{
		driverRegistry.addDriverUpdateListener(IDispatcherChannelSystemManager.class, handleManagerUpdates);
		driverRegistry.addDriverUpdateListener(IDispatcherChannelSystemService.class, handleServiceUpdates);
	}
	
	@Deactivate
	public void deactivate()
	{
		driverRegistry.removeDriverUpdateListener(IDispatcherChannelSystemManager.class, handleManagerUpdates);
		driverRegistry.removeDriverUpdateListener(IDispatcherChannelSystemService.class, handleServiceUpdates);
		
		((MessageDispatcherManagerImpl)MessageDispatcherManagerImpl.get()).shutdownAllDispatcher(); 
	}
	
	private static class HandleManagerUpdates implements BiConsumer<IDispatcherChannelSystemManager, IDispatcherChannelSystemManager>
	{

		@Override
		public void accept(IDispatcherChannelSystemManager newManager, IDispatcherChannelSystemManager oldManager)
		{
			if(oldManager != null)
			{
				((MessageDispatcherManagerImpl)MessageDispatcherManagerImpl.get()).unregisterSystemChannelManager(oldManager);
			}
			if(newManager != null)
			{
				((MessageDispatcherManagerImpl)MessageDispatcherManagerImpl.get()).registerSystemChannelManager(newManager);
			}
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private static class HandleServiceUpdates implements BiConsumer<IDispatcherChannelSystemService, IDispatcherChannelSystemService>
	{
		@Override
		public void accept(IDispatcherChannelSystemService newService, IDispatcherChannelSystemService oldService)
		{
			if(oldService != null)
			{
				((MessageDispatcherManagerImpl)MessageDispatcherManagerImpl.get()).unregisterSystemChannelService(oldService);
			}
			if(newService != null)
			{
				((MessageDispatcherManagerImpl)MessageDispatcherManagerImpl.get()).registerSystemChannelService(newService);
			}
		}
	}
}
