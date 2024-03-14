package org.sodeac.common.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.SQLIndex;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class CommonGenericPropertyNodeType extends CommonBaseBranchNodeType
{
	static{ModelRegistry.getBranchNodeMetaModel(CommonGenericPropertyNodeType.class);}
	
	@SQLColumn(name="property_type",type=SQLColumnType.VARCHAR, nullable=false, length=540)
	@XmlAttribute(name="type")
	@SQLIndex
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> type;
	
	@SQLColumn(name="property_key",type=SQLColumnType.VARCHAR, nullable=false, length=540)
	@SQLIndex
	@XmlAttribute(name="key")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> key;
	
	@SQLColumn(name="property_domain",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Domain")
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> domain;
	
	@SQLColumn(name="property_module",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Module")
	@IgnoreIfNull
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> module;
	
	@SQLColumn(name="property_format",type=SQLColumnType.VARCHAR, nullable=true, length=1080)
	@XmlElement(name="Format")
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> format;
	
	@SQLColumn(name="property_value",type=SQLColumnType.CLOB, nullable=true)
	@XmlElement(name="Value")
	public static volatile LeafNodeType<CommonGenericPropertyNodeType,String> value;
	

}
