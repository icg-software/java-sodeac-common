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
package org.sodeac.common.typedtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.sodeac.common.expression.BooleanFunction;
import org.sodeac.common.expression.Variable;
import org.sodeac.common.misc.TransformedList;
import org.sodeac.common.typedtree.BranchNode.ModifyListenerContainer.ModifyListenerWrapper;
import org.sodeac.common.typedtree.IChildNodeListener.ILeafNodeListener;
import org.sodeac.common.typedtree.ModelPath.NodeSelector;
import org.sodeac.common.typedtree.ModelPath.NodeSelector.Axis;
import org.sodeac.common.typedtree.ModelPath.NodeSelector.NodeSelectorPredicate;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

/**
 * A branch node is an instance of complex tree node.
 *
 * @param <P> type of parent branch node
 * @param <T> type of branch node
 *
 * @author Sebastian Palarus
 */
public class BranchNode<P extends BranchNodeMetaModel, T extends BranchNodeMetaModel> extends Node<P, T>
{
    private INodeType<P, T> referenceNodeType = null;
    private BranchNodeMetaModel model = null;
    private List<NodeContainer> _nodeContainerList = null;
    private List<NodeContainer> nodeContainerList = null;
    protected RootBranchNode<?, ?> rootNode = null;
    protected BranchNode<?, P> parentNode = null;
    private long OID = -1;
    private int positionInList = -1;
    private volatile ModifyListenerRegistration<T> modifyListenerRegistration = null;
    private BranchNodeToObjectWrapper bow = null;
    private volatile Node.PayloadLevel payloadLevel = null;
    
    protected static final Function<BranchNode, BranchNodeToObjectWrapper> FnBowFromBranchNode = n -> n.getBow();
    
