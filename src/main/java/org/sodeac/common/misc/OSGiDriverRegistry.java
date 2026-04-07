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
package org.sodeac.common.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.sodeac.common.misc.Driver.IDriver;

@Component(immediate = true, service = OSGiDriverRegistry.class)
public class OSGiDriverRegistry
{
    private ComponentContext componentContext;
    protected static OSGiDriverRegistry INSTANCE;
    private final Lock lock;
    private Map<Class, DriverServiceTracker> trackerIndex = new HashMap<Class, OSGiDriverRegistry.DriverServiceTracker>();
    
    // TODO ungetService concept ? is coupled with release driver
    
    public OSGiDriverRegistry()
    {
        super();
        this.lock = new ReentrantLock();
    }
    
    @Activate
    public void activate(final ComponentContext componentContext)
    {
        this.componentContext = componentContext;
        OSGiDriverRegistry.INSTANCE = this;
    }
    
    @Deactivate
    public void deactivate(final ComponentContext componentContext)
    {
        List<DriverServiceTracker> values = null;
        this.lock.lock();
        try
        {
            values = new ArrayList<OSGiDriverRegistry.DriverServiceTracker>(this.trackerIndex.values());
            this.trackerIndex.clear();
            this.trackerIndex = null;
        }
        finally
        {
            this.lock.unlock();
        }
        
        for (final DriverServiceTracker driverServiceTracker : values)
        {
            try
            {
                driverServiceTracker.close();
            }
            catch (final Exception e) { }
        }
        this.componentContext = null;
        OSGiDriverRegistry.INSTANCE = null;
    }
    
