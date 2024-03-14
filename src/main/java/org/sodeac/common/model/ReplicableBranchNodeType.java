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

import java.util.UUID;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.jdbc.schemax.IDefaultBySequence;
import org.sodeac.common.jdbc.schemax.IDefaultStaticValue;
import org.sodeac.common.jdbc.schemax.IDefaultUUID;
import org.sodeac.common.model.CommonBaseBranchNodeType.FalseIfNull;
import org.sodeac.common.model.CommonBaseBranchNodeType.GenerateUUID;
import org.sodeac.common.model.CommonBaseBranchNodeType.GenerateUUIDIfNull;
import org.sodeac.common.model.CommonBaseBranchNodeType.ValueBySequence;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.IgnoreIfFalse;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLPrimaryKey;
import org.sodeac.common.typedtree.annotation.SQLSequence;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class ReplicableBranchNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(ReplicableBranchNodeType.class);}
	
	public static UUID MAIN_RECORD_ID = new UUID(0,0);
	
	@SQLColumn(name="id",type=SQLColumnType.UUID,nullable=false,updatable=false,onInsert=GenerateUUIDIfNull.class,defaultValueExpressionDriver=IDefaultUUID.class)
	@SQLPrimaryKey
	@XmlAttribute(name="id")
	public static volatile LeafNodeType<ReplicableBranchNodeType,UUID> id;
	
	@SQLColumn(name="record_version_no",type=SQLColumnType.BIGINT,nullable=false,defaultValueExpressionDriver=IDefaultBySequence.class,onUpsert=ValueBySequence.class)
	@XmlElement(name="RecordVersionNumber")
	@SQLSequence
	@IgnoreIfNull
	public static volatile LeafNodeType<ReplicableBranchNodeType,Long> persistVersionNumber;
	
	@SQLColumn(name="record_version_id",type=SQLColumnType.UUID,nullable=false,onUpsert=GenerateUUID.class,defaultValueExpressionDriver=IDefaultUUID.class)
	@XmlElement(name="RecordVersionId")
	@IgnoreIfNull
	public static volatile LeafNodeType<ReplicableBranchNodeType,UUID> persistVersionId;
	
	@SQLColumn(name="record_offline_writing",type=SQLColumnType.BOOLEAN,nullable=false,onInsert=FalseIfNull.class,defaultValueExpressionDriver=IDefaultStaticValue.class,staticDefaultValue="false")
	@XmlAttribute(name="offline-writing")
	@IgnoreIfFalse
	public static volatile LeafNodeType<ReplicableBranchNodeType,Boolean> offlineWriting;
	
	@SQLColumn(name="record_dirty_cache",type=SQLColumnType.BOOLEAN,nullable=false,onInsert=FalseIfNull.class,defaultValueExpressionDriver=IDefaultStaticValue.class,staticDefaultValue="false")
	@XmlAttribute(name="dirty-cache")
	@IgnoreIfFalse
	public static volatile LeafNodeType<ReplicableBranchNodeType,Boolean> dirtyCache;
	
	@SQLColumn(name="record_basedon_version_no",type=SQLColumnType.BIGINT,nullable=true)
	@XmlElement(name="BasedOnRecordVersionNumber")
	@SQLSequence
	@IgnoreIfNull
	public static volatile LeafNodeType<ReplicableBranchNodeType,Long> basedOnVersionNumber;
	
	@SQLColumn(name="record_basedon_version_id",type=SQLColumnType.UUID,nullable=true)
	@XmlElement(name="BasedOnRecordVersionId")
	@IgnoreIfNull
	public static volatile LeafNodeType<ReplicableBranchNodeType,UUID> basedOnVersionId;
	
	@SQLColumn(name="record_deleted",type=SQLColumnType.BOOLEAN,nullable=false,onInsert=FalseIfNull.class,defaultValueExpressionDriver=IDefaultStaticValue.class,staticDefaultValue="false")
	@XmlAttribute(name="deleted")
	@IgnoreIfFalse
	public static volatile LeafNodeType<ReplicableBranchNodeType,Boolean> deleted;
	
	@SQLColumn(name="record_partition",type=SQLColumnType.VARCHAR,length=1080,nullable=true)
	@XmlAttribute(name="partition")
	@IgnoreIfNull
	public static volatile LeafNodeType<ReplicableBranchNodeType,String> partition;
}
