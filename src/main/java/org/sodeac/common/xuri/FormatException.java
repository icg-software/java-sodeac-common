/*******************************************************************************
 * Copyright (c) 2016, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri;

/**
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class FormatException extends RuntimeException
{
    /**
     *
     */
    private static final long serialVersionUID = 2434139633155210329L;
    
    public FormatException()
    {
        super();
    }
    
    public FormatException(final String message)
    {
        super(message);
    }
    
    public FormatException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
    
    public FormatException(final Throwable cause)
    {
        super(cause);
    }
    
    public FormatException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
