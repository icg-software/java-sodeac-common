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
package org.sodeac.common.xuri.ldapfilter;

/**
 * Interface of matchable property
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public interface IMatchable
{
	/**
	 * returns matchable match to ldap attribute
	 * 
	 * @param operator ldap operator
	 * @param name name of ldap attribute
	 * @param valueExpression value of ldap attribute
	 * 
	 * @return matchable match to atomic ldap expression
	 */
	public boolean matches(ComparativeOperator operator, String name, String valueExpression);
}