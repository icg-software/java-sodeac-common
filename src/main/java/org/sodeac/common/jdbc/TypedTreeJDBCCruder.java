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
package org.sodeac.common.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.sodeac.common.IService.IFactoryEnvironment;
import org.sodeac.common.annotation.ServiceFactory;
import org.sodeac.common.annotation.ServiceRegistration;
import org.sodeac.common.annotation.StringProperty;
import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.ConvertEvent.ConvertEventProvider;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.Session.RuntimeParameter;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.MASK;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode.ColumnNode;
import org.sodeac.common.misc.Driver;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.INodeType;
import org.sodeac.common.typedtree.LeafNode;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.Node;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.Association.AssociationType;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;

@ServiceFactory(factoryClass=TypedTreeJDBCCruder.LocalServiceFactory.class)
@ServiceRegistration(serviceType=TypedTreeJDBCCruder.class)
@StringProperty(key="a",value="b")
public class TypedTreeJDBCCruder implements AutoCloseable 
{
	protected TypedTreeJDBCCruder()
	{
		super();
		this.persistDefinitionContainer = new HashMap<INodeType, PreparedPersistDefinitionContainer>();
		this.loadDefinitionContainer = new HashMap<INodeType, PreparedLoadDefinitionContainer>();
		this.deleteDefinitionContainer = new HashMap<INodeType, PreparedDeleteDefinitionContainer>();
		this.lock = new ReentrantLock();
		this.rootNodeFactories = new HashMap<INodeType, Function<Object[], Collection<RootBranchNode<? extends TypedTreeMetaModel,? extends BranchNodeMetaModel>>>>();
	}
	
	public static final TypedTreeJDBCCruder get()
	{
		return new TypedTreeJDBCCruder();
	}
	
	protected static class LocalServiceFactory implements Function<IFactoryEnvironment<?,?>,TypedTreeJDBCCruder>
	{
		@Override
		public TypedTreeJDBCCruder apply(IFactoryEnvironment<?,?> t)
		{
			TypedTreeJDBCCruder cruder = TypedTreeJDBCCruder.get();
			cruder.softclose = true;
			return cruder;
		}	
	}
	
	private Map<INodeType, PreparedPersistDefinitionContainer> persistDefinitionContainer = null; 
	private Map<INodeType, PreparedDeleteDefinitionContainer> deleteDefinitionContainer = null; 
	private Map<INodeType, PreparedLoadDefinitionContainer> loadDefinitionContainer = null; 
	private Map<INodeType, Function<Object[], Collection<RootBranchNode<? extends TypedTreeMetaModel,? extends BranchNodeMetaModel>>>> rootNodeFactories = null; 
	
	private Lock lock = null;
	private boolean softclose = false;
	
	public Session openSession(DataSource mainDatasource)
	{
		return new Session(mainDatasource);
	}
	
