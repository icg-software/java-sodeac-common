/*******************************************************************************
 * Copyright (c) 2016, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri;

import org.sodeac.common.misc.Driver.IDriver;

/**
 * 
 * Interface for extensions to define and handle an expression string
 * 
 * @author Sebastian Palarus
 *
 */
public interface IExtension<T> extends IDriver
{
	/**
	 * 
	 * @return type of extension
	 */
	public String getType();
	
	/**
	 * 
	 * @return representative expression string for extension
	 */
	public String getExpression();
	
	/**
	 * getter for default decoder instance
	 * 
	 * @return default decoder instance
	 */
	public IDecodingExtensionHandler<T> getDecoder();
	
	
	/**
	 * getter for default encoder instance
	 * 
	 * @return default encoder instance
	 */
	public IEncodingExtensionHandler<T> getEncoder();
}
