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

@Component(immediate=true,service=OSGiDriverRegistry.class)
public class OSGiDriverRegistry
{
	private ComponentContext componentContext;
	protected static OSGiDriverRegistry INSTANCE;
	private Lock lock;
	private Map<Class,DriverServiceTracker> trackerIndex = new HashMap<Class, OSGiDriverRegistry.DriverServiceTracker>();
	
	// TODO ungetService concept ? is coupled with release driver 
	
	public OSGiDriverRegistry()
	{
		super();
		this.lock = new ReentrantLock();
	}
	
	@Activate
	public void activate(ComponentContext componentContext)
	{
		this.componentContext = componentContext;
		OSGiDriverRegistry.INSTANCE = this;
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext)
	{
		List<DriverServiceTracker> values = null;
		lock.lock();
		try
		{
			values = new ArrayList<OSGiDriverRegistry.DriverServiceTracker>(trackerIndex.values());
			trackerIndex.clear();
			trackerIndex = null;
		}
		finally 
		{
			lock.unlock();
		}
		
		for(DriverServiceTracker driverServiceTracker : values)
		{
			try
			{
				driverServiceTracker.close();
			}
			catch (Exception e) {}
		}
		this.componentContext = null;
		OSGiDriverRegistry.INSTANCE = null;
	}
	
