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

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class CommonListChunkBaseBranchNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(CommonListChunkBaseBranchNodeType.class);}
	
	@XmlAttribute(name="listsize")
	public static volatile LeafNodeType<CommonListChunkBaseBranchNodeType,Long> listSize;
	
	@XmlAttribute(name="chunksize")
	public static volatile LeafNodeType<CommonListChunkBaseBranchNodeType,Long> chunkSize;
	
	@XmlAttribute(name="chunkseq")
	public static volatile LeafNodeType<CommonListChunkBaseBranchNodeType,Long> chunkSequnece;
	
	@XmlAttribute(name="itemsize")
	public static volatile LeafNodeType<CommonListChunkBaseBranchNodeType,Integer> itemSize;
	
	@XmlAttribute(name="last")
	public static volatile LeafNodeType<CommonListChunkBaseBranchNodeType,Boolean> last;
	
}