    /**
     * Constructor to create new branch node.
     *
     * @param rootNode                root node instance
     * @param parentNode              parent node instance
     * @param referencedNodeContainer static type instance defined in model
     */
    protected BranchNode(final RootBranchNode<?, ?> rootNode, final BranchNode<?, P> parentNode, final NodeContainer referencedNodeContainer)
    {
        INodeType<P, T> nodeType = referencedNodeContainer.getNodeType();
        this.referenceNodeType = nodeType;
        Class<T> modelType = nodeType.getTypeClass();
        try
        {
            if ((rootNode == null) && (parentNode == null))
            {
                super.rootLinked = true;    // self root
            }
            
            this.model = ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(modelType); // TODO from NodeContainer
            
            this.nodeContainerList = new ArrayList<>();
            for (int i = 0; i < this.model.getNodeTypeNames().length; i++)
            {
                INodeType childNodeType = this.model.getNodeTypeList().get(i);
                
                NodeContainer nodeContainer = new NodeContainer(childNodeType, i);
                if (childNodeType.getClass() == LeafNodeType.class)
                {
                    nodeContainer.node = new LeafNode<>(this, nodeContainer);
                    nodeContainer.node.setRootLinked(super.rootLinked);
                }
                else if (childNodeType.getClass() == BranchNodeListType.class)
                {
                    nodeContainer.nodeList = new ArrayList<BranchNode>();
                    nodeContainer.unmodifiableNodeList = Collections.unmodifiableList(nodeContainer.nodeList);
                }
                
                this.nodeContainerList.add(nodeContainer);
            }
            this._nodeContainerList = this.nodeContainerList;
            this.nodeContainerList = Collections.unmodifiableList(this.nodeContainerList);
            if (rootNode == null)
            {
                if (this instanceof RootBranchNode)
                {
                    this.rootNode = (RootBranchNode<?, ?>) this;
                    this.OID = 0L;
                }
                else
                {
                    throw new RuntimeException("missing root node");
                }
            }
            else
            {
                this.rootNode = rootNode;
                this.OID = rootNode.nextOID();
            }
            this.parentNode = parentNode;
        }
        catch (final Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
    
    protected BranchNodeMetaModel getModel()
    {
        return this.model;
    }
    
    protected Node.PayloadLevel getPayloadLevel()
    {
        return this.payloadLevel;
    }
    
    protected void setPayloadLevel(final Node.PayloadLevel payloadLevel)
    {
        this.payloadLevel = payloadLevel;
    }
    
    protected void setHasChilds() { }
    
    /**
     * Dispose this node and all child nodes.
     */
    @Override
    protected void disposeNode()
    {
        super.disposed = true;
        try
        {
            try
            {
                if (this.bow != null)
                {
                    this.bow.dispose();
                }
            }
            catch (final Exception e) { }
            
            List<NodeContainer> nodeContainerList = this._nodeContainerList;
            
            if (nodeContainerList != null)
            {
                for (final NodeContainer container : nodeContainerList)
                {
                    try
                    {
                        if (container.node != null)
                        {
                            // notify ??
                            container.node.disposeNode();
                        }
                        if (container.nodeList != null)
                        {
                            for (final BranchNode<?, ?> item : container.nodeList)
                            {
                                item.disposeNode();
                            }
                            container.nodeList.clear();
                        }
                        if (container.nodeListenerList != null)
                        {
                            for (final IChildNodeListener<?> childNodeListener : container.nodeListenerList)
                            {
                                if (childNodeListener.getClass() == ModifyListenerContainer.class)
                                {
                                    (((ModifyListenerContainer) childNodeListener)).dispose();
                                }
                            }
                            container.nodeListenerList.clear();
                        }
                        
                        if (container.meta != null)
                        {
                            container.meta.dispose();
                        }
                    }
                    finally
                    {
                        container.node = null;
                        container.nodeList = null;
                        container.unmodifiableNodeList = null;
                        container.nodeType = null;
                        container.listComparator = null;
                        container.unmodifiableNodeListSnapshot = null;
                        container.unmodifiableBowListSnapshot = null;
                        container.nodeListenerList = null;
                        container.meta = null;
                    }
                }
                nodeContainerList.clear();
            }
        }
        finally
        {
            
            try
            {
                if (this.modifyListenerRegistration != null)
                {
                    try
                    {
                        this.modifyListenerRegistration.dispose();
                    }
                    catch (final Exception e) { }
                    this.modifyListenerRegistration = null;
                }
            }
            finally
            {
                this.model = null;
                this.nodeContainerList = null;
                this._nodeContainerList = null;
                this.rootNode = null;
                this.parentNode = null;
                this.OID = -1;
                this.positionInList = -1;
                this.referenceNodeType = null;
                this.bow = null;
                this.setRootLinked(false);
            }
        }
    }
    
    @Override
    public INodeType<P, T> getNodeType()
    {
        return this.referenceNodeType;
    }
    
    @Override
    protected void setRootLinked(final boolean rootLinked)
    {
        if (super.rootLinked == rootLinked)
        {
            return;
        }
        super.rootLinked = rootLinked;
        List<NodeContainer> nodeContainerList = this.nodeContainerList;
        if (nodeContainerList != null)
        {
            for (final NodeContainer nodeContainer : nodeContainerList)
            {
                if (nodeContainer.node != null)
                {
                    nodeContainer.node.setRootLinked(rootLinked);
                }
                if (nodeContainer.nodeList != null)
                {
                    for (final BranchNode<?, ?> node : nodeContainer.nodeList)
                    {
                        node.setRootLinked(rootLinked);
                    }
                }
            }
        }
    }
    
    /**
     * Getter for root node.
     *
     * @return root node
     */
    public RootBranchNode<?, ?> getRootNode()
    {
        return this.rootNode;
    }
    
    /**
     * Getter for parent node.
     *
     * @return parent node
     */
    public BranchNode<?, P> getParentNode()
    {
        return this.parentNode;
    }
    
    /**
     * Getter for parent node.
     *
     * @param parentOfParent parent type of parent node
     *
     * @return parent node
     */
    public <X extends BranchNodeMetaModel> BranchNode<X, P> getParentNode(final Class<X> parentOfParent)
    {
        return (BranchNode) this.parentNode;
    }
    
    /**
     * returns all leaf node types of model
     *
     * @return all leaf node types of model
     */
    public List<LeafNodeType> getLeafNodeTypeList()
    {
        return this.model.getLeafNodeTypeList();
    }
    
    /**
     * returns all branch node types of model
     *
     * @return all branch node types of model
     */
    public List<BranchNodeType> getBranchNodeTypeList()
    {
        return this.model.getBranchNodeTypeList();
    }
    
    /**
     * returns all branch node list types of model
     *
     * @return all branch node list types of model
     */
    public List<BranchNodeListType> getBranchNodeListTypeList()
    {
        return this.model.getBranchNodeListTypeList();
    }
    
    /**
     * Applies this branch node to a consumer.
     *
     * @param consumer consumer to consume this branch node
     *
     * @return this branch node
     */
    public BranchNode<P, T> applyToConsumer(final Consumer<BranchNode<P, T>> consumer)
    {
        if (consumer == null)
        {
            return this;
        }
        consumer.accept(this);
        return this;
    }
    
    /**
     * Applies this branch node to a consumer locked by tree's read lock.
     *
     * @param consumer consumer to consume this branch node
     *
     * @return this branch node
     */
    public BranchNode<P, T> applyToConsumerWithReadLock(final Consumer<BranchNode<P, T>> consumer)
    {
        if (consumer == null)
        {
            return this;
        }
        Lock lock = this.rootNode.getReadLock();
        lock.lock();
        try
        {
            consumer.accept(this);
        }
        finally
        {
            lock.unlock();
        }
        return this;
    }
    
    /**
     * Applies this branch node to a consumer locked by tree's write lock.
     *
     * @param consumer consumer to consume this branch node
     *
     * @return this branch node
     */
    public BranchNode<P, T> applyToConsumerWithWriteLock(final Consumer<BranchNode<P, T>> consumer)
    {
        if (consumer == null)
        {
            return this;
        }
        Lock lock = this.rootNode.getWriteLock();
        lock.lock();
        try
        {
            consumer.accept(this);
        }
        finally
        {
            lock.unlock();
        }
        return this;
    }
    
    /*
     * LeafNode methods
     */
    
    /**
     * Getter for a child node of requested {@link LeafNodeType}.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return child node
     */
    public <X> LeafNode<T, X> get(final LeafNodeType<? super T, X> nodeType)
    {
        return (LeafNode<T, X>) getLeafNodeByIndex(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected LeafNode getLeafNodeByIndex(final int nodeTypeIndex, final LeafNodeType nodeType)
    {
        return (LeafNode) getNodeContainer(nodeTypeIndex, nodeType).node;
    }
    
    /**
     * Applies a child node of requested {@link LeafNodeType} to consumer.
     *
     * @param nodeType static child node type instance from meta model
     * @param consumer consumer to consume child node
     *
     * @return this branch node
     */
    public <X> BranchNode<P, T> applyToConsumer(final LeafNodeType<? super T, X> nodeType, final BiConsumer<BranchNode<P, ? super T>, LeafNode<? super T, X>> consumer)
    {
        applyToLeafNodeConsumer(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, consumer);
        return this;
    }
    
    protected void applyToLeafNodeConsumer(final int nodeTypeIndex, final LeafNodeType nodeType, final BiConsumer consumer)
    {
        NodeContainer nodeContainer = this.getNodeContainer(nodeTypeIndex, nodeType);
        LeafNode<T, ?> node = (LeafNode<T, ?>) nodeContainer.node;
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            consumer.accept(this, node);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Sets a value for the child node of requested {@link LeafNodeType}
     *
     * @param nodeType static child node type instance from meta model
     * @param value    value for child node
     *
     * @return this branch node
     */
    public <X> BranchNode<P, T> setValue(final LeafNodeType<? super T, X> nodeType, final X value)
    {
        setLeafNodeValue(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, value);
        return this;
    }
    
    protected void setLeafNodeValue(final int nodeTypeIndex, final LeafNodeType nodeType, final Object value)
    {
        NodeContainer nodeContainer = this.getNodeContainer(nodeTypeIndex, nodeType);
        LeafNode<T, Object> node = (LeafNode<T, Object>) nodeContainer.node;
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            node.setValue(value);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        nodeContainer = null;
        node = null;
    }
    
    /**
     * Gets a value of the child node of requested {@link LeafNodeType}
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return node value
     */
    public <X> X getValue(final LeafNodeType<? super T, X> nodeType)
    {
        return (X) getLeafNodeValue(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected Object getLeafNodeValue(final int nodeTypeIndex, final LeafNodeType nodeType)
    {
        NodeContainer nodeContainer = this.getNodeContainer(nodeTypeIndex, nodeType);
        LeafNode<T, Object> node = (LeafNode<T, Object>) nodeContainer.node;
        nodeContainer = null;
        return node.getValue();
    }
    
    /*
     *  BranchNode methods
     */
    
    protected BranchNode getBranchNodeByIndex(final int nodeTypeIndex, final BranchNodeType nodeType)
    {
        return (BranchNode) getNodeContainer(nodeTypeIndex, nodeType).node;
    }
    
    /**
     * Applies a child node of requested {@link BranchNodeType} to consumer.
     *
     * @param nodeType static child node type instance from meta model
     * @param consumer consumer
     *
     * @return this branch node
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> applyToConsumer(final BranchNodeType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<?, X>> consumer)
    {
        if (consumer == null)
        {
            return this;
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            int nodeTypeIndex = this.model.getNodeTypeIndexByClass().get(nodeType);
            NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
            BranchNode<T, X> node = (BranchNode<T, X>) nodeContainer.node;
            if (node == null)
            {
                if (this.rootNode.isBranchNodeApplyToConsumerAutoCreate())
                {
                    boolean created = false;
                    
                    node = new BranchNode(this.rootNode, this, nodeContainer);
                    
                    try
                    {
                        if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                        {
                            nodeContainer.node = node;
                            nodeContainer.node.setRootLinked(super.rootLinked);
                            created = true;
                            if (this.bow != null)
                            {
                                this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                            }
                            this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                        }
                    }
                    finally
                    {
                        if (!created)
                        {
                            node.disposeNode();
                            node = null;
                        }
                        else
                        {
                            setHasChilds();
                        }
                    }
                }
            }
            consumer.accept(this, node);
            return this;
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Applies a child node of requested {@link BranchNodeType} to consumer.
     *
     * @param nodeType  static child node type instance from meta model
     * @param ifAbsent  consumer to use if the child node does not already exist
     * @param ifPresent consumer to use if the child node already exists
     *
     * @return this branch node
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> applyToConsumer(final BranchNodeType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> ifAbsent, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> ifPresent)
    {
        int nodeTypeIndex = this.model.getNodeTypeIndexByClass().get(nodeType);
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            BranchNode<T, X> node = (BranchNode) nodeContainer.node;
            if (node == null)
            {
                if (this.rootNode.isBranchNodeApplyToConsumerAutoCreate() && (!this.rootNode.isImmutable()))
                {
                    boolean created = false;
                    node = new BranchNode(this.rootNode, this, nodeContainer);
                    try
                    {
                        if (ifAbsent != null)
                        {
                            ifAbsent.accept(this, node);
                        }
                        
                        if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                        {
                            nodeContainer.node = node;
                            nodeContainer.node.setRootLinked(super.rootLinked);
                            created = true;
                            if (this.bow != null)
                            {
                                this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                            }
                            this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                        }
                    }
                    finally
                    {
                        if (!created)
                        {
                            node.disposeNode();
                            node = null;
                        }
                        else
                        {
                            setHasChilds();
                        }
                    }
                }
                else
                {
                    if (ifAbsent != null)
                    {
                        ifAbsent.accept(this, node);
                    }
                }
                
                return this;
            }
            
            if (ifPresent != null)
            {
                ifPresent.accept(this, node);
            }
            
            return this;
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Removes a child node of requested {@link BranchNodeType}.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return this branch node
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> remove(final BranchNodeType<T, X> nodeType)
    {
        return this.remove(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<P, T> remove(final int nodeTypeIndex, final BranchNodeType<T, X> nodeType)
    {
        if (this.rootNode.isImmutable())
        {
            return this;
        }
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (nodeContainer.node != null)
            {
                Node oldNode = nodeContainer.node;
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, oldNode, null))
                {
                    try
                    {
                        this.rootNode.notifyAfterModify(this, nodeContainer, oldNode, null);
                    }
                    finally
                    {
                        nodeContainer.node.disposeNode();
                        nodeContainer.node = null;
                    }
                }
            }
            return this;
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Creates a new child node of requested {@link BranchNodeType}.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return new child node
     */
    public <X extends BranchNodeMetaModel> BranchNode<T, X> create(final BranchNodeType<T, X> nodeType)
    {
        return create(nodeType, null);
    }
    
    /**
     * Creates a new child node of requested {@link BranchNodeType}.
     *
     * @param nodeType static child node type instance from meta model
     * @param consumer builder to set up the child
     *
     * @return new child node
     */
    
    public <X extends BranchNodeMetaModel> BranchNode<T, X> create(final BranchNodeType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        return create(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, consumer);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<T, X> create(final int nodeTypeIndex, final BranchNodeType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        if (this.rootNode.isImmutable())
        {
            return null;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            boolean created = false;
            BranchNode<T, X> oldNode = (BranchNode<T, X>) nodeContainer.node;
            BranchNode<T, X> newNode = new BranchNode(this.rootNode, this, nodeContainer);
            
            try
            {
                if (consumer != null)
                {
                    consumer.accept(this, newNode);
                }
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, oldNode, newNode))
                {
                    try
                    {
                        nodeContainer.node = newNode;
                        nodeContainer.node.setRootLinked(super.rootLinked);
                        created = true;
                        if (this.bow != null)
                        {
                            if (oldNode != null)
                            {
                                oldNode.bow = null;
                            }
                            this.bow.createNestedBow(nodeTypeIndex, nodeType, newNode);
                        }
                        this.rootNode.notifyAfterModify(this, nodeContainer, oldNode, newNode);
                    }
                    finally
                    {
                        if (oldNode != null)
                        {
                            oldNode.disposeNode();
                        }
                    }
                    return newNode;
                }
            }
            finally
            {
                if (!created)
                {
                    newNode.disposeNode();
                }
                else
                {
                    setHasChilds();
                }
            }
            return null;
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Getter for a child node of requested {@link BranchNodeType}. If the child node does not exist and {@link RootBranchNode#setBranchNodeGetterAutoCreate(boolean)} was invoked with parameter true,
     * the child node will be created automatically.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return child node or null, if child node does not exist and auto-create-mode is off
     */
    
    public <X extends BranchNodeMetaModel> BranchNode<T, X> get(final BranchNodeType<T, X> nodeType)
    {
        return get(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<T, X> get(final int nodeTypeIndex, final BranchNodeType<T, X> nodeType)
    {
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        if (!this.rootNode.isBranchNodeGetterAutoCreate())
        {
            return (BranchNode<T, X>) nodeContainer.node;
        }
        
        BranchNode<T, X> node = (BranchNode<T, X>) nodeContainer.node;
        if (node != null)
        {
            return node;
        }
        
        // autocreate
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            node = (BranchNode<T, X>) nodeContainer.node;
            if (node != null)
            {
                return node;
            }
            boolean created = false;
            
            node = new BranchNode(this.rootNode, this, nodeContainer);
            
            try
            {
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                {
                    nodeContainer.node = node;
                    nodeContainer.node.setRootLinked(super.rootLinked);
                    created = true;
                    if (this.bow != null)
                    {
                        this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                    }
                    this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                }
            }
            finally
            {
                if (!created)
                {
                    node.disposeNode();
                }
                else
                {
                    setHasChilds();
                }
            }
            return node;
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /*
     * BranchNode List
     */
    
    /**
     * Getter for unmodifiable child node list with all child nodes of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return unmodifiable node list
     */
    public <X extends BranchNodeMetaModel> List<BranchNode<T, X>> getUnmodifiableNodeList(final BranchNodeListType<T, X> nodeType)
    {
        return this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).unmodifiableNodeList;
    }
    
    protected <X extends BranchNodeMetaModel> List<BranchNode<T, X>> getUnmodifiableNodeList(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType)
    {
        return this.nodeContainerList.get(nodeTypeIndex).unmodifiableNodeList;
    }
    
    protected List<BranchNodeToObjectWrapper> getUnmodifiableBowList(final int nodeTypeIndex, final BranchNodeListType nodeType)
    {
        return this.nodeContainerList.get(nodeTypeIndex).unmodifiableBowList;
    }
    
    /**
     * Getter for a snapshot of unmodifiable child node list with all child nodes of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model
     *
     * @return unmodifiable node list snapshot
     */
    public <X extends BranchNodeMetaModel> List<BranchNode<T, X>> getUnmodifiableNodeListSnapshot(final BranchNodeListType<T, X> nodeType)
    {
        return getUnmodifiableNodeListSnapshot(nodeType, null);
    }
    
    /**
     * Getter for a snapshot of unmodifiable child node list with filtered child nodes of requested {@link BranchNodeListType}.
     *
     * @param nodeType  static child node type instance from meta model
     * @param predicate filter for snapshot
     *
     * @return snapshot unmodifiable node list snapshot
     */
    
    public <X extends BranchNodeMetaModel> List<BranchNode<T, X>> getUnmodifiableNodeListSnapshot(final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate)
    {
        return getUnmodifiableNodeListSnapshot(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, predicate);
    }
    
    protected <X extends BranchNodeMetaModel> List<BranchNode<T, X>> getUnmodifiableNodeListSnapshot(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate)
    {
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        if (predicate == null)
        {
            List<BranchNode<T, X>> unmodifiableNodeListSnapshot = nodeContainer.unmodifiableNodeListSnapshot;
            if (unmodifiableNodeListSnapshot != null)
            {
                return unmodifiableNodeListSnapshot;
            }
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (predicate == null)
            {
                List<BranchNode<T, X>> unmodifiableNodeListSnapshot = nodeContainer.unmodifiableNodeListSnapshot;
                if (unmodifiableNodeListSnapshot != null)
                {
                    return unmodifiableNodeListSnapshot;
                }
                
                List<BranchNode<T, X>> snapshot = new ArrayList<BranchNode<T, X>>();
                nodeContainer.nodeList.forEach(n -> snapshot.add(n));
                nodeContainer.unmodifiableNodeListSnapshot = Collections.unmodifiableList(snapshot);
                if (this.bow != null)
                {
                    nodeContainer.unmodifiableBowListSnapshot = TransformedList.createView((List<BranchNode>) nodeContainer.unmodifiableNodeListSnapshot, FnBowFromBranchNode);
                }
                return snapshot;
            }
            
            List<BranchNode<T, X>> filteredList = new ArrayList<BranchNode<T, X>>();
            nodeContainer.nodeList.forEach(n -> {
                if (predicate.test(n))
                {
                    filteredList.add(n);
                }
            });
            return Collections.unmodifiableList(filteredList);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    protected List<BranchNodeToObjectWrapper> getUnmodifiableBowSnapshot(final int nodeTypeIndex, final BranchNodeListType nodeType, final Predicate<BranchNode> predicate)
    {
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        if (predicate == null)
        {
            List<BranchNodeToObjectWrapper> unmodifiableBowListSnapshot = nodeContainer.unmodifiableBowListSnapshot;
            if (unmodifiableBowListSnapshot != null)
            {
                return unmodifiableBowListSnapshot;
            }
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (predicate == null)
            {
                List<BranchNodeToObjectWrapper> unmodifiableBowListSnapshot = nodeContainer.unmodifiableBowListSnapshot;
                if (unmodifiableBowListSnapshot != null)
                {
                    return unmodifiableBowListSnapshot;
                }
                
                List<BranchNode> snapshot = new ArrayList<BranchNode>();
                nodeContainer.nodeList.forEach(n -> snapshot.add(n));
                nodeContainer.unmodifiableNodeListSnapshot = Collections.unmodifiableList(snapshot);
                if (this.bow != null)
                {
                    nodeContainer.unmodifiableBowListSnapshot = TransformedList.createView((List<BranchNode>) nodeContainer.unmodifiableNodeListSnapshot, FnBowFromBranchNode);
                }
                return nodeContainer.unmodifiableBowListSnapshot;
            }
            
            List<BranchNode> filteredList = new ArrayList<BranchNode>();
            nodeContainer.nodeList.forEach(n -> {
                if (predicate.test(n))
                {
                    filteredList.add(n);
                }
            });
            return TransformedList.createView(Collections.unmodifiableList(filteredList), FnBowFromBranchNode);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    /**
     * Sets a comparator to sort all child nodes for requested {@link BranchNodeListType}.
     *
     * @param nodeType   static child node type instance from meta model.
     * @param comparator comparator to apply
     *
     * @return this branch node
     *
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> setComperator(final BranchNodeListType<T, X> nodeType, final Comparator<BranchNode<T, X>> comparator)
    {
        NodeContainer nodeContainer = getNodeContainer(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if ((nodeContainer.listComparator == null) && (comparator == null))
            {
                return this;
            }
            
            nodeContainer.listComparator = comparator;
            
            if (this.rootNode.isImmutable())
            {
                return this;
            }
            
            Collections.sort(nodeContainer.nodeList, nodeContainer.listComparator);
            nodeContainer.unmodifiableNodeListSnapshot = null;
            nodeContainer.unmodifiableBowListSnapshot = null;
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
     * Get first matched child node of requested {@link BranchNodeListType}.
     *
     * @param nodeType  static child node type instance from meta model
     * @param predicate filter
     *
     * @return first matched node
     */
    public <X extends BranchNodeMetaModel> BranchNode<T, X> get(final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate)
    {
        return get(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, predicate);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<T, X> get(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate)
    {
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            for (final BranchNode<T, X> node : nodeContainer.nodeList)
            {
                if (predicate.test(node))
                {
                    return node;
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        return null;
    }
    
    /**
     * Creates new a child node of requested {@link BranchNodeListType}.
     *
     * @param nodeType type static child node type instance from meta model.
     *
     * @return new child node
     */
    public <X extends BranchNodeMetaModel> BranchNode<T, X> create(final BranchNodeListType<T, X> nodeType)
    {
        return create(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<T, X> create(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType)
    {
        if (this.rootNode.isImmutable())
        {
            return null;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            boolean created = false;
            BranchNode<T, X> node = new BranchNode(this.rootNode, this, nodeContainer);
            try
            {
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                {
                    nodeContainer.nodeList.add(node);
                    node.setRootLinked(super.rootLinked);
                    node.positionInList = nodeContainer.nodeList.size() - 1;
                    
                    nodeContainer.unmodifiableNodeListSnapshot = null;
                    nodeContainer.unmodifiableBowListSnapshot = null;
                    created = true;
                    if (this.bow != null)
                    {
                        this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                    }
                    this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                    
                    return node;
                }
            }
            finally
            {
                if (!created)
                {
                    node.disposeNode();
                }
                else
                {
                    setHasChilds();
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        return null;
    }
    
    /**
     * Creates new a child node of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model.
     * @param consumer setup new child node
     *
     * @return this branch node
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> create(final BranchNodeListType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        return create(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, consumer);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<P, T> create(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        if (this.rootNode.isImmutable())
        {
            return this;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            boolean created = false;
            BranchNode<T, X> node = new BranchNode(this.rootNode, this, nodeContainer);
            try
            {
                if (consumer != null)
                {
                    consumer.accept(this, node);
                }
                
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                {
                    if ((nodeContainer.listComparator == null) || nodeContainer.nodeList.isEmpty())
                    {
                        nodeContainer.nodeList.add(node);
                        node.setRootLinked(super.rootLinked);
                        node.positionInList = nodeContainer.nodeList.size() - 1;
                    }
                    else
                    {
                        if (nodeContainer.nodeList.isEmpty() || nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(nodeContainer.nodeList.size() - 1)) > 0)
                        {
                            nodeContainer.nodeList.add(node);
                            node.setRootLinked(super.rootLinked);
                            node.positionInList = nodeContainer.nodeList.size() - 1;
                        }
                        else if (nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(0)) < 0)
                        {
                            nodeContainer.nodeList.add(0, node);
                            node.setRootLinked(super.rootLinked);
                            
                            int index = 0;
                            for (final BranchNode nodeItem : nodeContainer.nodeList)
                            {
                                nodeItem.positionInList = index++;
                                this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                            }
                        }
                        else
                        {
                            int beginIndex = 0;
                            int rangeSize = nodeContainer.nodeList.size();
                            int endIndex = rangeSize - 1;
                            int testIndex = endIndex / 2;
                            int testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                            
                            while (rangeSize > 1)
                            {
                                
                                if (testResult < 0)
                                {
                                    if (endIndex == testIndex)
                                    {
                                        testIndex = beginIndex;
                                        rangeSize = endIndex - beginIndex;
                                        testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                                        break;
                                    }
                                    endIndex = testIndex;
                                }
                                else
                                {
                                    beginIndex = testIndex;
                                }
                                
                                testIndex = (beginIndex + endIndex) / 2;
                                rangeSize = endIndex - beginIndex;
                                testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                            }
                            
                            if (testResult < 0)
                            {
                                nodeContainer.nodeList.add(testIndex, node);
                                node.setRootLinked(super.rootLinked);
                                
                                for (int i = testIndex; i < nodeContainer.nodeList.size(); i++)
                                {
                                    BranchNode nodeItem = nodeContainer.nodeList.get(i);
                                    nodeItem.positionInList = i;
                                    this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                                }
                            }
                            else if (nodeContainer.nodeList.size() - 1 > testIndex)
                            {
                                nodeContainer.nodeList.add(testIndex + 1, node);
                                node.setRootLinked(super.rootLinked);
                                for (int i = testIndex + 1; i < nodeContainer.nodeList.size(); i++)
                                {
                                    BranchNode nodeItem = nodeContainer.nodeList.get(i);
                                    nodeItem.positionInList = i;
                                    this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                                }
                            }
                            else
                            {
                                nodeContainer.nodeList.add(node);
                                node.setRootLinked(super.rootLinked);
                                node.positionInList = nodeContainer.nodeList.size() - 1;
                            }
                        }
                    }
                    nodeContainer.unmodifiableNodeListSnapshot = null;
                    nodeContainer.unmodifiableBowListSnapshot = null;
                    created = true;
                    if (this.bow != null)
                    {
                        this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                    }
                    this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                }
            }
            finally
            {
                if (!created)
                {
                    node.disposeNode();
                }
                else
                {
                    setHasChilds();
                }
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
     * Creates a new child node of requested {@link BranchNodeListType}, if no item exists matched by <code>predicate</code>.
     *
     * @param nodeType  static child node type instance from meta model.
     * @param predicate predicate to test existing items
     * @param consumer  setup new child node
     *
     * @return this child node
     */
    public <X extends BranchNodeMetaModel> BranchNode<P, T> createIfAbsent(final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        return createIfAbsent(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, predicate, consumer);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<P, T> createIfAbsent(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final Predicate<BranchNode<T, X>> predicate, final BiConsumer<BranchNode<P, T>, BranchNode<T, X>> consumer)
    {
        if (this.rootNode.isImmutable())
        {
            return this;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            for (final BranchNode<T, X> node : nodeContainer.nodeList)
            {
                if (predicate.test(node))
                {
                    if (consumer != null)
                    {
                        consumer.accept(this, node);
                    }
                    return this;
                }
            }
            boolean created = false;
            BranchNode<T, X> node = new BranchNode(this.rootNode, this, nodeContainer);
            try
            {
                if (consumer != null)
                {
                    consumer.accept(this, node);
                }
                
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
                {
                    if ((nodeContainer.listComparator == null) || nodeContainer.nodeList.isEmpty())
                    {
                        nodeContainer.nodeList.add(node);
                        node.setRootLinked(super.rootLinked);
                        node.positionInList = nodeContainer.nodeList.size() - 1;
                    }
                    else
                    {
                        if (nodeContainer.nodeList.isEmpty() || nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(nodeContainer.nodeList.size() - 1)) > 0)
                        {
                            nodeContainer.nodeList.add(node);
                            node.setRootLinked(super.rootLinked);
                            node.positionInList = nodeContainer.nodeList.size() - 1;
                        }
                        else if (nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(0)) < 0)
                        {
                            nodeContainer.nodeList.add(0, node);
                            node.setRootLinked(super.rootLinked);
                            int index = 0;
                            for (final BranchNode nodeItem : nodeContainer.nodeList)
                            {
                                nodeItem.positionInList = index++;
                                this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                            }
                        }
                        else
                        {
                            int beginIndex = 0;
                            int rangeSize = nodeContainer.nodeList.size();
                            int endIndex = rangeSize - 1;
                            int testIndex = endIndex / 2;
                            int testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                            
                            while (rangeSize > 1)
                            {
                                
                                if (testResult < 0)
                                {
                                    if (endIndex == testIndex)
                                    {
                                        testIndex = beginIndex;
                                        rangeSize = endIndex - beginIndex;
                                        testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                                        break;
                                    }
                                    endIndex = testIndex;
                                }
                                else
                                {
                                    beginIndex = testIndex;
                                }
                                
                                testIndex = (beginIndex + endIndex) / 2;
                                rangeSize = endIndex - beginIndex;
                                testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
                            }
                            
                            if (testResult < 0)
                            {
                                nodeContainer.nodeList.add(testIndex, node);
                                node.setRootLinked(super.rootLinked);
                                for (int i = testIndex; i < nodeContainer.nodeList.size(); i++)
                                {
                                    BranchNode nodeItem = nodeContainer.nodeList.get(i);
                                    nodeItem.positionInList = i;
                                    this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                                }
                            }
                            else if (nodeContainer.nodeList.size() - 1 > testIndex)
                            {
                                nodeContainer.nodeList.add(testIndex + 1, node);
                                node.setRootLinked(super.rootLinked);
                                for (int i = testIndex + 1; i < nodeContainer.nodeList.size(); i++)
                                {
                                    BranchNode nodeItem = nodeContainer.nodeList.get(i);
                                    nodeItem.positionInList = i;
                                    this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                                }
                            }
                            else
                            {
                                nodeContainer.nodeList.add(node);
                                node.setRootLinked(super.rootLinked);
                                node.positionInList = nodeContainer.nodeList.size() - 1;
                            }
                        }
                    }
                    nodeContainer.unmodifiableNodeListSnapshot = null;
                    nodeContainer.unmodifiableBowListSnapshot = null;
                    created = true;
                    if (this.bow != null)
                    {
                        this.bow.createNestedBow(nodeTypeIndex, nodeType, node);
                    }
                    this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
                }
            }
            finally
            {
                if (!created)
                {
                    node.disposeNode();
                }
                else
                {
                    setHasChilds();
                }
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
     * Remove child node of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model
     * @param node     node instance to remove
     *
     * @return true, if node successfully removed, otherwise false
     */
    public <X extends BranchNodeMetaModel> boolean remove(final BranchNodeListType<T, X> nodeType, final BranchNode<T, X> node)
    {
        return remove(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, node);
    }
    
    protected <X extends BranchNodeMetaModel> boolean remove(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final BranchNode<T, X> node)
    {
        if (this.rootNode.isImmutable())
        {
            return false;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (nodeContainer.nodeList.contains(node))
            {
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
                {
                    nodeContainer.nodeList.remove(node);
                    node.setRootLinked(false);
                    int positionInList = node.positionInList;
                    try
                    {
                        nodeContainer.unmodifiableNodeListSnapshot = null;
                        nodeContainer.unmodifiableBowListSnapshot = null;
                        for (int i = positionInList; i < nodeContainer.nodeList.size(); i++)
                        {
                            BranchNode nodeItem = nodeContainer.nodeList.get(i);
                            nodeContainer.nodeList.get(i).positionInList = i;
                            this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                        }
                        this.rootNode.notifyAfterModify(this, nodeContainer, node, null);
                    }
                    finally
                    {
                        node.disposeNode();
                    }
                    return true;
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        return false;
    }
    
    /**
     * Remove all child nodes of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model
     *                 return this branch node
     */
    
    public <X extends BranchNodeMetaModel> BranchNode<P, T> clear(final BranchNodeListType<T, X> nodeType)
    {
        return clear(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType);
    }
    
    protected <X extends BranchNodeMetaModel> BranchNode<P, T> clear(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType)
    {
        if (this.rootNode.isImmutable())
        {
            return this;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (nodeContainer.nodeList == null)
            {
                return this;
            }
            
            if (nodeContainer.nodeList.isEmpty())
            {
                return this;
            }
            
            List<BranchNode<P, T>> copy = (List<BranchNode<P, T>>) new ArrayList(nodeContainer.nodeList);
            
            for (final BranchNode<P, T> node : copy)
            {
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
                {
                    nodeContainer.nodeList.remove(node);
                    node.setRootLinked(false);
                    try
                    {
                        this.rootNode.notifyAfterModify(this, nodeContainer, node, null);
                    }
                    finally
                    {
                        node.disposeNode();
                    }
                    
                }
            }
            
            nodeContainer.unmodifiableNodeListSnapshot = null;
            nodeContainer.unmodifiableBowListSnapshot = null;
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
     * Remove child node of requested {@link BranchNodeListType}.
     *
     * @param nodeType static child node type instance from meta model
     * @param index    position index of child node in list
     *
     * @return true, if node is successfully removed, otherwise false
     */
    public <X extends BranchNodeMetaModel> boolean remove(final BranchNodeListType<T, X> nodeType, final int index)
    {
        return remove(this.model.getNodeTypeIndexByClass().get(nodeType), nodeType, index);
    }
    
    protected <X extends BranchNodeMetaModel> boolean remove(final int nodeTypeIndex, final BranchNodeListType<T, X> nodeType, final int index)
    {
        if (this.rootNode.isImmutable())
        {
            return false;
        }
        
        NodeContainer nodeContainer = getNodeContainer(nodeTypeIndex, nodeType);
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            BranchNode<T, X> node = (BranchNode<T, X>) nodeContainer.nodeList.get(index);
            
            if (node != null)
            {
                if (this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
                {
                    nodeContainer.nodeList.remove(index);
                    node.setRootLinked(false);
                    int positionInList = node.positionInList;
                    try
                    {
                        nodeContainer.unmodifiableNodeListSnapshot = null;
                        nodeContainer.unmodifiableBowListSnapshot = null;
                        for (int i = positionInList; i < nodeContainer.nodeList.size(); i++)
                        {
                            BranchNode nodeItem = nodeContainer.nodeList.get(i);
                            nodeContainer.nodeList.get(i).positionInList = i;
                            this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
                        }
                        this.rootNode.notifyAfterModify(this, nodeContainer, node, null);
                    }
                    finally
                    {
                        node.disposeNode();
                    }
                    return true;
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
        return false;
    }
    
    /*
     * Child node listener
     */
    
    /**
     * Removes child node listener
     *
     * @param listener listener to remove
     *
     * @return this branch node
     */
    public BranchNode<P, T> removeChildNodeListener(final IChildNodeListener<T> listener)
    {
        if (listener == null)
        {
            return this;
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            
            for (final NodeContainer container : this.nodeContainerList)
            {
                if (container.nodeListenerList != null)
                {
                    container.nodeListenerList.remove(listener);
                }
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
    
    public <X> BranchNode<P, T> addChildNodeListener(final LeafNodeType<T, X> nodeType, final ILeafNodeListener<T, X> listener)
    {
        return addChildNodeListener(listener, nodeType);
    }
    
    /**
     * Add listener to get notified if child leaf nodes are updated , and child branch nodes are removed or created.
     *
     * @param listener          listener to register
     * @param childNodeTypeMask affected child node types
     *
     * @return this branch node
     */
    public BranchNode<P, T> addChildNodeListener(final IChildNodeListener<T> listener, final INodeType<T, ?>... childNodeTypeMask)
    {
        if (listener == null)
        {
            return this;
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            
            if ((childNodeTypeMask == null) || (childNodeTypeMask.length == 0))
            {
                for (final NodeContainer container : this.nodeContainerList)
                {
                    if (container.nodeListenerList == null)
                    {
                        container.nodeListenerList = new LinkedList<IChildNodeListener>();
                    }
                    else if (container.nodeListenerList.contains(listener))
                    {
                        continue;
                    }
                    container.nodeListenerList.add(listener);
                }
            }
            else
            {
                for (final INodeType<T, ?> filteredType : childNodeTypeMask)
                {
                    if (filteredType == null)
                    {
                        continue;
                    }
                    NodeContainer container = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(filteredType));
                    if (container == null)
                    {
                        continue;
                    }
                    
                    if (container.nodeListenerList == null)
                    {
                        container.nodeListenerList = new LinkedList<IChildNodeListener>();
                    }
                    else if (container.nodeListenerList.contains(listener))
                    {
                        continue;
                    }
                    container.nodeListenerList.add(listener);
                }
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
    
    /*
     * Path Modify Listener
     */
    
    public <X> void registerForModify(final ModelPath<T, X> path, final IModifyListener<X> listener)
    {
        Objects.requireNonNull(path, "Model path is null");
        Objects.requireNonNull(path.getNodeSelectorList(), "Model path is disposed");
        Objects.requireNonNull(path.getClazz(), "Model path is disposed");
        Objects.requireNonNull(listener, "Listener path is null");
        
        if (path.getNodeSelectorList().isEmpty())
        {
            throw new RuntimeException("path is empty");
        }
        
        NodeSelector<?, ?> rootNodeSelector = path.getNodeSelectorList().getFirst();
        Objects.requireNonNull(rootNodeSelector.getChildSelectorList(), "path contains root self only");
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (this.modifyListenerRegistration == null)
            {
                this.modifyListenerRegistration = new ModifyListenerRegistration<T>();
            }
            
            this.modifyListenerRegistration.registerListener(path, listener);
            
            for (final NodeSelector<T, ?> rootSelector : this.modifyListenerRegistration.getRootNodeSelectorList())
            {
                if (rootSelector.getChildSelectorList() == null)
                {
                    continue;
                }
                
                this.recursiveRegisterIModifyListener(rootSelector.getChildSelectorList(), rootSelector.getPredicate());
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    public <X> void unregisterForModify(final ModelPath<T, X> path)
    {
        if (path == null)
        {
            return;
        }
        
        Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
        if (lock != null)
        {
            lock.lock();
        }
        try
        {
            if (this.modifyListenerRegistration == null)
            {
                return;
            }
            
            this.modifyListenerRegistration.unregister(path);
            this.recursiveCleanIModifyListener();
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }
    
    private void recursiveRegisterIModifyListener(final Collection<NodeSelector<?, ?>> selectorList, final NodeSelectorPredicate rootPredicate)
    {
        if (selectorList == null)
        {
            return;
        }
        
        for (final NodeSelector<?, ?> selector : selectorList)
        {
            NodeContainer container = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(selector.getType()));
            
            // ensure  BranchNode.ModifyListenerContainer is registered as IChildNodeListener
            
            ModifyListenerContainer childNodeListener = null;
            if (container.nodeListenerList == null)
            {
                container.nodeListenerList = new ArrayList<IChildNodeListener>();
            }
            else
            {
                for (final IChildNodeListener child : container.nodeListenerList)
                {
                    if
                    (
                        (child instanceof BranchNode.ModifyListenerContainer) &&
                        (((BranchNode.ModifyListenerContainer) child).selector.equals(selector)) // compare axis, predicate and type
                    )
                    {
                        childNodeListener = (BranchNode.ModifyListenerContainer) child;
                        break;
                    }
                }
            }
            
            if (childNodeListener == null)
            {
                childNodeListener = new BranchNode.ModifyListenerContainer();
                childNodeListener.selector = selector.clone(null, false); // copy for equals match
                childNodeListener.selector.setRootType(null);
                childNodeListener.container = container;
                
                container.nodeListenerList.add(childNodeListener);
            }
            
            // ensure ModifyListenerWrapper is registered (same selector and equal rootPredicate)
            
            ModifyListenerWrapper listenerWrapper = null;
            for (final ModifyListenerWrapper child : childNodeListener.listenerWrapperList)
            {
                if ((child.selector == selector) && (child.equalsRootPredicate(rootPredicate)))
                {
                    listenerWrapper = child;
                    break;
                }
            }
            
            if (listenerWrapper == null)
            {
                listenerWrapper = childNodeListener.new ModifyListenerWrapper();
                listenerWrapper.rootPathPredicate = rootPredicate;
                listenerWrapper.selector = selector;
                childNodeListener.listenerWrapperList.add(listenerWrapper);
            }
            
            childNodeListener.mergeActiveModifyListener();
            
            // for branch nodes and branch node lists : register recursive, if child and selector has child selectors
            
            if ((selector.getAxis() == Axis.CHILD) && (!(selector.getType() instanceof LeafNodeType)))
            {
                if ((listenerWrapper.selector.getChildSelectorList() != null) && (!listenerWrapper.selector.getChildSelectorList().isEmpty()))
                {
                    if (container.getNode() instanceof BranchNode) // branch node
                    {
                        ((BranchNode) container.getNode()).recursiveRegisterIModifyListener(listenerWrapper.selector.getChildSelectorList(), null);
                    }
                    if (container.getNodeList() != null)
                    {
                        for (final BranchNode node : container.getNodeList()) // branch node list
                        {
                            node.recursiveRegisterIModifyListener(listenerWrapper.selector.getChildSelectorList(), null);
                        }
                    }
                }
            }
        }
    }
    
    private void recursiveCleanIModifyListener()
    {
        for (final NodeContainer container : this.nodeContainerList)
        {
            if (container.nodeListenerList != null)
            {
                if (container.getNode() instanceof BranchNode) // branch node
                {
                    ((BranchNode) container.getNode()).recursiveCleanIModifyListener();
                }
                if (container.getNodeList() != null)
                {
                    for (final BranchNode node : container.getNodeList()) // branch node list
                    {
                        node.recursiveCleanIModifyListener();
                    }
                }
                
                int listenerIndex = 0;
                LinkedList<Integer> listenerToRemove = null;
                for (final IChildNodeListener child : container.nodeListenerList)
                {
                    if (child instanceof final BranchNode.ModifyListenerContainer childNodeListener)
                    {
                        
                        int wrapperIndex = 0;
                        LinkedList<Integer> wrapperToRemove = null;
                        for (final ModifyListenerWrapper wrapper : (List<ModifyListenerWrapper>) childNodeListener.listenerWrapperList)
                        {
                            if ((wrapper.selector == null) || wrapper.selector.isDisposed())
                            {
                                if (wrapperToRemove == null)
                                {
                                    wrapperToRemove = new LinkedList<>();
                                }
                                wrapperToRemove.addFirst(wrapperIndex);
                            }
                            wrapperIndex++;
                        }
                        
                        if (wrapperToRemove != null)
                        {
                            for (final int index : wrapperToRemove)
                            {
                                childNodeListener.listenerWrapperList.remove(index);
                            }
                            
                            wrapperToRemove.clear();
                        }
                        
                        if (childNodeListener.listenerWrapperList.isEmpty())
                        {
                            if (listenerToRemove == null)
                            {
                                listenerToRemove = new LinkedList<>();
                            }
                            listenerToRemove.addFirst(listenerIndex);
                        }
                    }
                    
                    listenerIndex++;
                }
                
                if (listenerToRemove != null)
                {
                    for (final int index : listenerToRemove)
                    {
                        BranchNode.ModifyListenerContainer childNodeListener = (BranchNode.ModifyListenerContainer) container.nodeListenerList.remove(index);
                        if (childNodeListener != null)
                        {
                            childNodeListener.dispose();
                        }
                    }
                    
                    listenerToRemove.clear();
                }
            }
        }
    }
    
    protected class ModifyListenerContainer implements IChildNodeListener<T>
    {
        private NodeSelector selector = null;
        private final boolean active = true;
        private List<ModifyListenerWrapper> listenerWrapperList = new ArrayList<ModifyListenerWrapper>();
        private Set<IModifyListener<?>> activeSet = null;
        private NodeContainer container = null;
        private BooleanFunction predicateEvaluator = null;
        
        @Override
        public void accept(final Node<T, ?> node, final Object oldValue)
        {
            if (this.active)
            {
                Set<IModifyListener<?>> activeSet = this.activeSet;
                
                if ((activeSet != null) && (!activeSet.isEmpty()))
                {
                    for (final IModifyListener modifyListener : activeSet)
                    {
                        if (!modifyListener.isEnabled())
                        {
                            continue;
                        }
                        if (this.selector.getAxis() == Axis.VALUE)
                        {
                            modifyListener.accept(((LeafNode) node).getValue(), oldValue);
                        }
                        else if (this.selector.getAxis() == Axis.CHILD)
                        {
                            if (this.selector.getType() instanceof LeafNodeType)
                            {
                                modifyListener.accept(node, oldValue);
                            }
                            else if (this.selector.getType() instanceof BranchNodeType)
                            {
                                modifyListener.accept(node, oldValue);
                            }
                            else if (this.selector.getType() instanceof BranchNodeListType)
                            {
                                modifyListener.accept(node, oldValue);
                            }
                        }
                    }
                }
            }
            
            for (final ModifyListenerWrapper listenerWrapper : this.listenerWrapperList)
            {
                if ((listenerWrapper.selector == null) || listenerWrapper.selector.isDisposed())
                {
                    continue;
                }
                
                if ((this.selector.getAxis() == Axis.CHILD) && (!(this.selector.getType() instanceof LeafNodeType)))
                {
                    if ((listenerWrapper.selector.getChildSelectorList() != null) && (!listenerWrapper.selector.getChildSelectorList().isEmpty()))
                    {
						/*if(oldValue instanceof BranchNode)
						{
							BranchNode<T,?> newBranchNode = (BranchNode<T,?>) oldValue;
							newBranchNode.unregisterIModifyListener(listenerWrapper.selector.getChildSelectorList(), null);							
						}*/
                        if (node instanceof BranchNode)
                        {
                            BranchNode<T, ?> newBranchNode = (BranchNode<T, ?>) node;
                            newBranchNode.recursiveRegisterIModifyListener(listenerWrapper.selector.getChildSelectorList(), null);
                        }
                    }
                }
            }
        }
        
        protected class ModifyListenerWrapper
        {
            private NodeSelectorPredicate rootPathPredicate = null;
            private NodeSelector selector = null;
            private boolean activeByParent = true;
            
            private boolean equalsRootPredicate(final NodeSelectorPredicate rootPathPredicate)
            {
                if (rootPathPredicate == null)
                {
                    return this.rootPathPredicate == null;
                }
                return rootPathPredicate.equals(this.rootPathPredicate);
            }
        }
        
        protected class LeafNodePredicateListener extends Variable<Boolean> implements IModifyListener<LeafNode<T, ?>>
        {
            
            public LeafNodePredicateListener()
            {
                super(Boolean.FALSE);
            }
            
            @Override
            public void accept(final LeafNode<T, ?> newValue, final LeafNode<T, ?> oldValue)
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onListenStart(final LeafNode<T, ?> value)
            {
                // TODO Auto-generated method stub
                IModifyListener.super.onListenStart(value);
            }
            
            @Override
            public void onListenStop(final LeafNode<T, ?> value)
            {
                // TODO Auto-generated method stub
                IModifyListener.super.onListenStop(value);
            }
            
            @Override
            public void dispose()
            {
                // TODO Auto-generated method stub
                super.dispose();
            }
            
        }
        
        protected void mergeActiveModifyListener()
        {
            List<IModifyListener<?>> listenStart = null;
            List<IModifyListener<?>> listenStop = null;
            Set<IModifyListener<?>> newActiveSet = null;
            Set<IModifyListener<?>> thisActiveSet = this.activeSet;
            
            if (this.active)
            {
                newActiveSet = new HashSet<>();
                
                for (final ModifyListenerWrapper listenerWrapper : this.listenerWrapperList)
                {
                    if ((listenerWrapper.selector == null) || listenerWrapper.selector.isDisposed())
                    {
                        continue;
                    }
                    if (!listenerWrapper.activeByParent)
                    {
                        continue;
                    }
                    if (listenerWrapper.selector.getModifyListenerList() != null)
                    {
                        for (final IModifyListener modifyListener : (Set<IModifyListener>) listenerWrapper.selector.getModifyListenerList())
                        {
                            newActiveSet.add(modifyListener);
                        }
                    }
                }
            }
            
            if ((thisActiveSet != null) && (!thisActiveSet.isEmpty()))
            {
                for (final IModifyListener<?> modifyLister : thisActiveSet)
                {
                    if ((newActiveSet == null) || (!newActiveSet.contains(modifyLister)))
                    {
                        if (listenStop == null)
                        {
                            listenStop = new ArrayList<>();
                        }
                        listenStop.add(modifyLister);
                    }
                }
            }
            
            if ((newActiveSet != null) && (!newActiveSet.isEmpty()))
            {
                for (final IModifyListener<?> modifyLister : newActiveSet)
                {
                    if ((thisActiveSet == null) || (!thisActiveSet.contains(modifyLister)))
                    {
                        if (listenStart == null)
                        {
                            listenStart = new ArrayList<>();
                        }
                        listenStart.add(modifyLister);
                    }
                }
            }
            
            if ((newActiveSet != null) && (!newActiveSet.isEmpty()))
            {
                this.activeSet = newActiveSet;
            }
            else
            {
                this.activeSet = null;
            }
            
            if (thisActiveSet != null)
            {
                thisActiveSet.clear();
                thisActiveSet = null;
            }
            
            if (listenStop != null)
            {
                if (this.selector.getAxis() == Axis.VALUE)
                {
                    LeafNode node = (LeafNode) this.container.node;
                    for (final IModifyListener modifyListener : listenStop)
                    {
                        modifyListener.onListenStop(node.getValue());
                    }
                }
                else if (this.selector.getAxis() == Axis.CHILD)
                {
                    for (final IModifyListener modifyListener : listenStart)
                    {
                        if (this.container.getNode() != null)
                        {
                            modifyListener.onListenStop(this.container.getNode());
                        }
                        if (this.container.getNodeList() != null)
                        {
                            for (final BranchNode node : this.container.getNodeList())
                            {
                                modifyListener.onListenStop(node);
                            }
                        }
                    }
                }
            }
            
            if (listenStart != null)
            {
                if (this.selector.getAxis() == Axis.VALUE)
                {
                    LeafNode node = (LeafNode) this.container.node;
                    for (final IModifyListener modifyListener : listenStart)
                    {
                        modifyListener.onListenStart(node.getValue());
                    }
                }
                else if (this.selector.getAxis() == Axis.CHILD)
                {
                    for (final IModifyListener modifyListener : listenStart)
                    {
                        if (this.container.getNode() != null)
                        {
                            modifyListener.onListenStart(this.container.getNode());
                        }
                        if (this.container.getNodeList() != null)
                        {
                            for (final BranchNode node : this.container.getNodeList())
                            {
                                modifyListener.onListenStart(node);
                            }
                        }
                    }
                }
            }
        }
        
        protected void dispose()
        {
            if (this.activeSet != null)
            {
                if (this.selector.getAxis() == Axis.VALUE)
                {
                    LeafNode node = (LeafNode) this.container.node;
                    for (final IModifyListener modifyLister : this.activeSet)
                    {
                        modifyLister.onListenStop(node.getValue());
                    }
                }
                else if (this.selector.getAxis() == Axis.CHILD)
                {
                    for (final IModifyListener modifyListener : this.activeSet)
                    {
                        if (this.container.getNode() != null)
                        {
                            modifyListener.onListenStop(this.container.getNode());
                        }
                        if (this.container.getNodeList() != null)
                        {
                            for (final BranchNode node : this.container.getNodeList())
                            {
                                modifyListener.onListenStop(node);
                            }
                        }
                    }
                }
                this.activeSet.clear();
                this.activeSet = null;
            }
            
            if (this.listenerWrapperList == null)
            {
                for (final ModifyListenerWrapper modifyListenerWrapper : this.listenerWrapperList)
                {
                    try
                    {
                        modifyListenerWrapper.rootPathPredicate = null;
                        modifyListenerWrapper.selector = null;
                        modifyListenerWrapper.activeByParent = true;
                    }
                    catch (final Exception e) { }
                }
            }
            try
            {
                this.listenerWrapperList.clear();
            }
            catch (final Exception e) { }
            
            if (this.selector != null)
            {
                this.selector.dispose();
            }
            this.selector = null;
            this.listenerWrapperList = null;
            
            if (this.predicateEvaluator != null)
            {
                this.predicateEvaluator.dispose();
                this.predicateEvaluator = null;
            }
            
            this.container = null;
        }
    }
    
    public static class NodeMeta
    {
        private volatile Boolean defined = null;
        private volatile Node.PayloadLevel requiredPayloadLevel = null;
        
        public Boolean isDefined()
        {
            return this.defined;
        }
        
        public NodeMeta setDefined(final boolean defined)
        {
            this.defined = defined ? Boolean.TRUE : Boolean.FALSE;
            return this;
        }
        
        protected Node.PayloadLevel getRequiredPayloadLevel()
        {
            return this.requiredPayloadLevel;
        }
        
        protected void setRequiredPayloadLevel(final Node.PayloadLevel requiredPayloadLevel)
        {
            this.requiredPayloadLevel = requiredPayloadLevel;
        }
        
        public void dispose()
        {
            this.defined = null;
            this.requiredPayloadLevel = null;
        }
    }
    
    protected static class NodeContainer
    {
        protected NodeContainer(final INodeType nodeType, final int index)
        {
            super();
            this.nodeType = nodeType;
            this.index = index;
        }
        
        private int index = -1;
        private INodeType nodeType = null;
        private volatile Node node = null;
        private ArrayList<BranchNode> nodeList = null;
        private List unmodifiableNodeList = null;
        private List unmodifiableBowList = null;
        private volatile Comparator listComparator = null;
        private volatile List unmodifiableNodeListSnapshot = null;
        private volatile List unmodifiableBowListSnapshot = null;
        private volatile List<IChildNodeListener> nodeListenerList = null;
        private volatile NodeMeta meta = null;
        
        protected INodeType getNodeType()
        {
            return this.nodeType;
        }
        
        protected void setNodeType(final INodeType nodeType)
        {
            this.nodeType = nodeType;
        }
        
        protected int getIndex()
        {
            return this.index;
        }
        
        protected void setIndex(final int index)
        {
            this.index = index;
        }
        
        protected Node getNode()
        {
            return this.node;
        }
        
        protected void setNode(final Node node)
        {
            this.node = node;
        }
        
        protected ArrayList<BranchNode> getNodeList()
        {
            return this.nodeList;
        }
        
        protected void setNodeList(final ArrayList<BranchNode> nodeList)
        {
            this.nodeList = nodeList;
        }
        
        protected List getUnmodifiableNodeList()
        {
            return this.unmodifiableNodeList;
        }
        
        /*protected void setUnmodifiableNodeList(List unmodifiableNodeList)
        {
            this.unmodifiableNodeList = unmodifiableNodeList;
        }*/
        protected Comparator getListComparator()
        {
            return this.listComparator;
        }
        
        protected void setListComparator(final Comparator listComparator)
        {
            this.listComparator = listComparator;
        }
        
        protected List getUnmodifiableNodeListSnapshot()
        {
            return this.unmodifiableNodeListSnapshot;
        }
        
        /*protected void setUnmodifiableNodeListSnapshot(List unmodifiableNodeListSnapshot)
        {
            this.unmodifiableNodeListSnapshot = unmodifiableNodeListSnapshot;
        }*/
        protected List<IChildNodeListener> getNodeListenerList()
        {
            return this.nodeListenerList;
        }
        
        protected void setNodeListenerList(final List<IChildNodeListener> nodeListenerList)
        {
            this.nodeListenerList = nodeListenerList;
        }
        
    }
    
    public BranchNode<P, T> copyFrom(final BranchNode<? extends BranchNodeMetaModel, ? extends T> copyFrom)
    {
        if (copyFrom == this)
        {
            return this;
        }
        if (copyFrom == null)
        {
            return null;
        }
        for (final NodeContainer nodeContainer : this._nodeContainerList)
        {
            if (nodeContainer.nodeType instanceof LeafNodeType)
            {
                this.setValue((LeafNodeType) nodeContainer.nodeType, copyFrom.getValue((LeafNodeType) nodeContainer.nodeType));
            }
            else if (nodeContainer.nodeType instanceof BranchNodeType)
            {
                BranchNode copyChild = copyFrom.get((BranchNodeType) nodeContainer.nodeType);
                if ((copyChild == null) && (get((BranchNodeType) nodeContainer.nodeType) != null))
                {
                    remove((BranchNodeType) nodeContainer.nodeType);
                }
                else if ((copyChild != null))
                {
                    this.create((BranchNodeType) nodeContainer.nodeType).copyFrom(copyChild);
                }
            }
            else if (nodeContainer.nodeType instanceof BranchNodeListType)
            {
                this.clear((BranchNodeListType) nodeContainer.nodeType);
                
                for (final BranchNode copyChild : (List<BranchNode>) copyFrom.getUnmodifiableNodeList((BranchNodeListType) nodeContainer.nodeType))
                {
                    this.create((BranchNodeListType) nodeContainer.nodeType).copyFrom(copyChild);
                }
            }
        }
        return this;
    }
    
    public BranchNodeToObjectWrapper getBow()
    {
        return this.bow;
    }
    
    protected void setBow(final BranchNodeToObjectWrapper bow)
    {
        this.bow = bow;
        for (final NodeContainer nodeContainer : this.nodeContainerList)
        {
            if (nodeContainer.unmodifiableNodeList != null)
            {
                nodeContainer.unmodifiableBowList = TransformedList.createView(((List<BranchNode>) nodeContainer.unmodifiableNodeList), FnBowFromBranchNode);
            }
        }
    }
    
    public RootBranchNode<?, T> unwrapRootBranchNode()
    {
        return (RootBranchNode) this;
    }
    
    protected NodeContainer getNodeContainer(final int nodeTypeIndex, final INodeType nodeType)
    {
        NodeContainer nodeContainer = this.nodeContainerList.get(nodeTypeIndex);
        if (nodeContainer.nodeType != nodeType)
        {
            INodeType origin = (INodeType) this.model.getNodeTypeIndexByHidden().get(nodeType);
            if (origin != nodeContainer.nodeType)
            {
                throw new IllegalStateException("Illegale index for nodeContainer. Index: " + nodeTypeIndex + " accepted type: " + nodeType + " addressed type by index: " + nodeContainer.nodeType);
            }
        }
        return nodeContainer;
    }
}
