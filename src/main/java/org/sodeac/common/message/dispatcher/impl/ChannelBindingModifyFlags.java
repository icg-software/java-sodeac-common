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
package org.sodeac.common.message.dispatcher.impl;

public class ChannelBindingModifyFlags
{
	
	protected ChannelBindingModifyFlags()
	{
		super();
	}
	
	private boolean rootSet = false;
	private boolean rootAdd = false;
	private boolean rootRemove = false;
	private boolean subSet = false;
	private boolean subAdd = false;
	private boolean subRemove = false;
	
	public void reset()
	{
		rootSet = false;
		rootAdd = false;
		rootRemove = false;
		subSet = false;
		subAdd = false;
		subRemove = false;
	}

	public boolean isRootSet()
	{
		return rootSet;
	}

	public void setRootSet(boolean rootSet)
	{
		this.rootSet = rootSet;
	}

	public boolean isRootAdd()
	{
		return rootAdd;
	}

	public void setRootAdd(boolean rootAdd)
	{
		this.rootAdd = rootAdd;
	}

	public boolean isRootRemove()
	{
		return rootRemove;
	}

	public void setRootRemove(boolean rootRemove)
	{
		this.rootRemove = rootRemove;
	}

	public boolean isSubSet()
	{
		return subSet;
	}

	public void setSubSet(boolean subSet)
	{
		this.subSet = subSet;
	}

	public boolean isSubAdd()
	{
		return subAdd;
	}

	public void setSubAdd(boolean subAdd)
	{
		this.subAdd = subAdd;
	}

	public boolean isSubRemove()
	{
		return subRemove;
	}

	public void setSubRemove(boolean subRemove)
	{
		this.subRemove = subRemove;
	}
}
