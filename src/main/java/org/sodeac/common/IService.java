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
package org.sodeac.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.sodeac.common.misc.DefaultServiceFactory;
import org.sodeac.common.misc.Version;
import org.sodeac.common.xuri.URI;
import org.sodeac.common.xuri.ldapfilter.FilterBuilder;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;

import jakarta.json.Json;

public interface IService
{
    URI URI_SERVICE_LOCATOR_SERVICE_REGISTRY =
        ServiceSelectorAddress.newBuilder()
                              .forDomain("sodeac.org")
                              .withServiceName("localserviceregistry")
                              .setFilter(
                                  FilterBuilder.andLinker()
                                               .criteriaWithName("version").gte(new Version(1).toString())
                                               .criteriaWithName("version").notGte(new Version(2).toString())
                                               .build()
                              )
                              .build();
    
    String REPLACED_BY_CLASS_NAME = "<REPLACED__BY__CLASS__NAME>";
    String REPLACED_BY_PACKAGE_NAME = "<REPLACED__BY__PACKAGE__NAME>";
    
    /**
     * Get service provider
     *
     * @param clazz   type of service
     * @param address address of registration encoded in URI
     *
     * @return service provider
     */
    <S> IServiceProvider<S> getServiceProvider(Class<S> clazz, URI address);
    
    IInjector getInjector();
    
    interface IInjector
    {
        void injectMembers(Object instance);
    }
    
    interface IServiceReference<S> extends Supplier<S>, AutoCloseable
    {
        IServiceReference<S> getServiceProvider();
    }
    
    interface IServiceProvider<S>
    {
        IServiceReference<S> getService(); // TODO getReference ??? get ServiceReference ????
        
        // TODO public Optional<IServiceReference<S>> getOptionalService();
        
        IServiceProvider<S> setAutoDisconnectTime(long ms);
        
        IServiceProvider<S> disconnect();
        
        Object getClient();
        
        boolean isMatched();
    }
    
    interface IOnServiceReferenceAttach<S>
    {
        void onServiceReferenceAttach(IServiceReference<S> serviceReference);
    }
    
    interface IOnServiceReferenceDetach<S>
    {
        void onServiceReferenceDetach(IServiceReference<S> serviceReference);
    }
    
    interface IServiceRegistry
    {
        /**
         *
         * register a service
         *
         * @param serviceName                  jmx name of Service
         * @param serviceImplementationClass   class of service instance
         * @param serviceFactoryPolicy         policy to manage service instance
         * @param serviceRegistrationAddresses addresses at which the service is registered
         *
         * @return registration object
         */
        IServiceRegistration registerService(String serviceName, Class<?> serviceImplementationClass, ServiceFactoryPolicy serviceFactoryPolicy, ServiceRegistrationAddress... serviceRegistrationAddresses);
        
        /**
         *
         * register a service
         *
         * @param serviceImplementationClass   class of service instance
         * @param serviceFactoryPolicy         policy to manage service instance
         * @param serviceRegistrationAddresses addresses at which the service is registered
         *
         * @return registration object
         */
        default IServiceRegistration registerService(final Class<?> serviceImplementationClass, final ServiceFactoryPolicy serviceFactoryPolicy, final ServiceRegistrationAddress... serviceRegistrationAddresses)
        {
            return registerService(null, serviceImplementationClass, serviceFactoryPolicy, serviceRegistrationAddresses);
        }
        
        /**
         *
         *
         * @author Sebastian Palarus
         *
         */
        interface IServiceRegistration
        {
            // TODO pause / resume / close
            //
            
            void close(); // no IServiceProvider.get() ... anymore / providers update to another Service
            
            void dispose(); // hard unregister
        }
    }
    
    interface IFactoryEnvironment<S, C>
    {
        Class<?> getReferenceClass();
        
        Class<S> getServiceClass();
        
        C getConfiguration();
        
        IServiceProvider<S> getInitialServiceProvider();
        
        boolean isRequireConfiguration();
    }
    
    class ServiceSelectorAddress
    {
        private ServiceSelectorAddress()
        {
            super();
            this.uriBuilder = new StringBuilder("sdc://serviceselector:");
        }
        
        private StringBuilder uriBuilder = null;
        
        public static ServiceSelectorAddress newBuilder()
        {
            return new ServiceSelectorAddress();
        }
        
        public SelectorAddressName forDomain(final String domain)
        {
            this.uriBuilder.append(domain + "/");
            return new SelectorAddressName();
        }
        
        public class SelectorAddressName
        {
            public SelectorAddressFilter withServiceName(final String serviceName)
            {
                ServiceSelectorAddress.this.uriBuilder.append(serviceName);
                return new SelectorAddressFilter();
            }
            
