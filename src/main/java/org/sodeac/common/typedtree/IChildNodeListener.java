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

import java.util.function.BiConsumer;

public interface IChildNodeListener<T extends BranchNodeMetaModel> extends BiConsumer<Node<T, ?>, Object>
{
    
    @Override
    void accept(Node<T, ?> node, Object oldValue);
    
    interface ILeafNodeListener<T extends BranchNodeMetaModel, X> extends IChildNodeListener<T>
    {
        @Override
        default void accept(final Node<T, ?> node, final Object oldValue)
        {
            this.onUpdate((LeafNode) node, (X) oldValue);
        }
        
        void onUpdate(LeafNode<T, X> node, X oldValue);
    }
    
}
