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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

/**
 * Intern helper class
 * 
 * @author Sebastian Palarus
 *
 */
public class ModelRegistry
{
	protected static final ModelRegistry DEFAULT_INSTANCE = new ModelRegistry();
	
	protected ModelRegistry()
	{
		ReadWriteLock lock = new ReentrantReadWriteLock(true);
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
		this.typedTreeModelIndex = new HashMap<Class<?>,TypedTreeMetaModel>();
		this.branchNodeModelIndex = new HashMap<Class<?>,BranchNodeMetaModel>();
	}
	
	private Lock readLock = null;
	private Lock writeLock = null;
	private Map<Class<?>,TypedTreeMetaModel> typedTreeModelIndex = null;
	private Map<Class<?>,BranchNodeMetaModel> branchNodeModelIndex = null;
	
	public static <M extends TypedTreeMetaModel> M getTypedTreeMetaModel(Class<M> clazz)
	{
		return ModelRegistry.DEFAULT_INSTANCE.getCachedTypedTreeMetaModel(clazz);
	}
		
	protected <M extends TypedTreeMetaModel> M getCachedTypedTreeMetaModel(Class<M> clazz)
	{
		this.readLock.lock();
		try
		{
			M modelObject = (M)this.typedTreeModelIndex.get(clazz);
			if(modelObject != null)
			{
				return modelObject;
			}
		}
		finally 
		{
			this.readLock.unlock();
		}
		
		this.writeLock.lock();
		try
		{
			M modelObject = (M)this.typedTreeModelIndex.get(clazz);
			if(modelObject != null)
			{
				return modelObject;
			}
			
			modelObject = (M) clazz.newInstance();
			this.typedTreeModelIndex.put(clazz, modelObject);
			
			return modelObject;
		}
		catch (RuntimeException e) 
		{
			throw e;
		}
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		finally 
		{
			this.writeLock.unlock();
		}
	}
	
	public static <M extends BranchNodeMetaModel> M getBranchNodeMetaModel(Class<M> clazz)
	{
		return ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(clazz);
	}
	
	protected <M extends BranchNodeMetaModel> M getCachedBranchNodeMetaModel(Class<M> clazz)
	{
		this.readLock.lock();
		try
		{
			M modelObject = (M)this.branchNodeModelIndex.get(clazz);
			if(modelObject != null)
			{
				return modelObject;
			}
		}
		finally 
		{
			this.readLock.unlock();
		}
		
		this.writeLock.lock();
		try
		{
			M modelObject = (M)this.branchNodeModelIndex.get(clazz);
			if(modelObject != null)
			{
				return modelObject;
			}
			
			modelObject = (M) clazz.newInstance();
			this.branchNodeModelIndex.put(clazz, modelObject);
			
			return modelObject;
		}
		catch (RuntimeException e) 
		{
			throw e;
		}
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		finally 
		{
			this.writeLock.unlock();
		}
	}
	
	public static void parse(Class<? extends BranchNodeMetaModel> clazz, ITypedTreeModelParserHandler handler)
	{
		if(clazz ==  null)
		{
			return;
		}
		
		
		Map<Class<? extends BranchNodeMetaModel>,Set<INodeType<BranchNodeMetaModel, ?>>> toParseSet = new HashMap<Class<? extends BranchNodeMetaModel>,Set<INodeType<BranchNodeMetaModel, ?>>>();
		List<Class<? extends BranchNodeMetaModel>> orderedParseClassList = new ArrayList<Class<? extends BranchNodeMetaModel>>();
		Set<Class<? extends BranchNodeMetaModel>> doneSet = new HashSet<Class<? extends BranchNodeMetaModel>>();
		
		try
		{
			toParseSet.put(clazz, new HashSet<>());
			orderedParseClassList.add(clazz);
			
			while(toParseSet.size() > doneSet.size())
			{
				for(Entry<Class<? extends BranchNodeMetaModel>,Set<INodeType<BranchNodeMetaModel, ?>>> entry : toParseSet.entrySet())
				{
					Class<? extends BranchNodeMetaModel> toParse = entry.getKey();
					if(doneSet.contains(toParse))
					{
						continue;
					}
					
					doneSet.add(toParse);
					BranchNodeMetaModel modelInstance = ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(toParse);
					
					boolean toParseModify = false;
					for(INodeType<BranchNodeMetaModel, ?> staticNodeTypeInstance : modelInstance.getNodeTypeList())
					{
						if(staticNodeTypeInstance instanceof BranchNodeType)
						{
							staticNodeTypeInstance.getValueDefaultInstance();
							if(! toParseSet.containsKey(staticNodeTypeInstance.getTypeClass()))
							{
								toParseModify = true;
								toParseSet.put((Class<BranchNodeMetaModel>)staticNodeTypeInstance.getTypeClass(), new HashSet<>());
								orderedParseClassList.add((Class<BranchNodeMetaModel>)staticNodeTypeInstance.getTypeClass());
							}
							toParseSet.get(staticNodeTypeInstance.getTypeClass()).add(staticNodeTypeInstance);
						}
						if(staticNodeTypeInstance instanceof BranchNodeListType)
						{
							staticNodeTypeInstance.getValueDefaultInstance();
							if(! toParseSet.containsKey(staticNodeTypeInstance.getTypeClass()))
							{
								toParseModify = true;
								toParseSet.put((Class<BranchNodeMetaModel>)staticNodeTypeInstance.getTypeClass(), new HashSet<>());
								orderedParseClassList.add((Class<BranchNodeMetaModel>)staticNodeTypeInstance.getTypeClass());
							}
							toParseSet.get(staticNodeTypeInstance.getTypeClass()).add(staticNodeTypeInstance);
						}
					}
					if(toParseModify)
					{
						break;
					}
				}
			}
			if(handler != null)
			{
				for(Class<? extends BranchNodeMetaModel> toParse : orderedParseClassList)
				{
					BranchNodeMetaModel modelInstance = ModelRegistry.DEFAULT_INSTANCE.getCachedBranchNodeMetaModel(toParse);
					
					handler.startModel(modelInstance,toParseSet.get(toParse));
					
					for(INodeType<BranchNodeMetaModel, ?> staticNodeTypeInstance : modelInstance.getNodeTypeList())
					{
						if(handler != null)
						{
							handler.onNodeType(modelInstance, staticNodeTypeInstance);
						}
					}
					
					handler.endModel(modelInstance,toParseSet.get(toParse));
				}
			}
		}
		finally 
		{
			if(toParseSet != null)
			{
				toParseSet.clear();
			}
			
			if(orderedParseClassList != null)
			{
				orderedParseClassList.clear();
			}
			
			if(doneSet != null)
			{
				doneSet.clear();
			}
		}
	}

}
