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
    public void accept(T newValue, T oldValue);
    
    public default boolean isEnabled()
    {
        return true;
    }
    
    public default String getNotifyBufferId()
    {
        return null;
    }
    
    public default void onListenStart(T value) { }
    
    public default void onListenStop(T value) { }
    
    public static <T> IModifyListener<T> onRemove(Consumer<T> consumer)
    {
        return new RemoveListener<T>(consumer);
    }
    
    public static <T> IModifyListener<T> onCreate(Consumer<T> consumer)
    {
        return new CreateListener<T>(consumer);
    }
    
    public static <T> IModifyListener<T> onModify(Consumer<T> consumer)
    {
        return new ModifyListener<T>(consumer);
    }
    
    public static <T> IModifyListener<T> onUpdate(BiConsumer<T, T> consumer)
    {
        return new UpdateListener<T>(consumer);
    }
    
    public class RemoveListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private RemoveListener(Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(T newValue, T oldValue)
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
            result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
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
            if (consumer == null)
            {
                if (other.consumer != null)
                {
                    return false;
                }
            }
            else if (!consumer.equals(other.consumer))
            {
                return false;
            }
            return true;
        }
    }
    
    public class CreateListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private CreateListener(Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(T newValue, T oldValue)
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
            result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
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
            if (consumer == null)
            {
                if (other.consumer != null)
                {
                    return false;
                }
            }
            else if (!consumer.equals(other.consumer))
            {
                return false;
            }
            return true;
        }
    }
    
    public class UpdateListener<T> implements IModifyListener<T>
    {
        private BiConsumer<T, T> consumer = null;
        
        private UpdateListener(BiConsumer<T, T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(T newValue, T oldValue)
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
            result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
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
            if (consumer == null)
            {
                if (other.consumer != null)
                {
                    return false;
                }
            }
            else if (!consumer.equals(other.consumer))
            {
                return false;
            }
            return true;
        }
    }
    
    public class ModifyListener<T> implements IModifyListener<T>
    {
        private Consumer<T> consumer = null;
        
        private ModifyListener(Consumer<T> consumer)
        {
            super();
            this.consumer = consumer;
        }
        
        @Override
        public void accept(T newValue, T oldValue)
        {
            this.consumer.accept(newValue);
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
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
            if (consumer == null)
            {
                if (other.consumer != null)
                {
                    return false;
                }
            }
            else if (!consumer.equals(other.consumer))
            {
                return false;
            }
            return true;
        }
    }
}
