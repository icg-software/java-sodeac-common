/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.expression;

import java.util.Objects;

import org.sodeac.common.function.ConplierBean;

public class Variable<T> implements IExpression<T>
{
	public Variable(Class<T> type)
	{
		super();
		this.container = new ConplierBean<>();
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	public Variable(T initialValue)
	{
		super();
		Objects.requireNonNull(initialValue);
		this.container = new ConplierBean<>(initialValue);
		this.type = (Class<T>)initialValue.getClass();
	}
	
	protected ConplierBean<T> container = null;
	protected Class<T> type = null;

	public ConplierBean<T> getContainer()
	{
		return container;
	}

	@Override
	public Class<T> getExpressionType()
	{
		return this.type;
	}

	@Override
	public String getExpressionString()
	{
		return this.container.get() == null ? "null" : this.container.get().toString();
	}

	@Override
	public T evaluate(Context context)
	{
		return this.container.get();
	}

	@Override
	public void dispose()
	{
		IExpression.super.dispose();
		if(this.container != null)
		{
			this.container.dispose();
		}
		this.container = null;
	}
}
