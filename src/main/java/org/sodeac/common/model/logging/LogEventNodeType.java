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
package org.sodeac.common.model.logging;

import java.util.Date;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.jdbc.schemax.IDefaultBySequence;
import org.sodeac.common.model.CommonBaseBranchNodeType;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.Association;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLReferencedByColumn;
import org.sodeac.common.typedtree.annotation.SQLSequence;
import org.sodeac.common.typedtree.annotation.SQLTable;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.XMLNodeList;
import org.sodeac.common.typedtree.annotation.Association.AssociationType;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;

@SQLTable(name="sdc_log_event",updatable= false)
@TypedTreeModel(modelClass=LoggingTreeModel.class)
@GenerateBow
public class LogEventNodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(LogEventNodeType.class);}
	
	@SQLColumn(name="log_type",type=SQLColumnType.VARCHAR, nullable=false, length=540)
	@XmlAttribute(name="type")
	public static volatile LeafNodeType<LogEventNodeType,String> type;
	
	@SQLColumn(name="log_uri",type=SQLColumnType.VARCHAR, length=1080)
	@XmlAttribute(name="uri")
	public static volatile LeafNodeType<LogEventNodeType,String> uri;
	
	@SQLColumn(name="log_timestamp",type=SQLColumnType.TIMESTAMP, nullable=false)
	@XmlAttribute(name="timestamp")
	public static volatile LeafNodeType<LogEventNodeType,Date> timestamp;
	
	@SQLColumn(name="log_date",type=SQLColumnType.DATE, nullable=false)
	@XmlElement(name="Date")
	public static volatile LeafNodeType<LogEventNodeType,Date> date;
	
	@SQLColumn(name="log_time",type=SQLColumnType.TIME, nullable=false)
	@XmlElement(name="Time")
	public static volatile LeafNodeType<LogEventNodeType,Date> time;
	
	@SQLColumn(name="log_level_value",type=SQLColumnType.INTEGER, nullable=false)
	@XmlAttribute(name="loglevel")
	public static volatile LeafNodeType<LogEventNodeType,Integer> logLevelValue;
	
	@SQLColumn(name="log_level_name",type=SQLColumnType.VARCHAR, nullable=false, length=7)
	@XmlAttribute(name="loglevelname")
	public static volatile LeafNodeType<LogEventNodeType,String> logLevelName;
	
	@SQLColumn(name="log_seq",type=SQLColumnType.BIGINT, nullable=false,defaultValueExpressionDriver=IDefaultBySequence.class,onUpsert=ValueBySequence.class)
	@SQLSequence(name="seq_sdc_log_event_log_seq",cycle=true)
	@XmlElement(name="Sequence")
	public static volatile LeafNodeType<LogEventNodeType,Long> sequence;
	
	@SQLColumn(name="log_domain",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Domain")
	@IgnoreIfNull
	public static volatile LeafNodeType<LogEventNodeType,String> domain;
	
	@SQLColumn(name="log_module",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Module")
	@IgnoreIfNull
	public static volatile LeafNodeType<LogEventNodeType,String> module;
	
	@SQLColumn(name="log_task",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Task")
	@IgnoreIfNull
	public static volatile LeafNodeType<LogEventNodeType,String> task;
	
	@SQLColumn(name="log_msg_format",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Format")
	@IgnoreIfNull
	public static volatile LeafNodeType<LogEventNodeType,String> format;
	
	@SQLColumn(name="log_msg_value",type=SQLColumnType.CLOB, nullable=true)
	@XmlElement(name="Message")
	public static volatile LeafNodeType<LogEventNodeType,String> message;
	
	@SQLReferencedByColumn(name="sdc_log_event_id", nullable=false)
	@Association(type=AssociationType.COMPOSITION)
	@XMLNodeList(childElementName="Property", listElement=false)
	public static volatile BranchNodeListType<LogEventNodeType,LogPropertyNodeType> properties;
}
