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
    
    public boolean createDatabase(SystemProperties properties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    public boolean removeDatabase(SystemProperties properties, DatabaseProperties databaseProperties, ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    // Users/Roles/Tablespaces?
    
    public boolean createSchema(ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    public boolean removeSchema(ConnectionProperties connectionProperties) throws SQLException, IOException;
    
    public String getConnectionString(ConnectionProperties connectionProperties);
    
    public String getUser(ConnectionProperties connectionProperties);
    
    public String getPassword(ConnectionProperties connectionProperties);
    
    public File backup(SystemProperties properties, ConnectionProperties connectionProperties, File backupDirectory, File tempDirectory, String key, String... schemas) throws SQLException, IOException;
    
    public void restore(SystemProperties properties, ConnectionProperties connectionProperties, File backupFile, File tempDirectory, String key) throws SQLException, IOException;
    
    public default Connection getConnection(ConnectionProperties connectionProperties) throws SQLException
    {
        Objects.requireNonNull(connectionProperties);
        
        return DriverManager.getConnection
                                (
                                    getConnectionString(connectionProperties),
                                    getUser(connectionProperties),
                                    getPassword(connectionProperties)
                                );
    }
    
    public static class H2PropertyBuilder
    {
        public static final String PAGE_SIZE = "PAGE_SIZE";
        public static final String CACHE_SIZE = "CACHE_SIZE";
        
        private H2PropertyBuilder()
        {
            super();
        }
        
        public static H2PropertyBuilder newInstance(String directory, String dbName)
        {
            H2PropertyBuilder builder = new H2PropertyBuilder();
            try
            {
                builder.connectionProperties = new ConnectionProperties().setDirectory(new File(directory).getCanonicalPath()).setDbname(dbName);
            }
            catch (IOException e)
            {
                throw new RuntimeWrappedException(e);
            }
            
            return builder;
        }
        
        public H2PropertyBuilder setConnectionUsername(String username)
        {
            this.connectionProperties.setUsername(username);
            return this;
        }
        
        public H2PropertyBuilder setConnectionPassword(String password)
        {
            this.connectionProperties.setPassword(password);
            return this;
        }
        
        public H2PropertyBuilder setConnectionEncryptionKey(String encryptionKey)
        {
            this.connectionProperties.setEncryptionKey(encryptionKey);
            return this;
        }
        
        public H2PropertyBuilder setCacheSizeInKB(int sizeInKB)
        {
            this.connectionProperties.getProperties().put(CACHE_SIZE, Integer.toString(sizeInKB));
            return this;
        }
        
        public H2PropertyBuilder setPageSize(int page)
        {
            this.connectionProperties.getProperties().put(PAGE_SIZE, Integer.toString(page));
            return this;
        }
        
        private ConnectionProperties connectionProperties = null;
        private SystemProperties systemProperties = null;
        private DatabaseProperties databaseProperties = null;
        
        public ConnectionProperties getConnectionProperties()
        {
            return connectionProperties;
        }
        
        public SystemProperties getSystemProperties()
        {
            return systemProperties;
        }
        
        public DatabaseProperties getDatabaseProperties()
        {
            return databaseProperties;
        }
        
    }
    
    public static class SystemProperties
    {
        public enum ConnectionProtocol
        {LOCAL, SSH, JCLOUD, K8, CUSTOM}
        
        private ConnectionProtocol connectionProtocol = ConnectionProtocol.LOCAL;
        private String server = null;
        private String port = null;
        private String user = null;
        private String password = null;
        private String installLocation = null;
        private Map<String, String> properties = new HashMap<String, String>();
        
        public ConnectionProtocol getConnectionProtocol()
        {
            return connectionProtocol;
        }
        
        public SystemProperties setConnectionProtocol(ConnectionProtocol connectionProtocol)
        {
            this.connectionProtocol = connectionProtocol;
            return this;
        }
        
        public String getServer()
        {
            return server;
        }
        
        public SystemProperties setServer(String server)
        {
            this.server = server;
            return this;
        }
        
        public String getPort()
        {
            return port;
        }
        
        public SystemProperties setPort(String port)
        {
            this.port = port;
            return this;
        }
        
        public String getUser()
        {
            return user;
        }
        
        public SystemProperties setUser(String user)
        {
            this.user = user;
            return this;
        }
        
        public String getPassword()
        {
            return password;
        }
        
        public SystemProperties setPassword(String password)
        {
            this.password = password;
            return this;
        }
        
        public String getInstallLocation()
        {
            return installLocation;
        }
        
        public SystemProperties setInstallLocation(String installLocation)
        {
            this.installLocation = installLocation;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return properties;
        }
        
        public SystemProperties fillProperties(Consumer<Map<String, String>> propertiesWriter)
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
    
    public static class DatabaseProperties
    {
        private String owner = null;
        private String defaultTablespace = null;
        private String charset = null;
        private int connectionLimit = -1;
        private Map<String, String> properties = new HashMap<String, String>();
        
        public String getOwner()
        {
            return owner;
        }
        
        public DatabaseProperties setOwner(String owner)
        {
            this.owner = owner;
            return this;
        }
        
        public String getDefaultTablespace()
        {
            return defaultTablespace;
        }
        
        public DatabaseProperties setDefaultTablespace(String defaultTablespace)
        {
            this.defaultTablespace = defaultTablespace;
            return this;
        }
        
        public int getConnectionLimit()
        {
            return connectionLimit;
        }
        
        public DatabaseProperties setConnectionLimit(int connectionLimit)
        {
            this.connectionLimit = connectionLimit;
            return this;
        }
        
        public String getCharset()
        {
            return charset;
        }
        
        public DatabaseProperties setCharset(String charset)
        {
            this.charset = charset;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return properties;
        }
        
        public DatabaseProperties fillProperties(Consumer<Map<String, String>> propertiesWriter)
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
    
    public static class ConnectionProperties
    {
        private String dbname = null;
        private String username = null;
        private String password = null;
        private String encryptionKey = null;
        private String schema = null;
        private String server = null;
        private String port = null;
        private String directory = null;
        private Map<String, String> properties = new HashMap<String, String>();
        
        public String getDbname()
        {
            return dbname;
        }
        
        public ConnectionProperties setDbname(String dbname)
        {
            this.dbname = dbname;
            return this;
        }
        
        public String getSchema()
        {
            return schema;
        }
        
        public ConnectionProperties setSchema(String schema)
        {
            this.schema = schema;
            return this;
        }
        
        public String getUsername()
        {
            return username;
        }
        
        public ConnectionProperties setUsername(String username)
        {
            this.username = username;
            return this;
        }
        
        public String getPassword()
        {
            return password;
        }
        
        public ConnectionProperties setPassword(String password)
        {
            this.password = password;
            return this;
        }
        
        public String getEncryptionKey()
        {
            return encryptionKey;
        }
        
        public ConnectionProperties setEncryptionKey(String encryptionKey)
        {
            this.encryptionKey = encryptionKey;
            return this;
        }
        
        public String getServer()
        {
            return server;
        }
        
        public ConnectionProperties setServer(String server)
        {
            this.server = server;
            return this;
        }
        
        public String getPort()
        {
            return port;
        }
        
        public ConnectionProperties setPort(String port)
        {
            this.port = port;
            return this;
        }
        
        public String getDirectory()
        {
            return directory;
        }
        
        public ConnectionProperties setDirectory(String directory)
        {
            this.directory = directory;
            return this;
        }
        
        public Map<String, String> getProperties()
        {
            return properties;
        }
        
        public ConnectionProperties fillProperties(Consumer<Map<String, String>> propertiesWriter)
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
