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

import org.sodeac.common.xuri.ldapfilter.ComparativeOperator;
import org.sodeac.common.xuri.ldapfilter.IMatchable;

public class TestMatchable implements IMatchable
{

	@Override
	public boolean matches(ComparativeOperator operator, String name, String valueExpression)
	{
		return name.equalsIgnoreCase(Boolean.TRUE.toString());
	}

}
