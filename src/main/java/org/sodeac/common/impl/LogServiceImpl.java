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
package org.sodeac.common.impl;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sodeac.common.ILogService;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.jdbc.ParseDBSchemaHandler;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.Session;
import org.sodeac.common.model.CoreTreeModel;
import org.sodeac.common.model.StacktraceNodeType;
import org.sodeac.common.model.ThrowableNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.logging.LogEventNodeType;
import org.sodeac.common.model.logging.LogEventType;
import org.sodeac.common.model.logging.LogLevel;
import org.sodeac.common.model.logging.LogPropertyNodeType;
import org.sodeac.common.model.logging.LogPropertyType;
import org.sodeac.common.model.logging.LoggingTreeModel;
import org.sodeac.common.typedtree.XMLMarshaller;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.Domain;

public class LogServiceImpl implements ILogService 
{
	private volatile LogLevel writeLogLevel = LogLevel.INFO;
	private volatile String defaultDomain = null;
	private volatile String defaultSource = null;
	private volatile String defaultModule = null;
	private volatile String defaultTask = null;
	private volatile UUID defaultNode = null;
	private volatile LogEventType defaultLogEventType = LogEventType.SYSTEM_LOG;
	private volatile List<Consumer<BranchNode<?,LogEventNodeType>>> backendList = new CopyOnWriteArrayList<Consumer<BranchNode<?,LogEventNodeType>>>();
	private volatile boolean autoDispose = true;
	
	private XMLMarshaller xmlMarshaller = null;
	
	public LogServiceImpl()
	{
		super();
		
		this.xmlMarshaller = ModelRegistry.getTypedTreeMetaModel(CoreTreeModel.class).getXMLMarshaller();
	}
	
	public LogServiceImpl addLoggerBackend(Consumer<BranchNode<?,LogEventNodeType>> logger)
	{
		if(! backendList.contains(logger))
		{
			backendList.add(logger);
		}
		return this;
	}
	
	public LogServiceImpl removeLoggerBackend(Consumer<BranchNode<?,LogEventNodeType>> logger)
	{
		while(backendList.contains(logger))
		{
			backendList.remove(logger);
		}
		return this;
	}
	
	@Override
	public LogLevel getWriteLogLevel() 
	{
		return this.writeLogLevel;
	}

	@Override
	public ILogService setWriteLogLevel(LogLevel logLevel) 
	{
		if(logLevel == null)
		{
			logLevel = LogLevel.INFO;
		}
		this.writeLogLevel = logLevel;
		return this;
	}

	@Override
	public ILogService setDefaultDomain(String domain) 
	{
		this.defaultDomain = domain;
		return this;
	}

	@Override
	public ILogService setDefaultModule(String module)
	{
		this.defaultModule = module;
		return this;
	}

	@Override
	public ILogService setDefaultTask(String task)
	{
		this.defaultTask = task;
		return this;
	}

	@Override
	public ILogService setDefaultNode(UUID node)
	{
		this.defaultNode = node;
		return this;
	}

	@Override
	public ILogService setDefaultSource(String source) 
	{
		this.defaultSource = source;
		return this;
	}
	
	@Override
	public ILogService setDefaultLogEventType(LogEventType logEventType)
	{
		if(logEventType ==  null)
		{
			logEventType = LogEventType.SYSTEM_LOG;
		}
		this.defaultLogEventType = logEventType;
		return this;
	}

	@Override
	public ILogService setAutoDispose(boolean autoDispose)
	{
		this.autoDispose  = autoDispose;
		return this;
	}
	
	@Override
	public ILogEventBuilder newEvent() 
	{
		return new LogEventBuilderImpl();
	}
	
	private class LogEventBuilderImpl implements ILogEventBuilder
	{
		private String domain = null; 
		private String module = null; 
		private UUID node = null;
		private String source = null;
		private String format = null;
		private String task = null;
		private String uri = null;
		private LogEventType logEventType = null;
		private LogLevel logLevel = LogLevel.INFO;
		private String messageString = null;
		private List<LogPropertyBuilder> properties = null;
		
		private LogEventBuilderImpl()
		{
			super();
			this.domain = LogServiceImpl.this.defaultDomain;
			this.source = LogServiceImpl.this.defaultSource;
			this.module = LogServiceImpl.this.defaultModule;
			this.logEventType = LogServiceImpl.this.defaultLogEventType;
			this.task = LogServiceImpl.this.defaultTask;
			this.node = LogServiceImpl.this.defaultNode;
		}
		
		private List<LogPropertyBuilder> getProperties()
		{
			Objects.nonNull(this.logEventType);
			if(this.properties == null)
			{
				this.properties = new ArrayList<LogServiceImpl.LogEventBuilderImpl.LogPropertyBuilder>();
			}
			return this.properties;
		}