            public class SelectorAddressFilter
            {
                private boolean preferencesPathItem = false;
                
                public URI build()
                {
                    return new URI(ServiceSelectorAddress.this.uriBuilder.toString());
                }
                
                public PreferenceAddressFilter setFilter(final IFilterItem filter)
                {
                    if (filter != null)
                    {
                        ServiceSelectorAddress.this.uriBuilder.append(filter);
                    }
                    return new PreferenceAddressFilter();
                }
                
                public PreferenceAddressFilter.PreferenceAddressFilterScore scoreThePreferenceFilter(final IFilterItem filter)
                {
                    return new PreferenceAddressFilter().new PreferenceAddressFilterScore(filter);
                }
                
                public class PreferenceAddressFilter
                {
                    public URI build()
                    {
                        return SelectorAddressFilter.this.build();
                    }
                    
                    public PreferenceAddressFilterScore scoreThePreferenceFilter(final IFilterItem filter)
                    {
                        return new PreferenceAddressFilterScore(filter);
                    }
                    
                    public class PreferenceAddressFilterScore
                    {
                        private IFilterItem filter = null;
                        
                        private PreferenceAddressFilterScore(final IFilterItem filter)
                        {
                            super();
                            this.filter = filter;
                        }
                        
                        public PreferenceAddressFilterScoreSatisfied with(final int counts)
                        {
                            return new PreferenceAddressFilterScoreSatisfied(counts);
                        }
                        
                        public class PreferenceAddressFilterScoreSatisfied
                        {
                            private final int counts;
                            
                            private PreferenceAddressFilterScoreSatisfied(final int counts)
                            {
                                super();
                                this.counts = counts;
                            }
                            
                            public PreferenceAddressFilter points()
                            {
                                if (!SelectorAddressFilter.this.preferencesPathItem)
                                {
                                    SelectorAddressFilter.this.preferencesPathItem = true;
                                    ServiceSelectorAddress.this.uriBuilder.append("/preferences");
                                }
                                ServiceSelectorAddress.this.uriBuilder.append(Json.createObjectBuilder().add("score", this.counts).add("filter", PreferenceAddressFilterScore.this.filter.toString()).build().toString());
                                return PreferenceAddressFilter.this;
                            }
                        }
                    }
                }
            }
        }
    }
    
    class ServiceFactoryPolicy
    {
        private ServiceFactoryPolicy()
        {
            super();
        }
        
        public static ServiceFactoryPolicy.FactoryPolicyBuilder newBuilder()
        {
            return new FactoryPolicyBuilder();
        }
        
        private int lowerScalingLimit = 1;
        private int upperScalingLimit = 1;
        private int initialScaling = 0;
        private boolean shared = true;
        
        private Class<?> requiredConfigurationClass = null;
        private Function<IFactoryEnvironment<?, ?>, ?> factory = null;
        
        private Map<String, Object> options = null;
        
        public int getLowerScalingLimit()
        {
            return this.lowerScalingLimit;
        }
        
        public int getUpperScalingLimit()
        {
            return this.upperScalingLimit;
        }
        
        public int getInitialScaling()
        {
            return this.initialScaling;
        }
        
        public boolean isShared()
        {
            return this.shared;
        }
        
        public Class<?> getRequiredConfigurationClass()
        {
            return this.requiredConfigurationClass;
        }
        
        public Function<IFactoryEnvironment<?, ?>, ?> getFactory()
        {
            return this.factory;
        }
        
        public Map<String, Object> getOptions()
        {
            return this.options;
        }
        
        public static class FactoryPolicyBuilder
        {
            private FactoryPolicyBuilder()
            {
                super();
                this.options = new HashMap<String, Object>();
            }
            
            // Scaling
            
            private int lowerScalingLimit = 1;
            private int upperScalingLimit = 1;
            private int initialScaling = 0;
            
            // Share
            
            private boolean shared = true;
            
            // Factory
            
            private Class<?> requiredConfigurationClass = null;
            private Function<IFactoryEnvironment<?, ?>, ?> factory = null;
            
            // Properties
            private Map<String, Object> options = null;
            
            public BuilderScalingLowerLimit1 defineServiceScalingLimits()
            {
                return new BuilderScalingLowerLimit1();
            }
            
            public BuilderShared defineServiceAsLazyLoadingSingleton()
            {
                this.lowerScalingLimit = 1;
                this.upperScalingLimit = 1;
                this.initialScaling = 0;
                
                return new BuilderShared();
            }
            
