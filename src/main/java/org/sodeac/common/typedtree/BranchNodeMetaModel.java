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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A branch node meta model defines type and cardinality of child nodes.
 *
 * @author Sebastian Palarus
 *
 */
public class BranchNodeMetaModel
{
    protected BranchNodeMetaModel()
    {
        super();
        
        // create static instances of types
        
        List<INodeType> list = new ArrayList<INodeType>();
        
        LinkedList<Class> classList = new LinkedList<>();
        classList.add(getClass());
        
        Class<?> currentSuperClass = getClass().getSuperclass();
        
        while ((currentSuperClass != null) && (currentSuperClass != Object.class))
        {
            classList.addFirst(currentSuperClass);
            currentSuperClass = currentSuperClass.getSuperclass();
        }
        List<Field> fieldList = new ArrayList<>();
        
        Map<String, Set<LeafNodeType>> leafNodeIndex = new HashMap<>();
        Map<String, Set<BranchNodeType>> branchNodeIndex = new HashMap<>();
        Map<String, Set<BranchNodeListType>> branchNodeListIndex = new HashMap<>();
        
        for (final Class modelClass : classList)
        {
            for (final Field field : modelClass.getDeclaredFields())
            {
                INodeType staticNodeTypeInstance = getStaticFieldInstance(field);
                if (staticNodeTypeInstance == null)
                {
                    continue;
                }
                String name = staticNodeTypeInstance.getNodeName();
                if (staticNodeTypeInstance instanceof LeafNodeType)
                {
                    Set<LeafNodeType> nodeIndex = leafNodeIndex.get(name);
                    if (nodeIndex == null)
                    {
                        nodeIndex = new HashSet<>();
                        leafNodeIndex.put(name, nodeIndex);
                    }
                    nodeIndex.add((LeafNodeType) staticNodeTypeInstance);
                }
                else if (staticNodeTypeInstance instanceof BranchNodeType)
                {
                    Set<BranchNodeType> nodeIndex = branchNodeIndex.get(name);
                    if (nodeIndex == null)
                    {
                        nodeIndex = new HashSet<>();
                        branchNodeIndex.put(name, nodeIndex);
                    }
                    nodeIndex.add((BranchNodeType) staticNodeTypeInstance);
                }
                else if (staticNodeTypeInstance instanceof BranchNodeListType)
                {
                    Set<BranchNodeListType> nodeIndex = branchNodeListIndex.get(name);
                    if (nodeIndex == null)
                    {
                        nodeIndex = new HashSet<>();
                        branchNodeListIndex.put(name, nodeIndex);
                    }
                    nodeIndex.add((BranchNodeListType) staticNodeTypeInstance);
                }
                int pos = -1;
                int idx = 0;
                for (final Field existingField : fieldList)
                {
                    if (existingField.getName().equals(field.getName()))
                    {
                        pos = idx;
                        break;
                    }
                    idx++;
                }
                if (pos > -1)
                {
                    fieldList.set(pos, field);
                }
                else
                {
                    fieldList.add(field);
                }
            }
        }
        for (final Field field : fieldList)
        {
            INodeType staticNodeTypeInstance = getStaticFieldInstance(field);
            if (staticNodeTypeInstance != null)
            {
                list.add(staticNodeTypeInstance);
            }
        }
        
        fieldList.clear();
        
        // create helper stuff
        
        this.leafNodeTypeList = new ArrayList<LeafNodeType>();
        this.branchNodeTypeList = new ArrayList<BranchNodeType>();
        this.branchNodeListTypeList = new ArrayList<BranchNodeListType>();
        this.nodeTypeList = Collections.unmodifiableList(list);
        this.nodeTypeNames = new String[this.nodeTypeList.size()];
        Map<String, Integer> nodeTypeIndexByName = new HashMap<String, Integer>();
        Map<Object, Integer> nodeTypeIndexByClass = new HashMap<Object, Integer>();
        Map<Object, Object> nodeTypeIndexByHidden = new HashMap<Object, Object>();
        for (int i = 0; i < this.nodeTypeList.size(); i++)
        {
            INodeType staticNodeTypeInstance = this.nodeTypeList.get(i);
            String name = staticNodeTypeInstance.getNodeName();
            this.nodeTypeNames[i] = name;
            nodeTypeIndexByName.put(name, i);
            nodeTypeIndexByClass.put(staticNodeTypeInstance, i);
            
            if (staticNodeTypeInstance instanceof LeafNodeType)
            {
                this.leafNodeTypeList.add((LeafNodeType) staticNodeTypeInstance);
                Set<LeafNodeType> nodeIndex = leafNodeIndex.get(name);
                for (final LeafNodeType node : nodeIndex)
                {
                    if (node != staticNodeTypeInstance)
                    {
                        nodeTypeIndexByClass.put(node, i);
                        nodeTypeIndexByHidden.put(node, staticNodeTypeInstance);
                    }
                }
            }
            if (staticNodeTypeInstance instanceof BranchNodeType)
            {
                this.branchNodeTypeList.add((BranchNodeType) staticNodeTypeInstance);
                Set<BranchNodeType> nodeIndex = branchNodeIndex.get(name);
                for (final BranchNodeType node : nodeIndex)
                {
                    if (node != staticNodeTypeInstance)
                    {
                        nodeTypeIndexByClass.put(node, i);
                        nodeTypeIndexByHidden.put(node, staticNodeTypeInstance);
                    }
                }
            }
            if (staticNodeTypeInstance instanceof BranchNodeListType)
            {
                this.branchNodeListTypeList.add((BranchNodeListType) staticNodeTypeInstance);
                Set<BranchNodeListType> nodeIndex = branchNodeListIndex.get(name);
                for (final BranchNodeListType node : nodeIndex)
                {
                    if (node != staticNodeTypeInstance)
                    {
                        nodeTypeIndexByClass.put(node, i);
                        nodeTypeIndexByHidden.put(node, staticNodeTypeInstance);
                    }
                }
            }
        }
        this.leafNodeTypeList = Collections.unmodifiableList(this.leafNodeTypeList);
        this.branchNodeTypeList = Collections.unmodifiableList(this.branchNodeTypeList);
        this.branchNodeListTypeList = Collections.unmodifiableList(this.branchNodeListTypeList);
        this.nodeTypeIndexByName = Collections.unmodifiableMap(nodeTypeIndexByName);
        this.nodeTypeIndexByClass = Collections.unmodifiableMap(nodeTypeIndexByClass);
        this.nodeTypeIndexByHidden = Collections.unmodifiableMap(nodeTypeIndexByHidden);
    }
    
