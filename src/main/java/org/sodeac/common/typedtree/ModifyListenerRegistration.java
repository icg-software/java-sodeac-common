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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.typedtree.ModelPath.NodeSelector;

public class ModifyListenerRegistration<R extends BranchNodeMetaModel>
{
    private Set<NodeSelector<R, ?>> rootNodeSelectorList = null;
    private boolean indisposable = false;
    private boolean finalState = false;
    
    protected ModifyListenerRegistration()
    {
        super();
        this.rootNodeSelectorList = new HashSet<ModelPath.NodeSelector<R, ?>>();
    }
    
    protected Set<NodeSelector<R, ?>> getRootNodeSelectorList()
    {
        return rootNodeSelectorList;
    }
    
    public void dispose()
    {
        if (this.indisposable)
        {
            return;
        }
        this.recursiveDispose(this.rootNodeSelectorList);
    }
    
    protected boolean isIndisposable()
    {
        return indisposable;
    }
    
    protected ModifyListenerRegistration<R> setIndisposable()
    {
        this.indisposable = true;
        return this;
    }
    
    protected boolean isFinal()
    {
        return finalState;
    }
    
    protected ModifyListenerRegistration<R> setFinal()
    {
        this.finalState = true;
        return this;
    }
    
    private void recursiveDispose(Collection nodeSelectorList)
    {
        if (nodeSelectorList == null)
        {
            return;
        }
        
        if (nodeSelectorList.isEmpty())
        {
            return;
        }
        
        for (NodeSelector<?, ?> nodeSelector : (Collection<NodeSelector<?, ?>>) nodeSelectorList)
        {
            
            // recursive
            if ((nodeSelector.getChildSelectorList() != null) && (!nodeSelector.getChildSelectorList().isEmpty()))
            {
                recursiveDispose(nodeSelector.getChildSelectorList());
                nodeSelector.getChildSelectorList().clear();
            }
            
            for (Entry<ConplierBean<Object>, Set<IModifyListener<?>>> entry : nodeSelector.getRegistrationObjects().entrySet())
            {
                if (entry.getKey() != null)
                {
                    entry.getKey().dispose();
                }
                if (entry.getValue() != null)
                {
                    entry.getValue().clear();
                }
            }
            nodeSelector.getRegistrationObjects().clear();
            
            if (nodeSelector.getModifyListenerList() != null)
            {
                nodeSelector.getModifyListenerList().clear();
            }
            
            nodeSelector.dispose();
        }
        
        nodeSelectorList.clear();
    }
    
    protected <T> ModifyListenerRegistration<R> registerListener(ModelPath<R, T> path, IModifyListener<T> listener)
    {
        return registerListeners(path, listener);
    }
    
    protected <T> ModifyListenerRegistration<R> registerListeners(ModelPath<R, T> path, IModifyListener<T>... listener)
    {
        if (finalState)
        {
            return this;
        }
        
        // deep copy
        ModelPath<R, T> originPath = path;
        path = path.clone();
        
        // add listener to deep copy of path
        NodeSelector targetNodeSelector = path.getNodeSelectorList().get(path.getNodeSelectorList().size() - 1);
        if (listener != null)
        {
            for (IModifyListener modifyListener : listener)
            {
                if (targetNodeSelector.getModifyListenerList() == null)
                {
                    targetNodeSelector.setModifyListenerList(new HashSet<>());
                }
                if (!targetNodeSelector.getModifyListenerList().contains(modifyListener))
                {
                    targetNodeSelector.getModifyListenerList().add(modifyListener);
                }
            }
        }
        List<NodeSelector<?, ?>> otherNodeSelectorList = new ArrayList<NodeSelector<?, ?>>();
        otherNodeSelectorList.add(path.getNodeSelectorList().get(0));
        
        recursiveRegister(null, this.rootNodeSelectorList, otherNodeSelectorList, originPath);
        
        return this;
    }
    
    protected <T> ModifyListenerRegistration<R> unregister(ModelPath<R, T> path)
    {
        recursiveUnregister(this.rootNodeSelectorList, path);
        return this;
    }
    
    protected <T> ModifyListenerRegistration<R> registerListener(ModifyListenerRegistration<?> registration, IModifyListener<T> listener)
    {
        return registerListeners(registration, listener);
    }
    
    protected <T> ModifyListenerRegistration<R> registerListeners(ModifyListenerRegistration<?> registration, IModifyListener<T>... listener)
    {
        recursiveRegister(null, this.rootNodeSelectorList, registration.rootNodeSelectorList, registration);
        return this;
    }
    
    protected <T> ModifyListenerRegistration<R> unregister(ModifyListenerRegistration<?> registration)
    {
        recursiveUnregister(this.rootNodeSelectorList, registration);
        return this;
    }
    
