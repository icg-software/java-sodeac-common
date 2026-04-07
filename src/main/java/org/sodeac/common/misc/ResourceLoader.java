/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
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

/**
 * Helper class around package resources
 *
 * @author "Sebastian Palarus"
 *
 */
public class ResourceLoader
{
    
    /**
     * Load package resource as string in OSGi or Non-OSGi environment.
     *
     * @param fileName     name of string resource
     * @param packageClass class represents the package
     *
     * @return string resource
     *
     * @throws IOException
     */
    public static String loadPackageFileAsString(final String fileName, final Class<?> packageClass) throws IOException
    {
        if (OSGiUtils.isOSGi())
        {
            String stringResource = OSGiUtils.loadPackageFileAsString(fileName, packageClass);
            if (stringResource != null)
            {
                return stringResource;
            }
        }
        InputStream inputStream = packageClass.getClassLoader().getResourceAsStream(packageClass.getPackage().getName().replaceAll("\\.", "/") + "/" + fileName);
        if (inputStream == null)
        {
            return null;
        }
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        try
        {
            int len;
            byte[] buf = new byte[1080];
            while ((len = inputStream.read(buf)) > 0)
            {
                byos.write(buf, 0, len);
            }
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (final Exception e) { }
            try
            {
                byos.flush();
            }
            catch (final Exception e) { }
            try
            {
                byos.close();
            }
            catch (final Exception e) { }
        }
        
        return byos.toString();
    }
    
    /**
     * Load package {@link InputStream} in OSGi or Non-OSGi environment.
     *
     * @param fileName     name of string resource
     * @param packageClass class represents the package
     *
     * @return input stream
     *
     * @throws IOException
     */
    public static InputStream loadPackageInputStream(final String fileName, final Class<?> packageClass) throws IOException
    {
        if (OSGiUtils.isOSGi())
        {
            InputStream inputStream = OSGiUtils.loadPackageInputStream(fileName, packageClass);
            if (inputStream != null)
            {
                return inputStream;
            }
        }
        return packageClass.getClassLoader().getResourceAsStream(packageClass.getPackage().getName().replaceAll("\\.", "/") + "/" + fileName);
    }
}
