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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Embedded version of Guava's transformed list. This is an one-way view of original list, so it is not possible to add new items.
 * 
 * @see <a href="https://guava.dev/releases/23.0/api/docs/com/google/common/collect/Lists.html#transform-java.util.List-com.google.common.base.Function-">Guava API Docs</a>
 *
 */
public class TransformedList
{
	/**
	 * @see <a href="https://guava.dev/releases/23.0/api/docs/com/google/common/collect/Lists.html#transform-java.util.List-com.google.common.base.Function-">Guava API Docs</a>
	 * 
	 * @param fromList original list
	 * @param function transform function
	 * @return transformed view
	 */
	public static <F, T> List<T> createView(List<F> fromList, Function<? super F, ? extends T> function) 
	{
		return (fromList instanceof RandomAccess)
			? new GuavasTransformingRandomAccessList<>(fromList, function)
			: new GuavasTransformingSequentialList<>(fromList, function);
	}
	
	private static class GuavasTransformingRandomAccessList<F, T> extends AbstractList<T> implements RandomAccess, Serializable 
	{
		final List<F> fromList;
		final Function<? super F, ? extends T> function;
		
		GuavasTransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) 
		{
			Objects.requireNonNull(fromList);
			Objects.requireNonNull(function);
			this.fromList = fromList;
			this.function = function;
		}
		
		@Override
		public void clear() 
		{
			fromList.clear();
		}
		
		@Override
		public T get(int index) 
		{
			return function.apply(fromList.get(index));
		}
		
		@Override
		public Iterator<T> iterator() 
		{
			return listIterator();
		}
		
		@Override
		public ListIterator<T> listIterator(int index) 
		{
			return new GuavasTransformedListIterator<F, T>(fromList.listIterator(index)) 
			{
				@Override
				T transform(F from) 
				{
					return function.apply(from);
				}
			};
		}
		
		@Override
		public boolean isEmpty() 
		{
			return fromList.isEmpty();
		}
		
		@Override
		public boolean removeIf(Predicate<? super T> filter) 
		{
			Objects.requireNonNull(filter);
			return fromList.removeIf(element -> filter.test(function.apply(element)));
		}
		
		@Override
		public T remove(int index) 
		{
			return function.apply(fromList.remove(index));
		}
		
		@Override
		public int size() 
		{
			return fromList.size();
		}
		
		private static final long serialVersionUID = 0;
	}
	
	private static class GuavasTransformingSequentialList<F, T> extends AbstractSequentialList<T> implements Serializable 
	{
		final List<F> fromList;
		final Function<? super F, ? extends T> function;
		
		GuavasTransformingSequentialList(List<F> fromList, Function<? super F, ? extends T> function) 
		{
			Objects.requireNonNull(fromList);
			Objects.requireNonNull(function);
			this.fromList = fromList;
			this.function = function;
		}
		
		/**
		 * The default implementation inherited is based on iteration and removal of each element which
		 * can be overkill. That's why we forward this call directly to the backing list.
		 */
		@Override
		public void clear() 
		{
			fromList.clear();
		}
		
		@Override
		public int size() 
		{
			return fromList.size();
		}
		
		@Override
		public ListIterator<T> listIterator(final int index) 
		{
			return new GuavasTransformedListIterator<F, T>(fromList.listIterator(index)) 
			{
				@Override
				T transform(F from) 
				{
					return function.apply(from);
				}
			};
		}
		
		@Override
		public boolean removeIf(Predicate<? super T> filter) 
		{
			Objects.requireNonNull(filter);
			return fromList.removeIf(element -> filter.test(function.apply(element)));
		}
		
		private static final long serialVersionUID = 0;
	}
	
	private abstract static class GuavasTransformedIterator<F, T> implements Iterator<T>
	{
		final Iterator<? extends F> backingIterator;
		
		GuavasTransformedIterator(Iterator<? extends F> backingIterator) 
		{
			Objects.requireNonNull(backingIterator);
			this.backingIterator = backingIterator;
			
		}
		
		abstract T transform(F from);
		
		@Override
		public final boolean hasNext() 
		{
			return backingIterator.hasNext();
		}
		
		@Override
		public final T next() 
		{
			return transform(backingIterator.next());
		}
		
		@Override
		public final void remove() 
		{
			backingIterator.remove();
		}
	}
	
	private abstract static class GuavasTransformedListIterator<F, T> extends GuavasTransformedIterator<F, T> implements ListIterator<T> 
	{
		GuavasTransformedListIterator(ListIterator<? extends F> backingIterator) 
		{
			super(backingIterator);
		}
		
		private ListIterator<? extends F> backingIterator() 
		{
			return (ListIterator)backingIterator;
		}
		
		@Override
		public final boolean hasPrevious() 
		{
			return backingIterator().hasPrevious();
		}
		
		@Override
		public final T previous() 
		{
			return transform(backingIterator().previous());
		}
		
		@Override
		public final int nextIndex() 
		{
			return backingIterator().nextIndex();
		}
		
		@Override
		public final int previousIndex() 
		{
			return backingIterator().previousIndex();
		}
		
		@Override
		public void set(T element) 
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void add(T element) 
		{
			throw new UnsupportedOperationException();
		}
	}
}
