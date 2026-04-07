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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.sodeac.common.model.dbschema.ForeignKeyNodeType;
import org.sodeac.common.model.dbschema.IndexNodeType;
import org.sodeac.common.model.dbschema.PrimaryKeyNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeysTest
{
    private EasyMockSupport support = new EasyMockSupport();
    
    public static List<Object[]> connectionList = null;
    public static final Map<String, Boolean> createdSchema = new HashMap<String, Boolean>();
    
    private String databaseID = "TESTDOMAIN";
    private String table1Name = "TableKeys1";
    private String table2Name = "TableKeys2";
    
    private String columnIdName = "id";
    private String columnFKName = "fk";
    private String columnFK2Name = "fk2";
    private String columnIdx1Name = "idx1";
    private String columnIdx2Name = "idx2";
    private String columnIdx3Name = "idx3";
    private String columnIdx4Name = "idx4";
    
    @Parameters
    public static List<Object[]> connections()
    {
        if (connectionList != null)
        {
            return connectionList;
        }
        return connectionList = Statics.connections(createdSchema, "dbschema");
    }
    
    public KeysTest(Callable<TestConnection> connectionFactory)
    {
        this.testConnectionFactory = connectionFactory;
    }
    
    Callable<TestConnection> testConnectionFactory = null;
    TestConnection testConnection = null;
    
    @Before
    public void setUp() throws Exception
    {
        this.testConnection = testConnectionFactory.call();
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
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001000primaryKey() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdName, false, 36);
        column1.create(ColumnNodeType.primaryKey);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        // table1 column properties
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // table1 create keys/indices
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001001primaryKeyAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdName, false, 36);
        column1.create(ColumnNodeType.primaryKey);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001002PrimaryKeyInsertSuccess() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
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
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test00103PrimaryKeyInsertFailure() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            String uuid = UUID.randomUUID().toString();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
        }
        catch (Exception e)
        {
            connection.rollback();
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
        
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test001010primaryKeyTS() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdName, false, 36);
        column1.create(ColumnNodeType.primaryKey).setValue(PrimaryKeyNodeType.tableSpace, "sodeacindex");
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        // table1 column properties
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // table1 create keys/indices
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001011primaryKeyAgainTS() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdName, false, 36);
        column1.create(ColumnNodeType.primaryKey).setValue(PrimaryKeyNodeType.tableSpace, "sodeacindex");
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001012PrimaryKeyInsertSuccessTS() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table2Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
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
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001013PrimaryKeyInsertFailureTS() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            String uuid = UUID.randomUUID().toString();
            
            prepStat = connection.prepareStatement("insert into " + table2Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table2Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
        }
        catch (Exception e)
        {
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
        
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test001020foreignKey() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFKName, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001021foreignKeyAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFKName, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001022foreignKeyInsertFailure() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnFKName + ")  values (?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, UUID.randomUUID().toString());
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
        }
        catch (Exception e)
        {
            connection.rollback();
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
        
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test001023foreignKeyInsertSuccess() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            String uuid = UUID.randomUUID().toString();
            
            prepStat = connection.prepareStatement("insert into " + table2Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnFKName + ")  values (?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, uuid);
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
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001024foreignKeyWithUsedName() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFK2Name, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001025foreignKeyInsertFailure() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnFK2Name + ")  values (?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, UUID.randomUUID().toString());
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
        }
        catch (Exception e)
        {
            connection.rollback();
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
        
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test001026foreignKey2InsertSuccess() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            String uuid = UUID.randomUUID().toString();
            
            prepStat = connection.prepareStatement("insert into " + table2Name + "  (" + columnIdName + ")  values (?) ");
            prepStat.setString(1, uuid);
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnFK2Name + ")  values (?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, uuid);
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
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001027ReForeignKey() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFKName, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_re_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001028DropForeignKey() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFK2Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001029ForeignKey2InsertSuccessAfterDrop() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnFK2Name + ")  values (?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, UUID.randomUUID().toString());
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
        }
        finally
        {
            connection.rollback();
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001040index() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdx1Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, ColumnNodeType> column2 = TableNodeType.createCharColumn(table1, columnIdx2Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column2Dictionary = new Hashtable<>();
        table1Column2Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column2Dictionary.put(ObjectType.TABLE, table1);
        table1Column2Dictionary.put(ObjectType.COLUMN, column2);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, false, "idx1_" + this.table1Name, columnIdx1Name, columnIdx2Name);
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.PRE, connection, databaseID, table1index1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.POST, connection, databaseID, table1index1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001041indexAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdx1Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, ColumnNodeType> column2 = TableNodeType.createCharColumn(table1, columnIdx2Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column2Dictionary = new Hashtable<>();
        table1Column2Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column2Dictionary.put(ObjectType.TABLE, table1);
        table1Column2Dictionary.put(ObjectType.COLUMN, column2);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, false, "idx1_" + this.table1Name, columnIdx1Name, columnIdx2Name);
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001043UniqueIndex() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdx3Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, ColumnNodeType> column2 = TableNodeType.createCharColumn(table1, columnIdx4Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column2Dictionary = new Hashtable<>();
        table1Column2Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column2Dictionary.put(ObjectType.TABLE, table1);
        table1Column2Dictionary.put(ObjectType.COLUMN, column2);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, true, "idx2_" + this.table1Name, columnIdx3Name, columnIdx4Name);
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.PRE, connection, databaseID, table1index1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.POST, connection, databaseID, table1index1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001044uniqueIndexAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnIdx3Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, ColumnNodeType> column2 = TableNodeType.createCharColumn(table1, columnIdx4Name, true, 36);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column2Dictionary = new Hashtable<>();
        table1Column2Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column2Dictionary.put(ObjectType.TABLE, table1);
        table1Column2Dictionary.put(ObjectType.COLUMN, column2);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, true, "idx2_" + this.table1Name, columnIdx3Name, columnIdx4Name);
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column2Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column2Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001045IndiziesInsertSuccess() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx1Name + "," + columnIdx2Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "a");
            prepStat.setString(3, "b");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx1Name + "," + columnIdx2Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "a");
            prepStat.setString(3, "b");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx3Name + "," + columnIdx4Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "a");
            prepStat.setString(3, "b");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx3Name + "," + columnIdx4Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "a");
            prepStat.setString(3, "c");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx3Name + "," + columnIdx4Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "b");
            prepStat.setString(3, "c");
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
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test001046IndiziesInsertFailure() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        Connection connection = testConnection.connection;
        
        PreparedStatement prepStat = null;
        ResultSet rset = null;
        try
        {
            
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + table1Name + "  (" + columnIdName + "," + columnIdx3Name + "," + columnIdx4Name + ")  values (?,?,?) ");
            prepStat.setString(1, UUID.randomUUID().toString());
            prepStat.setString(2, "b");
            prepStat.setString(3, "c");
            prepStat.executeUpdate();
            prepStat.close();
            connection.commit();
            
        }
        catch (Exception e)
        {
            connection.rollback();
            return;
        }
        finally
        {
            try
            {
                rset.close();
            }
            catch (Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (Exception e) { }
        }
        
        fail("Expected an SQLException to be thrown");
    }
    
    @Test
    public void test001047IndexWithTablespace() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFKName, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_re_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, false, "idx3_" + table1Name, columnFKName).setValue(IndexNodeType.tableSpace, "sodeacindex");
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.PRE, connection, databaseID, table1index1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.POST, connection, databaseID, table1index1Dictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
    
    @Test
    public void test001047IndexWithTablespaceAgain() throws SQLException, ClassNotFoundException, IOException
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        Connection connection = testConnection.connection;
        DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
        
        IMocksControl ctrl = support.createControl();
        IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
        
        ctrl.checkOrder(true);
        
        // create spec
        BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID, testConnection.dbmsSchemaName);
        
        // prepare spec for simulation
        Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
        schemaDictionary.put(ObjectType.SCHEMA, schema);
        DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
        
        // prepare table for simulation
        Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
        table1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Dictionary.put(ObjectType.TABLE, table1);
        TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
        
        BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createCharColumn(table1, columnFKName, true, 36);
        column1.create(ColumnNodeType.foreignKey)
               .setValue(ForeignKeyNodeType.constraintName, "fk1_re_xxx")
               .setValue(ForeignKeyNodeType.referencedTableName, table2Name)
               .setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
        
        // prepare column for simulation
        
        Dictionary<ObjectType, Object> table1Column1Dictionary = new Hashtable<>();
        table1Column1Dictionary.put(ObjectType.SCHEMA, schema);
        table1Column1Dictionary.put(ObjectType.TABLE, table1);
        table1Column1Dictionary.put(ObjectType.COLUMN, column1);
        
        BranchNode<TableNodeType, IndexNodeType> index1 = TableNodeType.createIndex(table1, false, "idx3_" + table1Name, columnFKName).setValue(IndexNodeType.tableSpace, "sodeacindex");
        
        Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
        table1index1Dictionary.put(ObjectType.SCHEMA, schema);
        table1index1Dictionary.put(ObjectType.TABLE, table1);
        table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1);
        
        // simulate listener
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        
        // table creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
        
        // table1 column creation
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
        
        // convert schema
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
        
        ctrl.replay();
        
        dbSchemaUtils.adaptSchema(schema);
        
        ctrl.verify();
        
    }
}
