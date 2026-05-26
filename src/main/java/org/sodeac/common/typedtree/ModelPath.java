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
 * @param <R> type of start node
 * @param <T> type of nodes or node's value to select
 *
 * @author Sebastian Palarus
 */
public class ModelPath<R extends BranchNodeMetaModel, T>
{
    private LinkedList<NodeSelector<?, ?>> selectorList = new LinkedList<>();
    private Class<T> clazz = null;
    private boolean indisposable = false;
    
    private ModelPath()
    {
        super();
    }
    
    protected ModelPath(final NodeSelector<R, T> lastSelector)
    {
        super();
        
        if (lastSelector.getType() == null)
        {
            this.clazz = (Class<T>) lastSelector.getRootType().getClass();
        }
        else
        {
            if (lastSelector.getAxis() == Axis.VALUE)
            {
                this.clazz = (Class<T>) lastSelector.getType().getTypeClass();
            }
            else
            {
                this.clazz = (Class<T>) lastSelector.getType().getClass();
            }
        }
        
        NodeSelector<?, ?> current = lastSelector;
        while (current != null)
        {
            this.selectorList.addFirst(current);
            current = current.getParentSelector();
        }
    }
    
    protected Class<T> getClazz()
    {
        return this.clazz;
    }
    
    protected LinkedList<NodeSelector<?, ?>> getNodeSelectorList()
    {
        return this.selectorList;
    }
    
    public boolean isIndisposable()
    {
        return this.indisposable;
    }
    
    public ModelPath<R, T> setIndisposable()
    {
        this.indisposable = true;
        return this;
    }
    
