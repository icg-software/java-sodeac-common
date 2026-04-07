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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

/**
 * A typed tree meta model defines all root nodes (trees) of this model.
 *
 * @param <T>
 *
 * @author Sebastian Palarus
 */
public class TypedTreeMetaModel<T extends TypedTreeMetaModel> extends BranchNodeMetaModel
{
    {
        init();
    }
    
    /**
     * get cached model instanced by <code>modelClass</code>
     *
     * @param modelClass class of model instance
     *
     * @return model instance
     */
    public static <M extends TypedTreeMetaModel> M getInstance(final Class<M> modelClass)
    {
        return ModelRegistry.DEFAULT_INSTANCE.getCachedTypedTreeMetaModel(modelClass);
    }
    
    /**
     * initialize whole model
     */
    public void init()
    {
    }
    
    /**
     * Create new root node instance provided by model
     *
     * @param type static type instance defined in model
     *
     * @return new root node instance
     */
    public <F extends BranchNodeMetaModel> RootBranchNode<T, F> createRootNode(final BranchNodeType<T, F> type)
    {
        return new RootBranchNode(type, getClass());
    }
    
    public <F extends BranchNodeMetaModel> RootBranchNode<T, F> createRootNode(final Class<F> clazz)
    {
        TypedTreeModel typedTreeModel = clazz.getAnnotation(TypedTreeModel.class);
        Objects.toString(typedTreeModel, "@TypedTreeModel not defined for " + clazz);
        if (typedTreeModel.modelClass() != getClass())
        {
            throw new IllegalStateException(clazz + " not type of model " + getClass().getCanonicalName());
        }
        
        BranchNodeMetaModel defaultModelInstance = ModelRegistry.getBranchNodeMetaModel(clazz);
        
        if (defaultModelInstance.anonymous == null)
        {
            try
            {
                defaultModelInstance.anonymous = new BranchNodeType<>(getClass(), clazz, BranchNodeMetaModel.class.getDeclaredField("anonymous"));
            }
            catch (NoSuchFieldException | SecurityException e)
            {
                throw new RuntimeException(e);
            }
        }
        
        return new RootBranchNode(defaultModelInstance.anonymous, getClass());
    }
    
    public <F extends BranchNodeMetaModel> Class<RootBranchNode<T, F>> getRootBranchNodeClass(final BranchNodeType<T, F> branchNodeType)
    {
        return (Class) RootBranchNode.class;
    }
    
    /**
     * A root node represents a typed tree
     *
     * @param <P> type of parent model
     * @param <R> type of root node
     *
     * @author Sebastian Palarus
     */
    public static class RootBranchNode<P extends TypedTreeMetaModel, R extends BranchNodeMetaModel> extends BranchNode<P, R> implements ITree<P, R>
    {
        private boolean nodeSynchronized;
        private boolean immutable;
        private boolean branchNodeGetterAutoCreate;
        private boolean branchNodeConsumeAutoCreate;
        
        private CopyOnWriteArrayList<ITreeModifyListener> modifyListeners;
        private ReadLock readLock;
        private WriteLock writeLock;
        private ConplierBean<Boolean> sharedDoit;
        private volatile long sequnceOID = 0L;
        private volatile boolean disableAllListener = false;
        private Class<? extends TypedTreeMetaModel<?>> modelClass = null;
        private boolean childs = false;
        
        /**
         * Constructor of root node.
         *
         * @param type static type instance defined in model
         */
        protected RootBranchNode(final BranchNodeType<P, R> type, final Class<? extends TypedTreeMetaModel<?>> modelClass)
        {
            super(null, null, new NodeContainer(type, -1));
            
            this.modelClass = modelClass;
            this.nodeSynchronized = false;
            this.immutable = false;
            this.branchNodeGetterAutoCreate = false;
            this.branchNodeConsumeAutoCreate = false;
            
            this.modifyListeners = null;
            this.sharedDoit = new ConplierBean<Boolean>(true);
            
            ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
            this.readLock = lock.readLock();
            this.writeLock = lock.writeLock();
        }
        
        protected ReadLock getReadLock()
        {
            return this.readLock;
        }
        
        protected WriteLock getWriteLock()
        {
            return this.writeLock;
        }
        
        @Override
        protected void setHasChilds()
        {
            this.childs = true;
        }
        
        protected boolean hasChilds()
        {
            return this.childs;
        }
        
        @Override
        public boolean isDisableAllListener()
        {
            return this.disableAllListener;
        }
        
        @Override
        public RootBranchNode<P, R> setDisableAllListener(final boolean disableAllListener)
        {
            this.disableAllListener = disableAllListener;
            return this;
        }
        
        @Override
        public void dispose()
        {
            super.disposeNode();
            
            if (this.modifyListeners != null)
            {
                this.modifyListeners.clear();
            }
            this.readLock = null;
            this.writeLock = null;
            this.modifyListeners = null;
            this.sharedDoit = null;
            this.modelClass = null;
        }
        
        @Override
        public boolean isSynchronized()
        {
            return this.nodeSynchronized;
        }
        
        @Override
        public RootBranchNode<P, R> setSynchronized(final boolean nodeSynchronized)
        {
            this.nodeSynchronized = nodeSynchronized;
            return this;
        }
        
        @Override
        public boolean isImmutable()
        {
            return this.immutable;
        }
        
        @Override
        public RootBranchNode<P, R> setImmutable()
        {
            this.immutable = true;
            return this;
        }
        
        @Override
        public boolean isBranchNodeGetterAutoCreate()
        {
            return this.branchNodeGetterAutoCreate;
        }
        
