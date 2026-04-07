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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sodeac.common.jdbc.IColumnType.ColumnType;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.MASK;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode.ColumnNode;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.DBSchemaTreeModel;
import org.sodeac.common.model.dbschema.ForeignKeyNodeType;
import org.sodeac.common.model.dbschema.IndexColumnNodeType;
import org.sodeac.common.model.dbschema.IndexNodeType;
import org.sodeac.common.model.dbschema.PrimaryKeyNodeType;
import org.sodeac.common.model.dbschema.SequenceNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.ITypedTreeModelParserHandler;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.INodeType;

public class ParseDBSchemaHandler implements ITypedTreeModelParserHandler 
{
	public ParseDBSchemaHandler(String database)
	{
		super();
		this.schema = DBSchemaTreeModel.newSchema(database);
	}
	
	public ParseDBSchemaHandler(RootBranchNode<?, DBSchemaNodeType> schema)
	{
		super();
		this.schema = schema;
	}
	
	private RootBranchNode<?, DBSchemaNodeType> schema = null;
	private Map<String,TableNode> tableNodes = new HashMap<>();
	
	@Override
	public void startModel(BranchNodeMetaModel model, Set<INodeType<BranchNodeMetaModel, ?>> references) 
	{
		Objects.nonNull(this.schema);
		Objects.nonNull(this.tableNodes);
		
		if(references == null)
		{
			return;
		}
		
		for(INodeType<? extends BranchNodeMetaModel, ?> nodeType : references)
		{
			TableNode tableNode = TypedTreeJDBCHelper.parseTableNode(nodeType, MASK.ALL);
			
			if(tableNode == null)
			{
				continue;
			}
			
			String schema = tableNode.getTableSchema();
			String tableName = tableNode.getTableName();
			
			String tableKey = ((schema == null) || (schema.isEmpty())) ? tableName : schema + "." + tableName;
			if((!tableNode.isTableSkipSchemaGeneration()) || (! this.tableNodes.containsKey(tableKey)))
			{
				this.tableNodes.put(tableKey, tableNode);
			}
		}
	}
	