    public void dispose()
    {
        if (this.indisposable)
        {
            return;
        }
        for (final NodeSelector<?, ?> selector : this.selectorList)
        {
            selector.dispose();
        }
        this.selectorList.clear();
        this.selectorList = null;
        this.clazz = null;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.selectorList == null) ? 0 : this.selectorList.hashCode());
        result = prime * result + ((this.clazz == null) ? 0 : this.clazz.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ModelPath other = (ModelPath) obj;
        if (this.selectorList == null)
        {
            if (other.selectorList != null)
            {
                return false;
            }
        }
        else if (!this.selectorList.equals(other.selectorList))
        {
            return false;
        }
        if (this.clazz == null)
        {
            return other.clazz == null;
        }
        else
        {
            return this.clazz.equals(other.clazz);
        }
    }
    
    @Override
    public ModelPath<R, T> clone()
    {
        ModelPath<R, T> clonedModelPath = new ModelPath<R, T>();
        if (this.selectorList != null)
        {
            clonedModelPath.selectorList = new LinkedList<NodeSelector<?, ?>>();
            
            NodeSelector previewsNodeSelector = null;
            for (final NodeSelector nodeSelector : this.selectorList)
            {
                NodeSelector<R, T> clonedNodeSelector = new NodeSelector<>();
                clonedNodeSelector.root = nodeSelector.root;
                clonedNodeSelector.parentSelector = previewsNodeSelector;
                clonedNodeSelector.type = nodeSelector.type;
                clonedNodeSelector.axis = nodeSelector.axis;
                clonedNodeSelector.predicate = nodeSelector.predicate;
                
                if (nodeSelector.childSelectorList != null)
                {
                    clonedNodeSelector.childSelectorList = new HashSet<NodeSelector<?, ?>>();
                }
                
                if ((previewsNodeSelector != null) && (previewsNodeSelector.childSelectorList != null))
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
    
    public static class ModelPathBuilder<R extends BranchNodeMetaModel, S extends BranchNodeMetaModel>
    {
        private BranchNodeMetaModel root = null;
        private BranchNodeMetaModel self = null;
        private NodeSelector<?, ?> selector = null;
        
        /**
         * Create new model path builder.
         *
         * @param rootClass type off root node in path (start node)
         *
         * @return builder
         */
        public static <R extends BranchNodeMetaModel> RootModelPathBuilder<R> newBuilder(final Class<R> rootClass)
        {
            try
            {
                return new RootModelPathBuilder<>(ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(rootClass));
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        
        /**
         * Create new model path builder.
         *
         * @param root root node of path (start node)
         *
         * @return builder
         */
        public static <R extends BranchNodeMetaModel> RootModelPathBuilder<R> newBuilder(final R root)
        {
            return new RootModelPathBuilder<>(root);
        }
        
        private ModelPathBuilder(final BranchNodeMetaModel root)
        {
            super();
            this.root = root;
            this.self = root;
            this.selector = new NodeSelector<>(root);
        }
        
        private ModelPathBuilder(final BranchNodeMetaModel root, final BranchNodeType field, final NodeSelector<?, ?> previews)
        {
            super();
            this.root = root;
            this.self = field.getValueDefaultInstance();
            this.selector = new NodeSelector<>(this.root, previews, field, NodeSelector.Axis.CHILD);
        }
        
        /**
         * Definition to navigate to next child node.
         *
         * @param field static child node type instance from meta model
         *
         * @return builder
         */
        public <N extends BranchNodeMetaModel> ModelPathBuilder<R, N> child(final BranchNodeType<S, N> field)
        {
            if ((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
            {
                throw new RuntimeException(getClass() + ": child already exists ");
            }
            return new ModelPathBuilder<R, N>(this.root, field, this.selector);
        }
        
        public <N extends BranchNodeMetaModel> BranchNodePredicateBuilder<R, N> childWithPredicates(final BranchNodeType<S, N> field) // TODO BranchNodeListType
        {
            if ((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
            {
                throw new RuntimeException(getClass() + ": child already exists ");
            }
            ModelPathBuilder<R, N> builder = new ModelPathBuilder<R, N>(this.root, field, this.selector);
            return new BranchNodePredicateBuilder(this.self, builder);
        }
        
        public <T> ModelPath<R, T> buildForValue(final LeafNodeType<S, T> field)
        {
            if ((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
            {
                throw new RuntimeException(getClass() + ": child already exists ");
            }
            return new ModelPath(new NodeSelector<R, T>(this.root, this.selector, field, NodeSelector.Axis.VALUE));
        }
        
        public <T> ModelPath<R, T> build()
        {
            return new ModelPath(this.selector);
        }
        
        public <T> ModelPath<R, LeafNode<?, T>> buildForNode(final LeafNodeType<S, T> field)
        {
            if ((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
            {
                throw new RuntimeException(getClass() + ": child already exists ");
            }
            return new ModelPath(new NodeSelector<R, LeafNode<?, T>>(this.root, this.selector, field, NodeSelector.Axis.CHILD));
        }
        
        public <T extends BranchNodeMetaModel> ModelPath<R, BranchNode<S, T>> buildForNode(final BranchNodeType<S, T> field)
        {
            if ((this.selector.childSelectorList != null) && (!this.selector.childSelectorList.isEmpty()))
            {
                throw new RuntimeException(getClass() + ": child already exists ");
            }
            return new ModelPath(new NodeSelector<R, BranchNode<S, T>>(this.root, this.selector, field, NodeSelector.Axis.CHILD));
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
         * @param <R> type off root node in path
         *
         * @author Sebastian Palarus
         */
        public static class RootModelPathBuilder<R extends BranchNodeMetaModel> extends ModelPathBuilder<R, R>
        {
            private RootModelPathBuilder(final BranchNodeMetaModel root)
            {
                super(root);
            }
            
            public BranchNodePredicateBuilder<R, R> childWithPredicates()
            {
                return new BranchNodePredicateBuilder(super.getSelf(), this);
            }
        }
        
        public static class BranchNodePredicateBuilder<R extends BranchNodeMetaModel, N extends BranchNodeMetaModel>
        {
            private ModelPathBuilder<R, N> builder = null;
            private NodeSelectorPredicate<N> rootPedicate = null;
            private NodeSelectorPredicate<N> currentPredicate = null;
            private N defaultModelInstance = null;
            
            private BranchNodePredicateBuilder(final N defaultModelInstance, final ModelPathBuilder<R, N> builder)
            {
                super();
                this.builder = builder;
                this.defaultModelInstance = defaultModelInstance;
                this.rootPedicate = new NodeSelectorPredicate(defaultModelInstance, null, LogicalOperator.AND, false);
                this.currentPredicate = this.rootPedicate;
            }
            
            public <T> BranchNodePredicateBuilder<R, N> addLeafNodePredicate(final LeafNodeType<N, T> field, final Predicate<T> predicate)
            {
                this.currentPredicate.addLeafNodePredicate(field, predicate);
                return this;
            }
            
            public <T> BranchNodePredicateBuilder<R, N> addPathPredicate(final Function<RootModelPathBuilder<N>, ModelPath<N, T>> pathBuilderFunction, final Predicate<T> predicate)
            {
                this.currentPredicate.addPathPredicate((Function) pathBuilderFunction, predicate);
                return this;
            }
            
            public BranchNodePredicateBuilder<R, N> and()
            {
                this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance, this.currentPredicate, LogicalOperator.AND, false);
                return this;
            }
            
            public BranchNodePredicateBuilder<R, N> andNot()
            {
                this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance, this.currentPredicate, LogicalOperator.AND, true);
                return this;
            }
            
            public BranchNodePredicateBuilder<R, N> or()
            {
                this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance, this.currentPredicate, LogicalOperator.OR, false);
                return this;
            }
            
            public BranchNodePredicateBuilder<R, N> orNot()
            {
                this.currentPredicate = new NodeSelectorPredicate(this.defaultModelInstance, this.currentPredicate, LogicalOperator.OR, true);
                return this;
            }
            
            public BranchNodePredicateBuilder<R, N> close()
            {
                this.currentPredicate = this.currentPredicate.getParent();
                Objects.requireNonNull(this.currentPredicate, "No child predicate to close");
                return this;
            }
            
            public ModelPathBuilder<R, N> build()
            {
                this.builder.selector.setPredicate(this.rootPedicate);
                return this.builder;
            }
        }
    }
    
    protected static class NodeSelector<R extends BranchNodeMetaModel, T>
    {
        protected enum Axis
        {SELF, CHILD, VALUE}
        
        private NodeSelector<?, ?> parentSelector = null;
        private Set<NodeSelector<?, ?>> childSelectorList = null;
        private Set<IModifyListener<?>> modifyListenerList = null;
        private Map<ConplierBean<Object>, Set<IModifyListener<?>>> registrationObjects = null;
        private NodeSelectorPredicate predicate = null;
        private INodeType<?, ?> type = null;
        private BranchNodeMetaModel root = null;
        private Axis axis = null;
        private volatile boolean disposed = false;
        
        protected NodeSelector(final BranchNodeMetaModel root)
        {
            super();
            this.root = root;
            this.axis = Axis.SELF;
        }
        
        protected NodeSelector<R, T> clone(final NodeSelector<?, ?> clonedParentSelector, final boolean deep)
        {
            NodeSelector<R, T> clonedNodeSelector = new NodeSelector<>();
            clonedNodeSelector.root = this.root;
            clonedNodeSelector.parentSelector = clonedParentSelector;
            clonedNodeSelector.type = this.type;
            clonedNodeSelector.axis = this.axis;
            if ((this.childSelectorList != null) && deep)
            {
                clonedNodeSelector.childSelectorList = new HashSet<NodeSelector<?, ?>>();
                for (final NodeSelector<?, ?> child : this.childSelectorList)
                {
                    clonedNodeSelector.childSelectorList.add(child.clone(clonedNodeSelector, true));
                }
            }
            clonedNodeSelector.predicate = this.predicate;
            
            return clonedNodeSelector;
        }
        
        private NodeSelector()
        {
            super();
        }
        
        protected NodeSelector(final BranchNodeMetaModel root, final NodeSelector<?, ?> previousSelector, final INodeType<?, ?> type, Axis axis)
        {
            super();
            this.root = root;
            this.parentSelector = previousSelector;
            this.type = type;
            if (previousSelector.childSelectorList == null)
            {
                previousSelector.childSelectorList = new HashSet<NodeSelector<?, ?>>();
            }
            previousSelector.childSelectorList.add(this);
            if (axis == null)
            {
                axis = Axis.CHILD;
            }
            this.axis = axis;
        }
        
        protected NodeSelector<?, ?> getParentSelector()
        {
            return this.parentSelector;
        }
        
        protected NodeSelector<?, ?> setParentSelector(final NodeSelector<?, ?> parentSelector)
        {
            this.parentSelector = parentSelector;
            return this;
        }
        
        protected Set<NodeSelector<?, ?>> getChildSelectorList()
        {
            return this.childSelectorList;
        }
        
        protected void setChildSelectorList(final Set<NodeSelector<?, ?>> childSelectorList)
        {
            this.childSelectorList = childSelectorList;
        }
        
        protected NodeSelector<?, ?> setPredicate(final NodeSelectorPredicate predicate)
        {
            this.predicate = predicate;
            return this;
        }
        
        protected NodeSelectorPredicate getPredicate()
        {
            return this.predicate;
        }
        
        protected INodeType<?, ?> getType()
        {
            return this.type;
        }
        
        protected BranchNodeMetaModel getRootType()
        {
            return this.root;
        }
        
        protected void setRootType(final BranchNodeMetaModel root)
        {
            this.root = root;
        }
        
        protected Axis getAxis()
        {
            return this.axis;
        }
        
        protected Set<IModifyListener<?>> getModifyListenerList()
        {
            return this.modifyListenerList;
        }
        
        protected void setModifyListenerList(final Set<IModifyListener<?>> modifyListenerList)
        {
            this.modifyListenerList = modifyListenerList;
        }
        
        protected Map<ConplierBean<Object>, Set<IModifyListener<?>>> getRegistrationObjects()
        {
            return this.registrationObjects;
        }
        
        protected void setRegistrationObjects(final Map<ConplierBean<Object>, Set<IModifyListener<?>>> registrationObjects)
        {
            this.registrationObjects = registrationObjects;
        }
        
        protected void dispose()
        {
            this.disposed = true;
            if (this.childSelectorList != null)
            {
                for (final NodeSelector<?, ?> child : this.childSelectorList)
                {
                    child.dispose();
                }
                this.childSelectorList.clear();
            }
            
            if (this.registrationObjects != null)
            {
                for (final Set<IModifyListener<?>> listener : this.registrationObjects.values())
                {
                    if (listener == null)
                    {
                        continue;
                    }
                    listener.clear();
                }
                this.registrationObjects.clear();
            }
            
            if (this.modifyListenerList != null)
            {
                this.modifyListenerList.clear();
            }
            
            if (this.predicate != null)
            {
                this.predicate.dispose();
            }
            this.parentSelector = null;
            this.childSelectorList = null;
            this.predicate = null;
            this.registrationObjects = null;
            this.type = null;
            this.root = null;
            this.axis = null;
        }
        
        protected boolean isDisposed()
        {
            return this.disposed;
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.axis == null) ? 0 : this.axis.hashCode());
            result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
            result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            NodeSelector other = (NodeSelector) obj;
            if (this.axis != other.axis)
            {
                return false;
            }
            if (this.predicate == null)
            {
                if (other.predicate != null)
                {
                    return false;
                }
            }
            else if (!this.predicate.equals(other.predicate))
            {
                return false;
            }
            if (this.type == null)
            {
                return other.type == null;
            }
            else
            {
                return this.type.equals(other.type);
            }
        }
        
        protected static class NodeSelectorPredicate<T extends BranchNodeMetaModel>
        {
            protected NodeSelectorPredicate(final T defaultMetaInstance, final NodeSelectorPredicate parent, final LogicalOperator logicalOperator, final boolean invert)
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
            private final T defaultMetaInstance = null;
            
            protected LogicalOperator getLogicalOperator()
            {
                return this.logicalOperator;
            }
            
            protected void setLogicalOperator(final LogicalOperator logicalOperator)
            {
                this.logicalOperator = logicalOperator;
            }
            
            protected boolean isInvert()
            {
                return this.invert;
            }
            
            protected void setInvert(final boolean invert)
            {
                this.invert = invert;
            }
            
            protected NodeSelectorPredicate getParent()
            {
                return this.parent;
            }
            
            protected NodeSelectorPredicate addLeafNodePredicate(final LeafNodeType<T, ?> field, final Predicate<?> predicate)
            {
                if (this.leafNodePredicateList == null)
                {
                    this.leafNodePredicateList = new ArrayList<NodeSelector.LeafNodePredicate<T, ?>>();
                }
                this.leafNodePredicateList.add(new LeafNodePredicate(field, predicate));
                return this;
            }
            
            protected NodeSelectorPredicate addPathPredicate(final Function<RootModelPathBuilder<T>, ModelPath<T, ?>> pathBuilderFunction, final Predicate<?> predicate)
            {
                Objects.requireNonNull(pathBuilderFunction, "path builder function is null");
                if (this.pathPredicateList == null)
                {
                    this.pathPredicateList = new ArrayList<NodeSelector.PathPredicate<T, ?>>();
                }
                RootModelPathBuilder<T> builder = ModelPathBuilder.newBuilder(this.defaultMetaInstance);
                ModelPath<T, ?> path = pathBuilderFunction.apply(builder);
                if (path != null)
                {
                    this.pathPredicateList.add(new PathPredicate(path, predicate));
                }
                return this;
            }
            
            protected NodeSelectorPredicate addChildPredicate(final NodeSelectorPredicate childPredicate)
            {
                if (this.childPredicateList == null)
                {
                    this.childPredicateList = new ArrayList<NodeSelectorPredicate>();
                }
                this.childPredicateList.add(childPredicate);
                return this;
            }
            
            protected void dispose()
            {
                if (this.childPredicateList != null)
                {
                    for (final NodeSelectorPredicate childPredicate : this.childPredicateList)
                    {
                        childPredicate.dispose();
                    }
                    this.childPredicateList.clear();
                }
                if (this.leafNodePredicateList != null)
                {
                    for (final LeafNodePredicate<?, ?> leafNodePredicate : this.leafNodePredicateList)
                    {
                        leafNodePredicate.dispose();
                    }
                    this.leafNodePredicateList.clear();
                }
                if (this.pathPredicateList != null)
                {
                    for (final PathPredicate<?, ?> pathPredicate : this.pathPredicateList)
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
                result = prime * result + ((this.childPredicateList == null) ? 0 : this.childPredicateList.hashCode());
                result = prime * result + (this.invert ? 1231 : 1237);
                result = prime * result + ((this.leafNodePredicateList == null) ? 0 : this.leafNodePredicateList.hashCode());
                result = prime * result + ((this.pathPredicateList == null) ? 0 : this.pathPredicateList.hashCode());
                result = prime * result + ((this.logicalOperator == null) ? 0 : this.logicalOperator.hashCode());
                result = prime * result + ((this.defaultMetaInstance == null) ? 0 : this.defaultMetaInstance.hashCode());
                return result;
            }
            
            @Override
            public boolean equals(final Object obj)
            {
                if (this == obj)
                {
                    return true;
                }
                if (obj == null)
                {
                    return false;
                }
                if (getClass() != obj.getClass())
                {
                    return false;
                }
                NodeSelectorPredicate other = (NodeSelectorPredicate) obj;
                if (this.childPredicateList == null)
                {
                    if (other.childPredicateList != null)
                    {
                        return false;
                    }
                }
                else if (!this.childPredicateList.equals(other.childPredicateList))
                {
                    return false;
                }
                if (this.invert != other.invert)
                {
                    return false;
                }
                if (this.leafNodePredicateList == null)
                {
                    if (other.leafNodePredicateList != null)
                    {
                        return false;
                    }
                }
                else if (!this.leafNodePredicateList.equals(other.leafNodePredicateList))
                {
                    return false;
                }
                if (this.pathPredicateList == null)
                {
                    if (other.pathPredicateList != null)
                    {
                        return false;
                    }
                }
                else if (!this.pathPredicateList.equals(other.pathPredicateList))
                {
                    return false;
                }
                if (this.logicalOperator != other.logicalOperator)
                {
                    return false;
                }
                return this.defaultMetaInstance == other.defaultMetaInstance;
            }
            
            public NodeSelectorPredicate clone(final NodeSelectorPredicate clonedParent)
            {
                NodeSelectorPredicate clonedPredicate = new NodeSelectorPredicate(this.defaultMetaInstance, clonedParent, this.logicalOperator, this.invert);
                
                if (this.leafNodePredicateList != null)
                {
                    clonedPredicate.leafNodePredicateList = new ArrayList<LeafNodePredicate<?, ?>>();
                    for (final LeafNodePredicate<?, ?> leafNodePredicate : this.leafNodePredicateList)
                    {
                        clonedPredicate.leafNodePredicateList.add(leafNodePredicate.clone());
                    }
                }
                
                if (this.pathPredicateList != null)
                {
                    clonedPredicate.pathPredicateList = new ArrayList<PathPredicate<?, ?>>();
                    for (final PathPredicate<?, ?> pathPredicate : this.pathPredicateList)
                    {
                        clonedPredicate.pathPredicateList.add(pathPredicate.clone());
                    }
                }
                
                if (this.childPredicateList != null)
                {
                    clonedPredicate.childPredicateList = new ArrayList<NodeSelectorPredicate>();
                    for (final NodeSelectorPredicate pathPredicate : this.childPredicateList)
                    {
                        clonedPredicate.childPredicateList.add(pathPredicate.clone(clonedPredicate));
                    }
                }
                
                return clonedPredicate;
            }
            
        }
        
        protected static class PathPredicate<N extends BranchNodeMetaModel, T>
        {
            protected PathPredicate(final ModelPath<N, T> path, final Predicate<T> predicate)
            {
                super();
                this.path = path;
                this.predicate = predicate;
            }
            
            private ModelPath<N, T> path = null;
            private Predicate<T> predicate = null;
            
            protected ModelPath<N, T> getPath()
            {
                return this.path;
            }
            
            protected Predicate<T> getPredicate()
            {
                return this.predicate;
            }
            
            protected void dispose()
            {
                if (this.path != null)
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
                result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
                result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
                return result;
            }
            
            @Override
            public boolean equals(final Object obj)
            {
                if (this == obj)
                {
                    return true;
                }
                if (obj == null)
                {
                    return false;
                }
                if (getClass() != obj.getClass())
                {
                    return false;
                }
                PathPredicate other = (PathPredicate) obj;
                if (this.path == null)
                {
                    if (other.path != null)
                    {
                        return false;
                    }
                }
                else if (!this.path.equals(other.path))
                {
                    return false;
                }
                if (this.predicate == null)
                {
                    return other.predicate == null;
                }
                else
                {
                    return this.predicate.equals(other.predicate);
                }
            }
            
            @Override
            protected PathPredicate<N, T> clone()
            {
                return new PathPredicate(this.path.clone(), this.predicate);
            }
        }
        
        protected static class LeafNodePredicate<N extends BranchNodeMetaModel, T>
        {
            protected LeafNodePredicate(final LeafNodeType<N, T> field, final Predicate<T> predicate)
            {
                super();
                this.field = field;
                this.predicate = predicate;
            }
            
            private LeafNodeType<N, T> field = null;
            private Predicate<T> predicate = null;
            
            protected LeafNodeType<N, T> getField()
            {
                return this.field;
            }
            
            protected Predicate<T> getPredicate()
            {
                return this.predicate;
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
                result = prime * result + ((this.field == null) ? 0 : this.field.hashCode());
                result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
                return result;
            }
            
            @Override
            public boolean equals(final Object obj)
            {
                if (this == obj)
                {
                    return true;
                }
                if (obj == null)
                {
                    return false;
                }
                if (getClass() != obj.getClass())
                {
                    return false;
                }
                LeafNodePredicate other = (LeafNodePredicate) obj;
                if (this.field == null)
                {
                    if (other.field != null)
                    {
                        return false;
                    }
                }
                else if (!this.field.equals(other.field))
                {
                    return false;
                }
                if (this.predicate == null)
                {
                    return other.predicate == null;
                }
                else
                {
                    return this.predicate.equals(other.predicate);
                }
            }
            
            @Override
            protected LeafNodePredicate<N, T> clone()
            {
                return new LeafNodePredicate(this.field, this.predicate);
            }
        }
        
    }
    
}