    public <T extends IDriver> void observe(final Class<T> driverClass)
    {
        this.lock.lock();
        try
        {
            if (this.trackerIndex.containsKey(driverClass))
            {
                return;
            }
            
            DriverServiceTracker driverServiceTracker = new DriverServiceTracker(this.componentContext.getBundleContext(), driverClass, new Customizer()); // BundleContext of Class?
            this.trackerIndex.put(driverClass, driverServiceTracker);
            driverServiceTracker.open(true);
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public <T extends IDriver> boolean addDriverUpdateListener(final Class<T> driverClass, final BiConsumer<T, T> updateListener)
    {
        if (this.lock == null)
        {
            return false;
        }
        
        this.lock.lock();
        try
        {
            DriverServiceTracker tracker = this.trackerIndex.get(driverClass);
            if (tracker == null)
            {
                observe(driverClass);
                tracker = this.trackerIndex.get(driverClass);
            }
            tracker.addUpdateListener(updateListener);
        }
        finally
        {
            this.lock.unlock();
        }
        return true;
    }
    
    public <T extends IDriver> boolean removeDriverUpdateListener(final Class<T> driverClass, final BiConsumer<T, T> updateListener)
    {
        if (this.lock == null)
        {
            return false;
        }
        
        this.lock.lock();
        try
        {
            DriverServiceTracker tracker = this.trackerIndex.get(driverClass);
            if (tracker == null)
            {
                observe(driverClass);
                tracker = this.trackerIndex.get(driverClass);
            }
            tracker.removeUpdateListener(updateListener);
        }
        finally
        {
            this.lock.unlock();
        }
        return true;
    }
    
    public <T extends IDriver> T getSingleDriver(final Class<T> driverClass, final Map<String, Object> properties)
    {
        this.lock.lock();
        try
        {
            DriverServiceTracker tracker = this.trackerIndex.get(driverClass);
            if (tracker == null)
            {
                observe(driverClass);
                tracker = this.trackerIndex.get(driverClass);
            }
            return (T) tracker.getSingleDriver(properties);
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    public <T extends IDriver> List<T> getDriverList(final Class<T> driverClass, final Map<String, Object> properties)
    {
        this.lock.lock();
        try
        {
            DriverServiceTracker tracker = this.trackerIndex.get(driverClass);
            if (tracker == null)
            {
                observe(driverClass);
                tracker = this.trackerIndex.get(driverClass);
            }
            return (List) tracker.getDriverList(properties);
        }
        finally
        {
            this.lock.unlock();
        }
    }
    
    protected class DriverServiceTracker extends ServiceTracker
    {
        private Class clazz = null;
        private Lock lock = null;
        
        private final List<BiConsumer<? extends IDriver, ? extends IDriver>> updateListenerList = new ArrayList<>();
        private Map<String, List<ServiceContainer>> listsByClassName = null;
        
        public DriverServiceTracker(final BundleContext context, final Class clazz, final Customizer customizer)
        {
            super(context, clazz, customizer);
            this.clazz = clazz;
            this.lock = new ReentrantLock();
            customizer.setTracker(this);
            this.listsByClassName = new HashMap<String, List<ServiceContainer>>();
        }
        
        public void addUpdateListener(final BiConsumer<? extends IDriver, ? extends IDriver> updateListener)
        {
            if (this.lock == null)
            {
                return;
            }
            
            this.lock.lock();
            try
            {
                for (final BiConsumer<? extends IDriver, ? extends IDriver> check : this.updateListenerList)
                {
                    if (check == updateListener)
                    {
                        return;
                    }
                }
                this.updateListenerList.add(updateListener);
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        public void removeUpdateListener(final BiConsumer<? extends IDriver, ? extends IDriver> updateListener)
        {
            if (this.lock == null)
            {
                return;
            }
            
            this.lock.lock();
            try
            {
                Stack<Integer> delete = new Stack<>();
                int index = 0;
                for (final BiConsumer<? extends IDriver, ? extends IDriver> check : this.updateListenerList)
                {
                    if (check == updateListener)
                    {
                        delete.push(index);
                    }
                    index++;
                }
                while (!delete.isEmpty())
                {
                    this.updateListenerList.remove((int) delete.pop());
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public void close()
        {
            super.close();
            if (this.lock == null)
            {
                return;
            }
            this.lock.lock();
            try
            {
                this.listsByClassName.values().forEach(i -> i.clear());
                this.listsByClassName.clear();
                this.updateListenerList.clear();
            }
            finally
            {
                this.lock.unlock();
            }
            this.lock = null;
            this.clazz = null;
            this.listsByClassName = null;
        }
        
        public Object getSingleDriver(final Map<String, Object> properties)
        {
            if (this.lock == null)
            {
                return null;
            }
            this.lock.lock();
            try
            {
                Object bestDriver = null;
                int bestIndex = -1;
                for (final List<ServiceContainer> serviceReferenceList : this.listsByClassName.values())
                {
                    if (serviceReferenceList.isEmpty())
                    {
                        continue;
                    }
                    Object driver = null;
                    for (final ServiceContainer container : serviceReferenceList)
                    {
                        driver = container.getService();
                        if (driver != null)
                        {
                            break;
                        }
                    }
                    if (driver == null)
                    {
                        continue;
                    }
                    
                    int applicableIndex = ((IDriver) driver).driverIsApplicableFor(properties);
                    if (applicableIndex > bestIndex)
                    {
                        bestDriver = driver;
                        bestIndex = applicableIndex;
                    }
                }
                return bestDriver;
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        public List<Object> getDriverList(final Map<String, Object> properties)
        {
            if (this.lock == null)
            {
                return null;
            }
            this.lock.lock();
            try
            {
                List<Object> driverList = new ArrayList<Object>();
                for (final List<ServiceContainer> serviceReferenceList : this.listsByClassName.values())
                {
                    if (serviceReferenceList.isEmpty())
                    {
                        continue;
                    }
                    Object driver = null;
                    for (final ServiceContainer container : serviceReferenceList)
                    {
                        driver = container.getService();
                        if (driver != null)
                        {
                            break;
                        }
                    }
                    if (driver == null)
                    {
                        continue;
                    }
                    
                    int applicableIndex = ((IDriver) driver).driverIsApplicableFor(properties);
                    if (applicableIndex > IDriver.APPLICABLE_NONE)
                    {
                        driverList.add(driver);
                    }
                }
                return driverList;
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        public void addDriver(final ServiceReference reference, final Object driver)
        {
            if (driver == null)
            {
                return;
            }
            if (!(driver instanceof IDriver))
            {
                return;
            }
            if (!this.clazz.isInstance(driver))
            {
                return;
            }
            if (reference == null)
            {
                return;
            }
            if (this.lock == null)
            {
                return;
            }
            this.lock.lock();
            try
            {
                List<ServiceContainer> list = this.listsByClassName.get(driver.getClass().getCanonicalName());
                if (list == null)
                {
                    list = new ArrayList<>();
                    this.listsByClassName.put(driver.getClass().getCanonicalName(), list);
                }
                ServiceContainer oldContainer = null;
                for (final ServiceContainer container : list)
                {
                    if (container.getServiceReference() == reference)
                    {
                        return;
                    }
                    if (oldContainer == null)
                    {
                        oldContainer = container;
                    }
                }
                list.add(new ServiceContainer(reference, driver));
                
                Collections.sort(list, Collections.reverseOrder(new Comparator<ServiceContainer>()
                                 {
                                     
                                     @Override
                                     public int compare(final ServiceContainer o1, final ServiceContainer o2)
                                     {
                                         ServiceReference sr1 = o1.getServiceReference();
                                         ServiceReference sr2 = o2.getServiceReference();
                                         
                                         if ((sr1 == null) || (sr2 == null))
                                         {
                                             return 0;
                                         }
                                         Bundle bundle1 = sr1.getBundle();
                                         Bundle bundle2 = sr2.getBundle();
                                         if ((bundle1 == null) || (bundle2 == null))
                                         {
                                             return 0;
                                         }
                                         Version version1 = bundle1.getVersion();
                                         Version version2 = bundle2.getVersion();
                                         if ((version1 == null) || (version2 == null))
                                         {
                                             return 0;
                                         }
                                         return version1.compareTo(version2);
                                     }
                                 })
                );
                
                if (oldContainer != list.get(0))
                {
                    for (final BiConsumer updateListener : this.updateListenerList)
                    {
                        try
                        {
                            updateListener.accept(list.get(0).getService(), oldContainer == null ? null : oldContainer.getService());
                        }
                        catch (final Exception e) { }
                    }
                }
                
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        public void removeDriver(final ServiceReference reference, final Object driver)
        {
            if (this.lock == null)
            {
                return;
            }
            this.lock.lock();
            try
            {
                Set<String> toRemoveLists = new HashSet<>();
                for (final Entry<String, List<ServiceContainer>> entry : this.listsByClassName.entrySet())
                {
                    List<ServiceContainer> list = entry.getValue();
                    LinkedList<Integer> toRemovePositions = new LinkedList<>();
                    int index = 0;
                    for (final ServiceContainer serviceContainer : list)
                    {
                        if (serviceContainer.getServiceReference() == reference)
                        {
                            toRemovePositions.addFirst(index);
                        }
                        index++;
                    }
                    if (toRemovePositions.isEmpty())
                    {
                        continue;
                    }
                    ServiceContainer oldFirstContainer = list.get(0);
                    for (final Integer toRemove : toRemovePositions)
                    {
                        list.remove((int) toRemove);
                    }
                    ServiceContainer newFirstContainer = list.isEmpty() ? null : list.get(0);
                    
                    if (newFirstContainer != oldFirstContainer)
                    {
                        for (final BiConsumer updateListener : this.updateListenerList)
                        {
                            try
                            {
                                updateListener.accept(newFirstContainer == null ? null : newFirstContainer.getService(), oldFirstContainer.getService());
                            }
                            catch (final Exception e) { }
                        }
                    }
                    
                    if (list.isEmpty())
                    {
                        toRemoveLists.add(entry.getKey());
                    }
                }
                for (final String toRemove : toRemoveLists)
                {
                    this.listsByClassName.remove(toRemove);
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        protected BundleContext getContext()
        {
            return super.context;
        }
        
        private class ServiceContainer
        {
            private ServiceContainer(final ServiceReference serviceReference, final Object service)
            {
                super();
                this.serviceReference = serviceReference;
                this.service = service;
            }
            
            private ServiceReference serviceReference = null;
            private Object service = null;
            
            public ServiceReference getServiceReference()
            {
                return this.serviceReference;
            }
            
            public Object getService()
            {
                return this.service;
            }
        }
        
        public Class getClazz()
        {
            return this.clazz;
        }
    }
    
    protected static class Customizer implements ServiceTrackerCustomizer
    {
        DriverServiceTracker tracker = null;
        
        public Customizer()
        {
            super();
        }
        
        public void setTracker(final DriverServiceTracker tracker)
        {
            this.tracker = tracker;
        }
        
        @Override
        public Object addingService(final ServiceReference reference)
        {
            
            // Object driver = tracker.getContext().getService(reference);
            Object driver = FrameworkUtil.getBundle(this.tracker.getClazz()).getBundleContext().getService(reference);
            this.tracker.addDriver(reference, driver);
            return driver;
        }
        
        @Override
        public void modifiedService(final ServiceReference reference, final Object service) { }
        
        @Override
        public void removedService(final ServiceReference reference, final Object service)
        {
            this.tracker.removeDriver(reference, service);
            FrameworkUtil.getBundle(this.tracker.getClazz()).getBundleContext().ungetService(reference);
        }
        
    }
}
