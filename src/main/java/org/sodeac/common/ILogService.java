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
package org.sodeac.common;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.sodeac.common.impl.LogServiceImpl;
import org.sodeac.common.misc.OSGiUtils;
import org.sodeac.common.model.logging.LogEventNodeType;
import org.sodeac.common.model.logging.LogEventType;
import org.sodeac.common.model.logging.LogLevel;
import org.sodeac.common.typedtree.BranchNode;

public interface ILogService extends AutoCloseable
{
    public LogLevel getWriteLogLevel();
    
    public ILogService setWriteLogLevel(LogLevel logLevel);
    
    public ILogService setDefaultDomain(String domain);
    
    public ILogService setDefaultModule(String module);
    
    public ILogService setDefaultTask(String task);
    
    public ILogService setDefaultSource(String source);
    
    public ILogService setDefaultNode(UUID node);
    
    public ILogService setDefaultLogEventType(LogEventType logEventType);
    
    public ILogService setAutoDispose(boolean autoDispose);
    
    public ILogService addLoggerBackend(Consumer<BranchNode<?, LogEventNodeType>> logger);
    
    public ILogService removeLoggerBackend(Consumer<BranchNode<?, LogEventNodeType>> logger);
    
    public default ILogService debug(String message)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.DEBUG.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.DEBUG)
            .setMessage(message)
            .fire();
        
        return this;
    }
    
    public default ILogService info(String message)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.INFO.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.INFO)
            .setMessage(message)
            .fire();
        
        return this;
    }
    
    public default ILogService warn(String message)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.WARN.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.WARN)
            .setMessage(message)
            .fire();
        
        return this;
    }
    
    public default ILogService warn(String message, Throwable throwable)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.WARN.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.WARN)
            .setMessage(message)
            .addThrowable(throwable)
            .fire();
        
        return this;
    }
    
    public default ILogService error(String message)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.ERROR.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.ERROR)
            .setMessage(message)
            .fire();
        
        return this;
    }
    
    public default ILogService error(String message, Throwable throwable)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.ERROR.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.ERROR)
            .setMessage(message)
            .addThrowable(throwable)
            .fire();
        
        return this;
    }
    
    public default ILogService fatal(String message)
    {
        newEvent()
            .setLogItemLevel(LogLevel.FATAL)
            .setMessage(message)
            .addStacktrace(Thread.currentThread().getStackTrace())
            .fire();
        
        return this;
    }
    
    public default ILogService fatal(String message, Throwable throwable)
    {
        if (getWriteLogLevel().getIntValue() > LogLevel.INFO.getIntValue())
        {
            return this;
        }
        
        newEvent()
            .setLogItemLevel(LogLevel.FATAL)
            .setMessage(message)
            .addThrowable(throwable)
            .fire();
        
        return this;
    }
    
    public ILogEventBuilder newEvent();
    
    public interface ILogEventBuilder
    {
        public ILogEventBuilder setLogItemLevel(LogLevel logLevel);
        
        public ILogEventBuilder setLogEventType(LogEventType logEventType);
        
        public ILogEventBuilder setDomain(String domain);
        
        public ILogEventBuilder setModule(String module);
        
        public ILogEventBuilder setNode(UUID node);
        
        public ILogEventBuilder setSource(String source);
        
        public ILogEventBuilder setFormat(String format);
        
        public ILogEventBuilder setTask(String task);
        
        public ILogEventBuilder setURI(String uri);
        
        public ILogEventBuilder setMessage(String message);
        
        public ILogEventBuilder addProperty(String key, String value);
        
        public ILogEventBuilder addProperty(String key, String value, String type);
        
        public ILogEventBuilder addProperty(String key, String value, String type, String domain);
        
        public ILogEventBuilder addTag(String tag);
        
        public ILogEventBuilder addComment(String comment);
        
        public ILogEventBuilder addComment(String comment, String id, String format);
        
        public ILogEventBuilder addThrowable(Throwable throwable);
        
        public ILogEventBuilder addStacktrace(StackTraceElement[] stacktrace);
        
        public ILogEventBuilder addCurrentStacktrace();
        
        public ILogService fire();
    }
    
    public static ILogService newLogService(Class<?> clazz)
    {
        String bundle = null;
        String bundleVersion = null;
        try
        {
            if (OSGiUtils.isOSGi())
            {
                bundle = OSGiUtils.getSymbolicName(clazz);
                bundleVersion = OSGiUtils.getVersion(clazz);
            }
        }
        catch (Exception e) { }
        String source = "sdc:///?class=" + clazz.getCanonicalName();
        if ((bundle != null) && (!bundle.isEmpty()))
        {
            source = source + "&bundlename=" + bundle;
        }
        if ((bundleVersion != null) && (!bundleVersion.isEmpty()))
        {
            source = source + "&bundleversion=" + bundleVersion;
        }
        return new LogServiceImpl().setDefaultSource(source);
    }
    
    public static ILogService newLogService(Class<?> clazz, Supplier<DataSource> dataSourceProvider, String schema) throws SQLException
    {
        return ILogService.newLogService(clazz).addLoggerBackend
            (
                new LogServiceImpl.LogServiceDatasourceBackend().setDataSource(dataSourceProvider, schema)
            );
    }
    
    public static Consumer<BranchNode<?, LogEventNodeType>> createDataSourceBackend(Supplier<DataSource> dataSourceProvider, String schema, boolean schemaCheck) throws SQLException
    {
        LogServiceImpl.LogServiceDatasourceBackend backend = new LogServiceImpl.LogServiceDatasourceBackend();
        backend.setDataSource(dataSourceProvider, schema, schemaCheck);
        return backend;
    }
    
    public static Consumer<BranchNode<?, LogEventNodeType>> createSystemLoggerBackend(Class<?> clazz)
    {
        return new LogServiceImpl.SystemLogger(clazz);
    }
}
