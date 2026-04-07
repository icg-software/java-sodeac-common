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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sodeac.common.jdbc.IColumnType.ColumnType;
import org.sodeac.common.jdbc.TypedTreeJDBCHelper.TableNode.ColumnNode;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.INodeType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.SQLIndex;
import org.sodeac.common.typedtree.annotation.SQLPrimaryKey;
import org.sodeac.common.typedtree.annotation.SQLReferencedByColumn;
import org.sodeac.common.typedtree.annotation.SQLReplace;
import org.sodeac.common.typedtree.annotation.SQLSequence;
import org.sodeac.common.typedtree.annotation.SQLTable;
import org.sodeac.common.typedtree.annotation.SQLUniqueIndex;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

public class TypedTreeJDBCHelper
{
    public enum MASK
    {ALL, PK_COLUMN, COLUMNS, LEAFNODE_COLUMNS, BRANCHNODE_COLUMNS}
    
    ;
    
    public static TableNode parseTableNode(INodeType nodeType, MASK... masks)
    {
        TableNode tableNode = new TypedTreeJDBCHelper().new TableNode();
        tableNode.nodeType = nodeType;
        
        Set<MASK> maskIndex = new HashSet<>();
        
        if (masks == null)
        {
            maskIndex.add(MASK.ALL);
        }
        if (masks.length == 0)
        {
            maskIndex.add(MASK.ALL);
        }
        for (int i = 0; i < masks.length; i++)
        {
            maskIndex.add(masks[i]);
        }
        
        BranchNodeMetaModel defaultInstance = (BranchNodeMetaModel) nodeType.getValueDefaultInstance();
        
        SQLTable table = (SQLTable) nodeType.getTypeClass().getAnnotation(SQLTable.class);
        SQLReplace[] replaces = (SQLReplace[]) nodeType.referencedByField().getAnnotationsByType(SQLReplace.class);
        
        for (SQLReplace replace : replaces)
        {
            for (SQLTable replacedTable : replace.table())
            {
                table = replacedTable;
            }
        }
        
        if (table == null)
        {
            return null;
        }
        
        tableNode.tableName = table.name();
        tableNode.tableCatalog = table.catalog();
        tableNode.tableSchema = table.schema();
        tableNode.tableReadable = table.readable();
        tableNode.tableInsertable = table.insertable();
        tableNode.tableUpdatable = table.updatable();
        tableNode.tableSkipSchemaGeneration = table.skipSchemaGeneration();
        
        if (nodeType.getTypeClass().getAnnotation(TypedTreeModel.class) != null)
        {
            tableNode.modelClass = ((TypedTreeModel) nodeType.getTypeClass().getAnnotation(TypedTreeModel.class)).modelClass();
        }
        
        // replaces by referenced field ???
        
        if (nodeType.getParentNodeClass() != null)
        {
            ColumnNode parentPKColumnNode = null;
            BranchNodeMetaModel defaultInstanceParent = ModelRegistry.getBranchNodeMetaModel(nodeType.getParentNodeClass());
            SQLTable tableParent = (SQLTable) nodeType.getParentNodeClass().getAnnotation(SQLTable.class);
            for (LeafNodeType<BranchNodeMetaModel, ?> leafNodeType : defaultInstanceParent.getLeafNodeTypeList())
            {
                SQLColumn sqlColumn = leafNodeType.referencedByField().getAnnotation(SQLColumn.class);
                SQLPrimaryKey primaryKey = leafNodeType.referencedByField().getAnnotation(SQLPrimaryKey.class);
                if ((primaryKey != null) && (sqlColumn != null))
                {
                    if (parentPKColumnNode != null)
                    {
                        throw new IllegalArgumentException("Multiple PKs not suppoerted");
                    }
                    
                    SQLSequence sequence = (SQLSequence) leafNodeType.referencedByField().getAnnotation(SQLSequence.class);
                    
                    parentPKColumnNode = tableNode.new ColumnNode();
                    
                    if (tableParent != null)
                    {
                        parentPKColumnNode.leafNodeType = leafNodeType;
                        parentPKColumnNode.tableName = tableParent.name();
                        parentPKColumnNode.tableCatalog = tableParent.catalog();
                        parentPKColumnNode.tableSchema = tableParent.schema();
                        parentPKColumnNode.tableReadable = tableParent.readable();
                        parentPKColumnNode.tableInsertable = tableParent.insertable();
                        parentPKColumnNode.tableUpdatable = tableParent.updatable();
                    }
                    parentPKColumnNode.name = sqlColumn.name();
                    parentPKColumnNode.nullable = sqlColumn.nullable();
                    parentPKColumnNode.sqlType = sqlColumn.type();
                    parentPKColumnNode.length = sqlColumn.length();
                    parentPKColumnNode.readable = sqlColumn.readable();
                    parentPKColumnNode.insertable = sqlColumn.insertable();
                    parentPKColumnNode.updatable = sqlColumn.updatable();
                    parentPKColumnNode.staticDefaultValue = sqlColumn.staticDefaultValue();
                    parentPKColumnNode.defaultValueExpressionDriver = sqlColumn.defaultValueExpressionDriver();
                    parentPKColumnNode.onInsert = sqlColumn.onInsert();
                    parentPKColumnNode.onUpdate = sqlColumn.onUpdate();
                    parentPKColumnNode.onUpsert = sqlColumn.onUpsert();
                    parentPKColumnNode.node2JDBC = sqlColumn.nodeValue2JDBC();
                    parentPKColumnNode.JDBC2Node = sqlColumn.JDBC2NodeValue();
                    parentPKColumnNode.javaType = leafNodeType.getTypeClass();
                    
                    if (parentPKColumnNode.name.isEmpty() && (leafNodeType != null))
                    {
                        parentPKColumnNode.name = leafNodeType.getNodeName();
                    }
                    
                    if (parentPKColumnNode.onInsert == SQLColumn.NoConsumer.class)
                    {
                        parentPKColumnNode.onInsert = null;
                    }
                    if (parentPKColumnNode.onUpdate == SQLColumn.NoConsumer.class)
                    {
                        parentPKColumnNode.onUpdate = null;
                    }
                    if (parentPKColumnNode.onUpsert == SQLColumn.NoConsumer.class)
                    {
                        parentPKColumnNode.onUpsert = null;
                    }
                    if (parentPKColumnNode.node2JDBC == SQLColumn.NoNode2JDBC.class)
                    {
                        parentPKColumnNode.node2JDBC = null;
                    }
                    if (parentPKColumnNode.JDBC2Node == SQLColumn.NoJDBC2Node.class)
                    {
                        parentPKColumnNode.JDBC2Node = null;
                    }
                    if (parentPKColumnNode.defaultValueExpressionDriver == SQLColumn.NoDefaultValueExpressionDriver.class)
                    {
                        parentPKColumnNode.defaultValueExpressionDriver = null;
                    }
                    
                    resolveAutoColumn(parentPKColumnNode);
                    
                    parentPKColumnNode.primaryKeyAutoGenerated = primaryKey.autoGenerated();
                    parentPKColumnNode.isPrimaryKey = true;
                    
                    if (sequence != null)
                    {
                        parentPKColumnNode.isSequence = true;
                        parentPKColumnNode.sequenceName = sequence.name();
                        parentPKColumnNode.sequenceMinValue = sequence.min();
                        parentPKColumnNode.sequenceMaxValue = sequence.max();
                        parentPKColumnNode.sequenceCache = sequence.cache();
                        parentPKColumnNode.sequenceCycle = sequence.cycle();
                        
                        if (parentPKColumnNode.sequenceName == null)
                        {
                            parentPKColumnNode.sequenceName = "seq_" + parentPKColumnNode.getTableName() + "_" + parentPKColumnNode.getColumnName();
                        }
                        if (parentPKColumnNode.sequenceCache < 1L)
                        {
                            parentPKColumnNode.sequenceCache = null;
                        }
                    }
                }
            }
            
            SQLReferencedByColumn referencedByColumn = nodeType.referencedByField().getAnnotation(SQLReferencedByColumn.class);
            if (referencedByColumn != null)
            {
                tableNode.referencedByColumnNode = tableNode.new ColumnNode();
                tableNode.referencedByColumnNode.referencedPrimaryKey = parentPKColumnNode;
                
                if (nodeType instanceof BranchNodeType)
                {
                    tableNode.referencedByColumnNode.branchNodeType = (BranchNodeType) nodeType;
                }
                else if (nodeType instanceof BranchNodeListType)
                {
                    tableNode.referencedByColumnNode.branchNodeListType = (BranchNodeListType) nodeType;
                }
                
                tableNode.referencedByColumnNode.name = referencedByColumn.name();
                tableNode.referencedByColumnNode.nullable = referencedByColumn.nullable();
                tableNode.referencedByColumnNode.sqlType = SQLColumnType.AUTO;
                if (parentPKColumnNode != null)
                {
                    tableNode.referencedByColumnNode.sqlType = parentPKColumnNode.sqlType;
                }
                
                if (referencedByColumn.type() != SQLColumnType.AUTO)
                {
                    tableNode.referencedByColumnNode.sqlType = referencedByColumn.type();
                }
                
                if (referencedByColumn.length() < 1) // defaultValue
                {
                    tableNode.referencedByColumnNode.length = parentPKColumnNode.length;
                }
                else
                {
                    tableNode.referencedByColumnNode.length = referencedByColumn.length();
                }
                
                tableNode.referencedByColumnNode.insertable = referencedByColumn.insertable();
                tableNode.referencedByColumnNode.updatable = referencedByColumn.updatable();
                tableNode.referencedByColumnNode.readable = referencedByColumn.readable();
                
                if (parentPKColumnNode != null)
                {
                    tableNode.referencedByColumnNode.javaType = parentPKColumnNode.javaType;
                    tableNode.referencedByColumnNode.referencedPrimaryKey = parentPKColumnNode;
                }
                
                if (tableNode.referencedByColumnNode.name.isEmpty())
                {
                    tableNode.referencedByColumnNode.name = nodeType.getNodeName();
                }
                
                if (tableNode.referencedByColumnNode.sqlType == SQLColumnType.AUTO)
                {
                    Objects.requireNonNull(tableNode.referencedByColumnNode.javaType, "PK type of " + nodeType.getParentNodeClass() + " not found");
                    resolveAutoColumn(tableNode.referencedByColumnNode);
                }
                
                for (SQLIndex sqlIndex : nodeType.referencedByField().getAnnotationsByType(SQLIndex.class))
                {
                    if (tableNode.referencedByColumnNode.indexSet == null)
                    {
                        tableNode.referencedByColumnNode.indexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        tableNode.referencedByColumnNode.indexSet.add("idx_" + tableNode.getTableName() + "__" + tableNode.referencedByColumnNode.getColumnName());
                    }
                    else
                    {
                        tableNode.referencedByColumnNode.indexSet.add(sqlIndex.name());
                    }
                }
                
                for (SQLUniqueIndex sqlIndex : nodeType.referencedByField().getAnnotationsByType(SQLUniqueIndex.class))
                {
                    if (tableNode.referencedByColumnNode.uniqueIndexSet == null)
                    {
                        tableNode.referencedByColumnNode.uniqueIndexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        tableNode.referencedByColumnNode.uniqueIndexSet.add("unq_" + tableNode.getTableName() + "__" + tableNode.referencedByColumnNode.getColumnName());
                    }
                    else
                    {
                        tableNode.referencedByColumnNode.uniqueIndexSet.add(sqlIndex.name());
                    }
                }
            }
        }
        
        ColumnNode primaryKeyNode = null;
        
        if ((maskIndex.contains(MASK.ALL) || maskIndex.contains(MASK.COLUMNS) || maskIndex.contains(MASK.LEAFNODE_COLUMNS) || maskIndex.contains(MASK.PK_COLUMN)))
        {
            for (LeafNodeType leafNodeType : defaultInstance.getLeafNodeTypeList())
            {
                SQLColumn sqlColumn = (SQLColumn) leafNodeType.referencedByField().getAnnotation(SQLColumn.class);
                SQLPrimaryKey primaryKey = (SQLPrimaryKey) leafNodeType.referencedByField().getAnnotation(SQLPrimaryKey.class);
                SQLSequence sequence = (SQLSequence) leafNodeType.referencedByField().getAnnotation(SQLSequence.class);
                
                for (SQLReplace replace : replaces)
                {
                    if (!leafNodeType.getNodeName().equals(replace.nodeName()))
                    {
                        continue;
                    }
                    for (SQLColumn replacedColumn : replace.column())
                    {
                        
                        sqlColumn = replacedColumn;
                        break;
                    }
                    for (SQLPrimaryKey replacedPrimaryKey : replace.primaryKey())
                    {
                        primaryKey = replacedPrimaryKey;
                        break;
                    }
                    for (SQLSequence replacedSequence : replace.sequence())
                    {
                        sequence = replacedSequence;
                        break;
                    }
                }
                if (sqlColumn == null)
                {
                    continue;
                }
                
                ColumnNode columnNode = tableNode.new ColumnNode();
                
                columnNode.leafNodeType = leafNodeType;
                columnNode.tableName = table.name();
                columnNode.tableCatalog = table.catalog();
                columnNode.tableSchema = table.schema();
                columnNode.tableReadable = table.readable();
                columnNode.tableInsertable = table.insertable();
                columnNode.tableUpdatable = table.updatable();
                columnNode.name = sqlColumn.name();
                columnNode.nullable = sqlColumn.nullable();
                columnNode.sqlType = sqlColumn.type();
                columnNode.length = sqlColumn.length();
                columnNode.readable = sqlColumn.readable();
                columnNode.insertable = sqlColumn.insertable();
                columnNode.updatable = sqlColumn.updatable();
                columnNode.staticDefaultValue = sqlColumn.staticDefaultValue();
                columnNode.defaultValueExpressionDriver = sqlColumn.defaultValueExpressionDriver();
                columnNode.onInsert = sqlColumn.onInsert();
                columnNode.onUpdate = sqlColumn.onUpdate();
                columnNode.onUpsert = sqlColumn.onUpsert();
                columnNode.node2JDBC = sqlColumn.nodeValue2JDBC();
                columnNode.JDBC2Node = sqlColumn.JDBC2NodeValue();
                columnNode.javaType = leafNodeType.getTypeClass();
                
                if (columnNode.name.isEmpty() && (leafNodeType != null))
                {
                    columnNode.name = leafNodeType.getNodeName();
                }
                
                if (columnNode.onInsert == SQLColumn.NoConsumer.class)
                {
                    columnNode.onInsert = null;
                }
                if (columnNode.onUpdate == SQLColumn.NoConsumer.class)
                {
                    columnNode.onUpdate = null;
                }
                if (columnNode.onUpsert == SQLColumn.NoConsumer.class)
                {
                    columnNode.onUpsert = null;
                }
                if (columnNode.node2JDBC == SQLColumn.NoNode2JDBC.class)
                {
                    columnNode.node2JDBC = null;
                }
                if (columnNode.JDBC2Node == SQLColumn.NoJDBC2Node.class)
                {
                    columnNode.JDBC2Node = null;
                }
                if (columnNode.defaultValueExpressionDriver == SQLColumn.NoDefaultValueExpressionDriver.class)
                {
                    columnNode.defaultValueExpressionDriver = null;
                }
                
                resolveAutoColumn(columnNode);
                
                for (SQLIndex sqlIndex : leafNodeType.referencedByField().getAnnotationsByType(SQLIndex.class))
                {
                    if (columnNode.indexSet == null)
                    {
                        columnNode.indexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        columnNode.indexSet.add("idx_" + columnNode.getTableName() + "__" + columnNode.getColumnName());
                    }
                    else
                    {
                        columnNode.indexSet.add(sqlIndex.name());
                    }
                }
                
                for (SQLUniqueIndex sqlIndex : leafNodeType.referencedByField().getAnnotationsByType(SQLUniqueIndex.class))
                {
                    if (columnNode.uniqueIndexSet == null)
                    {
                        columnNode.uniqueIndexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        columnNode.uniqueIndexSet.add("unq_" + columnNode.getTableName() + "__" + columnNode.getColumnName());
                    }
                    else
                    {
                        columnNode.uniqueIndexSet.add(sqlIndex.name());
                    }
                }
                
                if (primaryKey != null)
                {
                    if (primaryKeyNode != null)
                    {
                        throw new IllegalArgumentException("Multiple PKs not suppoerted");
                    }
                    primaryKeyNode = columnNode;
                    columnNode.primaryKeyAutoGenerated = primaryKey.autoGenerated();
                    columnNode.isPrimaryKey = true;
                    tableNode.primaryKeyNode = primaryKeyNode;
                }
                
                if (sequence != null)
                {
                    columnNode.isSequence = true;
                    columnNode.sequenceName = sequence.name();
                    columnNode.sequenceMinValue = sequence.min();
                    columnNode.sequenceMaxValue = sequence.max();
                    columnNode.sequenceCache = sequence.cache();
                    columnNode.sequenceCycle = sequence.cycle();
                    
                    if (columnNode.sequenceName == null)
                    {
                        columnNode.sequenceName = "seq_" + columnNode.getTableName() + "_" + columnNode.getColumnName();
                    }
                    if (columnNode.sequenceCache < 1L)
                    {
                        columnNode.sequenceCache = null;
                    }
                }
                if ((maskIndex.contains(MASK.ALL) || maskIndex.contains(MASK.COLUMNS) || maskIndex.contains(MASK.LEAFNODE_COLUMNS)))
                {
                    tableNode.getColumnList().add(columnNode);
                }
            }
        }
        
        if ((maskIndex.contains(MASK.ALL) || maskIndex.contains(MASK.COLUMNS) || maskIndex.contains(MASK.BRANCHNODE_COLUMNS)))
        {
            for (BranchNodeType branchNodeType : defaultInstance.getBranchNodeTypeList())
            {
                
                SQLColumn sqlColumn = (SQLColumn) branchNodeType.referencedByField().getAnnotation(SQLColumn.class);
                SQLReferencedByColumn sqlReferencedByColumn = (SQLReferencedByColumn) branchNodeType.referencedByField().getAnnotation(SQLReferencedByColumn.class);
                
                for (SQLReplace replace : replaces)
                {
                    if (!branchNodeType.getNodeName().equals(replace.nodeName()))
                    {
                        continue;
                    }
                    for (SQLColumn replacedColumn : replace.column())
                    {
                        sqlColumn = replacedColumn;
                        break;
                    }
                }
                
                if ((sqlColumn != null) && (sqlReferencedByColumn != null))
                {
                    throw new IllegalStateException("@SQLColumn and @SQLReferencedByColumn defined");
                }
                
                if (sqlColumn == null)
                {
                    continue;
                }
                
                TableNode referencedTable = parseTableNode(branchNodeType, MASK.PK_COLUMN);
                if (referencedTable == null)
                {
                    throw new RuntimeException("no table found in " + branchNodeType.getTypeClass().getCanonicalName());
                }
                
                ColumnNode columnNode = tableNode.new ColumnNode();
                
                columnNode.branchNodeType = branchNodeType;
                columnNode.tableName = table.name();
                columnNode.tableCatalog = table.catalog();
                columnNode.tableSchema = table.schema();
                columnNode.tableReadable = table.readable();
                columnNode.tableInsertable = table.insertable();
                columnNode.tableUpdatable = table.updatable();
                
                columnNode.name = sqlColumn.name();
                columnNode.nullable = sqlColumn.nullable();
                columnNode.sqlType = sqlColumn.type();
                columnNode.length = sqlColumn.length();
                columnNode.readable = sqlColumn.readable();
                columnNode.insertable = sqlColumn.insertable();
                columnNode.updatable = sqlColumn.updatable();
                columnNode.staticDefaultValue = sqlColumn.staticDefaultValue();
                columnNode.defaultValueExpressionDriver = sqlColumn.defaultValueExpressionDriver();
                columnNode.onInsert = sqlColumn.onInsert();
                columnNode.onUpdate = sqlColumn.onUpdate();
                columnNode.onUpsert = sqlColumn.onUpsert();
                columnNode.node2JDBC = sqlColumn.nodeValue2JDBC();
                columnNode.JDBC2Node = sqlColumn.JDBC2NodeValue();
                
                ColumnNode referencedPrimaryKey = referencedTable.getPrimaryKeyNode();
                
                if (referencedPrimaryKey == null)
                {
                    throw new IllegalStateException("no primary key found in " + branchNodeType.getTypeClass().getCanonicalName());
                }
                
                columnNode.referencedPrimaryKey = referencedPrimaryKey;
                columnNode.javaType = referencedPrimaryKey.javaType;
                
                if ((columnNode.sqlType == SQLColumnType.AUTO) && (referencedPrimaryKey.sqlType != SQLColumnType.AUTO))
                {
                    columnNode.sqlType = referencedPrimaryKey.sqlType;
                }
                
                if (columnNode.name.isEmpty() && (branchNodeType != null))
                {
                    columnNode.name = branchNodeType.getNodeName();
                }
                
                if (columnNode.onInsert == SQLColumn.NoConsumer.class)
                {
                    columnNode.onInsert = null;
                }
                if (columnNode.onUpdate == SQLColumn.NoConsumer.class)
                {
                    columnNode.onUpdate = null;
                }
                if (columnNode.onUpsert == SQLColumn.NoConsumer.class)
                {
                    columnNode.onUpsert = null;
                }
                if (columnNode.node2JDBC == SQLColumn.NoNode2JDBC.class)
                {
                    columnNode.node2JDBC = null;
                }
                if (columnNode.JDBC2Node == SQLColumn.NoJDBC2Node.class)
                {
                    columnNode.JDBC2Node = null;
                }
                if (columnNode.defaultValueExpressionDriver == SQLColumn.NoDefaultValueExpressionDriver.class)
                {
                    columnNode.defaultValueExpressionDriver = null;
                }
                
                resolveAutoColumn(columnNode);
                
                for (SQLIndex sqlIndex : branchNodeType.referencedByField().getAnnotationsByType(SQLIndex.class))
                {
                    if (columnNode.indexSet == null)
                    {
                        columnNode.indexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        columnNode.indexSet.add("idx_" + columnNode.getTableName() + "__" + columnNode.getColumnName());
                    }
                    else
                    {
                        columnNode.indexSet.add(sqlIndex.name());
                    }
                }
                
                for (SQLUniqueIndex sqlIndex : branchNodeType.referencedByField().getAnnotationsByType(SQLUniqueIndex.class))
                {
                    if (columnNode.uniqueIndexSet == null)
                    {
                        columnNode.uniqueIndexSet = new HashSet<>();
                    }
                    if (sqlIndex.name().isEmpty())
                    {
                        columnNode.uniqueIndexSet.add("unq_" + columnNode.getTableName() + "__" + columnNode.getColumnName());
                    }
                    else
                    {
                        columnNode.uniqueIndexSet.add(sqlIndex.name());
                    }
                }
                
                tableNode.getColumnList().add(columnNode);
            }
        }
        
        return tableNode;
    }
    
