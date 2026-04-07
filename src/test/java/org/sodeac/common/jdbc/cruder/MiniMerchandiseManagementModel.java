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

import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.annotation.Domain;
import org.sodeac.common.annotation.Version;

@Domain(name="test.sodeac.org",module="minimerchandisemanagement")
@Version(major=0,minor=6)
public class MiniMerchandiseManagementModel extends TypedTreeMetaModel<MiniMerchandiseManagementModel>
{
	static{get();}
	
	public static MiniMerchandiseManagementModel get()
	{
		return ModelRegistry.getTypedTreeMetaModel(MiniMerchandiseManagementModel.class);
	}
	
	public static volatile BranchNodeType<MiniMerchandiseManagementModel,ArticleNodeType> article;
}
