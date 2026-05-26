/*******************************************************************************
 * Copyright (c) 2017, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.jdbc.dbschemautils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.jdbc.DBSchemaUtils.ActionType;
import org.sodeac.common.jdbc.DBSchemaUtils.ObjectType;
import org.sodeac.common.jdbc.DBSchemaUtils.PhaseType;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.Statics;
import org.sodeac.common.jdbc.TestConnection;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.DBSchemaTreeModel;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ColumnTypeTextTest
{
    private final EasyMockSupport support = new EasyMockSupport();
    
    public static List<Object[]> connectionList = null;
    public static final Map<String, Boolean> createdSchema = new HashMap<String, Boolean>();
    
    private final String databaseID = "TESTDOMAIN";
    private final String table1Name = "TableColChar";
    private final String columnCharName = "column_char";
    private final String columnVarcharName = "column_varchar";
    private final String columnUUIDName = "column_uuid";
    private final String columnClobName = "column_clob";
    private static final UUID uuid = UUID.randomUUID();
    
    @Parameters
    public static List<Object[]> connections()
    {
        if (connectionList != null)
        {
            return connectionList;
        }
        return connectionList = Statics.connections(createdSchema, "dbschema");
    }
    
    public ColumnTypeTextTest(final Callable<TestConnection> connectionFactory)
    {
        this.testConnectionFactory = connectionFactory;
    }
    
    Callable<TestConnection> testConnectionFactory = null;
    TestConnection testConnection = null;
    
    @Before
    public void setUp() throws Exception
    {
        this.testConnection = this.testConnectionFactory.call();
    }
    
    @After
    public void tearDown()
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        if (this.testConnection.connection != null)
        {
            try
            {
                this.testConnection.connection.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000300char() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, this.columnCharName, true, 100);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("select " + this.columnCharName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            assertFalse("rset should contains no more entries", rset.next());
            rset.close();
            prepStat.close();
            
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (" + this.columnCharName + ") values (?)");
            prepStat.setString(1, "valueforcolumn");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnCharName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "valueforcolumn", rset.getString(1).trim());
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000301charAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, this.columnCharName, true, 100);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnCharName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "valueforcolumn", rset.getString(1).trim());
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000303charToLong() throws SQLException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (" + this.columnCharName + ") values (?)");
            prepStat.setString(1, "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
        }
        catch (final SQLException e)
        {
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test000310varchar() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createVarcharColumn(table1, this.columnVarcharName, true, 100);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("update " + this.table1Name + " set " + this.columnVarcharName + " = ? ");
            prepStat.setString(1, "value2forcolumn");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnVarcharName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "value2forcolumn", rset.getString(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000311varcharAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createVarcharColumn(table1, this.columnVarcharName, true, 100);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnVarcharName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "value2forcolumn", rset.getString(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000313varcharToLong() throws SQLException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (" + this.columnVarcharName + ") values (?)");
            prepStat.setString(1, "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
        }
        catch (final SQLException e)
        {
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test000320clob() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createClobColumn(table1, this.columnClobName, true);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("update " + this.table1Name + " set " + this.columnClobName + " = ? ");
            prepStat.setString(1, "valuelobforcolumn");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnClobName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "valuelobforcolumn", rset.getString(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000321clobAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createClobColumn(table1, this.columnClobName, true);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnClobName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", "valuelobforcolumn", rset.getString(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000323clobLong() throws SQLException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (" + this.columnClobName + ") values (?)");
            prepStat.setString
                        (
                            1,
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn" +
                            "valueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumnvalueforcolumn"
                        );
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000340uuid() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createUUIDColumn(table1, this.columnUUIDName, true);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("update " + this.table1Name + " set " + this.columnUUIDName + " = ? ");
            prepStat.setObject(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnUUIDName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", uuid, rset.getObject(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 2, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000341uuidAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = this.support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createUUIDColumn(table1, this.columnUUIDName, true);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, this.databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, this.databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, this.databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, this.databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, this.databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnUUIDName + " from " + this.table1Name);
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", uuid, rset.getObject(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 2, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000342uuidAuto() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        Connection connection = this.testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, "TableColUUID");
        
        TableNodeType.createUUIDColumnDefaultAuto(table1, this.columnUUIDName);
        TableNodeType.createVarcharColumn(table1, "col1", false, 12);
        
        dbSchemaUtils.adaptSchema(schema);
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into TableColUUID (col1) values ('val1')", Statement.RETURN_GENERATED_KEYS);
            prepStat.executeUpdate();
            
            ResultSet generatedKeys = prepStat.getGeneratedKeys();
            generatedKeys.next();
            UUID newID = UUID.fromString(generatedKeys.getString(1));
            generatedKeys.close();
            prepStat.close();
            connection.commit();
            
            int count = 0;
            prepStat = connection.prepareStatement("select " + this.columnUUIDName + " from TableColUUID ");
            rset = prepStat.executeQuery();
            while (rset.next())
            {
                count++;
                assertEquals("value should be correct", newID, rset.getObject(1));
            }
            rset.close();
            prepStat.close();
            
            assertEquals("rset should contains correct counts of entries", 1, count);
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
}
