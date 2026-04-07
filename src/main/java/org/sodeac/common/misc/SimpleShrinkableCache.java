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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class SimpleShrinkableCache<K, V>
{
    public SimpleShrinkableCache()
    {
        super();
        this.map = new HashMap<>();
        this.accessIndex = new HashMap<>();
        this.view = Collections.unmodifiableMap(this.map);
    }
    
    private long accessSequence = Long.MIN_VALUE;
    private Map<K, V> map = null;
    private Map<K, Long> accessIndex = null;
    private Map<K, V> view = null;
    
    public V put(final K key, final V value)
    {
        this.accessIndex.put(key, this.accessSequence++);
        return this.map.put(key, value);
    }
    
    public V get(final K key)
    {
        this.accessIndex.put(key, this.accessSequence++);
        return this.map.get(key);
    }
    
    public V remove(final K key)
    {
        this.accessIndex.remove(key);
        return this.map.remove(key);
    }
    
    public boolean containsKey(final K key)
    {
        return this.map.containsKey(key);
    }
    
    public void clear()
    {
        this.accessSequence = Long.MIN_VALUE;
        this.map.clear();
        this.accessIndex.clear();
    }
    
    public Map<K, V> getView()
    {
        return this.view;
    }
    
    public void shrink(int maxSize, final BiConsumer<K, V> removedEntriesConsumer)
    {
        if (maxSize < 0)
        {
            maxSize = 0;
        }
        if (this.map.size() <= maxSize)
        {
            return;
        }
        
        List<Map.Entry<K, Long>> entries = new ArrayList<>(this.accessIndex.entrySet());
        
        Collections.sort(entries, (e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
        
        for (final Map.Entry<K, Long> entry : entries)
        {
            if (maxSize-- > 0)
            {
                continue;
            }
            
            if (removedEntriesConsumer != null)
            {
                try
                {
                    removedEntriesConsumer.accept(entry.getKey(), this.map.get(entry.getKey()));
                }
                catch (Exception | Error e) { }
            }
            
            this.remove(entry.getKey());
        }
        
        try
        {
            entries.clear();
        }
        catch (final Exception e) { }
    }
    
    public static class ThreadSafeShrinkableCache<K, V> extends SimpleShrinkableCache<K, V>
    {
        public ThreadSafeShrinkableCache()
        {
            super();
            this.lock = new ReentrantLock();
        }
        
        private Lock lock = null;
        
        @Override
        public V put(final K key, final V value)
        {
            this.lock.lock();
            try
            {
                return super.put(key, value);
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public V get(final K key)
        {
            this.lock.lock();
            try
            {
                return super.get(key);
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public V remove(final K key)
        {
            this.lock.lock();
            try
            {
                return super.remove(key);
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public void clear()
        {
            this.lock.lock();
            try
            {
                super.clear();
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public boolean containsKey(final K key)
        {
            this.lock.lock();
            try
            {
                return super.containsKey(key);
            }
            finally
            {
                this.lock.unlock();
            }
        }
        
        @Override
        public void shrink(final int maxSize, final BiConsumer<K, V> removedEntriesConsumer)
        {
            this.lock.lock();
            try
            {
                super.shrink(maxSize, removedEntriesConsumer);
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }
    
}
