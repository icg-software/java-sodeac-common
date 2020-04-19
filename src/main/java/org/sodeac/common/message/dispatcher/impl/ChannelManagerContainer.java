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
package org.sodeac.common.message.dispatcher.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sodeac.common.message.dispatcher.api.DispatcherChannelSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager;
import org.sodeac.common.message.dispatcher.api.IFeatureConfigurableManager;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelDetach;
import org.sodeac.common.message.dispatcher.api.IOnChannelSignal;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemove;
import org.sodeac.common.message.dispatcher.api.IOnMessageStore;
import org.sodeac.common.message.dispatcher.api.IOnTaskDone;
import org.sodeac.common.message.dispatcher.api.IOnTaskError;
import org.sodeac.common.message.dispatcher.api.IOnTaskTimeout;
import org.sodeac.common.xuri.ldapfilter.Criteria;
import org.sodeac.common.xuri.ldapfilter.CriteriaLinker;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterDecodingHandler;

public class ChannelManagerContainer
{
	protected ChannelManagerContainer
	(
		MessageDispatcherImpl dispatcher,
		IDispatcherChannelManager queueController, 
		List<DispatcherChannelSetup.BoundedByChannelId> boundByIdList, 
		List<DispatcherChannelSetup.BoundedByChannelConfiguration> boundedByQueueConfigurationList
	)
	{
		super();
		this.boundedByQueueConfigurationList = boundedByQueueConfigurationList;
		this.boundByIdList = boundByIdList;
		this.dispatcher = dispatcher;
		this.channelController = queueController;
		this.createFilterObjectList();
		this.detectControllerImplementions();
		
		if(boundByIdList != null)
		{
			for(DispatcherChannelSetup.BoundedByChannelId config : boundByIdList)
			{
				if(config.isChannelMaster())
				{
					this.channelMaster = true;
					break;
				}
			}
		}
	}
	
	private MessageDispatcherImpl dispatcher = null;
	private volatile IDispatcherChannelManager channelController = null;
	private List<DispatcherChannelSetup.BoundedByChannelId> boundByIdList = null;
	private List<DispatcherChannelSetup.BoundedByChannelConfiguration> boundedByQueueConfigurationList = null;
	private boolean channelMaster = false;
	
	private volatile boolean registered = false;
	
	private volatile List<ControllerFilterObjects> filterObjectList;
	private volatile Set<String> filterAttributes;
	
	private volatile boolean implementsIOnTaskDone = false;
	private volatile boolean implementsIOnTaskError = false;
	private volatile boolean implementsIOnTaskTimeout = false;
	private volatile boolean implementsIOnChannelAttach = false;
	private volatile boolean implementsIOnChannelDetach = false;
	private volatile boolean implementsIOnChannelSignal = false;
	private volatile boolean implementsIOnMessageStore = false;
	private volatile boolean implementsIOnRemoveMessage = false;
	
	public void detectControllerImplementions()
	{
		if(this.channelController == null)
		{
			implementsIOnTaskDone = false;
			implementsIOnTaskError = false;
			implementsIOnTaskTimeout = false;
			implementsIOnChannelAttach = false;
			implementsIOnChannelDetach = false;
			implementsIOnChannelSignal = false;
			implementsIOnMessageStore = false;
			implementsIOnRemoveMessage = false;
			implementsIOnTaskTimeout = false;
			return;
		}
		
		
		if(this.channelController instanceof IFeatureConfigurableManager)
		{
			IFeatureConfigurableManager featureConfigurableController = (IFeatureConfigurableManager)this.channelController;
			implementsIOnTaskDone = featureConfigurableController.implementsOnTaskDone();
			implementsIOnTaskError = featureConfigurableController.implementsOnTaskError();
			implementsIOnTaskTimeout = featureConfigurableController.implementsOnTaskTimeout();
			implementsIOnChannelAttach = featureConfigurableController.implementsOnChannelAttach();
			implementsIOnChannelDetach = featureConfigurableController.implementsOnChannelDetach();
			implementsIOnChannelSignal = featureConfigurableController.implementsOnChannelSignal();
			implementsIOnMessageStore = featureConfigurableController.implementsOnMessageStore();
			implementsIOnRemoveMessage = featureConfigurableController.implementsOnMessageRemove();
		}
		else
		{
			implementsIOnTaskDone = this.channelController instanceof IOnTaskDone;
			implementsIOnTaskError = this.channelController instanceof IOnTaskError;
			implementsIOnTaskTimeout = this.channelController instanceof IOnTaskTimeout;
			implementsIOnChannelAttach = this.channelController instanceof IOnChannelAttach;
			implementsIOnChannelDetach = this.channelController instanceof IOnChannelDetach;
			implementsIOnChannelSignal = this.channelController instanceof IOnChannelSignal;
			implementsIOnMessageStore = this.channelController instanceof IOnMessageStore;
			implementsIOnRemoveMessage = this.channelController instanceof IOnMessageRemove;
		}
	}
	