	public RootBranchNode<?, DBSchemaNodeType> fillSchemaSpec(Class<? extends TypedTreeMetaModel<?>>... modelClasses) 
	{
		try
		{
			Set<Class<? extends TypedTreeMetaModel<?>>> modelClassSet = null;
			if((modelClasses != null) && (modelClasses.length > 0))
			{
				modelClassSet = new HashSet<>();
				for(Class<? extends TypedTreeMetaModel<?>> modelClass : modelClasses)
				{
					modelClassSet.add(modelClass);
				}
			}
			
			for(Entry<String,TableNode> tableNodeEntry : this.tableNodes.entrySet())
			{
				Map<String,BranchNode<TableNodeType,IndexNodeType>> indexSet = new HashMap<>();
				Map<String,BranchNode<TableNodeType,IndexNodeType>> uniqueIndexSet = new HashMap<>();
				
				TableNode tableNode = tableNodeEntry.getValue();
				
				if(tableNode.isTableSkipSchemaGeneration())
				{
					continue;
				}
				
				if(modelClassSet != null)
				{
					if(! modelClassSet.contains(tableNode.getModelClass()))
					{
						continue;
					}
				}
				ColumnNode primaryKeyColumn = tableNode.getPrimaryKeyNode();
				
				Objects.requireNonNull(primaryKeyColumn, "primary key is requiered for table " + tableNodeEntry.getKey());
				
				//	Table
				
				BranchNode<DBSchemaNodeType, TableNodeType>  tableSpec = this.schema.create(DBSchemaNodeType.tables).setValue(TableNodeType.name,tableNode.getTableName());
				if((tableNode.getTableSchema() != null) && (! tableNode.getTableSchema().isEmpty()))
				{
					tableSpec.setValue(TableNodeType.dbmsSchemaName, tableNode.getTableSchema());
				}
				
				// PrimaryKey
				
				ColumnType pkColumnType = TypedTreeJDBCHelper.getColumnType(primaryKeyColumn.getSqlType(),primaryKeyColumn.getJavaType());
				if(pkColumnType == ColumnType.VARCHAR)
				{
					if(primaryKeyColumn.getLength() < 0)
					{
						pkColumnType = ColumnType.CLOB;
					}
				}
				
				BranchNode<TableNodeType, ColumnNodeType> columnSpec = tableSpec.create(TableNodeType.columns)
					.setValue(ColumnNodeType.name, primaryKeyColumn.getColumnName())
					.setValue(ColumnNodeType.columnType, pkColumnType.name())
					.setValue(ColumnNodeType.nullable, primaryKeyColumn.isNullable());
				
				if((pkColumnType == ColumnType.VARCHAR) || (pkColumnType == ColumnType.VARCHAR))
				{
					columnSpec.setValue(ColumnNodeType.size, primaryKeyColumn.getLength());
				}
				
				if(primaryKeyColumn.getDefaultValueExpressionDriver() != null)
				{
					columnSpec.setValue(ColumnNodeType.defaultValueClass,primaryKeyColumn.getDefaultValueExpressionDriver());
					columnSpec.setValue(ColumnNodeType.defaultStaticValue, primaryKeyColumn.getStaticDefaultValue());
				}
				
				if(primaryKeyColumn.isSequence())
				{
					columnSpec.create(ColumnNodeType.sequence)
						.setValue(SequenceNodeType.name, primaryKeyColumn.getSequenceName())
						.setValue(SequenceNodeType.min, primaryKeyColumn.getSequenceMinValue())
						.setValue(SequenceNodeType.max, primaryKeyColumn.getSequenceMaxValue())
						.setValue(SequenceNodeType.cycle, primaryKeyColumn.getSequenceCycle())
						.setValue(SequenceNodeType.cache, primaryKeyColumn.getSequenceCache());
				}
				
				columnSpec.create(ColumnNodeType.primaryKey).setValue(PrimaryKeyNodeType.constraintName, "pk_" + tableNode.getTableName());
				
				if(primaryKeyColumn.getIndexSet() != null)
				{
					for(String indexName : primaryKeyColumn.getIndexSet())
					{
						BranchNode<TableNodeType,IndexNodeType> indexSpec = tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, false);
						indexSet.put(indexName, indexSpec);
						indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, primaryKeyColumn.getColumnName());
					}
				}
				
				if(primaryKeyColumn.getUniqueIndexSet() != null)
				{
					for(String indexName : primaryKeyColumn.getUniqueIndexSet())
					{
						BranchNode<TableNodeType,IndexNodeType> indexSpec = tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, true);
						uniqueIndexSet.put(indexName, indexSpec);
						indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, primaryKeyColumn.getColumnName());
					}
				}
				
				// Parent Referenced By
				
				ColumnNode referencedByColumnNode = tableNode.getReferencedByColumnNode();
				
				if(referencedByColumnNode != null)
				{
					ColumnNode parentPKColumnNode = referencedByColumnNode.getReferencedPrimaryKey();
					
					Objects.requireNonNull(parentPKColumnNode, tableNodeEntry.getKey() + ": pk column of parent table not defined / reference: " + tableNode.getNodeType().getParentNodeClass() + "::" + tableNode.getNodeType().getNodeName() );
					
					ColumnType columnType = TypedTreeJDBCHelper.getColumnType(referencedByColumnNode.getSqlType(),referencedByColumnNode.getJavaType());
					if(columnType == ColumnType.VARCHAR)
					{
						if(referencedByColumnNode.getLength() < 0)
						{
							columnType = ColumnType.CLOB;
						}
					}
					
					columnSpec = tableSpec.create(TableNodeType.columns)
						.setValue(ColumnNodeType.name, referencedByColumnNode.getColumnName())
						.setValue(ColumnNodeType.columnType, columnType.name())
						.setValue(ColumnNodeType.nullable, referencedByColumnNode.isNullable());
					
					if((columnType == ColumnType.VARCHAR) || (columnType == ColumnType.VARCHAR))
					{
						columnSpec.setValue(ColumnNodeType.size, referencedByColumnNode.getLength());
					}
					
					columnSpec.create(ColumnNodeType.foreignKey)
						.setValue(ForeignKeyNodeType.constraintName, "fk_" + tableNode.getTableName()  + "__" + referencedByColumnNode.getColumnName())
						.setValue(ForeignKeyNodeType.referencedTableName, parentPKColumnNode.getTableName()) // TODO referenced Schema
						.setValue(ForeignKeyNodeType.referencedColumnName, parentPKColumnNode.getColumnName());
					
					if(referencedByColumnNode.getIndexSet() != null)
					{
						for(String indexName : referencedByColumnNode.getIndexSet())
						{
							BranchNode<TableNodeType,IndexNodeType> indexSpec = indexSet.get(indexName);
							if(indexSpec == null)
							{
								indexSpec = tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, false);
								indexSet.put(indexName, indexSpec);
							}
							indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, referencedByColumnNode.getColumnName());
						}
					}
					
					if(referencedByColumnNode.getUniqueIndexSet() != null)
					{
						for(String indexName : referencedByColumnNode.getUniqueIndexSet())
						{
							BranchNode<TableNodeType,IndexNodeType> indexSpec = uniqueIndexSet.get(indexName);
							if(indexSpec == null)
							{
								indexSpec =tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, true);
								uniqueIndexSet.put(indexName, indexSpec);
							}
							indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, referencedByColumnNode.getColumnName());
						}
					}
				}
				
				for(ColumnNode columnNode : tableNode.getColumnList())
				{
					if(columnNode == primaryKeyColumn)
					{
						continue;
					}
					
					ColumnType columnType = TypedTreeJDBCHelper.getColumnType(columnNode.getSqlType(),columnNode.getJavaType());
					if(columnType == ColumnType.VARCHAR)
					{
						if(columnNode.getLength() < 0)
						{
							columnType = ColumnType.CLOB;
						}
					}
					
					columnSpec = tableSpec.create(TableNodeType.columns)
						.setValue(ColumnNodeType.name, columnNode.getColumnName())
						.setValue(ColumnNodeType.columnType, columnType.name())
						.setValue(ColumnNodeType.nullable, columnNode.isNullable());
					
					if((columnType == ColumnType.VARCHAR) || (columnType == ColumnType.VARCHAR))
					{
						columnSpec.setValue(ColumnNodeType.size, columnNode.getLength());
					}
					
					if(columnNode.getDefaultValueExpressionDriver() != null)
					{
						columnSpec.setValue(ColumnNodeType.defaultValueClass,columnNode.getDefaultValueExpressionDriver());
						columnSpec.setValue(ColumnNodeType.defaultStaticValue, columnNode.getStaticDefaultValue());
					}
					
					if(columnNode.isSequence())
					{
						columnSpec.create(ColumnNodeType.sequence)
							.setValue(SequenceNodeType.name, columnNode.getSequenceName())
							.setValue(SequenceNodeType.min, columnNode.getSequenceMinValue())
							.setValue(SequenceNodeType.max, columnNode.getSequenceMaxValue())
							.setValue(SequenceNodeType.cycle, columnNode.getSequenceCycle())
							.setValue(SequenceNodeType.cache, columnNode.getSequenceCache());
					}
					
					if(columnNode.getReferencedPrimaryKey() != null)
					{
						columnSpec.create(ColumnNodeType.foreignKey)
							.setValue(ForeignKeyNodeType.constraintName, "fk_" + columnNode.getTableName()  + "__" + columnNode.getColumnName())
							.setValue(ForeignKeyNodeType.referencedTableName, columnNode.getReferencedPrimaryKey().getTableName()) // TODO referenced Schema
							.setValue(ForeignKeyNodeType.referencedColumnName, columnNode.getReferencedPrimaryKey().getColumnName());
					}
					
					if(columnNode.getIndexSet() != null)
					{
						for(String indexName : columnNode.getIndexSet())
						{
							BranchNode<TableNodeType,IndexNodeType> indexSpec = indexSet.get(indexName);
							if(indexSpec == null)
							{
								indexSpec = tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, false);
								indexSet.put(indexName, indexSpec);
							}
							indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, columnNode.getColumnName());
						}
					}
					
					if(columnNode.getUniqueIndexSet() != null)
					{
						for(String indexName : columnNode.getUniqueIndexSet())
						{
							BranchNode<TableNodeType,IndexNodeType> indexSpec = uniqueIndexSet.get(indexName);
							if(indexSpec == null)
							{
								indexSpec =tableSpec.create(TableNodeType.indices).setValue(IndexNodeType.name, indexName).setValue(IndexNodeType.unique, true);
								uniqueIndexSet.put(indexName, indexSpec);
							}
							indexSpec.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, columnNode.getColumnName());
						}
					}
				}
				
				indexSet.clear();
				uniqueIndexSet.clear();
			}
			
			return schema;
		}
		finally 
		{
			
			tableNodes.clear();
			tableNodes = null;
			schema = null;
		}
	}

	@Override
	public void onNodeType(BranchNodeMetaModel model, INodeType<BranchNodeMetaModel, ?> nodeType){}

}
