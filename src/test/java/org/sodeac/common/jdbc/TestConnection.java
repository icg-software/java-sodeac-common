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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class TestConnection
{
    public TestConnection()
    {
        super();
    }
    
    public TestConnection(boolean enabled)
    {
        super();
        this.enabled = enabled;
    }
    
    public Connection connection;
    public boolean enabled = false;
    public String dbmsSchemaName = null;
    public String tableSpaceIndex = null;
    public String tableSpaceData = null;
    
    public DataSource getDataSource()
    {
        return new TestDataSource();
    }
    
    public class TestDataSource implements DataSource
    {
        
        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return null;
        }
        
        @Override
        public void setLogWriter(PrintWriter out) throws SQLException { }
        
        @Override
        public void setLoginTimeout(int seconds) throws SQLException { }
        
        @Override
        public int getLoginTimeout() throws SQLException
        {
            return 0;
        }
        
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            return null;
        }
        
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException
        {
            return null;
        }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException
        {
            return false;
        }
        
        @Override
        public Connection getConnection() throws SQLException
        {
            return new NonClosableConnection(TestConnection.this.connection);
        }
        
        @Override
        public Connection getConnection(String username, String password) throws SQLException
        {
            return TestConnection.this.connection;
        }
        
    }
}
