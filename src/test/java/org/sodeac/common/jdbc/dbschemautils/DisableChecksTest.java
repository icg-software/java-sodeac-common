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
import org.sodeac.common.model.dbschema.ForeignKeyNodeType;
import org.sodeac.common.model.dbschema.IndexNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

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

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DisableChecksTest
{
	private EasyMockSupport support = new EasyMockSupport();
	
	public static List<Object[]> connectionList = null;
	public static final Map<String,Boolean> createdSchema = new HashMap<String,Boolean>();
	
	private String databaseID = "TESTDOMAIN";
	private String table1Name = "TableDisableCheck1";
	private String table2Name = "TableDisableCheck2";
	
	private String columnIdName = "id";
	private String columnFKName = "fk";
	private String columnUniqueName = "unq1";

	@Parameters
    public static List<Object[]> connections()
    {
    	if(connectionList != null)
    	{
    		return connectionList;
    	}
    	return connectionList = Statics.connections(createdSchema,"dbschema");
    }
	
	public DisableChecksTest(Callable<TestConnection> connectionFactory)
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
	public void test001200generateWithDisabledChecks() throws SQLException, ClassNotFoundException, IOException 
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
		schema.setValue(DBSchemaNodeType.skipChecks, true);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> columnPK = TableNodeType.createCharColumn(table1, columnIdName, false,36);
		columnPK.create(ColumnNodeType.primaryKey);
		
		Dictionary<ObjectType, Object> table1ColumnPKDictionary = new Hashtable<>();
		table1ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnPKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnPKDictionary.put(ObjectType.COLUMN, columnPK);
		
		BranchNode<TableNodeType, ColumnNodeType> columnFKTable1 = TableNodeType.createCharColumn(table1, columnFKName, true,36);
		columnFKTable1.create(ColumnNodeType.foreignKey)
			.setValue(ForeignKeyNodeType.constraintName, "fk1_tbl_dis_check")
			.setValue(ForeignKeyNodeType.referencedTableName, table2Name)
			.setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnFKDictionary = new Hashtable<>();
		table1ColumnFKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnFKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnFKDictionary.put(ObjectType.COLUMN, columnFKTable1);
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable1 = TableNodeType.createCharColumn(table1, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnUnqDictionary = new Hashtable<>();
		table1ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnUnqDictionary.put(ObjectType.TABLE, table1);
		table1ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable1);
		
		TableNodeType.createIndex(table1, true, "unq1_tbl1_dis_check", columnUniqueName);
		
		BranchNode<DBSchemaNodeType, TableNodeType> table2 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table2Dictionary = new Hashtable<>();
		table2Dictionary.put(ObjectType.SCHEMA, schema);
		table2Dictionary.put(ObjectType.TABLE, table2);
		TableNodeType.addConsumer(table2, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<TableNodeType, ColumnNodeType> columnPkTable2 = TableNodeType.createCharColumn(table2, columnIdName, false,36);
		columnPkTable2.create(ColumnNodeType.primaryKey);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnPKDictionary = new Hashtable<>();
		table2ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnPKDictionary.put(ObjectType.TABLE, table2);
		table2ColumnPKDictionary.put(ObjectType.COLUMN, columnPkTable2);
		
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable2 = TableNodeType.createCharColumn(table2, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnUnqDictionary = new Hashtable<>();
		table2ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnUnqDictionary.put(ObjectType.TABLE, table2);
		table2ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable2);
		
		TableNodeType.createIndex(table2, true, "unq1_tbl2_dis_check", columnUniqueName);
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
		
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table2Dictionary,driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table2Dictionary, driver, null);
		
		// table1 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		
		// table2 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnUnqDictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test001201generateWithDisabledChecksAgain() throws SQLException, ClassNotFoundException, IOException 
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
		schema.setValue(DBSchemaNodeType.skipChecks, true);
		
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
		
		BranchNode<TableNodeType, ColumnNodeType> columnPK = TableNodeType.createCharColumn(table1, columnIdName, false,36);
		columnPK.create(ColumnNodeType.primaryKey);
		
		Dictionary<ObjectType, Object> table1ColumnPKDictionary = new Hashtable<>();
		table1ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnPKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnPKDictionary.put(ObjectType.COLUMN, columnPK);
		
		BranchNode<TableNodeType, ColumnNodeType> columnFKTable1 = TableNodeType.createCharColumn(table1, columnFKName, true,36);
		columnFKTable1.create(ColumnNodeType.foreignKey)
			.setValue(ForeignKeyNodeType.constraintName, "fk1_tbl_dis_check")
			.setValue(ForeignKeyNodeType.referencedTableName, table2Name)
			.setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnFKDictionary = new Hashtable<>();
		table1ColumnFKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnFKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnFKDictionary.put(ObjectType.COLUMN, columnFKTable1);
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable1 = TableNodeType.createCharColumn(table1, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnUnqDictionary = new Hashtable<>();
		table1ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnUnqDictionary.put(ObjectType.TABLE, table1);
		table1ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable1);
		
		TableNodeType.createIndex(table1, true, "unq1_tbl1_dis_check", columnUniqueName);
		
		BranchNode<DBSchemaNodeType, TableNodeType> table2 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table2Dictionary = new Hashtable<>();
		table2Dictionary.put(ObjectType.SCHEMA, schema);
		table2Dictionary.put(ObjectType.TABLE, table2);
		TableNodeType.addConsumer(table2, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<TableNodeType, ColumnNodeType> columnPkTable2 = TableNodeType.createCharColumn(table2, columnIdName, false,36);
		columnPkTable2.create(ColumnNodeType.primaryKey);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnPKDictionary = new Hashtable<>();
		table2ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnPKDictionary.put(ObjectType.TABLE, table2);
		table2ColumnPKDictionary.put(ObjectType.COLUMN, columnPkTable2);
		
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable2 = TableNodeType.createCharColumn(table2, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnUnqDictionary = new Hashtable<>();
		table2ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnUnqDictionary.put(ObjectType.TABLE, table2);
		table2ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable2);
		
		TableNodeType.createIndex(table2, true, "unq1_tbl2_dis_check", columnUniqueName);
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
		
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table2Dictionary, driver, null);
		
		// table1 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		
		// table2 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnUnqDictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test001250generateWithEnabledChecks() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> columnPK = TableNodeType.createCharColumn(table1, columnIdName, false,36);
		columnPK.create(ColumnNodeType.primaryKey);
		
		Dictionary<ObjectType, Object> table1ColumnPKDictionary = new Hashtable<>();
		table1ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnPKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnPKDictionary.put(ObjectType.COLUMN, columnPK);
		
		BranchNode<TableNodeType, ColumnNodeType> columnFKTable1 = TableNodeType.createCharColumn(table1, columnFKName, true,36);
		columnFKTable1.create(ColumnNodeType.foreignKey)
			.setValue(ForeignKeyNodeType.constraintName, "fk1_tbl_dis_check")
			.setValue(ForeignKeyNodeType.referencedTableName, table2Name)
			.setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnFKDictionary = new Hashtable<>();
		table1ColumnFKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnFKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnFKDictionary.put(ObjectType.COLUMN, columnFKTable1);
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable1 = TableNodeType.createCharColumn(table1, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnUnqDictionary = new Hashtable<>();
		table1ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnUnqDictionary.put(ObjectType.TABLE, table1);
		table1ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable1);
		
		BranchNode<TableNodeType, IndexNodeType> index1Table1 = TableNodeType.createIndex(table1, true, "unq1_tbl1_dis_check", columnUniqueName);
		
		Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
		table1index1Dictionary.put(ObjectType.SCHEMA, schema);
		table1index1Dictionary.put(ObjectType.TABLE, table1);
		table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1Table1);
		
		BranchNode<DBSchemaNodeType, TableNodeType> table2 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table2Dictionary = new Hashtable<>();
		table2Dictionary.put(ObjectType.SCHEMA, schema);
		table2Dictionary.put(ObjectType.TABLE, table2);
		TableNodeType.addConsumer(table2, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<TableNodeType, ColumnNodeType> columnPkTable2 = TableNodeType.createCharColumn(table2, columnIdName, false,36);
		columnPkTable2.create(ColumnNodeType.primaryKey);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnPKDictionary = new Hashtable<>();
		table2ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnPKDictionary.put(ObjectType.TABLE, table2);
		table2ColumnPKDictionary.put(ObjectType.COLUMN, columnPkTable2);
		
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable2 = TableNodeType.createCharColumn(table2, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnUnqDictionary = new Hashtable<>();
		table2ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnUnqDictionary.put(ObjectType.TABLE, table2);
		table2ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable2);
		
		BranchNode<TableNodeType,IndexNodeType> index1Table2 = TableNodeType.createIndex(table2, true, "unq1_tbl2_dis_check", columnUniqueName);
		
		Dictionary<ObjectType, Object> table2index1Dictionary = new Hashtable<>();
		table2index1Dictionary.put(ObjectType.SCHEMA, schema);
		table2index1Dictionary.put(ObjectType.TABLE, table2);
		table2index1Dictionary.put(ObjectType.TABLE_INDEX, index1Table2);
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
		
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table2Dictionary, driver, null);
		
		// table1 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		
		// table2 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnUnqDictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);

		// table1 column properties
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		
		// table2 column properties
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_NULLABLE, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
					
		// table1 create keys/indices
					
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.PRE, connection, databaseID, table1index1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.POST, connection, databaseID, table1index1Dictionary, driver, null);
		
		// table2 create keys/indices
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_PRIMARY_KEY, PhaseType.POST, connection, databaseID, table2Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.PRE, connection, databaseID, table2index1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.TABLE_INDEX, PhaseType.POST, connection, databaseID, table2index1Dictionary, driver, null);
		
		// table1 column foreign keys
		
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.COLUMN_FOREIGN_KEY, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
		
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
	
	@Test
	public void test001251generateWithEnabledChecksAgain() throws SQLException, ClassNotFoundException, IOException 
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
		
		BranchNode<TableNodeType, ColumnNodeType> columnPK = TableNodeType.createCharColumn(table1, columnIdName, false,36);
		columnPK.create(ColumnNodeType.primaryKey);
		
		Dictionary<ObjectType, Object> table1ColumnPKDictionary = new Hashtable<>();
		table1ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnPKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnPKDictionary.put(ObjectType.COLUMN, columnPK);
		
		BranchNode<TableNodeType, ColumnNodeType> columnFKTable1 = TableNodeType.createCharColumn(table1, columnFKName, true,36);
		columnFKTable1.create(ColumnNodeType.foreignKey)
			.setValue(ForeignKeyNodeType.constraintName, "fk1_tbl_dis_check")
			.setValue(ForeignKeyNodeType.referencedTableName, table2Name)
			.setValue(ForeignKeyNodeType.referencedColumnName, columnIdName);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnFKDictionary = new Hashtable<>();
		table1ColumnFKDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnFKDictionary.put(ObjectType.TABLE, table1);
		table1ColumnFKDictionary.put(ObjectType.COLUMN, columnFKTable1);
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable1 = TableNodeType.createCharColumn(table1, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table1ColumnUnqDictionary = new Hashtable<>();
		table1ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table1ColumnUnqDictionary.put(ObjectType.TABLE, table1);
		table1ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable1);
		
		BranchNode<TableNodeType, IndexNodeType> index1Table1 = TableNodeType.createIndex(table1, true, "unq1_tbl1_dis_check", columnUniqueName);
		
		Dictionary<ObjectType, Object> table1index1Dictionary = new Hashtable<>();
		table1index1Dictionary.put(ObjectType.SCHEMA, schema);
		table1index1Dictionary.put(ObjectType.TABLE, table1);
		table1index1Dictionary.put(ObjectType.TABLE_INDEX, index1Table1);
		
		BranchNode<DBSchemaNodeType, TableNodeType> table2 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, table2Name);
		
		// prepare table for simulation
		Dictionary<ObjectType, Object> table2Dictionary = new Hashtable<>();
		table2Dictionary.put(ObjectType.SCHEMA, schema);
		table2Dictionary.put(ObjectType.TABLE, table2);
		TableNodeType.addConsumer(table2, new DatabaseSchemaUpdateListener(updateListenerMock));
		
		BranchNode<TableNodeType, ColumnNodeType> columnPkTable2 = TableNodeType.createCharColumn(table2, columnIdName, false,36);
		columnPkTable2.create(ColumnNodeType.primaryKey);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnPKDictionary = new Hashtable<>();
		table2ColumnPKDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnPKDictionary.put(ObjectType.TABLE, table2);
		table2ColumnPKDictionary.put(ObjectType.COLUMN, columnPkTable2);
		
		
		BranchNode<TableNodeType, ColumnNodeType> columnUnqTable2 = TableNodeType.createCharColumn(table2, columnUniqueName, true,36);
		
		// prepare column for simulation
		
		Dictionary<ObjectType, Object> table2ColumnUnqDictionary = new Hashtable<>();
		table2ColumnUnqDictionary.put(ObjectType.SCHEMA, schema);
		table2ColumnUnqDictionary.put(ObjectType.TABLE, table2);
		table2ColumnUnqDictionary.put(ObjectType.COLUMN, columnUnqTable2);
		
		BranchNode<TableNodeType,IndexNodeType> index1Table2 = TableNodeType.createIndex(table2, true, "unq1_tbl2_dis_check", columnUniqueName);
		
		Dictionary<ObjectType, Object> table2index1Dictionary = new Hashtable<>();
		table2index1Dictionary.put(ObjectType.SCHEMA, schema);
		table2index1Dictionary.put(ObjectType.TABLE, table2);
		table2index1Dictionary.put(ObjectType.TABLE_INDEX, index1Table2);
		
		// simulate listener
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver, null);
		
		// table creation
				
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table1Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table1Dictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.PRE, connection, databaseID, table2Dictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.TABLE, PhaseType.POST, connection, databaseID, table2Dictionary, driver, null);
		
		// table1 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnFKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnFKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table1ColumnUnqDictionary, driver, null);
		
		// table2 column creation
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnPKDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnPKDictionary, driver, null);
		
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.PRE, connection, databaseID, table2ColumnUnqDictionary, driver, null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.COLUMN, PhaseType.POST, connection, databaseID, table2ColumnUnqDictionary, driver, null);
					
		// convert schema
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.PRE, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.UPDATE, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA_CONVERT_SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver,  null);
					
		updateListenerMock.onAction(ActionType.CHECK, ObjectType.SCHEMA, PhaseType.POST, connection, databaseID, schemaDictionary, driver, null);
				
		ctrl.replay();
				
		dbSchemaUtils.adaptSchema(schema);
		
		ctrl.verify();
	}
}