    public class TableNode
    {
        private INodeType nodeType = null;
        private Class<? extends TypedTreeMetaModel<?>> modelClass = null;
        private String tableName = null;
        private String tableCatalog = null;
        private String tableSchema = null;
        private boolean tableSkipSchemaGeneration = false;
        private boolean tableReadable = true;
        private boolean tableInsertable = true;
        private boolean tableUpdatable = true;
        private ColumnNode primaryKeyNode = null;
        private List<ColumnNode> columnList = new ArrayList<TypedTreeJDBCHelper.TableNode.ColumnNode>();
        
        private ColumnNode referencedByColumnNode = null;
        
        public INodeType getNodeType()
        {
            return nodeType;
        }
        
        public String getTableName()
        {
            return tableName;
        }
        
        public String getTableCatalog()
        {
            return tableCatalog;
        }
        
        public String getTableSchema()
        {
            return tableSchema;
        }
        
        public boolean isTableReadable()
        {
            return tableReadable;
        }
        
        public boolean isTableInsertable()
        {
            return tableInsertable;
        }
        
        public boolean isTableUpdatable()
        {
            return tableUpdatable;
        }
        
        public ColumnNode getPrimaryKeyNode()
        {
            return primaryKeyNode;
        }
        
        public List<ColumnNode> getColumnList()
        {
            return columnList;
        }
        
