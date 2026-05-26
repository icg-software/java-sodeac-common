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
package org.sodeac.common.jdbc;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TestTools
{
    
    public static String getSchemaName()
    {
        try
        {
            Date begin = new SimpleDateFormat("yyyyMMddHHmmssSSS").parse("20200101000000000");
            Date now = new Date();
            long diff = now.getTime() - begin.getTime();
            diff = diff / 1000;
            Thread.sleep(2000);
            return String.format("%09X", diff);
        }
        catch (final Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
