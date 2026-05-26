/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.typedtree;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

public abstract class BranchNodeToObjectWrapper implements Externalizable
{
    protected BranchNodeToObjectWrapper(final BranchNode<?, ? extends BranchNodeMetaModel> branchNode, final BranchNodeToObjectWrapper parent)
    {
        super();
        Objects.requireNonNull(branchNode, "BaseObjectWrapper requires a node to wrap");
        
        if (branchNode.getBow() != null)
        {
            throw new IllegalStateException("BranchNode is already linked with BranchNode-to-Object-Wrapper");
        }
        
        if ((branchNode instanceof RootBranchNode))
        {
            if (parent != null)
            {
                throw new IllegalStateException("Parent-Pow must be null for RootBranchNodes");
            }
            if (((RootBranchNode) branchNode).hasChilds())
            {
                throw new IllegalStateException("BranchNode has childs");
            }
        }
        else if (branchNode.getParentNode().getBow() != parent)
        {
            throw new IllegalStateException("Parent wrapper doesn't figure to parent tree node");
        }
        
        this.__branchNode = branchNode;
        this.__parent = parent;
        
        branchNode.setBow(this);
        
    }
    
    protected BranchNode<?, ? extends BranchNodeMetaModel> __branchNode = null;
    protected BranchNodeToObjectWrapper __parent = null;
    
    protected BranchNodeMetaModel getModel()
    {
        return this.__branchNode.getModel();
    }
    
    public <P extends BranchNodeToObjectWrapper> P getTypedParent(final Class<P> clazz)
    {
        return (P) this.__parent;
    }
    
    public BranchNode getWrappedBranchNode()
    {
        return this.__branchNode;
    }
    
    protected <M extends BranchNodeMetaModel> BranchNodeMetaModel getModel(final Class<M> clazz)
    {
        return ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(clazz);
    }
    
    protected Object getLeafNodeValue(final NodeField nodeField)
    {
        return this.__branchNode.getLeafNodeValue(nodeField.nodeTypeIndex, (LeafNodeType) nodeField.nodeType);
    }
    
    protected void setLeafNodeValue(final NodeField nodeField, final Object value)
    {
        this.__branchNode.setLeafNodeValue(nodeField.nodeTypeIndex, (LeafNodeType) nodeField.nodeType, value);
    }
    
    protected BranchNode createBranchNode(final NodeField nodeField)
    {
        return this.__branchNode.create(nodeField.nodeTypeIndex, (BranchNodeType) nodeField.nodeType, null);
    }
    
    protected BranchNode getBranchNode(final NodeField nodeField)
    {
        return this.__branchNode.get(nodeField.nodeTypeIndex, (BranchNodeType) nodeField.nodeType);
    }
    
    protected BranchNode createBranchNodeItem(final NodeField nodeField)
    {
        return this.__branchNode.create(nodeField.nodeTypeIndex, (BranchNodeListType) nodeField.nodeType);
    }
    
    protected List<BranchNodeToObjectWrapper> getBowList(final NodeField nodeField)
    {
        return this.__branchNode.getUnmodifiableBowList(nodeField.nodeTypeIndex, (BranchNodeListType) nodeField.nodeType);
    }
    
    protected Stream<BranchNodeToObjectWrapper> getBowStream(final NodeField nodeField)
    {
        return this.__branchNode.getUnmodifiableNodeList(nodeField.nodeTypeIndex, (BranchNodeListType) nodeField.nodeType).stream().map(BranchNode.FnBowFromBranchNode);
    }
    
    protected boolean removeBranchNodeItem(final NodeField nodeField, final BranchNodeToObjectWrapper nestedBow)
    {
        if ((nestedBow == null) || (nestedBow.getWrappedBranchNode() == null))
        {
            return false;
        }
        return this.__branchNode.remove(nodeField.nodeTypeIndex, (BranchNodeListType) nodeField.nodeType, nestedBow.getWrappedBranchNode());
    }
    
    protected BranchNodeToObjectWrapper createNestedBow(final int nodeTypeIndex, final INodeType nodeType, final BranchNode branchNode)
    {
        return null;
    }
    
    protected void dispose()
    {
        this.__branchNode = null;
        this.__parent = null;
    }
    
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
    {
        // TODO Auto-generated method stub
        
    }
    
    public static class NestedPowFactoryCache
    {
        public NestedPowFactoryCache(final NodeField nodeField, final BiFunction<BranchNode, BranchNodeToObjectWrapper, BranchNodeToObjectWrapper> factory)
        {
            super();
            this.nodeField = nodeField;
            this.factory = factory;
        }
        
        private final NodeField nodeField;
        private final BiFunction<BranchNode, BranchNodeToObjectWrapper, BranchNodeToObjectWrapper> factory;
        
        public NodeField getNodeField()
        {
            return this.nodeField;
        }
        
        public BiFunction<BranchNode, BranchNodeToObjectWrapper, BranchNodeToObjectWrapper> getFactory()
        {
            return this.factory;
        }
        
        public NestedPowFactoryCache copy()
        {
            return new NestedPowFactoryCache(this.nodeField, this.factory);
        }
    }
    
    public static class NodeField
    {
        
        public NodeField(final int nodeTypeIndex, final INodeType nodeType)
        {
            super();
            this.nodeTypeIndex = nodeTypeIndex;
            this.nodeType = nodeType;
        }
        
        private final int nodeTypeIndex;
        private final INodeType nodeType;
        
        public int getNodeTypeIndex()
        {
            return this.nodeTypeIndex;
        }
        
        public INodeType getNodeType()
        {
            return this.nodeType;
        }
        
        public NodeField copy()
        {
            return new NodeField(this.nodeTypeIndex, this.nodeType);
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.nodeType == null) ? 0 : this.nodeType.hashCode());
            result = prime * result + this.nodeTypeIndex;
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
            NodeField other = (NodeField) obj;
            if (this.nodeType == null)
            {
                if (other.nodeType != null)
                {
                    return false;
                }
            }
            else if (!this.nodeType.equals(other.nodeType))
            {
                return false;
            }
            return this.nodeTypeIndex == other.nodeTypeIndex;
        }
    }
}
