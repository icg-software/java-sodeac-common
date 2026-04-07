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
package org.sodeac.common.function;

import java.util.function.Supplier;

import org.sodeac.common.misc.RuntimeWrappedException;

public interface ExceptionCatchedSupplier<T> extends Supplier<T>
{
    
    @Override
    default T get()
    {
        try
        {
            return getWithException();
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeWrappedException(e);
        }
        catch (Error e)
        {
            throw new RuntimeWrappedException(e);
        }
    }
    
    public T getWithException() throws Exception, Error;
    
    public static <T> Supplier<T> wrap(ExceptionCatchedSupplier<T> supplier)
    {
        return new Supplier<T>()
        {
            @Override
            public T get()
            {
                return supplier.get();
            }
        };
    }
    
}
