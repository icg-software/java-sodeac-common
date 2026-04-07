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

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.jdbc.IDefaultValueExpressionDriver;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=DBSchemaTreeModel.class)
@GenerateBow(buildAlias=true)
public class ColumnNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(ColumnNodeType.class);}
	
	public static volatile LeafNodeType<ColumnNodeType,String> name;
	public static volatile LeafNodeType<ColumnNodeType,String> columnType;
	public static volatile LeafNodeType<ColumnNodeType,Boolean> quotedName;
	public static volatile LeafNodeType<ColumnNodeType,Boolean> nullable;
	public static volatile LeafNodeType<ColumnNodeType,Integer> size;
	public static volatile LeafNodeType<ColumnNodeType,String> defaultStaticValue;
	public static volatile LeafNodeType<ColumnNodeType,Class<? extends IDefaultValueExpressionDriver>> defaultValueClass;
	
	public static volatile BranchNodeType<ColumnNodeType,PrimaryKeyNodeType> primaryKey;
	public static volatile BranchNodeType<ColumnNodeType,ForeignKeyNodeType> foreignKey;
	public static volatile BranchNodeType<ColumnNodeType,SequenceNodeType> sequence;
}
