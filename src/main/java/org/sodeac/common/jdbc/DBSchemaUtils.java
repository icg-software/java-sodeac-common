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
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

import org.sodeac.common.ILogService;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.impl.LogServiceImpl;
import org.sodeac.common.misc.Driver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
//import org.sodeac.common.model.dbschema.DBSchemaBow;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.EventConsumerNodeType;
import org.sodeac.common.model.dbschema.IndexNodeType;
import org.sodeac.common.model.dbschema.SequenceNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

public class DBSchemaUtils
{
	private DBSchemaUtils()
	{
		super();
	}
	
	private Connection connection = null;
	private IDBSchemaUtilsDriver driver = null;
	private ILogService logService = null;
	
	public static DBSchemaUtils get(Connection connection)
	{
		Map<String,Object> properties = new HashMap<String, Object>();
		properties.put(Connection.class.getCanonicalName(), connection);
		IDBSchemaUtilsDriver driver = Driver.getSingleDriver(IDBSchemaUtilsDriver.class, properties);
		Objects.requireNonNull(driver, "No DBUtils-driver found for " + connection);
		DBSchemaUtils dbUtil = new DBSchemaUtils();
		dbUtil.connection = connection;
		dbUtil.driver = driver;
		dbUtil.logService = ILogService.newLogService(DBSchemaUtils.class).addLoggerBackend(new LogServiceImpl.SystemLogger(DBSchemaUtils.class));
		
		return dbUtil;
	}

	public IDBSchemaUtilsDriver getDriver()
	{
		return driver;
	}
	
//	@SuppressWarnings("unchecked")
//	public boolean adaptSchema(DBSchemaBow<?> schema) throws SQLException
//	{
//		Objects.requireNonNull(schema, "schema == null");
//		return adaptSchema(schema.getWrappedBranchNode());
//	}
	