        public ColumnNode getReferencedByColumnNode()
        {
            return referencedByColumnNode;
        }
        
        public boolean isTableSkipSchemaGeneration()
        {
            return tableSkipSchemaGeneration;
        }
        
        public Class<? extends TypedTreeMetaModel<?>> getModelClass()
        {
            return modelClass;
        }
        
        public class ColumnNode
        {
            private String tableName = null;
            private String tableCatalog = null;
            private String tableSchema = null;
            private boolean tableReadable = true;
            private boolean tableInsertable = true;
            private boolean tableUpdatable = true;
            private boolean isPrimaryKey = false;
            private LeafNodeType leafNodeType = null;
            private BranchNodeType branchNodeType = null;
            private BranchNodeListType branchNodeListType = null;
            private Class javaType = null;
            private String name = null;
            private boolean nullable = false;
            private SQLColumnType sqlType = null;
            private int length = 0;
            private boolean readable = true;
            private boolean insertable = true;
            private boolean updatable = false;
            private String staticDefaultValue = null;
            private Class<? extends IDefaultValueExpressionDriver> defaultValueExpressionDriver = null;
            private Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onInsert = null;
            private Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onUpdate = null;
            private Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onUpsert = null;
            private volatile Consumer<TypedTreeJDBCCruder.ConvertEvent> onInsertInstance = null;
            private volatile Consumer<TypedTreeJDBCCruder.ConvertEvent> onUpdateInstance = null;
            private volatile Consumer<TypedTreeJDBCCruder.ConvertEvent> onUpsertInstance = null;
            private Class<? extends Function<?, ?>> node2JDBC = null;
            private Class<? extends Function<?, ?>> JDBC2Node = null;
            private volatile Function<Object, Object> node2JDBCInstance = null;
            private volatile Function<Object, Object> JDBC2NodeInstance = null;
            private boolean primaryKeyAutoGenerated = false;
            private boolean isSequence = false;
            private String sequenceName = null;
            private Long sequenceMinValue = null;
            private Long sequenceMaxValue = null;
            private Long sequenceCache = null;
            private Boolean sequenceCycle = null;
            private ColumnNode referencedPrimaryKey = null;
            private Set<String> indexSet = null;
            private Set<String> uniqueIndexSet = null;
            
