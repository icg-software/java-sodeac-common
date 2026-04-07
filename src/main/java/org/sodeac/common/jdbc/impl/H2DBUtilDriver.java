/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.jdbc.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Component(service = IDBSchemaUtilsDriver.class, property = { "defaultdriver=true", "type=h2" })
public class H2DBUtilDriver implements IDBSchemaUtilsDriver
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    @Override
    public int driverIsApplicableFor(Map<String, Object> properties)
    {
        try
        {
            Connection connection = (Connection) properties.get(Connection.class.getCanonicalName());
            if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2"))
            {
                return IDriver.APPLICABLE_DEFAULT;
            }
        }
        catch (Exception e) { }
        return IDriver.APPLICABLE_NONE;
    }
    
    @Override
    public void setPrimaryKey
        (
            Connection connection, BranchNode<?, DBSchemaNodeType> schema,
            BranchNode<?, TableNodeType> table, Map<String, Object> tableProperties
        ) throws SQLException
    {
        IDBSchemaUtilsDriver.setPrimaryKeyWithIndex(connection, schema, table, tableProperties, this);
    }
    
    @Override
    public String objectNameGuidelineFormat
        (
            BranchNode<?, DBSchemaNodeType> schema, Connection connection,
            String name, String type
        )
    {
        return name == null ? name : name.toUpperCase();
    }
    
    @Override
    public boolean isSequenceExists(String schema, String sequenceName, Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT count(*) FROM INFORMATION_SCHEMA.SEQUENCES WHERE UPPER(SEQUENCE_SCHEMA) = ? AND UPPER(SEQUENCE_NAME) = ?");
        try
        {
            preparedStatement.setString(1, schema.toUpperCase());
            preparedStatement.setString(2, sequenceName.toUpperCase());
            ResultSet resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
            finally
            {
                resultSet.close();
            }
        }
        finally
        {
            preparedStatement.close();
        }
    }
    
    @Override
    public void createSequence(String schema, String sequenceName, Connection connection, long min, long max, boolean cycle, Long cache) throws SQLException
    {
        StringBuilder sqlBuilder = new StringBuilder("CREATE SEQUENCE IF NOT EXISTS " + schema + "." + sequenceName + " MINVALUE ? MAXVALUE ? ");
        sqlBuilder.append(cycle ? "CYCLE" : "NOCYCLE");
        sqlBuilder.append(cache == null ? " " : " CACHE ? ");
        PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());
        try
        {
            preparedStatement.setLong(1, min);
            preparedStatement.setLong(2, max);
            if (cache != null)
            {
                preparedStatement.setLong(3, cache);
            }
            preparedStatement.executeUpdate();
        }
        finally
        {
            preparedStatement.close();
        }
        
    }
    
    @Override
    public void dropSquence(String schema, String sequenceName, Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = connection.prepareStatement("DROP SEQUENCE " + schema + "." + sequenceName);
        try
        {
            preparedStatement.executeUpdate();
        }
        finally
        {
            preparedStatement.close();
        }
    }
    
    @Override
    public long nextFromSequence(String schema, String sequenceName, Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT NEXT VALUE FOR " + schema + "." + sequenceName);
        try
        {
            ResultSet resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                return resultSet.getLong(1);
            }
            finally
            {
                resultSet.close();
            }
        }
        finally
        {
            preparedStatement.close();
        }
    }
}
