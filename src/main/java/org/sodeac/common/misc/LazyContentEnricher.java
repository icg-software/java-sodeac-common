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
package org.sodeac.common.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sodeac.common.misc.LazyContentEnricher.CommonContentEnricher.CardinalityMode;
import org.sodeac.common.misc.LazyContentEnricher.CommonContentEnricher.WorkingMode;

public class LazyContentEnricher<T,R,E> implements AutoCloseable 
{
	private Map<T,Set<R>> referencesByObjectsToBeEnriched;
	private Map<R,Set<T>> objectsToBeEnrichByReference;
	private Consumer<LazyContentEnricher<T,R,E>> contentEnricher;

	
	public static LazyContentEnricher newInstance()
	{
		LazyContentEnricher lce = new LazyContentEnricher<>();
		lce.referencesByObjectsToBeEnriched = new HashMap<>();
		lce.objectsToBeEnrichByReference = new LinkedHashMap<>();
		
		return lce;
	}
	
	public LazyContentEnricher<T,R,E> defineContentEnricher(Consumer<LazyContentEnricher<T,R,E>> contentEnricher)
	{
		this.contentEnricher = contentEnricher;
		return this;
	}
	
	public Collection<R> getReferences()
	{
		return this.objectsToBeEnrichByReference.keySet();
	}

	public Map<T, Set<R>> getReferencesByObjectsToBeEnriched()
	{
		return referencesByObjectsToBeEnriched;
	}

	public Map<R, Set<T>> getObjectsToBeEnrichByReference()
	{
		return objectsToBeEnrichByReference;
	}

	public LazyContentEnricher<T,R,E> invokeContentEnricher()
	{
		try
		{
			if(this.contentEnricher != null)
			{
				this.contentEnricher.accept(this);
			}
		}
		finally 
		{
			this.clear();
		}
		return this;
	}
	
	private void clear()
	{
		try
		{
			for(Set<R> reference : this.referencesByObjectsToBeEnriched.values())
			{
				try
				{
					reference.clear();
				}
				catch (Exception | Error e) {}
			}
		}
		catch (Exception | Error e) {}
		
		try
		{
			this.referencesByObjectsToBeEnriched.clear();
		}
		catch (Exception | Error e) {}
		
		try
		{
			for(Set<T> objects : this.objectsToBeEnrichByReference.values())
			{
				try
				{
					objects.clear();
				}
				catch (Exception | Error e) {}
			}
		}
		catch (Exception | Error e) {}
		
		try
		{
			this.objectsToBeEnrichByReference.clear();
		}
		catch (Exception | Error e) {}
		
		try
		{
			if(this.contentEnricher instanceof IClearable)
			{
				((IClearable) this.contentEnricher).clear();
			}
		}
		catch (Exception | Error e) {}
	}
	
	public T register(T objectToBeEnriched, R reference)
	{
		Objects.requireNonNull(objectToBeEnriched, "object to be enriched not defined");
		Objects.requireNonNull(reference, "reference not defined");
		Set<R> registeredReferences = this.referencesByObjectsToBeEnriched.get(objectToBeEnriched);
		if(registeredReferences == null)
		{
			registeredReferences = new LinkedHashSet<>();
			this.referencesByObjectsToBeEnriched.put(objectToBeEnriched,registeredReferences);
		}
		registeredReferences.add(reference);
		Set<T> registeredObjectsToBeEnriched = this.objectsToBeEnrichByReference.get(reference);
		if(registeredObjectsToBeEnriched == null)
		{
			registeredObjectsToBeEnriched = new LinkedHashSet<>();
			this.objectsToBeEnrichByReference.put(reference,registeredObjectsToBeEnriched);
		}
		registeredObjectsToBeEnriched.add(objectToBeEnriched);
		
		return objectToBeEnriched;
	}
	

	@Override
	public void close() throws Exception
	{
		this.clear();
		if(this.contentEnricher instanceof AutoCloseable)
		{
			try
			{
				((AutoCloseable)this.contentEnricher).close();
			}
			catch (Exception e) {}
		}
		
	}
	
