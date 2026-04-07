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
package org.sodeac.common.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.jdbc.IColumnType;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.IDefaultValueExpressionDriver;
import org.sodeac.common.misc.Driver;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Component(name = "DefaultColumnType", service = IColumnType.class, property = "defaultdriver=true")
public class DefaultColumnTypeImpl implements IColumnType
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;

    private static final Set<String> supportedTypes;

    static
    {
        supportedTypes = new HashSet<>();
        supportedTypes.add(ColumnType.CHAR.name());
        supportedTypes.add(ColumnType.VARCHAR.name());
        supportedTypes.add(ColumnType.CLOB.name());
        supportedTypes.add(ColumnType.UUID.name());
        supportedTypes.add(ColumnType.BOOLEAN.name());
        supportedTypes.add(ColumnType.SMALLINT.name());
        supportedTypes.add(ColumnType.INTEGER.name());
        supportedTypes.add(ColumnType.BIGINT.name());
        supportedTypes.add(ColumnType.REAL.name());
        supportedTypes.add(ColumnType.DOUBLE.name());
        supportedTypes.add(ColumnType.TIMESTAMP.name());
        supportedTypes.add(ColumnType.DATE.name());
        supportedTypes.add(ColumnType.TIME.name());
        supportedTypes.add(ColumnType.BINARY.name());
        supportedTypes.add(ColumnType.BLOB.name());
    }

    @Override
    public int driverIsApplicableFor(final Map<String, Object> properties)
    {
        final BranchNode<?, ColumnNodeType> column = (BranchNode<?, ColumnNodeType>) properties.get("COLUMN");
        if(!supportedTypes.contains(column.getValue(ColumnNodeType.columnType).toUpperCase()))
        {
            return IDriver.APPLICABLE_NONE;
        }
        return IDriver.APPLICABLE_FALLBACK;
    }

    @Override
    public String getTypeExpression
            (
                    final Connection connection,
                    final BranchNode<?, DBSchemaNodeType> schema,
                    final BranchNode<?, TableNodeType> table,
                    final BranchNode<?, ColumnNodeType> column,
                    final String dbProduct,
                    final IDBSchemaUtilsDriver schemaDriver
            ) throws SQLException
    {
        final String columnType = column.getValue(ColumnNodeType.columnType);
        final String databaseName = connection.getMetaData().getDatabaseProductName();
        if((columnType == null) || ColumnType.VARCHAR.toString().equalsIgnoreCase(columnType) || ColumnType.CHAR.toString().equalsIgnoreCase(columnType))
        {
            final String type = (columnType == null) || ColumnType.VARCHAR.toString().equalsIgnoreCase(columnType) ? "VARCHAR" : "CHAR";

            if((column.getValue(ColumnNodeType.size) != null) && (column.getValue(ColumnNodeType.size).intValue() > 0))
            {
                if(ColumnType.VARCHAR.toString().equalsIgnoreCase(type) && databaseName.equalsIgnoreCase("Oracle"))
                {
                    return schemaDriver.objectNameGuidelineFormat(schema, connection, type + "(" + column.getValue(ColumnNodeType.size) + " CHAR)", "COLUMN_TYPE");
                }
                return schemaDriver.objectNameGuidelineFormat(schema, connection, type + "(" + column.getValue(ColumnNodeType.size) + ")", "COLUMN_TYPE");
            }
            else
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, type, "COLUMN_TYPE");
            }
        }

        if(ColumnType.CLOB.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("PostgreSQL"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "text", "COLUMN_TYPE");
            }
        }
        if(ColumnType.REAL.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("PostgreSQL"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "float4", "COLUMN_TYPE");
            }
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "FLOAT(63)", "COLUMN_TYPE");
            }
        }
        if(ColumnType.DOUBLE.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("PostgreSQL"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "float8", "COLUMN_TYPE");
            }
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "FLOAT(126)", "COLUMN_TYPE");
            }
        }
        if(ColumnType.BINARY.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("H2"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "BLOB", "COLUMN_TYPE");
            }
            if(databaseName.equalsIgnoreCase("PostgreSQL"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "bytea", "COLUMN_TYPE");
            }
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "LONG RAW", "COLUMN_TYPE");
            }
        }

        if(ColumnType.BLOB.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("PostgreSQL"))
            {// TODO: to BLOB, oid is for old PG Large Objects
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "oid", "COLUMN_TYPE");
            }
            if(databaseName.equalsIgnoreCase("H2"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "BLOB", "COLUMN_TYPE");
            }
        }

        if(ColumnType.BOOLEAN.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "CHAR(1)", "COLUMN_TYPE");
            }
        }

        if(ColumnType.SMALLINT.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "NUMBER(5)", "COLUMN_TYPE");
            }
        }

        if(ColumnType.INTEGER.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "NUMBER(10)", "COLUMN_TYPE");
            }
        }

        if(ColumnType.BIGINT.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "NUMBER(19)", "COLUMN_TYPE");
            }
        }

        if(ColumnType.TIME.toString().equalsIgnoreCase(columnType))
        {
            if(databaseName.equalsIgnoreCase("Oracle"))
            {
                return schemaDriver.objectNameGuidelineFormat(schema, connection, "DATE", "COLUMN_TYPE");
            }
        }

        return schemaDriver.objectNameGuidelineFormat(schema, connection, columnType, "COLUMN_TYPE");
    }

    @Override
    public String getDefaultValueExpression
            (
                    final Connection connection,
                    final BranchNode<?, DBSchemaNodeType> schema,
                    final BranchNode<?, TableNodeType> table,
                    final BranchNode<?, ColumnNodeType> column,
                    final String dbProduct,
                    final IDBSchemaUtilsDriver schemaDriver
            ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }

        String defaultValue = null;

        if(column.getValue(ColumnNodeType.defaultValueClass) != null)
        {
            final Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(Connection.class.getCanonicalName(), connection);

            final IDefaultValueExpressionDriver driver = Driver.getSingleDriver(column.getValue(ColumnNodeType.defaultValueClass), properties);
            Objects.requireNonNull(driver, "Extension Driver for " + column.getValue(ColumnNodeType.defaultValueClass).getCanonicalName() + " not found");
            defaultValue = driver.createExpression(column, connection, schemaName, properties, schemaDriver);

            properties.clear();
        }

        if(defaultValue != null)
        {
            return "DEFAULT " + defaultValue;
        }

        return "";
    }

}
