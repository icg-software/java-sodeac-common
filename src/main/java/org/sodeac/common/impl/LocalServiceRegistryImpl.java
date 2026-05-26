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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.sodeac.common.IService.IFactoryEnvironment;
import org.sodeac.common.IService.IServiceProvider;
import org.sodeac.common.IService.IServiceRegistry;
import org.sodeac.common.IService.ServiceFactoryPolicy;
import org.sodeac.common.IService.ServiceRegistrationAddress;
import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.impl.JMXBeans.ServiceRegistrationMBean;
import org.sodeac.common.misc.RuntimeWrappedException;
import org.sodeac.common.misc.Version;
import org.sodeac.common.xuri.IExtension;
import org.sodeac.common.xuri.PathSegment;
import org.sodeac.common.xuri.URI;
import org.sodeac.common.xuri.URISyntaxException;
import org.sodeac.common.xuri.json.JsonExtension;
import org.sodeac.common.xuri.ldapfilter.DefaultMatchableWrapper;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;
import org.sodeac.common.xuri.ldapfilter.IMatchable;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterDecodingHandler;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterExtension;

import jakarta.json.JsonObject;

public class LocalServiceRegistryImpl implements IServiceRegistry
{
    private static LocalServiceRegistryImpl INSTANCE = null;
    private final Lock lock;
    
    private List<RegisteredService> registeredServices = null;
    private final String scope = "local";
    private final UUID instance = null;
    
    // TODO unloadedBundles => String,String,String,Set<Long> , domain,name,typeName,BundleList
    
    // TODO activateBundle(long id), deactivateBundle(long id)
    // OSGi :> startBundle, if in unloadedBundles List =>
    // NonOSGi => search
    
    private Map<String, Map<String, Map<Class, ServiceController>>> addressIndex = null;
    
    private LocalServiceRegistryImpl()
    {
        super();
        this.lock = new ReentrantLock();
        this.addressIndex = new HashMap<>();
        this.registeredServices = new ArrayList<>();
    }
    
    protected static LocalServiceRegistryImpl get()
    {
        LocalServiceRegistryImpl instance = INSTANCE;
        if (instance != null)
        {
            return instance;
        }
        
        synchronized (LocalServiceRegistryImpl.class)
        {
            instance = INSTANCE;
            if (instance == null)
            {
                instance = new LocalServiceRegistryImpl();
                final ConplierBean<IServiceRegistry> registrySupplier = new ConplierBean<>(instance);
                instance.registerService
                            (
                                "org.sodeac.ServiceRegistry",
                                LocalServiceRegistryImpl.class,
                                ServiceFactoryPolicy.newBuilder()
                                                    .defineServiceAsSingletonWithAutoCreation()
                                                    .supplyServiceInstanceToMultipleServiceClients()
                                                    .applyFactory(e -> registrySupplier.get()).forNewServiceInstance()
                                                    .addOption("systemservice", true)
                                                    .build(),
                                ServiceRegistrationAddress.newBuilder()
                                                          .forDomain("sodeac.org")
                                                          .withServiceName("localserviceregistry")
                                                          .andVersion(1, 0, 0)
                                                          .addType(IServiceRegistry.class)
                                                          .build()
                            );
                INSTANCE = instance;
            }
            return instance;
        }
    }
    
