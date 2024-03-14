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
package org.sodeac.common.model;

import jakarta.xml.bind.annotation.XmlAttribute;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.IgnoreIfFalse;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class StacktraceElementNodeType extends BranchNodeMetaModel 
{
	static{ModelRegistry.getBranchNodeMetaModel(StacktraceElementNodeType.class);}
	
	@XmlAttribute(name="class")
	public static volatile LeafNodeType<StacktraceElementNodeType,String> className;
	
	@XmlAttribute(name="file")
	public static volatile LeafNodeType<StacktraceElementNodeType,String> fileName;
	
	@XmlAttribute(name="linenumber")
	public static volatile LeafNodeType<StacktraceElementNodeType,Integer> lineNumber;
	
	@XmlAttribute(name="methodname")
	public static volatile LeafNodeType<StacktraceElementNodeType,String> methodName;
	
	@XmlAttribute(name="native")
	@IgnoreIfFalse
	public static volatile LeafNodeType<StacktraceElementNodeType,Boolean> nativeMethod;
}
