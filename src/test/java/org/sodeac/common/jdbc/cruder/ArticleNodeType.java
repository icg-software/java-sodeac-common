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
import org.sodeac.common.model.CommonGenericPropertyNodeType;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.Association;
import org.sodeac.common.typedtree.annotation.Association.AssociationType;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLTable;
import org.sodeac.common.typedtree.annotation.SQLUniqueIndex;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.SQLIndex;
import org.sodeac.common.typedtree.annotation.SQLReferencedByColumn;
import org.sodeac.common.typedtree.annotation.SQLReplace;

@SQLTable(name="article")
@TypedTreeModel(modelClass=MiniMerchandiseManagementModel.class)
public class ArticleNodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(ArticleNodeType.class);}
	
	public static RootBranchNode<MiniMerchandiseManagementModel, ArticleNodeType> newNode()
	{
		return MiniMerchandiseManagementModel.get().createRootNode(ArticleNodeType.class);
	}
	
	@SQLColumn(name="article_number",nullable=false,updatable=false)
	@SQLUniqueIndex
	public static volatile LeafNodeType<ArticleNodeType,Long> number; 
	
	@SQLColumn(name="article_name",nullable=false,length=108)
	@SQLUniqueIndex
	public static volatile LeafNodeType<ArticleNodeType,String> name;
	
	@SQLColumn(name="article_description",nullable=true,type=SQLColumnType.CLOB)
	public static volatile LeafNodeType<ArticleNodeType,String> description;
	
	@SQLColumn(name="article_group_id", nullable=true)
	@Association(type=AssociationType.AGGREGATION)
	public static volatile BranchNodeType<ArticleNodeType,ArticleGroupNodeType> group;
	
	@SQLColumn(name="article_extension_1_id", nullable=true)
	@Association(type=AssociationType.COMPOSITION)
	@SQLUniqueIndex
	public static volatile BranchNodeType<ArticleNodeType,ArticleExtension1NodeType> extension1;
	
	@SQLReferencedByColumn(name="article_id")
	@Association(type=AssociationType.COMPOSITION)
	@SQLUniqueIndex
	public static volatile BranchNodeType<ArticleNodeType,ArticleExtension2NodeType> extension2;
	
	@SQLReferencedByColumn(name="article_id")
	@SQLIndex
	@Association(type=AssociationType.COMPOSITION)
	@SQLReplace(table=@SQLTable(name="article_property"))
	public static volatile BranchNodeListType<ArticleNodeType,CommonGenericPropertyNodeType> propertyList;
	
	
}