    @Override
    public IServiceRegistration registerService(String serviceName, final Class<?> serviceImplementationClass, final ServiceFactoryPolicy serviceFactoryPolicy, final ServiceRegistrationAddress... serviceRegistrationAddresses)
    {
        Objects.requireNonNull(serviceImplementationClass, "service implementation class not defined");
        Objects.requireNonNull(serviceFactoryPolicy, "service factory policy not defined");
        
        if ((serviceName == null) || serviceName.isEmpty())
        {
            serviceName = serviceImplementationClass.getCanonicalName();
        }
        
        final RegisteredService registeredService = new RegisteredService(serviceImplementationClass, serviceFactoryPolicy, serviceRegistrationAddresses);
        
        this.lock.lock();
        try
        {
            this.registeredServices.add(registeredService);
            
            try
            {
                final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                final String objectName = getObjectNamePrefix() + ",grouping=by-registration,service=" + serviceName + "-" + registeredService.getIdString();
                
                final ObjectName controllerObjectName = new ObjectName(objectName);
                
                try
                {
                    mBeanServer.registerMBean(new StandardMBean(registeredService, ServiceRegistrationMBean.class), controllerObjectName);
                }
                catch (final Exception e)
                {
                    // TODO
                    e.printStackTrace();
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
            
            for (final RegisteredService.Address address : registeredService.serviceRegistrationAddresseList)
            {
                
                Map<String, Map<Class, ServiceController>> byDomain = this.addressIndex.get(address.serviceRegistrationAddress.getDomain());
                if (byDomain == null)
                {
                    byDomain = new HashMap<>();
                    this.addressIndex.put(address.serviceRegistrationAddress.getDomain(), byDomain);
                }
                Map<Class, ServiceController> byServiceName = byDomain.get(address.serviceRegistrationAddress.getName());
                if (byServiceName == null)
                {
                    byServiceName = new HashMap<>();
                    byDomain.put(address.serviceRegistrationAddress.getName(), byServiceName);
                }
                if (address.serviceRegistrationAddress.getTypes().isEmpty())
                {
                    ServiceController serviceController = byServiceName.get(serviceImplementationClass);
                    if (serviceController == null)
                    {
                        serviceController = new ServiceController(serviceImplementationClass);
                        byServiceName.put(serviceImplementationClass, serviceController);
                    }
                    serviceController.addRegisteredService(address);
                    
                    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                    final String objectName = getObjectNamePrefix() + ",grouping=by-registration,service=" + serviceName + "-" + registeredService.getIdString()
                                              + ",address=" + address.serviceRegistrationAddress.getDomain() + "-" + address.serviceRegistrationAddress.getName() + "-" + serviceImplementationClass.getCanonicalName() + "-0";
                    
                    try
                    {
                        final ObjectName controllerObjectName = new ObjectName(objectName);
                        mBeanServer.registerMBean(new StandardMBean(registeredService, ServiceRegistrationMBean.class), controllerObjectName);
                        // TODO in Address
                    }
                    catch (final Exception e)
                    {
                        // TODO
                        e.printStackTrace();
                    }
                }
                else
                {
                    for (final Class type : address.serviceRegistrationAddress.getTypes())
                    {
                        ServiceController serviceController = byServiceName.get(type);
                        if (serviceController == null)
                        {
                            serviceController = new ServiceController(type);
                            byServiceName.put(type, serviceController);
                        }
                        serviceController.addRegisteredService(address);
                        
                        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                        final String objectName = getObjectNamePrefix() + ",grouping=by-registration,service=" + serviceName + "-" + registeredService.getIdString()
                                                  + ",address=" + address.serviceRegistrationAddress.getDomain() + "-" + address.serviceRegistrationAddress.getName()
                                                  + "-" + type.getCanonicalName() + "-" + UUID.randomUUID();
                        
                        try
                        {
                            final ObjectName controllerObjectName = new ObjectName(objectName);
                            mBeanServer.registerMBean(new StandardMBean(registeredService, ServiceRegistrationMBean.class), controllerObjectName);
                            // TODO in Address
                        }
                        catch (final Exception e)
                        {
                            // TODO
                            e.printStackTrace();
                        }
                        
                        // TODO
                    }
                }
            }
            
        }
        finally
        {
            this.lock.unlock();
        }
        
        return registeredService.serviceRegistration;
    }
	
	/*@Override
	public <S> IServiceRegistration registerService(Class<S> type, URI address,Function<IFactoryEnvironment<S>,S> serviceFactory)
	{
		Objects.requireNonNull(type, "Missing type");
		Objects.requireNonNull(address, "Missing address");
		Objects.requireNonNull(serviceFactory, "Missing service reference");
		
		RegisteredService service = new RegisteredService<>(type, address, serviceFactory);
		ServiceController serviceController = null;
		lock.lock();
		try
		{
			Map<String,Map<Class,ServiceController>> byDomain = this.registeredServices.get(service.domain);
			if(byDomain == null)
			{
				byDomain = new HashMap<>();
				this.registeredServices.put(service.domain, byDomain);
			}
			Map<Class,ServiceController> byServiceName = byDomain.get(service.serviceName);
			if(byServiceName == null)
			{
				byServiceName = new HashMap<>();
				byDomain.put(service.serviceName,byServiceName);
			}
			serviceController = byServiceName.get(service.type);
			if(serviceController == null)
			{
				serviceController = new ServiceController(type);
				byServiceName.put(service.type, serviceController);
			}
		}
		finally 
		{
			lock.unlock();
		}
		
		service.serviceController = serviceController;
		service.serviceRegistration = service.new ServiceRegistration();
		serviceController.addService(service);
		
		return service.serviceRegistration;
	}*/
    
    protected <S> IServiceProvider<S> getServiceProvider(final Class<S> type, final URI serviceAddress)
    {
        if (!serviceAddress.getScheme().getValue().equalsIgnoreCase("sdc"))
        {
            throw new URISyntaxException(serviceAddress.toString(), "expected schema is 'sdc', and not " + serviceAddress.getScheme().getValue());
        }
        
        if ((serviceAddress.getAuthority().getSubComponentList().size() > 2) || (serviceAddress.getAuthority().getSubComponentList().size() < 1))
        {
            throw new URISyntaxException(serviceAddress.toString(), "wrong authority size");
        }
        
        String domain = null;
        
        if (serviceAddress.getAuthority().getSubComponentList().size() == 2)
        {
            if (!serviceAddress.getAuthority().getSubComponentList().get(0).getValue().equalsIgnoreCase("serviceselector"))
            {
                throw new URISyntaxException(serviceAddress.toString(), "expected authority type is 'serviceselector', and not '" + serviceAddress.getAuthority().getSubComponentList().get(0).getValue() + "'");
            }
            domain = serviceAddress.getAuthority().getSubComponentList().get(1).getValue();
        }
        else
        {
            domain = serviceAddress.getAuthority().getSubComponentList().get(0).getValue();
        }
        
        if ((domain == null) || domain.isEmpty())
        {
            throw new URISyntaxException(serviceAddress.toString(), "domain of service not defined");
        }
        
        if (serviceAddress.getPath().getSubComponentList().size() == 0)
        {
            throw new URISyntaxException(serviceAddress.toString(), "wrong path size");
        }
        
        final String serviceName = serviceAddress.getPath().getSubComponentList().get(0).getValue();
        
        if (serviceName.isEmpty())
        {
            throw new URISyntaxException(serviceAddress.toString(), "name of service not defined");
        }
        
        // Collect HardConstraint
        
        final List<IFilterItem> filterList = new ArrayList<>();
        for (final IExtension extension : serviceAddress.getPath().getSubComponentList().get(0).getExtensionList(LDAPFilterExtension.TYPE))
        {
            final IFilterItem filterItem = (IFilterItem) extension.getDecoder().decodeFromString(extension.getExpression());
            filterList.add(filterItem);
        }
        
        // Collect SoftConstraint
        
        final Map<Long, List<IFilterItem>> preferencesList = new HashMap<Long, List<IFilterItem>>();
        
        for (int i = 1; i < serviceAddress.getPath().getSubComponentList().size(); i++)
        {
            final PathSegment pathSegment = serviceAddress.getPath().getSubComponentList().get(i);
            if (!"preferences".equalsIgnoreCase(pathSegment.getValue()))
            {
                continue;
            }
            
            for (final IExtension extension : pathSegment.getExtensionList(JsonExtension.TYPE))
            {
                final JsonObject jsonObject = (JsonObject) extension.getDecoder().decodeFromString(extension.getExpression());
                
                final long score = jsonObject.getJsonNumber("score").longValue();
                final IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(jsonObject.getString("filter"));
                
                List<IFilterItem> preferences = preferencesList.get(score);
                if (preferences == null)
                {
                    preferences = new ArrayList<>();
                    preferencesList.put(score, preferences);
                }
                
                preferences.add(filterItem);
            }
        }
        
        ServiceController serviceController = null;
        this.lock.lock();
        try
        {
            Map<String, Map<Class, ServiceController>> byDomain = this.addressIndex.get(domain);
            if (byDomain == null)
            {
                byDomain = new HashMap<>();
                this.addressIndex.put(domain, byDomain);
            }
            Map<Class, ServiceController> byServiceName = byDomain.get(serviceName);
            if (byServiceName == null)
            {
                byServiceName = new HashMap<>();
                byDomain.put(serviceName, byServiceName);
            }
            serviceController = byServiceName.get(type);
            if (serviceController == null)
            {
                serviceController = new ServiceController(type);
                byServiceName.put(type, serviceController);
            }
        }
        finally
        {
            this.lock.unlock();
        }
        
        // RegisteredService registeredService = serviceController.getRegisteredService(filterList, preferencesList);
        
        final LocalServiceProviderImpl<S> serviceProvider = new LocalServiceProviderImpl<>(serviceController, filterList, preferencesList);
        serviceController.localServiceProvider.add(serviceProvider);
        
        return serviceProvider;
    }
    
    public String getObjectNamePrefix()
    {
        if (this.instance == null)
        {
            return "org.sodeac:sodeacproject=service-registry,scope=" + this.scope;
        }
        return "org.sodeac:sodeacproject=service-registry,scope=" + this.scope + ",instance=" + this.instance;
    }
    
    protected class ServiceController
    {
        public ServiceController(final Class type)
        {
            super();
            this.type = type;
            this.lock = new ReentrantLock();
        }
        
        private Lock lock = null;
        private Class type = null;
        private final List<RegisteredService.Address> serviceList = new ArrayList<>();
        private final List<LocalServiceProviderImpl> localServiceProvider = new ArrayList<>();
        
        protected void addRegisteredService(final RegisteredService.Address serviceRegistrationAddress)
        {
            this.lock.lock();
            try
            {
                this.serviceList.add(serviceRegistrationAddress);
                // TODO re-check bindings
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        protected void removeRegisteredService(final RegisteredService.Address serviceRegistrationAddress)
        {
            this.lock.lock();
            try
            {
                final ListIterator<RegisteredService.Address> iteratror = this.serviceList.listIterator();
                while (iteratror.hasNext())
                {
                    if (iteratror.next() == serviceRegistrationAddress)
                    {
                        iteratror.remove();
                    }
                }
                // TODO re-check bindings
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        protected RegisteredService getRegisteredService(final List<IFilterItem> filterList, final Map<Long, List<IFilterItem>> preferencesList)
        {
            List<RegisteredService.Address> copy = null;
            this.lock.lock();
            try
            {
                
                if (this.serviceList.isEmpty())
                {
                    return null;
                }
                
                copy = new ArrayList<>(this.serviceList);
            }
            finally
            {
                this.lock.unlock();
            }
            
            long bestScore = Long.MIN_VALUE;
            RegisteredService.Address bestService = null;
            
            for (final RegisteredService.Address registeredService : copy)
            {
                if (filterList != null)
                {
                    for (final IFilterItem filterItem : filterList)
                    {
                        if (filterItem == null)
                        {
                            continue;
                        }
                        
                        if (!filterItem.matches(registeredService.options))
                        {
                            break;
                        }
                    }
                }
                
                long currentScore = 0;
                
                if (preferencesList != null)
                {
                    for (final Entry<Long, List<IFilterItem>> preferencesEntry : preferencesList.entrySet())
                    {
                        if (preferencesEntry.getKey() == null)
                        {
                            continue;
                        }
                        if (preferencesEntry.getValue() == null)
                        {
                            continue;
                        }
                        if (preferencesEntry.getValue().isEmpty())
                        {
                            continue;
                        }
                        
                        for (final IFilterItem filter : preferencesEntry.getValue())
                        {
                            if (filter.matches(registeredService.options))
                            {
                                currentScore += preferencesEntry.getKey();
                            }
                        }
                    }
                }
                
                if (currentScore > bestScore)
                {
                    bestService = registeredService;
                    bestScore = currentScore;
                }
            }
            
            copy.clear();
            copy = null;
            
            return bestService.getRegisteredService();
            
        }
    }
    
    protected class RegisteredService implements ServiceRegistrationMBean
    {
        private RegisteredService(final Class<?> serviceImplementationClass, final ServiceFactoryPolicy serviceFactoryPolicy, final ServiceRegistrationAddress... serviceRegistrationAddresses)
        {
            super();
            this.id = UUID.randomUUID();
            this.serviceImplementationClass = serviceImplementationClass;
            this.serviceName = this.serviceImplementationClass.getCanonicalName();
            this.serviceFactoryPolicy = serviceFactoryPolicy;
            this.serviceRegistrationAddresseList = new ArrayList<>();
            this.options = new HashMap<>();
            for (final Entry<String, Object> entry : serviceFactoryPolicy.getOptions().entrySet())
            {
                this.options.put(entry.getKey(), new DefaultMatchableWrapper(entry.getValue()));
            }
            
            if (serviceRegistrationAddresses != null)
            {
                for (final ServiceRegistrationAddress serviceRegistrationAddress : serviceRegistrationAddresses)
                {
                    if (serviceRegistrationAddress == null)
                    {
                        continue;
                    }
                    
                    this.serviceRegistrationAddresseList.add(new Address(serviceRegistrationAddress));
                }
            }
            
        }
        
        private UUID id = null;
        private final long bundleId = -1;
        private final String symbolicName = "non.osgi";
        private final Version version = new Version(1);
        private Class<?> serviceImplementationClass = null;
        private String serviceName = null;
        private ServiceFactoryPolicy serviceFactoryPolicy = null;
        private List<Address> serviceRegistrationAddresseList = null;
        private final ServiceRegistration serviceRegistration = null;
        private Map<String, IMatchable> options = null;
        
        protected Object supply()
        {
            return this.serviceFactoryPolicy.getFactory().apply(null); // TODO  IFactoryEnvironment
        }
        
        protected class Address
        {
            protected Address(final ServiceRegistrationAddress serviceRegistrationAddress)
            {
                super();
                this.serviceRegistrationAddress = serviceRegistrationAddress;
                this.options = new HashMap<>(RegisteredService.this.options);
                this.options.put("version", new DefaultMatchableWrapper(this.serviceRegistrationAddress.getVersion()));
            }
            
            public RegisteredService getRegisteredService()
            {
                return RegisteredService.this;
            }
            
            private final ServiceRegistrationAddress serviceRegistrationAddress;
            private Map<String, IMatchable> options = null;
        }
        
        protected class ServiceRegistration implements IServiceRegistration
        {
            
            @Override
            public void close()
            {
                // TODO Auto-generated method stub
            }
            
            @Override
            public void dispose()
            {
                // TODO Auto-generated method stub
            }
        }
        
        @Override
        public String getIdString()
        {
            return this.id.toString();
        }
    }
	
	/*protected class RegisteredServiceX<S>
	{
		protected RegisteredServiceX(Class<S> type, URI address, Function<IFactoryEnvironment<S,?>,S> serviceFactory)
		{
			super();
			this.registrationAddress = address;
			this.type = type;
			this.serviceFactory = serviceFactory;
			this.parse();
		}
		
		private void parse()
		{
			if(! registrationAddress.getScheme().getValue().equalsIgnoreCase("sdc"))
			{
				throw new URISyntaxException(registrationAddress.toString(), "expected schema is 'sdc', and not " + registrationAddress.getScheme().getValue());
			}
			
			if((registrationAddress.getAuthority().getSubComponentList().size() > 2) || (registrationAddress.getAuthority().getSubComponentList().size() < 1))
			{
				throw new URISyntaxException(registrationAddress.toString(), "wrong authority size");
			}
			
			if(registrationAddress.getAuthority().getSubComponentList().size() == 2)
			{
				if(! registrationAddress.getAuthority().getSubComponentList().get(0).getValue().equalsIgnoreCase("serviceaddress"))
				{
					throw new URISyntaxException(registrationAddress.toString(), "expected authority type is 'serviceaddress', and not '" + registrationAddress.getAuthority().getSubComponentList().get(0).getValue() + "'");
				}
				domain = registrationAddress.getAuthority().getSubComponentList().get(1).getValue();
			}
			else
			{
				domain = registrationAddress.getAuthority().getSubComponentList().get(0).getValue();
			}
			
			if((domain == null) || domain.isEmpty())
			{
				throw new URISyntaxException(registrationAddress.toString(), "domain of service not defined");
			}
			
			if(registrationAddress.getPath().getSubComponentList().size() != 3 )
			{
				throw new URISyntaxException(registrationAddress.toString(), "wrong path size");
			}
			
			serviceName = registrationAddress.getPath().getSubComponentList().get(0).getValue();
					
			if(serviceName.isEmpty())
			{
				throw new URISyntaxException(registrationAddress.toString(), "name of service not defined");
			}
			
			options = new HashMap<String, IMatchable>();
			
			version = Version.fromString(registrationAddress.getPath().getSubComponentList().get(1).getValue());
			
			options.put("version", new DefaultMatchableWrapper(version));
			
			if(! registrationAddress.getPath().getSubComponentList().get(2).getValue().equalsIgnoreCase("options"))
			{
				throw new URISyntaxException(registrationAddress.toString(), "service options not defined");
			}
			
			IExtension<JsonObject> jsonExtension = (IExtension<JsonObject>)registrationAddress.getPath().getSubComponentList().get(2).getExtension(JsonExtension.TYPE);
			if(jsonExtension != null)
			{
				jsonObject = jsonExtension.getDecoder().decodeFromString(jsonExtension.getExpression());
				for(Entry<String,JsonValue> jsonItem : jsonObject.entrySet())
				{
					if(jsonItem.getValue().getValueType() == ValueType.NULL)
					{
						options.put(jsonItem.getKey(), new DefaultMatchableWrapper(null));
					}
					else if(jsonItem.getValue().getValueType() == ValueType.TRUE)
					{
						options.put(jsonItem.getKey(), new DefaultMatchableWrapper(true));
					}
					else if(jsonItem.getValue().getValueType() == ValueType.FALSE)
					{
						options.put(jsonItem.getKey(), new DefaultMatchableWrapper(false));
					}
					else if(jsonItem.getValue().getValueType() == ValueType.NUMBER)
					{
						if(((JsonNumber)jsonItem.getValue()).isIntegral())
						{
							options.put(jsonItem.getKey(), new DefaultMatchableWrapper(((JsonNumber)jsonItem.getValue()).longValue()));
						}
						else
						{
							options.put(jsonItem.getKey(), new DefaultMatchableWrapper(((JsonNumber)jsonItem.getValue()).doubleValue()));
						}
					}
					else if(jsonItem.getValue().getValueType() == ValueType.STRING)
					{
						options.put(jsonItem.getKey(), new DefaultMatchableWrapper(((JsonString)jsonItem.getValue()).getString()));
					}
				}
			}
			
		}
		
		private URI registrationAddress = null;
		private Function<IFactoryEnvironment<S,?>,S> serviceFactory = null;
		private Class<S> type = null;
		
		private String domain = null;
		private String serviceName = null;
		private Version version = null;
		private Map<String,IMatchable> options = null;
		private JsonObject jsonObject = null;
		private ServiceController serviceController = null;
		
		private ServiceRegistration serviceRegistration = null;
		
		protected <S> S supply()
		{
			return (S)this.serviceFactory.apply(null); // TODO  IFactoryEnvironment
		}
		
		protected class ServiceRegistration implements IServiceRegistration
		{

			@Override
			public void close()
			{
				// TODO Auto-generated method stub
			}

			@Override
			public void dispose()
			{
				// TODO Auto-generated method stub		
			}
		}
		
	}*/
    
    public static class DefaultFactory implements Function<IFactoryEnvironment<?, ?>, Object>
    {
        
        @Override
        public Object apply(final IFactoryEnvironment<?, ?> t)
        {
            try
            {
                final Class<?> serviceClass = t.getServiceClass();
                
                constr:
                for (final Constructor constructor : serviceClass.getDeclaredConstructors())
                {
                    if (!constructor.isAccessible())
                    {
                        continue;
                    }
                    
                    int configurationParameter = -1;
                    boolean unknownParameter = false;
                    final int index = 0;
                    param:
                    for (final Type type : constructor.getGenericParameterTypes())
                    {
                        if (t.getConfiguration() != null)
                        {
                            if (((Class) type).isInstance(t.getConfiguration()))
                            {
                                configurationParameter = index;
                                continue param;
                            }
                        }
                        
                        unknownParameter = true;
                    }
                    
                    if (unknownParameter)
                    {
                        continue constr;
                    }
                    
                    if (configurationParameter > -1)
                    {
                        return constructor.newInstance(t.getConfiguration());
                    }
                }
                
                if (t.isRequireConfiguration())
                {
                    return null;
                }
                
                return t.getServiceClass().newInstance();
            }
            catch (final RuntimeException e)
            {
                throw e;
            }
            catch (final Exception e)
            {
                throw new RuntimeWrappedException(e);
            }
            catch (final Error e)
            {
                throw new RuntimeWrappedException(e);
            }
        }
    }
}
