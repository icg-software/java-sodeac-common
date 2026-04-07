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

package org.sodeac.common.xuri.ldapfilter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enum of logical operator to link ldap filter items
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum LogicalOperator 
{
	AND		( 1,	IFilterItem.AND	),
	OR		( 2,	IFilterItem.OR	);
	
	
	private LogicalOperator(int intValue, char abbreviation)
	{
		this.intValue = intValue;
		this.abbreviation = abbreviation;
	}
	
	private static volatile Set<LogicalOperator> ALL = null;
	
	private int intValue;
	private char abbreviation;
	
	
	public int getIntValue() 
	{
		return intValue;
	}

	public char getAbbreviation() 
	{
		return abbreviation;
	}
	
	public static Set<LogicalOperator> getAll()
	{
		if(LogicalOperator.ALL == null)
		{
			EnumSet<LogicalOperator> all = EnumSet.allOf(LogicalOperator.class);
			LogicalOperator.ALL = Collections.unmodifiableSet(all);
		}
		return LogicalOperator.ALL;
	}
	
	public static LogicalOperator findByInteger(int value)
	{
		for(LogicalOperator operation : getAll())
		{
			if(operation.intValue == value)
			{
				return operation;
			}
		}
		return null;
	}
	
	public static LogicalOperator findByAbbreviation(char abbreviation)
	{
		for(LogicalOperator operation : getAll())
		{
			if(operation.abbreviation == abbreviation)
			{
				return operation;
			}
		}
		return null;
	}
	
	public static LogicalOperator findByName(String name)
	{
		for(LogicalOperator operation : getAll())
		{
			if(operation.name().equalsIgnoreCase(name))
			{
				return operation;
			}
		}
		return null;
	}
}
