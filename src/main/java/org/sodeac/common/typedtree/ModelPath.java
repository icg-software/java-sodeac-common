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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.sodeac.common.expression.BooleanFunction.LogicalOperator;
import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.typedtree.ModelPath.ModelPathBuilder.RootModelPathBuilder;
import org.sodeac.common.typedtree.ModelPath.NodeSelector.Axis;
import org.sodeac.common.typedtree.ModelPath.NodeSelector.NodeSelectorPredicate;

/**
 * A model path selects nodes. It starts from source node and navigate to tree by path definition to select nodes or value of nodes.
 * 
 * @author Sebastian Palarus
 *
 * @param <R> type of start node
 * @param <T> type of nodes or node's value to select
 */
public class ModelPath<R extends BranchNodeMetaModel,T>
{
	private LinkedList<NodeSelector<?,?>> selectorList = new LinkedList<>();
	private Class<T> clazz = null;
	private boolean indisposable = false;
	
	private ModelPath()
	{
		super();
	}
	
	protected ModelPath(NodeSelector<R,T> lastSelector)
	{
		super();
		
		if(lastSelector.getType() == null)
		{
			this.clazz = (Class<T>) lastSelector.getRootType().getClass();
		}
		else
		{
			if(lastSelector.getAxis() == Axis.VALUE)
			{
				this.clazz = (Class<T>) lastSelector.getType().getTypeClass();
			}
			else
			{
				this.clazz = (Class<T>) lastSelector.getType().getClass();
			}
		}
		
		NodeSelector<?,?> current = lastSelector;
		while(current != null)
		{
			selectorList.addFirst(current);
			current = current.getParentSelector();
		}
	}
	
	protected Class<T> getClazz()
	{
		return clazz;
	}

	protected LinkedList<NodeSelector<?, ?>> getNodeSelectorList()
	{
		return selectorList;
	}

	
	public boolean isIndisposable()
	{
		return indisposable;
	}

	public ModelPath<R,T> setIndisposable()
	{
		this.indisposable = true;
		return this;
	}

