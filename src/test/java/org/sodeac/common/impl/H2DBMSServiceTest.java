package org.sodeac.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.IDatabaseManagementSystemService;
import org.sodeac.common.IDatabaseManagementSystemService.H2PropertyBuilder;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.misc.CloseableCollector;
import org.sodeac.common.misc.Driver;
//import org.sodeac.common.model.dbschema.DBSchemaBowFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class H2DBMSServiceTest
{
	@Test
	public void test00001ServiceRegistration()
	{
		IDatabaseManagementSystemService databaseManagementSystemService = getService();
		assertNotNull("value should not be null", databaseManagementSystemService);
	}
	
	@Test
	public void test00010RemoveDatabase() throws Exception
	{
		IDatabaseManagementSystemService databaseManagementSystemService = getService();
		assertNotNull("value should not be null", databaseManagementSystemService);
		
		File directory = new File("./target");
		
		Connection connection = DriverManager.getConnection("jdbc:h2:./target/h2-test-removedb","sa","sa");
		connection.close();
		
		File dbFile = new File(directory,"h2-test-removedb.mv.db");
		assertTrue("value should be correct", dbFile.exists());
		
		H2PropertyBuilder propertyBuilder = H2PropertyBuilder.newInstance(directory.getCanonicalPath(), "h2-test-removedb").setConnectionUsername("sa").setConnectionPassword("sa");
		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
	
		assertFalse("value should be correct", dbFile.exists());
		
	}
	
	@Test
	public void test00020CreateDatabase() throws Exception
	{
		String dbName = "h2-test-createdb";
		IDatabaseManagementSystemService databaseManagementSystemService = getService();
		assertNotNull("value should not be null", databaseManagementSystemService);
		
		File directory = new File("./target");
		
		H2PropertyBuilder propertyBuilder = H2PropertyBuilder.newInstance(directory.getCanonicalPath(), dbName).setConnectionUsername("sa").setConnectionPassword("sa");
		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		File dbFile = new File(directory, dbName + ".mv.db");
		assertFalse("value should be correct", dbFile.exists());
		
		databaseManagementSystemService.createDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		assertTrue("value should be correct", dbFile.exists());
		
		Connection connection = DriverManager.getConnection
		(
			databaseManagementSystemService.getConnectionString(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getUser(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getPassword(propertyBuilder.getConnectionProperties())
		);
		connection.close();
		
	}
	
	@Test
	public void test00021CreateEncryptedDatabase() throws Exception
	{
		String dbName = "h2-test-createcryptedb";
		IDatabaseManagementSystemService databaseManagementSystemService = getService();
		assertNotNull("value should not be null", databaseManagementSystemService);
		
		File directory = new File("./target");
		
		H2PropertyBuilder propertyBuilder = H2PropertyBuilder.newInstance(directory.getCanonicalPath(), dbName).setConnectionUsername("sa").setConnectionPassword("sa").setConnectionEncryptionKey("geheim");
		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		File dbFile = new File(directory, dbName + ".mv.db");
		assertFalse("value should be correct", dbFile.exists());
		
		databaseManagementSystemService.createDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		assertTrue("value should be correct", dbFile.exists());
		
		Connection connection = DriverManager.getConnection
		(
			databaseManagementSystemService.getConnectionString(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getUser(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getPassword(propertyBuilder.getConnectionProperties())
		);
		connection.close();
		
	}
	
	@Test
	public void test00030Schema() throws Exception
	{
		String dbName = "h2-test-createschema";
		String schemaName = "MY_SCHEMA";
		IDatabaseManagementSystemService databaseManagementSystemService = getService();
		assertNotNull("value should not be null", databaseManagementSystemService);
		
		File directory = new File("./target");
		
		H2PropertyBuilder propertyBuilder = H2PropertyBuilder.newInstance(directory.getCanonicalPath(), dbName).setConnectionUsername("sa").setConnectionPassword("sa");
		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		File dbFile = new File(directory, dbName + ".mv.db");
		assertFalse("value should be correct", dbFile.exists());
		
		databaseManagementSystemService.createDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
		
		assertTrue("value should be correct", dbFile.exists());
		
		boolean exists = false;
		Connection connection = DriverManager.getConnection
		(
			databaseManagementSystemService.getConnectionString(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getUser(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getPassword(propertyBuilder.getConnectionProperties())
		);
		try
		{
			DatabaseMetaData meta = connection.getMetaData();
			ResultSet resultSet = meta.getSchemas();
			while (resultSet.next()) 
			{
				String tableSchema = resultSet.getString(1); 
				if(tableSchema.equalsIgnoreCase(schemaName))
				{
					exists = true;
					break;
				}
		    }
			resultSet.close();
		}
		finally
		{
			connection.close();
		}
		
		assertFalse("value should be correct",exists);
		
		databaseManagementSystemService.createSchema(propertyBuilder.getConnectionProperties().copy().setSchema(schemaName));
		
		connection = DriverManager.getConnection
		(
			databaseManagementSystemService.getConnectionString(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getUser(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getPassword(propertyBuilder.getConnectionProperties())
		);
		try
		{
			DatabaseMetaData meta = connection.getMetaData();
			ResultSet resultSet = meta.getSchemas();
			while (resultSet.next()) 
			{
				String tableSchema = resultSet.getString(1);
				if(tableSchema.equalsIgnoreCase(schemaName))
				{
					exists = true;
					break;
				}
		    }
			resultSet.close();
		}
		finally
		{
			connection.close();
		}
		
		assertTrue("value should be correct",exists);
		
		databaseManagementSystemService.removeSchema(propertyBuilder.getConnectionProperties().copy().setSchema(schemaName));
		exists = false;
		
		connection = DriverManager.getConnection
		(
			databaseManagementSystemService.getConnectionString(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getUser(propertyBuilder.getConnectionProperties()),
			databaseManagementSystemService.getPassword(propertyBuilder.getConnectionProperties())
		);
		try
		{
			DatabaseMetaData meta = connection.getMetaData();
			ResultSet resultSet = meta.getSchemas();
			while (resultSet.next()) 
			{
				String tableSchema = resultSet.getString(1);
				if(tableSchema.equalsIgnoreCase(schemaName))
				{
					exists = true;
					break;
				}
		    }
			resultSet.close();
		}
		finally
		{
			connection.close();
		}
				
		assertFalse("value should be correct",exists);
		
	}
	
//	@SuppressWarnings("unchecked")
//	@Test
//	public void test00040BackupRestore() throws Exception
//	{
//		String dbName = "h2-test-backuprestore-origin";
//		String dbNameBackup = "h2-test-backuprestore-backup";
//		String schemaName1 = "MY_SCHEMA1";
//		String schemaName2 = "MY_SCHEMA2";
//		IDatabaseManagementSystemService databaseManagementSystemService = getService();
//		assertNotNull("value should not be null", databaseManagementSystemService);
//		
//		File directory = new File("./target");
//		
//		H2PropertyBuilder propertyBuilder = H2PropertyBuilder.newInstance(directory.getCanonicalPath(), dbName).setConnectionUsername("sa").setConnectionPassword("sa");//.setConnectionEncryptionKey("geheim");
//		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties());
//		databaseManagementSystemService.removeDatabase(propertyBuilder.getSystemProperties(), propertyBuilder.getDatabaseProperties(), propertyBuilder.getConnectionProperties().copy().setDbname(dbNameBackup));
//		
//		File dbFile = new File(directory, dbName + ".mv.db");
//		assertFalse("value should be correct", dbFile.exists());
//		
//		File dbFileBackup = new File(directory, dbNameBackup + ".mv.db");
//		assertFalse("value should be correct", dbFileBackup.exists());
//		
//		Connection connection = databaseManagementSystemService.getConnection(propertyBuilder.getConnectionProperties());
//		try
//		{
//			databaseManagementSystemService.createSchema(propertyBuilder.getConnectionProperties().copy().setSchema(schemaName1));
//			databaseManagementSystemService.createSchema(propertyBuilder.getConnectionProperties().copy().setSchema(schemaName2));
//			
//			DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
//			
//			dbSchemaUtils.adaptSchema
//			(
//				DBSchemaBowFactory.createSchema(schemaName1, schemaName1)
//					.createOneOfTables().setName("MY_TABLE_1")
//						.createUUIDColumnDefaultAuto("ID").createPrimaryKey().build().build()
//						.createVarcharColumn("NAME", false, 128).build()
//					.build()
//					.createOneOfTables().setName("MY_TABLE_2")
//						.createUUIDColumnDefaultAuto("ID").createPrimaryKey().build().build()
//						.createUUIDColumn("MY_TABLE_1_ID",true)
//							.createForeignKey().setConstraintName("FK1_MY_TABLE_2").setReferencedTableName("MY_TABLE_1").setReferencedColumnName("ID").build().build()
//						.createBigIntAutoIncrementColumn("NUM", "SEQ_MA_TABLE_2_1", 1L)
//					.build()
//				.build().getWrappedBranchNode()
//			);
//			
//			dbSchemaUtils.adaptSchema
//			(
//				DBSchemaBowFactory.createSchema(schemaName2, schemaName2)
//					.createOneOfTables().setName("MY_TABLE_A")
//						.createUUIDColumnDefaultAuto("ID").createPrimaryKey().build().build()
//						.createVarcharColumn("NAME", false, 128).build()
//					.build()
//					.createOneOfTables().setName("MY_TABLE_B")
//						.createUUIDColumnDefaultAuto("ID").createPrimaryKey().build().build()
//						.createUUIDColumn("MY_TABLE_A_ID",true)
//							.createForeignKey().setConstraintName("FK1_MY_TABLE_B").setReferencedTableName("MY_TABLE_A").setReferencedColumnName("ID").build().build()
//						.createBigIntAutoIncrementColumn("NUM", "SEQ_MA_TABLE_B_1", 1L)
//					.build()
//				.build().getWrappedBranchNode()
//			);
//			
//			connection.setAutoCommit(false);
//			
//			connection.setSchema(schemaName1);
//			
//			UUID newID1 = null; 
//			PreparedStatement insert1 = connection.prepareStatement("INSERT INTO MY_TABLE_1 (NAME) VALUES ('NAME 1')",Statement.RETURN_GENERATED_KEYS);
//			try
//			{
//				insert1.executeUpdate();
//				
//				ResultSet generatedKeys = insert1.getGeneratedKeys();
//				generatedKeys.next();
//				newID1 = UUID.fromString(generatedKeys.getString(1));
//				generatedKeys.close();
//			}
//			finally 
//			{
//				insert1.close();
//				insert1 = null;
//			}
//			
//			PreparedStatement insert2 = connection.prepareStatement("INSERT INTO MY_TABLE_2 (MY_TABLE_1_ID) VALUES (?)");
//			try
//			{
//				insert2.setObject(1, newID1);
//				insert2.executeUpdate();
//			}
//			finally 
//			{
//				insert2.close();
//				insert2 = null;
//			}
//			connection.commit();
//			
//			connection.setSchema(schemaName2);
//			
//			UUID newID2 = null; 
//			PreparedStatement insert3 = connection.prepareStatement("INSERT INTO MY_TABLE_A (NAME) VALUES ('NAME A')",Statement.RETURN_GENERATED_KEYS);
//			try
//			{
//				insert3.executeUpdate();
//				
//				ResultSet generatedKeys = insert3.getGeneratedKeys();
//				generatedKeys.next();
//				newID2 = UUID.fromString(generatedKeys.getString(1));
//				generatedKeys.close();
//			}
//			finally 
//			{
//				insert3.close();
//				insert3 = null;
//			}
//			
//			PreparedStatement insert4 = connection.prepareStatement("INSERT INTO MY_TABLE_B (MY_TABLE_A_ID) VALUES (?)");
//			try
//			{
//				insert4.setObject(1, newID2);
//				insert4.executeUpdate();
//				
//				insert4.setObject(1, newID2);
//				insert4.executeUpdate();
//			}
//			finally 
//			{
//				insert4.close();
//				insert4 = null;
//			}
//			connection.commit();
//			
//			File backupFile = databaseManagementSystemService.backup(null, propertyBuilder.getConnectionProperties(), directory, null, null);
//			databaseManagementSystemService.restore(null, propertyBuilder.getConnectionProperties().copy().setDbname(dbNameBackup), backupFile, null, null);
//			
//			Connection restoredConnection = databaseManagementSystemService.getConnection(propertyBuilder.getConnectionProperties().copy().setDbname(dbNameBackup));
//			try
//			{
//				restoredConnection.setAutoCommit(false);
//				
//				restoredConnection.setSchema(schemaName1);
//				connection.setSchema(schemaName1);
//				
//				insert2 = connection.prepareStatement("INSERT INTO MY_TABLE_2 (MY_TABLE_1_ID) VALUES (?)");
//				try
//				{
//					insert2.setObject(1, newID1);
//					insert2.executeUpdate();
//				}
//				finally 
//				{
//					insert2.close();
//					insert2 = null;
//				}
//				connection.commit();
//				
//				insert2 = restoredConnection.prepareStatement("INSERT INTO MY_TABLE_2 (MY_TABLE_1_ID) VALUES (?)");
//				try
//				{
//					insert2.setObject(1, newID1);
//					insert2.executeUpdate();
//				}
//				finally 
//				{
//					insert2.close();
//					insert2 = null;
//				}
//				restoredConnection.commit();
//				
//				compareTables(connection, restoredConnection, "MY_TABLE_1");
//				compareTables(connection, restoredConnection, "MY_TABLE_2");
//				
//				restoredConnection.setSchema(schemaName2);
//				connection.setSchema(schemaName2);
//				
//				insert4 = connection.prepareStatement("INSERT INTO MY_TABLE_B (MY_TABLE_A_ID) VALUES (?)");
//				try
//				{
//					insert4.setObject(1, newID2);
//					insert4.executeUpdate();
//					
//					insert4.setObject(1, newID2);
//					insert4.executeUpdate();
//				}
//				finally 
//				{
//					insert4.close();
//					insert4 = null;
//				}
//				connection.commit();
//				
//				insert4 = restoredConnection.prepareStatement("INSERT INTO MY_TABLE_B (MY_TABLE_A_ID) VALUES (?)");
//				try
//				{
//					insert4.setObject(1, newID2);
//					insert4.executeUpdate();
//					
//					insert4.setObject(1, newID2);
//					insert4.executeUpdate();
//				}
//				finally 
//				{
//					insert4.close();
//					insert4 = null;
//				}
//				restoredConnection.commit();
//				
//				compareTables(connection, restoredConnection, "MY_TABLE_A");
//				compareTables(connection, restoredConnection, "MY_TABLE_B");
//			}
//			finally 
//			{
//				restoredConnection.close();
//			}
//		}
//		finally
//		{
//			connection.close();
//		}
//	}
	
	public IDatabaseManagementSystemService getService()
	{
		Map<String,Object> properties = new HashMap<String, Object>();
		properties.put("TYPE", "H2");
		return Driver.getSingleDriver(IDatabaseManagementSystemService.class, properties);
	}
	
	public void compareTables(Connection origin, Connection restore, String table) throws SQLException, IOException
	{		
		try(CloseableCollector cc = CloseableCollector.newInstance())
		{
			ResultSet resultSetOrigin = cc.register((cc.register(origin.prepareStatement("select * from " + table + " order by id"))).executeQuery());
			ResultSet resultSetRestore = cc.register((cc.register(restore.prepareStatement("select * from " + table + " order by id"))).executeQuery());
			
			while(resultSetOrigin.next())
			{
				assertTrue("value should be correct",resultSetRestore.next());
				
				ResultSetMetaData metaOrigin = resultSetOrigin.getMetaData();
				ResultSetMetaData metaRestore = resultSetRestore.getMetaData();
				
				assertEquals("value should be correct", metaOrigin.getColumnCount(), metaRestore.getColumnCount());
				
				for(int i = 0; i < metaOrigin.getColumnCount(); i++)
				{
					String columnName = metaOrigin.getColumnName(i+1);
					assertEquals("value should be correct",resultSetOrigin.getString(columnName),resultSetOrigin.getString(columnName));
				}
				
			}
			assertFalse("table size of " + table +  "should be the same", resultSetRestore.next());
		}
	}
}
