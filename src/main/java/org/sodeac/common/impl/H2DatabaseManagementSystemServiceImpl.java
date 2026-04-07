/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.sodeac.common.IDatabaseManagementSystemService;
import org.sodeac.common.misc.CommonConsumer;
import org.sodeac.common.misc.Converter;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.RuntimeWrappedException;

@Component(name = "H2-DBMS-Service", service = IDatabaseManagementSystemService.class, property = { "type=h2" })
public class H2DatabaseManagementSystemServiceImpl implements IDatabaseManagementSystemService
{
    
    @Override
    public int driverIsApplicableFor(Map<String, Object> properties)
    {
        boolean wrongType = true;
        
        if ((properties.get("type") != null) && ("H2".equalsIgnoreCase(properties.get("type").toString())))
        {
            wrongType = false;
        }
        else if ((properties.get("TYPE") != null) && ("H2".equalsIgnoreCase(properties.get("TYPE").toString())))
        {
            wrongType = false;
        }
        
        if (wrongType)
        {
            return IDriver.APPLICABLE_NONE;
        }
        
        SystemProperties systemProperties = (SystemProperties) properties.get(SystemProperties.class.getCanonicalName());
        
        if (systemProperties != null)
        {
            if (systemProperties.getConnectionProtocol() != SystemProperties.ConnectionProtocol.LOCAL)
            {
                return IDriver.APPLICABLE_NONE;
            }
        }
        return IDriver.APPLICABLE_DEFAULT;
    }
    
    @Override
    public boolean createDatabase(SystemProperties systemProperties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties) throws SQLException
    {
        Objects.requireNonNull(connectionProperties, "ConnectionProperties are requiered");
        Objects.requireNonNull(connectionProperties.getDbname(), "db name is requiered");
        if (connectionProperties.getDbname().isEmpty())
        {
            throw new IllegalStateException("db name is required");
        }
        
        if ((systemProperties != null) && (systemProperties.getConnectionProtocol() != null) && (systemProperties.getConnectionProtocol() != SystemProperties.ConnectionProtocol.LOCAL))
        {
            throw new IllegalStateException("Only local access is supported");
        }
        String directory = "./";
        if ((connectionProperties.getDirectory() != null) && (!directory.isEmpty()))
        {
            try
            {
                directory = connectionProperties.getDirectory();
            }
            catch (Exception e)
            {
                throw new RuntimeWrappedException(e);
            }
        }
        
        File directoryFile = new File(directory);
        if (!directoryFile.exists())
        {
            directoryFile.mkdirs();
        }
        
        Connection connection = DriverManager.getConnection(this.getConnectionString(connectionProperties), this.getUser(connectionProperties), this.getPassword(connectionProperties));
        connection.close();
        
        return true;
    }
    
    @Override
    public String getUser(ConnectionProperties connectionProperties)
    {
        if (connectionProperties == null)
        {
            return null;
        }
        return connectionProperties.getUsername();
    }
    
    @Override
    public String getPassword(ConnectionProperties connectionProperties)
    {
        if (connectionProperties == null)
        {
            return null;
        }
        if (connectionProperties.getEncryptionKey() == null || connectionProperties.getEncryptionKey().isEmpty())
        {
            return connectionProperties.getPassword();
        }
        if (connectionProperties.getPassword() == null)
        {
            return connectionProperties.getEncryptionKey() + " ";
        }
        return connectionProperties.getEncryptionKey() + " " + connectionProperties.getPassword();
    }
    