	@Override
	public void close()
	{
		lock.lock();
		try
		{
			if(this.persistDefinitionContainer != null)
			{
				for(PreparedPersistDefinitionContainer container : this.persistDefinitionContainer.values())
				{
					container.close();
				}
				this.persistDefinitionContainer.clear();
				if(! softclose)
				{
					this.persistDefinitionContainer = null;
				}
			}
			
			if(this.deleteDefinitionContainer != null)
			{
				for(PreparedDeleteDefinitionContainer container : this.deleteDefinitionContainer.values())
				{
					container.close();
				}
				this.deleteDefinitionContainer.clear();
				if(! softclose)
				{
					this.deleteDefinitionContainer = null;
				}
			}
			
			if(this.loadDefinitionContainer != null)
			{
				for(PreparedLoadDefinitionContainer container : this.loadDefinitionContainer.values())
				{
					container.close();
				}
				this.loadDefinitionContainer.clear();
				if(! softclose)
				{
					this.loadDefinitionContainer = null;
				}
			}
			if(this.rootNodeFactories != null)
			{
				this.rootNodeFactories.clear();
			}
			if(! softclose)
			{
				this.rootNodeFactories = null;
			}
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	public Collection<INodeType<?,?>> getNodeTypeList(BranchNodeType<? extends BranchNodeMetaModel,? extends BranchNodeMetaModel> type) throws SQLException
	{
		return TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type).loadDefinition.nodeTypeList;
	}
	
	private PreparedDeleteDefinitionContainer getPreparedDeleteDefinitionContainer(INodeType nodeType)
	{
		lock.lock();
		try
		{
			PreparedDeleteDefinitionContainer container = deleteDefinitionContainer.get(nodeType);
			if(container != null)
			{
				return container;
			}
			container = new PreparedDeleteDefinitionContainer(nodeType);
			deleteDefinitionContainer.put(nodeType,container);
			return container;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	private PreparedPersistDefinitionContainer getPreparedPersistDefinitionContainer(INodeType nodeType)
	{
		lock.lock();
		try
		{
			PreparedPersistDefinitionContainer container = persistDefinitionContainer.get(nodeType);
			if(container != null)
			{
				return container;
			}
			container = new PreparedPersistDefinitionContainer(nodeType);
			persistDefinitionContainer.put(nodeType,container);
			return container;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	private PreparedLoadDefinitionContainer getPreparedLoadDefinitionContainer(INodeType nodeType)
	{
		lock.lock();
		try
		{
			PreparedLoadDefinitionContainer container = loadDefinitionContainer.get(nodeType);
			if(container != null)
			{
				return container;
			}
			container = new PreparedLoadDefinitionContainer(nodeType);
			loadDefinitionContainer.put(nodeType,container);
			return container;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	
	public class Session implements AutoCloseable
	{
		private volatile boolean error = false;
		private volatile DataSource mainDatasource = null;
		private volatile Connection mainConnection = null;
		private volatile IDBSchemaUtilsDriver mainUtilsDriver = null;
		private Map<String,PreparedStatement> preparedStatementCache = new HashMap<String,PreparedStatement>();
		private Map<String,PreparedStatement> preparedStatementResultSetCache = new HashMap<String,PreparedStatement>();
		private boolean isPostgreSQL = false;
		private boolean isH2 = false;
		
		protected Session(DataSource mainDatasource)
		{
			super();
			this.mainDatasource = mainDatasource;
		}
		
		private void checkMainConnection() throws SQLException
		{
			if(this.mainConnection == null)
			{
				this.mainConnection = mainDatasource.getConnection();
				this.mainConnection.setAutoCommit(false);
				
				Map<String,Object> driverProperties = new HashMap<>();
				driverProperties.put(Connection.class.getCanonicalName(), this.mainConnection);
				this.mainUtilsDriver = Driver.getSingleDriver(IDBSchemaUtilsDriver.class, driverProperties);
				
				String dbProduct = this.mainConnection.getMetaData().getDatabaseProductName();
				if(dbProduct.equalsIgnoreCase("PostgreSQL"))
				{
					this.isPostgreSQL = true;
				}
				else if(dbProduct.equalsIgnoreCase("H2"))
				{
					this.isH2 = true;
				}
			}
		}
		
		@Override
		public void close() throws Exception 
		{
			if(this.error)
			{
				try
				{
					if(this.mainConnection != null)
					{
						this.mainConnection.rollback();
					}
				}
				catch (Exception e) {}
			}
			for(PreparedStatement preparedStatement : this.preparedStatementResultSetCache.values())
			{
				try
				{
					preparedStatement.close();
				}
				catch (Exception e) {}
				catch (Error e) {}
			}
			this.preparedStatementResultSetCache.clear();
			for(PreparedStatement preparedStatement : this.preparedStatementCache.values())
			{
				try
				{
					preparedStatement.close();
				}
				catch (Exception e) {}
				catch (Error e) {}
			}
			this.preparedStatementCache.clear();
			try
			{
				if(this.mainConnection != null)
				{
					this.mainConnection.close();
				}
			}
			catch (Exception e) {e.printStackTrace();}
			
			this.mainDatasource = null;
			this.mainConnection = null;
			this.mainUtilsDriver = null;
			this.preparedStatementCache = null;
		}
		
		public <T extends BranchNodeMetaModel> List<BranchNode<?,T>> loadList(BranchNodeType<? extends BranchNodeMetaModel,T> type, BiFunction<IRuntimeParameter, String, String> sqlAppender, BiConsumer<IRuntimeParameter,PreparedStatement> prepareStatement, Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel,T>>> nodeFactory) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			
			List<BranchNode<?,T>> collector = new ArrayList<BranchNode<?,T>>();
			boolean valid = false;
			try
			{
				checkMainConnection();
				
				PreparedLoadDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type);
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.nodeFactory = (Function)nodeFactory;
				
				preparedDefinitionContainer.loadDefinition.selectNode(runtimeParameter,collector,sqlAppender,prepareStatement);
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			
			return collector;
		}
		
		public <T extends BranchNodeMetaModel> List<BranchNode<?,T>> loadList(BranchNodeType<? extends BranchNodeMetaModel,T> type, INodeType<T,?> searchField, Object[] searchValues, Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel,T>>> nodeFactory) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			
			List<BranchNode<?,T>> collector = new ArrayList<BranchNode<?,T>>();
			boolean valid = false;
			try
			{
				checkMainConnection();
				
				PreparedLoadDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type);
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.searchField = searchField;
				runtimeParameter.searchValues = searchValues;
				runtimeParameter.nodeFactory = (Function)nodeFactory;
				
				preparedDefinitionContainer.loadDefinition.selectNode(runtimeParameter,collector);
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			
			return collector;
		}
		
		public <P extends TypedTreeMetaModel,T extends BranchNodeMetaModel> RootBranchNode<P, T> loadRootNode(BranchNodeType<P,T> nodeType, Object id) throws SQLException
		{
			LeafNodeType<T,?> searchField = TypedTreeJDBCHelper.parseTableNode(nodeType, MASK.PK_COLUMN).getPrimaryKeyNode().getLeafNodeType();
			RootBranchNode<P, T> node = (RootBranchNode)loadItem((BranchNodeType)nodeType, (INodeType)searchField, new Object[] {id}, (Function)getRootNodeFactory(nodeType));
			return node;
		}
		
		public <T extends BranchNodeMetaModel,P extends BranchNodeMetaModel> BranchNode<P,T> loadItem(BranchNode<P,T> branchNode) throws SQLException
		{
			if(branchNode == null)
			{
				return null;
			}
			LeafNodeType searchField = TypedTreeJDBCHelper.parseTableNode(branchNode.getNodeType(), MASK.PK_COLUMN).getPrimaryKeyNode().getLeafNodeType();
			Object id = branchNode.getValue(searchField);
			if(id == null)
			{
				throw new IllegalStateException("can not load data without primary key value");
			}
			return loadItem((BranchNodeType)branchNode.getNodeType(), (INodeType)searchField, new Object[] {id}, ids -> (Collection)Collections.singleton(branchNode));
		}
		
		public <T extends BranchNodeMetaModel> BranchNode<?,T> loadItem(BranchNodeType<? extends BranchNodeMetaModel,T> type, INodeType<T,?> searchField, Object[] searchValues, Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel,T>>> nodeFactory) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			List<BranchNode<?,T>> collector = new ArrayList<BranchNode<?,T>>();
			boolean valid = false;
			try
			{
				PreparedLoadDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type);
				
				checkMainConnection();
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.searchField = searchField;
				runtimeParameter.searchValues = searchValues;
				runtimeParameter.nodeFactory = (Function)nodeFactory;
				
				preparedDefinitionContainer.loadDefinition.selectNode(runtimeParameter,collector);
				
				runtimeParameter.close();
				valid = true;
				
				if(collector.isEmpty())
				{
					return null;
				}
				
				return collector.get(0);
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
				
				collector.clear();
			}
		}
		
		public <T extends BranchNodeMetaModel> List<BranchNode<?,T>> loadListByReferencedNode(BranchNodeType<? extends BranchNodeMetaModel,T> type, Object[] searchValues, Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel,T>>> nodeFactory) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			List<BranchNode<?,T>> collector = new ArrayList<BranchNode<?,T>>();
			boolean valid = false;
			try
			{
				PreparedLoadDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type);
				
				checkMainConnection();
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.searchField = type;
				runtimeParameter.searchValues = searchValues;
				runtimeParameter.nodeFactory = (Function)nodeFactory;
				
				preparedDefinitionContainer.loadDefinition.selectNode(runtimeParameter,collector);
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			
			return collector;
		}
		
		public <T extends BranchNodeMetaModel> List<BranchNode<?,T>> loadListByReferencedNode(BranchNodeListType<? extends BranchNodeMetaModel,T> type, Object[] searchValues, Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel,T>>> nodeFactory) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			List<BranchNode<?,T>> collector = new ArrayList<BranchNode<?,T>>();
			boolean valid = false;
			try
			{
				PreparedLoadDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedLoadDefinitionContainer(type);
				
				checkMainConnection();
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.searchField = type;
				runtimeParameter.searchValues = searchValues;
				runtimeParameter.nodeFactory = (Function)nodeFactory;
				
				preparedDefinitionContainer.loadDefinition.selectNode(runtimeParameter,collector);
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			
			return collector;
		}
		
		public <P extends BranchNodeMetaModel,T extends BranchNodeMetaModel> void loadReferencedChildNodes(BranchNode<? extends BranchNodeMetaModel,P> node, BranchNodeListType<P, T> childNodeType) throws SQLException
		{
			if(! node.getUnmodifiableNodeList(childNodeType).isEmpty()) // node.isEmpty(nodeType);
			{
				node.clear(childNodeType);
			}
			loadListByReferencedNode(childNodeType, Collections.singleton(node.get(TypedTreeJDBCHelper.parseTableNode(node.getNodeType(), MASK.PK_COLUMN).getPrimaryKeyNode().getLeafNodeType()).getValue() ).toArray(), ids -> Collections.singletonList(node.create(childNodeType))).clear();
		}
		
		public <P extends BranchNodeMetaModel,T extends BranchNodeMetaModel> void loadReferencedChildNode(BranchNode<? extends BranchNodeMetaModel,P> node, BranchNodeType<P, T> childNodeType) throws SQLException
		{
			if(node.get(childNodeType) != null)
			{
				node.remove(childNodeType);
			}
			loadListByReferencedNode(childNodeType, Collections.singleton(node.get(TypedTreeJDBCHelper.parseTableNode(node.getNodeType(), MASK.PK_COLUMN).getPrimaryKeyNode().getLeafNodeType()).getValue() ).toArray(), ids -> Collections.singletonList(node.create(childNodeType))).clear();
		}
		
		public < P extends BranchNodeMetaModel, T extends BranchNodeMetaModel> BranchNode< P,T> persist(BranchNode< P,T> node) throws SQLException, InstantiationException, IllegalAccessException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			if(node == null)
			{
				return node;
			}
			boolean valid = false;
			try
			{
				PreparedPersistDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedPersistDefinitionContainer(node.getNodeType());
				
				checkMainConnection();
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.branchNode = node;
				
				if(preparedDefinitionContainer.checkPersistableIsNew.checkIsNew(runtimeParameter))
				{
					preparedDefinitionContainer.insertStatement.insertNode(runtimeParameter);
				}
				else
				{
					preparedDefinitionContainer.updateStatement.updateNode(runtimeParameter);
				}
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			
			return node;
		}
		
		public < P extends BranchNodeMetaModel, T extends BranchNodeMetaModel> BranchNode< P,T> delete(BranchNode< P,T> node) throws SQLException
		{
			if(error)
			{
				throw new RuntimeException("Session is invalid by thrown exception");
			}
			boolean valid = false;
			try
			{
				PreparedDeleteDefinitionContainer preparedDefinitionContainer = TypedTreeJDBCCruder.this.getPreparedDeleteDefinitionContainer(node.getNodeType());
				
				checkMainConnection();
				
				RuntimeParameter runtimeParameter = new RuntimeParameter();
				runtimeParameter.branchNode = node;
				
				preparedDefinitionContainer.preparedDeleteStatementDefinition.deleteNode(runtimeParameter);
				
				runtimeParameter.close();
				valid = true;
			}
			finally 
			{
				if(! valid)
				{
					this.error = true;
				}
			}
			return node;
		}
		
		public PreparedStatement getCachedPreparedStatement(String sql, int resultSetType,int resultSetConcurrency) throws SQLException
		{
			String key = "_" + resultSetType + "_" + resultSetConcurrency + "_" + sql; 
			
			PreparedStatement preparedStatement = this.preparedStatementResultSetCache.get(key);
			if((preparedStatement != null) && (! preparedStatement.isClosed()))
			{
				return preparedStatement;
			}
			preparedStatement = mainConnection.prepareStatement(sql,resultSetType,resultSetConcurrency);
			this.preparedStatementResultSetCache.put(key,preparedStatement);
			return preparedStatement;				
		}
		
		public PreparedStatement getCachedPreparedStatement(String sql, boolean returnGeneratedKey) throws SQLException
		{
			PreparedStatement preparedStatement = this.preparedStatementCache.get(sql);
			if((preparedStatement != null) && (! preparedStatement.isClosed()))
			{
				return preparedStatement;
			}
			if(returnGeneratedKey)
			{
				preparedStatement = mainConnection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			}
			else
			{
				preparedStatement = mainConnection.prepareStatement(sql);
			}
			this.preparedStatementCache.put(sql,preparedStatement);
			return preparedStatement;
		}
		
		public void flush()throws SQLException
		{
			
		}
		
		public void commit() throws SQLException
		{
			if(mainConnection != null)
			{
				mainConnection.commit();
			}
		}
		
		public void rollback() throws SQLException
		{
			if(mainConnection != null)
			{
				mainConnection.rollback();
			}
			error = false;
		}
		
		protected class RuntimeParameter implements IRuntimeParameter
		{
			private RuntimeParameter()
			{
				super();
				this.isNew = new ConplierBean<Boolean>(Boolean.FALSE);
				this.isExisting = new ConplierBean<Boolean>(Boolean.FALSE);
				this.conplierBean = new ConplierBean<Object>();
				this.convertEvent = new ConvertEventProvider();
				
				this.connection = Session.this.mainConnection;
				this.dbSchemaUtilsDriver = Session.this.mainUtilsDriver;
				this.convertEvent.setRuntimeParameter(this);
				this.convertEvent.setConnection(Session.this.mainConnection);
				this.convertEvent.setSchemaUtilDriver(Session.this.mainUtilsDriver);
			}
			
			private Connection connection = null;
			private IDBSchemaUtilsDriver dbSchemaUtilsDriver = null;
			private PreparedStatement preparedStatement;
			private ResultSet resultSet = null;
			
			private LeafNodeType<? extends BranchNodeMetaModel,?> type;
			private BranchNodeType<? extends BranchNodeMetaModel,? extends BranchNodeMetaModel> childType = null;
			private BranchNode<? extends BranchNodeMetaModel, ? extends  BranchNodeMetaModel> branchNode;
			private BranchNode<? extends BranchNodeMetaModel, ? extends  BranchNodeMetaModel> workingBranchNode;
			
			
			private Object staticValue;
			private ConplierBean<Boolean> isNew;
			private ConplierBean<Boolean> isExisting;
			private ConplierBean<Object> conplierBean;
			private ConvertEventProvider convertEvent;
			private INodeType searchField;
			private Object[] searchValues;
			private Object[] values = null;
			
			private Function<Object[], Collection<BranchNode<? extends BranchNodeMetaModel, ? extends BranchNodeMetaModel>>> nodeFactory;
			
			public Session getSession()
			{
				return Session.this;
			}
			public PreparedStatement getPreparedStatement() 
			{
				return preparedStatement;
			}
			public ResultSet getResultSet() 
			{
				return resultSet;
			}
			protected void setResultSet(ResultSet resultSet)
			{
				this.resultSet = resultSet;
			}
			protected Object[] getValues()
			{
				return values;
			}
			protected void setValues(Object[] values)
			{
				this.values = values;
			}
			public BranchNode<? extends BranchNodeMetaModel, ? extends BranchNodeMetaModel> getBranchNode() 
			{
				return branchNode;
			}
			public Object getStaticValue() 
			{
				return staticValue;
			}
			public Connection getConnection() 
			{
				return connection;
			}
			public void close()
			{
				this.connection = null;
				this.dbSchemaUtilsDriver = null;
				this.preparedStatement = null;
				this.resultSet = null;
				this.branchNode = null;
				this.workingBranchNode = null;
				this.staticValue = null;
				this.isNew = null;
				this.isExisting = null;
				if(this.conplierBean != null)
				{
					this.conplierBean.setValue(null);
				}
				this.conplierBean = null;
				if(this.convertEvent != null)
				{
					this.convertEvent.clear();
				}
				this.convertEvent = null;
				
				this.searchField = null;
				this.searchValues = null;
				this.nodeFactory = null;
				this.values = null;
				this.type = null;
				this.childType = null;
			}
			
			public PreparedStatement getPreparedStatement(String sql, int resultSetType,int resultSetConcurrency) throws SQLException
			{
				return Session.this.getCachedPreparedStatement(sql, resultSetType, resultSetConcurrency);
			}
			
			public PreparedStatement getPreparedStatement(String sql, boolean returnGeneratedKey) throws SQLException
			{
				return Session.this.getCachedPreparedStatement(sql, returnGeneratedKey);
			}
		}
	}
	
	private class PreparedDeleteDefinitionContainer
	{
		private INodeType nodeType = null;
		private PreparedDeleteStatementDefinition preparedDeleteStatementDefinition = null;
		
		private PreparedDeleteDefinitionContainer(INodeType nodeType)
		{
			super();
			this.nodeType = nodeType;
			this.preparedDeleteStatementDefinition = new PreparedDeleteStatementDefinition(nodeType);
		}
		
		private void close()
		{
			this.nodeType = null;
			this.preparedDeleteStatementDefinition.close();
			this.preparedDeleteStatementDefinition = null;
		}
	}
	private class PreparedPersistDefinitionContainer
	{
		private PreparedPersistDefinitionContainer(INodeType nodeType) 
		{
			super();
			this.nodeType = nodeType;
			this.insertStatement = new PreparedInsertStatementDefinition(nodeType);
			this.updateStatement = new PreparedUpdateStatementDefinition(nodeType);
			this.checkPersistableIsNew = new CheckPersistableIsNewDefinition(nodeType);
		}
		
		private INodeType nodeType = null;
		private PreparedInsertStatementDefinition insertStatement = null;
		private PreparedUpdateStatementDefinition updateStatement = null;
		private CheckPersistableIsNewDefinition checkPersistableIsNew = null;
		
		
		private void close()
		{
			try
			{
				this.insertStatement.close();
			}
			catch (Exception e) {e.printStackTrace();}
			
			try
			{
				this.updateStatement.close();
			}
			catch (Exception e) {e.printStackTrace();}
			
			try
			{
				this.checkPersistableIsNew.close();
			}
			catch (Exception e) {e.printStackTrace();}
			
			this.nodeType = null;
			this.insertStatement = null;
			this.checkPersistableIsNew = null;
		}
	}
	
	private class PreparedLoadDefinitionContainer
	{
		private PreparedLoadDefinitionContainer(INodeType nodeType) 
		{
			super();
			this.nodeType = nodeType;
			this.loadDefinition = new PreparedLoadResultSetDefinition(this.nodeType);
		}
		
		private PreparedLoadResultSetDefinition loadDefinition = null;
		private INodeType nodeType = null;
		
		private void close()
		{
			try
			{
				this.loadDefinition.close();
			}
			catch (Exception e) {e.printStackTrace();}
			
			this.nodeType = null;
			this.loadDefinition =  null;
		}
	}
	
	private class PreparedLoadResultSetDefinition
	{
		public PreparedLoadResultSetDefinition(INodeType nodeType) 
		{
			super();
			
			this.tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType, MASK.ALL);
			
			if(tableNode == null)
			{ 
				throw new RuntimeException("Annotation SQLTable not found in model " + nodeType.getTypeClass());
			}
			
			this.columns = new ArrayList<JDBCGetterDefinition>();
			this.nodeTypeList = new ArrayList<INodeType<?,?>>();
			this.constrainHelperIndex = Collections.synchronizedMap(new HashMap<>());
			
			StringBuilder sqlColumns = new StringBuilder();
			
			int cursorPosititon = 1;
			
			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getLeafNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isReadable())
				{
					continue;
				}
				
				if(sqlColumns.length() > 0)
				{
					sqlColumns.append(",");
				}
				
				sqlColumns.append(columnNode.getColumnName());
				BiConsumer<RuntimeParameter, PreparedLoadResultSetDefinition> nodeSetter = (r,d) -> {r.branchNode.get((LeafNodeType)r.type).setValue(r.staticValue);};
				this.columns.add(new JDBCGetterDefinition(columnNode, cursorPosititon++, nodeSetter));
				this.nodeTypeList.add(columnNode.getLeafNodeType());
			}
			

			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getBranchNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isReadable())
				{
					continue;
				}
				
				
				if(columnNode.getReferencedPrimaryKey() != null)
				{
					BiConsumer<RuntimeParameter, PreparedLoadResultSetDefinition> nodeSetter = (r,d) -> 
					{
						if(r.staticValue == null)
						{
							return;
						}
						BranchNode<? extends BranchNodeMetaModel,? extends BranchNodeMetaModel> childNode = r.branchNode.create((BranchNodeType)r.childType);
						childNode.setValue((LeafNodeType)r.type,r.staticValue);
					};
					JDBCGetterDefinition getColumnDefinition = new JDBCGetterDefinition(columnNode, cursorPosititon++,nodeSetter);
					
					getColumnDefinition.childType = columnNode.getBranchNodeType();
					getColumnDefinition.type = columnNode.getReferencedPrimaryKey().getLeafNodeType();
					
					if(sqlColumns.length() > 0)
					{
						sqlColumns.append(",");
					}
					sqlColumns.append(columnNode.getColumnName());
					
					this.columns.add(getColumnDefinition);
					this.nodeTypeList.add(columnNode.getBranchNodeType());
					
				}
				
			}
			
			this.sql = "select " + sqlColumns + "  from " + tableNode.getTableName() + " ";
			this.nodeTypeList = Collections.unmodifiableList(this.nodeTypeList);
		}
		
		private TableNode tableNode = null;
		private BranchNodeMetaModel type = null;
		private String domain = null;
		private String boundedContext = null;
		private String service = null;
		private String sql = null;
		private List<JDBCGetterDefinition> columns = null;
		protected List<INodeType<?,?>> nodeTypeList = null;
		private Map<INodeType,ConstraintHelper> constrainHelperIndex = null;
		
		private void close()
		{
			if(columns != null)
			{
				columns.forEach(c -> c.close());
				columns.clear();
			}
			if(constrainHelperIndex != null)
			{
				for(ConstraintHelper helper : constrainHelperIndex.values())
				{
					helper.column = null;
					helper.sqlType = null;
				}
				constrainHelperIndex.clear();
			}
			tableNode = null;
			type = null;
			domain = null;
			boundedContext = null;
			service = null;
			sql = null;
			columns = null;
			nodeTypeList = null;
			constrainHelperIndex = null;
		}
		
		private class ConstraintHelper
		{
			private String column;
			private String sqlType;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected void selectNode(RuntimeParameter runtimeParameter, List collector) throws SQLException
		{
			Objects.requireNonNull(runtimeParameter.searchField,"search field not defined");
			
			ConstraintHelper constraintHelper = constrainHelperIndex.get(runtimeParameter.searchField);
			if(constraintHelper == null)
			{
				ColumnNode columnNode  = null;
				if(this.tableNode.getReferencedByColumnNode() != null)
				{
					if
					(
						(this.tableNode.getReferencedByColumnNode().getBranchNodeType() == runtimeParameter.searchField) ||
						(this.tableNode.getReferencedByColumnNode().getBranchNodeListType() == runtimeParameter.searchField)
					)
					{
						columnNode = this.tableNode.getReferencedByColumnNode();
					}
				}
				if(columnNode == null)
				{
					for(ColumnNode check : this.tableNode.getColumnList())
					{
						if(check.getLeafNodeType() == runtimeParameter.searchField)
						{
							columnNode = check;
						}
						else if(check.getBranchNodeType() == runtimeParameter.searchField)
						{
							columnNode = check;
						}
					}
				}
				
				if(columnNode == null)
				{
					throw new IllegalStateException("unexpected searchfield. node: " +this.tableNode.getNodeType() + " field " + runtimeParameter.searchField);
				}
				
				Objects.requireNonNull(columnNode, "search field not found in searchable fields of " + runtimeParameter.searchField.getParentNodeClass());
				
				constraintHelper = new ConstraintHelper();
				constraintHelper.sqlType = "VARCHAR";
				constraintHelper.column = columnNode.getColumnName();
				constraintHelper.sqlType = columnNode.getSqlType().name().toUpperCase();
					
				constrainHelperIndex.put(runtimeParameter.searchField, constraintHelper);
			}
			
			
			String completeSQL = null;
			if(runtimeParameter.getSession().isPostgreSQL)
			{
				completeSQL = sql + " where " + constraintHelper.column + " in (select * from unnest(?))";
			}
			else if(runtimeParameter.getSession().isH2)
			{
				completeSQL = sql + " where " + constraintHelper.column + " in (UNNEST(?))";
			}
			else
			{
				completeSQL = sql + " where " + constraintHelper.column + " in (?)";
			}
			
			runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(completeSQL);
			runtimeParameter.values = new Object[this.columns.size()];
			runtimeParameter.convertEvent.setPreparedStatement(runtimeParameter.preparedStatement);
			runtimeParameter.preparedStatement.setArray(1, runtimeParameter.connection.createArrayOf(constraintHelper.sqlType, runtimeParameter.searchValues));
			
			ResultSet resultSet = runtimeParameter.preparedStatement.executeQuery();
			try
			{
				runtimeParameter.resultSet = resultSet;
				while(resultSet.next())
				{
					for(JDBCGetterDefinition column : this.columns)
					{
						try
						{
							column.getter.acceptWithException(runtimeParameter);
						}
						catch (SQLException e) 
						{
							throw e;
						}
						catch (Exception e) 
						{
							throw new RuntimeException(e);
						}
					}
					
					Collection<BranchNode<?, ?>> nodes = runtimeParameter.nodeFactory.apply(runtimeParameter.values);
					
					if(nodes == null)
					{
						continue;
					}
					for(BranchNode<?, ?> node : nodes)
					{
						if(node == null)
						{
							continue;
						}
						collector.add(node);
						runtimeParameter.branchNode = node;
						int i = 0;
						for(JDBCGetterDefinition column : this.columns)
						{
							runtimeParameter.childType = column.childType;
							runtimeParameter.type = column.type;
							
							runtimeParameter.staticValue = runtimeParameter.values[i++];
							if(column.nodeSetter != null)
							{
								column.nodeSetter.accept(runtimeParameter, this);
							}
						}
					}
					
					runtimeParameter.childType = null;
					runtimeParameter.type = null;
					runtimeParameter.branchNode = null;
					runtimeParameter.workingBranchNode = null;
				}
			}
			finally 
			{
				resultSet.close();
			}
			
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected void selectNode(RuntimeParameter runtimeParameter, List collector, BiFunction<IRuntimeParameter, String, String> sqlAppender, BiConsumer<IRuntimeParameter,PreparedStatement> prepareStatement) throws SQLException
		{
			Objects.requireNonNull(sqlAppender,"sqlAppender not defined");
			String completeSQL = sqlAppender.apply(runtimeParameter, sql);
			
			runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(completeSQL);
			
			runtimeParameter.values = new Object[this.columns.size()];
			runtimeParameter.convertEvent.setPreparedStatement(runtimeParameter.preparedStatement);
			if(prepareStatement != null)
			{
				prepareStatement.accept(runtimeParameter, runtimeParameter.preparedStatement);
			}
			
			ResultSet resultSet = runtimeParameter.preparedStatement.executeQuery();
			try
			{
				runtimeParameter.resultSet = resultSet;
				while(resultSet.next())
				{
					for(JDBCGetterDefinition column : this.columns)
					{
						try
						{
							column.getter.acceptWithException(runtimeParameter);
						}
						catch (SQLException e) 
						{
							throw e;
						}
						catch (Exception e) 
						{
							throw new RuntimeException(e);
						}
					}
					
					Collection<BranchNode<?, ?>> nodes = runtimeParameter.nodeFactory.apply(runtimeParameter.values);
					
					if(nodes == null)
					{
						continue;
					}
					for(BranchNode<?, ?> node : nodes)
					{
						if(node == null)
						{
							continue;
						}
						collector.add(node);
						runtimeParameter.branchNode = node;
						int i = 0;
						for(JDBCGetterDefinition column : this.columns)
						{
							runtimeParameter.childType = column.childType;
							runtimeParameter.type = column.type;
							
							runtimeParameter.staticValue = runtimeParameter.values[i++];
							if(column.nodeSetter != null)
							{
								column.nodeSetter.accept(runtimeParameter, this);
							}
						}
					}
					
					runtimeParameter.childType = null;
					runtimeParameter.type = null;
					runtimeParameter.branchNode = null;
					runtimeParameter.workingBranchNode = null;
				}
			}
			finally 
			{
				resultSet.close();
			}
		}
	}
	
	private class PreparedInsertStatementDefinition
	{
		
		public PreparedInsertStatementDefinition(INodeType nodeType) 
		{
			super();
			this.tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType);
			
			if(tableNode == null)
			{ 
				throw new RuntimeException("Annotation SQLTable not found in model " + nodeType.getTypeClass());
			}
			
			columns = new ArrayList<JDBCSetterDefinition>();
			
			StringBuilder sqlColumns = new StringBuilder();
			StringBuilder sqlValues = new StringBuilder();
			
			int cursorPosititon = 1;
			
			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getLeafNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isInsertable())
				{
					continue;
				}
				
				if(columnNode.isPrimaryKey() && (columnNode.isPrimaryKeyAutoGenerated()))
				{
					BiConsumer<RuntimeParameter, PreparedLoadResultSetDefinition> nodeSetter = (r,d) -> {r.branchNode.get((LeafNodeType)r.type).setValue(r.staticValue);};
					this.autoGeneratedRetrieve = new JDBCGetterDefinition(columnNode, 1, nodeSetter);
					
					continue;
				}
				
				if(sqlColumns.length() > 0)
				{
					sqlColumns.append(",");
				}
				if(sqlValues.length() > 0)
				{
					sqlValues.append(",");
				}
				
				sqlColumns.append(columnNode.getColumnName());
				sqlValues.append("?");
				
				this.columns.add(new JDBCSetterDefinition(columnNode, cursorPosititon++));
			}
			
			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getBranchNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isInsertable())
				{
					continue;
				}
				
				
				if(columnNode.getReferencedPrimaryKey() != null)
				{
					if(sqlColumns.length() > 0)
					{
						sqlColumns.append(",");
					}
					if(sqlValues.length() > 0)
					{
						sqlValues.append(",");
					}
					
					sqlColumns.append(columnNode.getColumnName());
					sqlValues.append("?");
					
					this.columns.add(new JDBCSetterDefinition(columnNode, cursorPosititon++));
				}
				
				
			}
			
			// column defined by Parent
			
			if(tableNode.getReferencedByColumnNode()  != null)
			{
				JDBCSetterDefinition setColumnDefinition = new JDBCSetterDefinition(tableNode.getReferencedByColumnNode(), cursorPosititon++);
				setColumnDefinition.parentType = true;
				
				if(sqlColumns.length() > 0)
				{
					sqlColumns.append(",");
				}
				if(sqlValues.length() > 0)
				{
					sqlValues.append(",");
				}
				
				sqlColumns.append(tableNode.getReferencedByColumnNode().getColumnName());
				sqlValues.append("?");
				
				this.columns.add(setColumnDefinition);
			}
			
			this.sql = "insert into " + tableNode.getTableName() + " (" + sqlColumns + ") values (" + sqlValues + ")";
		}
		
		
		private TableNode tableNode = null;
		private String sql = null;
		private List<JDBCSetterDefinition> columns = null;
		private JDBCGetterDefinition autoGeneratedRetrieve = null; 
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void insertNode(RuntimeParameter runtimeParameter) throws SQLException, InstantiationException, IllegalAccessException
		{
			if(autoGeneratedRetrieve == null)
			{
				runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(this.sql);
			}
			else
			{
				runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(this.sql,true);
			}
			
			runtimeParameter.convertEvent.setPreparedStatement(runtimeParameter.preparedStatement);
			runtimeParameter.convertEvent.setPersistNode(runtimeParameter.branchNode);
			
			for(JDBCSetterDefinition column : this.columns)
			{
				try
				{
					runtimeParameter.convertEvent.setColumnNode(column.columnNode);
					
					// select nodes
					
					Node node = null;
					if(column.parentType)
					{
						node = runtimeParameter.branchNode.getParentNode();
						runtimeParameter.workingBranchNode = (BranchNode)node;
					}
					else if(column.branchNodeType != null)
					{
						boolean backupAutocreate = runtimeParameter.branchNode.getRootNode().isBranchNodeGetterAutoCreate();
						if(backupAutocreate)
						{
							runtimeParameter.branchNode.getRootNode().setBranchNodeGetterAutoCreate(false);
						}
						node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
						runtimeParameter.workingBranchNode = (BranchNode)node;
						if(backupAutocreate)
						{
							runtimeParameter.branchNode.getRootNode().setBranchNodeGetterAutoCreate(true);
						}
					}
					else if(column.leafNodeType != null)
					{
						node = runtimeParameter.branchNode.get((LeafNodeType)column.leafNodeType);
						runtimeParameter.workingBranchNode = runtimeParameter.branchNode;
					}
					else
					{
						throw new RuntimeException("invalid nodetype settings");
					}
					
					runtimeParameter.convertEvent.setNode(node);
					
					// trigger
					
					if(column.columnNode.getOnUpsert() != null)
					{
						column.columnNode.getOnUpsertInstance().accept(runtimeParameter.convertEvent);
						if(column.branchNodeType != null)
						{
							node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
							runtimeParameter.workingBranchNode = (BranchNode)node;
						}
					}
					if(column.columnNode.getOnInsert() != null)
					{
						column.columnNode.getOnInsertInstance().accept(runtimeParameter.convertEvent);
						
						if(column.branchNodeType != null)
						{
							node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
							runtimeParameter.workingBranchNode = (BranchNode)node;
						}
					}
					
					// set parameter
					
					try
					{
						column.setter.acceptWithException(runtimeParameter);
					}
					catch (SQLException e) 
					{
						throw e;
					}
					catch (Exception e) 
					{
						throw new RuntimeException(e);
					}
					
					runtimeParameter.convertEvent.setNode(null);
					
				}
				finally 
				{
					runtimeParameter.workingBranchNode = null;
				}
			}
			
			runtimeParameter.preparedStatement.executeUpdate();
			
			// get autogen key
			
			if(this.autoGeneratedRetrieve != null)
			{
				try
				{
					ResultSet backupResultSet = runtimeParameter.getResultSet();
					try
					{
						Object[] backupValues = runtimeParameter.getValues();
						try
						{
							runtimeParameter.setValues(new Object[1]);
							runtimeParameter.setResultSet(runtimeParameter.preparedStatement.getGeneratedKeys());
							try
							{
								runtimeParameter.getResultSet().next();
								this.autoGeneratedRetrieve.getter.acceptWithException(runtimeParameter);
								
								runtimeParameter.childType = this.autoGeneratedRetrieve.childType;
								runtimeParameter.type = this.autoGeneratedRetrieve.type;
								runtimeParameter.staticValue = runtimeParameter.values[0];
								
								this.autoGeneratedRetrieve.nodeSetter.accept(runtimeParameter, null);
								
								runtimeParameter.childType = null;
								runtimeParameter.type = null;
								runtimeParameter.staticValue = null;
							}
							finally 
							{
								runtimeParameter.getResultSet().close();
							}
						}
						finally 
						{
							runtimeParameter.setValues(backupValues);
						}
					}
					finally 
					{
						runtimeParameter.setResultSet(backupResultSet);
					}
				}
				catch (SQLException e) 
				{
					throw e;
				}
				catch (RuntimeException e) 
				{
					throw e;
				}
				catch (Exception e) 
				{
					throw new RuntimeException(e);
				}
			}
		}
		
		private void close()
		{
			this.tableNode = null;
			this.sql = null;
			if(this.columns != null)
			{
				this.columns.forEach(c -> c.close());
				this.columns.clear();
			}
			this.columns = null;
		}
	}
	
	private class PreparedDeleteStatementDefinition
	{
		
		public PreparedDeleteStatementDefinition(INodeType nodeType) 
		{
			super();
			this.tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType);
			this.sql = "DELETE FROM " + tableNode.getTableName() + " WHERE " + tableNode.getPrimaryKeyNode().getColumnName() + " = ? ";
			
		}
		
		private TypedTreeJDBCHelper.TableNode tableNode = null;
		private BranchNodeMetaModel type = null;
		private String sql = null;
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void deleteNode(RuntimeParameter runtimeParameter) throws SQLException
		{
			runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(this.sql);
			
			Object value = runtimeParameter.branchNode.getValue(tableNode.getPrimaryKeyNode().getLeafNodeType());
			
			if(tableNode.getPrimaryKeyNode().getLeafNodeType().getTypeClass() == String.class)
			{
				if(tableNode.getPrimaryKeyNode().getSqlType() == SQLColumnType.UUID)
				{
					runtimeParameter.preparedStatement.setObject(1, UUID.fromString((String)value));
				}
				else
				{
					runtimeParameter.preparedStatement.setString(1, (String)value);
				}
			}
			else if(tableNode.getPrimaryKeyNode().getLeafNodeType().getTypeClass() == UUID.class)
			{
				if(tableNode.getPrimaryKeyNode().getSqlType() == SQLColumnType.VARCHAR)
				{
					runtimeParameter.preparedStatement.setString(1, ((UUID)value).toString());
				}
				else if(tableNode.getPrimaryKeyNode().getSqlType() == SQLColumnType.CHAR)
				{
					runtimeParameter.preparedStatement.setString(1, ((UUID)value).toString());
				}
				else
				{
					runtimeParameter.preparedStatement.setObject(1, (UUID)value);
				}
				
			}
			else if(tableNode.getPrimaryKeyNode().getLeafNodeType().getTypeClass() == Long.class)
			{
				runtimeParameter.preparedStatement.setLong(1, (Long)value);
			}
			else if(tableNode.getPrimaryKeyNode().getLeafNodeType().getTypeClass() == Integer.class)
			{
				runtimeParameter.preparedStatement.setLong(1, (Integer)value);
			}
			else
			{
				runtimeParameter.preparedStatement.setString(1, value.toString());
			}
			
			runtimeParameter.preparedStatement.executeUpdate();
		}
		
		private void close()
		{
			this.tableNode = null;
			this.type = null;
			this.sql = null;
			this.tableNode = null;
		}
	}
	
	private class PreparedUpdateStatementDefinition
	{
		
		public PreparedUpdateStatementDefinition(INodeType nodeType) 
		{
			super();
			TableNode tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType, MASK.ALL);
			
			if(tableNode == null)
			{
				throw new RuntimeException("Annotation SQLTable not found in model " + nodeType.getTypeClass());
			}
			
			columns = new ArrayList<JDBCSetterDefinition>();
			
			StringBuilder sqlColumns = new StringBuilder();
			
			int cursorPosititon = 1;
			
			if(tableNode.getPrimaryKeyNode() == null)
			{
				throw new RuntimeException("PrimaryKey not found in " + nodeType.getTypeClass());
			}
			
			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getLeafNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isUpdatable())
				{
					continue;
				}
				
				if(columnNode.isPrimaryKey())
				{
					continue;
				}
				
				if(sqlColumns.length() > 0)
				{
					sqlColumns.append(",");
				}
				
				sqlColumns.append(columnNode.getColumnName() + " = ? ");
				
				this.columns.add(new JDBCSetterDefinition(columnNode, cursorPosititon++));
			}
			
			for(ColumnNode columnNode : tableNode.getColumnList())
			{
				if(columnNode.getBranchNodeType() == null)
				{
					continue;
				}
				
				if(! columnNode.isUpdatable())
				{
					continue;
				}
				
				
				if(columnNode.getReferencedPrimaryKey() != null)
				{
					if(sqlColumns.length() > 0)
					{
						sqlColumns.append(" , ");
					}
					
					sqlColumns.append(columnNode.getColumnName() + " = ? ");
					this.columns.add(new JDBCSetterDefinition(columnNode, cursorPosititon++));
					
				}
				
			}
			
			// columns defined by Parent
			
			if(tableNode.getReferencedByColumnNode()  != null)
			{
				JDBCSetterDefinition setColumnDefinition = new JDBCSetterDefinition(tableNode.getReferencedByColumnNode(), cursorPosititon++);
				setColumnDefinition.parentType = true;
							
				if(sqlColumns.length() > 0)
				{
					sqlColumns.append(",");
				}
				sqlColumns.append(tableNode.getReferencedByColumnNode().getColumnName() + " = ? ");
					
				this.columns.add(setColumnDefinition);
			}
			
			
			this.columns.add(new JDBCSetterDefinition(tableNode.getPrimaryKeyNode(), cursorPosititon++));
			
			this.sql = "update " + tableNode.getTableName() + " set " + sqlColumns +  " where " + tableNode.getPrimaryKeyNode().getColumnName() + " = ? ";
		}
		
		private TableNode tableNode = null;
		private BranchNodeMetaModel type = null;
		private String domain = null;
		private String boundedContext = null;
		private String service = null;
		private String sql = null;
		private List<JDBCSetterDefinition> columns = null;
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void updateNode(RuntimeParameter runtimeParameter) throws SQLException, InstantiationException, IllegalAccessException
		{
			runtimeParameter.preparedStatement = runtimeParameter.getPreparedStatement(this.sql);
			
			runtimeParameter.convertEvent.setPreparedStatement(runtimeParameter.preparedStatement);
			runtimeParameter.convertEvent.setPersistNode(runtimeParameter.branchNode);
			
			for(JDBCSetterDefinition column : this.columns)
			{
				try
				{
					runtimeParameter.convertEvent.setColumnNode(column.columnNode);
					
					Node node = null;		// TriggerParameter
					if(column.parentType)
					{
						node = runtimeParameter.branchNode.getParentNode();
						runtimeParameter.workingBranchNode = (BranchNode)node;
					}
					else if(column.branchNodeType != null)
					{
						boolean backupAutocreate = runtimeParameter.branchNode.getRootNode().isBranchNodeGetterAutoCreate();
						if(backupAutocreate)
						{
							runtimeParameter.branchNode.getRootNode().setBranchNodeGetterAutoCreate(false);
						}
						node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
						runtimeParameter.workingBranchNode = (BranchNode)node;
						if(backupAutocreate)
						{
							runtimeParameter.branchNode.getRootNode().setBranchNodeGetterAutoCreate(true);
						}
					}
					else if(column.leafNodeType != null)
					{
						node = runtimeParameter.branchNode.get((LeafNodeType)column.leafNodeType);
						runtimeParameter.workingBranchNode = runtimeParameter.branchNode;
					}
					else
					{
						throw new RuntimeException("invalid nodetype settings");
					}
					
					runtimeParameter.convertEvent.setNode(node);
					
					if(column.columnNode.getOnUpsert() != null)
					{
						column.columnNode.getOnUpsertInstance().accept(runtimeParameter.convertEvent);
						if(column.branchNodeType != null)
						{
							node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
							runtimeParameter.workingBranchNode = (BranchNode)node;
						}
					}
					if(column.columnNode.getOnUpdate() != null)
					{
						column.columnNode.getOnUpdateInstance().accept(runtimeParameter.convertEvent);
						if(column.branchNodeType != null)
						{
							node = runtimeParameter.branchNode.get((BranchNodeType)column.branchNodeType);
							runtimeParameter.workingBranchNode = (BranchNode)node;
						}
					}
					
					try
					{
						column.setter.acceptWithException(runtimeParameter);
					}
					catch (SQLException e) 
					{
						throw e;
					}
					catch (Exception e) 
					{
						throw new RuntimeException(e);
					}
					
					runtimeParameter.convertEvent.setNode(null);
				}
				finally 
				{
					runtimeParameter.workingBranchNode = null;
				}
			}
			
			runtimeParameter.preparedStatement.executeUpdate();
		}
		
		private void close()
		{
			this.tableNode = null;
			this.type = null;
			this.domain = null;
			this.boundedContext = null;
			this.service = null;
			this.sql = null;
			if(this.columns != null)
			{
				this.columns.forEach(c -> c.close());
				this.columns.clear();
			}
			this.columns = null;
		}
	}
	
	private class CheckPersistableIsNewDefinition
	{
		public CheckPersistableIsNewDefinition(INodeType nodeType)
		{
			super();
			this.checks = new ArrayList<ExceptionCatchedConsumer<RuntimeParameter>>();
			TableNode tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType, MASK.PK_COLUMN);
			if(tableNode == null)
			{ 
				throw new RuntimeException("Annotation SQLTable not found in model " + nodeType.getTypeClass());
			}
			checks.add(new CheckPKLeafNode(tableNode.getPrimaryKeyNode()));
		}
		
		private List<ExceptionCatchedConsumer<RuntimeParameter>> checks = null;
		
		private boolean checkIsNew(RuntimeParameter runtimeParameter)
		{
			runtimeParameter.isExisting.setValue(false);
			runtimeParameter.isNew.setValue(false);
			
			for(Consumer<RuntimeParameter> check : checks)
			{
				check.accept(runtimeParameter);
			}
			
			if(runtimeParameter.isExisting.getValue().booleanValue() && runtimeParameter.isNew.getValue().booleanValue())
			{
				throw new RuntimeException("conflict detected in new state for branch node");
			}
			
			if((!runtimeParameter.isExisting.getValue().booleanValue()) && (!runtimeParameter.isNew.getValue().booleanValue()))
			{
				throw new RuntimeException("new-state not detected for branch node");
			}
			
			return runtimeParameter.isNew.getValue().booleanValue();
		}
		
		private void close()
		{
			if(checks != null)
			{
				for(Consumer<RuntimeParameter> check : checks)
				{
					if(check instanceof AutoCloseable)
					{
						try
						{
							((AutoCloseable)check).close();
						}
						catch (Exception e) {}
					}
				}
				checks.clear();
			}
			checks = null;
		}
		
		private class CheckPKLeafNode implements ExceptionCatchedConsumer<RuntimeParameter>, AutoCloseable
		{
			private JDBCSetterDefinition columnDefinition = null;
			private ColumnNode columnNode = null;
			
			private CheckPKLeafNode(ColumnNode columnNode)
			{
				super();
				this.columnDefinition = new JDBCSetterDefinition(columnNode, -1);
				this.columnNode = columnNode;
			}

			@Override
			public void acceptWithException(RuntimeParameter runtimeParameter) 
			{
				LeafNode<BranchNodeMetaModel, ?> leafNode = runtimeParameter.branchNode.get(columnNode.getLeafNodeType());
				if(leafNode.getValue() == null)
				{
					runtimeParameter.isNew.setValue(true);
				}
				else
				{
					// TODO Check DB
					runtimeParameter.isExisting.setValue(true);
				}				
			}

			@Override
			public void close() throws Exception
			{
				this.columnDefinition = null;
				this.columnNode = null;
			}
		}
	}
	
	public class JDBCSetterDefinition
	{
		public JDBCSetterDefinition(ColumnNode columnNode, int cursorPosition)
		{
			super();
			this.cursorPosition = cursorPosition;
			this.columnNode = columnNode;
			if(columnNode.getReferencedPrimaryKey() != null)
			{
				this.leafNodeType = columnNode.getReferencedPrimaryKey().getLeafNodeType();
				this.branchNodeType = columnNode.getBranchNodeType();
			}
			else
			{
				this.leafNodeType = columnNode.getLeafNodeType();
			}
			
			SQLColumnType type = columnNode.getSqlType();
			
			if((type == SQLColumnType.BOOLEAN))
			{
				this.setter = new LeafNodeBooleanJDBCSetter(); 
			}
			else if((type == SQLColumnType.UUID))
			{
				this.setter = new LeafNodeUUIDJDBCSetter(); 
			}
			else if((type == SQLColumnType.INTEGER))
			{
				this.setter = new LeafNodeIntegerJDBCSetter(); 
			}
			else if((type == SQLColumnType.BIGINT))
			{
				this.setter = new LeafNodeLongJDBCSetter(); 
			}
			else if((type == SQLColumnType.REAL))
			{
				this.setter = new LeafNodeFloatJDBCSetter(); 
			}
			else if((type == SQLColumnType.DOUBLE))
			{
				this.setter = new LeafNodeDoubleJDBCSetter(); 
			}
			else if((type == SQLColumnType.TIMESTAMP))
			{
				this.setter = new LeafNodeTimestampJDBCSetter(); 
			}
			else if((type == SQLColumnType.DATE))
			{
				this.setter = new LeafNodeDateJDBCSetter(); 
			}
			else if((type == SQLColumnType.TIME))
			{
				this.setter = new LeafNodeTimeJDBCSetter(); 
			}
			else
			{
				this.setter = new LeafNodeStringJDBCSetter();
			}
		}
		
		private ColumnNode columnNode = null;
		private LeafNodeType<? extends BranchNodeMetaModel,?> leafNodeType;
		private BranchNodeType<? extends BranchNodeMetaModel,?> branchNodeType = null;
		private boolean parentType = false;
		private int cursorPosition;
		private ExceptionCatchedConsumer<RuntimeParameter> setter = null;
		private AssociationType associationType = null;
		
		public INodeType<? extends BranchNodeMetaModel, ?> getType() 
		{
			return leafNodeType;
		}
		public int getCursorPosition() 
		{
			return cursorPosition;
		}
		
		public AssociationType getAssociationType() 
		{
			return associationType;
		}
		
		private abstract class LeafNodeJDBCSetter implements ExceptionCatchedConsumer<RuntimeParameter>
		{
			@Override
			public void acceptWithException(RuntimeParameter runtimeParameter) throws SQLException, InstantiationException, IllegalAccessException
			{
				Object value = null;
				if(columnNode.getNode2JDBC() != null)
				{
					if(runtimeParameter.workingBranchNode == null)
					{
						value = columnNode.getNode2JDBCInstance().apply(null);
					}
					else
					{
						value = columnNode.getNode2JDBCInstance().apply(runtimeParameter.workingBranchNode.getValue((LeafNodeType)JDBCSetterDefinition.this.leafNodeType));
					}
				}
				else
				{
					
					if(runtimeParameter.workingBranchNode == null)
					{
						value = null;
					}
					else
					{
						value = runtimeParameter.workingBranchNode.getValue((LeafNodeType)JDBCSetterDefinition.this.leafNodeType);
					}
				}
				
				this.setValue(runtimeParameter, value);
			}
			
			public abstract void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException;
		}
		
		private class LeafNodeStringJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.VARCHAR);
				}
				else
				{
					if(value instanceof String)
					{
						runtimeParameter.preparedStatement.setString(JDBCSetterDefinition.this.cursorPosition, (String) value);
					}
					else
					{
						runtimeParameter.preparedStatement.setString(JDBCSetterDefinition.this.cursorPosition, value.toString());
					}
				}
			}
		}
		
		private class LeafNodeBooleanJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.BOOLEAN);
				}
				else
				{
					runtimeParameter.preparedStatement.setBoolean(JDBCSetterDefinition.this.cursorPosition, (Boolean) value);
				}
			}
		}
		
		private class LeafNodeUUIDJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				runtimeParameter.preparedStatement.setObject(JDBCSetterDefinition.this.cursorPosition, (UUID) value);
			}
		}
		
		private class LeafNodeIntegerJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.INTEGER);
				}
				else
				{
					runtimeParameter.preparedStatement.setInt(JDBCSetterDefinition.this.cursorPosition, (Integer) value);
				}
			}
		}
		
		private class LeafNodeLongJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.BIGINT);
				}
				else
				{
					runtimeParameter.preparedStatement.setLong(JDBCSetterDefinition.this.cursorPosition, (Long) value);
				}
			}
		}
		
		private class LeafNodeFloatJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.FLOAT);
				}
				else
				{
					runtimeParameter.preparedStatement.setFloat(JDBCSetterDefinition.this.cursorPosition, (Float) value);
				}
			}
		}
		
		private class LeafNodeDoubleJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.DOUBLE);
				}
				else
				{
					runtimeParameter.preparedStatement.setDouble(JDBCSetterDefinition.this.cursorPosition, (Double) value);
				}
			}
		}
		
		private class LeafNodeTimestampJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.TIMESTAMP);
				}
				else
				{
					runtimeParameter.preparedStatement.setTimestamp(JDBCSetterDefinition.this.cursorPosition, new java.sql.Timestamp(( (Date) value).getTime()));
				}
			}
		}
		
		private class LeafNodeDateJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.DATE);
				}
				else
				{
					runtimeParameter.preparedStatement.setDate(JDBCSetterDefinition.this.cursorPosition, new java.sql.Date(( (Date) value).getTime()));
				}
			}
		}
		
		private class LeafNodeTimeJDBCSetter extends LeafNodeJDBCSetter
		{
			@Override
			public void setValue(RuntimeParameter runtimeParameter, Object value) throws SQLException
			{
				if(value == null)
				{
					runtimeParameter.preparedStatement.setNull(JDBCSetterDefinition.this.cursorPosition, Types.TIME);
				}
				else
				{
					runtimeParameter.preparedStatement.setTime(JDBCSetterDefinition.this.cursorPosition, new java.sql.Time(( (Date) value).getTime()));
				}
			}
		}
		
		public void close()
		{
			this.leafNodeType = null;
			this.columnNode = null;
			this.branchNodeType = null;
			this.parentType = false;
			this.setter = null;
			this.associationType = null;
		}
	}
	
	public class JDBCGetterDefinition
	{
		public JDBCGetterDefinition(ColumnNode columnNode, int cursorPosition,  BiConsumer<RuntimeParameter, PreparedLoadResultSetDefinition> nodeSetter)
		{
			super();
			this.cursorPosition = cursorPosition;
			this.type = columnNode.getLeafNodeType();
			this.nodeSetter = nodeSetter;
			
			SQLColumnType type = columnNode.getSqlType();
			
			Class converterClass = columnNode.getJDBC2Node();
			if(converterClass == SQLColumn.NoJDBC2Node.class)
			{
				converterClass = null;
			}
			
			if((type == SQLColumnType.BOOLEAN))
			{
				this.getter = new LeafNodeBooleanJDBCGetter(); 
			}
			else if((type == SQLColumnType.UUID))
			{
				this.getter = new LeafNodeUUIDJDBCGetter(); 
			}
			else if((type == SQLColumnType.INTEGER))
			{
				this.getter = new LeafNodeIntegerJDBCGetter(); 
			}
			else if((type == SQLColumnType.BIGINT))
			{
				this.getter = new LeafNodeLongJDBCGetter(); 
			}
			else if((type == SQLColumnType.REAL))
			{
				this.getter = new LeafNodeFloatJDBCGetter(); 
			}
			else if((type == SQLColumnType.DOUBLE))
			{
				this.getter = new LeafNodeDoubleJDBCGetter(); 
			}
			else if((type == SQLColumnType.TIMESTAMP))
			{
				this.getter = new LeafNodeTimestampJDBCGetter(); 
			}
			else if((type == SQLColumnType.DATE))
			{
				this.getter = new LeafNodeDateJDBCGetter(); 
			}
			else if((type == SQLColumnType.TIME))
			{
				this.getter = new LeafNodeTimeJDBCGetter(); 
			}
			else
			{
				this.getter = new LeafNodeStringJDBCGetter();
			}
			
			
			 if(converterClass != null)
			 {
				 try
				 {
					 this.converter = (Function)converterClass.newInstance();
				 }
				 catch (Exception e) 
				 {
					throw new RuntimeException(e);
				}
			 }
		}
		
		private LeafNodeType<? extends BranchNodeMetaModel,?> type;
		private BranchNodeType<? extends BranchNodeMetaModel,?> childType = null;
		private boolean parentType = false;
		private int cursorPosition;
		private Function<Object, Object> converter = null;
		private ExceptionCatchedConsumer<RuntimeParameter> getter = null;
		private AssociationType associationType = null;
		private BiConsumer<RuntimeParameter, PreparedLoadResultSetDefinition> nodeSetter = null;
		
		public void close()
		{
			this.type = null;
			this.childType = null;
			this.converter = null;
			this.getter = null;
			this.associationType = null;
			this.nodeSetter = null;
		}
		
		public INodeType<? extends BranchNodeMetaModel, ?> getType() 
		{
			return type;
		}
		public int getCursorPosition() 
		{
			return cursorPosition;
		}
		public Function<?, ?> getConverter() 
		{
			return converter;
		}
		public AssociationType getAssociationType() 
		{
			return associationType;
		}
		
		private abstract class LeafNodeJDBCGetter implements ExceptionCatchedConsumer<RuntimeParameter>
		{
			@Override
			public void acceptWithException(RuntimeParameter runtimeParameter) throws Exception
			{
				Object value = this.getValue(runtimeParameter);
				if(converter != null)
				{
					value = converter.apply(value);
				}
				runtimeParameter.values[JDBCGetterDefinition.this.cursorPosition - 1] = value;
			}
			
			public abstract Object getValue(RuntimeParameter runtimeParameter) throws SQLException;
		}
		
		private class LeafNodeStringJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				return runtimeParameter.getResultSet().getString(JDBCGetterDefinition.this.cursorPosition);
			}
		}
		
		private class LeafNodeBooleanJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				boolean value = runtimeParameter.getResultSet().getBoolean(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeUUIDJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				UUID value = (UUID)runtimeParameter.getResultSet().getObject(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeIntegerJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				int value = runtimeParameter.getResultSet().getInt(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeLongJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				long value = runtimeParameter.getResultSet().getLong(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeFloatJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				float value = runtimeParameter.getResultSet().getFloat(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeDoubleJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				double value = runtimeParameter.getResultSet().getDouble(JDBCGetterDefinition.this.cursorPosition);
				return runtimeParameter.getResultSet().wasNull() ? null : value;
			}
		}
		
		private class LeafNodeTimestampJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				Timestamp value = runtimeParameter.getResultSet().getTimestamp(JDBCGetterDefinition.this.cursorPosition);
				return value == null ? null : new Date(value.getTime());
			}
		}
		
		private class LeafNodeDateJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				java.sql.Date value = runtimeParameter.getResultSet().getDate(JDBCGetterDefinition.this.cursorPosition);
				return value == null ? null : new Date(value.getTime());
			}
		}
		
		private class LeafNodeTimeJDBCGetter extends LeafNodeJDBCGetter
		{
			@Override
			public Object getValue(RuntimeParameter runtimeParameter) throws SQLException
			{
				java.sql.Time value = runtimeParameter.getResultSet().getTime(JDBCGetterDefinition.this.cursorPosition);
				return value == null ? null : new Date(value.getTime());
			}
		}
	}
	
	public interface IRuntimeParameter
	{
		public default PreparedStatement getPreparedStatement(String sql)throws SQLException
		{
			return getPreparedStatement(sql, false);
		}
		public PreparedStatement getPreparedStatement(String sql, boolean returnGeneratedKey)throws SQLException;
		public Session getSession();
	}
	
	private <P extends TypedTreeMetaModel,T extends BranchNodeMetaModel> Function<Object[],Collection<RootBranchNode<P,T>>> getRootNodeFactory(BranchNodeType<P,T> nodeType)
	{
		Objects.requireNonNull(nodeType);
		
		lock.lock();
		try
		{
			Function<Object[],Collection<RootBranchNode<P,T>>> factory = (Function) this.rootNodeFactories.get(nodeType);
			if(factory != null)
			{
				return factory;
			}
			Class<? extends P> treeModelClass = nodeType.getParentNodeClass();
			
			factory = t -> Collections.singletonList(TypedTreeMetaModel.getInstance(treeModelClass).createRootNode(nodeType));
			this.rootNodeFactories.put(nodeType,(Function)factory);
			return factory;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	public static class ConvertEvent
	{
		protected BranchNode persistNode = null;
		protected Node node = null;
		protected ColumnNode columnNode;
		
		protected IDBSchemaUtilsDriver schemaUtilDriver = null;
		protected Connection connection = null;
		protected PreparedStatement preparedStatement = null;
		protected IRuntimeParameter  runtimeParameter = null;
		
		public BranchNode getPersistNode()
		{
			return persistNode;
		}
		public Node getNode()
		{
			return node;
		}
		public IDBSchemaUtilsDriver getSchemaUtilDriver()
		{
			return schemaUtilDriver;
		}
		public Connection getConnection()
		{
			return connection;
		}
		public PreparedStatement getPreparedStatement()
		{
			return preparedStatement;
		}
		public IRuntimeParameter getRuntimeParameter()
		{
			return runtimeParameter;
		}
		public ColumnNode getColumnNode()
		{
			return columnNode;
		}

		protected static class ConvertEventProvider extends ConvertEvent
		{
			protected ConvertEventProvider()
			{
				super();
			}
			public void setPersistNode(BranchNode persistNode)
			{
				super.persistNode = persistNode;
			}
			public void setNode(Node node)
			{
				super.node = node;
			}
			public void setSchemaUtilDriver(IDBSchemaUtilsDriver schemaUtilDriver)
			{
				super.schemaUtilDriver = schemaUtilDriver;
			}
			public void setConnection(Connection connection)
			{
				super.connection = connection;
			}
			public void setPreparedStatement(PreparedStatement preparedStatement)
			{
				super.preparedStatement = preparedStatement;
			}
			public void setRuntimeParameter(IRuntimeParameter runtimeParameter)
			{
				super.runtimeParameter = runtimeParameter;
			}
			public void setColumnNode(ColumnNode columnNode)
			{
				super.columnNode = columnNode;
			}
			
			protected void clear()
			{
				super.persistNode = null;
				super.node = null;
				super.columnNode = null;
				super.schemaUtilDriver = null;
				super.connection = null;
				super.preparedStatement = null;
				super.runtimeParameter = null;
			}
		}
	}

}
