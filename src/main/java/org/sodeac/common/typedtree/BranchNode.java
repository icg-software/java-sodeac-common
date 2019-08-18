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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.IChildNodeListener.ILeafNodeListener;
import org.sodeac.common.typedtree.ModelPath.NodeSelector;
import org.sodeac.common.typedtree.ModelPath.NodeSelector.Axis;

/**
 * A branch node is an instance of complex tree node.
 * 
 * @author Sebastian Palarus
 *
 * @param <P> type of parent branch node
 * @param <T> type of branch node
 */
public class BranchNode<P extends BranchNodeMetaModel, T extends BranchNodeMetaModel> extends Node<P,T>
{
	private INodeType<P,T> nodeType = null;
	private BranchNodeMetaModel model = null;
	private List<NodeContainer> _nodeContainerList = null;
	private List<NodeContainer> nodeContainerList = null;
	protected RootBranchNode<?,?> rootNode = null;
	protected BranchNode<?,P> parentNode = null;
	private long OID = -1;
	private int positionInList = -1;
	private volatile List<ModifyListenerRegistration<?>> modifyListenerRegistrationList = null;
	
	/**
	 * Constructor to create new branch node.
	 * 
	 * @param rootNode root node instance
	 * @param parentNode parent node instance
	 * @param referencedNodeContainer static type instance defined in model
	 */
	protected BranchNode(RootBranchNode<?,?> rootNode, BranchNode<?,P> parentNode, NodeContainer referencedNodeContainer)
	{
		INodeType<P,T> nodeType = referencedNodeContainer.getNodeType();
		this.nodeType = nodeType;
		Class<T> modelType = nodeType.getTypeClass();
		try
		{
			if((rootNode == null) && (parentNode == null))
			{
				super.rootLinked = true;	// self root
			}
			
			this.model = ModelingProcessor.DEFAULT_INSTANCE.getModel(modelType);
			
			this.nodeContainerList = new ArrayList<>();
			for(int i = 0; i < this.model.getNodeTypeNames().length; i++)
			{
				INodeType childNodeType = model.getNodeTypeList().get(i);
				
				NodeContainer nodeContainer = new NodeContainer();
				nodeContainer.nodeType = childNodeType;
				if(childNodeType.getClass() == LeafNodeType.class)
				{
					nodeContainer.node = new LeafNode<>(this,nodeContainer);
					nodeContainer.node.setRootLinked(super.rootLinked);
				}
				else if (childNodeType.getClass() == BranchNodeListType.class)
				{
					nodeContainer.nodeList = new ArrayList<BranchNode>();
					nodeContainer.unmodifiableNodeList = Collections.unmodifiableList(nodeContainer.nodeList);
				}
				
				nodeContainerList.add(nodeContainer);
			}
			this._nodeContainerList = this.nodeContainerList;
			this.nodeContainerList = Collections.unmodifiableList(this.nodeContainerList);
			if(rootNode == null)
			{
				if(this instanceof RootBranchNode)
				{
					this.rootNode = (RootBranchNode<?,?>)this;
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
		catch (Exception e) 
		{
			if(e instanceof RuntimeException)
			{
				throw (RuntimeException)e;
			}
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Dispose this node and all child nodes.
	 */
	protected void disposeNode()
	{
		try
		{
			if(modifyListenerRegistrationList != null)
			{
				for(ModifyListenerRegistration<?> registration : modifyListenerRegistrationList)
				{
					registration.unregister();
				}
				
				modifyListenerRegistrationList.clear();
			}
		}
		finally 
		{
			
			try
			{
				List<NodeContainer> nodeContainerList = this._nodeContainerList;
				
				if(nodeContainerList != null)
				{
					for(NodeContainer container : nodeContainerList)
					{
						try
						{
							if(container.node != null)
							{
								container.node.disposeNode();
							}
							if(container.nodeList != null)
							{
								for(BranchNode<?,?> item : container.nodeList)
								{
									item.disposeNode();
								}
								container.nodeList.clear();
							}
							if(container.nodeListenerList != null)
							{
								container.nodeListenerList.clear();
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
							container.nodeListenerList = null;
						}
					}
					nodeContainerList.clear();
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
				this.nodeType = null;
				this.setRootLinked(false);
			}
		}
	}
	
	@Override
	public INodeType<P, T> getNodeType()
	{
		return this.nodeType;
	}

	@Override
	protected void setRootLinked(boolean rootLinked)
	{
		if(super.rootLinked == rootLinked)
		{
			return;
		}
		super.rootLinked = rootLinked;
		List<NodeContainer> nodeContainerList = this.nodeContainerList;
		if(nodeContainerList != null)
		{
			for(NodeContainer nodeContainer : nodeContainerList)
			{
				if(nodeContainer.node != null)
				{
					nodeContainer.node.setRootLinked(rootLinked);
				}
				if(nodeContainer.nodeList != null)
				{
					for(BranchNode<?,?> node : nodeContainer.nodeList)
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
		return rootNode;
	}

	/**
	 * Getter for parent node.
	 * 
	 * @return parent node
	 */
	public BranchNode<?, P> getParentNode()
	{
		return parentNode;
	}

	/**
	 * Applies this branch node to a consumer.
	 * 
	 * @param consumer consumer to consume this branch node
	 * @return this branch node
	 */
	public BranchNode<P, T> applyToConsumer(Consumer<BranchNode<P, T>> consumer)
	{
		if(consumer == null)
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
	 * @return this branch node
	 */
	public BranchNode<P, T> applyToConsumerWithReadLock(Consumer<BranchNode<P, T>> consumer)
	{
		if(consumer == null)
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
	 * @return this branch node
	 */
	public BranchNode<P, T> applyToConsumerWithWriteLock(Consumer<BranchNode<P, T>> consumer)
	{
		if(consumer == null)
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
	 * @return child node
	 */
	public <X> LeafNode<T,X> get(LeafNodeType<? super T,X> nodeType)
	{
		return (LeafNode<T,X>) this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).node;
	}
	
	/**
	 * Applies a child node of requested {@link LeafNodeType} to consumer.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param consumer consumer to consume child node
	 * @return this branch node
	 */
	public <X> BranchNode<P,T> applyToConsumer(LeafNodeType<? super T,X> nodeType, BiConsumer<BranchNode<P, ? super T>, LeafNode<? super T,X>> consumer)
	{
		LeafNode<T,X> node = (LeafNode<T,X>) this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).node;
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			consumer.accept(this, node);
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		return this;
	}
	
	/**
	 * Sets a value for the child node of requested {@link LeafNodeType}
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param value value for child node
	 * @return this branch node
	 */
	public <X> BranchNode<P,T> setValue(LeafNodeType<? super T,X> nodeType, X value)
	{
		LeafNode<T,X> node = (LeafNode<T,X>) this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).node;
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			node.setValue(value);
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		return this;
	}
	
	/**
	 * Gets a value of the child node of requested {@link LeafNodeType}
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @return node value
	 */
	public <X> X getValue(LeafNodeType<? super T,X> nodeType)
	{
		return ((LeafNode<T,X>) this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).node).getValue();
	}
	
	/*
	 *  BranchNode methods
	 */
	
	/**
	 * Applies a child node of requested {@link BranchNodeType} to consumer.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param consumer consumer
	 * @return this branch node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> applyToConsumer(BranchNodeType<T,X> nodeType, BiConsumer<BranchNode<P, T>, BranchNode<?,X>> consumer)
	{
		if(consumer == null)
		{
			return this;
		}
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
			BranchNode<T,X> node = (BranchNode<T,X>)nodeContainer.node;
			if(node == null)
			{
				if(this.rootNode.isBranchNodeApplyToConsumerAutoCreate())
				{
					boolean created = false;
					
					node = new BranchNode(this.rootNode,this,nodeContainer);
					
					try
					{
						if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
						{
							nodeContainer.node = node;
							nodeContainer.node.setRootLinked(super.rootLinked);
							created = true;
							this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
						}
					}
					finally 
					{
						if(! created)
						{
							node.disposeNode();
							node = null;
						}
					}
				}
			}
			consumer.accept(this, node);
			return this;
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
	}
	
	/**
	 * Applies a child node of requested {@link BranchNodeType} to consumer.
	 *  
	 * @param nodeType static child node type instance from meta model
	 * @param ifAbsent consumer to use if the child node does not already exist
	 * @param ifPresent consumer to use if the child node already exists
	 * @return this branch node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> applyToConsumer(BranchNodeType<T,X> nodeType,BiConsumer<BranchNode<P, T>, BranchNode<T,X>> ifAbsent,BiConsumer<BranchNode<P, T>, BranchNode<T,X>> ifPresent)
	{
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			BranchNode<T,X> node = (BranchNode)nodeContainer.node;
			if(node == null)
			{
				if(rootNode.isBranchNodeApplyToConsumerAutoCreate() && (! rootNode.isImmutable()))
				{
					boolean created = false;
					node = new BranchNode(this.rootNode,this,nodeContainer);
					try
					{
						if(ifAbsent != null)
						{
							ifAbsent.accept(this, node);
						}
						
						if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
						{
							nodeContainer.node = node;
							nodeContainer.node.setRootLinked(super.rootLinked);
							created = true;
							this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
						}
					}
					finally 
					{
						if(! created)
						{
							node.disposeNode();
							node = null;
						}
					}
				}
				else
				{
					if(ifAbsent != null)
					{
						ifAbsent.accept(this, node);
					}
				}
				
				return this;
			}
			
			if(ifPresent != null)
			{
				ifPresent.accept(this, node);
			}
			
			return this;
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
	}
	
	/**
	 * Removes a child node of requested {@link BranchNodeType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @return this branch node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> remove(BranchNodeType<T,X> nodeType)
	{
		if(rootNode.isImmutable())
		{
			return this;
		}
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			if(nodeContainer.node != null)
			{
				Node oldNode = nodeContainer.node;
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, oldNode, null))
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
			if(lock != null)
			{
				lock.unlock();
			}
		}
	}
	
	/**
	 * Creates a new child node of requested {@link BranchNodeType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @return new child node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<T,X> create(BranchNodeType<T,X> nodeType)
	{
		return create(nodeType, null);
	}
	
	/**
	 * Creates a new child node of requested {@link BranchNodeType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param consumer builder to set up the child
	 * @return new child node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<T,X> create(BranchNodeType<T,X> nodeType, BiConsumer<BranchNode<P, T>, BranchNode<T,X>> consumer)
	{
		if(rootNode.isImmutable())
		{
			return null;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			boolean created = false;
			BranchNode<T,X> oldNode = (BranchNode<T,X>)nodeContainer.node;
			BranchNode<T,X> newNode = new BranchNode(this.rootNode,this,nodeContainer);
			
			try
			{
				if(consumer != null)
				{
					consumer.accept(this, newNode);
				}
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, oldNode, newNode))
				{
					try
					{
						nodeContainer.node = newNode;
						nodeContainer.node.setRootLinked(super.rootLinked);
						created = true;
						this.rootNode.notifyAfterModify(this, nodeContainer, oldNode, newNode);
					}
					finally
					{
						if(oldNode != null)
						{
							oldNode.disposeNode();
						}
					}
					return newNode;
				}
			}
			finally 
			{
				if(! created)
				{
					newNode.disposeNode();
				}
			}
			return null;
		}
		finally 
		{
			if(lock != null)
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
	 * @return child node or null, if child node does not exist and auto-create-mode is off
	 */
	public <X extends BranchNodeMetaModel> BranchNode<T,X> get(BranchNodeType<T,X> nodeType)
	{
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		if(! this.rootNode.isBranchNodeGetterAutoCreate())
		{
			return (BranchNode<T,X>) nodeContainer.node;
		}
		
		BranchNode<T,X> node = (BranchNode<T,X>)nodeContainer.node;
		if(node != null)
		{
			return node;
		}
		
		// autocreate
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			node = (BranchNode<T,X>)nodeContainer.node;
			if(node != null)
			{
				return node;
			}
			boolean created = false;
			
			node = new BranchNode(this.rootNode,this,nodeContainer);
			
			try
			{
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
				{
					nodeContainer.node = node;
					nodeContainer.node.setRootLinked(super.rootLinked);
					created = true;
					this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
				}
			}
			finally 
			{
				if(! created)
				{
					node.disposeNode();
				}
			}
			return node;
		}
		finally 
		{
			if(lock != null)
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
	 * @return unmodifiable node list
	 */
	public <X extends BranchNodeMetaModel> List<BranchNode<T,X>> getUnmodifiableNodeList(BranchNodeListType<T,X> nodeType)
	{
		return this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType)).unmodifiableNodeList;
	}
	
	/**
	 * Getter for a snapshot of unmodifiable child node list with all child nodes of requested {@link BranchNodeListType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @return unmodifiable node list snapshot
	 */
	public <X extends BranchNodeMetaModel> List<BranchNode<T,X>> getUnmodifiableNodeListSnapshot(BranchNodeListType<T,X> nodeType)
	{
		return getUnmodifiableNodeListSnapshot(nodeType, null);
	}
	
	/**
	 * Getter for a snapshot of unmodifiable child node list with filtered child nodes of requested {@link BranchNodeListType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param predicate filter for snapshot
	 * @return snapshot unmodifiable node list snapshot
	 */
	public <X extends BranchNodeMetaModel> List<BranchNode<T,X>> getUnmodifiableNodeListSnapshot(BranchNodeListType<T,X> nodeType, Predicate<BranchNode<T,X>> predicate)
	{
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		if(predicate == null)
		{
			List<BranchNode<T,X>> unmodifiableNodeListSnapshot = nodeContainer.unmodifiableNodeListSnapshot;
			if(unmodifiableNodeListSnapshot != null)
			{
				return unmodifiableNodeListSnapshot;
			}
		}
			
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			if(predicate == null)
			{
				List<BranchNode<T,X>> unmodifiableNodeListSnapshot = nodeContainer.unmodifiableNodeListSnapshot;
				if(unmodifiableNodeListSnapshot != null)
				{
					return unmodifiableNodeListSnapshot;
				}
				
				List<BranchNode<T,X>> snapshot = new ArrayList<BranchNode<T,X>>();
				nodeContainer.nodeList.forEach(n -> snapshot.add(n));
				nodeContainer.unmodifiableNodeListSnapshot = Collections.unmodifiableList(snapshot);
				return snapshot;
			}
			
			List<BranchNode<T,X>> filteredList = new ArrayList<BranchNode<T,X>>();
			nodeContainer.nodeList.forEach(n -> { if(predicate.test(n)) {filteredList.add(n);} });
			return Collections.unmodifiableList(filteredList);
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
	}
	
	/**
	 * Sets a comparator to sort all child nodes for requested {@link BranchNodeListType}.
	 * 
	 * @param nodeType static child node type instance from meta model.
	 * @param comparator comparator to apply
	 * @return this branch node
	 *  
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> setComperator(BranchNodeListType<T,X> nodeType, Comparator<BranchNode<T,X>> comparator)
	{
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			if((nodeContainer.listComparator == null) && (comparator == null))
			{
				return this;
			}
			
			nodeContainer.listComparator = comparator;
			
			if(this.rootNode.isImmutable())
			{
				return this;
			}
			
			Collections.sort(nodeContainer.nodeList, nodeContainer.listComparator);
			nodeContainer.unmodifiableNodeListSnapshot = null;
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		
		return this;
	}
	
	/**
	 * Get first matched child node of requested {@link BranchNodeListType}.
	 * 
	 * @param nodeType static child node type instance from meta model
	 * @param predicate filter
	 * @return first matched node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<T,X> get(BranchNodeListType<T,X> nodeType, Predicate<BranchNode<T,X>> predicate)
	{
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			for(BranchNode<T,X> node : nodeContainer.nodeList)
			{
				if(predicate.test(node))
				{
					return node;
				}
			}
		}
		finally 
		{
			if(lock != null)
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
	 * @return new child node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<T,X> create(BranchNodeListType<T,X> nodeType)
	{
		if(this.rootNode.isImmutable())
		{
			return null;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			boolean created = false;
			BranchNode<T,X> node = new BranchNode(this.rootNode,this,nodeContainer);
			try
			{
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
				{
					nodeContainer.nodeList.add(node);
					node.setRootLinked(super.rootLinked);
					node.positionInList = nodeContainer.nodeList.size() -1;
					
					nodeContainer.unmodifiableNodeListSnapshot = null;
					created = true;
					
					this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
					
					return node;
				}
			}
			finally
			{
				if(! created)
				{
					node.disposeNode();
				}
			}
		}
		finally 
		{
			if(lock != null)
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
	 * @return this branch node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> create(BranchNodeListType<T,X> nodeType, BiConsumer<BranchNode<P, T>, BranchNode<T,X>> consumer)
	{
		if(this.rootNode.isImmutable())
		{
			return this;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			boolean created = false;
			BranchNode<T,X> node = new BranchNode(this.rootNode,this,nodeContainer);
			try
			{
				if(consumer != null)
				{
					consumer.accept(this, node);
				}
				
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
				{
					if((nodeContainer.listComparator == null) || nodeContainer.nodeList.isEmpty())
					{
						nodeContainer.nodeList.add(node);
						node.setRootLinked(super.rootLinked);
						node.positionInList = nodeContainer.nodeList.size() -1;
					}
					else
					{
						if(nodeContainer.nodeList.isEmpty() || nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(nodeContainer.nodeList.size() -1)) > 0)
						{
							nodeContainer.nodeList.add(node);
							node.setRootLinked(super.rootLinked);
							node.positionInList = nodeContainer.nodeList.size() -1;
						}
						else if(nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(0)) < 0)
						{
							nodeContainer.nodeList.add(0,node);
							node.setRootLinked(super.rootLinked);
							
							int index = 0;
							for(BranchNode nodeItem : nodeContainer.nodeList)
							{
								nodeItem.positionInList = index++;
								this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
							}
						}
						else
						{
							int beginIndex = 0;
							int rangeSize = nodeContainer.nodeList.size();
							int endIndex = rangeSize -1;
							int testIndex = endIndex / 2;
							int testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
							
							while(rangeSize > 1)
							{
								
								if(testResult < 0)
								{
									if(endIndex == testIndex)
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
							
							if(testResult < 0)
							{
								nodeContainer.nodeList.add(testIndex,node);
								node.setRootLinked(super.rootLinked);
								
								for(int i = testIndex; i < nodeContainer.nodeList.size(); i++)
								{
									BranchNode nodeItem = nodeContainer.nodeList.get(i);
									nodeItem.positionInList = i;
									this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
								}
							}
							else if(nodeContainer.nodeList.size() -1 > testIndex)
							{
								nodeContainer.nodeList.add(testIndex + 1,node);
								node.setRootLinked(super.rootLinked);
								for(int i = testIndex + 1; i < nodeContainer.nodeList.size(); i++)
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
								node.positionInList = nodeContainer.nodeList.size() -1;
							}
						}
					}
					nodeContainer.unmodifiableNodeListSnapshot = null;
					created = true;
					this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
				}
			}
			finally 
			{
				if(!created)
				{
					node.disposeNode();
				}
			}
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		return this;
	}
	
	/**
	 * Creates a new child node of requested {@link BranchNodeListType}, if no item exists matched by <code>predicate</code>.
	 * 
	 * @param nodeType static child node type instance from meta model.
	 * @param predicate predicate to test existing items
	 * @param consumer setup new child node
	 * 
	 * @return this child node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T> createIfAbsent(BranchNodeListType<T,X> nodeType, Predicate<BranchNode<T,X>> predicate,  BiConsumer<BranchNode<P, T>, BranchNode<T,X>> consumer)
	{
		if(this.rootNode.isImmutable())
		{
			return this;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			for(BranchNode<T,X> node : nodeContainer.nodeList)
			{
				if(predicate.test(node))
				{
					if(consumer != null)
					{
						consumer.accept(this, node);
					}
					return this;
				}
			}
			boolean created = false;
			BranchNode<T,X> node = new BranchNode(this.rootNode,this,nodeContainer);
			try
			{
				if(consumer != null)
				{
					consumer.accept(this, node);
				}
				
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, null, node))
				{
					if((nodeContainer.listComparator == null) || nodeContainer.nodeList.isEmpty())
					{
						nodeContainer.nodeList.add(node);
						node.setRootLinked(super.rootLinked);
						node.positionInList = nodeContainer.nodeList.size() -1;
					}
					else
					{
						if(nodeContainer.nodeList.isEmpty() || nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(nodeContainer.nodeList.size() -1)) > 0)
						{
							nodeContainer.nodeList.add(node);
							node.setRootLinked(super.rootLinked);
							node.positionInList = nodeContainer.nodeList.size() -1;
						}
						else if(nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(0)) < 0)
						{
							nodeContainer.nodeList.add(0,node);
							node.setRootLinked(super.rootLinked);
							int index = 0;
							for(BranchNode nodeItem : nodeContainer.nodeList)
							{
								nodeItem.positionInList = index++;
								this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
							}
						}
						else
						{
							int beginIndex = 0;
							int rangeSize = nodeContainer.nodeList.size();
							int endIndex = rangeSize -1;
							int testIndex = endIndex / 2;
							int testResult = nodeContainer.listComparator.compare(node, nodeContainer.nodeList.get(testIndex));
							
							while(rangeSize > 1)
							{
								
								if(testResult < 0)
								{
									if(endIndex == testIndex)
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
							
							if(testResult < 0)
							{
								nodeContainer.nodeList.add(testIndex,node);
								node.setRootLinked(super.rootLinked);
								for(int i = testIndex; i < nodeContainer.nodeList.size(); i++)
								{
									BranchNode nodeItem = nodeContainer.nodeList.get(i);
									nodeItem.positionInList = i;
									this.rootNode.notifyAfterModify(this, nodeContainer, nodeItem, nodeItem);
								}
							}
							else if(nodeContainer.nodeList.size() -1 > testIndex)
							{
								nodeContainer.nodeList.add(testIndex + 1,node);
								node.setRootLinked(super.rootLinked);
								for(int i = testIndex + 1; i < nodeContainer.nodeList.size(); i++)
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
								node.positionInList = nodeContainer.nodeList.size() -1;
							}
						}
					}
					nodeContainer.unmodifiableNodeListSnapshot = null;
					created = true;
					
					this.rootNode.notifyAfterModify(this, nodeContainer, null, node);
				}
			}
			finally 
			{
				if(!created)
				{
					node.disposeNode();
				}
			}
		}
		finally 
		{
			if(lock != null)
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
	 * @param node node instance to remove
	 * @return true, if node successfully removed, otherwise false
	 */
	public <X extends BranchNodeMetaModel> boolean remove(BranchNodeListType<T,X> nodeType, BranchNode<P, T> node)
	{
		if(this.rootNode.isImmutable())
		{
			return false;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			if(nodeContainer.nodeList.contains(node))
			{
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
				{
					nodeContainer.nodeList.remove(node);
					node.setRootLinked(false);
					int positionInList = node.positionInList;
					try
					{
						nodeContainer.unmodifiableNodeListSnapshot = null;
						for(int i = positionInList; i < nodeContainer.nodeList.size(); i++)
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
			if(lock != null)
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
	 * return this branch node
	 */
	public <X extends BranchNodeMetaModel> BranchNode<P, T>  clear(BranchNodeListType<T,X> nodeType)
	{
		if(this.rootNode.isImmutable())
		{
			return this;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			List<BranchNode<P, T>> copy = (List<BranchNode<P, T>>)new ArrayList(nodeContainer.nodeList);
			
			for(BranchNode<P,T> node : copy)
			{
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
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
		}
		finally 
		{
			if(lock != null)
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
	 * @param index position index of child node in list
	 * @return true, if node is successfully removed, otherwise false
	 */
	public <X extends BranchNodeMetaModel> boolean remove(BranchNodeListType<T,X> nodeType, int index)
	{
		if(this.rootNode.isImmutable())
		{
			return false;
		}
		
		NodeContainer nodeContainer = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(nodeType));
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			BranchNode<T,X> node = (BranchNode<T,X>)nodeContainer.nodeList.get(index);
			
			if(node != null)
			{
				if(this.rootNode.notifyBeforeModify(this, nodeContainer, node, null))
				{
					nodeContainer.nodeList.remove(index);
					node.setRootLinked(false);
					int positionInList = node.positionInList;
					try
					{
						nodeContainer.unmodifiableNodeListSnapshot = null;
						for(int i = positionInList; i < nodeContainer.nodeList.size(); i++)
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
			if(lock != null)
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
	 * @return this branch node
	 */
	public BranchNode<P, T> removeChildNodeListener(IChildNodeListener<T> listener)
	{
		if(listener == null)
		{
			return this;
		}
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
		
			for(NodeContainer container :  this.nodeContainerList)
			{
				if(container.nodeListenerList != null)
				{
					container.nodeListenerList.remove(listener);
				}
			}
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		
		return this;
	}
	
	public <X> BranchNode<P,T> addChildNodeListener(LeafNodeType<T,X> nodeType,ILeafNodeListener<T, X> listener)
	{
		return addChildNodeListener(listener, nodeType);
	}
	
	/**
	 * Add listener to get notified if child leaf nodes are updated , and child branch nodes are removed or created.
	 *  
	 * @param listener listener to register
	 * @param filter child node filter apply to listener
	 * @return this branch node
	 */
	public BranchNode<P, T> addChildNodeListener(IChildNodeListener<T> listener, INodeType<T,?>... filter)
	{
		if(listener == null)
		{
			return this;
		}
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
		
			if((filter == null) || (filter.length == 0))
			{
				for(NodeContainer container :  this.nodeContainerList)
				{
					if(container.nodeListenerList == null)
					{
						container.nodeListenerList = new LinkedList<IChildNodeListener>();
					}
					else if(container.nodeListenerList.contains(listener))
					{
						continue;
					}
					container.nodeListenerList.add(listener);
				}
			}
			else
			{
				for(INodeType<T, ?> filteredType : filter)
				{
					if(filteredType == null)
					{
						continue;
					}
					NodeContainer container = this.nodeContainerList.get(this.model.getNodeTypeIndexByClass().get(filteredType));
					if(container == null)
					{
						continue;
					}
					
					if(container.nodeListenerList == null)
					{
						container.nodeListenerList = new LinkedList<IChildNodeListener>();
					}
					else if(container.nodeListenerList.contains(listener))
					{
						continue;
					}
					container.nodeListenerList.add(listener);
				}
			}
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		
		return this;
	}
	
	/*
	 * Path Modify
	 */
	
	public <X> ModifyListenerRegistration<X> registerForModify(ModelPath<T, X> path, IModifyListener<X> listener)
	{
		Objects.requireNonNull(path, "Model path is null");
		Objects.requireNonNull(path.getNodeSelectorList(), "Model path is disposed");
		Objects.requireNonNull(path.getClazz(), "Model path is disposed");
		Objects.requireNonNull(listener, "Listener path is null");
		
		if(path.getNodeSelectorList().isEmpty())
		{
			throw new RuntimeException("path is empty");
		}
		
		NodeSelector rootNodeSelector = path.getNodeSelectorList().getFirst();
		Objects.requireNonNull(rootNodeSelector.getNextSelector(), "path contains root self only");
		
		
		ModifyListenerRegistration registration = new ModifyListenerRegistration(path.getClazz());
		
		Lock lock = this.rootNode.isSynchronized() ? this.rootNode.getWriteLock() : null;
		if(lock != null)
		{
			lock.lock();
		}
		try
		{
			if(this.modifyListenerRegistrationList == null)
			{
				this.modifyListenerRegistrationList = new ArrayList<ModifyListenerRegistration<?>>();
			}
			
			// TODO root Predicate
			
			NodeSelector nextSelector = rootNodeSelector.getNextSelector();
			
			if(nextSelector.getNextSelector() == null)
			{
				for(NodeContainer container : this.nodeContainerList)
				{
					if(container.getNodeType() == nextSelector.getType())
					{
						ModifyListenerContainer foundListener = null;
						if(container.nodeListenerList == null)
						{
							container.nodeListenerList = new ArrayList<IChildNodeListener>();
						}
						else
						{
							for(IChildNodeListener childNodeListener : container.nodeListenerList)
							{
								if((childNodeListener instanceof BranchNode.ModifyListenerContainer) && ((BranchNode.ModifyListenerContainer)childNodeListener).selector.equals(nextSelector))
								{
									foundListener = (BranchNode.ModifyListenerContainer) childNodeListener;
									break;
								}
							}
						}
						if(foundListener == null)
						{
							foundListener = new BranchNode.ModifyListenerContainer();
							foundListener.selector = nextSelector.clone(null);
							foundListener.selector.setRoot(null);
							
							container.nodeListenerList.add(foundListener);
						}
						
						Set<ModifyListenerRegistration> registrationSet = foundListener.registrationListByListener.get(listener);
						if(registrationSet == null)
						{
							registrationSet = new HashSet<ModifyListenerRegistration>();
							foundListener.registrationListByListener.put(listener,registrationSet);
						}
						registrationSet.add(registration);
						break;
					}
				}
			}
			else
			{
				for(NodeContainer container : this.nodeContainerList)
				{
					if(container.getNodeType() == nextSelector.getType())
					{
						ModifyListenerNodeSelector foundListener = null;
						if(container.nodeListenerList == null)
						{
							container.nodeListenerList = new ArrayList<IChildNodeListener>();
						}
						else
						{
							for(IChildNodeListener childNodeListener : container.nodeListenerList)
							{
								if((childNodeListener instanceof BranchNode.ModifyListenerNodeSelector) && ((BranchNode.ModifyListenerNodeSelector)childNodeListener).selector.equals(nextSelector))
								{
									foundListener = (BranchNode.ModifyListenerNodeSelector) childNodeListener;
									break;
								}
							}
						}
						if(foundListener == null)
						{
							foundListener = new BranchNode.ModifyListenerNodeSelector();
							foundListener.selector = nextSelector.clone(null);
							foundListener.selector.setRoot(null);
							container.nodeListenerList.add(foundListener);
						}
						
						foundListener.pathPositionByModifyListenerRegistration.put(registration, 1);
						break;
					}
				}
			}
			
			this.modifyListenerRegistrationList.add(registration);
		}
		finally 
		{
			if(lock != null)
			{
				lock.unlock();
			}
		}
		
		return registration;
	}
	
	protected class ModifyListenerNodeSelector implements IChildNodeListener<T>
	{
		private NodeSelector selector = null;
		private Map<ModifyListenerRegistration, Integer> pathPositionByModifyListenerRegistration = new HashMap<ModifyListenerRegistration,Integer>();
		private boolean active = false;

		@Override
		public void accept(Node<T, ?> node, Object oldValue)
		{
			System.out.print("XXX: " + node);
		}
		
	}
	
	protected class ModifyListenerContainer implements IChildNodeListener<T>
	{
		private NodeSelector selector = null;
		private Map<IModifyListener,Set<ModifyListenerRegistration>> registrationListByListener = new HashMap<IModifyListener,Set<ModifyListenerRegistration>>();
		private boolean active = true;
		
		@Override
		public void accept(Node<T, ?> node, Object oldValue)
		{
			if(! this.active)
			{
				return;
			}
			
			for(IModifyListener modifyListener: registrationListByListener.keySet())
			{
				if(selector.getAxis() == Axis.VALUE)
				{
					modifyListener.accept(((LeafNode)node).getValue(), oldValue);
				}
				else
				{
					modifyListener.accept(node, oldValue);
				}
			}
		}
		
		protected void checkActive()
		{
			// TODO
		}
		
	}
	
	public class ModifyListenerRegistration<X>
	{
		private Class<X> clazz = null;
		private UUID id = null;
		
		private ModifyListenerRegistration(Class<X> clazz)
		{
			super();
			this.clazz = clazz;
			this.id = UUID.randomUUID();
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ModifyListenerRegistration other = (ModifyListenerRegistration) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (clazz == null)
			{
				if (other.clazz != null)
					return false;
			} else if (!clazz.equals(other.clazz))
				return false;
			if (id == null)
			{
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
		
		public void unregister()
		{
			// TODO
		}

		private BranchNode getOuterType()
		{
			return BranchNode.this;
		}
	}
	
	// Helper Node
	
	protected static class NodeContainer
	{
		protected NodeContainer()
		{
			super();
		}
		protected NodeContainer(INodeType nodeType)
		{
			super();
			this.nodeType = nodeType;
		}
		private INodeType nodeType = null;
		private volatile Node node = null;
		private ArrayList<BranchNode> nodeList = null; 
		private List unmodifiableNodeList = null;
		private volatile Comparator listComparator = null; 
		private volatile List unmodifiableNodeListSnapshot = null;
		private volatile List<IChildNodeListener> nodeListenerList = null;
		
		protected INodeType getNodeType()
		{
			return nodeType;
		}
		protected void setNodeType(INodeType nodeType)
		{
			this.nodeType = nodeType;
		}
		protected Node getNode()
		{
			return node;
		}
		protected void setNode(Node node)
		{
			this.node = node;
		}
		protected ArrayList<BranchNode> getNodeList()
		{
			return nodeList;
		}
		protected void setNodeList(ArrayList<BranchNode> nodeList)
		{
			this.nodeList = nodeList;
		}
		protected List getUnmodifiableNodeList()
		{
			return unmodifiableNodeList;
		}
		protected void setUnmodifiableNodeList(List unmodifiableNodeList)
		{
			this.unmodifiableNodeList = unmodifiableNodeList;
		}
		protected Comparator getListComparator()
		{
			return listComparator;
		}
		protected void setListComparator(Comparator listComparator)
		{
			this.listComparator = listComparator;
		}
		protected List getUnmodifiableNodeListSnapshot()
		{
			return unmodifiableNodeListSnapshot;
		}
		protected void setUnmodifiableNodeListSnapshot(List unmodifiableNodeListSnapshot)
		{
			this.unmodifiableNodeListSnapshot = unmodifiableNodeListSnapshot;
		}
		protected List<IChildNodeListener> getNodeListenerList()
		{
			return nodeListenerList;
		}
		protected void setNodeListenerList(List<IChildNodeListener> nodeListenerList)
		{
			this.nodeListenerList = nodeListenerList;
		}
		
	}
	
	public List<LeafNodeType> getLeafNodeTypeList()
	{
		return this.model.getLeafNodeTypeList();
	}
	public List<BranchNodeType> getBranchNodeTypeList()
	{
		return this.model.getBranchNodeTypeList();
	}
	public List<BranchNodeListType> getBranchNodeListTypeList()
	{
		return this.model.getBranchNodeListTypeList();
	}
	
	// for tests only
	
	protected boolean isDisposed()
	{
		return 
			model == null &&
			nodeContainerList == null &&
			modifyListenerRegistrationList == null &&
			rootNode == null &&
			parentNode == null &&
			OID == -1L &&
			positionInList == -1;
	}
}
