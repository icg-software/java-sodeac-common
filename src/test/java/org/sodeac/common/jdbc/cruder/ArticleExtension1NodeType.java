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

@SQLTable(name="article_extension_1")
@TypedTreeModel(modelClass=MiniMerchandiseManagementModel.class)
public class ArticleExtension1NodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(ArticleExtension1NodeType.class);}
	
	public static RootBranchNode<MiniMerchandiseManagementModel, ArticleExtension1NodeType> newNode()
	{
		return MiniMerchandiseManagementModel.get().createRootNode(ArticleExtension1NodeType.class);
	}
	
	@SQLColumn(name="article_feature_1",length=108)
	public static volatile LeafNodeType<ArticleExtension1NodeType,String> feature1;
	
	@SQLColumn(name="article_feature_2",length=108)
	public static volatile LeafNodeType<ArticleExtension1NodeType,String> feature2;
	
	@SQLColumn(name="article_feature_3",length=108)
	public static volatile LeafNodeType<ArticleExtension1NodeType,String> feature3;
	
}
