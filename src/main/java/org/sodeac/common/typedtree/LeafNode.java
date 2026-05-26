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

import java.util.concurrent.locks.Lock;

import org.sodeac.common.typedtree.BranchNode.NodeContainer;

/**
 * A leaf node is an instance of simple tree node with a value, but without child nodes.
 *
 * @param <P> type of parent branch node
 * @param <T> type of node's value
 *
 * @author Sebastian Palarus
 */
public class LeafNode<P extends BranchNodeMetaModel, T> extends Node<P, T>
{
    
    /**
     * Constructor for leaf node
     *
     * @param parentNode    parent node instance
     * @param nodeContainer container for leaf node
     */
    protected LeafNode(final BranchNode<?, P> parentNode, final NodeContainer nodeContainer)
    {
        super();
        this.parentNode = parentNode;
        this.nodeContainer = nodeContainer;
    }
    
    private NodeContainer nodeContainer = null;
    private BranchNode<?, P> parentNode = null;
    private T value = null;
    
    /**
     * Dispose this node.
     */
    @Override
    protected void disposeNode()
    {
        super.disposed = true;
        this.value = null;
        this.parentNode = null;
        this.nodeContainer = null;
    }
    
    /**
     * Getter for value of leaf node.
     *
     * @return value of leaf node.
     */
    public T getValue()
    {
        return this.value;
    }
    
    /**
     * Setter for value of leaf node.
     *
     * @param value leaf node's value to set.
     *
     * @return this leaf node
     */
    public LeafNode<P, T> setValue(final T value)
    {
        if (this.parentNode.getRootNode().isImmutable())
        {
            return this;
        }
        Lock lock = this.parentNode.getRootNode().isSynchronized() ? this.parentNode.getRootNode().getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            T oldValue = this.value;
            if
            (
                this.parentNode.getRootNode().notifyBeforeModify
                    (
                        this.parentNode,
                        this.nodeContainer,
                        oldValue,
                        value
                    )
            )
            {
                this.value = value;
                
                this.parentNode.getRootNode().notifyAfterModify
                    (
                        this.parentNode,
                        this.nodeContainer,
                        oldValue,
                        value
                    );
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        return this;
    }
    
    /**
     * Getter for parent node.
     *
     * @return parent node.
     */
    protected BranchNode<?, P> getParentNode()
    {
        return this.parentNode;
    }
    
    @Override
    public INodeType<P, T> getNodeType()
    {
        return this.nodeContainer.getNodeType();
    }
    
}
