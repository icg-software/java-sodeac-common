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
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.Association;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLIndex;
import org.sodeac.common.typedtree.annotation.SQLReferencedByColumn;
import org.sodeac.common.typedtree.annotation.SQLReplace;
import org.sodeac.common.typedtree.annotation.SQLTable;
import org.sodeac.common.typedtree.annotation.SQLUniqueIndex;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.Association.AssociationType;

@SQLTable(name="article_group")
@TypedTreeModel(modelClass=MiniMerchandiseManagementModel.class)
public class ArticleGroupNodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(ArticleGroupNodeType.class);}
	
	public static RootBranchNode<MiniMerchandiseManagementModel, ArticleGroupNodeType> newNode()
	{
		return MiniMerchandiseManagementModel.get().createRootNode(ArticleGroupNodeType.class);
	}
	
	@SQLColumn(name="group_number",nullable=false,updatable=false)
	@SQLUniqueIndex
	public static volatile LeafNodeType<ArticleGroupNodeType,Long> number; 
	
	@SQLColumn(name="group_name",nullable=false,length=108)
	@SQLUniqueIndex
	public static volatile LeafNodeType<ArticleGroupNodeType,String> name;
	
	@SQLColumn(name="group_tax",nullable=true)
	public static volatile LeafNodeType<ArticleGroupNodeType,Double> tax;
	
	@SQLReferencedByColumn(name="article_group_id")
	@SQLIndex
	@Association(type=AssociationType.COMPOSITION)
	@SQLReplace(table=@SQLTable(name="article_group_property"))
	public static volatile BranchNodeListType<ArticleGroupNodeType,CommonGenericPropertyNodeType> propertyList;
}
