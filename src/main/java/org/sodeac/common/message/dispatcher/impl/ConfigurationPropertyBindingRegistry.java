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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.common.message.dispatcher.impl.ChannelManagerContainer.ControllerFilterObjects;
import org.sodeac.common.message.dispatcher.impl.ServiceContainer.ServiceFilterObjects;

public class ConfigurationPropertyBindingRegistry
{
    protected ConfigurationPropertyBindingRegistry()
    {
        super();
        this.controllerContainerIndex = new HashMap<String, Set<ChannelManagerContainer>>();
        this.serviceContainerIndex = new HashMap<String, Set<ServiceContainer>>();
        this.lock = new ReentrantLock();
    }
    
    private Map<String, Set<ChannelManagerContainer>> controllerContainerIndex = null;
    private Map<String, Set<ServiceContainer>> serviceContainerIndex = null;
    private Lock lock = null;
    
    public void register(final ChannelManagerContainer controllerContainer)
    {
        if (controllerContainer == null)
        {
            return;
        }
        
        List<ControllerFilterObjects> controllerFilterObjectsList = controllerContainer.getFilterObjectList();
        if (controllerFilterObjectsList == null)
        {
            return;
        }
        
        this.lock.lock();
        try
        {
            for (final ControllerFilterObjects controllerFilterObjects : controllerFilterObjectsList)
            {
                if ((controllerFilterObjects.attributes != null) && (!controllerFilterObjects.attributes.isEmpty()))
                {
                    for (final String attributeName : controllerFilterObjects.attributes)
                    {
                        Set<ChannelManagerContainer> controllerContainerSet = this.controllerContainerIndex.get(attributeName);
                        if (controllerContainerSet == null)
                        {
                            controllerContainerSet = new HashSet<ChannelManagerContainer>();
                            this.controllerContainerIndex.put(attributeName, controllerContainerSet);
                        }
                        controllerContainerSet.add(controllerContainer);
                    }
                }
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public Set<ChannelManagerContainer> getManagerContainer(final String... attributes)
    {
        if (attributes == null)
        {
            return null;
        }
        
        if (attributes.length == 0)
        {
            return null;
        }
        
        this.lock.lock();
        try
        {
            Set<ChannelManagerContainer> set = null;
            for (final String attribute : attributes)
            {
                Set<ChannelManagerContainer> controllerContainerSet = this.controllerContainerIndex.get(attribute);
                if (controllerContainerSet == null)
                {
                    continue;
                }
                if (set == null)
                {
                    set = new HashSet<ChannelManagerContainer>();
                }
                set.addAll(controllerContainerSet);
            }
            return set;
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public void unregister(final ChannelManagerContainer controllerContainer)
    {
        if (controllerContainer == null)
        {
            return;
        }
        
        List<ControllerFilterObjects> controllerFilterObjectsList = controllerContainer.getFilterObjectList();
        if (controllerFilterObjectsList == null)
        {
            return;
        }
        
        LinkedList<String> removeList = null;
        
        this.lock.lock();
        try
        {
            if (this.controllerContainerIndex != null)
            {
                for (final Entry<String, Set<ChannelManagerContainer>> controllerContainerSetEntry : this.controllerContainerIndex.entrySet())
                {
                    if (controllerContainerSetEntry.getValue().remove(controllerContainer))
                    {
                        
                        if (controllerContainerSetEntry.getValue().isEmpty())
                        {
                            if (removeList == null)
                            {
                                removeList = new LinkedList<String>();
                            }
                            removeList.add(controllerContainerSetEntry.getKey());
                        }
                    }
                }
                
                if (removeList != null)
                {
                    for (final String attribute : removeList)
                    {
                        this.controllerContainerIndex.remove(attribute);
                    }
                }
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public void register(final ServiceContainer serviceContainer)
    {
        if (serviceContainer == null)
        {
            return;
        }
        
        List<ServiceFilterObjects> serviceFilterObjectsList = serviceContainer.getFilterObjectList();
        if (serviceFilterObjectsList == null)
        {
            return;
        }
        
        this.lock.lock();
        try
        {
            for (final ServiceFilterObjects serviceFilterObjects : serviceFilterObjectsList)
            {
                if ((serviceFilterObjects.attributes != null) && (!serviceFilterObjects.attributes.isEmpty()))
                {
                    for (final String attributeName : serviceFilterObjects.attributes)
                    {
                        Set<ServiceContainer> serviceContainerSet = this.serviceContainerIndex.get(attributeName);
                        if (serviceContainerSet == null)
                        {
                            serviceContainerSet = new HashSet<ServiceContainer>();
                            this.serviceContainerIndex.put(attributeName, serviceContainerSet);
                        }
                        serviceContainerSet.add(serviceContainer);
                    }
                }
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public Set<ServiceContainer> getServiceContainer(final String... attributes)
    {
        if (attributes == null)
        {
            return null;
        }
        
        if (attributes.length == 0)
        {
            return null;
        }
        
        this.lock.lock();
        try
        {
            Set<ServiceContainer> set = null;
            for (final String attribute : attributes)
            {
                Set<ServiceContainer> serviceContainerSet = this.serviceContainerIndex.get(attribute);
                if (serviceContainerSet == null)
                {
                    continue;
                }
                if (set == null)
                {
                    set = new HashSet<ServiceContainer>();
                }
                set.addAll(serviceContainerSet);
            }
            return set;
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public void unregister(final ServiceContainer serviceContainer)
    {
        if (serviceContainer == null)
        {
            return;
        }
        
        List<ServiceFilterObjects> serviceFilterObjectsList = serviceContainer.getFilterObjectList();
        if (serviceFilterObjectsList == null)
        {
            return;
        }
        
        LinkedList<String> removeList = null;
        
        this.lock.lock();
        try
        {
            if (this.serviceContainerIndex != null)
            {
                for (final Entry<String, Set<ServiceContainer>> serviceContainerSetEntry : this.serviceContainerIndex.entrySet())
                {
                    if (serviceContainerSetEntry.getValue().remove(serviceContainer))
                    {
                        
                        if (serviceContainerSetEntry.getValue().isEmpty())
                        {
                            if (removeList == null)
                            {
                                removeList = new LinkedList<String>();
                            }
                            removeList.add(serviceContainerSetEntry.getKey());
                        }
                    }
                }
                
                if (removeList != null)
                {
                    for (final String attribute : removeList)
                    {
                        this.serviceContainerIndex.remove(attribute);
                    }
                }
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public void clear()
    {
        this.lock.lock();
        try
        {
            for (final Entry<String, Set<ChannelManagerContainer>> controllerContainerSetEntry : this.controllerContainerIndex.entrySet())
            {
                if (controllerContainerSetEntry.getValue() == null)
                {
                    continue;
                }
                controllerContainerSetEntry.getValue().clear();
            }
            this.controllerContainerIndex.clear();
            this.controllerContainerIndex = null;
            
            for (final Entry<String, Set<ServiceContainer>> serviceContainerSetEntry : this.serviceContainerIndex.entrySet())
            {
                if (serviceContainerSetEntry.getValue() == null)
                {
                    continue;
                }
                serviceContainerSetEntry.getValue().clear();
            }
            this.serviceContainerIndex.clear();
            this.serviceContainerIndex = null;
        }
        finally
        {
            this.lock.unlock();
        }
    }
}
