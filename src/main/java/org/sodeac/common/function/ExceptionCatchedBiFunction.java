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

import java.util.function.BiFunction;

import org.sodeac.common.misc.RuntimeWrappedException;

public interface ExceptionCatchedBiFunction<T, U, R> extends BiFunction<T, U, R>
{

	@Override
	default R apply(T t, U u)
	{
		try
		{
			return applyWithException(t,u);
		}
		catch (Exception e) 
		{
			if(e instanceof RuntimeException)
			{
				throw (RuntimeException)e;
			}
			throw new RuntimeWrappedException(e);
		}
		catch (Error e) 
		{
			throw new RuntimeWrappedException(e);
		}
	}
	
	/**
	 * Applies this function to the given arguments with potentially throws an exception.
	 * 
	 * @param t the first function argument
     * @param u the second function argument
	 * @return the function result
	 * @throws Exception
	 */
	public R applyWithException(T t, U u) throws Exception, Error;
	
	public static <T, U, R> BiFunction<T, U, R> wrap(ExceptionCatchedBiFunction<T, U, R> function)
	{
		return new BiFunction<T, U, R>()
		{
			@Override
			public R apply(T t, U u)
			{
				return function.apply(t,u);
			}
		};
	}
}