            public ColumnNode()
            {
                super();
            }
            
            public String getTableName()
            {
                return tableName;
            }
            
            public String getTableCatalog()
            {
                return tableCatalog;
            }
            
            public String getTableSchema()
            {
                return tableSchema;
            }
            
            public boolean isTableReadable()
            {
                return tableReadable;
            }
            
            public boolean isTableInsertable()
            {
                return tableInsertable;
            }
            
            public boolean isTableUpdatable()
            {
                return tableUpdatable;
            }
            
            public LeafNodeType getLeafNodeType()
            {
                return leafNodeType;
            }
            
            public BranchNodeType getBranchNodeType()
            {
                return branchNodeType;
            }
            
            public BranchNodeListType getBranchNodeListType()
            {
                return branchNodeListType;
            }
            
            public Class getJavaType()
            {
                return javaType;
            }
            
            public String getColumnName()
            {
                return name;
            }
            
            public boolean isNullable()
            {
                return nullable;
            }
            
            public SQLColumnType getSqlType()
            {
                return sqlType;
            }
            
            public int getLength()
            {
                return length;
            }
            
            public boolean isReadable()
            {
                return readable;
            }
            
            public boolean isInsertable()
            {
                return insertable;
            }
            
            public boolean isUpdatable()
            {
                return updatable;
            }
            
