package org.sodeac.common.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
			Bundle bundle = FrameworkUtil.getBundle(packageClass);
			if(bundle == null)
			{
				return null;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			URL url = bundle.getResource(packageClass.getPackage().getName().replaceAll("\\.", "/") + "/" + fileName);
			InputStream inputStream = url.openStream();
			try
			{
				
				url = null;
				bundle = null;
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
	}
	
	public static <T extends IDriver> T getSingleDriver(Class<T> driverClass, Map<String,Object> properties)
	{
		return OSGiDriverRegistry.INSTANCE.getSingleDriver(driverClass, properties);
	}
	
	private static class TesterConfiguration
	{
		private volatile Boolean isOSGI = null;
	}
}