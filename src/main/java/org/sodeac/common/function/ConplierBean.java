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
package org.sodeac.common.function;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A mutable single value container provides access by functional interfaces {@link Supplier} with {@link Consumer} and classic java bean pattern.
 * 
 * <p>
 * Not Thread save!!!
 * 
 * @author Sebastian Palarus
 *
 * @param <T>
 */
public class ConplierBean<T> implements Supplier<T>,Consumer<T>
{
	/**
	 * Constructor creates a conplier bean without initial value;
	 */
	public ConplierBean()
	{
		super();
	}
	
	/**
	 * Constructor creates a conplier bean with initial value;
	 * 
	 * @param initialValue initial value
	 */
	public ConplierBean(T initialValue)
	{
		super();
		this.value = initialValue;
	}
	
	private PropertyChangeSupport changes = new PropertyChangeSupport( this );
	private volatile T value = null;
	private volatile boolean equalsBySameValue = false;
	
	@Override
	public void accept(T t)
	{
		this.setValue(t);
	}

	@Override
	public T get()
	{
		return this.getValue();
	}
	
	/**
	 * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener)
	 * 
	 * @param propertyChangeListener
	 */
	public void addPropertyChangeListener( PropertyChangeListener propertyChangeListener )
	{
		changes.addPropertyChangeListener( propertyChangeListener );
	}

	/**
	 * {@link java.beans.PropertyChangeSupport#removePropertyChangeListener(PropertyChangeListener)}
	 * 
	 * @param propertyChangeListener
	 */
	public void removePropertyChangeListener( PropertyChangeListener propertyChangeListener )
	{
		changes.removePropertyChangeListener( propertyChangeListener );
	}

	/**
	 * bean like getter
	 * 
	 * @return
	 */
	public T getValue()
	{
		return this.value;
	}
	
	/**
	 * bean like setter
	 * 
	 * @param value
	 */
	public void setValue(T value)
	{
		T oldValue = this.value;
		this.value = value;
		changes.firePropertyChange( "value", oldValue, this.value );
	}
	
	/**
	 * get value as {@link Optional}
	 * 
	 * @return value as {@link Optional}
	 */
	public Optional<T> getOptional()
	{
		return Optional.ofNullable(this.value);
	}
	
	/**
	 * 
	 * supplies a consumer with value of conplier bean
	 * 
	 * @param consumer consume value of conplier bean
	 * 
	 * @return conplier
	 */
	public ConplierBean<T> supply(Consumer<T> consumer)
	{
		consumer.accept(this.value);
		return this;
	}
	
	/**
	 * set new value by supplier
	 * 
	 * @param supplier supplies to set new value
	 */
	public ConplierBean<T> consume(Supplier<T> supplier)
	{
		this.setValue(supplier.get());
		return this;
	}

	/**
	 * applies value of conplier bean to an unary operator and returns operators result
	 * 
	 * @param operator operator to apply value of conplier bean
	 * @return result of operator
	 */
	public T unaryOperate(UnaryOperator<T> operator)
	{
		this.value = operator.apply(value);
		return this.value;
	}
	
	/**
	 * applies value of conplier bean as first operand and a parameterized second operand to an binary operator and returns operators result
	 * 
	 * @param operator operator to apply value of conplier bean as first operand and a second operand
	 * @param secondOperand second operand
	 * @return result of operator
	 */
	public T binaryOperate(BinaryOperator<T> operator, T secondOperand)
	{
		this.value = operator.apply(value,secondOperand);
		return this.value;
	}	
	
	/**
	 * tests value of conplier bean by {@link Predicate}
	 * 
	 * @param predicate predicate to test value of conplier bean
	 * @return result of predicate test
	 */
	public boolean test(Predicate<T> predicate)
	{
		return predicate.test(value);
	}
	
	/**
	 * creates a {@link PropertyChangeListener} delegates all value changes to parameterized {@link Consumer}
	 * 
	 * @param consumer consumer to consume all value changes
	 * 
	 * @return {@link ConplierBean.ConsumeOnChangeListener} to be able to stop consuming by {@link ConplierBean.ConsumeOnChangeListener#unregister()}
	 */
	public ConsumeOnChangeListener consumeOnChange(Consumer<T> consumer)
	{
		ConsumeOnChangeListener listener = new ConsumeOnChangeListener(consumer);
		this.addPropertyChangeListener(listener);
		return listener;
	}
	
	/**
	 * {@link PropertyChangeListener} delegates all value changes to parameterized {@link Consumer}
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public class ConsumeOnChangeListener implements PropertyChangeListener
	{
		private Consumer<T> consumer;
		
		/**
		 * constructor with parameterized {@link Consumer} to delegate all value changes
		 * @param consumer {@link Consumer} to delegate all value changes
		 */
		public ConsumeOnChangeListener(Consumer<T> consumer)
		{
			super();
			this.consumer = consumer;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			consumer.accept((T)evt.getNewValue());
		}
		
		/**
		 * stop to delegate value changes by unregister this {@link PropertyChangeListener}
		 */
		public void unregister()
		{
			ConplierBean.this.removePropertyChangeListener(this);
		}
	}
	
	/**
	 * Helps GC and make this ConplierBean unusable.
	 */
	public void dispose()
	{
		if(changes != null)
		{
			for(PropertyChangeListener listener : changes.getPropertyChangeListeners())
			{
				this.changes.removePropertyChangeListener(listener);
			}
		}
		
		this.value = null;
		this.changes = null;
	}

	/**
	 * Property for equals-behavior. If true, the equals method returns true, if other conplierBean wraps same object, otherwise false.
	 * 
	 * @param equalsBySameValue if true, the equals method returns true, if other conplierBean wraps same object, therwise false
	 * @return this conplier bean
	 */
	public ConplierBean<T> setEqualsBySameValue(boolean equalsBySameValue)
	{
		this.equalsBySameValue = equalsBySameValue;
		return this;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConplierBean other = (ConplierBean) obj;
		
		if(this.equalsBySameValue)
		{
			return this.value == other.value;
		}
		
		if (value == null)
		{
			if (other.value != null)
			{
				return false;
			}
		} 
		else if (!value.equals(other.value))
		{
			return false;
		}
		return true;
	}
	 
}
