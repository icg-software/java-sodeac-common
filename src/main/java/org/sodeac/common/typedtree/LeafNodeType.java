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
 * A leaf node type defines a simple child node. This kind of child type can not include other child nodes. But for serialization reasons it is recommend to restrict the type of node to one of the following list:
 *
 * <br>{@link java.lang.Character}
 * <br>{@link java.lang.String}
 * <br>{@link java.lang.Boolean}
 * <br>{@link java.lang.Byte}
 * <br>{@link java.lang.Short}
 * <br>{@link java.lang.Integer}
 * <br>{@link java.lang.Long}
 * <br>{@link java.lang.Float}
 * <br>{@link java.lang.Double}
 * <br>{@link java.util.UUID}
 * <br>{@link java.util.Date}
 * <br>{@link java.nio.ByteBuffer}
 * <br>array of bytes
 *
 * @param <P> type of parent node
 * @param <T> type of leaf node
 *
 * @author Sebastian Palarus
 */
public class LeafNodeType<P extends BranchNodeMetaModel, T> implements INodeType<P, T>
{
    private Class<T> typeClass = null;
    private Class<P> parentNodeClass = null;
    private String name = null;
    private Field field = null;
    private int hashCode = 1;
    private boolean transientFlag = false;
    
    /**
     * Constructor for leaf node type.
     *
     * @param parentNodeClass class of parent node (should be a class of {@link BranchNodeMetaModel})
     * @param typeClass       type of child node's value
     * @param field           referenced by field
     */
    public LeafNodeType(final Class<P> parentNodeClass, final Class<T> typeClass, final Field field)
    {
        this.parentNodeClass = parentNodeClass;
        this.typeClass = typeClass;
        this.name = field.getName();
        this.field = field;
        this.transientFlag = field.getAnnotation(Transient.class) != null;
        
        // generate hashcode
        
        final int prime = 31;
        this.hashCode = prime * this.hashCode + ((field == null) ? 0 : field.hashCode());
        this.hashCode = prime * this.hashCode + ((this.name == null) ? 0 : this.name.hashCode());
        this.hashCode = prime * this.hashCode + ((parentNodeClass == null) ? 0 : parentNodeClass.hashCode());
        this.hashCode = prime * this.hashCode + ((typeClass == null) ? 0 : typeClass.hashCode());
    }
    
    @Override
    public Class<T> getTypeClass()
    {
        return this.typeClass;
    }
    
    @Override
    public Class<P> getParentNodeClass()
    {
        return this.parentNodeClass;
    }
    
    @Override
    public String getNodeName()
    {
        return this.name;
    }
    
    @Override
    public Field referencedByField()
    {
        return this.field;
    }
    
    @Override
    public int hashCode()
    {
        return this.hashCode;
    }
    
    @Override
    public boolean equals(final Object obj)
    {
        return this == obj;
    }
    
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " " + this.getNodeName();
    }
    
    @Override
    public boolean isTransient()
    {
        return this.transientFlag;
    }
}
