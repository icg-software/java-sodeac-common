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

import org.sodeac.common.message.dispatcher.api.ComponentBindingSetup;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelManager;
import org.sodeac.common.message.dispatcher.api.IFeatureConfigurableManager;
import org.sodeac.common.message.dispatcher.api.IOnChannelAttach;
import org.sodeac.common.message.dispatcher.api.IOnChannelDetach;
import org.sodeac.common.message.dispatcher.api.IOnChannelSignal;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemove;
import org.sodeac.common.message.dispatcher.api.IOnMessageRemoveSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnMessageStore;
import org.sodeac.common.message.dispatcher.api.IOnMessageStoreSnapshot;
import org.sodeac.common.message.dispatcher.api.IOnTaskDone;
import org.sodeac.common.message.dispatcher.api.IOnTaskError;
import org.sodeac.common.message.dispatcher.api.IOnTaskTimeout;
import org.sodeac.common.xuri.ldapfilter.Criteria;
import org.sodeac.common.xuri.ldapfilter.CriteriaLinker;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;

public class ChannelManagerContainer
{
    protected ChannelManagerContainer
        (
            final MessageDispatcherImpl dispatcher,
            final IDispatcherChannelManager queueController,
            final List<ComponentBindingSetup.BoundedByChannelId> boundByIdList,
            final List<ComponentBindingSetup.BoundedByChannelConfiguration> boundedByQueueConfigurationList
        )
    {
        super();
        this.boundedByQueueConfigurationList = boundedByQueueConfigurationList;
        this.boundByIdList = boundByIdList;
        this.dispatcher = dispatcher;
        this.channelController = queueController;
        this.createFilterObjectList();
        this.detectControllerImplementions();
        
        if (boundByIdList != null)
        {
            for (final ComponentBindingSetup.BoundedByChannelId config : boundByIdList)
            {
                if (config.isChannelMaster())
                {
                    this.channelMaster = true;
                    break;
                }
            }
        }
    }
    
    private MessageDispatcherImpl dispatcher = null;
    private volatile IDispatcherChannelManager channelController = null;
    private List<ComponentBindingSetup.BoundedByChannelId> boundByIdList = null;
    private List<ComponentBindingSetup.BoundedByChannelConfiguration> boundedByQueueConfigurationList = null;
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
    private volatile boolean implementsIOnMessageRemove = false;
    private volatile boolean implementsIOnMessageStoreSnapshot = false;
    private volatile boolean implementsIOnMessageRemoveSnapshot = false;
    
    public void detectControllerImplementions()
    {
        if (this.channelController == null)
        {
            this.implementsIOnTaskDone = false;
            this.implementsIOnTaskError = false;
            this.implementsIOnTaskTimeout = false;
            this.implementsIOnChannelAttach = false;
            this.implementsIOnChannelDetach = false;
            this.implementsIOnChannelSignal = false;
            this.implementsIOnMessageStore = false;
            this.implementsIOnMessageRemove = false;
            this.implementsIOnTaskTimeout = false;
            this.implementsIOnMessageStoreSnapshot = false;
            this.implementsIOnMessageRemoveSnapshot = false;
            return;
        }
        
        if (this.channelController instanceof final IFeatureConfigurableManager featureConfigurableController)
        {
            this.implementsIOnTaskDone = featureConfigurableController.implementsOnTaskDone();
            this.implementsIOnTaskError = featureConfigurableController.implementsOnTaskError();
            this.implementsIOnTaskTimeout = featureConfigurableController.implementsOnTaskTimeout();
            this.implementsIOnChannelAttach = featureConfigurableController.implementsOnChannelAttach();
            this.implementsIOnChannelDetach = featureConfigurableController.implementsOnChannelDetach();
            this.implementsIOnChannelSignal = featureConfigurableController.implementsOnChannelSignal();
            this.implementsIOnMessageStore = featureConfigurableController.implementsOnMessageStore();
            this.implementsIOnMessageRemove = featureConfigurableController.implementsOnMessageRemove();
            this.implementsIOnMessageStoreSnapshot = featureConfigurableController.implementsOnMessageStoreSnapshot();
            this.implementsIOnMessageRemoveSnapshot = featureConfigurableController.implementsOnMessageRemoveSnapshot();
        }
        else
        {
            this.implementsIOnTaskDone = this.channelController instanceof IOnTaskDone;
            this.implementsIOnTaskError = this.channelController instanceof IOnTaskError;
            this.implementsIOnTaskTimeout = this.channelController instanceof IOnTaskTimeout;
            this.implementsIOnChannelAttach = this.channelController instanceof IOnChannelAttach;
            this.implementsIOnChannelDetach = this.channelController instanceof IOnChannelDetach;
            this.implementsIOnChannelSignal = this.channelController instanceof IOnChannelSignal;
            this.implementsIOnMessageStore = this.channelController instanceof IOnMessageStore;
            this.implementsIOnMessageRemove = this.channelController instanceof IOnMessageRemove;
            this.implementsIOnMessageStoreSnapshot = this.channelController instanceof IOnMessageStoreSnapshot;
            this.implementsIOnMessageRemoveSnapshot = this.channelController instanceof IOnMessageRemoveSnapshot;
        }
    }
    