		@Override
		public ILogEventBuilder setLogItemLevel(LogLevel logLevel) 
		{
			Objects.nonNull(this.logEventType);
			if(logLevel == null)
			{
				logLevel = LogLevel.INFO;
			}
			this.logLevel = logLevel;
			return this;
		}

		@Override
		public ILogEventBuilder setLogEventType(LogEventType logEventType) 
		{
			if(logEventType == null)
			{
				logEventType = LogEventType.SYSTEM_LOG;
			}
			this.logEventType = logEventType;
			return this;
		}

		@Override
		public ILogEventBuilder setDomain(String domain) 
		{
			Objects.nonNull(this.logEventType);
			this.domain = domain;
			return this;
		}

		@Override
		public ILogEventBuilder setModule(String module)
		{
			Objects.nonNull(this.logEventType);
			this.module = module;
			return this;
		}

		@Override
		public ILogEventBuilder setTask(String task)
		{
			this.task = task;
			return this;
		}

		@Override
		public ILogEventBuilder setURI(String uri)
		{
			this.uri = uri;
			return this;
		}

		@Override
		public ILogEventBuilder setNode(UUID node)
		{
			this.node = node;
			return this;
		}

		@Override
		public ILogEventBuilder setSource(String source) 
		{
			Objects.nonNull(this.logEventType);
			this.source = source;
			return this;
		}

		@Override
		public ILogEventBuilder setFormat(String format) 
		{
			Objects.nonNull(this.logEventType);
			this.format = format;
			return this ;
		}

		@Override
		public ILogEventBuilder setMessage(String message) 
		{
			Objects.nonNull(this.logEventType);
			this.messageString = message;
			return this;
		}

		@Override
		public ILogEventBuilder addProperty(String key, String value) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.PROPERTY;
			property.key = key;
			property.value = value;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addProperty(String key, String value, String format) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.PROPERTY;
			property.key = key;
			property.value = value;
			property.format = format;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addProperty(String key, String value, String format, String domain) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.PROPERTY;
			property.key = key;
			property.value = value;
			property.format = format;
			property.domain = domain;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addTag(String tag) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.TAG;
			property.key = tag;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addComment(String comment) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.COMMENT;
			property.value = comment;
			getProperties().add(property);
			return this;
		}
		
		@Override
		public ILogEventBuilder addComment(String comment, String id, String format)
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.COMMENT;
			property.key = id;
			property.value = comment;
			property.format = format;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addThrowable(Throwable throwable) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.THROWABLE;
			property.throwable = throwable;
			property.domain = CoreTreeModel.class.getDeclaredAnnotation(Domain.class).name();
			property.module = CoreTreeModel.class.getDeclaredAnnotation(Domain.class).module();
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addStacktrace(StackTraceElement[] stacktrace) 
		{
			Objects.nonNull(this.logEventType);
			LogPropertyBuilder property = new LogPropertyBuilder();
			property.type = LogPropertyType.STACKTRACE;
			property.stacktrace = stacktrace;
			getProperties().add(property);
			return this;
		}

		@Override
		public ILogEventBuilder addCurrentStacktrace()
		{
			StackTraceElement[] fullStacktrace = Thread.currentThread().getStackTrace();
			StackTraceElement[] stacktrace = new StackTraceElement[fullStacktrace.length - 2];
			for(int i = 0; i < stacktrace.length; i++)
			{
				stacktrace[i] = fullStacktrace[i + 2];
			}
			return this.addStacktrace(stacktrace);
		}

