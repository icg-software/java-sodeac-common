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
package org.sodeac.common.jdbc.misc;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sodeac.common.ILogService;
import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.jdbc.Statics;
import org.sodeac.common.jdbc.TestConnection;
import org.sodeac.common.misc.CloseableCollector;
import org.sodeac.common.model.logging.LogEventListChunkNodeType;
import org.sodeac.common.model.logging.LoggingTreeModel;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.XMLMarshaller;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LogServiceTest
{
    public static List<Object[]> connectionList = null;
    public static final Map<String, Boolean> createdSchema = new HashMap<String, Boolean>();
    
    @Parameters
    public static List<Object[]> connections()
    {
        if (connectionList != null)
        {
            return connectionList;
        }
        return connectionList = Statics.connections(createdSchema, "logger");
    }
    
    public LogServiceTest(Callable<TestConnection> connectionFactory)
    {
        this.testConnectionFactory = connectionFactory;
    }
    
    Callable<TestConnection> testConnectionFactory = null;
    TestConnection testConnection = null;
    
    @Before
    public void setUp() throws Exception
    {
        this.testConnection = testConnectionFactory.call();
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
            catch (Exception e) { }
        }
    }
    
    @Test
    public void test00001LogServiceDatasoure() throws Exception
    {
        if (!testConnection.enabled)
        {
            return;
        }
        
        ConplierBean<DataSource> dataSourceProvider = new ConplierBean<DataSource>(testConnection.getDataSource());
        
        RootBranchNode<LoggingTreeModel, LogEventListChunkNodeType> chunk = LoggingTreeModel.createLogEventListChunk(2, 1, 0, true);
        
        ILogService logService = ILogService.newLogService(LogServiceTest.class, dataSourceProvider, null)
                                            .addLoggerBackend(e -> LogEventListChunkNodeType.addLogToEventListChunk(chunk, e));
        
        logService.error("TEST_MESSAGE_1", new RuntimeException("xxx"));
        
        logService.info("TEST_MESSAGE_2");
        
        try (CloseableCollector closeableCollector = CloseableCollector.newInstance())
        {
            Connection connection = closeableCollector.register(dataSourceProvider.get().getConnection());
            
            ResultSet resultSetLogEventCount = closeableCollector.register(closeableCollector.register(connection.prepareStatement("SELECT COUNT(*) FROM SDC_LOG_EVENT")).executeQuery());
            resultSetLogEventCount.next();
            assertEquals("size should be correct", 2, resultSetLogEventCount.getInt(1));
            
            ResultSet resultSetLogPropertyCount = closeableCollector.register(closeableCollector.register(connection.prepareStatement("SELECT COUNT(*) FROM SDC_LOG_PROPERTY")).executeQuery());
            resultSetLogPropertyCount.next();
            assertEquals("size should be correct", 1, resultSetLogPropertyCount.getInt(1));
            
        }
        
        XMLMarshaller marshaller = ModelRegistry.getTypedTreeMetaModel(LoggingTreeModel.class).getXMLMarshaller();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(chunk, baos, true);
        
        String xml1 = baos.toString();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        RootBranchNode<LoggingTreeModel, LogEventListChunkNodeType> chunk2 = ModelRegistry.getTypedTreeMetaModel(LoggingTreeModel.class).createRootNode(LoggingTreeModel.logEventListChunk);
        marshaller.unmarshal(chunk2, bais, true);
        
        baos = new ByteArrayOutputStream();
        marshaller.marshal(chunk2, baos, true);
        
        String xml2 = baos.toString();
        assertEquals("value should be correct", xml1, xml2);
        
        logService.newEvent().setMessage("TEST_MESSAGE_3").addCurrentStacktrace().fire();
        
        logService.close();
    }
}
