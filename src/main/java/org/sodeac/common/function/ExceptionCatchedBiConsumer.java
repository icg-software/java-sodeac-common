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

import java.util.function.BiConsumer;

import org.sodeac.common.misc.RuntimeWrappedException;

/**
 *
 * @param <T>
 * @param <U>
 *
 * @author Sebastian Palarus
 */
public interface ExceptionCatchedBiConsumer<T, U> extends BiConsumer<T, U>
{
    
    @Override
    default void accept(T t, U u)
    {
        try
        {
            acceptWithException(t, u);
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
    
    /**
     *
     * Consume object with potentially throws an exception.
     *
     * @param t object to consume
     * @param u object to consume
     *
     * @throws Exception
     */
    public void acceptWithException(T t, U u) throws Exception, Error;
    
    public static <T, U> BiConsumer<T, U> wrap(ExceptionCatchedBiConsumer<T, U> consumer)
    {
        return new BiConsumer<T, U>()
        {
            @Override
            public void accept(T t, U u)
            {
                consumer.accept(t, u);
            }
        };
    }
    
}
