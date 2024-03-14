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

import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.annotation.Domain;
import org.sodeac.common.annotation.GenerateBowFactory;
import org.sodeac.common.annotation.Version;

@Domain(name="sodeac.org",module="core")
@Version(major=0,minor=6)
@GenerateBowFactory
public class CoreTreeModel extends TypedTreeMetaModel<CoreTreeModel> 
{
	static{ModelRegistry.getTypedTreeMetaModel(CoreTreeModel.class);}
	
	@XmlElement(name="Throwable")
	public static volatile BranchNodeType<CoreTreeModel,ThrowableNodeType> throwable;
	
	@XmlElement(name="Stacktrace")
	public static volatile BranchNodeType<CoreTreeModel,StacktraceNodeType> stacktrace;
}
