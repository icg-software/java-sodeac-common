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
package org.sodeac.common.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class CommonPreviewBranchNodeType extends ReplicableBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(CommonPreviewBranchNodeType.class);}
	
	@SQLColumn(name="record_code",type=SQLColumnType.VARCHAR,length=108)
	@XmlAttribute(name="code")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonPreviewBranchNodeType,String> code;
	
	@SQLColumn(name="workmode_node_link",type=SQLColumnType.VARCHAR,length=1080)
	@XmlElement(name="workmode-node-link")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonPreviewBranchNodeType,String> workmodeNodeLink;
	
	@SQLColumn(name="record_name",type=SQLColumnType.VARCHAR,length=1080)
	@XmlElement(name="Name")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonPreviewBranchNodeType,String> name;
	
	@SQLColumn(name="record_abbr",type=SQLColumnType.VARCHAR,length=108)
	@XmlElement(name="Abbreviation")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonPreviewBranchNodeType,String> abbreviation;
}
