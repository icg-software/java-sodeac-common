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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.sodeac.common.misc.Driver.IDriver;

public class OSGiUtils 
{
	public static final TesterConfiguration TESTER_CONFIGURATION = new TesterConfiguration();
	
	public static boolean isOSGi()
	{
		if(OSGiUtils.TESTER_CONFIGURATION.isOSGI != null)
		{
			return OSGiUtils.TESTER_CONFIGURATION.isOSGI;
		}
		
		OSGiUtils.TESTER_CONFIGURATION.isOSGI = false;
		
		try
		{
			Class<?> clazz = OSGiUtils.class.getClassLoader().loadClass("org.osgi.framework.FrameworkUtil");
			Objects.requireNonNull(clazz);
			if(InternalUtils.test())
			{
				OSGiUtils.TESTER_CONFIGURATION.isOSGI = true;
				return true;
			}
		}
		catch (Error e){}
		catch (Exception e) {}
		
		OSGiUtils.TESTER_CONFIGURATION.isOSGI = false;
		
		return false;
	}
	
	public static String getSymbolicName(Class<?> clazz)
	{
		if(! isOSGi())
		{
			return null;
		}
		
		return InternalUtils.getSymbolicName(clazz);
	}
	
	public static String getVersion(Class<?> clazz)
	{
		if(! isOSGi())
		{
			return null;
		}
		
		return InternalUtils.getVersion(clazz);
	}
	
	public static String loadPackageFileAsString(String fileName, Class<?> packageClass) throws IOException
	{
		if(! isOSGi())
		{
			return null;
		}
		
		return InternalUtils.loadPackageFileAsString(fileName, packageClass);
	}
	
	public static InputStream loadPackageInputStream(String fileName, Class<?> packageClass) throws IOException
	{
		if(! isOSGi())
		{
			return null;
		}
		
		return InternalUtils.loadPackageInputStream(fileName, packageClass);
	}
	
	private static class InternalUtils
	{
		private static boolean test()
		{
			org.osgi.framework.Bundle bundle = null;
			if((bundle = org.osgi.framework.FrameworkUtil.getBundle(OSGiUtils.class)) == null)
			{
				return false;
			}
			
			if(bundle.getBundleId() < 1L)
			{
				return false;
			}
			
			return bundle.getState() == org.osgi.framework.Bundle.ACTIVE;
		}
		
		private static String getSymbolicName(Class<?> clazz)
		{
			org.osgi.framework.Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(clazz);
			if(bundle == null)
			{
				return null;
			}
			return bundle.getSymbolicName();
		}
		
		private static String getVersion(Class<?> clazz)
		{
			org.osgi.framework.Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(clazz);
			if(bundle == null)
			{
				return null;
			}
			org.osgi.framework.Version version = bundle.getVersion();
			if(version == null)
			{
				return null;
			}
			return bundle.getVersion().toString();
		}
		
		private static String loadPackageFileAsString(String fileName, Class<?> packageClass) throws IOException
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream inputStream = loadPackageInputStream(fileName, packageClass);
			try
			{
				if(inputStream == null)
				{
					return null;
				}
				
				int len;
				byte[] buf = new byte[1080];
				while((len = inputStream.read(buf)) > 0)
				{
					baos.write(buf, 0, len);
				}
			}
			finally
			{
				try
				{
					if(inputStream != null)
					{
						inputStream.close();
						inputStream = null;
					}
				}
				catch (Exception e) {}
				try
				{
					baos.flush();
				}
				catch (Exception e) {}
				try
				{
					baos.close();
				}
				catch (Exception e) {}
			}
			return baos.toString();
		}
		
		private static InputStream loadPackageInputStream(String fileName, Class<?> packageClass) throws IOException
		{
			Bundle bundle = FrameworkUtil.getBundle(packageClass);
			if(bundle == null)
			{
				return null;
			}
			URL url = bundle.getResource(packageClass.getPackage().getName().replaceAll("\\.", "/") + "/" + fileName);
			return url.openStream();
		}
	}
	
	public static <T extends IDriver> T getSingleDriver(Class<T> driverClass, Map<String,Object> properties)
	{
		return OSGiDriverRegistry.INSTANCE.getSingleDriver(driverClass, properties);
	}
	
	public static <T extends IDriver> List<T> getDriverList(Class<T> driverClass, Map<String,Object> properties)
	{
		return OSGiDriverRegistry.INSTANCE.getDriverList(driverClass, properties);
	}
	
	private static class TesterConfiguration
	{
		private volatile Boolean isOSGI = null;
	}
}
