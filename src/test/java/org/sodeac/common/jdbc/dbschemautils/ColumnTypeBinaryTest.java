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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.jdbc.Statics;
import org.sodeac.common.jdbc.TestConnection;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.DBSchemaTreeModel;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ColumnTypeBinaryTest
{
    
    public static List<Object[]> connectionList = null;
    public static final Map<String, Boolean> createdSchema = new HashMap<String, Boolean>();
    
    private final String databaseID = "TESTDOMAIN";
    private final String table1Name = "TableColBin";
    private final String columnBinaryName = "col_binary";
    private final String columnBlobName = "col_blob";
    
    @Parameters
    public static List<Object[]> connections()
    {
        if (connectionList != null)
        {
            return connectionList;
        }
        return connectionList = Statics.connections(createdSchema, "dbschema");
    }
    
    public ColumnTypeBinaryTest(final Callable<TestConnection> connectionFactory)
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
    public void test000700binarySimpleTest() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        
        final Connection connection = this.testConnection.connection;
        final DBSchemaUtils dbSchemaUtils = DBSchemaUtils.get(connection);
        
        final BranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = DBSchemaTreeModel.newSchema(this.databaseID, this.testConnection.dbmsSchemaName);
        final BranchNode<DBSchemaNodeType, TableNodeType> table1 = schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name, this.table1Name);
        
        TableNodeType.createCharColumn(table1, "id", false, 36);
        TableNodeType.createBinaryColumn(table1, this.columnBinaryName, true);
        TableNodeType.createBlobColumn(table1, this.columnBlobName, true);
        
        dbSchemaUtils.adaptSchema(schema);
        
        PreparedStatement prepStat = null;
        final ResultSet rset = null;
        try
        {
            connection.setAutoCommit(false);
            
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (id) values (?)");
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
            catch (final Exception e) { }
            try
            {
                prepStat.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void test000701testBinaryBytes() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        final Connection connection = this.testConnection.connection;
        final boolean ac = connection.getAutoCommit();
        connection.setAutoCommit(false);
        
        final byte[] b = new byte[200];
        for (int i = 0; i < 200; i++)
        {
            b[i] = (byte) (i + 10);
        }
        
        final String id = UUID.randomUUID().toString();
        
        PreparedStatement prepStat = null;
        
        try
        {
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (id,col_binary) values (?,?) ");
            prepStat.setString(1, id);
            prepStat.setBytes(2, b);
            prepStat.executeUpdate();
            connection.commit();
        }
        finally
        {
            if (prepStat != null)
            {
                prepStat.close();
            }
        }
        
        ResultSet rset = null;
        prepStat = null;
        byte[] testByte = null;
        
        try
        {
            prepStat = connection.prepareStatement("select col_binary from " + this.table1Name + " where id = ? ");
            prepStat.setString(1, id);
            
            rset = prepStat.executeQuery();
            rset.next();
            testByte = rset.getBytes(1);
        }
        finally
        {
            if (rset != null)
            {
                rset.close();
            }
            if (prepStat != null)
            {
                prepStat.close();
            }
            
        }
        
        assertEquals("byte length should be correct", b.length, testByte.length);
        for (int i = 0; i < b.length; i++)
        {
            assertEquals("byte should be correct", b[i], testByte[i]);
        }
        
        connection.setAutoCommit(ac);
    }
    
    @Test
    public void test000702testBinaryStream() throws SQLException, ClassNotFoundException, IOException
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        final Connection connection = this.testConnection.connection;
        final boolean ac = connection.getAutoCommit();
        connection.setAutoCommit(false);
        
        final byte[] b = new byte[200];
        for (int i = 0; i < 200; i++)
        {
            b[i] = (byte) (i + 20);
        }
        
        final String id = UUID.randomUUID().toString();
        
        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        
        PreparedStatement prepStat = null;
        
        try
        {
            prepStat = connection.prepareStatement("insert into " + this.table1Name + " (id,col_binary) values (?,?) ");
            prepStat.setString(1, id);
            prepStat.setBinaryStream(2, bais);
            prepStat.executeUpdate();
            connection.commit();
            
            bais.close();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            if (prepStat != null)
            {
                prepStat.close();
            }
        }
        
        ResultSet rset = null;
        prepStat = null;
        byte[] testByte = null;
        InputStream is = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
        
        try
        {
            prepStat = connection.prepareStatement("select col_binary from " + this.table1Name + " where id = ? ");
            prepStat.setString(1, id);
            
            rset = prepStat.executeQuery();
            rset.next();
            is = rset.getBinaryStream(1);
            
            final byte[] buf = new byte[27];
            int len;
            while ((len = is.read(buf)) > 0)
            {
                baos.write(buf, 0, len);
            }
            is.close();
            baos.close();
            testByte = baos.toByteArray();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            if (rset != null)
            {
                rset.close();
            }
            if (prepStat != null)
            {
                prepStat.close();
            }
            
        }
        
        assertEquals("byte length should be correct", b.length, testByte.length);
        for (int i = 0; i < b.length; i++)
        {
            assertEquals("byte should be correct", b[i], testByte[i]);
        }
        
        connection.setAutoCommit(ac);
    }
    
    // TODO
	
	/*@Test
	public void test000703testBlobInsertAndRead() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		IDatabaseSchemaDriver driver = databaseSchemaProcessor.getDatabaseSchemaDriver(connection);
		
		SchemaSpec spec = new SchemaSpec(databaseID);
		spec.setDbmsSchemaName(testConnection.dbmsSchemaName);
		
		TableSpec table1 = spec.addTable(table1Name);
		
		table1.addColumn("id",IColumnType.ColumnType.CHAR.toString(),false,36);
		table1.addColumn(columnBinaryName, IColumnType.ColumnType.BINARY.toString());
		table1.addColumn(columnBlobName, IColumnType.ColumnType.BLOB.toString());
		
		databaseSchemaProcessor.checkSchemaSpec(spec, connection);
		
		boolean ac = connection.getAutoCommit();
		connection.setAutoCommit(false);
		
		byte[] b= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			b[i] = (byte)(i+ 20);
		}
		
		String id = UUID.randomUUID().toString();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		
		PreparedStatement prepStat = null;
		
		Blob blob = driver.createBlob(connection);
		OutputStream os = blob.setBinaryStream(1);
		
		byte[] buf = new byte[27];
		int len;
		while((len = bais.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			connection.commit();
			
			bais.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		ResultSet rset = null;
		prepStat = null;
		byte[] testByte = null;
		InputStream is = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
			is = blob.getBinaryStream();
			
			while((len = is.read(buf)) > 0)
			{
				baos.write(buf, 0, len);
			}
			is.close();
			baos.close();
			testByte = baos.toByteArray();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
		
		blob.free();
		
		assertEquals("byte length should be correct", b.length, testByte.length);
		for(int i = 0; i < b.length; i++)
		{
			assertEquals("byte should be correct", b[i], testByte[i]);
		}
		
		connection.setAutoCommit(ac);
	}
	
	@Test
	public void test000704testBlobCopyByReference() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		IDatabaseSchemaDriver driver = databaseSchemaProcessor.getDatabaseSchemaDriver(connection);
		
		boolean ac = connection.getAutoCommit();
		connection.setAutoCommit(false);
		
		byte[] b1= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			b1[i] = (byte)(i+ 20);
		}
		
		String id1 = UUID.randomUUID().toString();
		String id2 = UUID.randomUUID().toString();
		
		ByteArrayInputStream bais1 = new ByteArrayInputStream(b1);
		
		PreparedStatement prepStat = null;
		
		Blob blob = driver.createBlob(connection);
		OutputStream os = blob.setBinaryStream(1);
		
		byte[] buf = new byte[27];
		int len;
		while((len = bais1.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais1.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id1);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			connection.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		ResultSet rset = null;
		prepStat = null;
		byte[] testByte = null;
		InputStream is = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id1);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
	
		// write again in another row
		
		prepStat = null;
		try
		{
			
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id2);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			try
			{
				blob.free();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			connection.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		// change origin blob
		
		byte[] bx= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			bx[i] = (byte)(i+ 25);
		}
		
		bais1 = new ByteArrayInputStream(bx);
		blob = driver.createBlob(connection);
		os = blob.setBinaryStream(1);
		
		while((len = bais1.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais1.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("update "+ table1Name + " set col_blob = ?  where id = ? ");
			driver.setBlob(connection, prepStat, blob, 1);
			prepStat.setString(2, id1);
			prepStat.executeUpdate();
			connection.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		// read again
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id2);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
			is = blob.getBinaryStream();
			
			while((len = is.read(buf)) > 0)
			{
				baos.write(buf, 0, len);
			}
			is.close();
			baos.close();
			testByte = baos.toByteArray();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
		
		assertEquals("byte length should be correct", b1.length, testByte.length);
		for(int i = 0; i < b1.length; i++)
		{
			assertEquals("byte should be correct", b1[i], testByte[i]);
		}
		
		blob.free();
		connection.setAutoCommit(ac);
	}
	
	@Test
	public void test000705testBlobNull() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		IDatabaseSchemaDriver driver = databaseSchemaProcessor.getDatabaseSchemaDriver(connection);
		
		boolean ac = connection.getAutoCommit();
		connection.setAutoCommit(false);
		
		byte[] b1= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			b1[i] = (byte)(i+ 20);
		}
		
		String id1 = UUID.randomUUID().toString();
		
		ByteArrayInputStream bais1 = new ByteArrayInputStream(b1);
		
		PreparedStatement prepStat = null;
		
		Blob blob = driver.createBlob(connection);
		OutputStream os = blob.setBinaryStream(1);
		
		byte[] buf = new byte[27];
		int len;
		while((len = bais1.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais1.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id1);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			
			connection.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		try
		{
			prepStat = connection.prepareStatement("update  "+ table1Name + " set col_blob = ? where id = ? ");
			driver.cleanBlob(connection, blob);
			driver.setBlob(connection, prepStat, null, 1);
			prepStat.setString(2, id1);
			prepStat.executeUpdate();
			connection.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		ResultSet rset = null;
		prepStat = null;
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id1);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
			if(blob != null)
			{
				blob.free();
			}
			assertNull("blob schould be null", blob);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
				
		connection.setAutoCommit(ac);
	}

	@Test
	public void test000706testBlobInsertAndReadTwice() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		IDatabaseSchemaDriver driver = databaseSchemaProcessor.getDatabaseSchemaDriver(connection);
		
		SchemaSpec spec = new SchemaSpec(databaseID);
		spec.setDbmsSchemaName(testConnection.dbmsSchemaName);
		
		TableSpec table1 = spec.addTable(table1Name);
		
		table1.addColumn("id",IColumnType.ColumnType.CHAR.toString(),false,36);
		table1.addColumn(columnBinaryName, IColumnType.ColumnType.BINARY.toString());
		table1.addColumn(columnBlobName, IColumnType.ColumnType.BLOB.toString());
		
		databaseSchemaProcessor.checkSchemaSpec(spec, connection);
		
		boolean ac = connection.getAutoCommit();
		connection.setAutoCommit(false);
		
		byte[] b= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			b[i] = (byte)(i+ 20);
		}
		
		String id = UUID.randomUUID().toString();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		
		PreparedStatement prepStat = null;
		
		Blob blob = driver.createBlob(connection);
		OutputStream os = blob.setBinaryStream(1);
		
		byte[] buf = new byte[27];
		int len;
		while((len = bais.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			connection.commit();
			
			bais.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		ResultSet rset = null;
		prepStat = null;
		byte[] testByte = null;
		InputStream is = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
			is = blob.getBinaryStream();
			
			while((len = is.read(buf)) > 0)
			{
				baos.write(buf, 0, len);
			}
			is.close();
			baos.close();
			testByte = baos.toByteArray();
			
			baos = new ByteArrayOutputStream(200);
			is = blob.getBinaryStream();
			
			while((len = is.read(buf)) > 0)
			{
				baos.write(buf, 0, len);
			}
			is.close();
			baos.close();
			testByte = baos.toByteArray();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
		
		blob.free();
		
		assertEquals("byte length should be correct", b.length, testByte.length);
		for(int i = 0; i < b.length; i++)
		{
			assertEquals("byte should be correct", b[i], testByte[i]);
		}
		
		connection.setAutoCommit(ac);
	}
	
	@Test
	public void test000707testBlobInsertAndWriteReadedWithExc() throws SQLException, ClassNotFoundException, IOException 
	{
		if(! testConnection.enabled)
		{
			return;
		}
		Connection connection = testConnection.connection;
		IDatabaseSchemaDriver driver = databaseSchemaProcessor.getDatabaseSchemaDriver(connection);
		
		SchemaSpec spec = new SchemaSpec(databaseID);
		spec.setDbmsSchemaName(testConnection.dbmsSchemaName);
		
		TableSpec table1 = spec.addTable(table1Name);
		
		table1.addColumn("id",IColumnType.ColumnType.CHAR.toString(),false,36);
		table1.addColumn(columnBinaryName, IColumnType.ColumnType.BINARY.toString());
		table1.addColumn(columnBlobName, IColumnType.ColumnType.BLOB.toString());
		
		databaseSchemaProcessor.checkSchemaSpec(spec, connection);
		
		boolean ac = connection.getAutoCommit();
		connection.setAutoCommit(false);
		
		byte[] b= new byte[200];
		for(int i = 0; i < 200; i++ )
		{
			b[i] = (byte)(i+ 20);
		}
		
		String id = UUID.randomUUID().toString();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		
		PreparedStatement prepStat = null;
		
		Blob blob = driver.createBlob(connection);
		OutputStream os = blob.setBinaryStream(1);
		
		byte[] buf = new byte[27];
		int len;
		while((len = bais.read(buf)) > 0)
		{
			os.write(buf, 0, len);
		}
		bais.close();
		os.close();
		
		try
		{
			prepStat = connection.prepareStatement("insert into "+ table1Name + " (id,col_blob) values (?,?) ");
			prepStat.setString(1, id);
			driver.setBlob(connection, prepStat, blob, 2);
			prepStat.executeUpdate();
			connection.commit();
			
			bais.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw e;
		}
		finally 
		{
			if(prepStat != null)
			{
				prepStat.close();
			}
		}
		
		blob.free();
		
		ResultSet rset = null;
		prepStat = null;
		
		try
		{
			prepStat = connection.prepareStatement("select col_blob from "+ table1Name + " where id = ? ");
			prepStat.setString(1, id);
			
			rset = prepStat.executeQuery();
			rset.next();
			blob = driver.getBlob(connection, rset, 1);
			os = blob.setBinaryStream(1);
			os.write("Gehobener Zeigefinger: Das darf man aber nicht!".getBytes());
			os.flush();
			
			try
			{
				blob.free();
			}
			catch (Exception e) {}
			
			try
			{
				connection.setAutoCommit(ac);
			}
			catch (Exception e) {}
			fail("Expected SQLException to be thrown");
			return;
		}
		catch (Exception e) 
		{
			try
			{
				connection.rollback();
			}
			catch (Exception e2) {}
			// e.printStackTrace();
		}
		finally 
		{
			if(rset != null)
			{
				rset.close();
			}
			if(prepStat != null)
			{
				prepStat.close();
			}
			
		}
		
		try
		{
			blob.free();
		}
		catch (Exception e) {}
		
		try
		{
			connection.setAutoCommit(ac);
		}
		catch (Exception e) {}
	}*/
}
