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
package org.sodeac.common.misc;

public class RuntimeWrappedException extends RuntimeException
{
    /**
     *
     */
    private static final long serialVersionUID = 8985443414667960529L;
    
    public RuntimeWrappedException(final Throwable throwable)
    {
        super(throwable.getMessage(), throwable);
    }
    
}
