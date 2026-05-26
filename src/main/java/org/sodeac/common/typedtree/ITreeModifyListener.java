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

import org.sodeac.common.function.ConplierBean;

/**
 *
 * A tree modify listener is a low level modify listener. It notifies for all modifications in tree.
 *
 * @author Sebastian Palarus
 *
 */
public interface ITreeModifyListener
{
    /**
     *
     * Notify  before modification is invoked.
     *
     * @param parentNode             parent node of modified node
     * @param staticNodeTypeInstance static child node type instance from meta model
     * @param oldValue
     * @param newValue
     * @param doit
     */
    default <C extends INodeType<?, ?>, T> void beforeModify(final BranchNode<?, ?> parentNode, final Object staticNodeTypeInstance, final T oldValue, final T newValue, final ConplierBean<Boolean> doit) { }
    
    default <C extends INodeType<?, ?>, T> void afterModify(final BranchNode<?, ?> parentNode, final Object staticNodeTypeInstance, final T oldValue, final T newValue) { }
    
}