            public BuilderShared defineServiceAsSingletonWithAutoCreation()
            {
                this.lowerScalingLimit = 1;
                this.upperScalingLimit = 1;
                this.initialScaling = 1;
                
                return new BuilderShared();
            }
            
            public class BuilderScalingLowerLimit1
            {
                public BuilderScalingLowerLimit2 forTheLowerLimitDefine(final int lowerLimit)
                {
                    FactoryPolicyBuilder.this.lowerScalingLimit = lowerLimit;
                    return new BuilderScalingLowerLimit2();
                }
                
                public BuilderScalingLowerLimit2.BuilderScalingUpperLimit1 forTheLowerLimitDefineOneInstance()
                {
                    FactoryPolicyBuilder.this.lowerScalingLimit = 1;
                    return new BuilderScalingLowerLimit2().new BuilderScalingUpperLimit1();
                }
                
                public class BuilderScalingLowerLimit2
                {
                    public BuilderScalingUpperLimit1 instances()
                    {
                        return new BuilderScalingUpperLimit1();
                    }
                    
                    public class BuilderScalingUpperLimit1
                    {
                        public BuilderScalingUpperLimit2 forTheUpperLimitDefine(final int upperLimit)
                        {
                            FactoryPolicyBuilder.this.upperScalingLimit = upperLimit;
                            return new BuilderScalingUpperLimit2();
                        }
                        
                        public BuilderScalingUpperLimit2.BuilderScalingInitialSize1 forTheUpperLimitDefineOneInstance()
                        {
                            FactoryPolicyBuilder.this.upperScalingLimit = 1;
                            return new BuilderScalingUpperLimit2().new BuilderScalingInitialSize1();
                        }
                        
                        public class BuilderScalingUpperLimit2
                        {
                            public BuilderScalingInitialSize1 instances()
                            {
                                return new BuilderScalingInitialSize1();
                            }
                            
                            public class BuilderScalingInitialSize1
                            {
                                public BuilderScalingInitialSize2 initializeServiceWith(final int initialScaling)
                                {
                                    FactoryPolicyBuilder.this.initialScaling = initialScaling;
                                    return new BuilderScalingInitialSize2();
                                }
                                
                                public BuilderShared initializeServiceWithOneInstance()
                                {
                                    FactoryPolicyBuilder.this.initialScaling = 1;
                                    return new BuilderShared();
                                }
                                
                                public class BuilderScalingInitialSize2
                                {
                                    public BuilderShared instances()
                                    {
                                        return new BuilderShared();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            public class BuilderShared
            {
                public BuilderFactory1 supplyServiceInstanceToMultipleServiceClients()
                {
                    FactoryPolicyBuilder.this.shared = true;
                    return new BuilderFactory1();
                }
                
                public BuilderFactory1 supplyServiceInstanceToOneSingleServiceClientOnly()
                {
                    FactoryPolicyBuilder.this.shared = false;
                    return new BuilderFactory1();
                }
            }
            
            public class BuilderFactory1
            {
                public BuilderFactory2 applyConstructorForNewServiceInstance()
                {
                    FactoryPolicyBuilder.this.factory = e -> new DefaultServiceFactory().apply(e);
                    return new BuilderFactory2();
                }
                
                public BuilderFactory1b applyFactory(final Function<IFactoryEnvironment<?, ?>, ?> factory)
                {
                    FactoryPolicyBuilder.this.factory = factory;
                    return new BuilderFactory1b();
                }
                
                public class BuilderFactory1b
                {
                    public BuilderFactory2 forNewServiceInstance()
                    {
                        return new BuilderFactory2();
                    }
                }
                
                public class BuilderFactory2
                {
                    public BuilderOptions newServiceInstanceRequiresConfiguration(final Class<?> configurationClass)
                    {
                        FactoryPolicyBuilder.this.requiredConfigurationClass = configurationClass;
                        return new BuilderOptions();
                    }
                    
                    public BuilderOptions addOption(final String name, final String value)
                    {
                        FactoryPolicyBuilder.this.options.put(name, value);
                        return new BuilderOptions();
                    }
                    
                    public BuilderOptions addOption(final String name, final boolean value)
                    {
                        return new BuilderOptions().addOption(name, value);
                    }
                    
                    public BuilderOptions addOption(final String name, final long value)
                    {
                        return new BuilderOptions().addOption(name, value);
                    }
                    
                    public BuilderOptions addOption(final String name, final double value)
                    {
                        return new BuilderOptions().addOption(name, value);
                    }
                    
                    public ServiceFactoryPolicy build()
                    {
                        return FactoryPolicyBuilder.this.build();
                    }
                }
            }
            
            public class BuilderOptions
            {
                public BuilderOptions addOption(final String name, final String value)
                {
                    FactoryPolicyBuilder.this.options.put(name, value);
                    return this;
                }
                
                public BuilderOptions addOption(final String name, final boolean value)
                {
                    FactoryPolicyBuilder.this.options.put(name, value);
                    return this;
                }
                
                public BuilderOptions addOption(final String name, final long value)
                {
                    FactoryPolicyBuilder.this.options.put(name, value);
                    return this;
                }
                
                public BuilderOptions addOption(final String name, final double value)
                {
                    FactoryPolicyBuilder.this.options.put(name, value);
                    return this;
                }
                
                public ServiceFactoryPolicy build()
                {
                    return FactoryPolicyBuilder.this.build();
                }
            }
            
            private ServiceFactoryPolicy build()
            {
                final ServiceFactoryPolicy serviceFactoryPolicy = new ServiceFactoryPolicy();
                serviceFactoryPolicy.lowerScalingLimit = this.lowerScalingLimit;
                serviceFactoryPolicy.upperScalingLimit = this.upperScalingLimit;
                serviceFactoryPolicy.initialScaling = this.initialScaling;
                serviceFactoryPolicy.shared = this.shared;
                
                serviceFactoryPolicy.requiredConfigurationClass = this.requiredConfigurationClass;
                serviceFactoryPolicy.factory = this.factory;
                
                serviceFactoryPolicy.options = Collections.unmodifiableMap(new HashMap<String, Object>(this.options));
                return serviceFactoryPolicy;
            }
        }
    }
    
