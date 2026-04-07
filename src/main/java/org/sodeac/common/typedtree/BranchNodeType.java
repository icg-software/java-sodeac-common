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

import java.lang.reflect.Field;

import org.sodeac.common.typedtree.annotation.Transient;

/**
 * A branch node type defines a single complex node as child node of parent node. This node type can defines new child nodes again.
 *
 * @param <P> type of parent node
 * @param <T> type of branch node
 *
 * @author Sebastian Palarus
 */
public class BranchNodeType<P extends BranchNodeMetaModel, T extends BranchNodeMetaModel> implements INodeType<P, T>
{
    private Class<T> typeClass = null;
    private Class<P> parentNodeClass = null;
    private String name = null;
    private Field field = null;
    private int hashCode = 1;
    private boolean transientFlag = false;
    
    /**
     * Constructor for branch node type.
     *
     * @param parentNodeClass class of parent node (should be a class of {@link BranchNodeMetaModel})
     * @param typeClass       type of child node (should be a class of {@link BranchNodeMetaModel})
     * @param field           referenced by field
     */
    public BranchNodeType(Class<P> parentNodeClass, Class<T> typeClass, Field field)
    {
        this.parentNodeClass = parentNodeClass;
        this.typeClass = typeClass;
        this.name = field.getName();
        this.field = field;
        this.transientFlag = field.getAnnotation(Transient.class) != null;
        
        // generate hashcode
        
        final int prime = 31;
        hashCode = prime * hashCode + ((field == null) ? 0 : field.hashCode());
        hashCode = prime * hashCode + ((name == null) ? 0 : name.hashCode());
        hashCode = prime * hashCode + ((parentNodeClass == null) ? 0 : parentNodeClass.hashCode());
        hashCode = prime * hashCode + ((typeClass == null) ? 0 : typeClass.hashCode());
    }
    
    @Override
    public Class<T> getTypeClass()
    {
        return typeClass;
    }
    
    @Override
    public Class<P> getParentNodeClass()
    {
        return parentNodeClass;
    }
    
    @Override
    public String getNodeName()
    {
        return this.name;
    }
    
    @Override
    public T getValueDefaultInstance()
    {
        return ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(getTypeClass());
    }
    
    @Override
    public int hashCode()
    {
        return this.hashCode;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return (this == obj);
    }
    
    @Override
    public Field referencedByField()
    {
        return this.field;
    }
    
    /**
     * returns class of {@link BranchNode}
     *
     * @return branch node class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<BranchNode<P, T>> getBranchNodeClass()
    {
        return (Class) BranchNode.class;
    }
    
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " " + this.getNodeName();
    }
    
    @Override
    public boolean isTransient()
    {
        return transientFlag;
    }
}
