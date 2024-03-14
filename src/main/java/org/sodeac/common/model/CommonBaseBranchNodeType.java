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
package org.sodeac.common.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.ConvertEvent;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode.ColumnNode;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentTimestamp;
import org.sodeac.common.jdbc.schemax.IDefaultStaticValue;
import org.sodeac.common.typedtree.LeafNode;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.IgnoreIfTrue;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class CommonBaseBranchNodeType extends ReplicableBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(CommonBaseBranchNodeType.class);}
	
	@SQLColumn(name="create_timestamp",type=SQLColumnType.TIMESTAMP,nullable=false,updatable=false,onInsert=CurrentTimestamp.class,defaultValueExpressionDriver=IDefaultCurrentTimestamp.class)
	@XmlElement(name="CreateTimestamp")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,Date> createTimestamp;
	
	@SQLColumn(name="persist_timestamp",type=SQLColumnType.TIMESTAMP,nullable=false,onUpsert=CurrentTimestamp.class,defaultValueExpressionDriver=IDefaultCurrentTimestamp.class)
	@XmlElement(name="PersistTimestamp")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,Date> persistTimestamp;
	
	@SQLColumn(name="create_node_id",type=SQLColumnType.UUID,updatable=false)
	@XmlElement(name="CreateNodeId")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,UUID> createNodeId;
	
	@SQLColumn(name="persist_node_id",type=SQLColumnType.UUID)
	@XmlElement(name="PersistNodeId")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,UUID> persistNodeId;

	@SQLColumn(name="create_client_uri",type=SQLColumnType.VARCHAR,length=1080,updatable=false)
	@XmlElement(name="CreateClientURI")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,String> createClientURI;
	
	@SQLColumn(name="persist_client_uri",type=SQLColumnType.VARCHAR,length=1080)
	@XmlElement(name="PersistClientURI")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,String> persistClientURI;
	
	@SQLColumn(name="record_enabled",type=SQLColumnType.BOOLEAN,nullable=false,onInsert=TrueIfNull.class,defaultValueExpressionDriver=IDefaultStaticValue.class,staticDefaultValue="true")
	@XmlAttribute(name="enabled")
	@IgnoreIfTrue
	public static volatile LeafNodeType<CommonBaseBranchNodeType,Boolean> enabled;
	
	@SQLColumn(name="record_valid_from",type=SQLColumnType.DATE)
	@XmlAttribute(name="valid-from")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,Date> validFrom;
	
	@SQLColumn(name="record_valid_through",type=SQLColumnType.DATE)
	@XmlAttribute(name="valid-through")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonBaseBranchNodeType,Date> validThrough;
	
	public static class GenerateUUIDIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(t.getNode().getNodeType().getTypeClass() == UUID.class)
			{
				if(((LeafNode<?,UUID>)t.getNode()).getValue() == null)
				{
					((LeafNode<?,UUID>)t.getNode()).setValue(UUID.randomUUID());
				}
			}
			else if(t.getNode().getNodeType().getTypeClass() == String.class)
			{
				if((((LeafNode<?,String>)t.getNode()).getValue() == null) || ((LeafNode<?,String>)t.getNode()).getValue().isEmpty())
				{
					((LeafNode<?,String>)t.getNode()).setValue(UUID.randomUUID().toString());
				}
			}
			else
			{
				throw new IllegalStateException("Unsupported type for generating UUID: " + t.getNode().getNodeType().getTypeClass());
			}
		}
	}
	
	public static class GenerateUUID implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(t.getNode().getNodeType().getTypeClass() == UUID.class)
			{
				((LeafNode<?,UUID>)t.getNode()).setValue(UUID.randomUUID());
			}
			else if(t.getNode().getNodeType().getTypeClass() == String.class)
			{
				((LeafNode<?,String>)t.getNode()).setValue(UUID.randomUUID().toString());
			}
			else
			{
				throw new IllegalStateException("Unsupported type for generating UUID: " + t.getNode().getNodeType().getTypeClass());
			}
		}
	}
	
	public static class CurrentTimestamp implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			((LeafNode<?,Date>)t.getNode()).setValue(new Date());
		}
	}
	
	public static class CurrentDate implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			((LeafNode<?,Date>)t.getNode()).setValue(cal.getTime());
		}
	}
	
	public static class TrueIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Boolean>)t.getNode()).setValue(true);
			}
		}
	}
	
	public static class FalseIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Boolean>)t.getNode()).setValue(false);
			}
		}
	}
	
	public static class LongZeroIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Long>)t.getNode()).setValue(0L);
			}
		}
	}
	
	public static class DoubleZeroIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Double>)t.getNode()).setValue(0.0d);
			}
		}
	}
	
	public static class IntegerZeroIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Integer>)t.getNode()).setValue(0);
			}
		}
	}
	
	public static class FloatZeroIfNull implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(((LeafNode)t.getNode()).getValue() == null) 
			{
				((LeafNode<?,Float>)t.getNode()).setValue(0.0f);
			}
		}
	}
	
	public static class ValueBySequence implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
	{
		@Override
		public void accept(ConvertEvent t)
		{
			if(t.getNode().getNodeType().getTypeClass() != Long.class)
			{
				throw new IllegalStateException("Sequence for long nodes not supported for type " + t.getNode().getNodeType().getTypeClass()); 
			}
			
			ColumnNode columnNode = t.getColumnNode();
			
			String sequenceName = columnNode.getSequenceName();
			if(sequenceName.isEmpty())
			{
				sequenceName = "seq_" + columnNode.getTableName() + "_" + columnNode.getColumnName();
				
			}
			
			Connection connection = t.getConnection();
			IDBSchemaUtilsDriver driver = t.getSchemaUtilDriver();
			
			try
			{
				long next = driver.nextFromSequence(DBSchemaUtils.getSchema(connection), sequenceName, connection);
				((LeafNode<?,Long>)t.getNode()).setValue(next);
			}
			catch (SQLException e) 
			{
				throw new RuntimeException(e);
			}
		}
	}
}