    private void createFilterObjectList()
    {
        List<ControllerFilterObjects> list = new ArrayList<ControllerFilterObjects>();
        if (this.boundedByQueueConfigurationList != null)
        {
            for (final ComponentBindingSetup.BoundedByChannelConfiguration boundedByQueueConfiguration : this.boundedByQueueConfigurationList)
            {
                if (boundedByQueueConfiguration.getLdapFilter() == null)
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
                    
                    while (!discoverLDAPItem.isEmpty())
                    {
                        filter = discoverLDAPItem.removeFirst();
                        
                        if (filter instanceof Criteria)
                        {
                            controllerFilterObjects.attributes.add(((Criteria) filter).getName());
                        }
                        else if (filter instanceof CriteriaLinker)
                        {
                            discoverLDAPItem.addAll(((CriteriaLinker) filter).getLinkedItemList());
                        }
                    }
                    
                    list.add(controllerFilterObjects);
                }
                catch (final Exception e)
                {
                    this.dispatcher.logError("parse bounded channel configuration " + boundedByQueueConfiguration.getLdapFilter(), e);
                }
            }
        }
        this.filterObjectList = list;
        this.filterAttributes = new HashSet<String>();
        for (final ControllerFilterObjects controllerFilterObjects : this.filterObjectList)
        {
            if (controllerFilterObjects.attributes != null)
            {
                for (final String attribute : controllerFilterObjects.attributes)
                {
                    this.filterAttributes.add(attribute);
                }
            }
        }
    }
    
    public IDispatcherChannelManager getChannelManager()
    {
        return this.channelController;
    }
    
    public boolean isRegistered()
    {
        return this.registered;
    }
    
    public void setRegistered(final boolean registered)
    {
        this.registered = registered;
    }
    
    public List<ComponentBindingSetup.BoundedByChannelConfiguration> getBoundedByChannelConfigurationList()
    {
        return this.boundedByQueueConfigurationList;
    }
    
    public List<ComponentBindingSetup.BoundedByChannelId> getBoundByIdList()
    {
        return this.boundByIdList;
    }
    
    public List<ControllerFilterObjects> getFilterObjectList()
    {
        return this.filterObjectList;
    }
    
    public Set<String> getFilterAttributeSet()
    {
        return this.filterAttributes;
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
        ComponentBindingSetup.BoundedByChannelConfiguration bound = null;
        IFilterItem filter = null;
        Set<String> attributes = new HashSet<String>();
    }
    
    public boolean isImplementingIOnTaskDone()
    {
        return this.implementsIOnTaskDone;
    }
    
    public boolean isImplementingIOnTaskError()
    {
        return this.implementsIOnTaskError;
    }
    
    public boolean isImplementingIOnChannelAttach()
    {
        return this.implementsIOnChannelAttach;
    }
    
    public boolean isImplementingIOnChannelDetach()
    {
        return this.implementsIOnChannelDetach;
    }
    
    public boolean isImplementingIOnChannelSignal()
    {
        return this.implementsIOnChannelSignal;
    }
    
    public boolean isImplementingIOnMessageStore()
    {
        return this.implementsIOnMessageStore;
    }
    
    public boolean isImplementingIOnMessageRemove()
    {
        return this.implementsIOnMessageRemove;
    }
    
    public boolean isImplementingIOnMessageStoreSnapshot()
    {
        return this.implementsIOnMessageStoreSnapshot;
    }
    
    public boolean isImplementingIOnMessageRemoveSnapshot()
    {
        return this.implementsIOnMessageRemoveSnapshot;
    }
    
    public boolean isImplementingIOnTaskTimeout()
    {
        return this.implementsIOnTaskTimeout;
    }
    
    public List<ComponentBindingSetup> getComponentConfigurationList()
    {
        List<ComponentBindingSetup> list = new ArrayList<ComponentBindingSetup>();
        
        if (this.boundByIdList != null)
        {
            list.addAll(this.boundByIdList);
        }
        if (this.boundedByQueueConfigurationList != null)
        {
            list.addAll(this.boundedByQueueConfigurationList);
        }
        
        return list;
    }
    
}
