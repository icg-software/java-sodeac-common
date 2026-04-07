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
    
    public V put(K key, V value)
    {
        this.accessIndex.put(key, this.accessSequence++);
        return this.map.put(key, value);
    }
    
    public V get(K key)
    {
        this.accessIndex.put(key, this.accessSequence++);
        return this.map.get(key);
    }
    
    public V remove(K key)
    {
        this.accessIndex.remove(key);
        return this.map.remove(key);
    }
    
    public boolean containsKey(K key)
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
        return view;
    }
    
    public void shrink(int maxSize, BiConsumer<K, V> removedEntriesConsumer)
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
        
        for (Map.Entry<K, Long> entry : entries)
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
        catch (Exception e) { }
    }
    
    public static class ThreadSafeShrinkableCache<K, V> extends SimpleShrinkableCache<K, V>
    {
        public ThreadSafeShrinkableCache()
        {
            super();
            this.lock = new ReentrantLock();
        }
        
        private Lock lock = null;
        
        public V put(K key, V value)
        {
            lock.lock();
            try
            {
                return super.put(key, value);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        public V get(K key)
        {
            lock.lock();
            try
            {
                return super.get(key);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        public V remove(K key)
        {
            lock.lock();
            try
            {
                return super.remove(key);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        public void clear()
        {
            lock.lock();
            try
            {
                super.clear();
            }
            finally
            {
                lock.unlock();
            }
        }
        
        public boolean containsKey(K key)
        {
            lock.lock();
            try
            {
                return super.containsKey(key);
            }
            finally
            {
                lock.unlock();
            }
        }
        
        public void shrink(int maxSize, BiConsumer<K, V> removedEntriesConsumer)
        {
            lock.lock();
            try
            {
                super.shrink(maxSize, removedEntriesConsumer);
            }
            finally
            {
                lock.unlock();
            }
        }
    }
    
}