	public boolean adaptSchema(BranchNode<?, DBSchemaNodeType> schema) throws SQLException
	{
		
		if(schema == null)
		{
			return false;
		}
		
		CheckProperties checkProperties = new CheckProperties();
		String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
		
		DBSchemaEvent event = new DBSchemaEvent();
		event.setConnection(connection);
		event.setDriver(driver);
		event.setSchemaSpecificationName(schemaSpecificationName);
		
		if(! schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers).isEmpty())
		{
			Dictionary<ObjectType, Object> objects = new Hashtable<>();
			objects.put(ObjectType.SCHEMA, schema);
			
			event.setObjects(objects);
			event.setActionType(ActionType.CHECK);
			event.setObjectType(ObjectType.SCHEMA);
			event.setPhaseType(PhaseType.PRE);
			event.setException(null);
			
			for(BranchNode<DBSchemaNodeType, EventConsumerNodeType> consumerNode : schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers))
			{
				ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
				if(consumer == null)
				{
					continue;
				}
				try
				{
					consumer.acceptWithException(event);
				}
				catch (Exception e) 
				{
					this.logError(e, schema, "Error on UpdateListener.Schema.Check.Pre " + event.getSchemaSpecificationName(),checkProperties);
				}
			}
			event.setObjects(null);
			event.setActionType(null);
			event.setObjectType(null);
			event.setPhaseType(null);
			event.setException(null);
		}
		
		List<TableTracker> tableTrackerList = new ArrayList<TableTracker>();
		
		// create sequences
		
		for(BranchNode<DBSchemaNodeType, TableNodeType> table : schema.getUnmodifiableNodeList(DBSchemaNodeType.tables))
		{
			try
			{
				for(BranchNode<TableNodeType,ColumnNodeType> column : table.getUnmodifiableNodeList(TableNodeType.columns))
				{
					BranchNode<ColumnNodeType, SequenceNodeType> sequence = column.get(ColumnNodeType.sequence);
					if(sequence != null)
					{
						try
						{
							String schemaName = DBSchemaUtils.getSchema(connection);
							if((schema.getValue(DBSchemaNodeType.dbmsSchemaName) != null) && (! schema.getValue(DBSchemaNodeType.dbmsSchemaName).isEmpty()))
							{
								schemaName = schema.getValue(DBSchemaNodeType.dbmsSchemaName);
							}
							if((table.getValue(TableNodeType.dbmsSchemaName) != null) && (! table.getValue(TableNodeType.dbmsSchemaName).isEmpty()))
							{
								schemaName = table.getValue(TableNodeType.dbmsSchemaName);
							}
							
							String sequenceName = sequence.getValue(SequenceNodeType.name);
							if((sequenceName == null) || sequenceName.isEmpty())
							{
								sequenceName = driver.objectNameGuidelineFormat(schema, connection, "seq_" + table.getValue(TableNodeType.name) + "_" + column.getValue(ColumnNodeType.name), "SEQUENCE") ;
							}
							if(! driver.isSequenceExists(schemaName, sequenceName, connection))
							{
								Long min = sequence.getValue(SequenceNodeType.min); if(min == null) {min = 1L;}
								Long max = sequence.getValue(SequenceNodeType.max); if(max == null) {max = Long.MAX_VALUE;}
								Boolean cycle = sequence.getValue(SequenceNodeType.cycle); if(cycle == null) {cycle = false;}
								
								driver.createSequence(schemaName, sequenceName, connection, min, max, cycle, sequence.getValue(SequenceNodeType.cache));
							}
						}
						catch (Exception e) 
						{
							this.logError
							(
								e, 
								schema, 
								"Error on checkSchema / sequence " 
										+ table.getValue(TableNodeType.name) 
										+ "." + column.getValue(ColumnNodeType.name) 
										+ "." + sequence.getValue(SequenceNodeType.name), 
								checkProperties
							);
						}
					}
				}
			}
			catch (Exception e) 
			{
				this.logError(e, schema, "Error on checkSchema / section sequences", checkProperties);
			}
			if(checkProperties.isInterrupted())
			{
				return checkProperties.getUnusableExceptionList().isEmpty();
			}
		}
		
		// create tables
		
		for(BranchNode<DBSchemaNodeType, TableNodeType> table : schema.getUnmodifiableNodeList(DBSchemaNodeType.tables))
		{
			try
			{
				tableTrackerList.add(TableProcessor.checkTableDefinition(this, connection, driver, schema, table, checkProperties));
			}
			catch (Exception e) 
			{
				this.logError(e, schema, "Error on checkSchema " + schemaSpecificationName, checkProperties);
			}
			if(checkProperties.isInterrupted())
			{
				return checkProperties.getUnusableExceptionList().isEmpty();
			}
		}
		
		
		// create columns
		
		for(TableTracker tableTracker : tableTrackerList)
		{
			if(tableTracker.isExits())
			{
				for(BranchNode<TableNodeType, ColumnNodeType> column : tableTracker.getTable().getUnmodifiableNodeList(TableNodeType.columns))
				{
					tableTracker.getColumnTrackerList().add(ColumnProcessor.checkColumnDefinition
					(
						this, connection, driver, schema, tableTracker.getTable(), column, tableTracker.getTableProperties(), checkProperties
					));
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
			}
		}
		
		// schema convert phase
		
		try
		{
			if(! schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers).isEmpty())
			{
				Dictionary<ObjectType, Object> objects = new Hashtable<>();
				objects.put(ObjectType.SCHEMA, schema);
				
				event.setObjects(objects);
				event.setActionType(ActionType.CHECK);
				event.setObjectType(ObjectType.SCHEMA_CONVERT_SCHEMA);
				event.setPhaseType(PhaseType.PRE);
				event.setException(null);
				
				for(BranchNode<DBSchemaNodeType, EventConsumerNodeType> consumerNode : schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers))
				{
					ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
					if(consumer == null)
					{
						continue;
					}
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						logSQLException(e);
					}
					catch (Exception e) 
					{
						this.logError(e, schema, "Convert Schema " + schemaSpecificationName + " Error on UpdateListener.Schema.Check.Pre ", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
				
				
				
				for(BranchNode<DBSchemaNodeType, EventConsumerNodeType> consumerNode : schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers))
				{
					event.setObjects(objects);
					event.setActionType(ActionType.UPDATE);
					event.setObjectType(ObjectType.SCHEMA_CONVERT_SCHEMA);
					event.setPhaseType(PhaseType.PRE);
					event.setException(null);
					
					ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
					if(consumer == null)
					{
						continue;
					}
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						logSQLException(e);
					}
					catch (Exception e) 
					{
						this.logError(e, schema, "Convert Schema " + schemaSpecificationName + " Error on UpdateListener.Schema.Update.Pre ", checkProperties);
					}
					
					event.setObjects(objects);
					event.setActionType(ActionType.UPDATE);
					event.setObjectType(ObjectType.SCHEMA_CONVERT_SCHEMA);
					event.setPhaseType(PhaseType.POST);
					event.setException(null);
					
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						logSQLException(e);
					}
					catch (Exception e) 
					{
						this.logError(e, schema, "Convert Schema " + schemaSpecificationName + " Error on UpdateListener.Schema.Update.Post ", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
				
				event.setObjects(objects);
				event.setActionType(ActionType.CHECK);
				event.setObjectType(ObjectType.SCHEMA_CONVERT_SCHEMA);
				event.setPhaseType(PhaseType.POST);
				event.setException(null);
				
				for(BranchNode<DBSchemaNodeType, EventConsumerNodeType> consumerNode : schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers))
				{
					ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
					if(consumer == null)
					{
						continue;
					}
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						logSQLException(e);
					}
					catch (Exception e) 
					{
						this.logError(e, schema, "Convert Schema " + schemaSpecificationName + " Error on UpdateListener.Schema.Check.Post ", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
				
				event.setObjects(null);
				event.setActionType(null);
				event.setObjectType(null);
				event.setPhaseType(null);
				event.setException(null);
			}
		}
		catch(Exception e)
		{
			this.logError(e, schema, "Error on schema ConvertPhase " + schemaSpecificationName, checkProperties);
		}
		
		if(checkProperties.isInterrupted())
		{
			return checkProperties.getUnusableExceptionList().isEmpty();
		}
			
		try
		{
			SQLWarning warning = connection.getWarnings();
			if(warning != null)
			{
				logSQLException(warning);
			}
			connection.clearWarnings();
		}
		catch(SQLException e)
		{
			logSQLException(e);
			try
			{
				connection.clearWarnings();
			}
			catch(Exception e2){}
		}
		
		// column properties
		
		boolean skipChecks = schema.getValue(DBSchemaNodeType.skipChecks) == null ? false : schema.getValue(DBSchemaNodeType.skipChecks).booleanValue();
		
		for(TableTracker tableTracker : tableTrackerList)
		{
			if(tableTracker.isExits())
			{
				for(ColumnTracker columnTracker : tableTracker.getColumnTrackerList())
				{
					if(columnTracker.isExits())
					{
						boolean backupNullable = columnTracker.getColumn().getValue(ColumnNodeType.nullable) == null ? true : columnTracker.getColumn().getValue(ColumnNodeType.nullable).booleanValue();
						
						if(skipChecks)
						{
							columnTracker.getColumn().setValue(ColumnNodeType.nullable, true);
						}
						try
						{
							ColumnProcessor.checkColumnProperties
							(
								this, connection, driver, schema, tableTracker.getTable(), columnTracker.getColumn(), columnTracker, columnTracker.getColumnProperties(),checkProperties
							);
						}
						finally 
						{
							if(skipChecks)
							{
								columnTracker.getColumn().setValue(ColumnNodeType.nullable,backupNullable);
							}
						}
					}
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
			}
		}
		
		if(! skipChecks)
		{
			for(TableTracker tableTracker : tableTrackerList)
			{
				if(tableTracker.isExits())
				{
					TableProcessor.createTableKeys(this, connection, driver, schema, tableTracker, checkProperties);
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
					
					TableProcessor.createTableIndices(this, connection, driver, schema, tableTracker, checkProperties);
					
					if(checkProperties.isInterrupted())
					{
						return checkProperties.getUnusableExceptionList().isEmpty();
					}
				}
			}
			
			for(TableTracker tableTracker : tableTrackerList)
			{
				if(tableTracker.isExits())
				{
					for(ColumnTracker columnTracker : tableTracker.getColumnTrackerList())
					{
						if(columnTracker.isExits())
						{
							ColumnProcessor.createColumnKeys
							(
								this, connection, driver, schema, tableTracker.getTable(), columnTracker, checkProperties
							);
							
							if(checkProperties.isInterrupted())
							{
								return checkProperties.getUnusableExceptionList().isEmpty();
							}
						}
					}
				}
			}
		}
		
		try
		{
			driver.dropDummyColumns(connection, schema);
		}
		catch(SQLException e)
		{
			logSQLException(e);
		}
		catch (Exception e) 
		{
			this.logError(e, schema, "Error on drop dummy columns " + schemaSpecificationName, checkProperties);
		}
		
		if(checkProperties.isInterrupted())
		{
			return checkProperties.getUnusableExceptionList().isEmpty();
		}
		
		if(! schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers).isEmpty())
		{
			
			Dictionary<ObjectType, Object> objects = new Hashtable<>();
			objects.put(ObjectType.SCHEMA, schema);
			
			event.setObjects(objects);
			event.setActionType(ActionType.CHECK);
			event.setObjectType(ObjectType.SCHEMA);
			event.setPhaseType(PhaseType.POST);
			event.setException(null);
			
			for(BranchNode<DBSchemaNodeType, EventConsumerNodeType> consumerNode : schema.getUnmodifiableNodeList(DBSchemaNodeType.consumers))
			{
				ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
				if(consumer == null)
				{
					continue;
				}
				try
				{
					consumer.acceptWithException(event);
				}
				catch(SQLException e)
				{
					logSQLException(e);
				}
				catch (Exception e) 
				{
					this.logError(e, schema, "Error on UpdateListener.Schema.Check.Post " + schemaSpecificationName, checkProperties);
				}
				
				if(checkProperties.isInterrupted())
				{
					return checkProperties.getUnusableExceptionList().isEmpty();
				}
			}
			
			event.setObjects(null);
			event.setActionType(null);
			event.setObjectType(null);
			event.setPhaseType(null);
			event.setException(null);
		}
		
		event.setConnection(null);
		event.setDriver(null);
		event.setSchemaSpecificationName(null);
		
		return checkProperties.getUnusableExceptionList().isEmpty();
	}
	
	protected void logUpdate(String message, BranchNode<?, DBSchemaNodeType> schema)
	{
		if((schema.getValue(DBSchemaNodeType.logUpdates) != null) && (!schema.getValue(DBSchemaNodeType.logUpdates).booleanValue()))
		{
			return;
		}
		this.logService.info(message);
	}
	
	protected void logSQLException(SQLException e)
	{
		if(this.logService == null)
		{
			e.printStackTrace();
			return;
		}
		
		this.logService.error("{(type=sqlerror)(sqlstate=" + e.getSQLState() + ")(errorcode=" + e.getErrorCode() + ")} " + e.getMessage(),e);
	
		SQLException nextException = e.getNextException();
		if(nextException != null)
		{
			logSQLException(nextException);
		}
		
		if(e instanceof SQLWarning)
		{
			SQLWarning nextWarning = ((SQLWarning)e).getNextWarning();
			if(nextWarning == null)
			{
				return;
			}
			if(nextException == null)
			{
				logSQLException(nextWarning);
				return;
			}
			if(nextException != nextWarning)
			{
				logSQLException(nextWarning);
			}
		}
		
	}
	
	protected void logError(Throwable throwable, BranchNode<?, DBSchemaNodeType> schema, String msg, CheckProperties checkProperties)
	{
		if( throwable instanceof SchemaUnusableException) 
		{
			checkProperties.getUnusableExceptionList().add((SchemaUnusableException)throwable);
			if(throwable.getCause() instanceof TerminateException )
			{
				checkProperties.setInterrupted(true);
			}
		}
		if( throwable instanceof TerminateException) 
		{
			checkProperties.setInterrupted(true);
		}
		
		if(this.logService != null)
		{
			this.logService.error( msg,throwable);
		}
		else
		{
			System.err.println("" + msg);
			if(throwable != null)
			{
				throwable.printStackTrace();
			}
		}
	}
	
	public void close()
	{
		this.connection = null;
		this.driver = null;
	}
	
	/**
	 * An action type describes the kind of process in schema update workflow. The types specify the event in  {@link IDatabaseSchemaUpdateListener#onAction(ActionType, ObjectType, PhaseType, java.sql.Connection, String, java.util.Dictionary, IDatabaseSchemaDriver, Exception)}.
	 * 
	 * @author Sebastian Palarus
	 * @since 1.0
	 * @version 1.0
	 *
	 */
	public enum ActionType 
	{
		/**
		 * schema processor checks the existence or validity of an object (table,column, key ...)
		 */
		CHECK(1),
		
		/**
		 * schema processor has to update an object (table,column, key ...)
		 */
		UPDATE(2);
		
		private ActionType(int intValue)
		{
			this.intValue = intValue;
		}
		
		private static volatile Set<ActionType> ALL = null;
		
		private int intValue;
		
		/**
		 * getter for all action types
		 * 
		 * @return Set of all action types
		 */
		public static Set<ActionType> getAll()
		{
			if(ActionType.ALL == null)
			{
				EnumSet<ActionType> all = EnumSet.allOf(ActionType.class);
				ActionType.ALL = Collections.unmodifiableSet(all);
			}
			return ActionType.ALL;
		}
		
		/**
		 * search action type enum represents by {@code value}
		 * 
		 * @param value integer value of action type
		 * 
		 * @return action type enum represents by {@code value}
		 */
		public static ActionType findByInteger(int value)
		{
			for(ActionType actionType : getAll())
			{
				if(actionType.intValue == value)
				{
					return actionType;
				}
			}
			return null;
		}
		
		/**
		 * search action type enum represents by {@code name}
		 * 
		 * @param name of action type
		 * 
		 * @return enum represents by {@code name}
		 */
		public static ActionType findByName(String name)
		{
			for(ActionType actionType : getAll())
			{
				if(actionType.name().equalsIgnoreCase(name))
				{
					return actionType;
				}
			}
			return null;
		}
	}
	
	/**
	 * 
	 * An object type describes the kind of object (table,column,key ...) in schema update workflow. The types specify the event in  {@link IDatabaseSchemaUpdateListener#onAction(ActionType, ObjectType, PhaseType, java.sql.Connection, String, java.util.Dictionary, IDatabaseSchemaDriver, Exception)}.
	 * 
	 * @author Sebastian Palarus
	 * @since 1.0
	 * @version 1.0
	 *
	 */
	public enum ObjectType 
	{
		/**
		 * handle schema
		 */
		SCHEMA(1),
		
		/**
		 * handle table
		 */
		TABLE(2),
		
		/**
		 * handle primary key
		 */
		TABLE_PRIMARY_KEY(5),
		
		/**
		 * handle index
		 */
		TABLE_INDEX(8),
		
		/**
		 * handle column
		 */
		COLUMN(9),
		
		/**
		 * handle default value of column
		 */
		COLUMN_DEFAULT_VALUE(11),
		
		/**
		 * handle nullable option of column
		 */
		COLUMN_NULLABLE(12),
		
		/**
		 * handle size of text (char,varchar) columns
		 */
		COLUMN_SIZE(13),
		
		/**
		 * handle type of column (char,float,blob ...)
		 */
		COLUMN_TYPE(14),
		
		/**
		 * handle foreign key
		 */
		COLUMN_FOREIGN_KEY(15),
		
		/**
		 * handle custom schema-transformation between createing tables/columns and column-properties/keys
		 */
		SCHEMA_CONVERT_SCHEMA(16)
		
		;
		
		private ObjectType(int intValue)
		{
			this.intValue = intValue;
		}
		
		private static volatile Set<ObjectType> ALL = null;
		
		private int intValue;
		
		/**
		 * getter for all object types
		 * 
		 * @return Set of all object types
		 */
		public static Set<ObjectType> getAll()
		{
			if(ObjectType.ALL == null)
			{
				EnumSet<ObjectType> all = EnumSet.allOf(ObjectType.class);
				ObjectType.ALL = Collections.unmodifiableSet(all);
			}
			return ObjectType.ALL;
		}
		
		/**
		 * search object type enum represents by {@code value}
		 * 
		 * @param value integer value of object type
		 * 
		 * @return object type enum represents by {@code value}
		 */
		public static ObjectType findByInteger(int value)
		{
			for(ObjectType actionType : getAll())
			{
				if(actionType.intValue == value)
				{
					return actionType;
				}
			}
			return null;
		}
		
		/**
		 * search object type enum represents by {@code name}
		 * 
		 * @param name of object type
		 * 
		 * @return enum represents by {@code name}
		 */
		public static ObjectType findByName(String name)
		{
			for(ObjectType actionType : getAll())
			{
				if(actionType.name().equalsIgnoreCase(name))
				{
					return actionType;
				}
			}
			return null;
		}
	}
	
	/**
	 * An phase type describes the moment of handle database object in schema update workflow. The types specify the event in  {@link IDatabaseSchemaUpdateListener#onAction(ActionType, ObjectType, PhaseType, java.sql.Connection, String, java.util.Dictionary, IDatabaseSchemaDriver, Exception)}.
	 * 
	 * @author Sebastian Palarus
	 * @since 1.0
	 * @version 1.0
	 *
	 */
	public enum PhaseType 
	{
		/**
		 * before checks or update an object
		 */
		PRE(1),
		
		/**
		 * after checks or update an object
		 */
		POST(2);
		
		private PhaseType(int intValue)
		{
			this.intValue = intValue;
		}
		
		private static volatile Set<PhaseType> ALL = null;
		
		private int intValue;
		
		public static Set<PhaseType> getAll()
		{
			if(PhaseType.ALL == null)
			{
				EnumSet<PhaseType> all = EnumSet.allOf(PhaseType.class);
				PhaseType.ALL = Collections.unmodifiableSet(all);
			}
			return PhaseType.ALL;
		}
		
		public static PhaseType findByInteger(int value)
		{
			for(PhaseType actionType : getAll())
			{
				if(actionType.intValue == value)
				{
					return actionType;
				}
			}
			return null;
		}
		
		public static PhaseType findByName(String name)
		{
			for(PhaseType actionType : getAll())
			{
				if(actionType.name().equalsIgnoreCase(name))
				{
					return actionType;
				}
			}
			return null;
		}
	}
	
	public class DBSchemaEvent
	{
		private ActionType actionType; 
		private ObjectType objectType; 
		private PhaseType phaseType;
		private Connection connection;
		private String schemaSpecificationName;
		private Dictionary<ObjectType, Object> objects;
		private IDBSchemaUtilsDriver driver;
		private Exception exception;
		
		/**
		 * 
		 * @return action types describe the kind of process in schema update workflow
		 */
		public ActionType getActionType()
		{
			return actionType;
		}
		
		/**
		 * 
		 * @return objectType object types describe the kind of object (table,column,key ...) in schema update workflow
		 */
		public ObjectType getObjectType()
		{
			return objectType;
		}
		
		/**
		 * 
		 * @return phase type describe the moment of handle database object in schema update workflow
		 */
		public PhaseType getPhaseType()
		{
			return phaseType;
		}
		
		/**
		 * 
		 * @return used connection
		 */
		public Connection getConnection()
		{
			return connection;
		}
		
		/**
		 * 
		 * @return name of schema specification
		 */
		public String getSchemaSpecificationName()
		{
			return schemaSpecificationName;
		}
		
		/**
		 * 
		 * @return property objects (involved specification objects)
		 */
		public Dictionary<ObjectType, Object> getObjects()
		{
			return objects;
		}
		
		/**
		 * 
		 * @return schema driver
		 */
		public IDBSchemaUtilsDriver getDriver()
		{
			return driver;
		}
		
		/**
		 * 
		 * @return possible catched exceptions for {@link PhaseType#POST} - events
		 */
		public Exception getException()
		{
			return exception;
		}
		
		protected void setActionType(ActionType actionType)
		{
			this.actionType = actionType;
		}
		protected void setObjectType(ObjectType objectType)
		{
			this.objectType = objectType;
		}
		protected void setPhaseType(PhaseType phaseType)
		{
			this.phaseType = phaseType;
		}
		protected void setConnection(Connection connection)
		{
			this.connection = connection;
		}
		protected void setSchemaSpecificationName(String schemaSpecificationName)
		{
			this.schemaSpecificationName = schemaSpecificationName;
		}
		protected void setObjects(Dictionary<ObjectType, Object> objects)
		{
			this.objects = objects;
		}
		protected void setDriver(IDBSchemaUtilsDriver driver)
		{
			this.driver = driver;
		}
		protected void setException(Exception exception)
		{
			this.exception = exception;
		}
	}
	
	protected class CheckProperties
	{
		private boolean interrupted = false;
		private List<SchemaUnusableException> unusableExceptionList = new ArrayList<SchemaUnusableException>();

		public boolean isInterrupted()
		{
			return interrupted;
		}
		public void setInterrupted(boolean interrupted)
		{
			this.interrupted = interrupted;
		}
		
		public List<SchemaUnusableException> getUnusableExceptionList()
		{
			return unusableExceptionList;
		}
		public void setUnusableExceptionList(List<SchemaUnusableException> unusableExceptionList)
		{
			this.unusableExceptionList = unusableExceptionList;
		}
	}
	
	/**
	 * This exception should thrown, if schema is unusable. In this case, the process {@link IDatabaseSchemaProcessor#checkSchemaSpec(SchemaSpec, java.sql.Connection)} return false. 
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public class SchemaUnusableException extends RuntimeException
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 2808225220948944523L;

		public SchemaUnusableException()
		{
			super();
		}

		public SchemaUnusableException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
		{
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public SchemaUnusableException(String message, Throwable cause)
		{
			super(message, cause);
		}

		public SchemaUnusableException(String message)
		{
			super(message);
		}

		public SchemaUnusableException(Throwable cause)
		{
			super(cause);
		}
		
	}
	
	/**
	 * This exception should thrown, if process {@link IDatabaseSchemaProcessor#checkSchemaSpec(SchemaSpec, java.sql.Connection)} have to stop to work. 
	 * Furthermore, if cause is instance of {@link SchemaUnusableException}, the processor returns false in {@link IDatabaseSchemaProcessor#checkSchemaSpec(SchemaSpec, java.sql.Connection)}.
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public class TerminateException extends RuntimeException
	{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 4392864023695139818L;

		public TerminateException()
		{
			super();
		}

		public TerminateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
		{
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public TerminateException(String message, Throwable cause)
		{
			super(message, cause);
		}

		public TerminateException(String message)
		{
			super(message);
		}

		public TerminateException(Throwable cause)
		{
			super(cause);
		}

	}
	
	protected class TableTracker
	{
		private BranchNode<DBSchemaNodeType,TableNodeType> table = null;
		private boolean created = false;
		private boolean exits = false;
		private Map<String,Object> tableProperties = null;
		private List<ColumnTracker> columnTrackerList = new ArrayList<ColumnTracker>();
		
		public boolean isCreated()
		{
			return created;
		}
		public void setCreated(boolean created)
		{
			this.created = created;
		}
		public boolean isExits()
		{
			return exits;
		}
		public void setExits(boolean exits)
		{
			this.exits = exits;
		}
		protected BranchNode<DBSchemaNodeType, TableNodeType> getTable()
		{
			return table;
		}
		protected void setTable(BranchNode<DBSchemaNodeType, TableNodeType> table)
		{
			this.table = table;
		}
		public Map<String, Object> getTableProperties()
		{
			return tableProperties;
		}
		public void setTableProperties(Map<String, Object> tableProperties)
		{
			this.tableProperties = tableProperties;
		}
		protected void setColumnTrackerList(List<ColumnTracker> columnTrackerList)
		{
			this.columnTrackerList = columnTrackerList;
		}
		public List<ColumnTracker> getColumnTrackerList()
		{
			return columnTrackerList;
		}
	}
	
	protected class ColumnTracker
	{
		private BranchNode<TableNodeType, ColumnNodeType> column = null;
		private boolean created = false;
		private boolean exits = false;
		private Map<String,Object> columnProperties = null;
		
		public boolean isCreated()
		{
			return created;
		}
		public void setCreated(boolean created)
		{
			this.created = created;
		}
		public boolean isExits()
		{
			return exits;
		}
		public void setExits(boolean exits)
		{
			this.exits = exits;
		}
		protected BranchNode<TableNodeType, ColumnNodeType> getColumn()
		{
			return column;
		}
		protected void setColumn(BranchNode<TableNodeType, ColumnNodeType> column)
		{
			this.column = column;
		}
		public Map<String, Object> getColumnProperties()
		{
			return columnProperties;
		}
		public void setColumnProperties(Map<String, Object> columnProperties)
		{
			this.columnProperties = columnProperties;
		}
	}
	
	protected static class TableProcessor
	{
		protected static TableTracker checkTableDefinition
		(
			DBSchemaUtils dbSchemaUtils, 
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			BranchNode<DBSchemaNodeType, TableNodeType> table,
			CheckProperties checkProperties
		)
		{
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			TableTracker tableTracker = dbSchemaUtils.new TableTracker();
			tableTracker.setTable(table);
			try
			{
				if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
				{
					Dictionary<ObjectType, Object> objects = new Hashtable<>();
					objects.put(ObjectType.SCHEMA, schema);
					objects.put(ObjectType.TABLE, table);
					
					event.setObjects(objects);
					event.setActionType(ActionType.CHECK);
					event.setObjectType(ObjectType.TABLE);
					event.setPhaseType(PhaseType.PRE);
					event.setException(null);
					
					for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
					{
						ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
						if(consumer == null)
						{
							continue;
						}
						try
						{
							consumer.acceptWithException(event);
						}
						catch(SQLException e)
						{
							dbSchemaUtils.logSQLException(e);
						}
						catch (Exception e) 
						{
							dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Table.Check.Pre ", checkProperties);
						}
						
						if(checkProperties.isInterrupted())
						{
							return tableTracker;
						}
					}
					
					event.setObjects(null);
					event.setActionType(null);
					event.setObjectType(null);
					event.setPhaseType(null);
					event.setException(null);
				}
				
				Map<String,Object> tableProperties = new HashMap<String, Object>(); 
				tableTracker.setTableProperties(tableProperties);
				tableTracker.setExits(driver.tableExists(connection, schema, table, tableProperties));
				if(! tableTracker.isExits())
				{
					dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=createtable)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + ")} create table " + table.getValue(TableNodeType.name),schema);
						
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.TABLE);
						event.setPhaseType(PhaseType.PRE);
						event.setException(null);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Table.Insert.Pre ", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return tableTracker;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
					
					Exception exc= null;
					
					try
					{
						driver.createTable(connection, schema, table, tableProperties);
						
						tableTracker.setCreated(true);
						tableTracker.setExits(true);
					}
					catch(SQLException e)
					{
						exc = e;
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						exc = e;
						dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " can not create ", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return tableTracker;
					}
						
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.TABLE);
						event.setPhaseType(PhaseType.POST);
						event.setException(exc);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Table.Insert.Post ", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return tableTracker;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
				}
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
			}
			catch (Exception e) 
			{
				dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error", checkProperties);
			}
			
			if(checkProperties.isInterrupted())
			{
				return tableTracker;
			}
			
			if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
			{
				Dictionary<ObjectType, Object> objects = new Hashtable<>();
				objects.put(ObjectType.SCHEMA, schema);
				objects.put(ObjectType.TABLE, table);
				
				event.setObjects(objects);
				event.setActionType(ActionType.CHECK);
				event.setObjectType(ObjectType.TABLE);
				event.setPhaseType(PhaseType.POST);
				event.setException(null);
				
				for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
				{
					ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
					if(consumer == null)
					{
						continue;
					}
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Table.Check.Post",checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return tableTracker;
					}
				}
				
				event.setObjects(null);
				event.setActionType(null);
				event.setObjectType(null);
				event.setPhaseType(null);
				event.setException(null);
			}
				
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
			
			return tableTracker;
		}
		
		public static void createTableKeys
		(
			DBSchemaUtils dbSchemaUtils,
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			TableTracker tableTracker, 
			CheckProperties checkProperties
		)
		{
			if(! tableTracker.isExits())
			{
				return;
			}
			
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			BranchNode<DBSchemaNodeType, TableNodeType>  table = tableTracker.getTable();
			
			try
			{
				boolean pkExists = driver.primaryKeyExists(connection, schema, table, tableTracker.getTableProperties());
				if(! pkExists)
				{
					dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=createprimarykey)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + ")} create primarykey " + table.getValue(TableNodeType.name),schema);
						
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.TABLE_PRIMARY_KEY);
						event.setPhaseType(PhaseType.PRE);
						event.setException(null);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.PrimaryKey.Pre ",  checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
					
					Exception exc= null;
					
					try
					{
						driver.setPrimaryKey(connection, schema, table, tableTracker.getTableProperties());
					}
					catch(SQLException e)
					{
						exc = e;
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						exc = e;
						dbSchemaUtils.logError(e, schema,  "Primary Key for Table " + table.getValue(TableNodeType.name) + " can not create ",  checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return;
					}
							
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.TABLE_PRIMARY_KEY);
						event.setPhaseType(PhaseType.POST);
						event.setException(exc);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.PrimaryKey.Post ",  checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
				}
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
			}
			catch (Exception e) 
			{
				dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error handle primary key ",checkProperties);
			}
			
			if(checkProperties.isInterrupted())
			{
				return;
			}
			
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
		}
		
		public static void createTableIndices
		(
			DBSchemaUtils dbSchemaUtils,
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			TableTracker tableTracker, 
			CheckProperties checkProperties
		)
		{
			if(! tableTracker.isExits())
			{
				return;
			}
			
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			BranchNode<DBSchemaNodeType, TableNodeType>  table = tableTracker.getTable();
			
			try
			{
				for(BranchNode<TableNodeType,IndexNodeType> index : table.getUnmodifiableNodeList(TableNodeType.indices))
				{
					try
					{
						Map<String,Object> columnIndexProperties = new HashMap<String,Object>();
						boolean indexExists = driver.isValidIndex(connection, schema, table, index, columnIndexProperties);
						
						if(! indexExists)
						{
							
							dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=createindex)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + ")} create index " + index.getValue(IndexNodeType.name),schema);
							
							if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
							{
								Dictionary<ObjectType, Object> objects = new Hashtable<>();
								objects.put(ObjectType.SCHEMA, schema);
								objects.put(ObjectType.TABLE, table);
								objects.put(ObjectType.TABLE_INDEX, index);
								
								event.setObjects(objects);
								event.setActionType(ActionType.UPDATE);
								event.setObjectType(ObjectType.TABLE_INDEX);
								event.setPhaseType(PhaseType.PRE);
								event.setException(null);
								
								for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
								{
									ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
									if(consumer == null)
									{
										continue;
									}
									try
									{
										consumer.acceptWithException(event);
									}
									catch(SQLException e)
									{
										dbSchemaUtils.logSQLException(e);
									}
									catch (Exception e) 
									{
										dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Index.Pre", checkProperties);
									}
									
									if(checkProperties.isInterrupted())
									{
										return;
									}
								}
								
								event.setObjects(null);
								event.setActionType(null);
								event.setObjectType(null);
								event.setPhaseType(null);
								event.setException(null);
							}
							
							Exception exc = null;
							try
							{
								driver.setValidIndex(connection, schema, table, index, columnIndexProperties);
							}
							catch(SQLException e)
							{
								exc = e;
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								exc = e;
								dbSchemaUtils.logError(e, schema,  "Index " + index.getValue(IndexNodeType.name) + " can not create ", checkProperties);
							}
								
							if(checkProperties.isInterrupted())
							{
								return;
							}
							
							if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
							{
								Dictionary<ObjectType, Object> objects = new Hashtable<>();
								objects.put(ObjectType.SCHEMA, schema);
								objects.put(ObjectType.TABLE, table);
								objects.put(ObjectType.TABLE_INDEX, index);
								
								event.setObjects(objects);
								event.setActionType(ActionType.UPDATE);
								event.setObjectType(ObjectType.TABLE_INDEX);
								event.setPhaseType(PhaseType.POST);
								event.setException(exc);
								
								for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
								{
									ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
									if(consumer == null)
									{
										continue;
									}
									try
									{
										consumer.acceptWithException(event);
									}
									catch(SQLException e)
									{
										dbSchemaUtils.logSQLException(e);
									}
									catch (Exception e) 
									{
										dbSchemaUtils.logError(e, schema, "Table " + table.getValue(TableNodeType.name) + " Error on UpdateListener.Index.Post", checkProperties);
									}
									
									if(checkProperties.isInterrupted())
									{
										return;
									}
								}
								
								event.setObjects(null);
								event.setActionType(null);
								event.setObjectType(null);
								event.setPhaseType(null);
								event.setException(null);
							}
						}
					}
					catch(SQLException e)
					{
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						dbSchemaUtils.logError(e, schema, "error: " + index.getValue(IndexNodeType.name), checkProperties);
					}
				}
				
			}
			catch (Exception e) 
			{
				dbSchemaUtils.logError(e, schema,e.getMessage(), checkProperties);
			}
			
			if(checkProperties.isInterrupted())
			{
				return;
			}
				
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
		}
	}
	
	protected static class ColumnProcessor
	{
		public static ColumnTracker checkColumnDefinition
		(
			DBSchemaUtils dbSchemaUtils, 
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			BranchNode<DBSchemaNodeType, TableNodeType> table,
			BranchNode<TableNodeType, ColumnNodeType> column,
			Map<String,Object> tableProperties, 
			CheckProperties checkProperties
		)
		{
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			ColumnTracker columnTracker = dbSchemaUtils.new ColumnTracker();
			columnTracker.setColumn(column);
			try
			{
				if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
				{
					Dictionary<ObjectType, Object> objects = new Hashtable<>();
					objects.put(ObjectType.SCHEMA, schema);
					objects.put(ObjectType.TABLE, table);
					objects.put(ObjectType.COLUMN, column);
					
					event.setObjects(objects);
					event.setActionType(ActionType.CHECK);
					event.setObjectType(ObjectType.COLUMN);
					event.setPhaseType(PhaseType.PRE);
					event.setException(null);
					
					for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
					{
						ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
						if(consumer == null)
						{
							continue;
						}
						try
						{
							consumer.acceptWithException(event);
						}
						catch(SQLException e)
						{
							dbSchemaUtils.logSQLException(e);
						}
						catch (Exception e) 
						{
							dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Column.Check.Pre ", checkProperties);
						}
						
						if(checkProperties.isInterrupted())
						{
							return columnTracker;
						}
					}
					
					event.setObjects(null);
					event.setActionType(null);
					event.setObjectType(null);
					event.setPhaseType(null);
					event.setException(null);
				}
				
				Map<String,Object> columnProperties = new HashMap<String, Object>(); 
				if(tableProperties != null)
				{
					for(Entry<String, Object> entry : tableProperties.entrySet())
					{
						columnProperties.put(entry.getKey(), entry.getValue());
					}
				}
				columnTracker.setColumnProperties(columnProperties);
				columnTracker.setExits(driver.columnExists(connection, schema, table,column, columnProperties));
					
				if(! columnTracker.isExits())
				{
					dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=createcolumn)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} create column " + table.getValue(TableNodeType.name).toUpperCase() + "." + column.getValue(ColumnNodeType.name),schema);
						
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						objects.put(ObjectType.COLUMN, column);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.COLUMN);
						event.setPhaseType(PhaseType.PRE);
						event.setException(null);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Column.Insert.Pre ", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return columnTracker;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
					
					Exception exc= null;
					
					try
					{
						driver.createColumn(connection, schema, table, column, columnProperties);
						
						columnTracker.setExits(true);
						columnTracker.setCreated(true);
					}
					catch(SQLException e)
					{
						exc = e;
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						exc = e;
						dbSchemaUtils.logError(e, schema,  "Column " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " can not create ",  checkProperties);
					}
						
					if(checkProperties.isInterrupted())
					{
						return columnTracker;
					}
					
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						objects.put(ObjectType.COLUMN, column);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.COLUMN);
						event.setPhaseType(PhaseType.POST);
						event.setException(exc);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Column.Insert.Post ", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return columnTracker;
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
				}
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
			}
			catch (Exception e) 
			{
				dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error ", checkProperties);
			}
			
			if(checkProperties.isInterrupted())
			{
				return columnTracker;
			}
			
			if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
			{
				Dictionary<ObjectType, Object> objects = new Hashtable<>();
				objects.put(ObjectType.SCHEMA, schema);
				objects.put(ObjectType.TABLE, table);
				objects.put(ObjectType.COLUMN, column);
				
				event.setObjects(objects);
				event.setActionType(ActionType.CHECK);
				event.setObjectType(ObjectType.COLUMN);
				event.setPhaseType(PhaseType.POST);
				event.setException(null);
				
				for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
				{
					ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
					if(consumer == null)
					{
						continue;
					}
					try
					{
						consumer.acceptWithException(event);
					}
					catch(SQLException e)
					{
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Column.Check.Post",  checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return columnTracker;
					}
				}
				
				event.setObjects(null);
				event.setActionType(null);
				event.setObjectType(null);
				event.setPhaseType(null);
				event.setException(null);
			}
				
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
			
			return columnTracker;
		}
		
		public static void checkColumnProperties
		(
			DBSchemaUtils dbSchemaUtils,  
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			BranchNode<?, TableNodeType> table,
			BranchNode<?, ColumnNodeType> column,
			ColumnTracker columnTracker, 
			Map<String,Object> columnProperties, 
			CheckProperties checkProperties
		)
		{
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			if(! columnTracker.isExits())
			{
				return;
			}
			try
			{
				boolean columnPropertiesValid = driver.isValidColumnProperties(connection, schema, table, column, columnProperties);
				
				if(! columnPropertiesValid)
				{
					if(columnProperties.get("INVALID_NULLABLE") != null)
					{
						dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=setnullable)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} set nullable " + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + " " + column.getValue(ColumnNodeType.nullable),schema);
						
						if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_NULLABLE);
							event.setPhaseType(PhaseType.PRE);
							event.setException(null);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Nullable.Pre ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
					
					if(columnProperties.get("INVALID_SIZE") != null)
					{
						dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=setcolsize)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} set colsize " + table.getValue(TableNodeType.name)+ "." + column.getValue(ColumnNodeType.name) + " " + column.getValue(ColumnNodeType.size),schema);
						
						if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_SIZE);
							event.setPhaseType(PhaseType.PRE);
							event.setException(null);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Size.Pre ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
						
						
					if(columnProperties.get("INVALID_TYPE") != null)
					{
						dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=setcoltype)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} set type " + table.getValue(TableNodeType.name)+ "." + column.getValue(ColumnNodeType.name) + " " + column.getValue(ColumnNodeType.columnType),schema);
						
						if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_TYPE);
							event.setPhaseType(PhaseType.PRE);
							event.setException(null);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Type.Pre ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
						
					if(columnProperties.get("INVALID_DEFAULT") != null)
					{
						// TODO Fehlermeldung anpassen
						dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=setcoldefaultvalue)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} set nullable " + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + " " + column.getValue(ColumnNodeType.defaultValueClass),schema);
						
						if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_DEFAULT_VALUE);
							event.setPhaseType(PhaseType.PRE);
							event.setException(null);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.defaultvalue.Pre ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
						
						
					Exception exc = null;
					
					try
					{
						driver.setValidColumnProperties(connection, schema, table, column, columnProperties);
					}
					catch(SQLException e)
					{
						exc = e;
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						exc = e;
						dbSchemaUtils.logError(e, schema,  "Column properties for " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " can not update", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return;
					}
					
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						if(columnProperties.get("INVALID_NULLABLE") != null)
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_NULLABLE);
							event.setPhaseType(PhaseType.POST);
							event.setException(exc);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Nullable.Post ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
							
						if(columnProperties.get("INVALID_SIZE") != null)
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_SIZE);
							event.setPhaseType(PhaseType.POST);
							event.setException(exc);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Size.Post ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
							
								
						if(columnProperties.get("INVALID_TYPE") != null)
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_TYPE);
							event.setPhaseType(PhaseType.POST);
							event.setException(exc);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.Type.Post ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
								
						if(columnProperties.get("INVALID_DEFAULT") != null)
						{
							Dictionary<ObjectType, Object> objects = new Hashtable<>();
							objects.put(ObjectType.SCHEMA, schema);
							objects.put(ObjectType.TABLE, table);
							objects.put(ObjectType.COLUMN, column);
							
							event.setObjects(objects);
							event.setActionType(ActionType.UPDATE);
							event.setObjectType(ObjectType.COLUMN_DEFAULT_VALUE);
							event.setPhaseType(PhaseType.POST);
							event.setException(exc);
							
							for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
							{
								ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
								if(consumer == null)
								{
									continue;
								}
								try
								{
									consumer.acceptWithException(event);
								}
								catch(SQLException e)
								{
									dbSchemaUtils.logSQLException(e);
								}
								catch (Exception e) 
								{
									dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.defaultvalu.Post ", checkProperties);
								}
								
								if(checkProperties.isInterrupted())
								{
									return;
								}
							}
							
							event.setObjects(null);
							event.setActionType(null);
							event.setObjectType(null);
							event.setPhaseType(null);
							event.setException(null);
						}
					}
					
				}
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
			}
			
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
		}
		
		public static void createColumnKeys
		(
			DBSchemaUtils dbSchemaUtils, 
			Connection connection, 
			IDBSchemaUtilsDriver driver, 
			BranchNode<?, DBSchemaNodeType> schema, 
			BranchNode<?, TableNodeType> table,
			ColumnTracker columnTracker, 
			CheckProperties checkProperties
		)
		{
			if(! columnTracker.isExits())
			{
				return;
			}
			
			String schemaSpecificationName = schema.getValue(DBSchemaNodeType.name);
			DBSchemaEvent event = dbSchemaUtils.new DBSchemaEvent();
			event.setConnection(connection);
			event.setDriver(driver);
			event.setSchemaSpecificationName(schemaSpecificationName);
			
			BranchNode<?, ColumnNodeType> column = columnTracker.getColumn();
			Map<String,Object> columnProperties = columnTracker.getColumnProperties();
			try
			{
				boolean foreinKeyValid = driver.isValidForeignKey(connection, schema, table, column, columnTracker.getColumnProperties());
				if(! foreinKeyValid)
				{
					dbSchemaUtils.logUpdate("{(type=updatedbmodel)(action=createforeignkey)(database=" + schemaSpecificationName + ")(object=" + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name) + ")} create foreignkey " + table.getValue(TableNodeType.name) + "." + column.getValue(ColumnNodeType.name),schema);
					
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						objects.put(ObjectType.COLUMN, column);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.COLUMN_FOREIGN_KEY);
						event.setPhaseType(PhaseType.PRE);
						event.setException(null);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.FK.Pre ", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
					
					Exception exc = null;
					
					try
					{
						driver.setValidForeignKey(connection, schema, table, column, columnProperties);
					}
					catch(SQLException e)
					{
						exc = e;
						dbSchemaUtils.logSQLException(e);
					}
					catch (Exception e) 
					{
						exc = e;
						dbSchemaUtils.logError(e, schema,  "Column foreign key for " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " can not update", checkProperties);
					}
					
					if(checkProperties.isInterrupted())
					{
						return;
					}
					
					if(! table.getUnmodifiableNodeList(TableNodeType.consumers).isEmpty())
					{
						Dictionary<ObjectType, Object> objects = new Hashtable<>();
						objects.put(ObjectType.SCHEMA, schema);
						objects.put(ObjectType.TABLE, table);
						objects.put(ObjectType.COLUMN, column);
						
						event.setObjects(objects);
						event.setActionType(ActionType.UPDATE);
						event.setObjectType(ObjectType.COLUMN_FOREIGN_KEY);
						event.setPhaseType(PhaseType.POST);
						event.setException(exc);
						
						for(BranchNode<TableNodeType, EventConsumerNodeType> consumerNode : table.getUnmodifiableNodeList(TableNodeType.consumers))
						{
							ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent> consumer = consumerNode.getValue(EventConsumerNodeType.eventConsumer);
							if(consumer == null)
							{
								continue;
							}
							try
							{
								consumer.acceptWithException(event);
							}
							catch(SQLException e)
							{
								dbSchemaUtils.logSQLException(e);
							}
							catch (Exception e) 
							{
								dbSchemaUtils.logError(e, schema,  "Table " + table.getValue(TableNodeType.name) + " Col " + column.getValue(ColumnNodeType.name) + " Error on UpdateListener.FK.Post", checkProperties);
							}
							
							if(checkProperties.isInterrupted())
							{
								return;
							}
						}
						
						event.setObjects(null);
						event.setActionType(null);
						event.setObjectType(null);
						event.setPhaseType(null);
						event.setException(null);
					}
				}
				
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
			}
				
			try
			{
				SQLWarning warning = connection.getWarnings();
				if(warning != null)
				{
					dbSchemaUtils.logSQLException(warning);
				}
				connection.clearWarnings();
			}
			catch(SQLException e)
			{
				dbSchemaUtils.logSQLException(e);
				try
				{
					connection.clearWarnings();
				}
				catch(Exception e2){}
			}
			
			event.setConnection(null);
			event.setDriver(null);
			event.setSchemaSpecificationName(null);
		}
	}
	
	public static String getSchema(Connection connection) throws SQLException
	{
		try
		{
			return connection.getSchema();
		}
		catch (NoSuchMethodError e) // Android
		{
			try
			{
				return (String)connection.getClass().getMethod("getSchema").invoke(connection);
			}
			catch (Exception e2) {}
			throw e;
		}
	}
	
	public static void setSchema(Connection connection, String schema) throws SQLException
	{
		try
		{
			connection.setSchema(schema);
		}
		catch (NoSuchMethodError e) // Android
		{
			try
			{
				connection.getClass().getMethod("setSchema",String.class).invoke(connection,schema);
			}
			catch (Exception e2) {}
			throw e;
		}
	}
	
	
}
