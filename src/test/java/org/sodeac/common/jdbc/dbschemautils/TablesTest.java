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
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.DBSchemaTreeModel;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.Statics;
import org.sodeac.common.jdbc.TestConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TablesTest
{
	public static final String SCHEMA_NAME = "SODEAC_TEST";
	private static final String DATABASE_ID = "TestDatabase";
	
	private EasyMockSupport support = new EasyMockSupport();
	
	public static List<Object[]> connectionList = null;
	public static final Map<String,Boolean> createdSchema = new HashMap<String,Boolean>();
	
	@Parameters
    public static List<Object[]> connections()
    {
    	if(connectionList != null)
    	{
    		return connectionList;
    	}
    	return connectionList = Statics.connections(createdSchema,"dbschema");
    }
	
	public TablesTest(Callable<TestConnection> connectionFactory)
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
		if(! this.testConnection.enabled)
		{
			return;
		}
		if(this.testConnection.connection != null)
		{
			try
			{
				this.testConnection.connection.close();
			}
			catch (Exception e) {}
		}
	}
	
	@Test
	public void test000101CreateTableUnquoted() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTable1";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
		
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
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000102CreateTableUnquotedAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTable1";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
				
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
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000111CreateTableQuoted() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTableQ1";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
				
		// prepare spec for simulation
		Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
		schemaDictionary.put(ObjectType.SCHEMA, schema);
		
		DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
		table1.setValue(TableNodeType.quotedName, true);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
		table1Dictionary.put(ObjectType.SCHEMA, schema);
		table1Dictionary.put(ObjectType.TABLE, table1);
		TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000112CreateTableQuotedAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTableQ1";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
				
		// prepare spec for simulation
		Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
		schemaDictionary.put(ObjectType.SCHEMA, schema);
		
		DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
		table1.setValue(TableNodeType.quotedName, true);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
		table1Dictionary.put(ObjectType.SCHEMA, schema);
		table1Dictionary.put(ObjectType.TABLE, table1);
		TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000121CreateTableUnquotedTableSpace() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTable1TS";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
		schema.setValue(DBSchemaNodeType.tableSpaceData,"sodeacdata");
		
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
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000122CreateTableUnquotedTableSpaceAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTable1TS";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
		schema.setValue(DBSchemaNodeType.tableSpaceData,"sodeacdata");
		
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
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000131CreateTableQuotedTableSpace() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTableQ1TS";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
		schema.setValue(DBSchemaNodeType.tableSpaceData,"sodeacdata");
		
		// prepare spec for simulation
		Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
		schemaDictionary.put(ObjectType.SCHEMA, schema);
					
		DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
						
		BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
		table1.setValue(TableNodeType.quotedName, true);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
		table1Dictionary.put(ObjectType.SCHEMA, schema);
		table1Dictionary.put(ObjectType.TABLE, table1);
		TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
								
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test000132CreateTableQuotedTableSpaceAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		IDBSchemaUtilsDriver driver = dbSchemaUtils.getDriver();
		
		String table1Name = "EmptyTableQ1TS";
		
		IMocksControl ctrl = support.createControl();
		IDatabaseSchemaUpdateListener updateListenerMock = ctrl.createMock(IDatabaseSchemaUpdateListener.class);
		
		ctrl.checkOrder(true);
		
		// create spec
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(DATABASE_ID,testConnection.dbmsSchemaName);
		schema.setValue(DBSchemaNodeType.tableSpaceData,"sodeacdata");
		
		// prepare spec for simulation
		Dictionary<ObjectType, Object> schemaDictionary = new Hashtable<>();
		schemaDictionary.put(ObjectType.SCHEMA, schema);
					
		DBSchemaNodeType.addConsumer(schema, new DatabaseSchemaUpdateListener(updateListenerMock));
						
		BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table1Name);
		table1.setValue(TableNodeType.quotedName, true);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table1Dictionary = new Hashtable<>();
		table1Dictionary.put(ObjectType.SCHEMA, schema);
		table1Dictionary.put(ObjectType.TABLE, table1);
		TableNodeType.addConsumer(table1, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, DATABASE_ID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, DATABASE_ID, table1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, DATABASE_ID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
}
