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
package org.sodeac.common.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component(immediate=true,service=LocalOSGiServiceRegistry.class)
public class LocalOSGiServiceRegistry
{
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile InternalDriverLoaded internalBootstrapDep;
	
	private ComponentContext componentContext;
	protected static LocalOSGiServiceRegistry INSTANCE;
	private Lock lock;
	private Map<Class,SrvServiceTracker> trackerIndex = new HashMap<Class, LocalOSGiServiceRegistry.SrvServiceTracker>();
	
	public LocalOSGiServiceRegistry()
	{
		super();
		this.lock = new ReentrantLock();
	}
	
	@Activate
	public void activate(ComponentContext componentContext)
	{
		this.componentContext = componentContext;
		LocalOSGiServiceRegistry.INSTANCE = this;
		
		LocalService.getLocalServiceRegistryImpl();
		
		// TODO Find Bundles
		
		/*componentContext.getBundleContext().addBundleListener(new SynchronousBundleListener()
		{
			
			@Override
			public void bundleChanged(BundleEvent event)
			{
				//System.out.println(event.getType() + " / " + event.getSource() + " / " + event.getBundle() + " / " + event.getOrigin());
				
				Bundle bundle = event.getBundle();
				Enumeration<URL> entries = bundle.findEntries("SDC-INF/servicecomponents/", "*.xml", false); // TODO possible NPE in uninstall events
	        	if(entries == null)
	        	{
	        		return;
	        	}
	        	while(entries.hasMoreElements())
	        	{
	        		System.out.println("\t" + entries.nextElement());
	        	}
				
			}
		});*/
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext)
	{
		List<SrvServiceTracker> values = null;
		lock.lock();
		try
		{
			values = new ArrayList<LocalOSGiServiceRegistry.SrvServiceTracker>(trackerIndex.values());
			trackerIndex.clear();
			trackerIndex = null;
		}
		finally 
		{
			lock.unlock();
		}
		
		for(SrvServiceTracker srvServiceTracker : values)
		{
			try
			{
				srvServiceTracker.close();
			}
			catch (Exception e) {}
		}
		this.componentContext = null;
		LocalOSGiServiceRegistry.INSTANCE = null;
	}
	
	public <T> void observe(Class<T> type)
	{
		lock.lock();
		try
		{
			if(trackerIndex.containsKey(type))
			{
				return;
			}
			
			SrvServiceTracker srvServiceTracker = new SrvServiceTracker(this.componentContext.getBundleContext(), type, new Customizer());
			this.trackerIndex.put(type, srvServiceTracker);
			srvServiceTracker.open(true);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	
	protected class SrvServiceTracker extends ServiceTracker
	{
		private Class clazz = null;
		private Lock lock = null;
		
		private Map<String,List<ServiceContainer>> listsByClassName = null;
		
		public SrvServiceTracker(BundleContext context, Class clazz, Customizer customizer)
		{
			super(context, clazz,customizer);
			this.clazz = clazz;
			this.lock = new ReentrantLock();
			customizer.setTracker(this);
			this.listsByClassName = new HashMap<String,List<ServiceContainer>>();
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
			}
			finally 
			{
				lock.unlock();
			}
			lock = null;
			clazz = null;
			listsByClassName = null;
		}
		
		
		
		public void addService(ServiceReference reference, Object service)
		{
			if(service == null)
			{
				return;
			}
			if(reference == null)
			{
				return;
			}
			if(!this.clazz.isInstance(service))
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
				List<ServiceContainer> list = listsByClassName.get(service.getClass().getCanonicalName());
				if(list == null)
				{
					list = new ArrayList<>();
					listsByClassName.put(service.getClass().getCanonicalName(),list);
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
				list.add(new ServiceContainer(reference, service));
				
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
				
				
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		public void removeService(ServiceReference reference, Object service)
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
					
					ListIterator<ServiceContainer> itr = list.listIterator();
					while(itr.hasNext())
					{
						ServiceContainer serviceContainer = itr.next();
						if(serviceContainer.getServiceReference() == reference)
						{
							itr.remove();
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
	}
	
	protected static class Customizer implements ServiceTrackerCustomizer
	{
		SrvServiceTracker tracker = null;
		public Customizer()
		{
			super();
		}
		
		public void setTracker(SrvServiceTracker tracker)
		{
			this.tracker = tracker;
		}
		
		@Override
		public Object addingService(ServiceReference reference)
		{
			Object service = tracker.getContext().getService(reference);
			tracker.addService(reference,service);
			return service;
		}

		@Override
		public void modifiedService(ServiceReference reference, Object service){}

		@Override
		public void removedService(ServiceReference reference, Object service)
		{
			tracker.removeService(reference,service);
		}
		
	}
}