	public <T extends IDriver> void observe(Class<T> driverClass)
	{
		lock.lock();
		try
		{
			if(trackerIndex.containsKey(driverClass))
			{
				return;
			}
			
			DriverServiceTracker driverServiceTracker = new DriverServiceTracker(this.componentContext.getBundleContext(), driverClass, new Customizer()); // BundleContext of Class?
			this.trackerIndex.put(driverClass, driverServiceTracker);
			driverServiceTracker.open(true);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	public <T extends IDriver> boolean addDriverUpdateListener(Class<T> driverClass, BiConsumer<T, T> updateListener)
	{
		if(lock == null)
		{
			return false;
		}
		
		lock.lock();
		try
		{
			DriverServiceTracker tracker = trackerIndex.get(driverClass);
			if(tracker == null)
			{
				observe(driverClass);
				tracker = trackerIndex.get(driverClass);
			}
			tracker.addUpdateListener(updateListener);
		}
		finally 
		{
			lock.unlock();
		}
		return true;
	}
	
	public <T extends IDriver> boolean removeDriverUpdateListener(Class<T> driverClass, BiConsumer<T, T> updateListener)
	{
		if(lock == null)
		{
			return false;
		}
		
		lock.lock();
		try
		{
			DriverServiceTracker tracker = trackerIndex.get(driverClass);
			if(tracker == null)
			{
				observe(driverClass);
				tracker = trackerIndex.get(driverClass);
			}
			tracker.removeUpdateListener(updateListener);
		}
		finally 
		{
			lock.unlock();
		}
		return true;
	}
	
	public <T extends IDriver> T getSingleDriver(Class<T> driverClass, Map<String,Object> properties)
	{
		lock.lock();
		try
		{
			DriverServiceTracker tracker = trackerIndex.get(driverClass);
			if(tracker == null)
			{
				observe(driverClass);
				tracker = trackerIndex.get(driverClass);
			}
			return (T)tracker.getSingleDriver(properties);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	public <T extends IDriver> List<T> getDriverList(Class<T> driverClass, Map<String,Object> properties)
	{
		lock.lock();
		try
		{
			DriverServiceTracker tracker = trackerIndex.get(driverClass);
			if(tracker == null)
			{
				observe(driverClass);
				tracker = trackerIndex.get(driverClass);
			}
			return (List)tracker.getDriverList(properties);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	protected class DriverServiceTracker extends ServiceTracker
	{
		private Class clazz = null;
		private Lock lock = null;
		
		private List<BiConsumer<? extends IDriver, ? extends IDriver>> updateListenerList = new ArrayList<>();
		private Map<String,List<ServiceContainer>> listsByClassName = null;
		
		public DriverServiceTracker(BundleContext context, Class clazz, Customizer customizer)
		{
			super(context, clazz,customizer);
			this.clazz = clazz;
			this.lock = new ReentrantLock();
			customizer.setTracker(this);
			this.listsByClassName = new HashMap<String,List<ServiceContainer>>();
		}
		
		public void addUpdateListener(BiConsumer<? extends IDriver, ? extends IDriver> updateListener)
		{
			if(lock == null)
			{
				return;
			}
			
			lock.lock();
			try
			{
				for(BiConsumer<? extends IDriver, ? extends IDriver> check : updateListenerList)
				{
					if(check == updateListener)
					{
						return;
					}
				}
				this.updateListenerList.add(updateListener);
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		public void removeUpdateListener(BiConsumer<? extends IDriver, ? extends IDriver> updateListener)
		{
			if(lock == null)
			{
				return;
			}
			
			lock.lock();
			try
			{
				Stack<Integer> delete = new Stack<>();
				int index = 0;
				for(BiConsumer<? extends IDriver, ? extends IDriver> check : updateListenerList)
				{
					if(check == updateListener)
					{
						delete.push(index);
					}
					index++;
				}
				while(! delete.isEmpty())
				{
					this.updateListenerList.remove((int)delete.pop());
				}
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		@Override
		public void close()
		{
			super.close();
			if(lock == null)
			{
				return;
			}
			lock.lock();
			try
			{
				listsByClassName.values().forEach(i -> i.clear());
				listsByClassName.clear();
				updateListenerList.clear();
			}
			finally 
			{
				lock.unlock();
			}
			lock = null;
			clazz = null;
			listsByClassName = null;
		}
		
		public Object getSingleDriver(Map<String,Object> properties)
		{
			if(lock == null)
			{
				return null;
			}
			lock.lock();
			try
			{
				Object bestDriver = null;
				int bestIndex = -1;
				for(List<ServiceContainer> serviceReferenceList : this.listsByClassName.values())
				{
					if(serviceReferenceList.isEmpty())
					{
						continue;
					}
					Object driver = null;
					for(ServiceContainer container : serviceReferenceList)
					{
						driver = container.getService();
						if(driver != null)
						{
							break;
						}
					}
					if(driver == null)
					{
						continue;
					}
					
					int applicableIndex = ((IDriver)driver).driverIsApplicableFor(properties);
					if(applicableIndex > bestIndex)
					{
						bestDriver = driver;
						bestIndex = applicableIndex;
					}
				}
				return bestDriver;
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		public List<Object> getDriverList(Map<String,Object> properties)
		{
			if(lock == null)
			{
				return null;
			}
			lock.lock();
			try
			{
				List<Object> driverList = new ArrayList<Object>();
				for(List<ServiceContainer> serviceReferenceList : this.listsByClassName.values())
				{
					if(serviceReferenceList.isEmpty())
					{
						continue;
					}
					Object driver = null;
					for(ServiceContainer container : serviceReferenceList)
					{
						driver = container.getService();
						if(driver != null)
						{
							break;
						}
					}
					if(driver == null)
					{
						continue;
					}
					
					int applicableIndex = ((IDriver)driver).driverIsApplicableFor(properties);
					if(applicableIndex > IDriver.APPLICABLE_NONE)
					{
						driverList.add(driver);
					}
				}
				return driverList;
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		public void addDriver(ServiceReference reference, Object driver)
		{
			if(driver == null)
			{
				return;
			}
			if(!(driver instanceof IDriver))
			{
				return;
			}
			if(!this.clazz.isInstance(driver))
			{
				return;
			}
			if(reference == null)
			{
				return;
			}
			if(lock == null)
			{
				return;
			}
			lock.lock();
			try
			{
				List<ServiceContainer> list = listsByClassName.get(driver.getClass().getCanonicalName());
				if(list == null)
				{
					list = new ArrayList<>();
					listsByClassName.put(driver.getClass().getCanonicalName(),list);
				}
				ServiceContainer oldContainer = null;
				for(ServiceContainer container : list)
				{
					if(container.getServiceReference() == reference)
					{
						return;
					}
					if(oldContainer == null)
					{
						oldContainer = container;
					}
				}
				list.add(new ServiceContainer(reference, driver));
				
				Collections.sort(list, Collections.reverseOrder(new Comparator<ServiceContainer>()
				{

					@Override
					public int compare(ServiceContainer o1, ServiceContainer o2)
					{
						ServiceReference sr1 = o1.getServiceReference(); 
						ServiceReference sr2 = o2.getServiceReference();
						
						if((sr1 == null) || (sr2 == null))
						{
							return 0;
						}
						Bundle bundle1 = sr1.getBundle();
						Bundle bundle2 = sr2.getBundle();
						if((bundle1 == null) || (bundle2 == null))
						{
							return 0;
						}
						Version version1 = bundle1.getVersion();
						Version version2 = bundle2.getVersion();
						if((version1 == null) || (version2 == null))
						{
							return 0;
						}
						return version1.compareTo(version2);
					}
				}));
				
				if(oldContainer != list.get(0))
				{
					for(BiConsumer updateListener : this.updateListenerList)
					{
						try
						{
							updateListener.accept(list.get(0).getService(), oldContainer == null ? null : oldContainer.getService());
						}
						catch (Exception e) {}
					}
				}
				
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		public void removeDriver(ServiceReference reference, Object driver)
		{
			if(lock == null)
			{
				return;
			}
			lock.lock();
			try
			{
				Set<String> toRemoveLists = new HashSet<>();
				for(Entry<String,List<ServiceContainer>> entry  : listsByClassName.entrySet())
				{
					List<ServiceContainer> list = entry.getValue();
					LinkedList<Integer> toRemovePositions = new LinkedList<>();
					int index = 0;
					for(ServiceContainer serviceContainer : list)
					{
						if(serviceContainer.getServiceReference() == reference)
						{
							toRemovePositions.addFirst(index);
						}
						index++;
					}
					if(toRemovePositions.isEmpty())
					{
						continue;
					}
					ServiceContainer oldFirstContainer = list.get(0);
					for(Integer toRemove : toRemovePositions)
					{
						list.remove((int)toRemove);
					}
					ServiceContainer newFirstContainer = list.isEmpty() ? null : list.get(0);
					
					if(newFirstContainer != oldFirstContainer)
					{
						for(BiConsumer updateListener : this.updateListenerList)
						{
							try
							{
								updateListener.accept(newFirstContainer == null ? null : newFirstContainer.getService(), oldFirstContainer.getService());
							}
							catch (Exception e) {}
						}
					}
					
					if(list.isEmpty())
					{
						toRemoveLists.add(entry.getKey());
					}
				}
				for(String toRemove : toRemoveLists)
				{
					listsByClassName.remove(toRemove);
				}
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		protected BundleContext getContext()
		{
			return super.context;
		}
		
		private class ServiceContainer
		{
			private ServiceContainer(ServiceReference serviceReference, Object service)
			{
				super();
				this.serviceReference = serviceReference;
				this.service = service;
			}
			
			private ServiceReference serviceReference = null;
			private Object service = null;
			
			public ServiceReference getServiceReference()
			{
				return serviceReference;
			}
			public Object getService()
			{
				return service;
			}
		}

		public Class getClazz()
		{
			return clazz;
		}
	}
	
	protected static class Customizer implements ServiceTrackerCustomizer
	{
		DriverServiceTracker tracker = null;
		public Customizer()
		{
			super();
		}
		
		public void setTracker(DriverServiceTracker tracker)
		{
			this.tracker = tracker;
		}
		
		@Override
		public Object addingService(ServiceReference reference)
		{
			
			//Object driver = tracker.getContext().getService(reference);
			Object driver = FrameworkUtil.getBundle(tracker.getClazz()).getBundleContext().getService(reference);
			tracker.addDriver(reference,driver);
			return driver;
		}

		@Override
		public void modifiedService(ServiceReference reference, Object service){}

		@Override
		public void removedService(ServiceReference reference, Object service)
		{
			tracker.removeDriver(reference,service);
			FrameworkUtil.getBundle(tracker.getClazz()).getBundleContext().ungetService(reference);
		}
		
	}
}