            public String getStaticDefaultValue()
            {
                return staticDefaultValue;
            }
            
            public Class<? extends IDefaultValueExpressionDriver> getDefaultValueExpressionDriver()
            {
                return defaultValueExpressionDriver;
            }
            
            public Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> getOnInsert()
            {
                return onInsert;
            }
            
            public Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> getOnUpdate()
            {
                return onUpdate;
            }
            
            public Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> getOnUpsert()
            {
                return onUpsert;
            }
            
            public Class<? extends Function<?, ?>> getNode2JDBC()
            {
                return node2JDBC;
            }
            
            public Class<? extends Function<?, ?>> getJDBC2Node()
            {
                return JDBC2Node;
            }
            
            public boolean isPrimaryKey()
            {
                return isPrimaryKey;
            }
            
            public boolean isPrimaryKeyAutoGenerated()
            {
                return primaryKeyAutoGenerated;
            }
            
            public boolean isSequence()
            {
                return isSequence;
            }
            
            public String getSequenceName()
            {
                return sequenceName;
            }
            
            public Long getSequenceMinValue()
            {
                return sequenceMinValue;
            }
            
            public Long getSequenceMaxValue()
            {
                return sequenceMaxValue;
            }
            
            public Long getSequenceCache()
            {
                return sequenceCache;
            }
            
