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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A node modify listener is a high level modify listener. It notifies for modifications of registered nodes or node's values only.
 *
 * @param <T>
 *
 * @author Sebastian Palarus
 */
@FunctionalInterface
public interface IModifyListener<T> extends BiConsumer<T, T>
{
    @Override
    void accept(T newValue, T oldValue);
    
    default boolean isEnabled()
    {
        return true;
    }
    
    default String getNotifyBufferId()
    {
        return null;
    }
    
    default void onListenStart(final T value) { }
    
    default void onListenStop(final T value) { }
    
    static <T> IModifyListener<T> onRemove(final Consumer<T> consumer)
    {
        return new RemoveListener<T>(consumer);
    }
    
    static <T> IModifyListener<T> onCreate(final Consumer<T> consumer)
    {
        return new CreateListener<T>(consumer);
    }
    
    static <T> IModifyListener<T> onModify(final Consumer<T> consumer)
    {
        return new ModifyListener<T>(consumer);
    }
    
    static <T> IModifyListener<T> onUpdate(final BiConsumer<T, T> consumer)
    {
        return new UpdateListener<T>(consumer);
    }
    
    class RemoveListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private RemoveListener(final Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(final T newValue, final T oldValue)
        {
            if ((oldValue != null) && (newValue == null))
            {
                this.consumer.accept(oldValue);
            }
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.consumer == null) ? 0 : this.consumer.hashCode());
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
            RemoveListener other = (RemoveListener) obj;
            if (this.consumer == null)
            {
                return other.consumer == null;
            }
            else
            {
                return this.consumer.equals(other.consumer);
            }
        }
    }
    
    class CreateListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private CreateListener(final Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(final T newValue, final T oldValue)
        {
            if ((oldValue == null) && (newValue != null))
            {
                this.consumer.accept(newValue);
            }
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.consumer == null) ? 0 : this.consumer.hashCode());
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
            CreateListener other = (CreateListener) obj;
            if (this.consumer == null)
            {
                return other.consumer == null;
            }
            else
            {
                return this.consumer.equals(other.consumer);
            }
        }
    }
    
    class UpdateListener<T> implements IModifyListener<T>
    {
        private BiConsumer<T, T> consumer = null;
        
        private UpdateListener(final BiConsumer<T, T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(final T newValue, final T oldValue)
        {
            if ((oldValue != null) && (newValue != null) && (!oldValue.equals(newValue)))
            {
                this.consumer.accept(oldValue, newValue);
            }
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.consumer == null) ? 0 : this.consumer.hashCode());
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
            UpdateListener other = (UpdateListener) obj;
            if (this.consumer == null)
            {
                return other.consumer == null;
            }
            else
            {
                return this.consumer.equals(other.consumer);
            }
        }
    }
    
    class ModifyListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private ModifyListener(final Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(final T newValue, final T oldValue)
        {
            this.consumer.accept(newValue);
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.consumer == null) ? 0 : this.consumer.hashCode());
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
            ModifyListener other = (ModifyListener) obj;
            if (this.consumer == null)
            {
                return other.consumer == null;
            }
            else
            {
                return this.consumer.equals(other.consumer);
            }
        }
    }
}
