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

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.XMLNodeList;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class StacktraceNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(StacktraceNodeType.class);}
	
	@XMLNodeList(childElementName="StacktraceElement", listElement=false)
	public static volatile BranchNodeListType<StacktraceNodeType,StacktraceElementNodeType> elements;
}