            public Boolean getSequenceCycle()
            {
                return sequenceCycle;
            }
            
            public ColumnNode getReferencedPrimaryKey()
            {
                return referencedPrimaryKey;
            }
            
            public Set<String> getIndexSet()
            {
                return indexSet;
            }
            
            public Set<String> getUniqueIndexSet()
            {
                return uniqueIndexSet;
            }
            
            public Consumer<TypedTreeJDBCCruder.ConvertEvent> getOnInsertInstance() throws InstantiationException, IllegalAccessException
            {
                if (onInsert == null)
                {
                    return null;
                }
                
                Consumer<TypedTreeJDBCCruder.ConvertEvent> instance = this.onInsertInstance;
                if (instance != null)
                {
                    return instance;
                }
                
                instance = onInsert.newInstance();
                this.onInsertInstance = instance;
                
                return instance;
            }
            
            public Consumer<TypedTreeJDBCCruder.ConvertEvent> getOnUpdateInstance() throws InstantiationException, IllegalAccessException
            {
                if (onUpdate == null)
                {
                    return null;
                }
                
                Consumer<TypedTreeJDBCCruder.ConvertEvent> instance = this.onUpdateInstance;
                if (instance != null)
                {
                    return instance;
                }
                
                instance = onUpdate.newInstance();
                this.onUpdateInstance = instance;
                
                return instance;
            }
            
