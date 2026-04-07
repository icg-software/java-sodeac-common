package org.sodeac.common.impl;

import org.sodeac.common.IService.IServiceProvider;
import org.sodeac.common.xuri.URI;

public class LocalService
{
	private static final LocalServiceRegistryImpl LOCAL_SERVICE_REGISTRY = LocalServiceRegistryImpl.get();
	
	public static <S> IServiceProvider<S> getServiceProvider(Class<S> clazz, URI address)
	{
		return LOCAL_SERVICE_REGISTRY.getServiceProvider(clazz, address);
	}
	
	protected static LocalServiceRegistryImpl getLocalServiceRegistryImpl()
	{
		return LOCAL_SERVICE_REGISTRY;
	}
}
