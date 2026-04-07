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
package org.sodeac.common.snapdeque;

public class CapacityExceededException extends IllegalStateException
{
    private long capacity;
    
    /**
     *
     */
    private static final long serialVersionUID = -4229386960263853604L;
    
    public CapacityExceededException(long capacity)
    {
        super();
        this.capacity = capacity;
    }
    
    public CapacityExceededException(long capacity, String message, Throwable cause)
    {
        super(message, cause);
        this.capacity = capacity;
    }
    
    public CapacityExceededException(long capacity, String s)
    {
        super(s);
        this.capacity = capacity;
    }
    
    public CapacityExceededException(long capacity, Throwable cause)
    {
        super(cause);
        this.capacity = capacity;
    }
    
    public long getCapacity()
    {
        return capacity;
    }
}
