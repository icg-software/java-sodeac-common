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

import java.util.function.Consumer;

import org.sodeac.common.annotation.BowMethod;
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.BowParameter.AutomaticConsumer;
import org.sodeac.common.annotation.GenerateBowFactory;
import org.sodeac.common.annotation.Version;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.annotation.Domain;

@Domain(name = "sodeac.org", module = "dbschema")
@Version(major = 0, minor = 6)
@GenerateBowFactory
public class DBSchemaTreeModel extends TypedTreeMetaModel<DBSchemaTreeModel>
{
    static
    {
        ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class);
    }
    
    public static volatile BranchNodeType<DBSchemaTreeModel, DBSchemaNodeType> schema;
    
    @BowMethod(convertReturnValueToBow = true, name = "createSchema", createBowFromReturnValue = true)
    public static RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> newSchema(final String name)
    {
        RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class).createRootNode(DBSchemaTreeModel.schema);
        schema.setValue(DBSchemaNodeType.name, name);
        
        return schema;
    }
    
    public static RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> newSchema(final String name, final String dbmsSchemaName)
    {
        return newSchema(name, dbmsSchemaName, null);
    }
    
    @BowMethod(convertReturnValueToBow = true, name = "createSchema")
    protected static RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> newSchema(final String name, final String dbmsSchemaName, @BowParameter(automaticConsumerMode = AutomaticConsumer.NEW_BOW_BY_RETURNTYPE) final Consumer<RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType>> onRootNodeCreated)
    {
        RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class).createRootNode(DBSchemaTreeModel.schema);
        
        if (onRootNodeCreated != null)
        {
            onRootNodeCreated.accept(schema);
        }
        
        schema
            .setValue(DBSchemaNodeType.name, name)
            .setValue(DBSchemaNodeType.dbmsSchemaName, dbmsSchemaName);
        
        return schema;
    }
}
