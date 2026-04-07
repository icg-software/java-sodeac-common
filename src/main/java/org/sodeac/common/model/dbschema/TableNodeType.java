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
package org.sodeac.common.model.dbschema;

import org.sodeac.common.annotation.BowMethod;
import org.sodeac.common.annotation.BowMethod.ReturnBowMode;
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.DBSchemaUtils.DBSchemaEvent;
import org.sodeac.common.jdbc.IColumnType;
import org.sodeac.common.jdbc.schemax.IDefaultBySequence;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentDate;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentTime;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentTimestamp;
import org.sodeac.common.jdbc.schemax.IDefaultUUID;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass = DBSchemaTreeModel.class)
@GenerateBow(buildAlias = true)
public class TableNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(TableNodeType.class);
    }
    
    public static volatile LeafNodeType<TableNodeType, String> name;
    public static volatile LeafNodeType<TableNodeType, String> dbmsSchemaName;
    public static volatile LeafNodeType<TableNodeType, String> tableSpace;
    public static volatile LeafNodeType<TableNodeType, Boolean> quotedName;
    public static volatile BranchNodeListType<TableNodeType, ColumnNodeType> columns;
    public static volatile BranchNodeListType<TableNodeType, IndexNodeType> indices;
    public static volatile BranchNodeListType<TableNodeType, EventConsumerNodeType> consumers;
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createCharColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable, final int length)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.CHAR.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.size, length)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createVarcharColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable, final int length)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.VARCHAR.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.size, length)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createClobColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.CLOB.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createUUIDColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.UUID.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createUUIDColumnDefaultAuto(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.UUID.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultValueClass, IDefaultUUID.class);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBooleanColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BOOLEAN.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBooleanColumnWithDefault(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BOOLEAN.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Boolean.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createSmallIntColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.SMALLINT.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createSmallIntColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final short defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.SMALLINT.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Short.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createIntegerColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.INTEGER.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createIntegerColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final int defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.INTEGER.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Integer.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBigIntColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BIGINT.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBigIntAutoIncrementColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final String sequenceName)
    {
        return createBigIntAutoIncrementColumn(table, columnName, sequenceName, null);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBigIntAutoIncrementColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final String sequenceName, final Long cache)
    {
        final BranchNode<TableNodeType, ColumnNodeType> column = table.create(TableNodeType.columns)
                                                                      .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BIGINT.name())
                                                                      .setValue(ColumnNodeType.name, columnName)
                                                                      .setValue(ColumnNodeType.nullable, false)
                                                                      .setValue(ColumnNodeType.defaultValueClass, IDefaultBySequence.class);
        
        column.create(ColumnNodeType.sequence)
              .setValue(SequenceNodeType.name, sequenceName)
              .setValue(SequenceNodeType.min, 1L)
              .setValue(SequenceNodeType.max, Long.MAX_VALUE)
              .setValue(SequenceNodeType.cycle, false)
              .setValue(SequenceNodeType.cache, cache);
        
        return column;
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBigIntColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final long defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BIGINT.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Long.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createRealColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.REAL.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createRealColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final float defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.REAL.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Float.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createDoubleColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.DOUBLE.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createDoubleColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final double defaultValue)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.DOUBLE.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultStaticValue, Double.toString(defaultValue));
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createTimeColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.TIME.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createTimeColumnDefaultCurrent(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.TIME.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultValueClass, IDefaultCurrentTime.class);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createDateColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.DATE.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createDateColumnDefaultCurrent(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.DATE.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultValueClass, IDefaultCurrentDate.class);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createTimestampColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.TIMESTAMP.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createTimestampColumnDefaultCurrent(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.TIMESTAMP.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, false)
                    .setValue(ColumnNodeType.defaultValueClass, IDefaultCurrentTimestamp.class);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBinaryColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BINARY.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, ColumnNodeType> createBlobColumn(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final String columnName, final boolean nullable)
    {
        return table.create(TableNodeType.columns)
                    .setValue(ColumnNodeType.columnType, IColumnType.ColumnType.BLOB.name())
                    .setValue(ColumnNodeType.name, columnName)
                    .setValue(ColumnNodeType.nullable, nullable);
    }
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.NESTED_BOW)
    public static BranchNode<TableNodeType, IndexNodeType> createIndex(@BowParameter(self = true) final BranchNode<?, TableNodeType> table, final boolean unique, final String keyName, final String... columnNames)
    {
        if (columnNames == null)
        {
            throw new IllegalStateException("no columns defined");
        }
        if (columnNames.length == 0)
        {
            throw new IllegalStateException("no columns defined");
        }
        final BranchNode<TableNodeType, IndexNodeType> index = table.create(TableNodeType.indices)
                                                                    .setValue(IndexNodeType.unique, unique)
                                                                    .setValue(IndexNodeType.name, keyName);
        for (final String columnName : columnNames)
        {
            index.create(IndexNodeType.members).setValue(IndexColumnNodeType.columName, columnName);
        }
        
        return index;
    }
    
    @BowMethod
    public static void addConsumer(final BranchNode<?, TableNodeType> table, final ExceptionCatchedConsumer<DBSchemaEvent> consumer)
    {
        table.create(TableNodeType.consumers).setValue(EventConsumerNodeType.eventConsumer, consumer);
    }
}
