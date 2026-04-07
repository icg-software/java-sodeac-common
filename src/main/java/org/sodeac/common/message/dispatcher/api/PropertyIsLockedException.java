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

/**
 *
 * @author Sebastian Palarus
 *
 */
public class PropertyIsLockedException extends RuntimeException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -6738548351245631901L;
    
    public PropertyIsLockedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
    public PropertyIsLockedException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
    
    public PropertyIsLockedException(final String message)
    {
        super(message);
    }
    
    public PropertyIsLockedException(final Throwable cause)
    {
        super(cause);
    }
    
}
