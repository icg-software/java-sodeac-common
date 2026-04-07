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
package org.sodeac.common.jdbc.schemax;

import java.util.Map;

import org.sodeac.common.jdbc.IDefaultValueExpressionDriver;
import org.sodeac.common.misc.Driver.IDriver;

public interface IDefaultUUID extends IDefaultValueExpressionDriver
{
	@Override
	default int driverIsApplicableFor(Map<String, Object> properties)
	{
		return IDriver.APPLICABLE_NONE;
	}
}
