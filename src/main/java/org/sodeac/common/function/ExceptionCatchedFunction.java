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
	default R apply(T t)
	{
		try
		{
			return applyWithException(t);
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
	 * Applies this function to the given argument with potentially throws an exception.
	 * 
	 * @param t the function argument
	 * @return the function result
	 * @throws Exception
	 */
	public R applyWithException(T t) throws Exception, Error;
	
	public static <T,R> Function<T,R> wrap(ExceptionCatchedFunction<T,R> function)
	{
		return new Function<T,R>()
		{
			@Override
			public R apply(T t)
			{
				return function.apply(t);
			}
		};
	}
}
