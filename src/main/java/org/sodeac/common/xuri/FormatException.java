/*******************************************************************************
 * Copyright (c) 2016, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri;

/**
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public class FormatException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2434139633155210329L;

	public FormatException()
	{
		super();
	}
	
	public FormatException(String message) 
	{
		super(message);
	}
	
	public FormatException(String message, Throwable cause) 
	{
		super(message,cause);
	}
	
	public FormatException(Throwable cause) 
	{
		super(cause);
	}
	
	public FormatException(String message, Throwable cause,boolean enableSuppression,boolean writableStackTrace) 
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