	public interface IClearable
	{
		public void clear();
	}
	
	public static class CommonContentEnricher<T,R,E> implements AutoCloseable,IClearable,Consumer<LazyContentEnricher<T,R,E>>
	{
		public static enum CardinalityMode {ONE_REFERENCE_TO_ONE_ENRICHMENT,ONE_REFERENCE_TO_MANY_ENRICHMENTS}
		public static enum WorkingMode {DEDICATED,ON_THE_FLY}
		
		private CardinalityMode cardinalityMode = CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT;
		private WorkingMode workingMode = WorkingMode.ON_THE_FLY;
		
		private SimpleShrinkableCache<R, E> enrichmentOneCache = null;
		private SimpleShrinkableCache<R, List<E>> enrichmentManyCache = null;
		
		private Function<CommonContentEnricher<T,R,E>, E> newBlankEnrichmentHandler = null;
		private Function<CommonContentEnricher<T,R,E>, E> cloneEnrichmentHandler = null;
		private Consumer<CommonContentEnricher<T,R,E>> linkEnrichmentHandler = null;
		private BiConsumer<Collection<R>, CommonContentEnricher<T,R,E>> enricheCycleHandler = null;
		
		private BiConsumer<R,E> removeFromCacheHandler = null;
		private int cacheSize = 0;
		
		private Map<String,Object> properties = null;

		private CommonContentEnricher()
		{
			super();
		}
		
		// working stuff
		
		private Collection<R> references = null;
		private LazyContentEnricher<T,R,E> lazyContentEnricher = null;
		private T objectToBeEnriched = null;
		private R reference = null;
		private E enrichment = null;
		
