/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.typedtree;

import java.util.Set;

@FunctionalInterface
public interface ITypedTreeModelParserHandler
{
    default public void startModel(BranchNodeMetaModel model, Set<INodeType<BranchNodeMetaModel, ?>> references) { }
    
    ;
    
    default public void endModel(BranchNodeMetaModel model, Set<INodeType<BranchNodeMetaModel, ?>> references) { }
    
    ;
    
    public void onNodeType(BranchNodeMetaModel model, INodeType<BranchNodeMetaModel, ?> nodeType);
}
