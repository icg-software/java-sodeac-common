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

import java.util.function.Function;

import org.sodeac.common.misc.RuntimeWrappedException;

public interface ExceptionCatchedFunction<T, R> extends Function<T, R>
{
    
    @Override
    default R apply(final T t)
    {
        try
        {
            return applyWithException(t);
        }
        catch (final Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeWrappedException(e);
        }
        catch (final Error e)
        {
            throw new RuntimeWrappedException(e);
        }
    }
    
    /**
     * Applies this function to the given argument with potentially throws an exception.
     *
     * @param t the function argument
     *
     * @return the function result
     *
     * @throws Exception
     */
    R applyWithException(T t) throws Exception, Error;
    
    static <T, R> Function<T, R> wrap(final ExceptionCatchedFunction<T, R> function)
    {
        return new Function<T, R>()
        {
            @Override
            public R apply(final T t)
            {
                return function.apply(t);
            }
        };
    }
}