        @Override
        public RootBranchNode<P, R> setBranchNodeGetterAutoCreate(final boolean branchNodeGetterAutoCreate)
        {
            this.branchNodeGetterAutoCreate = branchNodeGetterAutoCreate;
            return this;
        }
        
        @Override
        public boolean isBranchNodeApplyToConsumerAutoCreate()
        {
            return this.branchNodeConsumeAutoCreate;
        }
        
        @Override
        public RootBranchNode<P, R> setBranchNodeApplyToConsumerAutoCreate(final boolean branchNodeConsumeAutoCreate)
        {
            this.branchNodeConsumeAutoCreate = branchNodeConsumeAutoCreate;
            return this;
        }
        
        /**
         * Sequencer for tree's object-ID
         *
         * @return next objectId
         */
        protected long nextOID()
        {
            return ++this.sequnceOID;
        }
        
        @Override
        public RootBranchNode<P, R> addTreeModifyListener(final ITreeModifyListener modifyListener)
        {
            if (modifyListener == null)
            {
                return this;
            }
            if (this.modifyListeners == null)
            {
                this.modifyListeners = new CopyOnWriteArrayList<ITreeModifyListener>();
            }
            this.modifyListeners.addIfAbsent(modifyListener);
            return this;
        }
        
        @Override
        public RootBranchNode<P, R> addTreeModifyListeners(final ITreeModifyListener... modifyListeners)
        {
            if (modifyListeners == null)
            {
                return this;
            }
            if (modifyListeners.length == 0)
            {
                return this;
            }
            if (this.modifyListeners == null)
            {
                this.modifyListeners = new CopyOnWriteArrayList<ITreeModifyListener>();
            }
            boolean hasNullItem = false;
            for (final ITreeModifyListener modifyListener : modifyListeners)
            {
                if (modifyListener == null)
                {
                    hasNullItem = true;
                    break;
                }
            }
            if (hasNullItem)
            {
                List<ITreeModifyListener> modifyListenerList = new ArrayList<ITreeModifyListener>();
                for (final ITreeModifyListener modifyListener : modifyListeners)
                {
                    if (modifyListener != null)
                    {
                        modifyListenerList.add(modifyListener);
                    }
                }
                if (modifyListenerList.isEmpty())
                {
                    return this;
                }
                this.modifyListeners.addAllAbsent(modifyListenerList);
            }
            else
            {
                this.modifyListeners.addAllAbsent(Arrays.asList(modifyListeners));
            }
            return this;
        }
        
        @Override
        public RootBranchNode<P, R> removeTreeModifyListener(final ITreeModifyListener modifyListener)
        {
            if (modifyListener == null)
            {
                return this;
            }
            if (this.modifyListeners == null)
            {
                return this;
            }
            
            this.modifyListeners.remove(modifyListener);
            return this;
        }
        
        protected <C extends INodeType<?, ?>, T> boolean notifyBeforeModify(final BranchNode<?, ?> parentNode, final NodeContainer nodeContainer, final T oldValue, final T newValue)
        {
            if (this.disableAllListener)
            {
                return true;
            }
            ConplierBean<Boolean> doit = null;
            
            if ((this.modifyListeners != null) && (!this.modifyListeners.isEmpty()))
            {
                for (final ITreeModifyListener modifyListener : this.modifyListeners)
                {
                    if (doit == null)
                    {
                        doit = this.sharedDoit;
                        if (!isSynchronized())
                        {
                            doit = new ConplierBean<Boolean>(true); // TODO recycle
                        }
                    }
                    modifyListener.beforeModify(parentNode, nodeContainer.getNodeType(), oldValue, newValue, doit);
                    
                    if ((doit.get() != null) && (!doit.get().booleanValue()))
                    {
                        return false;
                    }
                }
            }
            
            return true;
        }
        
        protected <C extends INodeType<?, ?>, T> void notifyAfterModify(final BranchNode<?, ?> parentNode, final NodeContainer nodeContainer, final T oldValue, final T newValue)
        {
            if (this.disableAllListener)
            {
                return;
            }
            if ((this.modifyListeners != null) && (!this.modifyListeners.isEmpty()))
            {
                for (final ITreeModifyListener modifyListener : this.modifyListeners)
                {
                    modifyListener.afterModify(parentNode, nodeContainer.getNodeType(), oldValue, newValue);
                }
            }
            if (nodeContainer.getNodeListenerList() != null)
            {
                if (nodeContainer.getNodeType() instanceof LeafNodeType)
                {
                    for (final IChildNodeListener listener : nodeContainer.getNodeListenerList())
                    {
                        listener.accept(nodeContainer.getNode(), oldValue);
                    }
                }
                else // FIXME: remove this wrong else statement?
                {
                    for (final IChildNodeListener listener : nodeContainer.getNodeListenerList())
                    {
                        listener.accept(newValue, oldValue);
                    }
                }
                
            }
        }
        
        public XMLMarshaller getXMLMarshaller()
        {
            return ModelRegistry.getTypedTreeMetaModel(this.modelClass).getXMLMarshaller();
        }
    }
    
    private volatile XMLMarshaller xmlMarshaller = null;
    
    public XMLMarshaller getXMLMarshaller()
    {
        XMLMarshaller xmlMarshaller = this.xmlMarshaller;
        if (xmlMarshaller != null)
        {
            return xmlMarshaller;
        }
        
        xmlMarshaller = XMLMarshaller.getForTreeModel((Class<? extends TypedTreeMetaModel<?>>) this.getClass());
        this.xmlMarshaller = xmlMarshaller;
        return xmlMarshaller;
    }
}
