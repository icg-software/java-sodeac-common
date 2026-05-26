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
package org.sodeac.common.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Date;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.sodeac.common.function.ExceptionCatchedBiFunction;

public class JDBCToolbox
{
    public static final BiFunction<ResultSet, String, String> StringResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                      {
                                                                                                                          String value = rs.getString(col);
                                                                                                                          if (rs.wasNull())
                                                                                                                          {
                                                                                                                              return null;
                                                                                                                          }
                                                                                                                          return value;
                                                                                                                      });
    
    public static final BiFunction<ResultSet, String, UUID> UUIDResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                  {
                                                                                                                      UUID value = (UUID) rs.getObject(col);
                                                                                                                      if (rs.wasNull())
                                                                                                                      {
                                                                                                                          return null;
                                                                                                                      }
                                                                                                                      return value;
                                                                                                                  });
    
    public static final BiFunction<ResultSet, String, Boolean> BooleanResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                        {
                                                                                                                            Boolean value = rs.getBoolean(col);
                                                                                                                            if (rs.wasNull())
                                                                                                                            {
                                                                                                                                return null;
                                                                                                                            }
                                                                                                                            return value;
                                                                                                                        });
    
    public static final BiFunction<ResultSet, String, Byte> ByteResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                  {
                                                                                                                      Byte value = rs.getByte(col);
                                                                                                                      if (rs.wasNull())
                                                                                                                      {
                                                                                                                          return null;
                                                                                                                      }
                                                                                                                      return value;
                                                                                                                  });
    
    public static final BiFunction<ResultSet, String, Short> ShortResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                    {
                                                                                                                        Short value = rs.getShort(col);
                                                                                                                        if (rs.wasNull())
                                                                                                                        {
                                                                                                                            return null;
                                                                                                                        }
                                                                                                                        return value;
                                                                                                                    });
    
    public static final BiFunction<ResultSet, String, Integer> IntegerResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                        {
                                                                                                                            Integer value = rs.getInt(col);
                                                                                                                            if (rs.wasNull())
                                                                                                                            {
                                                                                                                                return null;
                                                                                                                            }
                                                                                                                            return value;
                                                                                                                        });
    
    public static final BiFunction<ResultSet, String, Long> LongResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                  {
                                                                                                                      Long value = rs.getLong(col);
                                                                                                                      if (rs.wasNull())
                                                                                                                      {
                                                                                                                          return null;
                                                                                                                      }
                                                                                                                      return value;
                                                                                                                  });
    
    public static final BiFunction<ResultSet, String, Float> FloatResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                    {
                                                                                                                        Float value = rs.getFloat(col);
                                                                                                                        if (rs.wasNull())
                                                                                                                        {
                                                                                                                            return null;
                                                                                                                        }
                                                                                                                        return value;
                                                                                                                    });
    
    public static final BiFunction<ResultSet, String, Double> DoubleResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                      {
                                                                                                                          Double value = rs.getDouble(col);
                                                                                                                          if (rs.wasNull())
                                                                                                                          {
                                                                                                                              return null;
                                                                                                                          }
                                                                                                                          return value;
                                                                                                                      });
    
    public static final BiFunction<ResultSet, String, Date> DateResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                  {
                                                                                                                      Date value = rs.getDate(col);
                                                                                                                      if ((value == null) || rs.wasNull())
                                                                                                                      {
                                                                                                                          return null;
                                                                                                                      }
                                                                                                                      return new Date(value.getTime());
                                                                                                                  });
    
    public static final BiFunction<ResultSet, String, Date> TimeResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                  {
                                                                                                                      Date value = rs.getTime(col);
                                                                                                                      if ((value == null) || rs.wasNull())
                                                                                                                      {
                                                                                                                          return null;
                                                                                                                      }
                                                                                                                      return new Date(value.getTime());
                                                                                                                  });
    
    public static final BiFunction<ResultSet, String, Date> TimestampResultSetParser = ExceptionCatchedBiFunction.wrap((rs, col) ->
                                                                                                                       {
                                                                                                                           Date value = rs.getTimestamp(col);
                                                                                                                           if ((value == null) || rs.wasNull())
                                                                                                                           {
                                                                                                                               return null;
                                                                                                                           }
                                                                                                                           return new Date(value.getTime());
                                                                                                                       });
    
    public class DataSourceConnectionWrapper implements DataSource
    {
        public DataSourceConnectionWrapper(final String url, final String user, final String password)
        {
            super();
            this.url = url;
            this.user = user;
            this.password = password;
        }
        
        private String url = null;
        private String user = null;
        private String password = null;
        
        @Override
        public Connection getConnection() throws SQLException
        {
            return DriverManager.getConnection(this.url, this.user, this.password);
        }
        
        @Override
        public Connection getConnection(final String username, final String password) throws SQLException
        {
            return DriverManager.getConnection(this.url, username, password);
        }
        
        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException
        {
            return null;
        }
        
        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException
        {
            return false;
        }
        
        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return null;
        }
        
        @Override
        public void setLogWriter(final PrintWriter out) throws SQLException
        {
        
        }
        
        @Override
        public void setLoginTimeout(final int seconds) throws SQLException
        {
        
        }
        
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
    }
}
