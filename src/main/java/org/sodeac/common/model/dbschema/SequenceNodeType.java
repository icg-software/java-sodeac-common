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
package org.sodeac.common.model.dbschema;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=DBSchemaTreeModel.class)
@GenerateBow(buildAlias=true)
public class SequenceNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(SequenceNodeType.class);}
	
	public static volatile LeafNodeType<SequenceNodeType,String> name;
	public static volatile LeafNodeType<SequenceNodeType,String> dbmsSchemaName;	
	public static volatile LeafNodeType<SequenceNodeType,Long> min;
	public static volatile LeafNodeType<SequenceNodeType,Long> max;
	public static volatile LeafNodeType<SequenceNodeType,Long> cache;
	public static volatile LeafNodeType<SequenceNodeType,Boolean> cycle;
}