	private void createFilterObjectList()
	{
		List<ControllerFilterObjects> list = new ArrayList<ControllerFilterObjects>();
		if(this.boundedByQueueConfigurationList != null)
		{
			for(DispatcherChannelSetup.BoundedByChannelConfiguration boundedByQueueConfiguration : boundedByQueueConfigurationList)
			{
				if(boundedByQueueConfiguration.getLdapFilter() == null)
				{
					continue;
				}
				ControllerFilterObjects controllerFilterObjects = new ControllerFilterObjects();
				controllerFilterObjects.bound = boundedByQueueConfiguration;
				controllerFilterObjects.filter = boundedByQueueConfiguration.getLdapFilter();
				
				try
				{
					LinkedList<IFilterItem> discoverLDAPItem = new LinkedList<IFilterItem>();
					IFilterItem filter = controllerFilterObjects.filter;
					
					discoverLDAPItem.addLast(filter);
					
					while(! discoverLDAPItem.isEmpty())
					{
						filter = discoverLDAPItem.removeFirst();
						
						if(filter instanceof Criteria) 
						{
							controllerFilterObjects.attributes.add(((Criteria)filter).getName());
						}
						else if(filter instanceof CriteriaLinker)
						{
							discoverLDAPItem.addAll(((CriteriaLinker)filter).getLinkedItemList());
						}
					}
					
					list.add(controllerFilterObjects);
				}
				catch (Exception e) 
				{
					dispatcher.logError("parse bounded queue configuration " + boundedByQueueConfiguration.getLdapFilter(),e);
				}
			}
		}
		this.filterObjectList = list;
		this.filterAttributes = new HashSet<String>();
		for(ControllerFilterObjects controllerFilterObjects : this.filterObjectList)
		{
			if(controllerFilterObjects.attributes != null)
			{
				for(String attribute : controllerFilterObjects.attributes)
				{
					this.filterAttributes.add(attribute);
				}
			}
		}
	}
	
	public IDispatcherChannelManager getChannelManager()
	{
		return channelController;
	}
	public boolean isRegistered()
	{
		return registered;
	}
	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}
	public List<DispatcherChannelSetup.BoundedByChannelConfiguration> getBoundedByChannelConfigurationList()
	{
		return boundedByQueueConfigurationList;
	}
	public List<DispatcherChannelSetup.BoundedByChannelId> getBoundByIdList()
	{
		return boundByIdList;
	}
	
	public List<ControllerFilterObjects> getFilterObjectList()
	{
		return filterObjectList;
	}

	public Set<String> getFilterAttributeSet()
	{
		return filterAttributes;
	}
	
	public boolean isChannelMaster()
	{
		return this.channelMaster;
	}
	
	public void clean()
	{
		this.dispatcher = null;
		this.channelController = null;
		this.boundByIdList = null;
		this.boundedByQueueConfigurationList = null;
		this.filterObjectList = null;
		this.filterAttributes = null;
	}
	
	public class ControllerFilterObjects
	{
		DispatcherChannelSetup.BoundedByChannelConfiguration bound = null;
		IFilterItem filter = null;
		Set<String> attributes = new HashSet<String>();
	}

	public boolean isImplementingIOnTaskDone()
	{
		return implementsIOnTaskDone;
	}

	public boolean isImplementingIOnTaskError()
	{
		return implementsIOnTaskError;
	}

	public boolean isImplementingIOnChannelAttach()
	{
		return implementsIOnChannelAttach;
	}

	public boolean isImplementingIOnChannelDetach()
	{
		return implementsIOnChannelDetach;
	}

	public boolean isImplementingIOnChannelSignal()
	{
		return implementsIOnChannelSignal;
	}

	public boolean isImplementingIOnMessageStored()
	{
		return implementsIOnMessageStore;
	}

	public boolean isImplementingIOnRemoveMessage()
	{
		return implementsIOnRemoveMessage;
	}

	public boolean isImplementingIOnTaskTimeout()
	{
		return implementsIOnTaskTimeout;
	}
	
	public List<DispatcherChannelSetup> getComponentConfigurationList()
	{
		List<DispatcherChannelSetup> list = new ArrayList<DispatcherChannelSetup>();
		
		if(boundByIdList != null)
		{
			list.addAll(boundByIdList);
		}
		if(boundedByQueueConfigurationList != null)
		{
			list.addAll(boundedByQueueConfigurationList);
		}
		
		return list;
	}
	
}
