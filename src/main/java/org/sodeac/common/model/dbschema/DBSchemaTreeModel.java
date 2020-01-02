/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.model.dbschema;

import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;


public class DBSchemaTreeModel extends TypedTreeMetaModel<DBSchemaTreeModel> 
{
	static{ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class);}
	
	public static volatile BranchNodeType<DBSchemaTreeModel,DBSchemaNodeType> schema;
	
	public static BranchNode<DBSchemaTreeModel, DBSchemaNodeType> newSchema(String name)
	{
		RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class).createRootNode(DBSchemaTreeModel.schema);
		schema.setValue(DBSchemaNodeType.name, name);
		
		return schema;
	}
	
	public static RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> newSchema(String name, String dbmsSchemaName)
	{
		RootBranchNode<DBSchemaTreeModel, DBSchemaNodeType> schema = ModelRegistry.getTypedTreeMetaModel(DBSchemaTreeModel.class).createRootNode(DBSchemaTreeModel.schema);
		
		schema
			.setValue(DBSchemaNodeType.name, name)
			.setValue(DBSchemaNodeType.dbmsSchemaName, dbmsSchemaName);
		
		return schema;
	}
}
