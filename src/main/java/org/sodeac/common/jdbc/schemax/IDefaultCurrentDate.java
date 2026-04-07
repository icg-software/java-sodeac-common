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

import java.sql.Connection;
import java.util.Dictionary;
import java.util.Map;

import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.IDefaultValueExpressionDriver;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.typedtree.BranchNode;

public interface IDefaultCurrentDate extends IDefaultValueExpressionDriver
{
    
    @Override
    default int driverIsApplicableFor(Map<String, Object> properties)
    {
        return IDriver.APPLICABLE_DEFAULT;
    }
    
    @Override
    default String createExpression
        (
            BranchNode<?, ColumnNodeType> column,
            Connection connection,
            String schema,
            Dictionary<String, Object> properties,
            IDBSchemaUtilsDriver driver
        )
    {
        return driver.getFunctionExpression("CURRENT_DATE");
    }
    
}