		@Override
		public void accept(LazyContentEnricher<T, R, E> lazyContentEnricher)
		{
			Collection<R> originReferences = lazyContentEnricher.getReferences();
			this.lazyContentEnricher = lazyContentEnricher;
			this.references = null;
			
			boolean referencesToClear = false;
			try
			{
				if(this.workingMode == WorkingMode.DEDICATED)
				{
					if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_MANY_ENRICHMENTS)
					{
						if(this.enrichmentManyCache.getView().isEmpty())
						{
							this.references = originReferences;
						}
						else
						{
							this.references = new ArrayList<>(originReferences.size()); referencesToClear = true;
							for(R reference : originReferences)
							{
								if(! this.enrichmentManyCache.containsKey(reference))
								{
									this.references.add(reference);
									this.enrichmentManyCache.put(reference, null);
								}
							}
						}
					}
					else if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
					{
						if(this.enrichmentOneCache.getView().isEmpty())
						{
							this.references = originReferences;
						}
						else
						{
							this.references = new ArrayList<>(originReferences.size()); referencesToClear = true;
							for(R reference : originReferences)
							{
								if(! this.enrichmentOneCache.containsKey(reference))
								{
									this.references.add(reference);
									this.enrichmentOneCache.put(reference, null);
								}
							}
						}
					}
					
					if(! this.references.isEmpty())
					{
						this.enricheCycleHandler.accept(this.references, this);
					}
					
					for(Entry<R,Set<T>> objectsEntry : this.lazyContentEnricher.getObjectsToBeEnrichByReference().entrySet())
					{
						this.reference = objectsEntry.getKey();
						
						for(T object : objectsEntry.getValue())
						{
							this.objectToBeEnriched = object;
							
							if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_MANY_ENRICHMENTS)
							{
								List<E> enrichmentList = this.enrichmentManyCache.get(this.reference);
								
								if(enrichmentList != null)
								{
									for(E enrichment : enrichmentList)
									{
										this.enrichment = enrichment;
										
										if(this.cloneEnrichmentHandler != null)
										{
											this.enrichment = this.cloneEnrichmentHandler.apply(this);
										}
										if(this.linkEnrichmentHandler != null)
										{
											this.linkEnrichmentHandler.accept(this);
										}
									}
								}
							}
							else if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
							{
								this.enrichment = this.enrichmentOneCache.get(this.reference);
								
								if(this.cloneEnrichmentHandler != null)
								{
									this.enrichment = this.cloneEnrichmentHandler.apply(this);
								}
								if(this.linkEnrichmentHandler != null)
								{
									this.linkEnrichmentHandler.accept(this);
								}
							}
						}
						
						this.objectToBeEnriched = null;
						this.reference = null;
						this.enrichment = null;
					}
				}
				else if(workingMode == WorkingMode.ON_THE_FLY)
				{
					if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_MANY_ENRICHMENTS)
					{
						if((this.cacheSize == 0) || this.enrichmentManyCache.getView().isEmpty())
						{
							this.references = originReferences;
						}
						else
						{
							this.references = new LinkedHashSet<>(); referencesToClear = true;
							for(R reference : originReferences)
							{
								if(this.enrichmentManyCache.containsKey(reference))
								{
									this.reference = reference;
									Collection<T> objects  = this.lazyContentEnricher.getObjectsToBeEnrichByReference().get(reference);
									if(objects != null)
									{
										for(T object : objects)
										{	
											this.objectToBeEnriched = object;
											
											List<E> enrichmentList = this.enrichmentManyCache.get(reference);
											if(enrichmentList != null)
											{
												for(E enrichment : enrichmentList)
												{
													this.enrichment = enrichment;
													
													// Cache First
													
													if(this.cloneEnrichmentHandler != null)
													{
														this.enrichment = this.cloneEnrichmentHandler.apply(this);
													}
													if(this.linkEnrichmentHandler != null)
													{
														this.linkEnrichmentHandler.accept(this);
													}
												}
											}
											this.objectToBeEnriched = null;
										}
									}
									this.reference = null;
								}
								else
								{
									this.references.add(reference);
									this.enrichmentManyCache.put(reference, null);
								}
							}
						}
					}
					else if(this.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
					{
						if((this.cacheSize == 0) || this.enrichmentOneCache.getView().isEmpty())
						{
							this.references = originReferences;
						}
						else
						{
							this.references = new LinkedHashSet<>(); referencesToClear = true;
							for(R reference : originReferences)
							{
								if(this.enrichmentOneCache.containsKey(reference))
								{
									this.reference = reference;
									Collection<T> objects  = this.lazyContentEnricher.getObjectsToBeEnrichByReference().get(reference);
									if(objects != null)
									{
										for(T object : objects)
										{
											this.objectToBeEnriched = object;
											this.enrichment = this.enrichmentOneCache.get(reference);
											
											// Cache First
											
											if(this.cloneEnrichmentHandler != null)
											{
												this.enrichment = this.cloneEnrichmentHandler.apply(this);
											}
											if(this.linkEnrichmentHandler != null)
											{
												this.linkEnrichmentHandler.accept(this);
											}
											
											this.objectToBeEnriched = null;
											this.enrichment = null;
										}
									}
									this.reference = null;
								}
								else
								{
									this.references.add(reference);
									this.enrichmentOneCache.put(reference, null);
								}
							}
						}
					}
					
					if(! this.references.isEmpty())
					{
						this.enricheCycleHandler.accept(this.references, this);
					}
				}
			}
			finally 
			{
				if(referencesToClear)
				{
					try
					{
						this.references.clear();
					}
					catch (Exception e) {}
				}
				
				this.lazyContentEnricher = null;
				this.references = null;
				this.objectToBeEnriched = null;
				this.reference = null;
				this.enrichment = null;
				
			}
			
		}
		
		public void supplyEnrichment(R reference, E enrichment)
		{
			if(this.workingMode == WorkingMode.DEDICATED)
			{
				if(cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
				{
					this.enrichmentOneCache.put(reference, enrichment);
				}
				else
				{
					List<E> enrichmentList = this.enrichmentManyCache.get(reference);
					if(enrichmentList == null)
					{
						enrichmentList = new LinkedList<>();
						this.enrichmentManyCache.put(reference, enrichmentList);
					}
					enrichmentList.add(enrichment);
				}
			}
			else if(this.workingMode == WorkingMode.ON_THE_FLY)
			{
				this.reference = reference;
				this.enrichment = enrichment;
				
				Collection<T> objects  = this.lazyContentEnricher.getObjectsToBeEnrichByReference().get(reference);
				if(objects != null)
				{
					boolean first = true;
					for(T object : objects)
					{
						this.objectToBeEnriched = object;
						if(first)
						{
							first = false;
							if(this.linkEnrichmentHandler != null)
							{
								this.linkEnrichmentHandler.accept(this);
							}
						}
						else
						{
							if(this.cloneEnrichmentHandler != null)
							{
								this.enrichment = this.cloneEnrichmentHandler.apply(this);
							}
							if(this.linkEnrichmentHandler != null)
							{
								this.linkEnrichmentHandler.accept(this);
							}
						}
						this.objectToBeEnriched = null;
					}
				}
				
				if(this.cacheSize > 0)
				{
					if(cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
					{
						this.enrichmentOneCache.put(reference, enrichment);
					}
					else
					{
						List<E> enrichmentList = this.enrichmentManyCache.get(reference);
						if(enrichmentList == null)
						{
							enrichmentList = new LinkedList<>();
							this.enrichmentManyCache.put(reference, enrichmentList);
						}
						enrichmentList.add(enrichment);
					}
				}
				
				this.reference = null;
				this.enrichment = null;
				
			}
		}
		
		public E createBlankEnrichment(R reference)
		{
			this.reference = reference;
			Set<T> objects = this.lazyContentEnricher.getObjectsToBeEnrichByReference().get(reference);
			if((objects != null) && (!objects.isEmpty()))
			{
				this.objectToBeEnriched = objects.iterator().next();
			}
			try
			{
				return this.newBlankEnrichmentHandler.apply(this);
			}
			finally 
			{
				this.objectToBeEnriched = null;
				this.reference = null;
				
			}
		}

		public Collection<R> getReferences()
		{
			return references;
		}

		public T getObjectToBeEnriched()
		{
			return objectToBeEnriched;
		}

		public R getReference()
		{
			return reference;
		}

		public E getEnrichment()
		{
			return enrichment;
		}

		@Override
		public void clear()
		{
			if(this.cacheSize > 0)
			{
				if(this.enrichmentManyCache != null)
				{
					if(removeFromCacheHandler == null)
					{
						this.enrichmentManyCache.shrink(this.cacheSize,null);
					}
					else
					{
						this.enrichmentManyCache.shrink(this.cacheSize, (r,l) -> { if(l != null) {l.forEach(e -> removeFromCacheHandler.accept(r, e)); l.clear();} });
					}
				}
				if(this.enrichmentOneCache != null)
				{
					this.enrichmentOneCache.shrink(this.cacheSize, removeFromCacheHandler);
				}
			}
			else
			{
				if(this.enrichmentManyCache != null)
				{
					if(this.removeFromCacheHandler != null)
					{
						this.enrichmentManyCache.getView().forEach((r,l) -> { if(l != null) {l.forEach(e -> removeFromCacheHandler.accept(r, e)); l.clear();} });
					}
					this.enrichmentManyCache.clear();
				}
				if(this.enrichmentOneCache != null)
				{
					if(this.removeFromCacheHandler != null)
					{
						this.enrichmentOneCache.getView().forEach((r,e) -> removeFromCacheHandler.accept(r, e) );
					}
					this.enrichmentOneCache.clear();
				}
			}
			
		}

		@Override
		public void close() throws Exception
		{
			try
			{
				if(this.enrichmentManyCache != null)
				{
					if(this.removeFromCacheHandler != null)
					{
						this.enrichmentManyCache.getView().forEach((r,l) -> { if(l != null) {l.forEach(e -> removeFromCacheHandler.accept(r, e)); l.clear();} });
					}
					this.enrichmentManyCache.clear();
				}
				if(this.enrichmentOneCache != null)
				{
					if(this.removeFromCacheHandler != null)
					{
						this.enrichmentOneCache.getView().forEach((r,e) -> removeFromCacheHandler.accept(r, e) );
					}
					this.enrichmentOneCache.clear();
				}
			}
			finally 
			{
				this.lazyContentEnricher = null;
				this.enrichmentOneCache = null;
				this.enrichmentManyCache = null;
				
				this.newBlankEnrichmentHandler = null;
				this.cloneEnrichmentHandler = null;
				this.linkEnrichmentHandler = null;
				this.enricheCycleHandler = null;
				
				this.removeFromCacheHandler = null;
				this.cacheSize = 0;
			}
		}

		public LazyContentEnricher<T, R, E> getLazyContentEnricher()
		{
			return lazyContentEnricher;
		}
		
		public static WorkingModeOption newBuilder()
		{
			return new WorkingModeOption();
		}
		
		public static class WorkingModeOption
		{
			private WorkingModeOption()
			{
				super();
				this.cce = new CommonContentEnricher<>();
			}
			
			private CommonContentEnricher cce = null;
			
			public CardinalityModeOption workingMode(WorkingMode workingMode)
			{
				if(workingMode != null)
				{
					this.cce.workingMode = workingMode;
				}
				return new CardinalityModeOption(this.cce);
			}
		}
		
		public static class CardinalityModeOption
		{
			private CardinalityModeOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public TypeOfObjetToBeEnrichedOption cardinalityMode(CardinalityMode cardinalityMode)
			{
				if(cardinalityMode != null)
				{
					this.cce.cardinalityMode = cardinalityMode;
				}
				return new TypeOfObjetToBeEnrichedOption(this.cce);
			}
		}
		
		public static class TypeOfObjetToBeEnrichedOption
		{
			private TypeOfObjetToBeEnrichedOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public <T> TypeOfReferenceOption<T> typeOfObjectsToBeEnriched(Class<T> tp)
			{
				return new TypeOfReferenceOption<>(this.cce);
			}
		}
		
		public static class TypeOfReferenceOption<T>
		{
			private TypeOfReferenceOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public <R> TypeOfEnrichmentOption<T,R> typeOfReferences(Class<R> tp)
			{
				return new TypeOfEnrichmentOption<>(this.cce);
			}
		}
		
		public static class TypeOfEnrichmentOption<T,R>
		{
			private TypeOfEnrichmentOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public <E> NewBlankHandlerOption<T,R,E> typeOfEnrichments(Class<E> tp)
			{
				return new NewBlankHandlerOption<>(this.cce);
			}
		}
		
		public static class NewBlankHandlerOption<T,R,E>
		{
			private NewBlankHandlerOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public CloneHandlerOption<T,R,E> newBlankEnrichmentHandler(Function<CommonContentEnricher<T,R,E>,E> newBlankEnrichmentHandler)
			{
				Objects.requireNonNull(newBlankEnrichmentHandler, "handler to create new blank enrichments not defined");
				this.cce.newBlankEnrichmentHandler = newBlankEnrichmentHandler;
				
				return new CloneHandlerOption<>(this.cce);
			}
		}
		
		public static class CloneHandlerOption<T,R,E>
		{
			private CloneHandlerOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			private CommonContentEnricher cce = null;
			
			public EnrichHandlerWithOptionallyLinkOption<T,R,E>  cloneEnrichmentHandler(Function<CommonContentEnricher<T,R,E>, E> cloneEnrichmentHandler)
			{
				this.cce.cloneEnrichmentHandler = cloneEnrichmentHandler;
				return new EnrichHandlerWithOptionallyLinkOption(this.cce);
			}
		}
		
		public static class EnrichHandlerOption <T,R,E>
		{
			private EnrichHandlerOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			
			protected CommonContentEnricher cce = null;
			
			public HandleRemoveFromCacheOption<T,R,E> enrichCycleHandler (BiConsumer<Collection<R>, CommonContentEnricher<T,R,E>> enricheCycleHandler)
			{
				this.cce.enricheCycleHandler = enricheCycleHandler;
				return new HandleRemoveFromCacheOption<>(this.cce);
			}
		}
		
		public static class EnrichHandlerWithOptionallyLinkOption<T,R,E> extends EnrichHandlerOption<T, R, E>
		{
			private EnrichHandlerWithOptionallyLinkOption(CommonContentEnricher cce)
			{
				super(cce);
			}
			
			public EnrichHandlerOption<T,R,E> linkEnrichmentHandler(Consumer<CommonContentEnricher<T,R,E>> linkEnrichmentHandler)
			{
				super.cce.linkEnrichmentHandler = linkEnrichmentHandler;
				return this;
			}
		}
		
		public static class PrebuildOption<T,R,E>
		{
			public PrebuildOption(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			protected CommonContentEnricher cce = null;
			
			public PreBuilder<T,R,E> prebuild()
			{
				return new PreBuilder<>(this.cce);
			}
			
			public CommonContentEnricher<T,R,E> buildCommonContentEnricher()
			{
				return new PreBuilder(this.cce).buildCommonContentEnricher();
			}
			
			public LazyContentEnricher<T,R,E> buildLazyContentEnricher()
			{
				return new PreBuilder(this.cce).buildLazyContentEnricher();
			}
		}
		
		public static class SetDefaultCacheOption<T,R,E> extends PrebuildOption<T, R, E>
		{
			public SetDefaultCacheOption(CommonContentEnricher cce)
			{
				super(cce);
			}
			
			public PrebuildOption<T, R, E> defaultCacheSize(int cacheSize)
			{
				if(cacheSize < 0)
				{
					cacheSize = 0;
				}
				super.cce.cacheSize = cacheSize;
				return this;
			}
		}
		
		public static class HandleRemoveFromCacheOption<T,R,E> extends SetDefaultCacheOption<T,R,E> 
		{
			public HandleRemoveFromCacheOption(CommonContentEnricher cce)
			{
				super(cce);
			}
			
			public SetDefaultCacheOption<T,R,E> removeFromCacheHandler(BiConsumer<R,E> removeFromCacheHandler)
			{
				super.cce.removeFromCacheHandler = removeFromCacheHandler;
				return this;
			}
		}
		
		public static class PreBuilder<T,R,E>
		{
			public PreBuilder(CommonContentEnricher cce)
			{
				super();
				this.cce = cce;
			}
			protected CommonContentEnricher cce = null;
			protected int cacheSize = -1;
			protected Map<String,Object> properties = new HashMap();
			
			public Builder<T,R,E> cacheSize(int cacheSize)
			{
				if(cacheSize < 0)
				{
					cacheSize = 0;
				}
				
				Builder<T,R,E> builder = new Builder(this.cce);
				builder.cce.cacheSize = cacheSize;
				return builder;
			}
			
			public Builder<T,R,E> putProperty(String key, Object value)
			{
				return new Builder(this.cce).property(key, value);
			}
			
			public CommonContentEnricher<T,R,E> buildCommonContentEnricher()
			{
				return new Builder(this.cce).buildCommonContentEnricher();
			}
			
			public LazyContentEnricher<T,R,E> buildLazyContentEnricher()
			{
				return new Builder(this.cce).buildLazyContentEnricher();
			}
		}
		
		public static class Builder<T,R,E>
		{
			public Builder(CommonContentEnricher cce)
			{
				super();
				this.cce = new CommonContentEnricher();
				
				this.cce.cardinalityMode = cce.cardinalityMode;
				this.cce.workingMode = cce.workingMode;
				
				this.cce.enrichmentOneCache = cce.enrichmentOneCache;
				this.cce.enrichmentManyCache = cce.enrichmentManyCache;
				
				this.cce.newBlankEnrichmentHandler = cce.newBlankEnrichmentHandler;
				this.cce.cloneEnrichmentHandler = cce.cloneEnrichmentHandler;
				this.cce.linkEnrichmentHandler = cce.linkEnrichmentHandler;
				this.cce.enricheCycleHandler = cce.enricheCycleHandler;
				
				this.cce.removeFromCacheHandler = cce.removeFromCacheHandler;
				this.cce.cacheSize = cce.cacheSize;
				this.cce.properties = new HashMap<>();
			}
			
			protected CommonContentEnricher cce = null;
			
			public Builder<T,R,E> property(String key, Object value)
			{
				Objects.requireNonNull(key, "key no defined");
				
				this.cce.properties.put(key, value);
				return this;
			}
			
			public CommonContentEnricher<T,R,E> buildCommonContentEnricher()
			{
				CommonContentEnricher cce = new CommonContentEnricher();
				
				cce.cardinalityMode = this.cce.cardinalityMode;
				cce.workingMode = this.cce.workingMode;
				
				cce.enrichmentOneCache = this.cce.enrichmentOneCache;
				cce.enrichmentManyCache = this.cce.enrichmentManyCache;
				
				cce.newBlankEnrichmentHandler = this.cce.newBlankEnrichmentHandler;
				cce.cloneEnrichmentHandler = this.cce.cloneEnrichmentHandler;
				cce.linkEnrichmentHandler = this.cce.linkEnrichmentHandler;
				cce.enricheCycleHandler = this.cce.enricheCycleHandler;
				
				cce.removeFromCacheHandler = this.cce.removeFromCacheHandler;
				cce.cacheSize = this.cce.cacheSize;
				cce.properties = new HashMap<>(this.cce.properties);
				
				if((cce.workingMode == WorkingMode.DEDICATED) || (cce.cacheSize > 0))
				{
					if(cce.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_MANY_ENRICHMENTS)
					{
						cce.enrichmentManyCache = new SimpleShrinkableCache<>();
					}
					
					if(cce.cardinalityMode == CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
					{
						cce.enrichmentOneCache = new SimpleShrinkableCache<>();
					}
				}
				
				this.cce.properties.clear();
				
				this.cce.cardinalityMode = null;
				this.cce.workingMode = null;
				
				this.cce.enrichmentOneCache = null;
				this.cce.enrichmentManyCache = null;
				
				this.cce.newBlankEnrichmentHandler = null;
				this.cce.cloneEnrichmentHandler = null;
				this.cce.linkEnrichmentHandler = null;
				this.cce.enricheCycleHandler = null;
				
				this.cce.removeFromCacheHandler = null;
				this.cce.cacheSize = 0;
				this.cce.properties = null;
				
				return cce;
			}
			
			public LazyContentEnricher<T,R,E> buildLazyContentEnricher()
			{
				return LazyContentEnricher.newInstance().defineContentEnricher(buildCommonContentEnricher());
			}
		}
		
	}
	
	public void t()
	{
		CommonContentEnricher.newBuilder()
			.workingMode(WorkingMode.DEDICATED)
			.cardinalityMode(CardinalityMode.ONE_REFERENCE_TO_ONE_ENRICHMENT)
			.typeOfObjectsToBeEnriched(String.class)
			.typeOfReferences(Integer.class)
			.typeOfEnrichments(Long.class)
			.newBlankEnrichmentHandler(cce -> null)
			.cloneEnrichmentHandler(cce -> null)
			.linkEnrichmentHandler(null).enrichCycleHandler(null).removeFromCacheHandler(null).defaultCacheSize(1).prebuild().cacheSize(5).property("dsa", null).property("", null).buildCommonContentEnricher()
			//optional link
			//optional unlink
			// handleEnrichCycle
			// optionallyHandleRemoveFromCache
			// optionallySetDefaultCacheSize
			// prebuild
		;
	}

}
