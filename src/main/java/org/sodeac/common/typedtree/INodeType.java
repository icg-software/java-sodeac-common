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
package org.sodeac.common.typedtree;

import java.lang.reflect.Field;

/**
 * A node type defines the type of node.
 * 
 * @author Sebastian Palarus
 *
 * @param <P> type parent node
 * @param <T> type of node
 */
public interface INodeType<P extends BranchNodeMetaModel, T>
{
	/**
	 * Getter for node's type.
	 * 
	 * @return type of node.
	 */
	public Class<T> getTypeClass();
	
	/**
	 * Getter for type of parent node.
	 * 
	 * @return type of parent node
	 */
	public Class<P> getParentNodeClass();
	
	/**
	 * Getter for name of node type.
	 * 
	 * @return name of node type
	 */
	public String getNodeName();
	
	public Field referencedByField();
	
	/**
	 * 
	 * @return default instance of node value
	 */
	public default T getValueDefaultInstance()
	{
		try
		{
			return getTypeClass().newInstance();
		} 
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
}