	public void dispose()
	{
		if(indisposable)
		{
			return;
		}
		for(NodeSelector<?,?> selector : selectorList)
		{
			selector.dispose();
		}
		selectorList.clear();
		selectorList = null;
		clazz = null;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((selectorList == null) ? 0 : selectorList.hashCode());
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
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
		ModelPath other = (ModelPath) obj;
		if (selectorList == null)
		{
			if (other.selectorList != null)
				return false;
		} else if (!selectorList.equals(other.selectorList))
			return false;
		if (clazz == null)
		{
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		return true;
	}
	
	public ModelPath<R,T> clone()
	{
		ModelPath<R, T> clonedModelPath = new ModelPath<R,T>();
		if(this.selectorList != null)
		{
			clonedModelPath.selectorList = new LinkedList<NodeSelector<?,?>>();
			
			NodeSelector previewsNodeSelector = null;
			for(NodeSelector nodeSelector :  this.selectorList)
			{
				NodeSelector<R,T> clonedNodeSelector = new NodeSelector<>();
				clonedNodeSelector.root = nodeSelector.root;
				clonedNodeSelector.parentSelector = previewsNodeSelector;
				clonedNodeSelector.type = nodeSelector.type;
				clonedNodeSelector.axis = nodeSelector.axis;
				clonedNodeSelector.predicate = nodeSelector.predicate;
				
				if(nodeSelector.childSelectorList != null)
				{
					clonedNodeSelector.childSelectorList = new HashSet<NodeSelector<?,?>>();
				}
				
				if((previewsNodeSelector != null) && (previewsNodeSelector.childSelectorList != null))
				{
					previewsNodeSelector.childSelectorList.add(clonedNodeSelector);
				}
				
				previewsNodeSelector = clonedNodeSelector;
				clonedModelPath.selectorList.add(clonedNodeSelector);
			}
		}
		clonedModelPath.clazz = this.clazz;
		return clonedModelPath;
	}

	public static class ModelPathBuilder<R extends BranchNodeMetaModel,S extends BranchNodeMetaModel>
	{
		private BranchNodeMetaModel root = null;
		private BranchNodeMetaModel self = null;
		private NodeSelector<?, ?> selector = null;
		
		/**
		 * Create new model path builder.
		 * 
		 * @param rootClass type off root node in path (start node)
		 * @return builder
		 */
		public static <R extends BranchNodeMetaModel>  RootModelPathBuilder<R> newBuilder(Class<R> rootClass)
		{
			try
			{
				return new RootModelPathBuilder<>(ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(rootClass));
			}
			catch (Exception e) 
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 * Create new model path builder.
		 * 
		 * @param root root node of path (start node)
		 * @return builder
		 */
		public static <R extends BranchNodeMetaModel>  RootModelPathBuilder<R> newBuilder(R root)
		{
			return new RootModelPathBuilder<>(root);
		}
		
		private ModelPathBuilder(BranchNodeMetaModel root)
		{
			super();
			this.root = root;
			this.self = root;
			this.selector = new NodeSelector<>(root);
		}
		
		private ModelPathBuilder(BranchNodeMetaModel root,BranchNodeType field, NodeSelector<?,?> previews)
		{
			super();
			this.root = root;
			this.self = field.getValueDefaultInstance();
			this.selector = new NodeSelector<>(this.root,previews,field,NodeSelector.Axis.CHILD);
		}
		
		/**
		 * Definition to navigate to next child node.
		 * 
		 * @param field static child node type instance from meta model
		 * @return builder
		 */
		public <N extends BranchNodeMetaModel> ModelPathBuilder<R,N> child(BranchNodeType<S, N> field)
		{
			if((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
			{
				throw new RuntimeException(getClass() + ": child already exists ");
			}
			return new ModelPathBuilder<R,N>(this.root,field,this.selector);
		}
		
		public <N extends BranchNodeMetaModel> BranchNodePredicateBuilder<R,N> childWithPredicates(BranchNodeType<S, N> field) // TODO BranchNodeListType
		{
			if((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
			{
				throw new RuntimeException(getClass() + ": child already exists ");
			}
			ModelPathBuilder<R,N> builder = new ModelPathBuilder<R,N>(this.root,field,selector);
			return new BranchNodePredicateBuilder(this.self,builder);
		}
		
		public <T> ModelPath<R, T> buildForValue(LeafNodeType<S, T> field) 
		{
			if((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
			{
				throw new RuntimeException(getClass() + ": child already exists ");
			}
			return new ModelPath(new NodeSelector<R,T>(this.root,this.selector,field, NodeSelector.Axis.VALUE));
		}
		
		public <T> ModelPath<R, T> build() 
		{
			return new ModelPath(this.selector);
		}
		
		public <T> ModelPath<R, LeafNode<?, T>> buildForNode(LeafNodeType<S, T> field) 
		{
			if((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
			{
				throw new RuntimeException(getClass() + ": child already exists ");
			}
			return new ModelPath(new NodeSelector<R,LeafNode<?, T>>(this.root,this.selector, (INodeType)field, NodeSelector.Axis.CHILD));
		}
		
		public <T extends BranchNodeMetaModel> ModelPath<R, BranchNode<S, T>> buildForNode(BranchNodeType<S, T> field) 
		{
			if((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
			{
				throw new RuntimeException(getClass() + ": child already exists ");
			}
			return new ModelPath(new NodeSelector<R,BranchNode<S, T>>(this.root,this.selector, (INodeType)field , NodeSelector.Axis.CHILD));
		}
		
		protected BranchNodeMetaModel getSelf()
		{
			return this.self;
		}
		
		protected BranchNodeMetaModel getRoot()
		{
			return this.root;
		}
		
		/**
		 * Helper class to build model paths.
		 * 
		 * @author Sebastian Palarus
		 *
		 * @param <R> type off root node in path
		 */
		public static class RootModelPathBuilder<R extends BranchNodeMetaModel> extends ModelPathBuilder<R, R>
		{
			private RootModelPathBuilder(BranchNodeMetaModel root)
			{
				super(root);
			}
			
			public BranchNodePredicateBuilder<R,R> childWithPredicates()
			{
				return new BranchNodePredicateBuilder(super.getSelf(),this);
			}
		}
		
		public static class BranchNodePredicateBuilder<R extends BranchNodeMetaModel,N extends BranchNodeMetaModel>
		{
			private ModelPathBuilder<R,N> builder = null;
			private NodeSelectorPredicate<N> rootPedicate = null;
			private NodeSelectorPredicate<N> currentPredicate = null;
			private N defaultModelInstance = null;
			
			private BranchNodePredicateBuilder(N defaultModelInstance, ModelPathBuilder<R,N> builder)
			{
				super();
				this.builder = builder;
				this.defaultModelInstance = defaultModelInstance;
				this.rootPedicate = new NodeSelectorPredicate(defaultModelInstance,null, LogicalOperator.AND, false);
				this.currentPredicate = this.rootPedicate;
			}
			
			public <T> BranchNodePredicateBuilder<R,N> addLeafNodePredicate(LeafNodeType<N, T> field, Predicate<T> predicate)
			{
				this.currentPredicate.addLeafNodePredicate((LeafNodeType)field, (Predicate)predicate );
				return this;
			}
			
			public <T> BranchNodePredicateBuilder<R,N> addPathPredicate(Function<RootModelPathBuilder<N>,ModelPath<N, T>> pathBuilderFunction, Predicate<T> predicate)
			{
				this.currentPredicate.addPathPredicate((Function)pathBuilderFunction, (Predicate)predicate);
				return this;
			}
			
			public BranchNodePredicateBuilder<R,N> and()
			{
				this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance,this.currentPredicate, LogicalOperator.AND, false);
				return this;
			}
			
			public BranchNodePredicateBuilder<R,N> andNot()
			{
				this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance,this.currentPredicate, LogicalOperator.AND, true);
				return this;
			}
			
			public BranchNodePredicateBuilder<R,N> or()
			{
				this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance,this.currentPredicate, LogicalOperator.OR, false);
				return this;
			}
			
			public BranchNodePredicateBuilder<R,N> orNot()
			{
				this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance,this.currentPredicate, LogicalOperator.OR, true);
				return this;
			}
			
			public BranchNodePredicateBuilder<R,N> close()
			{
				this.currentPredicate = currentPredicate.getParent();
				Objects.requireNonNull(this.currentPredicate, "No child predicate to close");
				return this;
			}
			
			public ModelPathBuilder<R,N> build()
			{
				this.builder.selector.setPredicate(this.rootPedicate);
				return this.builder;
			}
		}
	}

	protected static class NodeSelector<R extends BranchNodeMetaModel,T>
	{
		protected static enum Axis {SELF,CHILD,VALUE}
		
		private NodeSelector<?,?> parentSelector = null;
		private Set<NodeSelector<?,?>> childSelectorList = null;
		private Set<IModifyListener<?>> modifyListenerList = null;
		private Map<ConplierBean<Object>,Set<IModifyListener<?>>> registrationObjects = null;
		private NodeSelectorPredicate predicate = null;
		private INodeType<?, ?> type = null;
		private BranchNodeMetaModel root = null;
		private Axis axis = null;
		private volatile boolean disposed = false;
		
		protected NodeSelector(BranchNodeMetaModel root)
		{
			super();
			this.root = root;
			this.axis = Axis.SELF;
		}
		
		protected NodeSelector<R,T> clone(NodeSelector<?,?> clonedParentSelector, boolean deep)
		{
			NodeSelector<R,T> clonedNodeSelector = new NodeSelector<>();
			clonedNodeSelector.root = this.root;
			clonedNodeSelector.parentSelector = clonedParentSelector;
			clonedNodeSelector.type = this.type;
			clonedNodeSelector.axis = this.axis;
			if((childSelectorList != null) && deep)
			{
				clonedNodeSelector.childSelectorList = new HashSet<NodeSelector<?,?>>(); 
				for(NodeSelector<?, ?> child : this.childSelectorList)
				{
					clonedNodeSelector.childSelectorList.add(child.clone(clonedNodeSelector,true));
				}
			}
			clonedNodeSelector.predicate = this.predicate;
			
			return clonedNodeSelector;
		}
		
		private NodeSelector()
		{
			super();
		}
		
		protected NodeSelector(BranchNodeMetaModel root, NodeSelector<?,?> previousSelector, INodeType<?,?> type, Axis axis)
		{
			super();
			this.root = root;
			this.parentSelector = previousSelector;
			this.type = type;
			if(previousSelector.childSelectorList == null)
			{
				previousSelector.childSelectorList = new HashSet<NodeSelector<?,?>>();
			}
			previousSelector.childSelectorList.add(this);
			if(axis == null)
			{
				axis = Axis.CHILD;
			}
			this.axis = axis;
		}
		
		protected NodeSelector<?, ?> getParentSelector()
		{
			return parentSelector;
		}
		protected NodeSelector<?, ?> setParentSelector(NodeSelector<?, ?> parentSelector)
		{
			this.parentSelector = parentSelector;
			return this;
		}
		protected Set<NodeSelector<?, ?>> getChildSelectorList()
		{
			return this.childSelectorList;
		}
		protected void setChildSelectorList(Set<NodeSelector<?, ?>> childSelectorList)
		{
			this.childSelectorList = childSelectorList;
		}

		protected NodeSelector<?, ?> setPredicate(NodeSelectorPredicate predicate)
		{
			this.predicate = predicate;
			return this;
		}

		protected NodeSelectorPredicate getPredicate()
		{
			return predicate;
		}
		
		protected INodeType<?, ?> getType()
		{
			return type;
		}

		protected BranchNodeMetaModel getRootType()
		{
			return root;
		}

		protected void setRootType(BranchNodeMetaModel root)
		{
			this.root = root;
		}

		protected Axis getAxis()
		{
			return axis;
		}

		protected Set<IModifyListener<?>> getModifyListenerList()
		{
			return modifyListenerList;
		}

		protected void setModifyListenerList(Set<IModifyListener<?>> modifyListenerList)
		{
			this.modifyListenerList = modifyListenerList;
		}

		protected Map<ConplierBean<Object>, Set<IModifyListener<?>>> getRegistrationObjects()
		{
			return registrationObjects;
		}

		protected void setRegistrationObjects(Map<ConplierBean<Object>, Set<IModifyListener<?>>> registrationObjects)
		{
			this.registrationObjects = registrationObjects;
		}

		protected void dispose()
		{
			this.disposed = true;
			if(childSelectorList != null)
			{
				for(NodeSelector<?, ?> child : this.childSelectorList)
				{
					child.dispose();
				}
				this.childSelectorList.clear();
			}
			
			if(registrationObjects != null)
			{
				for(Set<IModifyListener<?>> listener : registrationObjects.values())
				{
					if(listener == null)
					{
						continue;
					}
					listener.clear();
				}
				registrationObjects.clear();
			}
			
			if(this.modifyListenerList != null)
			{
				this.modifyListenerList.clear();
			}
			
			if(predicate != null)
			{
				predicate.dispose();
			}
			parentSelector = null;
			childSelectorList = null;
			predicate = null;
			registrationObjects = null;
			type = null;
			root = null;
			axis = null;
		}
		
		protected boolean isDisposed()
		{
			return disposed;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((axis == null) ? 0 : axis.hashCode());
			result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			NodeSelector other = (NodeSelector) obj;
			if (axis != other.axis)
				return false;
			if (predicate == null)
			{
				if (other.predicate != null)
					return false;
			} else if (!predicate.equals(other.predicate))
				return false;
			if (type == null)
			{
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}



		protected static class NodeSelectorPredicate<T extends BranchNodeMetaModel>
		{
			protected NodeSelectorPredicate(T defaultMetaInstance,NodeSelectorPredicate parent, LogicalOperator logicalOperator,boolean invert)
			{
				super();
				this.parent = parent;
				this.logicalOperator = logicalOperator;
			}
			
			private NodeSelectorPredicate parent = null;
			private LogicalOperator logicalOperator = null;
			private boolean invert = false;
			
			private List<LeafNodePredicate<T, ?>> leafNodePredicateList = null;
			private List<PathPredicate<T, ?>> pathPredicateList = null;
			private List<NodeSelectorPredicate> childPredicateList = null;
			private T defaultMetaInstance = null;
			
			protected LogicalOperator getLogicalOperator()
			{
				return logicalOperator;
			}
			protected void setLogicalOperator(LogicalOperator logicalOperator)
			{
				this.logicalOperator = logicalOperator;
			}

			protected boolean isInvert()
			{
				return invert;
			}
			protected void setInvert(boolean invert)
			{
				this.invert = invert;
			}

			protected NodeSelectorPredicate getParent()
			{
				return parent;
			}

			protected NodeSelectorPredicate addLeafNodePredicate(LeafNodeType<T, ?> field, Predicate<?> predicate)
			{
				if(this.leafNodePredicateList == null)
				{
					this.leafNodePredicateList = new ArrayList<NodeSelector.LeafNodePredicate<T, ?>>();
				}
				this.leafNodePredicateList.add(new LeafNodePredicate(field, predicate) );
				return this;
			}
			
			protected NodeSelectorPredicate addPathPredicate(Function<RootModelPathBuilder<T>,ModelPath<T, ?>> pathBuilderFunction, Predicate<?> predicate)
			{
				Objects.requireNonNull(pathBuilderFunction, "path builder function is null");
				if(this.pathPredicateList == null)
				{
					this.pathPredicateList = new ArrayList<NodeSelector.PathPredicate<T,?>>();
				}
				RootModelPathBuilder<T> builder = ModelPathBuilder.newBuilder(this.defaultMetaInstance);
				ModelPath<T, ?> path = pathBuilderFunction.apply(builder);
				if(path != null)
				{
					this.pathPredicateList.add(new PathPredicate(path, predicate));
				}
				return this;
			}
			
			protected NodeSelectorPredicate addChildPredicate(NodeSelectorPredicate childPredicate)
			{
				if(this.childPredicateList == null)
				{
					this.childPredicateList = new ArrayList<NodeSelectorPredicate>();
				}
				this.childPredicateList.add(childPredicate);
				return this;
			}
			
			protected void dispose()
			{
				if(this.childPredicateList != null)
				{
					for(NodeSelectorPredicate childPredicate : this.childPredicateList)
					{
						childPredicate.dispose();
					}
					this.childPredicateList.clear();
				}
				if(this.leafNodePredicateList != null)
				{
					for(LeafNodePredicate<?,?> leafNodePredicate : this.leafNodePredicateList)
					{
						leafNodePredicate.dispose();
					}
					this.leafNodePredicateList.clear();
				}
				if(this.pathPredicateList != null)
				{
					for(PathPredicate<?,?> pathPredicate : this.pathPredicateList)
					{
						pathPredicate.dispose();
					}
					this.pathPredicateList.clear();
				}
				
				this.parent = null;
				this.logicalOperator = null;
				this.childPredicateList = null;
				this.leafNodePredicateList = null;
				this.pathPredicateList = null;
			}
			@Override
			public int hashCode()
			{
				final int prime = 31;
				int result = 1;
				result = prime * result + ((childPredicateList == null) ? 0 : childPredicateList.hashCode());
				result = prime * result + (invert ? 1231 : 1237);
				result = prime * result + ((leafNodePredicateList == null) ? 0 : leafNodePredicateList.hashCode());
				result = prime * result + ((pathPredicateList == null) ? 0 : pathPredicateList.hashCode());
				result = prime * result + ((logicalOperator == null) ? 0 : logicalOperator.hashCode());
				result = prime * result + ((defaultMetaInstance == null) ? 0 : defaultMetaInstance.hashCode());
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
				NodeSelectorPredicate other = (NodeSelectorPredicate) obj;
				if (childPredicateList == null)
				{
					if (other.childPredicateList != null)
						return false;
				} else if (!childPredicateList.equals(other.childPredicateList))
					return false;
				if (invert != other.invert)
					return false;
				if (leafNodePredicateList == null)
				{
					if (other.leafNodePredicateList != null)
						return false;
				} else if (!leafNodePredicateList.equals(other.leafNodePredicateList))
					return false;
				if (pathPredicateList == null)
				{
					if (other.pathPredicateList != null)
						return false;
				} else if (!pathPredicateList.equals(other.pathPredicateList))
					return false;
				if (logicalOperator != other.logicalOperator)
					return false;
				if (defaultMetaInstance != other.defaultMetaInstance)
					return false;
				return true;
			}
			
			public NodeSelectorPredicate clone(NodeSelectorPredicate clonedParent)
			{
				NodeSelectorPredicate clonedPredicate = new NodeSelectorPredicate(this.defaultMetaInstance, clonedParent, this.logicalOperator, this.invert);
				
				if(this.leafNodePredicateList != null)
				{
					clonedPredicate.leafNodePredicateList = new ArrayList<LeafNodePredicate<?, ?>>();
					for(LeafNodePredicate<?, ?> leafNodePredicate : this.leafNodePredicateList)
					{
						clonedPredicate.leafNodePredicateList.add(leafNodePredicate.clone());
					}
				}
				
				if(this.pathPredicateList != null)
				{
					clonedPredicate.pathPredicateList = new ArrayList<PathPredicate<?, ?>>();
					for(PathPredicate<?, ?> pathPredicate : this.pathPredicateList)
					{
						clonedPredicate.pathPredicateList.add(pathPredicate.clone());
					}
				}
				
				if(this.childPredicateList != null)
				{
					clonedPredicate.childPredicateList = new ArrayList<NodeSelectorPredicate>();
					for(NodeSelectorPredicate pathPredicate : this.childPredicateList)
					{
						clonedPredicate.childPredicateList.add(pathPredicate.clone(clonedPredicate));
					}
				}
				
				return clonedPredicate;
			}
			
		}
		
		protected static class PathPredicate<N extends BranchNodeMetaModel,T>
		{
			protected PathPredicate(ModelPath<N,T> path,Predicate<T> predicate)
			{
				super();
				this.path = path;
				this.predicate = predicate;
			}
			
			private ModelPath<N,T> path = null;
			private Predicate<T> predicate = null;
			
			protected ModelPath<N, T> getPath()
			{
				return path;
			}
			protected Predicate<T> getPredicate()
			{
				return predicate;
			}
			
			protected void dispose()
			{
				if(this.path != null)
				{
					this.path.dispose();
				}
				this.path = null;
				this.predicate = null;
			}
			@Override
			public int hashCode()
			{
				final int prime = 31;
				int result = 1;
				result = prime * result + ((path == null) ? 0 : path.hashCode());
				result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
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
				PathPredicate other = (PathPredicate) obj;
				if (path == null)
				{
					if (other.path != null)
						return false;
				} else if (!path.equals(other.path))
					return false;
				if (predicate == null)
				{
					if (other.predicate != null)
						return false;
				} else if (!predicate.equals(other.predicate))
					return false;
				return true;
			}
			
			@Override
			protected PathPredicate<N,T> clone()
			{
				return new PathPredicate(this.path.clone(), this.predicate);
			}
		}
		
		protected static class LeafNodePredicate<N extends BranchNodeMetaModel,T>
		{
			protected LeafNodePredicate(LeafNodeType<N, T> field, Predicate<T> predicate)
			{
				super();
				this.field = field;
				this.predicate = predicate;
			}
			
			private LeafNodeType<N, T> field = null;
			private Predicate<T> predicate = null;
			
			protected LeafNodeType<N, T> getField()
			{
				return field;
			}
			protected Predicate<T> getPredicate()
			{
				return predicate;
			}
			
			protected void dispose()
			{
				this.field = null;
				this.predicate = null;
			}
			
			@Override
			public int hashCode()
			{
				final int prime = 31;
				int result = 1;
				result = prime * result + ((field == null) ? 0 : field.hashCode());
				result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
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
				LeafNodePredicate other = (LeafNodePredicate) obj;
				if (field == null)
				{
					if (other.field != null)
						return false;
				} else if (!field.equals(other.field))
					return false;
				if (predicate == null)
				{
					if (other.predicate != null)
						return false;
				} else if (!predicate.equals(other.predicate))
					return false;
				return true;
			}
			
			@Override
			protected LeafNodePredicate<N,T> clone()
			{
				return new LeafNodePredicate(this.field, this.predicate);
			}
		}
		
	}
	
}