    class ServiceRegistrationAddress
    {
        private ServiceRegistrationAddress()
        {
            super();
        }
        
        public static ServiceRegistrationAddressBuilder newBuilder()
        {
            return new ServiceRegistrationAddressBuilder();
        }
        
        private String domain = null;
        private String name = null;
        private Version version = null;
        private Set<Class<?>> types = null;
        
        public String getDomain()
        {
            return this.domain;
        }
        
        public String getName()
        {
            return this.name;
        }
        
        public Version getVersion()
        {
            return this.version;
        }
        
        public Set<Class<?>> getTypes()
        {
            return this.types;
        }
        
        public static class ServiceRegistrationAddressBuilder
        {
            private ServiceRegistrationAddressBuilder()
            {
                super();
                this.types = new ArrayList<Class<?>>();
            }
            
            private String domain = null;
            private String name = null;
            private Version version = null;
            private List<Class<?>> types = null;
            
            public RegistrationAddressName forDomain(final String domain)
            {
                ServiceRegistrationAddressBuilder.this.domain = domain;
                return new RegistrationAddressName();
            }
            
            public class RegistrationAddressName
            {
                public RegistrationAddressVersion withServiceName(final String serviceName)
                {
                    ServiceRegistrationAddressBuilder.this.name = serviceName;
                    return new RegistrationAddressVersion();
                }
                
                public class RegistrationAddressVersion
                {
                    
                    public RegistrationAddressTypes andVersion(final int major, final int minor, final int service)
                    {
                        ServiceRegistrationAddressBuilder.this.version = new Version(major, major, service);
                        return new RegistrationAddressTypes();
                    }
                    
                    public class RegistrationAddressTypes
                    {
                        public RegistrationAddressTypes addType(final Class<?> type)
                        {
                            ServiceRegistrationAddressBuilder.this.types.add(type);
                            return this;
                        }
                        
                        public ServiceRegistrationAddress build()
                        {
                            final ServiceRegistrationAddress address = new ServiceRegistrationAddress();
                            address.domain = ServiceRegistrationAddressBuilder.this.domain;
                            address.name = ServiceRegistrationAddressBuilder.this.name;
                            address.version = ServiceRegistrationAddressBuilder.this.version;
                            address.types = Collections.unmodifiableSet(new HashSet<Class<?>>(ServiceRegistrationAddressBuilder.this.types));
                            return address;
                        }
                    }
					
					/*public class RegistrationAddressOptions
					{
						private JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
						
						public RegistrationAddressOptions addOption(String name, String value)
						{
							this.jsonBuilder.add(name, value);
							return this;
						}
						
						public RegistrationAddressOptions addOption(String name, boolean value)
						{
							this.jsonBuilder.add(name, value);
							return this;
						}
						
						public RegistrationAddressOptions addOption(String name, long value)
						{
							this.jsonBuilder.add(name, value);
							return this;
						}
						
						public RegistrationAddressOptions addOption(String name, double value)
						{
							this.jsonBuilder.add(name, value);
							return this;
						}
						
						public URI build()
						{
							return new URI(uriBuilder.toString() + jsonBuilder.build());
						}
					}*/
                }
            }
        }
    }
    
}
