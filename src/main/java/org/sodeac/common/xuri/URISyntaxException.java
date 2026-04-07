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
package org.sodeac.common.xuri;

public class URISyntaxException extends RuntimeException
{
	private String input = null;
	
	public URISyntaxException(String input, String reason)
	{
		super(reason);
		this.input = input;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -107918100196279807L;

	public String getInput()
	{
		return input;
	}

}
