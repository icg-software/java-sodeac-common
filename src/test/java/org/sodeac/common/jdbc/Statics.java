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

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class Statics
{
    public static final Boolean ENABLED_H2 = true;
    public static final Boolean ENABLED_POSTGRES = false;
    
    // TODO mvn -DmyVariable=someValue for DB Config
    
    public static List<Object[]> connections(Map<String, Boolean> createdSchema, String dbName)
    {
        final String schemaName = "S_" + TestTools.getSchemaName();
        return Arrays.asList
                         (
                             new Object[][]
                                 {
                                     { new Callable<TestConnection>()
                                     {
                                         
                                         @Override
                                         public TestConnection call() throws ClassNotFoundException, SQLException
                                         {
                                             TestConnection testConnection = new TestConnection(Statics.ENABLED_H2);
                                             if (!testConnection.enabled)
                                             {
                                                 return testConnection;
                                             }
                                             
                                             try
                                             {
                                                 Class.forName("org.h2.Driver").newInstance();
                                             }
                                             catch (Exception e) { }
                                             testConnection.connection = DriverManager.getConnection("jdbc:h2:./target/" + dbName, "sa", "sa");
                                             
                                             if (createdSchema.get("H2_" + schemaName) == null)
                                             {
                                                 createdSchema.put("H2_" + schemaName, true);
                                                 
                                                 PreparedStatement prepStat = testConnection.connection.prepareStatement("CREATE SCHEMA " + schemaName);
                                                 prepStat.executeUpdate();
                                                 prepStat.close();
                                             }
                                             testConnection.connection.setSchema(schemaName);
                                             testConnection.dbmsSchemaName = schemaName;
                                             return testConnection;
                                         }
                                     } }
                                     ,
                                     { new Callable<TestConnection>()
                                     {
                                         
                                         @Override
                                         public TestConnection call() throws ClassNotFoundException, SQLException
                                         {
                                             TestConnection testConnection = new TestConnection(Statics.ENABLED_POSTGRES);
                                             if (!testConnection.enabled)
                                             {
                                                 return testConnection;
                                             }
                                             
                                             // docker run --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sodeac -d -p 5432:5432 postgres:12
                                             
                                             // docker exec -it postgres bash:
                                             // mkdir /var/lib/postgresql/data/sodeacdata
                                             // mkdir /var/lib/postgresql/data/sodeacindex
                                             // chown postgres.postgres /var/lib/postgresql/data/sodeacdata
                                             // chown postgres.postgres /var/lib/postgresql/data/sodeacindex
                                             
                                             // psql -h 127.0.0.1 -U postgres --dbname=postgres ::
                                             
                                             // CREATE USER sodeac with SUPERUSER CREATEDB CREATEROLE INHERIT REPLICATION LOGIN PASSWORD 'sodeac';
                                             // CREATE TABLESPACE sodeacdata OWNER sodeac LOCATION '//var//lib//postgresql//data//sodeacdata';
                                             // CREATE TABLESPACE sodeacindex OWNER sodeac LOCATION '//var//lib//postgresql//data//sodeacindex';
                                             // CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
                                             
                                             // psql -h 127.0.0.1 -U sodeac --dbname=sodeac
                                             // CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
                                             
                                             // CREATE SCHEMA IF NOT EXISTS sodeac1 AUTHORIZATION sodeac;
                                             try
                                             {
                                                 Class.forName("org.postgresql.Driver").newInstance();
                                             }
                                             catch (Exception e) { }
                                             testConnection.connection = DriverManager.getConnection("jdbc:postgresql://192.168.178.19:5432/sodeac", "sodeac", "sodeac");
                                             
                                             if (createdSchema.get("POSTGRES_" + schemaName) == null)
                                             {
                                                 createdSchema.put("POSTGRES_" + schemaName, true);
                                                 
                                                 PreparedStatement prepStat = testConnection.connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS " + schemaName.toLowerCase() + " AUTHORIZATION sodeac");
                                                 prepStat.executeUpdate();
                                                 prepStat.close();
                                             }
                                             testConnection.connection.setSchema(schemaName.toLowerCase());
                                             testConnection.dbmsSchemaName = schemaName.toLowerCase();
                                             
                                             return testConnection;
                                         }
                                     } }
                                     
                                 }
                         );
    }
}
