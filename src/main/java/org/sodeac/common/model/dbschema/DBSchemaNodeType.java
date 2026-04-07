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
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.DBSchemaUtils.DBSchemaEvent;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass = DBSchemaTreeModel.class)
@GenerateBow
public class DBSchemaNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(DBSchemaNodeType.class);
    }
    
    public static volatile LeafNodeType<DBSchemaNodeType, String> name;
    public static volatile LeafNodeType<DBSchemaNodeType, String> dbmsSchemaName;
    public static volatile LeafNodeType<DBSchemaNodeType, String> tableSpaceData;
    public static volatile LeafNodeType<DBSchemaNodeType, String> tableSpaceIndex;
    public static volatile LeafNodeType<DBSchemaNodeType, Boolean> skipChecks;
    public static volatile LeafNodeType<DBSchemaNodeType, Boolean> logUpdates;
    public static volatile BranchNodeListType<DBSchemaNodeType, TableNodeType> tables;
    public static volatile BranchNodeListType<DBSchemaNodeType, EventConsumerNodeType> consumers;
    
    @BowMethod
    public static void addConsumer(@BowParameter(self = true) BranchNode<?, DBSchemaNodeType> schema, ExceptionCatchedConsumer<DBSchemaEvent> consumer)
    {
        schema.create(DBSchemaNodeType.consumers).setValue(EventConsumerNodeType.eventConsumer, consumer);
    }
}
