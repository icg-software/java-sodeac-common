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
    LogLevel getWriteLogLevel();
    
    ILogService setWriteLogLevel(LogLevel logLevel);
    
    ILogService setDefaultDomain(String domain);
    
    ILogService setDefaultModule(String module);
    
    ILogService setDefaultTask(String task);
    
    ILogService setDefaultSource(String source);
    
    ILogService setDefaultNode(UUID node);
    
    ILogService setDefaultLogEventType(LogEventType logEventType);
    
    ILogService setAutoDispose(boolean autoDispose);
    
    ILogService addLoggerBackend(Consumer<BranchNode<?, LogEventNodeType>> logger);
    
    ILogService removeLoggerBackend(Consumer<BranchNode<?, LogEventNodeType>> logger);
    
    default ILogService debug(final String message)
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
    
    default ILogService info(final String message)
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
    
    default ILogService warn(final String message)
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
    
    default ILogService warn(final String message, final Throwable throwable)
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
    
    default ILogService error(final String message)
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
    
    default ILogService error(final String message, final Throwable throwable)
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
    
    default ILogService fatal(final String message)
    {
        newEvent()
            .setLogItemLevel(LogLevel.FATAL)
            .setMessage(message)
            .addStacktrace(Thread.currentThread().getStackTrace())
            .fire();
        
        return this;
    }
    
    default ILogService fatal(final String message, final Throwable throwable)
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
    
    ILogEventBuilder newEvent();
    
    interface ILogEventBuilder
    {
        ILogEventBuilder setLogItemLevel(LogLevel logLevel);
        
        ILogEventBuilder setLogEventType(LogEventType logEventType);
        
        ILogEventBuilder setDomain(String domain);
        
        ILogEventBuilder setModule(String module);
        
        ILogEventBuilder setNode(UUID node);
        
        ILogEventBuilder setSource(String source);
        
        ILogEventBuilder setFormat(String format);
        
        ILogEventBuilder setTask(String task);
        
        ILogEventBuilder setURI(String uri);
        
        ILogEventBuilder setMessage(String message);
        
        ILogEventBuilder addProperty(String key, String value);
        
        ILogEventBuilder addProperty(String key, String value, String type);
        
        ILogEventBuilder addProperty(String key, String value, String type, String domain);
        
        ILogEventBuilder addTag(String tag);
        
        ILogEventBuilder addComment(String comment);
        
        ILogEventBuilder addComment(String comment, String id, String format);
        
        ILogEventBuilder addThrowable(Throwable throwable);
        
        ILogEventBuilder addStacktrace(StackTraceElement[] stacktrace);
        
        ILogEventBuilder addCurrentStacktrace();
        
        ILogService fire();
    }
    
    static ILogService newLogService(final Class<?> clazz)
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
        catch (final Exception e) { }
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
    
    static ILogService newLogService(final Class<?> clazz, final Supplier<DataSource> dataSourceProvider, final String schema) throws SQLException
    {
        return ILogService.newLogService(clazz).addLoggerBackend
            (
                new LogServiceImpl.LogServiceDatasourceBackend().setDataSource(dataSourceProvider, schema)
            );
    }
    
    static Consumer<BranchNode<?, LogEventNodeType>> createDataSourceBackend(final Supplier<DataSource> dataSourceProvider, final String schema, final boolean schemaCheck) throws SQLException
    {
        LogServiceImpl.LogServiceDatasourceBackend backend = new LogServiceImpl.LogServiceDatasourceBackend();
        backend.setDataSource(dataSourceProvider, schema, schemaCheck);
        return backend;
    }
    
    static Consumer<BranchNode<?, LogEventNodeType>> createSystemLoggerBackend(final Class<?> clazz)
    {
        return new LogServiceImpl.SystemLogger(clazz);
    }
}