            public Consumer<TypedTreeJDBCCruder.ConvertEvent> getOnUpsertInstance() throws InstantiationException, IllegalAccessException
            {
                if (onUpsert == null)
                {
                    return null;
                }
                
                Consumer<TypedTreeJDBCCruder.ConvertEvent> instance = this.onUpsertInstance;
                if (instance != null)
                {
                    return instance;
                }
                
                instance = onUpsert.newInstance();
                this.onUpsertInstance = instance;
                
                return instance;
            }
            
            public Function<Object, Object> getNode2JDBCInstance() throws InstantiationException, IllegalAccessException
            {
                if (node2JDBC == null)
                {
                    return null;
                }
                Function<Object, Object> instance = this.node2JDBCInstance;
                if (instance != null)
                {
                    return instance;
                }
                
                instance = (Function) node2JDBC.newInstance();
                this.node2JDBCInstance = instance;
                return instance;
            }
            
            public Function<Object, Object> getJDBC2NodeInstance() throws InstantiationException, IllegalAccessException
            {
                if (JDBC2Node == null)
                {
                    return null;
                }
                Function<Object, Object> instance = this.JDBC2NodeInstance;
                if (instance != null)
                {
                    return instance;
                }
                
                instance = (Function) JDBC2Node.newInstance();
                this.JDBC2NodeInstance = instance;
                return instance;
            }
        }
    }
    
