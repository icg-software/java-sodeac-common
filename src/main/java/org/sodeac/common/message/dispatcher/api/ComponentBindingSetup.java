/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.api;

import java.io.Serializable;

import org.sodeac.common.xuri.ldapfilter.IFilterItem;

public abstract class ComponentBindingSetup implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = -6854196332026025401L;
    
    /**
     * specified scopes allowed to apply this configuration type
     *
     * @return array of allowed scope
     */
    public abstract Class<? extends IDispatcherChannelComponent>[] getScopes();
    
    public abstract ComponentBindingSetup copy();
    
    private ComponentBindingSetup()
    {
        super();
    }
    
    private String dispatcherId = null;
    private String name = null;
    private Long channelCapacity = null;
    private PrivateChannelWorkerRequirement privateChannelWorkerRequirement = PrivateChannelWorkerRequirement.NoPreferenceOrRequirement;
    
    /**
     * getter for dispatcherId
     *
     * @return id of dispatcher (NULL for all dispatchers ) managed the channel
     */
    protected String getDispatcherId()
    {
        return this.dispatcherId;
    }
    
    /**
     * setter for dispatcher id
     *
     * @param dispatcherId id of dispatcher (NULL for all dispatchers ) managed the channel
     *
     * @return channel component configuration
     */
    protected ComponentBindingSetup setDispatcherId(final String dispatcherId)
    {
        this.dispatcherId = dispatcherId;
        return this;
    }
    
    /**
     * getter for name of channel component
     *
     * @return name of channel component
     */
    protected String getName()
    {
        return this.name;
    }
    
    /**
     * setter for name of channel component
     *
     * @param name name of channel component
     *
     * @return component configuration
     */
    protected ComponentBindingSetup setName(final String name)
    {
        this.name = name;
        return this;
    }
    
    /**
     * getter for max channel size
     *
     * @return max channel size or null, if property not set
     */
    protected Long getChannelCapacity()
    {
        return this.channelCapacity;
    }
    
    /**
     * setter for max channel size
     *
     * @param channelCapacity max size of stored messages
     *
     * @return channel component configuration
     */
    protected ComponentBindingSetup setChannelCapacity(final Long channelCapacity)
    {
        this.channelCapacity = channelCapacity;
        return this;
    }
    
    /**
     * getter for  private channel worker requirement
     *
     * @return necessity to use same channel worker thread for synchronized channel activities at all times
     */
    protected PrivateChannelWorkerRequirement getPrivateChannelWorkerRequirement()
    {
        return this.privateChannelWorkerRequirement;
    }
    
    /**
     * setter for  private channel worker requirement
     *
     * @param privateChannelWorkerRequirement declares necessity to use same channel worker thread for synchronized channel activities at all times
     *
     * @return channel component configuration
     */
    protected ComponentBindingSetup setPrivateChannelWorkerRequirement(final PrivateChannelWorkerRequirement privateChannelWorkerRequirement)
    {
        this.privateChannelWorkerRequirement = privateChannelWorkerRequirement;
        return this;
    }
    
    /**
     * Configuration to bind a {@link IDispatcherChannelComponent} (a {@link IDispatcherChannelManager} or a {@link IDispatcherChannelService}) to the {@link IDispatcherChannel}
     * with specified channelId managed by {@link IMessageDispatcher} with specified id.
     *
     * @author Sebastian Palarus
     *
     */
    public final static class BoundedByChannelId extends ComponentBindingSetup
    {
        /**
         *
         */
        private static final long serialVersionUID = 6587259399825230943L;
        
        private final String channelId;
        private boolean channelMaster = true;
        
        /**
         * Constructor to  bind a {@link IDispatcherChannelComponent} to the {@link IDispatcherChannel} with specified channelId.
         *
         * @param channelId id of channel
         */
        public BoundedByChannelId(final String channelId)
        {
            super();
            this.channelId = channelId;
        }
        
        /**
         * getter for channel id
         *
         * @return id of channel
         */
        public String getChannelId()
        {
            return this.channelId;
        }
        
        @Override
        public String getName()
        {
            return super.getName();
        }
        
        @Override
        public BoundedByChannelId setName(final String name)
        {
            return (BoundedByChannelId) super.setName(name);
        }
        
        /**
         * setter for channel master mode
         *
         * @param channelMaster if true, dispatcher creates automatically a channel with {@code channelId} if it still does not exist
         *
         * @return channel component configuration
         */
        public BoundedByChannelId setChannelMaster(final boolean channelMaster)
        {
            this.channelMaster = channelMaster;
            return this;
        }
        
        /**
         * getter for auto channel master mode
         *
         * @return true, if channel should automatically create, if not exists. return false, if not
         */
        public boolean isChannelMaster()
        {
            return this.channelMaster;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends IDispatcherChannelComponent>[] getScopes()
        {
            return new Class[] { IDispatcherChannelManager.class, IDispatcherChannelService.class };
        }
        
        @Override
        public BoundedByChannelId setDispatcherId(final String dispatcherId)
        {
            return (BoundedByChannelId) super.setDispatcherId(dispatcherId);
        }
        
        @Override
        protected BoundedByChannelId setChannelCapacity(final Long channelCapacity)
        {
            return (BoundedByChannelId) super.setChannelCapacity(channelCapacity);
        }
        
        @Override
        public BoundedByChannelId setPrivateChannelWorkerRequirement(final PrivateChannelWorkerRequirement privateChannelWorkerRequirement)
        {
            return (BoundedByChannelId) super.setPrivateChannelWorkerRequirement(privateChannelWorkerRequirement);
        }
        
        @Override
        public String getDispatcherId()
        {
            return super.getDispatcherId();
        }
        
        @Override
        protected Long getChannelCapacity()
        {
            return super.getChannelCapacity();
        }
        
        @Override
        public PrivateChannelWorkerRequirement getPrivateChannelWorkerRequirement()
        {
            return super.getPrivateChannelWorkerRequirement();
        }
        
        @Override
        public BoundedByChannelId copy()
        {
            return new BoundedByChannelId(this.channelId)
                .setDispatcherId(super.dispatcherId)
                .setName(super.name)
                .setChannelMaster(this.channelMaster)
                .setPrivateChannelWorkerRequirement(super.privateChannelWorkerRequirement)
                .setChannelCapacity(super.channelCapacity);
        }
        
    }
    
    /**
     * Configuration to bind a {@link IDispatcherChannelComponent} (a {@link IDispatcherChannelManager} or a {@link IDispatcherChannelService}) to existing {@link IDispatcherChannel}s
     * whose properties match a ldapfilter.
     *
     * @author Sebastian Palarus
     *
     */
    public final static class BoundedByChannelConfiguration extends ComponentBindingSetup
    {
        
        /**
         *
         */
        private static final long serialVersionUID = 7663354400942715419L;
        
        private final IFilterItem ldapFilter;
        
        /**
         * Constructor to  bind a {@link IDispatcherChannelComponent} to existing {@link IDispatcherChannel}s whose properties match the specified ldap-filter.
         *
         */
        public BoundedByChannelConfiguration(final IFilterItem ldapFilter)
        {
            super();
            this.ldapFilter = ldapFilter;
        }
        
        /**
         * getter for ldap filter
         *
         * @return ldap filter
         */
        public IFilterItem getLdapFilter()
        {
            return this.ldapFilter;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends IDispatcherChannelComponent>[] getScopes()
        {
            return new Class[] { IDispatcherChannelManager.class, IDispatcherChannelService.class };
        }
        
        @Override
        public BoundedByChannelConfiguration setDispatcherId(final String dispatcherId)
        {
            return (BoundedByChannelConfiguration) super.setDispatcherId(dispatcherId);
        }
        
        @Override
        protected BoundedByChannelConfiguration setChannelCapacity(final Long channelCapacity)
        {
            return (BoundedByChannelConfiguration) super.setChannelCapacity(channelCapacity);
        }
        
        @Override
        public BoundedByChannelConfiguration setPrivateChannelWorkerRequirement(final PrivateChannelWorkerRequirement privateChannelWorkerRequirement)
        {
            return (BoundedByChannelConfiguration) super.setPrivateChannelWorkerRequirement(privateChannelWorkerRequirement);
        }
        
        @Override
        public String getDispatcherId()
        {
            return super.getDispatcherId();
        }
        
        @Override
        public String getName()
        {
            return super.getName();
        }
        
        @Override
        public BoundedByChannelConfiguration setName(final String name)
        {
            return (BoundedByChannelConfiguration) super.setName(name);
        }
        
        @Override
        protected Long getChannelCapacity()
        {
            return super.getChannelCapacity();
        }
        
        @Override
        public PrivateChannelWorkerRequirement getPrivateChannelWorkerRequirement()
        {
            return super.getPrivateChannelWorkerRequirement();
        }
        
        @Override
        public BoundedByChannelConfiguration copy()
        {
            return new BoundedByChannelConfiguration(this.ldapFilter)
                .setDispatcherId(super.dispatcherId)
                .setName(super.name)
                .setPrivateChannelWorkerRequirement(super.privateChannelWorkerRequirement)
                .setChannelCapacity(super.channelCapacity);
        }
    }
    
    /**
     * Configuration for Channel Services
     *
     * @author Sebastian Palarus
     *
     */
    public static class ChannelServiceConfiguration extends ComponentBindingSetup
    {
        
        public ChannelServiceConfiguration(final String serviceId)
        {
            super();
            this.serviceId = serviceId;
        }
        
        /**
         *
         */
        private static final long serialVersionUID = 7301276962907883651L;
        
        private final String serviceId;
        private long timeOutInMS = -1;
        private long heartbeatTimeOutInMS = -1;
        private long startDelayInMS = 0;
        private long periodicRepetitionIntervalMS = -1;
        
        @Override
        public String getDispatcherId()
        {
            return super.getDispatcherId();
        }
        
        @Override
        public String getName()
        {
            return super.getName();
        }
        
        @Override
        public ChannelServiceConfiguration setName(final String name)
        {
            return (ChannelServiceConfiguration) super.setName(name);
        }
        
        /**
         * getter for service timeout in ms
         *
         * @return service timeout in ms
         */
        public long getTimeOutInMS()
        {
            return this.timeOutInMS;
        }
        
        /**
         *
         * setter for service timeout in ms
         *
         * @param timeOutInMS service timeout in ms
         *
         * @return channel service configuration
         */
        public ChannelServiceConfiguration setTimeOutInMS(final long timeOutInMS)
        {
            this.timeOutInMS = timeOutInMS;
            return this;
        }
        
        /**
         * getter for service heartbeat timeout in ms
         *
         * @return service heartbeat timeout
         */
        public long getHeartbeatTimeOutInMS()
        {
            return this.heartbeatTimeOutInMS;
        }
        
        /**
         * setter for service heartbeat timeout in ms
         *
         * @param heartbeatTimeOutInMS service heartbeat timeout in ms
         *
         * @return channel service configuration
         */
        public ChannelServiceConfiguration setHeartbeatTimeOutInMS(final long heartbeatTimeOutInMS)
        {
            this.heartbeatTimeOutInMS = heartbeatTimeOutInMS;
            return this;
        }
        
        /**
         * getter for start delay of service in ms
         *
         * @return start delay of service in ms
         */
        public long getStartDelayInMS()
        {
            return this.startDelayInMS;
        }
        
        /**
         * setter for start delay of service in ms
         *
         * @param startDelayInMS start delay of service in ms
         *
         * @return channel service configuration
         */
        public ChannelServiceConfiguration setStartDelayInMS(final long startDelayInMS)
        {
            this.startDelayInMS = startDelayInMS;
            return this;
        }
        
        /**
         * getter for periodic repetition interval of service in ms
         *
         * @return periodic repetition interval of service in ms
         */
        public long getPeriodicRepetitionIntervalMS()
        {
            return this.periodicRepetitionIntervalMS;
        }
        
        /**
         * setter for periodic repetition interval of service in ms
         *
         * @param periodicRepetitionIntervalMS repetition interval of service in ms
         *
         * @return channel service configuration
         */
        public ChannelServiceConfiguration setPeriodicRepetitionIntervalMS(final long periodicRepetitionIntervalMS)
        {
            this.periodicRepetitionIntervalMS = periodicRepetitionIntervalMS;
            return this;
        }
        
        @Override
        protected ChannelServiceConfiguration setChannelCapacity(final Long channelCapacity)
        {
            return (ChannelServiceConfiguration) super.setChannelCapacity(channelCapacity);
        }
        
        @Override
        public ChannelServiceConfiguration setPrivateChannelWorkerRequirement(final PrivateChannelWorkerRequirement privateChannelWorkerRequirement)
        {
            return (ChannelServiceConfiguration) super.setPrivateChannelWorkerRequirement(privateChannelWorkerRequirement);
        }
        
        @Override
        protected Long getChannelCapacity()
        {
            return super.getChannelCapacity();
        }
        
        @Override
        public PrivateChannelWorkerRequirement getPrivateChannelWorkerRequirement()
        {
            return super.getPrivateChannelWorkerRequirement();
        }
        
        /**
         * getter for service id
         *
         * @return id of service
         */
        public String getServiceId()
        {
            return this.serviceId;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends IDispatcherChannelComponent>[] getScopes()
        {
            return new Class[] { IDispatcherChannelService.class };
        }
        
        @Override
        public ChannelServiceConfiguration copy()
        {
            return new ChannelServiceConfiguration(this.serviceId)
                .setName(super.name)
                .setTimeOutInMS(this.timeOutInMS)
                .setHeartbeatTimeOutInMS(this.heartbeatTimeOutInMS)
                .setStartDelayInMS(this.startDelayInMS)
                .setPeriodicRepetitionIntervalMS(this.periodicRepetitionIntervalMS);
        }
    }
}
