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

import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=DBSchemaTreeModel.class)
public class PrimaryKeyNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(PrimaryKeyNodeType.class);}
	
	public static volatile LeafNodeType<PrimaryKeyNodeType,String> indexName;
	public static volatile LeafNodeType<PrimaryKeyNodeType,String> constraintName;	
	public static volatile LeafNodeType<PrimaryKeyNodeType,String> tableSpace;
	public static volatile LeafNodeType<PrimaryKeyNodeType,Boolean> quotedName;
}