		@Override
		public ILogService fire() 
		{
			Objects.nonNull(this.logEventType);
			
			if(LogServiceImpl.this.getWriteLogLevel().getIntValue() > this.logLevel.getIntValue())
			{
				if(properties != null)
				{
					for(LogPropertyBuilder propertyBuilder : properties)
					{
						propertyBuilder.dispose();
					}
					properties.clear();
				}
				
				domain = null; 
				module = null;
				source = null;
				format = null;
				logEventType = null;
				logLevel = null;
				messageString = null;
				properties = null;
				
				return LogServiceImpl.this;
			}
			
			RootBranchNode<LoggingTreeModel,LogEventNodeType> logEvent = TypedTreeMetaModel.getInstance(LoggingTreeModel.class).createRootNode(LoggingTreeModel.logEvent);
			
			Date now = new Date();
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			logEvent
				.setValue(LogEventNodeType.type, logEventType.name())
				.setValue(LogEventNodeType.logLevelName, logLevel.name())
				.setValue(LogEventNodeType.logLevelValue, logLevel.getIntValue())
				.setValue(LogEventNodeType.domain, domain)
				.setValue(LogEventNodeType.module, module)
				.setValue(LogEventNodeType.task, task)
				.setValue(LogEventNodeType.uri, uri)
				.setValue(LogEventNodeType.createNodeId, this.node)
				.setValue(LogEventNodeType.persistNodeId, this.node)
				.setValue(LogEventNodeType.createClientURI, source)
				.setValue(LogEventNodeType.persistClientURI, source)
				.setValue(LogEventNodeType.format, format)
				.setValue(LogEventNodeType.timestamp, now)
				.setValue(LogEventNodeType.date, cal.getTime())
				.setValue(LogEventNodeType.time, now);
			
			logEvent.setValue(LogEventNodeType.message, this.messageString);
			if(properties != null)
			{
				for(LogPropertyBuilder propertyBuilder : properties)
				{
					BranchNode<LogEventNodeType, LogPropertyNodeType> property = logEvent.create(LogEventNodeType.properties)
						.setValue(LogPropertyNodeType.type, propertyBuilder.type.name())
						.setValue(LogPropertyNodeType.domain, propertyBuilder.domain)
						.setValue(LogPropertyNodeType.module, propertyBuilder.module)
						.setValue(LogPropertyNodeType.key, propertyBuilder.key == null ? "" : propertyBuilder.key)
						.setValue(LogPropertyNodeType.format, propertyBuilder.format)
						.setValue(LogPropertyNodeType.value, propertyBuilder.value);
					
					if(propertyBuilder.throwable != null)
					{
						RootBranchNode<CoreTreeModel,ThrowableNodeType> nodeFromThrowable = ThrowableNodeType.nodeFromThrowable(propertyBuilder.throwable);
						
						try
						{
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							xmlMarshaller.marshal(nodeFromThrowable, baos,true);
							property.setValue(LogPropertyNodeType.value,baos.toString());
							baos = null;
						}
						catch (Exception e) 
						{
							throw new RuntimeException(e);
						}
						
						property.setValue(LogPropertyNodeType.originValue, propertyBuilder.throwable);
					}
					else if(propertyBuilder.stacktrace != null)
					{
						RootBranchNode<CoreTreeModel, StacktraceNodeType> nodeFromStacktrace = ThrowableNodeType.nodeFromStacktrace(propertyBuilder.stacktrace);
						
						try
						{
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							xmlMarshaller.marshal(nodeFromStacktrace, baos,true);
							property.setValue(LogPropertyNodeType.value,baos.toString());
							baos = null;
						}
						catch (Exception e) 
						{
							throw new RuntimeException(e);
						}
						
						property.setValue(LogPropertyNodeType.originValue, propertyBuilder.stacktrace);
					}
					
					propertyBuilder.dispose();
				}
				properties.clear();
			}
			
			domain = null; 
			source = null;
			format = null;
			logEventType = null;
			logLevel = null;
			messageString = null;
			properties = null;
			
			for(Consumer<BranchNode<?,LogEventNodeType>> backend : LogServiceImpl.this.backendList)
			{
				backend.accept(logEvent);
			}
			
			if(autoDispose)
			{
				logEvent.dispose(); 
			}
			return LogServiceImpl.this;
		}
		
		private class LogPropertyBuilder
		{
			private LogPropertyType type;
			private String key;
			private String value; 
			private String format;
			private String domain;
			private String module;
			private Throwable throwable;
			private StackTraceElement[] stacktrace;
			
