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
import static org.junit.Assert.assertTrue;

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
import java.util.concurrent.Callable;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ColumnTypeDecimalTest
{
	private EasyMockSupport support = new EasyMockSupport();
	
	public static List<Object[]> connectionList = null;
	public static final Map<String,Boolean> createdSchema = new HashMap<String,Boolean>();
		
	private String databaseID = "TESTDOMAIN";
	private String table1Name = "TableColDec";
	private String columnRealName = "col_real";
	private String columnDoubleName = "col_double";

	@Parameters
    public static List<Object[]> connections()
    {
    	if(connectionList != null)
    	{
    		return connectionList;
    	}
    	return connectionList = Statics.connections(createdSchema,"dbschema");
    }
	
	public ColumnTypeDecimalTest(Callable<TestConnection> connectionFactory)
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
	public void test000500real() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createRealColumn(table1, columnRealName, true);
		
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
			
			prepStat = connection.prepareStatement("select " + columnRealName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			assertFalse("rset should contains no more entries", rset.next());
			rset.close();
			prepStat.close();
			
			prepStat = connection.prepareStatement("insert into " +  table1Name + " (" + columnRealName + ") values (?)");
			prepStat.setFloat(1, 1.1f);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnRealName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 1.1f, rset.getFloat(1),0.01);
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
	public void test000501realAgain() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createRealColumn(table1, columnRealName, true);
		
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

			int count = 0;
			prepStat = connection.prepareStatement("select " + columnRealName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 1.1f, rset.getFloat(1),0.01);
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
	public void test000503RealUpdate() throws SQLException
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
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnRealName + " = ? ");
			prepStat.setFloat(1, 2.1f);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnRealName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 2.1f, rset.getFloat(1),0.01);
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
	public void test000510Double() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createDoubleColumn(table1, columnDoubleName, true);
		
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
			
			prepStat = connection.prepareStatement("select " + columnDoubleName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			assertTrue("rset should contains entries", rset.next());
			rset.close();
			prepStat.close();
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnDoubleName + " = ? ");
			prepStat.setDouble(1, 1111111111111111111111111111111111111111.13333d);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDoubleName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 1111111111111111111111111111111111111111.13333d, rset.getDouble(1),0.01);
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
	public void test000511DoubleAgain() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> column1 = TableNodeType.createDoubleColumn(table1, columnDoubleName, true);
		
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

			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDoubleName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 1111111111111111111111111111111111111111.13333d, rset.getDouble(1),0.01);
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
	public void test000513DoubleUpdate() throws SQLException
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
			connection.setAutoCommit(false);
			
			prepStat = connection.prepareStatement("update " +  table1Name + " set " + columnDoubleName + " = ? ");
			prepStat.setDouble(1, 2111111111111111111111111111111111111112.15555d);
			prepStat.executeUpdate();
			prepStat.close();
			connection.commit();
			
			int count = 0;
			prepStat = connection.prepareStatement("select " + columnDoubleName + " from " +  table1Name);
			rset = prepStat.executeQuery();
			while( rset.next())
			{
				count++;
				assertEquals("value should be correct", 2111111111111111111111111111111111111112.15555d, rset.getDouble(1),0.01);
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
}
