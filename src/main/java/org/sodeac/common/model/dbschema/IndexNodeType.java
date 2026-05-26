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
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass = DBSchemaTreeModel.class)
@GenerateBow(buildAlias = true)
public class IndexNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(IndexNodeType.class);
    }
    
    public static volatile LeafNodeType<IndexNodeType, String> name;
    public static volatile LeafNodeType<IndexNodeType, String> dbmsSchemaName;
    public static volatile LeafNodeType<IndexNodeType, String> tableSpace;
    public static volatile LeafNodeType<IndexNodeType, Boolean> quotedName;
    public static volatile LeafNodeType<IndexNodeType, Boolean> unique;
    public static volatile BranchNodeListType<IndexNodeType, IndexColumnNodeType> members;
}
