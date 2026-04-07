/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.sodeac.common.misc.Driver;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.CommonBaseBranchNodeType;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.ForeignKeyNodeType;
import org.sodeac.common.model.dbschema.IndexColumnNodeType;
import org.sodeac.common.model.dbschema.IndexNodeType;
import org.sodeac.common.model.dbschema.PrimaryKeyNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

public interface IDBSchemaUtilsDriver extends IDriver
{
    int HANDLE_NONE = -1;
    int HANDLE_FALLBACK = 0;
    int HANDLE_DEFAULT = 10000;
    
    String REQUIRED_DEFAULT_COLUMN = "SodeacDfltCol";
    
    /**
     * create a new schema
     *
     * @param connection underlying connection to create schema name
     * @param schemaName name of schema to create
     * @param properties further driver-specific informations
     *
     * @throws SQLException
     */
    default void createSchema(final Connection connection, final String schemaName, final Map<String, Object> properties) throws SQLException
    {
        String sql = "CREATE SCHEMA IF NOT EXISTS " + objectNameGuidelineFormat(null, connection, schemaName, "SCHEMA") + " AUTHORIZATION " + connection.getMetaData().getUserName();
        PreparedStatement prepStat = connection.prepareStatement(sql);
        prepStat.executeUpdate();
        prepStat.close();
    }
    
    /**
     * check the schema exists
     *
     * @param connection underlying connection to check the schema
     * @param schemaName name of schema to check
     *
     * @return true, if schema exists, otherwise false
     *
     * @throws SQLException
     */
    default boolean schemaExists(final Connection connection, final String schemaName) throws SQLException
    {
        boolean exist = false;
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet resultSet = meta.getSchemas();
        while (resultSet.next())
        {
            String tableSchema = resultSet.getString(1);        // TABLE_SCHEM String => schema name
            // String tableCatalog = resultSet.getString(2);	// TABLE_CATALOG String => catalog name (may be null)
            if (tableSchema.equalsIgnoreCase(schemaName))
            {
                exist = true;
                break;
            }
        }
        resultSet.close();
        
        return exist;
    }
    
    /**
     * drops a schema
     *
     * @param connection underlying connection to drop the schema
     * @param schemaName name of schema to drop
     * @param properties informations to confirm drop
     *
     * @throws SQLException
     */
    default void dropSchema(final Connection connection, final String schemaName, final Map<String, Object> properties) throws SQLException
    {
        if (!confirmDropSchema(connection, schemaName, properties))
        {
            throw new SQLException("you should confirm drop schema");
        }
        PreparedStatement prepStat = connection.prepareStatement("DROP SCHEMA " + objectNameGuidelineFormat(null, connection, schemaName, "SCHEMA") + " CASCADE ");
        prepStat.executeUpdate();
        prepStat.close();
        
    }
    
    static boolean confirmDropSchema(final Connection connection, final String schemaName, final Map<String, Object> properties) throws SQLException
    {
        if (properties == null)
        {
            throw new SQLException("no confirm informations to drop schema");
        }
        
        if (properties.get("YES_I_REALLY_WANT_DROP_SCHEMA_" + schemaName.toUpperCase()) == null)
        {
            throw new SQLException("no main confirmation to drop schema");
        }
        
        if (!((Boolean) properties.get("YES_I_REALLY_WANT_DROP_SCHEMA_" + schemaName.toUpperCase())).booleanValue())
        {
            throw new SQLException("no main confirmation to drop schema");
        }
        
        if (properties.get("OF_COURSE_I_HAVE_A_BACKUP_OF_ALL_IMPORTANT_RECORDS") == null)
        {
            throw new SQLException("no backup confirmation of all important records to drop schema");
        }
        
        if (!((Boolean) properties.get("OF_COURSE_I_HAVE_A_BACKUP_OF_ALL_IMPORTANT_RECORDS")).booleanValue())
        {
            throw new SQLException("no backup confirmation of all important records to drop schema");
        }
        return true;
    }
    
