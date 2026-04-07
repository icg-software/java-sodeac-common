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

package org.sodeac.common.xuri.ldapfilter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enum of ldap operators
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum ComparativeOperator 
{
	EQUAL		( 1,	"="		),
	GTE			( 2,	">="		),
	LTE			( 3,	"<="		),
	APPROX		( 4,	"~="		);
	
	
	private ComparativeOperator(int intValue, String abbreviation)
	{
		this.intValue = intValue;
		this.abbreviation = abbreviation;
	}
	
	private static volatile Set<ComparativeOperator> ALL = null;
	
	private int intValue;
	private String abbreviation;
	
	
	public int getIntValue() 
	{
		return intValue;
	}

	public String getAbbreviation() 
	{
		return abbreviation;
	}
	
	public static Set<ComparativeOperator> getAll()
	{
		if(ComparativeOperator.ALL == null)
		{
			EnumSet<ComparativeOperator> all = EnumSet.allOf(ComparativeOperator.class);
			ComparativeOperator.ALL = Collections.unmodifiableSet(all);
		}
		return ComparativeOperator.ALL;
	}
	
	public static ComparativeOperator findByInteger(int value)
	{
		for(ComparativeOperator operation : getAll())
		{
			if(operation.intValue == value)
			{
				return operation;
			}
		}
		return null;
	}
	
	public static ComparativeOperator findByAbbreviation(String abbreviation)
	{
		for(ComparativeOperator operation : getAll())
		{
			if(operation.abbreviation.equals(abbreviation))
			{
				return operation;
			}
		}
		return null;
	}
	
	public static ComparativeOperator findByName(String name)
	{
		for(ComparativeOperator operation : getAll())
		{
			if(operation.name().equalsIgnoreCase(name))
			{
				return operation;
			}
		}
		return null;
	}
}
