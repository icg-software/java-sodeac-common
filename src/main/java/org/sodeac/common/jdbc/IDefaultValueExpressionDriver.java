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
package org.sodeac.common.jdbc;

import java.sql.Connection;
import java.util.Dictionary;

import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.typedtree.BranchNode;

public interface IDefaultValueExpressionDriver extends IDriver
{
    public String createExpression(BranchNode<?, ColumnNodeType> column, Connection connection, String schema, Dictionary<String, Object> properties, IDBSchemaUtilsDriver driver);
    
    public default boolean updateRequired(BranchNode<?, ColumnNodeType> column, Connection connection, String schema, Dictionary<String, Object> properties, IDBSchemaUtilsDriver driver, String currentValue)
    {
        String defaultValue = createExpression(column, connection, schema, properties, driver);
        
        return !defaultValue.equalsIgnoreCase(currentValue);
        
    }
}