    /**
     * check existence of table by {@code tableSpec}.
     *
     * @param connection      underlying connection to check existence of table
     * @param schema          hole schema specification
     * @param table           table specification to check
     * @param tableProperties properties to store working parameter
     *
     * @return true, if table exists or false if table not exists
     *
     * @throws SQLException
     */
    default boolean tableExists
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final Map<String, Object> tableProperties
    ) throws SQLException
    {
        String catalog = connection.getCatalog();
        
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean quoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            quoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl;
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getTables
                                            (
                                                catalogSearchPattern(schema, connection, catalog),
                                                schemaSearchPattern(schema, connection, schemaName),
                                                objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), quoted, "TABLE"),
                                                new String[] { "TABLE" }
                                            );
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						in H2 the name of DB
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * TABLE_TYPE 						"TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
                 * REMARKS 							explanatory comment on the table
                 * TYPE_CAT
                 * TYPE_SCHEM
                 * TYPE_NAME
                 * SELF_REFERENCING_COL_NAME 		name of the designated "identifier" column of a typed table
                 * REF_GENERATION 					specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED"
                 */
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                if (quoted && tbl.equals(table.getValue(TableNodeType.name)))
                {
                    return true;
                }
                
                if ((!quoted) && tbl.equalsIgnoreCase(table.getValue(TableNodeType.name)))
                {
                    return true;
                }
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        try
        {
            // Try again with wildcard tablename
            
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getTables(null, null, "%", new String[] { "TABLE" });
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						in H2 the name of DB
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * TABLE_TYPE 						"TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
                 * REMARKS 							explanatory comment on the table
                 * TYPE_CAT
                 * TYPE_SCHEM
                 * TYPE_NAME
                 * SELF_REFERENCING_COL_NAME 		name of the designated "identifier" column of a typed table
                 * REF_GENERATION 					specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED"
                 */
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                if (quoted && tbl.equals(table.getValue(TableNodeType.name)))
                {
                    return true;
                }
                
                if ((!quoted) && tbl.equalsIgnoreCase(table.getValue(TableNodeType.name)))
                {
                    return true;
                }
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        return false;
    }
    
    /**
     *
     * create a table object in database
     *
     * @param connection      underlying connection to create table
     * @param schema          hole schema specification
     * @param table           table specification to create table
     * @param tableProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void createTable
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final Map<String, Object> tableProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean quoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            quoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        String defaultColumn = "";
        
        if (tableRequiresColumn())
        {
            defaultColumn = " " + objectNameGuidelineFormat(schema, connection, REQUIRED_DEFAULT_COLUMN, "COLUMN") + " char(1) NULL ";
        }
        
        String tableSpace = objectNameGuidelineFormat(schema, connection, schema.getValue(DBSchemaNodeType.tableSpaceData), "TABLESPACE");
        
        if ((table.getValue(TableNodeType.tableSpace) != null) && (!table.getValue(TableNodeType.tableSpace).isEmpty()))
        {
            tableSpace = objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.tableSpace), "TABLESPACE");
        }
        
        String tableSpaceDefinition = "";
        
        if ((tableSpace != null) && (!tableSpace.isEmpty()))
        {
            tableSpaceDefinition = tableSpaceAppendix(connection, schema, table, tableProperties, tableSpace, "TABLE");
        }
        
        PreparedStatement createTableStatement = null;
        try
        {
            String sql =
                quoted ?
                    "CREATE TABLE " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + "(" + defaultColumn + ")" + tableSpaceDefinition :
                    "CREATE TABLE " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + "(" + defaultColumn + ")" + tableSpaceDefinition;
            createTableStatement = connection.prepareStatement(sql);
            createTableStatement.executeUpdate();
        }
        finally
        {
            if (createTableStatement != null)
            {
                try
                {
                    createTableStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     *
     * check existence of tables primary key by {@code tableSpec}.
     *
     * @param connection      underlying connection to check existence of primary key
     * @param schema          hole schema specification
     * @param table           table specification to check
     * @param tableProperties properties to store working parameter
     *
     * @return true, if primary exists or false if primary key not exists
     *
     * @throws SQLException
     */
    default boolean primaryKeyExists
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final Map<String, Object> tableProperties
    ) throws SQLException
    {
        String catalog = connection.getCatalog();
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        BranchNode<?, ColumnNodeType> column = null;
        for (final BranchNode<TableNodeType, ColumnNodeType> columnPK : table.getUnmodifiableNodeList(TableNodeType.columns))
        {
            if (columnPK.get(ColumnNodeType.primaryKey) == null)
            {
                continue;
            }
            if (column != null)
            {
                throw new RuntimeException("Multible PKs not supported !!!");
            }
            column = columnPK;
        }
        
        if (column == null)
        {
            return true;
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl = null;
        String col = null;
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getPrimaryKeys
                                            (
                                                catalogSearchPattern(schema, connection, catalog),
                                                schemaSearchPattern(schema, connection, schemaName),
                                                objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), tableQuoted, "TABLE")
                                            );
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						UI / MASTER / POS .... (in H2 the name of DB )
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * COLUMN_NAME
                 * KEY_SEQ
                 * PK_NAME							CONSTRAINT-NAME
                 */
                
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                col = resultSet.getString("COLUMN_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (col == null)
                {
                    col = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                if (tableQuoted && (!tbl.equals(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                if ((!tableQuoted) && (!tbl.equalsIgnoreCase(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                
                if (columnQuoted && (!col.equals(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                if ((!columnQuoted) && (!col.equalsIgnoreCase(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                
                // Key name ignored
                
                return true;
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        return false;
    }
    
    /**
     * create primary key
     *
     * @param connection      underlying connection to create primary key
     * @param schema          hole schema specification
     * @param table           table specification to create primary key
     * @param tableProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void setPrimaryKey
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final Map<String, Object> tableProperties
    ) throws SQLException
    {
        IDBSchemaUtilsDriver.setPrimaryKeyWithoutIndex(connection, schema, table, tableProperties, this);
    }
    
    static void setPrimaryKeyWithIndex
        (
            final Connection connection,
            final BranchNode<?, DBSchemaNodeType> schema,
            final BranchNode<?, TableNodeType> table,
            final Map<String, Object> tableProperties,
            final IDBSchemaUtilsDriver dbSchemaUtilsDriver
        ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        BranchNode<?, ColumnNodeType> column = null;
        for (final BranchNode<TableNodeType, ColumnNodeType> columnPK : table.getUnmodifiableNodeList(TableNodeType.columns))
        {
            if (columnPK.get(ColumnNodeType.primaryKey) == null)
            {
                continue;
            }
            if (column != null)
            {
                throw new RuntimeException("Multible PKs not supported !!!");
            }
            column = columnPK;
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        BranchNode<ColumnNodeType, PrimaryKeyNodeType> primaryKey = column.get(ColumnNodeType.primaryKey);
        
        if (primaryKey == null)
        {
            return;
        }
        
        boolean nameQuoted = false;
        
        if (primaryKey.getValue(PrimaryKeyNodeType.quotedName) != null)
        {
            nameQuoted = primaryKey.getValue(PrimaryKeyNodeType.quotedName).booleanValue();
        }
        
        String constraintName = primaryKey.getValue(PrimaryKeyNodeType.constraintName);
        if ((constraintName == null) || constraintName.isEmpty())
        {
            constraintName = "PK_" + table.getValue(TableNodeType.name).toUpperCase();
        }
        
        if (nameQuoted)
        {
            constraintName = dbSchemaUtilsDriver.quotedChar() + constraintName + dbSchemaUtilsDriver.quotedChar();
        }
        else
        {
            constraintName = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, constraintName, "CONSTRAINT");
        }
        
        String indexName = primaryKey.getValue(PrimaryKeyNodeType.indexName);
        if ((indexName == null) || indexName.isEmpty())
        {
            indexName = "PKX_" + table.getValue(TableNodeType.name).toUpperCase();
        }
        
        if (nameQuoted)
        {
            indexName = dbSchemaUtilsDriver.quotedChar() + indexName + dbSchemaUtilsDriver.quotedChar();
        }
        else
        {
            indexName = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, indexName, "INDEX");
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + dbSchemaUtilsDriver.quotedChar() + table.getValue(TableNodeType.name) + dbSchemaUtilsDriver.quotedChar() + " " :
            " " + schemaName + "." + dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String columnPart = columnQuoted ?
            " " + dbSchemaUtilsDriver.quotedChar() + column.getValue(ColumnNodeType.name) + dbSchemaUtilsDriver.quotedChar() + " " :
            " " + dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, column.getValue(ColumnNodeType.name), "COLUMN") + " ";
        
        String tableSpace = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, schema.getValue(DBSchemaNodeType.tableSpaceIndex), "TABLESPACE");
        
        if ((primaryKey.getValue(PrimaryKeyNodeType.tableSpace) != null) && (!primaryKey.getValue(PrimaryKeyNodeType.tableSpace).isEmpty()))
        {
            tableSpace = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, primaryKey.getValue(PrimaryKeyNodeType.tableSpace), "TABLESPACE");
        }
        String tableSpaceDefinition = "";
        
        if ((tableSpace != null) && (!tableSpace.isEmpty()))
        {
            tableSpaceDefinition = dbSchemaUtilsDriver.tableSpaceAppendix(connection, schema, table, tableProperties, tableSpace, "PRIMARYKEY");
        }
        
        PreparedStatement createPKStatement = null;
        try
        {
            String createPK = "ALTER TABLE " + tablePart + " ADD CONSTRAINT " + constraintName + " PRIMARY KEY(" + columnPart + ") INDEX " + indexName + " " + tableSpaceDefinition;
            createPKStatement = connection.prepareStatement(createPK);
            createPKStatement.executeUpdate();
        }
        finally
        {
            if (createPKStatement != null)
            {
                try
                {
                    createPKStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    static void setPrimaryKeyWithoutIndex
        (
            final Connection connection,
            final BranchNode<?, DBSchemaNodeType> schema,
            final BranchNode<?, TableNodeType> table,
            final Map<String, Object> tableProperties,
            final IDBSchemaUtilsDriver dbSchemaUtilsDriver
        ) throws SQLException
    {
        
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        BranchNode<?, ColumnNodeType> column = null;
        for (final BranchNode<TableNodeType, ColumnNodeType> columnPK : table.getUnmodifiableNodeList(TableNodeType.columns))
        {
            if (columnPK.get(ColumnNodeType.primaryKey) == null)
            {
                continue;
            }
            if (column != null)
            {
                throw new RuntimeException("Multible PKs not supported !!!");
            }
            column = columnPK;
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        BranchNode<ColumnNodeType, PrimaryKeyNodeType> primaryKey = column.get(ColumnNodeType.primaryKey);
        
        if (primaryKey == null)
        {
            return;
        }
        
        boolean nameQuoted = false;
        
        if (primaryKey.getValue(PrimaryKeyNodeType.quotedName) != null)
        {
            nameQuoted = primaryKey.getValue(PrimaryKeyNodeType.quotedName).booleanValue();
        }
        
        String constraintName = primaryKey.getValue(PrimaryKeyNodeType.constraintName);
        if ((constraintName == null) || constraintName.isEmpty())
        {
            constraintName = "PK_" + table.getValue(TableNodeType.name).toUpperCase();
        }
        
        if (nameQuoted)
        {
            constraintName = dbSchemaUtilsDriver.quotedChar() + constraintName + dbSchemaUtilsDriver.quotedChar();
        }
        else
        {
            constraintName = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, constraintName, "CONSTRAINT");
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + dbSchemaUtilsDriver.quotedChar() + table.getValue(TableNodeType.name) + dbSchemaUtilsDriver.quotedChar() + " " :
            " " + schemaName + "." + dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String columnPart = columnQuoted ?
            " " + dbSchemaUtilsDriver.quotedChar() + column.getValue(ColumnNodeType.name) + dbSchemaUtilsDriver.quotedChar() + " " :
            " " + dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, column.getValue(ColumnNodeType.name), "COLUMN") + " ";
        
        String tableSpace = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, schema.getValue(DBSchemaNodeType.tableSpaceIndex), "TABLESPACE");
        
        if ((primaryKey.getValue(PrimaryKeyNodeType.tableSpace) != null) && (!primaryKey.getValue(PrimaryKeyNodeType.tableSpace).isEmpty()))
        {
            tableSpace = dbSchemaUtilsDriver.objectNameGuidelineFormat(schema, connection, primaryKey.getValue(PrimaryKeyNodeType.tableSpace), "TABLESPACE");
        }
        String tableSpaceDefinition = "";
        
        if ((tableSpace != null) && (!tableSpace.isEmpty()))
        {
            tableSpaceDefinition = dbSchemaUtilsDriver.tableSpaceAppendix(connection, schema, table, tableProperties, tableSpace, "PRIMARYKEY");
        }
        
        PreparedStatement createPKStatement = null;
        try
        {
            String createPK = "ALTER TABLE " + tablePart + " ADD CONSTRAINT " + constraintName + " PRIMARY KEY(" + columnPart + ") " + tableSpaceDefinition;
            createPKStatement = connection.prepareStatement(createPK);
            createPKStatement.executeUpdate();
        }
        finally
        {
            if (createPKStatement != null)
            {
                try
                {
                    createPKStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     *
     * check existence of column by {@code columnSpec}.
     *
     * @param connection       underlying connection to check existence of column
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to check
     * @param columnProperties properties to store working parameter
     *
     * @return true if column exists, otherwise false
     *
     * @throws SQLException
     */
    default boolean columnExists
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String catalog = connection.getCatalog();
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl = null;
        String col = null;
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getColumns
                                            (
                                                catalogSearchPattern(schema, connection, catalog),
                                                schemaSearchPattern(schema, connection, schemaName),
                                                objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), tableQuoted, "TABLE"),
                                                objectSearchPattern(schema, connection, column.getValue(ColumnNodeType.name), columnQuoted, "COLUMN")
                                            );
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						in H2 the name of DB
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * COLUMN_NAME 						column name
                 * DATA_TYPE 						SQL type from java.sql.Types (int)
                 * TYPE_NAME						SQL type - Name
                 * COLUMN_SIZE						size (int)
                 * DECIMAL_DIGITS					digits for float .... (int)
                 * NULLABLE 						nullable
                 
                 */
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                col = resultSet.getString("COLUMN_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (col == null)
                {
                    col = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                if (tableQuoted && (!tbl.equals(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                if ((!tableQuoted) && (!tbl.equalsIgnoreCase(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                
                if (columnQuoted && (!col.equals(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                if ((!columnQuoted) && (!col.equalsIgnoreCase(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                
                columnProperties.put("COLUMN_TABLE_CAT", cat);
                columnProperties.put("COLUMN_TABLE_SCHEM", schem);
                columnProperties.put("COLUMN_TABLE_NAME", tbl);
                
                columnProperties.put("COLUMN_COLUMN_NAME", col);
                columnProperties.put("COLUMN_DATA_TYPE", resultSet.getInt("DATA_TYPE"));
                columnProperties.put("COLUMN_TYPE_NAME", resultSet.getString("TYPE_NAME"));
                columnProperties.put("COLUMN_COLUMN_SIZE", resultSet.getInt("COLUMN_SIZE"));
                columnProperties.put("COLUMN_DECIMAL_DIGITS", resultSet.getInt("DECIMAL_DIGITS"));
                columnProperties.put("COLUMN_NULLABLE", resultSet.getInt("NULLABLE"));
                columnProperties.put("COLUMN_COLUMN_DEF", resultSet.getString("COLUMN_DEF"));
                
                return true;
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getColumns(null, null, "%", "%");
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						in H2 the name of DB
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * COLUMN_NAME 						column name
                 * DATA_TYPE 						SQL type from java.sql.Types (int)
                 * TYPE_NAME						SQL type - Name
                 * COLUMN_SIZE						size (int)
                 * DECIMAL_DIGITS					digits for float .... (int)
                 * NULLABLE 						nullable
                 
                 */
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                col = resultSet.getString("COLUMN_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (col == null)
                {
                    col = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                if (tableQuoted && (!tbl.equals(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                if ((!tableQuoted) && (!tbl.equalsIgnoreCase(table.getValue(TableNodeType.name))))
                {
                    continue;
                }
                
                if (columnQuoted && (!col.equals(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                if ((!columnQuoted) && (!col.equalsIgnoreCase(column.getValue(ColumnNodeType.name))))
                {
                    continue;
                }
                
                columnProperties.put("COLUMN_TABLE_CAT", cat);
                columnProperties.put("COLUMN_TABLE_SCHEM", schem);
                columnProperties.put("COLUMN_TABLE_NAME", tbl);
                
                columnProperties.put("COLUMN_COLUMN_NAME", col);
                columnProperties.put("COLUMN_DATA_TYPE", resultSet.getInt("DATA_TYPE"));
                columnProperties.put("COLUMN_TYPE_NAME", resultSet.getString("TYPE_NAME"));
                columnProperties.put("COLUMN_COLUMN_SIZE", resultSet.getInt("COLUMN_SIZE"));
                columnProperties.put("COLUMN_DECIMAL_DIGITS", resultSet.getInt("DECIMAL_DIGITS"));
                columnProperties.put("COLUMN_NULLABLE", resultSet.getInt("NULLABLE"));
                columnProperties.put("COLUMN_COLUMN_DEF", resultSet.getString("COLUMN_DEF"));
                
                return true;
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        return false;
    }
    
    /**
     *
     * determine type of column
     *
     * @param connection       underlying connection to check valid type of existing column
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to check
     * @param columnProperties properties with working parameter
     *
     * @return null, if driver can not determine type of column, otherwise type
     *
     * @throws SQLException
     */
    default String determineColumnType
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        if (columnProperties == null)
        {
            return null;
        }
        if (columnProperties.get("COLUMN_TYPE_NAME") == null)
        {
            return null;
        }
        for (final IColumnType.ColumnType type : IColumnType.ColumnType.values())
        {
            if (type.toString().equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
            {
                return type.toString();
            }
        }
        if ("bool".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.BOOLEAN.toString();
        }
        if ("text".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.CLOB.toString();
        }
        if ("int2".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.SMALLINT.toString();
        }
        if ("int4".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.INTEGER.toString();
        }
        if ("int8".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.BIGINT.toString();
        }
        if ("float4".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.REAL.toString();
        }
        if ("float8".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.DOUBLE.toString();
        }
        if ("varbinary".equalsIgnoreCase(columnProperties.get("COLUMN_TYPE_NAME").toString()))
        {
            return IColumnType.ColumnType.BINARY.toString();
        }
        return null;
    }
    
    /**
     * create column
     *
     * @param connection       connection underlying connection to create column
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to create
     * @param columnProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void createColumn
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String columnPart = columnQuoted ?
            " " + quotedChar() + column.getValue(ColumnNodeType.name) + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, column.getValue(ColumnNodeType.name), "COLUMN") + " ";
        
        PreparedStatement createColumnStatement = null;
        try
        {
            StringBuilder sqlBuilder = new StringBuilder("ALTER TABLE  " + tablePart + " ADD " + columnPart + " ");
            
            Map<String, Object> driverProperties = new HashMap<String, Object>();
            driverProperties.put(Connection.class.getCanonicalName(), connection);
            driverProperties.put("SCHEMA", schema);
            driverProperties.put("TABLE", table);
            driverProperties.put("COLUMN", column);
            
            String productName = connection.getMetaData().getDatabaseProductName();
            IColumnType columnType = Driver.getSingleDriver(IColumnType.class, driverProperties);
            
            if (columnType == null)
            {
                throw new SQLException(productName + ": no driver found for column type \"" + column.getValue(ColumnNodeType.columnType) + "\"");
            }
            
            sqlBuilder.append(" " + columnType.getTypeExpression(connection, schema, table, column, productName, this));
            sqlBuilder.append(" " + columnType.getDefaultValueExpression(connection, schema, table, column, productName, this));
            
            createColumnStatement = connection.prepareStatement(sqlBuilder.toString());
            createColumnStatement.executeUpdate();
            
        }
        finally
        {
            if (createColumnStatement != null)
            {
                try
                {
                    createColumnStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     * drop column
     *
     * @param connection underlying connection to drop column
     * @param schema     hole schema specification
     * @param table      table specification of columns table
     * @param columnName name of column to drop
     * @param quoted     use column name as quoted object name
     *
     * @throws SQLException
     */
    default void dropColumn
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final String columnName,
        final boolean quoted
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String columnPart = quoted ?
            " " + quotedChar() + columnName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, columnName, "COLUMN") + " ";
        
        PreparedStatement createColumnStatement = null;
        try
        {
            
            createColumnStatement = connection.prepareStatement("ALTER TABLE  " + tablePart + " DROP COLUMN " + columnPart + " ");
            createColumnStatement.executeUpdate();
            
        }
        finally
        {
            if (createColumnStatement != null)
            {
                try
                {
                    createColumnStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     * check column properties (default-value, type, nullable)
     *
     * @param connection       underlying connection to check column
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to check
     * @param columnProperties properties to store working parameter
     *
     * @return return true column setup is valid, otherwise false
     *
     * @throws SQLException
     */
    default boolean isValidColumnProperties
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        
        boolean valid = true;
        
        // Nullable
        
        boolean columnSpecNullable = column.getValue(ColumnNodeType.nullable) == null || column.getValue(ColumnNodeType.nullable).booleanValue();
        if (columnProperties.get("COLUMN_NULLABLE") != null) // exists
        {
            boolean nullable = ((Integer) columnProperties.get("COLUMN_NULLABLE")).intValue() > 0;
            if (nullable != columnSpecNullable)
            {
                valid = false;
                columnProperties.put("INVALID_NULLABLE", true);
            }
        }
        else // not exists (nullable is default)
        {
            if (!columnSpecNullable)
            {
                valid = false;
                columnProperties.put("INVALID_NULLABLE", true);
            }
        }
        
        // TYPE
        
        if (columnProperties.get("COLUMN_TYPE_NAME") != null) // exists
        {
            String type = determineColumnType(connection, schema, table, column, columnProperties);
            if (type != null)
            {
                if (!type.equalsIgnoreCase(column.getValue(ColumnNodeType.columnType)))
                {
                    valid = false;
                    columnProperties.put("INVALID_TYPE", true);
                }
            }
        }
        
        if (columnProperties.containsKey("COLUMN_COLUMN_DEF")) // exists
        {
            if (column.getValue(ColumnNodeType.defaultValueClass) == null)
            {
                if ((columnProperties.get("COLUMN_COLUMN_DEF") != null) && (!((String) columnProperties.get("COLUMN_COLUMN_DEF")).isEmpty()))
                {
                    if
                    (!
                        (
                            ((String) columnProperties.get("COLUMN_COLUMN_DEF")).equalsIgnoreCase("null") ||
                            ((String) columnProperties.get("COLUMN_COLUMN_DEF")).equalsIgnoreCase("null ")
                        )
                    )
                    {
                        valid = false;
                        columnProperties.put("INVALID_DEFAULT", true);
                    }
                }
            }
            else
            {
                Hashtable<String, Object> properties = new Hashtable<>();
                properties.put(Connection.class.getCanonicalName(), connection);
                
                IDefaultValueExpressionDriver driver = Driver.getSingleDriver(column.getValue(ColumnNodeType.defaultValueClass), properties);
                Objects.requireNonNull(driver, "Extension Driver for " + column.getValue(ColumnNodeType.defaultValueClass).getCanonicalName() + " not found");
                
                if (driver.updateRequired(column, connection, schemaName, properties, this, (String) columnProperties.get("COLUMN_COLUMN_DEF")))
                {
                    valid = false;
                    columnProperties.put("INVALID_DEFAULT", true);
                }
                properties.clear();
            }
        }
        
        if (columnProperties.get("COLUMN_COLUMN_SIZE") != null) // exists
        {
            int size = column.getValue(ColumnNodeType.size) == null ? 0 : column.getValue(ColumnNodeType.size).intValue();
            if
            (
                (
                    IColumnType.ColumnType.CHAR.toString().equals(column.getValue(ColumnNodeType.columnType))
                    ||
                    IColumnType.ColumnType.VARCHAR.toString().equals(column.getValue(ColumnNodeType.columnType))
                )
                &&
                size > 0
            )
            {
                if (((int) columnProperties.get("COLUMN_COLUMN_SIZE")) != size)
                {
                    valid = false;
                    columnProperties.put("INVALID_SIZE", true);
                }
            }
        }
        
        return valid;
    }
    
    /**
     * set valid column properties (default-value, type, nullable)
     *
     * @param connection       connection underlying connection to setup column
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification for setup
     * @param columnProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void setValidColumnProperties
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String columnPart = columnQuoted ?
            " " + quotedChar() + column.getValue(ColumnNodeType.name) + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, column.getValue(ColumnNodeType.name), "COLUMN") + " ";
        
        boolean nullable = column.getValue(ColumnNodeType.nullable) == null || column.getValue(ColumnNodeType.nullable).booleanValue();
        
        if (columnProperties.get("INVALID_NULLABLE") != null)
        {
            PreparedStatement updateNullableStatment = null;
            try
            {
                String nullableStatement = "ALTER TABLE  " + tablePart + " ALTER COLUMN " + columnPart + " SET " + (nullable ? "" : "NOT") + " NULL";
                updateNullableStatment = connection.prepareStatement(nullableStatement);
                updateNullableStatment.executeUpdate();
            }
            finally
            {
                if (updateNullableStatment != null)
                {
                    try
                    {
                        updateNullableStatment.close();
                    }
                    catch (final Exception e) { }
                }
            }
        }
        
        if
        (
            (columnProperties.get("INVALID_SIZE") != null) ||
            (columnProperties.get("INVALID_DEFAULT") != null) ||
            (columnProperties.get("INVALID_TYPE") != null)
        )
        {
            PreparedStatement createColumnStatement = null;
            try
            {
                StringBuilder sqlBuilder = new StringBuilder("ALTER TABLE  " + tablePart + " ALTER " + columnPart + " ");
                
                Map<String, Object> driverProperties = new HashMap<String, Object>();
                driverProperties.put(Connection.class.getCanonicalName(), connection);
                driverProperties.put("SCHEMA", schema);
                driverProperties.put("TABLE", table);
                driverProperties.put("COLUMN", column);
                
                String productName = connection.getMetaData().getDatabaseProductName();
                IColumnType columnType = Driver.getSingleDriver(IColumnType.class, driverProperties);
                
                if (columnType == null)
                {
                    throw new SQLException(productName + ": no driver found for column type \"" + column.getValue(ColumnNodeType.columnType) + "\"");
                }
                
                sqlBuilder.append(" " + columnType.getTypeExpression(connection, schema, table, column, productName, this));
                sqlBuilder.append(" " + columnType.getDefaultValueExpression(connection, schema, table, column, productName, this));
                
                createColumnStatement = connection.prepareStatement(sqlBuilder.toString());
                createColumnStatement.executeUpdate();
            }
            finally
            {
                if (createColumnStatement != null)
                {
                    try
                    {
                        createColumnStatement.close();
                    }
                    catch (final Exception e) { }
                }
            }
        }
        
        if
        (
            (columnProperties.get("INVALID_DEFAULT") != null) &&
            (column.getValue(ColumnNodeType.defaultValueClass) == null)
        )
        {
            PreparedStatement createColumnStatement = null;
            try
            {
                String sql = "ALTER TABLE  " + tablePart + " ALTER " + columnPart + " DROP DEFAULT ";
                
                createColumnStatement = connection.prepareStatement(sql);
                createColumnStatement.executeUpdate();
            }
            finally
            {
                if (createColumnStatement != null)
                {
                    try
                    {
                        createColumnStatement.close();
                    }
                    catch (final Exception e) { }
                }
            }
        }
    }
    
    /**
     *
     * check valid created foreign key
     *
     * @param connection       underlying connection to check foreign key
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to check foreign key
     * @param columnProperties properties to store working parameter
     *
     * @return true, if foreign key is created
     *
     * @throws SQLException
     */
    default boolean isValidForeignKey
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String catalog = connection.getCatalog();
        String schemaName = DBSchemaUtils.getSchema(connection);
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl = null;
        String col = null;
        String keyName = null;
        
        boolean keyQuoted = false;
        if ((column.get(ColumnNodeType.foreignKey) != null) && (column.get(ColumnNodeType.foreignKey).getValue(ForeignKeyNodeType.quotedKeyName) != null))
        {
            keyQuoted = column.get(ColumnNodeType.foreignKey).getValue(ForeignKeyNodeType.quotedKeyName).booleanValue();
        }
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getImportedKeys
                                            (
                                                catalogSearchPattern(schema, connection, catalog),
                                                schemaSearchPattern(schema, connection, schemaName),
                                                objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), tableQuoted, "TABLE")
                                            );
            while (resultSet.next())
            {
                /*
                 * PKTABLE_CAT String => primary key table catalog being imported (may be null)
                 * PKTABLE_SCHEM String => primary key table schema being imported (may be null)
                 * PKTABLE_NAME String => primary key table name being imported
                 * PKCOLUMN_NAME String => primary key column name being imported
                 * FKTABLE_CAT String => foreign key table catalog (may be null)
                 * FKTABLE_SCHEM String => foreign key table schema (may be null)
                 * FKTABLE_NAME String => foreign key table name
                 * FKCOLUMN_NAME String => foreign key column name
                 * KEY_SEQ short => sequence number within a foreign key( a value of 1 represents the first column of the foreign key, a value of 2 would represent the second column within the foreign key).
                 * UPDATE_RULE short => What happens to a foreign key when the primary key is updated:
                 * 		importedNoAction - do not allow update of primary key if it has been imported
                 * 		importedKeyCascade - change imported key to agree with primary key update
                 * 		importedKeySetNull - change imported key to NULL if its primary key has been updated
                 * 		importedKeySetDefault - change imported key to default values if its primary key has been updated
                 * 		importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
                 * DELETE_RULE short => What happens to the foreign key when primary is deleted.
                 * 		importedKeyNoAction - do not allow delete of primary key if it has been imported
                 * 		importedKeyCascade - delete rows that import a deleted key
                 * 		importedKeySetNull - change imported key to NULL if its primary key has been deleted
                 * 		importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
                 * 		importedKeySetDefault - change imported key to default if its primary key has been deleted
                 * FK_NAME String => foreign key name (may be null)
                 * PK_NAME String => primary key name (may be null)
                 * DEFERRABILITY short => can the evaluation of foreign key constraints be deferred until commit
                 * 		importedKeyInitiallyDeferred - see SQL92 for definition
                 * 		importedKeyInitiallyImmediate - see SQL92 for definition
                 * 		importedKeyNotDeferrable - see SQL92 for definition
                 *
                 * PKTABLE_CAT: 		SODEAC
                 * PKTABLE_SCHEM: 		PUBLIC
                 * PKTABLE_NAME: 		SODEAC_DOMAIN
                 * PKCOLUMN_NAME: 		ID
                 * FKTABLE_CAT: 		SODEAC
                 * FKTABLE_SCHEM: 		PUBLIC
                 * FKTABLE_NAME: 		SODEAC_USER
                 * FKCOLUMN_NAME: 		SODEAC_DOMAIN_ID
                 * KEY_SEQ: 			1
                 * UPDATE_RULE: 		1
                 * DELETE_RULE: 		1
                 * FK_NAME: 			CONSTRAINT_F
                 * PK_NAME: 			PRIMARY_KEY_4
                 * DEFERRABILITY:		7
                 */
                
                cat = resultSet.getString("FKTABLE_CAT");
                schem = resultSet.getString("FKTABLE_SCHEM");
                tbl = resultSet.getString("FKTABLE_NAME");
                col = resultSet.getString("FKCOLUMN_NAME");
                keyName = resultSet.getString("FK_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (keyName == null)
                {
                    keyName = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                
                boolean tableNameMatch = tableQuoted && tbl.equals(table.getValue(TableNodeType.name));
                if ((!tableQuoted) && tbl.equalsIgnoreCase(table.getValue(TableNodeType.name)))
                {
                    tableNameMatch = true;
                }
                
                boolean columnNameMatch = columnQuoted && col.equals(column.getValue(ColumnNodeType.name));
                if ((!columnQuoted) && col.equalsIgnoreCase(column.getValue(ColumnNodeType.name)))
                {
                    columnNameMatch = true;
                }
                
                BranchNode<ColumnNodeType, ForeignKeyNodeType> foreignKey = column.get(ColumnNodeType.foreignKey);
                
                if ((foreignKey == null) && tableNameMatch && columnNameMatch)
                {
                    return false;
                }
                
                if (foreignKey == null)
                {
                    continue;
                }
                
                boolean keyNameMatch = keyQuoted && (keyName.equals(foreignKey.getValue(ForeignKeyNodeType.constraintName) == null ? "" : foreignKey.getValue(ForeignKeyNodeType.constraintName)));
                if ((!keyQuoted) && (keyName.equalsIgnoreCase(foreignKey.getValue(ForeignKeyNodeType.constraintName) == null ? "" : foreignKey.getValue(ForeignKeyNodeType.constraintName))))
                {
                    keyNameMatch = true;
                }
                
                if (keyNameMatch && ((!columnNameMatch) || (!tableNameMatch)))
                {
                    columnProperties.put("CLEAN_FK", true);
                    return false;
                }
                
                if ((!columnNameMatch) || (!tableNameMatch))
                {
                    continue;
                }
                
                if (!keyNameMatch)
                {
                    continue;
                }
                
                String referencedTableName = foreignKey.getValue(ForeignKeyNodeType.referencedTableName);
                String referencedColumName = foreignKey.getValue(ForeignKeyNodeType.referencedColumnName);
                
                if ((referencedColumName == null) || referencedColumName.isEmpty())
                {
                    referencedColumName = CommonBaseBranchNodeType.id.getNodeName();
                }
                
                if
                (
                    resultSet.getString("PKTABLE_NAME").equalsIgnoreCase(referencedTableName) &&
                    resultSet.getString("PKCOLUMN_NAME").equalsIgnoreCase(referencedColumName)
                )
                {
                    return true;
                }
                columnProperties.put("CLEAN_FK", true);
                return false;
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        return column.get(ColumnNodeType.foreignKey) == null;
    }
    
    /**
     * create or update foreign key setting for {@code columnSpec}
     *
     * @param connection       underlying connection to set foreign key
     * @param schema           hole schema specification
     * @param table            table specification of columns table
     * @param column           column specification to set foreign key
     * @param columnProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void setValidForeignKey
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, ColumnNodeType> column,
        final Map<String, Object> columnProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        BranchNode<ColumnNodeType, ForeignKeyNodeType> foreignKey = column.get(ColumnNodeType.foreignKey);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        boolean keyQuoted = false;
        if ((foreignKey != null) && (foreignKey.getValue(ForeignKeyNodeType.quotedKeyName) != null))
        {
            keyQuoted = column.get(ColumnNodeType.foreignKey).getValue(ForeignKeyNodeType.quotedKeyName).booleanValue();
        }
        
        boolean refTableQuoted = false;
        if ((foreignKey != null) && (foreignKey.getValue(ForeignKeyNodeType.quotedRefTableName) != null))
        {
            refTableQuoted = column.get(ColumnNodeType.foreignKey).getValue(ForeignKeyNodeType.quotedRefTableName);
        }
        
        boolean refColumnQuoted = false;
        if ((foreignKey != null) && (foreignKey.getValue(ForeignKeyNodeType.quotedRefColumnName) != null))
        {
            refTableQuoted = column.get(ColumnNodeType.foreignKey).getValue(ForeignKeyNodeType.quotedRefColumnName);
        }
        
        if ((columnProperties.get("CLEAN_FK") != null) && ((Boolean) columnProperties.get("CLEAN_FK")).booleanValue() && (foreignKey != null))
        {
            dropForeignKey(connection, schema, table, foreignKey.getValue(ForeignKeyNodeType.constraintName), keyQuoted);
        }
        
        IDBSchemaUtilsDriver.cleanColumnForeignKeys(connection, schema, table, column, this);
        
        if (foreignKey == null)
        {
            return;
        }
        
        String tableName = table.getValue(TableNodeType.name);
        String columnName = column.getValue(ColumnNodeType.name);
        String constraintName = foreignKey.getValue(ForeignKeyNodeType.constraintName);
        String referencedTableName = foreignKey.getValue(ForeignKeyNodeType.referencedTableName);
        String referencedColumName = foreignKey.getValue(ForeignKeyNodeType.referencedColumnName);
        
        if ((referencedTableName == null) || referencedTableName.isEmpty())
        {
            throw new IllegalStateException("referenced table for " + tableName + "." + columnName + " not defined");
        }
        if ((referencedColumName == null) || referencedColumName.isEmpty())
        {
            referencedColumName = CommonBaseBranchNodeType.id.getNodeName();
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + tableName + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, tableName, "TABLE") + " ";
        
        String columnPart = columnQuoted ?
            " " + quotedChar() + columnName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, columnName, "COLUMN") + " ";
        
        String constraintPart = keyQuoted ?
            " " + quotedChar() + constraintName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, constraintName, "FOREIGNKEY") + " ";
        
        String refTablePart = refTableQuoted ?
            " " + schemaName + "." + quotedChar() + referencedTableName + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, referencedTableName, "TABLE") + " ";
        
        String refColumnPart = refColumnQuoted ?
            " " + quotedChar() + referencedColumName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, referencedColumName, "COLUMN") + " ";
        
        PreparedStatement createFKStatement = null;
        try
        {
            String createFK = "ALTER TABLE  " + tablePart + " ADD CONSTRAINT " + constraintPart + " FOREIGN KEY (" + columnPart + ") REFERENCES "
                              + refTablePart + " (" + refColumnPart + ") ";
            createFKStatement = connection.prepareStatement(createFK);
            createFKStatement.executeUpdate();
        }
        finally
        {
            if (createFKStatement != null)
            {
                try
                {
                    createFKStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    static void cleanColumnForeignKeys
        (
            final Connection connection,
            final BranchNode<?, DBSchemaNodeType> schema,
            final BranchNode<?, TableNodeType> table,
            final BranchNode<?, ColumnNodeType> column,
            final IDBSchemaUtilsDriver dbSchemaUtilsDriver
        )
        throws SQLException
    {
        String catalog = connection.getCatalog();
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean columnQuoted = false;
        if (column.getValue(ColumnNodeType.quotedName) != null)
        {
            columnQuoted = column.getValue(ColumnNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl = null;
        String col = null;
        String keyName = null;
        
        List<String> toDelete = new ArrayList<>();
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getImportedKeys
                                            (
                                                dbSchemaUtilsDriver.catalogSearchPattern(schema, connection, catalog),
                                                dbSchemaUtilsDriver.schemaSearchPattern(schema, connection, schemaName),
                                                dbSchemaUtilsDriver.objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), tableQuoted, "TABLE")
                                            );
            while (resultSet.next())
            {
                /*
                 * PKTABLE_CAT String => primary key table catalog being imported (may be null)
                 * PKTABLE_SCHEM String => primary key table schema being imported (may be null)
                 * PKTABLE_NAME String => primary key table name being imported
                 * PKCOLUMN_NAME String => primary key column name being imported
                 * FKTABLE_CAT String => foreign key table catalog (may be null)
                 * FKTABLE_SCHEM String => foreign key table schema (may be null)
                 * FKTABLE_NAME String => foreign key table name
                 * FKCOLUMN_NAME String => foreign key column name
                 * KEY_SEQ short => sequence number within a foreign key( a value of 1 represents the first column of the foreign key, a value of 2 would represent the second column within the foreign key).
                 * UPDATE_RULE short => What happens to a foreign key when the primary key is updated:
                 * 		importedNoAction - do not allow update of primary key if it has been imported
                 * 		importedKeyCascade - change imported key to agree with primary key update
                 * 		importedKeySetNull - change imported key to NULL if its primary key has been updated
                 * 		importedKeySetDefault - change imported key to default values if its primary key has been updated
                 * 		importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
                 * DELETE_RULE short => What happens to the foreign key when primary is deleted.
                 * 		importedKeyNoAction - do not allow delete of primary key if it has been imported
                 * 		importedKeyCascade - delete rows that import a deleted key
                 * 		importedKeySetNull - change imported key to NULL if its primary key has been deleted
                 * 		importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
                 * 		importedKeySetDefault - change imported key to default if its primary key has been deleted
                 * FK_NAME String => foreign key name (may be null)
                 * PK_NAME String => primary key name (may be null)
                 * DEFERRABILITY short => can the evaluation of foreign key constraints be deferred until commit
                 * 		importedKeyInitiallyDeferred - see SQL92 for definition
                 * 		importedKeyInitiallyImmediate - see SQL92 for definition
                 * 		importedKeyNotDeferrable - see SQL92 for definition
                 *
                 * PKTABLE_CAT: 		SODEAC
                 * PKTABLE_SCHEM: 		PUBLIC
                 * PKTABLE_NAME: 		SODEAC_DOMAIN
                 * PKCOLUMN_NAME: 		ID
                 * FKTABLE_CAT: 		SODEAC
                 * FKTABLE_SCHEM: 		PUBLIC
                 * FKTABLE_NAME: 		SODEAC_USER
                 * FKCOLUMN_NAME: 		SODEAC_DOMAIN_ID
                 * KEY_SEQ: 			1
                 * UPDATE_RULE: 		1
                 * DELETE_RULE: 		1
                 * FK_NAME: 			CONSTRAINT_F
                 * PK_NAME: 			PRIMARY_KEY_4
                 * DEFERRABILITY:		7
                 */
                
                cat = resultSet.getString("FKTABLE_CAT");
                schem = resultSet.getString("FKTABLE_SCHEM");
                tbl = resultSet.getString("FKTABLE_NAME");
                col = resultSet.getString("FKCOLUMN_NAME");
                keyName = resultSet.getString("FK_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (keyName == null)
                {
                    keyName = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog) || cat.equalsIgnoreCase(schemaName)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                
                boolean tableNameMatch = tableQuoted && tbl.equals(table.getValue(TableNodeType.name));
                if ((!tableQuoted) && tbl.equalsIgnoreCase(table.getValue(TableNodeType.name)))
                {
                    tableNameMatch = true;
                }
                
                boolean columnNameMatch = columnQuoted && col.equals(column.getValue(ColumnNodeType.name));
                if ((!columnQuoted) && col.equalsIgnoreCase(column.getValue(ColumnNodeType.name)))
                {
                    columnNameMatch = true;
                }
                
                if ((!columnNameMatch) || (!tableNameMatch))
                {
                    continue;
                }
                
                toDelete.add(keyName);
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        for (final String toDeleteKey : toDelete)
        {
            dbSchemaUtilsDriver.dropForeignKey(connection, schema, table, toDeleteKey, true);
        }
    }
    
    /**
     * drop foreign key
     *
     * @param connection underlying connection to drop foreign key
     * @param schema     hole schema specification
     * @param table      table specification of columns table
     * @param keyName    name of foreign key to drop
     * @param quoted     use key name as quoted object name
     *
     * @throws SQLException
     */
    default void dropForeignKey
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final String keyName,
        final boolean quoted
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String keyPart = quoted ?
            " " + quotedChar() + keyName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, keyName, "FOREIGNKEY") + " ";
        
        PreparedStatement createColumnStatement = null;
        try
        {
            createColumnStatement = connection.prepareStatement("ALTER TABLE  " + tablePart + " DROP CONSTRAINT " + keyPart + " ");
            createColumnStatement.executeUpdate();
            
        }
        finally
        {
            if (createColumnStatement != null)
            {
                try
                {
                    createColumnStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     * check valid index setup
     *
     * @param connection      underlying connection to check index
     * @param schema          hole schema specification
     * @param table           table specification of index
     * @param index           index specification
     * @param indexProperties properties to store working parameter
     *
     * @return true, if index has valid setup, otherwise false
     *
     * @throws SQLException
     */
    default boolean isValidIndex
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, IndexNodeType> index,
        final Map<String, Object> indexProperties
    ) throws SQLException
    {
        String catalog = connection.getCatalog();
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean indexQuoted = false;
        if (index.getValue(IndexNodeType.quotedName) != null)
        {
            indexQuoted = index.getValue(IndexNodeType.quotedName).booleanValue();
        }
        
        String cat = null;
        String schem = null;
        String tbl = null;
        String col = null;
        String idx = null;
        
        ResultSet resultSet = null;
        try
        {
            
            List<BranchNode<IndexNodeType, IndexColumnNodeType>> columnList = index.getUnmodifiableNodeList(IndexNodeType.members);
            
            if (columnList.isEmpty())
            {
                return false;
            }
            
            if (index.getValue(IndexNodeType.name) == null)
            {
                return false;
            }
            
            if (index.getValue(IndexNodeType.name).isEmpty())
            {
                return false;
            }
            
            Map<String, Short> columnExists = new HashMap<String, Short>();
            boolean unique = false;
            
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getIndexInfo
                                            (
                                                catalogSearchPattern(schema, connection, catalog),
                                                schemaSearchPattern(schema, connection, schemaName),
                                                objectSearchPattern(schema, connection, table.getValue(TableNodeType.name), tableQuoted, "TABLE"),
                                                false, false
                                            );
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT String => table catalog (may be null)
                 * TABLE_SCHEM String => table schema (may be null)
                 * TABLE_NAME String => table name
                 * NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
                 * INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
                 * INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
                 * TYPE short => index type:
                 * 		tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
                 * 		tableIndexClustered - this is a clustered index
                 * 		tableIndexHashed - this is a hashed index
                 * 		tableIndexOther - this is some other style of index
                 * ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
                 * COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
                 * ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
                 * CARDINALITY long => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
                 * PAGES long => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
                 * FILTER_CONDITION String => Filter condition, if any. (may be null)
                 *
                 * Key:
                 * 	TABLE_CAT: SODEAC
                 * 	TABLE_SCHEM: PUBLIC
                 * 	TABLE_NAME: SODEAC_USER
                 * 	NON_UNIQUE: false
                 * 	INDEX_QUALIFIER: SODEAC
                 * 	INDEX_NAME: IDX2
                 * 	TYPE: 3
                 * 	ORDINAL_POSITION: 1
                 * 	COLUMN_NAME: SODEAC_DOMAIN_ID
                 * 	ASC_OR_DESC: A
                 * 	CARDINALITY: 0
                 * 	PAGES : 0
                 * 	FILTER_CONDITION:
                 * Key:
                 * 	TABLE_CAT: SODEAC
                 * 	TABLE_SCHEM: PUBLIC
                 * 	TABLE_NAME: SODEAC_USER
                 * 	NON_UNIQUE: false
                 * 	INDEX_QUALIFIER: SODEAC
                 * 	INDEX_NAME: IDX2
                 * 	TYPE: 3
                 * 	ORDINAL_POSITION: 2
                 * 	COLUMN_NAME: LOGIN_NAME
                 * 	ASC_OR_DESC: A
                 * 	CARDINALITY: 0
                 * 	PAGES : 0
                 * 	FILTER_CONDITION:
                 */
                
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                col = resultSet.getString("COLUMN_NAME");
                idx = resultSet.getString("INDEX_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (cat == null)
                {
                    cat = "";
                }
                if (schem == null)
                {
                    schem = "";
                }
                if (tbl == null)
                {
                    tbl = "";
                }
                if (idx == null)
                {
                    idx = "";
                }
                
                if (!(cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase(catalog)))
                {
                    continue;
                }
                if (!schem.equalsIgnoreCase(schemaName))
                {
                    continue;
                }
                
                boolean tableNameMatch = tableQuoted && tbl.equals(table.getValue(TableNodeType.name));
                if ((!tableQuoted) && tbl.equalsIgnoreCase(table.getValue(TableNodeType.name)))
                {
                    tableNameMatch = true;
                }
                
                if (!tableNameMatch)
                {
                    continue;
                }
                
                boolean indexNameMatch = indexQuoted && (idx.equals(index.getValue(IndexNodeType.name)));
                if ((!indexQuoted) && (idx.equalsIgnoreCase(index.getValue(IndexNodeType.name))))
                {
                    indexNameMatch = true;
                }
                
                if (!indexNameMatch)
                {
                    continue;
                }
                
                unique = !resultSet.getBoolean("NON_UNIQUE");
                columnExists.put(col.toUpperCase(), resultSet.getShort("ORDINAL_POSITION"));
            }
            resultSet.close();
            
            boolean diff = false;
            boolean keyExists = !columnExists.isEmpty();
            
            boolean indexUnique = index.getValue(IndexNodeType.unique) != null && index.getValue(IndexNodeType.unique).booleanValue();
            if (unique != indexUnique)
            {
                diff = true;
            }
            else if ((columnExists.size() != columnList.size()))
            {
                diff = true;
            }
            else
            {
                for (final BranchNode<IndexNodeType, IndexColumnNodeType> column : columnList)
                {
                    if (!columnExists.containsKey(column.getValue(IndexColumnNodeType.columName).toUpperCase()))
                    {
                        diff = true;
                        break;
                    }
                }
            }
            
            if (keyExists && diff)
            {
                indexProperties.put("CLEAR_INDEX", true);
                
                keyExists = false;
            }
            return keyExists;
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     * setup valid index by {@code indexSpec}
     *
     * @param connection      underlying connection to setup index
     * @param schema          hole schema specification
     * @param table           table specification of index
     * @param index           index specification
     * @param indexProperties properties to store working parameter
     *
     * @throws SQLException
     */
    default void setValidIndex
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final BranchNode<?, IndexNodeType> index,
        final Map<String, Object> indexProperties
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        boolean tableQuoted = false;
        if (table.getValue(TableNodeType.quotedName) != null)
        {
            tableQuoted = table.getValue(TableNodeType.quotedName).booleanValue();
        }
        
        boolean indexQuoted = false;
        if (index.getValue(IndexNodeType.quotedName) != null)
        {
            indexQuoted = index.getValue(IndexNodeType.quotedName).booleanValue();
        }
        
        List<BranchNode<IndexNodeType, IndexColumnNodeType>> columnList = index.getUnmodifiableNodeList(IndexNodeType.members);
        
        if ((indexProperties.get("CLEAR_INDEX") != null) && ((Boolean) indexProperties.get("CLEAR_INDEX")).booleanValue())
        {
            dropIndex(connection, schema, table, index.getValue(IndexNodeType.name), index.getValue(IndexNodeType.quotedName) != null && index.getValue(IndexNodeType.quotedName).booleanValue());
        }
        
        String tablePart = tableQuoted ?
            " " + schemaName + "." + quotedChar() + table.getValue(TableNodeType.name) + quotedChar() + " " :
            " " + schemaName + "." + objectNameGuidelineFormat(schema, connection, table.getValue(TableNodeType.name), "TABLE") + " ";
        
        String indexPart = indexQuoted ?
            " " + quotedChar() + index.getValue(IndexNodeType.name) + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, index.getValue(IndexNodeType.name), "INDEX") + " ";
        
        String tableSpace = objectNameGuidelineFormat(schema, connection, schema.getValue(DBSchemaNodeType.tableSpaceIndex), "TABLESPACE");
        
        if ((index.getValue(IndexNodeType.tableSpace) != null) && (!index.getValue(IndexNodeType.tableSpace).isEmpty()))
        {
            tableSpace = objectNameGuidelineFormat(schema, connection, index.getValue(IndexNodeType.tableSpace), "TABLESPACE");
        }
        String tableSpaceDefinition = "";
        
        if ((tableSpace != null) && (!tableSpace.isEmpty()))
        {
            tableSpaceDefinition = tableSpaceAppendix(connection, schema, table, indexProperties, tableSpace, "INDEX");
        }
        
        boolean indexUnique = index.getValue(IndexNodeType.unique) != null && index.getValue(IndexNodeType.unique).booleanValue();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE ");
        if (indexUnique)
        {
            sqlBuilder.append("UNIQUE ");
        }
        sqlBuilder.append("INDEX ");
        sqlBuilder.append(indexPart + " ON ");
        sqlBuilder.append(tablePart + " (");
        String separator = "";
        for (final BranchNode<IndexNodeType, IndexColumnNodeType> column : columnList)
        {
            boolean columnQuoted = false;
            if (column.getValue(IndexColumnNodeType.quotedName) != null)
            {
                columnQuoted = column.getValue(IndexColumnNodeType.quotedName);
            }
            
            String columnPart = columnQuoted ?
                " " + quotedChar() + column.getValue(IndexColumnNodeType.columName) + quotedChar() + " " :
                " " + objectNameGuidelineFormat(schema, connection, column.getValue(IndexColumnNodeType.columName), "COLUMN") + " ";
            
            sqlBuilder.append(separator + columnPart);
            separator = ",";
        }
        sqlBuilder.append(") ");
        sqlBuilder.append(tableSpaceDefinition);
        
        PreparedStatement createIndexStatement = null;
        try
        {
            createIndexStatement = connection.prepareStatement(sqlBuilder.toString());
            createIndexStatement.executeUpdate();
        }
        finally
        {
            if (createIndexStatement != null)
            {
                try
                {
                    createIndexStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    /**
     * drop index with indexName
     *
     * @param connection underlying connection to drop index
     * @param schema     hole schema specification
     * @param table      table specification of index
     * @param indexName  name of index to drop
     * @param quoted     use index name as quoted object name
     *
     * @throws SQLException
     */
    default void dropIndex
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final String indexName,
        final boolean quoted
    ) throws SQLException
    {
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = table.getValue(TableNodeType.dbmsSchemaName);
        }
        
        String indexPart = quoted ?
            " " + quotedChar() + indexName + quotedChar() + " " :
            " " + objectNameGuidelineFormat(schema, connection, indexName, "INDEX") + " ";
        
        PreparedStatement createColumnStatement = null;
        try
        {
            createColumnStatement = connection.prepareStatement("DROP INDEX " + schemaName + "." + indexPart + " ");
            createColumnStatement.executeUpdate();
        }
        finally
        {
            if (createColumnStatement != null)
            {
                try
                {
                    createColumnStatement.close();
                }
                catch (final Exception e) { }
            }
        }
    }
    
    boolean isSequenceExists(String schema, String sequenceName, Connection connection) throws SQLException;
    
    void createSequence(String schema, String sequenceName, Connection connection, long min, long max, boolean cycle, Long cache) throws SQLException;
    
    void dropSquence(String schema, String sequenceName, Connection connection) throws SQLException;
    
    long nextFromSequence(String schema, String sequenceName, Connection connection) throws SQLException;
    
    /**
     * clean schema from columns created with table-objects by dbms can not create tables without columns
     *
     * @param connection underlying connection to clean from dummy columns
     * @param schema     hole schema specification
     *
     * @throws SQLException
     */
    default void dropDummyColumns(final Connection connection, final BranchNode<?, DBSchemaNodeType> schema) throws SQLException
    {
        Map<String, BranchNode<DBSchemaNodeType, TableNodeType>> tableIndex = new HashMap<String, BranchNode<DBSchemaNodeType, TableNodeType>>();
        Map<String, Map<String, String>> colIndex = new HashMap<String, Map<String, String>>();
        
        for (final BranchNode<DBSchemaNodeType, TableNodeType> table : schema.getUnmodifiableNodeList(DBSchemaNodeType.tables))
        {
            tableIndex.put(table.getValue(TableNodeType.name).toUpperCase(), table);
        }
        
        String schemaName = DBSchemaUtils.getSchema(connection);
        
        if ((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (!schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
        {
            schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
        }
        
        String tbl = null;
        String col = null;
        String cat = null;
        String schem = null;
        
        ResultSet resultSet = null;
        try
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            resultSet = databaseMetaData.getColumns(null, null, "%", "%");
            while (resultSet.next())
            {
                /*
                 * TABLE_CAT 						in H2 the name of DB
                 * TABLE_SCHEM 						INFORMATION_SCHEMA / PUBLIC
                 * TABLE_NAME
                 * COLUMN_NAME 						column name
                 * DATA_TYPE 						SQL type from java.sql.Types (int)
                 * TYPE_NAME						SQL type - Name
                 * COLUMN_SIZE						size (int)
                 * DECIMAL_DIGITS					digits for float .... (int)
                 * NULLABLE 						nullable
                 
                 */
                cat = resultSet.getString("TABLE_CAT");
                schem = resultSet.getString("TABLE_SCHEM");
                tbl = resultSet.getString("TABLE_NAME");
                col = resultSet.getString("COLUMN_NAME");
                
                if (schem == null)
                {
                    schem = cat;
                }
                
                if (tbl == null)
                {
                    tbl = "";
                }
                if (col == null)
                {
                    col = "";
                }
                
                if (!tableIndex.containsKey(tbl.toUpperCase()))
                {
                    continue;
                }
                
                BranchNode<DBSchemaNodeType, TableNodeType> table = tableIndex.get(tbl.toUpperCase());
                
                String tableSchemaName = schemaName;
                
                if ((table.getValue(TableNodeType.dbmsSchemaName) != null) && (!table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
                {
                    tableSchemaName = table.getValue(TableNodeType.dbmsSchemaName);
                }
                
                if (!schem.equalsIgnoreCase(tableSchemaName))
                {
                    continue;
                }
                
                Map<String, String> cols = colIndex.get(tbl);
                if (cols == null)
                {
                    cols = new HashMap<String, String>();
                    colIndex.put(tbl, cols);
                }
                cols.put(col, col);
            }
        }
        finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                }
                catch (final Exception e) { }
            }
        }
        
        for (final Entry<String, Map<String, String>> colEntry : colIndex.entrySet())
        {
            BranchNode<DBSchemaNodeType, TableNodeType> table = tableIndex.get(tbl.toUpperCase());
            
            String colName = null;
            for (final String columnName : colEntry.getValue().keySet())
            {
                if (columnName.equalsIgnoreCase("SODEACDFLTCOL"))
                {
                    colName = columnName;
                    break;
                }
            }
            
            if ((colName != null) && (colEntry.getValue().size() > 1))
            {
                dropColumn(connection, schema, table, colName, true);
            }
        }
    }
    
    /**
     * convert function name to  function syntax
     *
     * @param function name of function
     *
     * @return valid function syntax
     */
    default String getFunctionExpression(final String function)
    {
        if (function.trim().endsWith(")"))
        {
            return function;
        }
        return function + "()";
    }
    
    /**
     *
     *
     * @return true, of dbms requires a column on table creation, otherwise false
     */
    default boolean tableRequiresColumn()
    {
        return false;
    }
    
    /**
     *
     * @param schema     hole schema specification
     * @param connection underlying connection
     * @param catalog    catalog name
     *
     * @return catalog filter object for jdbc meta api
     */
    default String catalogSearchPattern(final BranchNode<?, DBSchemaNodeType> schema, final Connection connection, final String catalog)
    {
        return catalog;
    }
    
    /**
     *
     * @param schema     hole schema specification
     * @param connection underlying connection
     * @param schemaName schema name
     *
     * @return schema filter object for jdbc meta api
     */
    default String schemaSearchPattern(final BranchNode<?, DBSchemaNodeType> schema, final Connection connection, final String schemaName)
    {
        return objectNameGuidelineFormat(schema, connection, schemaName, "SCHEMA");
    }
    
    /**
     *
     * @param schema     hole schema specification
     * @param connection underlying connection
     * @param name       object's name
     * @param quoted     use name as quoted object name
     * @param type       object type (TABLE,COLUMN ...)
     *
     * @return filter object for jdbc meta api
     */
    default String objectSearchPattern(final BranchNode<?, DBSchemaNodeType> schema, final Connection connection, final String name, final boolean quoted, final String type)
    {
        return quoted ? name : objectNameGuidelineFormat(schema, connection, name, type);
    }
    
    /**
     * convert object name (e.g. table name, column name, key name ) to dbms conform name
     *
     * @param schema     hole schema specification
     * @param connection underlying connection
     * @param name       object's name
     * @param type       object type
     *
     * @return jdbc specific object name
     */
    default String objectNameGuidelineFormat(final BranchNode<?, DBSchemaNodeType> schema, final Connection connection, final String name, final String type)
    {
        return name;
    }
    
    /**
     *
     * @return character to quote an identifier
     */
    default char quotedChar()
    {
        return '"';
    }
    
    /**
     *
     *
     * @param connection
     * @param schema
     * @param table
     * @param properties
     * @param tableSpace
     * @param type
     *
     * @return
     */
    default String tableSpaceAppendix
    (
        final Connection connection,
        final BranchNode<?, DBSchemaNodeType> schema,
        final BranchNode<?, TableNodeType> table,
        final Map<String, Object> properties,
        final String tableSpace,
        final String type
    )
    {
        return "";
    }
}