    private <T> void recursiveUnregister(Collection nodeSelectorList, Object pathObject)
    {
        if (finalState)
        {
            return;
        }
        
        if (nodeSelectorList == null)
        {
            return;
        }
        
        if (nodeSelectorList.isEmpty())
        {
            return;
        }
        
        ConplierBean<Object> sameWrapper = new ConplierBean<Object>(pathObject);
        
        List<NodeSelector<?, ?>> toRemoveSelector = new ArrayList<NodeSelector<?, ?>>();
        for (NodeSelector<?, ?> nodeSelector : (Collection<NodeSelector<?, ?>>) nodeSelectorList)
        {
            if (nodeSelector.getRegistrationObjects().get(sameWrapper) == null)
            {
                // nothing todo
                continue;
            }
            
            // recursive
            if ((nodeSelector.getChildSelectorList() != null) && (!nodeSelector.getChildSelectorList().isEmpty()))
            {
                recursiveUnregister(nodeSelector.getChildSelectorList(), pathObject);
            }
            
            // remove registration
            if (nodeSelector.getRegistrationObjects().get(sameWrapper) != null)
            {
                nodeSelector.getRegistrationObjects().get(sameWrapper).clear();
            }
            nodeSelector.getRegistrationObjects().remove(sameWrapper);
            
            // merge modify listener list by still existing registrations
            if ((nodeSelector.getModifyListenerList() != null) && (!nodeSelector.getModifyListenerList().isEmpty()))
            {
                Set<IModifyListener<?>> listenerIndex = new HashSet<IModifyListener<?>>();
                for (Set<IModifyListener<?>> listenerList : nodeSelector.getRegistrationObjects().values())
                {
                    listenerIndex.addAll(listenerList);
                }
                
                List<IModifyListener<?>> toRemoveListener = new ArrayList<IModifyListener<?>>();
                for (IModifyListener<?> modifyListener : nodeSelector.getModifyListenerList())
                {
                    if (!listenerIndex.contains(modifyListener))
                    {
                        toRemoveListener.add(modifyListener);
                    }
                }
                
                listenerIndex.clear();
                listenerIndex = null;
                
                for (IModifyListener<?> toRemove : toRemoveListener)
                {
                    nodeSelector.getModifyListenerList().remove(toRemove);
                }
                toRemoveListener.clear();
            }
            
            // remove registration, if no registration object exists anymore
            if (nodeSelector.getRegistrationObjects().isEmpty())
            {
                nodeSelector.dispose();
                toRemoveSelector.add(nodeSelector);
            }
        }
        
        for (NodeSelector<?, ?> toRemove : toRemoveSelector)
        {
            nodeSelectorList.remove(toRemove);
        }
        
        sameWrapper.dispose();
    }
    
    private <T> void recursiveRegister(NodeSelector<?, ?> parent, Collection thisNodeSelectorList, Collection otherNodeSelectorList, Object pathObject)
    {
        if (finalState)
        {
            return;
        }
        
        Objects.requireNonNull(thisNodeSelectorList, "this node selector is null");
        Objects.requireNonNull(pathObject, "pathObject is null");
        
        if (otherNodeSelectorList == null)
        {
            return;
        }
        
        if (otherNodeSelectorList.isEmpty())
        {
            return;
        }
        
        for (NodeSelector<?, ?> otherNodeSelector : (Collection<NodeSelector<?, ?>>) otherNodeSelectorList)
        {
            // current this nodeSelector
            
            NodeSelector<?, ?> nodeSelector = null;
            for (NodeSelector<?, ?> thisNodeSelector : (Collection<NodeSelector<?, ?>>) thisNodeSelectorList)
            {
                if (thisNodeSelector.equals(otherNodeSelector))
                {
                    nodeSelector = thisNodeSelector;
                    break;
                }
            }
            if (nodeSelector == null)
            {
                nodeSelector = otherNodeSelector.clone(parent, false);
                thisNodeSelectorList.add(nodeSelector);
            }
            
            // registration object
            
            ConplierBean<Object> sameObjectWrapper = new ConplierBean<Object>(pathObject).setEqualsBySameValue(true);
            if (nodeSelector.getRegistrationObjects() == null)
            {
                nodeSelector.setRegistrationObjects(new HashMap<ConplierBean<Object>, Set<IModifyListener<?>>>());
            }
            if (nodeSelector.getRegistrationObjects().get(sameObjectWrapper) == null)
            {
                nodeSelector.getRegistrationObjects().put(new ConplierBean<Object>(pathObject).setEqualsBySameValue(true), new HashSet<IModifyListener<?>>());
            }
            
            // modify listener
            
            if ((otherNodeSelector.getModifyListenerList() != null) && (!otherNodeSelector.getModifyListenerList().isEmpty()))
            {
                Set<IModifyListener<?>> registrationSet = nodeSelector.getRegistrationObjects().get(sameObjectWrapper);
                if (nodeSelector.getModifyListenerList() == null)
                {
                    nodeSelector.setModifyListenerList(new HashSet<IModifyListener<?>>());
                }
                for (IModifyListener<?> modifyListener : otherNodeSelector.getModifyListenerList())
                {
                    if (!nodeSelector.getModifyListenerList().contains(modifyListener))
                    {
                        nodeSelector.getModifyListenerList().add(modifyListener);
                    }
                    if (!registrationSet.contains(modifyListener))
                    {
                        registrationSet.add(modifyListener);
                    }
                }
            }
            
            sameObjectWrapper.dispose();
            sameObjectWrapper = null;
            
            // recursive
            
            if ((otherNodeSelector.getChildSelectorList() != null) && (!otherNodeSelector.getChildSelectorList().isEmpty()))
            {
                if (nodeSelector.getChildSelectorList() == null)
                {
                    nodeSelector.setChildSelectorList(new HashSet<NodeSelector<?, ?>>());
                }
                recursiveRegister(nodeSelector, nodeSelector.getChildSelectorList(), otherNodeSelector.getChildSelectorList(), pathObject);
            }
            
        }
    }
    
}
