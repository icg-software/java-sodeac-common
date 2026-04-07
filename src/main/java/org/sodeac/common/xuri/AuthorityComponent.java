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

/*
 * https://tools.ietf.org/html/rfc3986#section-3.2
 */

/**
 * Authority component of URI. Authority components contains multiple subcomponents of type {@link AuthoritySubComponent}. 
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public class AuthorityComponent extends AbstractComponent<AuthoritySubComponent>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3937914592142761246L;


	/**
	 * constructor for authority component
	 */
	public AuthorityComponent()
	{
		super(ComponentType.AUTHORITY);
	}
	
}