    protected static void resolveAutoColumn(ColumnNode columnNode)
    {
        if (columnNode.sqlType == SQLColumnType.AUTO)
        {
            if (columnNode.javaType == Boolean.class)
            {
                columnNode.sqlType = SQLColumnType.BOOLEAN;
            }
            else if (columnNode.javaType == Integer.class)
            {
                columnNode.sqlType = SQLColumnType.INTEGER;
            }
            else if (columnNode.javaType == Long.class)
            {
                columnNode.sqlType = SQLColumnType.BIGINT;
            }
            else if (columnNode.javaType == Float.class)
            {
                columnNode.sqlType = SQLColumnType.REAL;
            }
            else if (columnNode.javaType == Double.class)
            {
                columnNode.sqlType = SQLColumnType.DOUBLE;
            }
            else if (columnNode.javaType == Date.class)
            {
                columnNode.sqlType = SQLColumnType.TIMESTAMP;
            }
            else if (columnNode.javaType == UUID.class)
            {
                columnNode.sqlType = SQLColumnType.UUID;
            }
            else
            {
                columnNode.sqlType = SQLColumnType.VARCHAR;
            }
            
        }
    }
    
    public static ColumnType getColumnType(SQLColumnType type, Class clazz)
    {
        if (type == SQLColumnType.AUTO)
        {
            if (clazz == Boolean.class)
            {
                type = SQLColumnType.BOOLEAN;
            }
            else if (clazz == Integer.class)
            {
                type = SQLColumnType.INTEGER;
            }
            else if (clazz == Long.class)
            {
                type = SQLColumnType.BIGINT;
            }
            else if (clazz == Float.class)
            {
                type = SQLColumnType.REAL;
            }
            else if (clazz == Double.class)
            {
                type = SQLColumnType.DOUBLE;
            }
            else if (clazz == Date.class)
            {
                type = SQLColumnType.TIMESTAMP;
            }
            else if (clazz == UUID.class)
            {
                type = SQLColumnType.UUID;
            }
            else
            {
                type = SQLColumnType.VARCHAR;
            }
        }
        
        if (type == SQLColumnType.BIGINT)
        {
            return ColumnType.BIGINT;
        }
        if (type == SQLColumnType.INTEGER)
        {
            return ColumnType.INTEGER;
        }
        if (type == SQLColumnType.SMALLINT)
        {
            return ColumnType.SMALLINT;
        }
        if (type == SQLColumnType.REAL)
        {
            return ColumnType.REAL;
        }
        if (type == SQLColumnType.DOUBLE)
        {
            return ColumnType.DOUBLE;
        }
        if (type == SQLColumnType.TIMESTAMP)
        {
            return ColumnType.TIMESTAMP;
        }
        if (type == SQLColumnType.DATE)
        {
            return ColumnType.DATE;
        }
        if (type == SQLColumnType.TIME)
        {
            return ColumnType.TIME;
        }
        if (type == SQLColumnType.BOOLEAN)
        {
            return ColumnType.BOOLEAN;
        }
        if (type == SQLColumnType.UUID)
        {
            return ColumnType.UUID;
        }
        if (type == SQLColumnType.CLOB)
        {
            return ColumnType.CLOB;
        }
        if (type == SQLColumnType.BLOB)
        {
            return ColumnType.BLOB;
        }
        return ColumnType.VARCHAR;
    }
    
}