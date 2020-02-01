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
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.Statics;
import org.sodeac.common.jdbc.TestConnection;
import org.sodeac.common.jdbc.DBSchemaUtils.ActionType;
import org.sodeac.common.jdbc.DBSchemaUtils.ObjectType;
import org.sodeac.common.jdbc.DBSchemaUtils.PhaseType;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.DBSchemaTreeModel;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;


@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ColumnTypeTimeTest
{
	private EasyMockSupport support = new EasyMockSupport();
	
	public static List<Object[]> connectionList = null;
	public static final Map<String,Boolean> createdSchema = new HashMap<String,Boolean>();
	
	private String databaseID = "TESTDOMAIN";
	private String table1Name = "TableColTime";
	private String columnTimeName = "col_time";
	private String columnDateName = "col_date";
	private String columnTimestampName = "col_ts";
	
	private String tableDfltName = "TableColDflt";
	private String columnDfltTimeName = "col_time";
	private String columnDfltDateName = "col_date";
	private String columnDfltTimestampName = "col_ts";

	@Parameters
    public static List<Object[]> connections()
    {
    	if(connectionList != null)
    	{
    		return connectionList;
    	}
    	return connectionList = Statics.connections(createdSchema,"dbschema");
    }
	
	public ColumnTypeTimeTest(Callable<TestConnection> connectionFactory)
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
	public void test000600time() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createTimeColumn(table1, columnTimeName, true);
		
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
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		// table1 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1Column1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1Column1Dictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("select " + columnTimeName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			assertFalse("rset should contains no more entries", rset.next());
			rset.close();
			prepStat.close();
			
			Time time = new Time(0,12,00);
			
			prepStat = connection.prepareStatement("insert into " +  table1Name + " (" + columnTimeName + ") values (?)");
			prepStat.setTime(1, time);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimeName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", time.toString(), rset.getTime(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000601timeAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createTimeColumn(table1, columnTimeName, true);
		
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
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			Time time = new Time(0,12,00);

			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimeName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", time.toString(), rset.getTime(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000603TimeUpdate() throws SQLException
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			Time time = new Time(0,13,00);
			
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnTimeName + " = ? ");
			prepStat.setTime(1, time);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimeName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", time.toString(), rset.getTime(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
			
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000610date() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createDateColumn(table1, columnDateName, true);
		
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
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			connection.setAutoCommit(false);
			
			java.sql.Date date = new java.sql.Date(118,0,1); 
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnDateName + " = ?");
			prepStat.setDate(1, date);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDateName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getDate(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000611dateAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createDateColumn(table1, columnDateName, true);
		
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
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			java.sql.Date date = new java.sql.Date(118,0,1); 

			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDateName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getDate(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000613DateUpdate() throws SQLException
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			java.sql.Date date = new java.sql.Date(117,0,1); 
			
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnDateName + " = ? ");
			prepStat.setDate(1, date);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDateName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getDate(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
			
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000620timestamp() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createTimestampColumn(table1, columnTimestampName, true);
		
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
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			connection.setAutoCommit(false);
			
			java.sql.Timestamp date = new java.sql.Timestamp(118,0,1,12,0,0,0); 
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnTimestampName + " = ?");
			prepStat.setTimestamp(1, date);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimestampName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getTimestamp(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000621TimestampAgain() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
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
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createTimestampColumn(table1, columnTimestampName, true);
		
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
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			java.sql.Timestamp date = new java.sql.Timestamp(118,0,1,12,0,0,0); 

			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimestampName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getTimestamp(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000623TimestampUpdate() throws SQLException
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			java.sql.Timestamp date = new java.sql.Timestamp(115,0,1,17,0,0,0); 
			
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnTimestampName + " = ? ");
			prepStat.setTimestamp(1, date);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnTimestampName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", date.toString(), rset.getTimestamp(1).toString());
			}
			rset.close();
			prepStat.close();
			
			assertEquals("rset should contains correct counts of entries", 1, count);
			
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
	
	@Test
	public void test000630timedefaults() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
		
		BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(databaseID,testConnection.dbmsSchemaName);
		BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, tableDfltName);
		
		
		TableNodeType.createCharColumn(table1, "id" , false, 36);
		TableNodeType.createTimeColumnDefaultCurrent(table1, columnDfltTimeName);
		TableNodeType.createDateColumnDefaultCurrent(table1, columnDfltDateName);
		TableNodeType.createTimestampColumnDefaultCurrent(table1, columnDfltTimestampName);
		
		dbSchemaUtils.adaptSchema(schema);
		
		PreparedStatement prepStat = null;
		ResultSet rset = null;
		try
		{
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("insert into " +  tableDfltName + " (id) values (?)");
			prepStat.setString(1, UUID.randomUUID().toString());
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
		}
		finally 
		{
			try {rset.close();}catch (Exception e) {}
			try {prepStat.close();}catch (Exception e) {}
		}
	}
}