    @Override
    public boolean removeDatabase(SystemProperties systemProperties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties)
    {
        Objects.requireNonNull(connectionProperties, "ConnectionProperties are requiered");
        Objects.requireNonNull(connectionProperties.getDbname(), "db name is requiered");
        if (connectionProperties.getDbname().isEmpty())
        {
            throw new IllegalStateException("db name is required");
        }
        
        if ((systemProperties != null) && (systemProperties.getConnectionProtocol() != null) && (systemProperties.getConnectionProtocol() != SystemProperties.ConnectionProtocol.LOCAL))
        {
            throw new IllegalStateException("Only local access is supported");
        }
        String directory = "./";
        if ((connectionProperties.getDirectory() != null) && (!directory.isEmpty()))
        {
            try
            {
                directory = connectionProperties.getDirectory();
            }
            catch (Exception e)
            {
                throw new RuntimeWrappedException(e);
            }
        }
        
        File directoryFile = new File(directory);
        if (!directoryFile.exists())
        {
            return true;
        }
        
        boolean failed = false;
        
        for (File file : directoryFile.listFiles())
        {
            if (file.isDirectory())
            {
                if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".lobs.db"))
                {
                    for (File lob : file.listFiles())
                    {
                        if (lob.getName().toLowerCase().endsWith(".lob.db")) // lob
                        {
                            if (!lob.delete())
                            {
                                failed = true;
                            }
                        }
                    }
                    file.delete();
                }
                continue;
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".mv.db")) // MVStore DB
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".newFile")) // MVStore compaction temp file
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".test.tempFile")) // MVStore compaction temp file
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".h2.db")) // PageStore DB
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".lock.db")) // Lockfile
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().equalsIgnoreCase(connectionProperties.getDbname() + ".trace.db")) // Tracefile
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
            if (file.getName().toLowerCase().startsWith(connectionProperties.getDbname().toLowerCase() + ".") && file.getName().toLowerCase().endsWith(".temp.db")) // temp file for blobs and large RSs
            {
                if (!file.delete())
                {
                    failed = true;
                }
            }
        }
        
        return failed;
    }
    
    @Override
    public boolean createSchema(ConnectionProperties connectionProperties) throws SQLException
    {
        Objects.requireNonNull(connectionProperties);
        Objects.requireNonNull(connectionProperties.getSchema());
        if (connectionProperties.getSchema().isEmpty())
        {
            Objects.requireNonNull(null);
        }
        
        Connection connection = DriverManager.getConnection
                                                 (
                                                     getConnectionString(connectionProperties),
                                                     getUser(connectionProperties),
                                                     getPassword(connectionProperties)
                                                 );
        try
        {
            String sql = "CREATE SCHEMA IF NOT EXISTS " + connectionProperties.getSchema().toUpperCase() + " AUTHORIZATION " + connection.getMetaData().getUserName();
            PreparedStatement prepStat = connection.prepareStatement(sql);
            try
            {
                prepStat.executeUpdate();
            }
            finally
            {
                prepStat.close();
            }
        }
        finally
        {
            connection.close();
        }
        
        return true;
    }
    
    @Override
    public boolean removeSchema(ConnectionProperties connectionProperties) throws SQLException
    {
        Objects.requireNonNull(connectionProperties);
        Objects.requireNonNull(connectionProperties.getSchema());
        if (connectionProperties.getSchema().isEmpty())
        {
            Objects.requireNonNull(null);
        }
        
        Connection connection = DriverManager.getConnection
                                                 (
                                                     getConnectionString(connectionProperties),
                                                     getUser(connectionProperties),
                                                     getPassword(connectionProperties)
                                                 );
        try
        {
            PreparedStatement prepStat = connection.prepareStatement("DROP SCHEMA " + connectionProperties.getSchema().toUpperCase() + " CASCADE ");
            try
            {
                prepStat.executeUpdate();
            }
            finally
            {
                prepStat.close();
            }
        }
        finally
        {
            connection.close();
        }
        
        return true;
    }
    
    @Override
    public String getConnectionString(ConnectionProperties connectionProperties)
    {
        Objects.requireNonNull(connectionProperties, "connection properties are requiered");
        
        Objects.requireNonNull(connectionProperties.getDbname(), "db name is requiered");
        if (connectionProperties.getDbname().isEmpty())
        {
            throw new IllegalStateException("db name is required");
        }
        
        String directory = "./";
        if ((connectionProperties.getDirectory() != null) && (!directory.isEmpty()))
        {
            directory = connectionProperties.getDirectory();
        }
        
        try
        {
            File directoryFile = new File(directory);
            directory = directoryFile.getCanonicalPath();
        }
        catch (Exception e)
        {
            throw new RuntimeWrappedException(e);
        }
        
        StringBuilder connectionString = new StringBuilder("jdbc:h2:");
        
        if (connectionProperties.getDirectory().endsWith(".zip") || connectionProperties.getDirectory().endsWith(".ZIP"))
        {
            connectionString.append("zip:" + directory + "!/" + connectionProperties.getDbname());
        }
        else
        {
            connectionString.append("file:" + directory + "/" + connectionProperties.getDbname());
        }
        
        if ((connectionProperties.getEncryptionKey() != null) && (!connectionProperties.getEncryptionKey().isEmpty()))
        {
            connectionString.append(";CIPHER=AES");
        }
        if ((connectionProperties.getProperties().get(H2PropertyBuilder.CACHE_SIZE) != null) && (!connectionProperties.getProperties().get(H2PropertyBuilder.CACHE_SIZE).isEmpty()))
        {
            connectionString.append(";CACHE_SIZE=" + Integer.parseInt(connectionProperties.getProperties().get(H2PropertyBuilder.CACHE_SIZE)));
        }
        if ((connectionProperties.getProperties().get(H2PropertyBuilder.PAGE_SIZE) != null) && (!connectionProperties.getProperties().get(H2PropertyBuilder.PAGE_SIZE).isEmpty()))
        {
            connectionString.append(";PAGE_SIZE=" + Integer.parseInt(connectionProperties.getProperties().get(H2PropertyBuilder.PAGE_SIZE)));
        }
        return connectionString.toString();
    }
    
    @Override
    public File backup(SystemProperties properties, ConnectionProperties connectionProperties, File backupDirectory, File tempDirectory, String key, String... schemas) throws SQLException, IOException
    {
        Objects.requireNonNull(connectionProperties, "connection properties are requiered");
        if (connectionProperties.getDbname().isEmpty())
        {
            throw new IllegalStateException("db name is required");
        }
        
        Objects.requireNonNull(backupDirectory, "backup directory is required");
        if (!backupDirectory.exists())
        {
            backupDirectory.mkdirs();
        }
        
        if (tempDirectory == null)
        {
            tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        }
        if (!tempDirectory.exists())
        {
            tempDirectory.mkdirs();
        }
        
        Connection connection = getConnection(connectionProperties);
        try
        {
            File cloneFile = new File(tempDirectory, UUID.randomUUID().toString() + ".zip");
            try
            {
                Date now = new Date();
                PreparedStatement preparedStatementClone = connection.prepareStatement("BACKUP TO '" + cloneFile.getCanonicalPath() + "' ");
                try
                {
                    preparedStatementClone.executeUpdate();
                }
                finally
                {
                    preparedStatementClone.close();
                }
                
                StringBuilder schemaPart = new StringBuilder();
                
                if ((schemas != null) && (schemas.length > 0))
                {
                    for (String schema : schemas)
                    {
                        if (schema == null)
                        {
                            continue;
                        }
                        if (schema.isEmpty())
                        {
                            continue;
                        }
                        
                        schemaPart.append("--" + schema.toLowerCase());
                    }
                }
                
                String filename = "db-backup-" + connectionProperties.getDbname().toLowerCase() + schemaPart.toString();
                
                if (filename.length() > 200)
                {
                    filename = filename.substring(0, 197) + "...";
                }
                
                if (schemaPart.toString().isEmpty())
                {
                    filename += "-";
                }
                else
                {
                    filename += "--";
                }
                
                filename += new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(now) + ".h2.gz.enc";
                
                File backupFile = new File(backupDirectory, filename);
                
                OutputStream backupOutputStream = Converter.OutputStreamToCryptedOutputStream.apply(new FileOutputStream(backupFile), key);
                try
                {
                    ConnectionProperties cloneProperties = connectionProperties.copy().setDirectory(cloneFile.getCanonicalPath());
                    Connection cloneConnection = getConnection(cloneProperties);
                    
                    try
                    {
                        StringBuilder schemaQuery = new StringBuilder();
                        
                        if ((schemas != null) && (schemas.length > 0))
                        {
                            for (String schema : schemas)
                            {
                                if (schema == null)
                                {
                                    continue;
                                }
                                if (schema.isEmpty())
                                {
                                    continue;
                                }
                                
                                if (schemaQuery.toString().isEmpty())
                                {
                                    schemaQuery.append(" SCHEMA");
                                }
                                
                                schemaQuery.append(" " + schema);
                            }
                        }
                        PreparedStatement preparedStatementScript = cloneConnection.prepareStatement("SCRIPT DROP BLOCKSIZE 8096" + schemaQuery.toString());
                        try
                        {
                            ResultSet resultSet = preparedStatementScript.executeQuery();
                            try
                            {
                                while (resultSet.next())
                                {
                                    backupOutputStream.write((resultSet.getString(1) + "\n").getBytes());
                                }
                            }
                            finally
                            {
                                resultSet.close();
                            }
                        }
                        finally
                        {
                            preparedStatementScript.close();
                        }
                    }
                    finally
                    {
                        cloneConnection.close();
                    }
                    
                    return backupFile;
                }
                finally
                {
                    backupOutputStream.close();
                }
            }
            finally
            {
                if (cloneFile.exists())
                {
                    if (!cloneFile.delete())
                    {
                        cloneFile.deleteOnExit();
                    }
                }
            }
        }
        finally
        {
            connection.close();
        }
    }
    
    @Override
    public void restore(SystemProperties properties, ConnectionProperties connectionProperties, File backupFile, File tempDirectory, String key) throws SQLException, IOException
    {
        Objects.requireNonNull(connectionProperties, "connection properties are requiered");
        if (connectionProperties.getDbname().isEmpty())
        {
            throw new IllegalStateException("db name is required");
        }
        
        Objects.requireNonNull(backupFile, "backup file is required");
        if (!backupFile.exists())
        {
            backupFile = null;
            Objects.requireNonNull(backupFile, "backup file is required");
        }
        
        if (tempDirectory == null)
        {
            tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        }
        if (!tempDirectory.exists())
        {
            tempDirectory.mkdirs();
        }
        
        Connection connectionRestore = getConnection(connectionProperties);
        try
        {
            connectionRestore.setAutoCommit(false);
            File tempRestore = new File(tempDirectory, "restore-" + UUID.randomUUID().toString() + ".sql");
            try
            {
                CommonConsumer.CopyStreamAndClose.accept
                                                     (
                                                         Converter.CryptedInputStreamToInputStream.apply(new FileInputStream(backupFile), null),
                                                         new FileOutputStream(tempRestore)
                                                     );
                
                PreparedStatement preparedStatement = connectionRestore.prepareStatement("RUNSCRIPT FROM '" + tempRestore.getCanonicalPath() + "'");
                try
                {
                    preparedStatement.executeUpdate();
                }
                finally
                {
                    preparedStatement.close();
                }
                connectionRestore.commit();
            }
            finally
            {
                if (tempDirectory.exists())
                {
                    if (!tempRestore.delete())
                    {
                        tempRestore.deleteOnExit();
                    }
                }
            }
        }
        finally
        {
            connectionRestore.close();
        }
    }
    
}