			private void dispose()
			{
				this.type = null;
				this.key = null;
				this.value = null; 
				this.format = null;
				this.domain = null;
				this.module = null;
				this.throwable = null;
				this.stacktrace = null;
			}
		}
		
	}
	
	public static class SystemLogger implements Consumer<BranchNode<?,LogEventNodeType>>, AutoCloseable
	{
		private Logger logger = null;
		
		public SystemLogger(Class<?> clazz)
		{
			super();
			logger = LoggerFactory.getLogger(clazz);
		}

		@Override
		public void accept(BranchNode<?, LogEventNodeType> logEvent)
		{
			if(! LogEventType.SYSTEM_LOG.name().equals(logEvent.getValue(LogEventNodeType.type)))
			{
				return;
			}
			
			Logger logger = this.logger;
			
			if(logger == null)
			{
				return;
			}
			
			Throwable throwable = null;
			
			for(BranchNode<LogEventNodeType, LogPropertyNodeType> property : logEvent.getUnmodifiableNodeList(LogEventNodeType.properties))
			{
				if(! LogPropertyType.THROWABLE.name().equals(property.getValue(LogPropertyNodeType.type)))
				{
					continue;
				}
				
				if(property.getValue(LogPropertyNodeType.originValue) instanceof Throwable)
				{
					throwable = (Throwable)property.getValue(LogPropertyNodeType.originValue);
				}
			}
			
			if( LogLevel.DEBUG.getIntValue() ==  logEvent.getValue(LogEventNodeType.logLevelValue))
			{
				logger.debug(logEvent.getValue(LogEventNodeType.message),throwable);
			}
			else if( LogLevel.FATAL.getIntValue() ==  logEvent.getValue(LogEventNodeType.logLevelValue))
			{
				logger.error(logEvent.getValue(LogEventNodeType.message),throwable);
			}
			else if( LogLevel.ERROR.getIntValue() ==  logEvent.getValue(LogEventNodeType.logLevelValue))
			{
				logger.error(logEvent.getValue(LogEventNodeType.message),throwable);
			}
			else if( LogLevel.WARN.getIntValue() ==  logEvent.getValue(LogEventNodeType.logLevelValue))
			{
				logger.warn(logEvent.getValue(LogEventNodeType.message),throwable);
			}
			else
			{
				logger.info(logEvent.getValue(LogEventNodeType.message),throwable);
			}
			
		}

		@Override
		public void close() throws Exception
		{
			this.logger = null;
		}
		
	}
	
	public static class LogServiceDatasourceBackend implements Consumer<BranchNode<?,LogEventNodeType>>, AutoCloseable
	{
		private Supplier<DataSource> dataSourceProvider = null;
		private TypedTreeJDBCCruder cruder = null;
		
		public LogServiceDatasourceBackend setDataSource(Supplier<DataSource> dataSourceProvider, String schema) throws SQLException
		{
			return setDataSource(dataSourceProvider, schema, true);
		}
		
		public LogServiceDatasourceBackend setDataSource(Supplier<DataSource> dataSourceProvider, String schema, boolean schemaCheck) throws SQLException
		{			
			Connection connection = dataSourceProvider.get().getConnection();
			
			if(schemaCheck)
			{
				try
				{
					ParseDBSchemaHandler parseDBSchemaHandler = new ParseDBSchemaHandler("Logging");
					ModelRegistry.parse(LoggingTreeModel.class, parseDBSchemaHandler);
					RootBranchNode<?, DBSchemaNodeType> schemaSpec = parseDBSchemaHandler.fillSchemaSpec(); 
					schemaSpec.setValue(DBSchemaNodeType.logUpdates, false);
					
					if((schema != null) && (! schema.isEmpty()))
					{
						schemaSpec.setValue(DBSchemaNodeType.dbmsSchemaName,schema);
					}
					else
					{
						schemaSpec.setValue(DBSchemaNodeType.dbmsSchemaName,DBSchemaUtils.getSchema(connection));
					}
					
					connection.setAutoCommit(false);
					DBSchemaUtils schemaUtils = DBSchemaUtils.get(connection);
					schemaUtils.adaptSchema(schemaSpec);
					connection.commit();
					schemaSpec.dispose();
				}
				finally 
				{
					connection.close();
				}
			}
			
			this.dataSourceProvider = dataSourceProvider;
			this.cruder = TypedTreeJDBCCruder.get();
			
			return this;
		}
		public void accept(BranchNode<?,LogEventNodeType> logEvent) 
		{
			this.heartBeatLogger();
			
			if(logEvent == null)
			{
				return;
			}
			
			if((this.dataSourceProvider == null) && (this.cruder == null))
			{
				throw new IllegalStateException("Logger is closed");
			}
			
			try
			{
				Session session = cruder.openSession(dataSourceProvider.get());
				try
				{
					session.persist(logEvent);
					logEvent.getUnmodifiableNodeList(LogEventNodeType.properties).forEach(ExceptionCatchedConsumer.wrap(p -> {session.persist(p); session.persist(p.get(LogPropertyNodeType.correlatedLogEvent));}));
					
					session.flush();
					session.commit();
				}
				finally 
				{
					session.close();
				}
			}
			catch (Exception e) {e.printStackTrace();}
			catch (Error e) {e.printStackTrace();}
		}
		
		public void heartBeatLogger()
		{
			
		}
		
		@Override
		public void close() throws Exception
		{
			try
			{
				this.cruder.close();
			}
			catch (Exception e) {}
			this.cruder = null;
			this.dataSourceProvider = null;
		}
	}

	@Override
	public void close() throws Exception
	{
		if(this.backendList != null)
		{
			for(Consumer<BranchNode<?,LogEventNodeType>> backend : this.backendList)
			{
				if(backend instanceof AutoCloseable)
				{
					((AutoCloseable) backend).close();
				}
			}
			this.backendList.clear();
			this.backendList = null;
			this.defaultDomain = null;
			this.defaultLogEventType = null;
			this.defaultSource = null;
			this.defaultModule = null;
			this.writeLogLevel = null;
			this.xmlMarshaller = null;
		}
		
	}


}