    protected volatile BranchNodeType<? extends BranchNodeMetaModel, ? extends BranchNodeMetaModel> anonymous;
    
    private String[] nodeTypeNames = null;
    private List<INodeType> nodeTypeList = null;
    private List<LeafNodeType> leafNodeTypeList = null;
    private List<BranchNodeType> branchNodeTypeList = null;
    private List<BranchNodeListType> branchNodeListTypeList = null;
    private Map<String, Integer> nodeTypeIndexByName = null;
    private Map<Object, Integer> nodeTypeIndexByClass = null;
    private Map<Object, Object> nodeTypeIndexByHidden = null;
    
    protected String[] getNodeTypeNames()
    {
        return this.nodeTypeNames;
    }
    
    public List<INodeType> getNodeTypeList()
    {
        return this.nodeTypeList;
    }
    
    public List<LeafNodeType> getLeafNodeTypeList()
    {
        return this.leafNodeTypeList;
    }
    
    public List<BranchNodeType> getBranchNodeTypeList()
    {
        return this.branchNodeTypeList;
    }
    
    public List<BranchNodeListType> getBranchNodeListTypeList()
    {
        return this.branchNodeListTypeList;
    }
    
    public Map<String, Integer> getNodeTypeIndexByName()
    {
        return this.nodeTypeIndexByName;
    }
    
    public Map<Object, Integer> getNodeTypeIndexByClass()
    {
        return this.nodeTypeIndexByClass;
    }
    
    public Map<Object, Object> getNodeTypeIndexByHidden()
    {
        return this.nodeTypeIndexByHidden;
    }
    
    protected static INodeType getStaticFieldInstance(final Field field)
    {
        Class<?> modelClass = field.getDeclaringClass();
        boolean isField = false;
        for (final Class<?> clazz : field.getType().getInterfaces())
        {
            if (clazz == INodeType.class)
            {
                isField = true;
                break;
            }
        }
        if (!isField)
        {
            return null;
        }
        Type type = field.getGenericType();
        Class<?> fieldClass = field.getType();
        int fieldModifier = field.getModifiers();
        
        if ((type instanceof final ParameterizedType pType) && ((fieldClass == LeafNodeType.class) || (fieldClass == BranchNodeType.class) || (fieldClass == BranchNodeListType.class)))
        {
            if ((pType.getActualTypeArguments() != null) && (pType.getActualTypeArguments().length == 2))
            {
                if (pType.getActualTypeArguments()[0] == modelClass)
                {
                    
                    Type type2 = pType.getActualTypeArguments()[1];
                    
                    try
                    {
                        if (Modifier.isStatic(fieldModifier) && (!Modifier.isFinal(fieldModifier)))
                        {
                            if (field.get(null) == null)
                            {
                                if (type2 instanceof final ParameterizedType pType2)
                                {
                                    type2 = pType2.getRawType();
                                }
                                Object nodeType = fieldClass.getConstructor(Class.class, Class.class, Field.class).newInstance(modelClass, type2, field);
                                field.set(null, nodeType);
                            }
                            return (INodeType) field.get(null);
                        }
                    }
                    catch (final Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        return null;
    }
}
