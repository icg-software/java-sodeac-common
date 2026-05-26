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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;

public class Driver
{
    public static <T extends IDriver> T getSingleDriver(final Class<T> driverClass, final Map<String, Object> properties)
    {
        if (OSGiUtils.isOSGi())
        {
            T driver = OSGiUtils.getSingleDriver(driverClass, properties);
            if (driver != null)
            {
                return driver;
            }
        }
        ServiceLoader<T> serviceLoader = ServiceLoader.load(driverClass);
        Iterator<T> iterator = serviceLoader.iterator();
        T bestDriver = null;
        int bestIndex = -1;
        boolean hasNext = true;
        while (hasNext)
        {
            try
            {
                hasNext = iterator.hasNext();
                if (hasNext)
                {
                    T driverInstance = iterator.next();
                    int applicableIndex = driverInstance.driverIsApplicableFor(properties);
                    if (applicableIndex > bestIndex)
                    {
                        bestDriver = driverInstance;
                        bestIndex = applicableIndex;
                    }
                }
            }
            catch (final Exception e) { }
            catch (final Error e) { }
        }
        return bestDriver;
    }
    
    public static <T extends IDriver> boolean addUpdateListener(final Class<T> driverClass, final BiConsumer<T, T> updateListener)
    {
        if (OSGiUtils.isOSGi())
        {
            return OSGiUtils.addDriverUpdateListener(driverClass, updateListener);
        }
        return true;
    }
    
    public static <T extends IDriver> boolean removeUpdateListener(final Class<T> driverClass, final BiConsumer<T, T> updateListener)
    {
        if (OSGiUtils.isOSGi())
        {
            return OSGiUtils.removeDriverUpdateListener(driverClass, updateListener);
        }
        return true;
    }
    
    public static <T extends IDriver> List<T> getDriverList(final Class<T> driverClass, final Map<String, Object> properties)
    {
        if (OSGiUtils.isOSGi())
        {
            List<T> driverList = OSGiUtils.getDriverList(driverClass, properties);
            if (!driverList.isEmpty())
            {
                return driverList;
            }
        }
        ServiceLoader<T> serviceLoader = ServiceLoader.load(driverClass);
        Iterator<T> iterator = serviceLoader.iterator();
        List<T> list = new ArrayList<T>();
        Set<String> uniqueIndex = new HashSet<String>();
        boolean hasNext = true;
        while (hasNext)
        {
            try
            {
                hasNext = iterator.hasNext();
                if (hasNext)
                {
                    T driverInstance = iterator.next();
                    if (uniqueIndex.contains(driverInstance.getClass().getCanonicalName()))
                    {
                        continue;
                    }
                    int applicableIndex = driverInstance.driverIsApplicableFor(properties);
                    if (applicableIndex > IDriver.APPLICABLE_NONE)
                    {
                        list.add(driverInstance);
                        uniqueIndex.add(driverInstance.getClass().getCanonicalName());
                    }
                }
            }
            catch (final Exception e) { }
            catch (final Error e) { }
        }
        uniqueIndex.clear();
        return list;
    }
    
    public interface IDriver
    {
        String TYPE = "TYPE";
        
        int APPLICABLE_NONE = -1;
        int APPLICABLE_FALLBACK = 0;
        int APPLICABLE_DEFAULT = 10000;
        
        int driverIsApplicableFor(Map<String, Object> properties);
    }
}
