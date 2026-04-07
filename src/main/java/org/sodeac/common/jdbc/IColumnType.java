/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
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
import java.sql.SQLException;

import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

/**
 * Interface for column type driver implementations
 *
 * @author Sebastian Palarus
 *
 */
public interface IColumnType extends IDriver
{
    public static enum ColumnType
    {CHAR, VARCHAR, CLOB, UUID, BOOLEAN, SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, TIMESTAMP, DATE, TIME, BINARY, BLOB}
    
    /**
     * return the expression for column type in create or alter column command
     *
     * @param connection   used connection
     * @param schema       used schema specification
     * @param table        used table specification
     * @param column       used column specification
     * @param dbProduct    database product name
     * @param schemaDriver used schema driver
     *
     * @return expression for column type in create or alter column command
     *
     * @throws SQLException
     */
    public String getTypeExpression
    (
        Connection connection,
        BranchNode<?, DBSchemaNodeType> schema,
        BranchNode<?, TableNodeType> table,
        BranchNode<?, ColumnNodeType> column,
        String dbProduct,
        IDBSchemaUtilsDriver schemaDriver
    ) throws SQLException;
    
    /**
     * return the expression for default type of column in create or alter column command
     *
     * @param connection   used connection
     * @param schema       used schema specification
     * @param table        used table specification
     * @param column       used column specification
     * @param dbProduct    database product name
     * @param schemaDriver used schema driver
     *
     * @return expression for default type of column in create or alter column command
     *
     * @throws SQLException
     */
    public String getDefaultValueExpression
    (
        Connection connection,
        BranchNode<?, DBSchemaNodeType> schema,
        BranchNode<?, TableNodeType> table,
        BranchNode<?, ColumnNodeType> column,
        String dbProduct,
        IDBSchemaUtilsDriver schemaDriver
    ) throws SQLException;
}
