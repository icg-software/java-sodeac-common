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
package org.sodeac.common;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.RuntimeWrappedException;

public interface IDatabaseManagementSystemService extends IDriver
{
    // Installer ?
    
    boolean createDatabase(SystemProperties properties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    boolean removeDatabase(SystemProperties properties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    // Users/Roles/Tablespaces?
    
    boolean createSchema(ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    boolean removeSchema(ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    String getConnectionString(ConnectionProperties connectionProperties);
    
    String getUser(ConnectionProperties connectionProperties);
    
    String getPassword(ConnectionProperties connectionProperties);
    
    File backup(SystemProperties properties, ConnectionProperties connectionProperties, File backupDirectory, File tempDirectory, String key, String... schemas) throws SQLException, IOException;
    
    void restore(SystemProperties properties, ConnectionProperties connectionProperties, File backupFile, File tempDirectory, String key) throws SQLException, IOException;
    
    default Connection getConnection(final ConnectionProperties connectionProperties) throws SQLException
    {
        Objects.requireNonNull(connectionProperties);
        
        return DriverManager.getConnection
                                (
                                    getConnectionString(connectionProperties),
                                    getUser(connectionProperties),
                                    getPassword(connectionProperties)
                                );
    }
    
    class H2PropertyBuilder
    {
        public static final String PAGE_SIZE = "PAGE_SIZE";
        public static final String CACHE_SIZE = "CACHE_SIZE";
        
        private H2PropertyBuilder()
        {
            super();
        }
        
        public static H2PropertyBuilder newInstance(final String directory, final String dbName)
        {
            H2PropertyBuilder builder = new H2PropertyBuilder();
            try
            {
                builder.connectionProperties = new ConnectionProperties().setDirectory(new File(directory).getCanonicalPath()).setDbname(dbName);
            }
            catch (final IOException e)
            {
                throw new RuntimeWrappedException(e);
            }
            
            return builder;
        }
        
        public H2PropertyBuilder setConnectionUsername(final String username)
        {
            this.connectionProperties.setUsername(username);
            return this;
        }
        
        public H2PropertyBuilder setConnectionPassword(final String password)
        {
            this.connectionProperties.setPassword(password);
            return this;
        }
        
        public H2PropertyBuilder setConnectionEncryptionKey(final String encryptionKey)
        {
            this.connectionProperties.setEncryptionKey(encryptionKey);
            return this;
        }
        
        public H2PropertyBuilder setCacheSizeInKB(final int sizeInKB)
        {
            this.connectionProperties.getProperties().put(CACHE_SIZE, Integer.toString(sizeInKB));
            return this;
        }
        
        public H2PropertyBuilder setPageSize(final int page)
        {
            this.connectionProperties.getProperties().put(PAGE_SIZE, Integer.toString(page));
            return this;
        }
        
        private ConnectionProperties connectionProperties = null;
        private final SystemProperties systemProperties = null;
        private final DatabaseProperties databaseProperties = null;
        
        public ConnectionProperties getConnectionProperties()
        {
            return this.connectionProperties;
        }
        
        public SystemProperties getSystemProperties()
        {
            return this.systemProperties;
        }
        
        public DatabaseProperties getDatabaseProperties()
        {
            return this.databaseProperties;
        }
        
    }
    
    class SystemProperties
    {
        public enum ConnectionProtocol
        {LOCAL, SSH, JCLOUD, K8, CUSTOM}
        
        private ConnectionProtocol connectionProtocol = ConnectionProtocol.LOCAL;
        private String server = null;
        private String port = null;
        private String user = null;
        private String password = null;
        private String installLocation = null;
        private final Map<String, String> properties = new HashMap<String, String>();
        
        public ConnectionProtocol getConnectionProtocol()
        {
            return this.connectionProtocol;
        }
        
        public SystemProperties setConnectionProtocol(final ConnectionProtocol connectionProtocol)
        {
            this.connectionProtocol = connectionProtocol;
            return this;
        }
        
        public String getServer()
        {
            return this.server;
        }
        
        public SystemProperties setServer(final String server)
        {
            this.server = server;
            return this;
        }
        
        public String getPort()
        {
            return this.port;
        }
        
        public SystemProperties setPort(final String port)
        {
            this.port = port;
            return this;
        }
        
        public String getUser()
        {
            return this.user;
        }
        
        public SystemProperties setUser(final String user)
        {
            this.user = user;
            return this;
        }
        
        public String getPassword()
        {
            return this.password;
        }
        
        public SystemProperties setPassword(final String password)
        {
            this.password = password;
            return this;
        }
        
        public String getInstallLocation()
        {
            return this.installLocation;
        }
        
        public SystemProperties setInstallLocation(final String installLocation)
        {
            this.installLocation = installLocation;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return this.properties;
        }
        
        public SystemProperties fillProperties(final Consumer<Map<String, String>> propertiesWriter)
        {
            if (propertiesWriter == null)
            {
                return this;
            }
            propertiesWriter.accept(this.properties);
            return this;
        }
        
        public SystemProperties copy()
        {
            SystemProperties copy = new SystemProperties()
                .setServer(this.server)
                .setPort(this.port)
                .setUser(this.user)
                .setPassword(this.password)
                .setInstallLocation(this.installLocation);
            
            copy.getProperties().putAll(this.properties);
            
            return copy;
        }
    }
    
    class DatabaseProperties
    {
        private String owner = null;
        private String defaultTablespace = null;
        private String charset = null;
        private int connectionLimit = -1;
        private final Map<String, String> properties = new HashMap<String, String>();
        
        public String getOwner()
        {
            return this.owner;
        }
        
        public DatabaseProperties setOwner(final String owner)
        {
            this.owner = owner;
            return this;
        }
        
        public String getDefaultTablespace()
        {
            return this.defaultTablespace;
        }
        
        public DatabaseProperties setDefaultTablespace(final String defaultTablespace)
        {
            this.defaultTablespace = defaultTablespace;
            return this;
        }
        
        public int getConnectionLimit()
        {
            return this.connectionLimit;
        }
        
        public DatabaseProperties setConnectionLimit(final int connectionLimit)
        {
            this.connectionLimit = connectionLimit;
            return this;
        }
        
        public String getCharset()
        {
            return this.charset;
        }
        
        public DatabaseProperties setCharset(final String charset)
        {
            this.charset = charset;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return this.properties;
        }
        
        public DatabaseProperties fillProperties(final Consumer<Map<String, String>> propertiesWriter)
        {
            if (propertiesWriter == null)
            {
                return this;
            }
            propertiesWriter.accept(this.properties);
            return this;
        }
        
        public DatabaseProperties copy()
        {
            DatabaseProperties copy = new DatabaseProperties()
                .setCharset(this.charset)
                .setOwner(this.owner)
                .setDefaultTablespace(this.defaultTablespace)
                .setConnectionLimit(this.connectionLimit);
            
            copy.getProperties().putAll(this.properties);
            
            return copy;
        }
    }
    
    class ConnectionProperties
    {
        private String dbname = null;
        private String username = null;
        private String password = null;
        private String encryptionKey = null;
        private String schema = null;
        private String server = null;
        private String port = null;
        private String directory = null;
        private final Map<String, String> properties = new HashMap<String, String>();
        
        public String getDbname()
        {
            return this.dbname;
        }
        
        public ConnectionProperties setDbname(final String dbname)
        {
            this.dbname = dbname;
            return this;
        }
        
        public String getSchema()
        {
            return this.schema;
        }
        
        public ConnectionProperties setSchema(final String schema)
        {
            this.schema = schema;
            return this;
        }
        
        public String getUsername()
        {
            return this.username;
        }
        
        public ConnectionProperties setUsername(final String username)
        {
            this.username = username;
            return this;
        }
        
        public String getPassword()
        {
            return this.password;
        }
        
        public ConnectionProperties setPassword(final String password)
        {
            this.password = password;
            return this;
        }
        
        public String getEncryptionKey()
        {
            return this.encryptionKey;
        }
        
        public ConnectionProperties setEncryptionKey(final String encryptionKey)
        {
            this.encryptionKey = encryptionKey;
            return this;
        }
        
        public String getServer()
        {
            return this.server;
        }
        
        public ConnectionProperties setServer(final String server)
        {
            this.server = server;
            return this;
        }
        
        public String getPort()
        {
            return this.port;
        }
        
        public ConnectionProperties setPort(final String port)
        {
            this.port = port;
            return this;
        }
        
        public String getDirectory()
        {
            return this.directory;
        }
        
        public ConnectionProperties setDirectory(final String directory)
        {
            this.directory = directory;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return this.properties;
        }
        
        public ConnectionProperties fillProperties(final Consumer<Map<String, String>> propertiesWriter)
        {
            if (propertiesWriter == null)
            {
                return this;
            }
            propertiesWriter.accept(this.properties);
            return this;
        }
        
        public ConnectionProperties copy()
        {
            ConnectionProperties copy = new ConnectionProperties()
                .setDbname(this.dbname)
                .setUsername(this.username)
                .setPassword(this.password)
                .setEncryptionKey(this.encryptionKey)
                .setSchema(this.schema)
                .setServer(this.server)
                .setPort(this.port)
                .setDirectory(this.directory);
            
            copy.getProperties().putAll(this.properties);
            
            return copy;
        }
    }
}
