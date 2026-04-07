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

import java.util.function.Consumer;

import org.sodeac.common.misc.RuntimeWrappedException;

/**
 * Extends {@link Consumer} to consume with potentially throws an exception. Catched exceptions will delegate as {@link RuntimeWrappedException}
 *
 * @param <T>
 *
 * @author Sebastian Palarus
 */
@FunctionalInterface
public interface ExceptionCatchedConsumer<T> extends Consumer<T>
{
    @Override
    default void accept(T t)
    {
        try
        {
            acceptWithException(t);
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
     * Consume object with potentially throws an exception.
     *
     * @param t object to consume
     *
     * @throws Exception
     */
    public void acceptWithException(T t) throws Exception, Error;
    
    public static <T> Consumer<T> wrap(ExceptionCatchedConsumer<T> consumer)
    {
        return new Consumer<T>()
        {
            @Override
            public void accept(T t)
            {
                consumer.accept(t);
            }
        };
    }
}
