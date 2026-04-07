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
package org.sodeac.common.jdbc.cruder;

import org.sodeac.common.model.CommonBaseBranchNodeType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLTable;
import org.sodeac.common.typedtree.annotation.SQLUniqueIndex;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@SQLTable(name="article_extension_2")
@TypedTreeModel(modelClass=MiniMerchandiseManagementModel.class)
public class ArticleExtension2NodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(ArticleExtension2NodeType.class);}
	
	public static RootBranchNode<MiniMerchandiseManagementModel, ArticleExtension2NodeType> newNode()
	{
		return MiniMerchandiseManagementModel.get().createRootNode(ArticleExtension2NodeType.class);
	}
	
	@SQLColumn(name="article_feature_a",length=108)
	public static volatile LeafNodeType<ArticleExtension2NodeType,String> featureA;
	
	@SQLColumn(name="article_feature_b",length=108)
	public static volatile LeafNodeType<ArticleExtension2NodeType,String> featureB;
	
	@SQLColumn(name="article_feature_c",length=108)
	public static volatile LeafNodeType<ArticleExtension2NodeType,String> featureC;
	
}
