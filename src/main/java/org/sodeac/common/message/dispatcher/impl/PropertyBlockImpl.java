/*******************************************************************************
 * Copyright (c) 2017, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;

import org.sodeac.common.message.dispatcher.api.IPropertyBlock;
import org.sodeac.common.message.dispatcher.api.IPropertyBlockAtomicProcedure;
import org.sodeac.common.message.dispatcher.api.IPropertyBlockModifyListener;
import org.sodeac.common.message.dispatcher.api.IPropertyLock;
import org.sodeac.common.message.dispatcher.api.PropertyBlockModifyItem;
import org.sodeac.common.message.dispatcher.api.PropertyIsLockedException;
import org.sodeac.common.xuri.ldapfilter.DefaultMatchableWrapper;
import org.sodeac.common.xuri.ldapfilter.IMatchable;

public class PropertyBlockImpl implements IPropertyBlock
{
    protected PropertyBlockImpl(final MessageDispatcherImpl dispatcher)
    {
        super();
        
        this.propertiesLock = new ReentrantReadWriteLock(true);
        this.propertiesReadLock = this.propertiesLock.readLock();
        this.propertiesWriteLock = this.propertiesLock.writeLock();
        
        this.lockedProperties = null;
        
        this.dispatcher = dispatcher;
    }
    
    public static final Map<String, Object> EMPTY_PROPERTIES = Collections.emptyMap();
    public static final Set<String> EMPTY_KEYSET = Collections.emptySet();
    public static final List<IPropertyBlockModifyListener> EMPTY_MODIFY_LISTENER = Collections.emptyList();
    
    private volatile List<IPropertyBlockModifyListener> modifyListenerList = null;
    private volatile List<IPropertyBlockModifyListener> modifyListenerListCopy = null;
    
    private Map<String, Object> properties;
    private Map<String, Object> propertiesCopy;
    private Map<String, IMatchable> matchables;
    private Set<String> keySet;
    
    private final ReentrantReadWriteLock propertiesLock;
    private final ReadLock propertiesReadLock;
    private final WriteLock propertiesWriteLock;
    
    private Map<String, UUID> lockedProperties;
    
    private final MessageDispatcherImpl dispatcher;
    
    @Override
    public Object setProperty(final String key, final Object value)
    {
        Object old = null;
        IPropertyBlockModifyListener.ModifyType modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
        List<IPropertyBlockModifyListener> listenerList = null;
        
        this.propertiesWriteLock.lock();
        try
        {
            if ((this.lockedProperties != null) && (this.lockedProperties.get(key) != null))
            {
                throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
            }
            if (this.properties == null)
            {
                this.properties = new HashMap<String, Object>();
            }
            else
            {
                if (this.properties.containsKey(key))
                {
                    modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
                }
                old = this.properties.get(key);
            }
            this.properties.put(key, value);
            this.propertiesCopy = null;
            this.matchables = null;
            this.keySet = null;
            listenerList = getModifyListenerList();
            
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        
        if ((listenerList != null) && (!listenerList.isEmpty()))
        {
            try
            {
                for (final IPropertyBlockModifyListener listener : listenerList)
                {
                    try
                    {
                        listener.onModify(modifyType, key, old, value);
                    }
                    catch (final Exception e)
                    {
                        if (this.dispatcher != null)
                        {
                            this.dispatcher.logError("execute property modify listener (update/insert)", e);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                if (this.dispatcher != null)
                {
                    this.dispatcher.logError("execute property modify listener list (update/insert)", e);
                }
            }
        }
        return old;
    }
    
    @Override
    public Map<String, Object> setPropertyEntrySet(final Set<Entry<String, Object>> propertyEntrySet, final boolean ignoreIfEquals)
    {
        if (propertyEntrySet == null)
        {
            return EMPTY_PROPERTIES;
        }
        
        if (propertyEntrySet.isEmpty())
        {
            return EMPTY_PROPERTIES;
        }
        
        Map<String, Object> oldValues = null;
        List<PropertyBlockModifyItem> modifyList = null;
        List<IPropertyBlockModifyListener> listenerList = null;
        
        this.propertiesWriteLock.lock();
        try
        {
            if (this.lockedProperties != null)
            {
                for (final Entry<String, Object> entry : propertyEntrySet)
                {
                    if (this.lockedProperties.get(entry.getKey()) != null)
                    {
                        throw new PropertyIsLockedException("writable access to \"" + entry.getKey() + "\" denied by lock");
                    }
                }
            }
            
            if (this.properties == null)
            {
                this.properties = new HashMap<String, Object>();
            }
            
            IPropertyBlockModifyListener.ModifyType modifyType;
            String key;
            Object oldValue;
            Object newValue;
            boolean update;
            
            for (final Entry<String, Object> propertyEntry : propertyEntrySet)
            {
                if (this.properties.containsKey(propertyEntry.getKey()))
                {
                    modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
                }
                else
                {
                    modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
                }
                
                key = propertyEntry.getKey();
                oldValue = this.properties.get(key);
                newValue = propertyEntry.getValue();
                
                update = !ignoreIfEquals;
                if (ignoreIfEquals)
                {
                    if
                    (
                        ((oldValue == null) && (newValue != null)) ||
                        ((oldValue != null) && (newValue == null))
                    )
                    {
                        update = true;
                    }
                    else if ((oldValue == null) && (newValue != null))
                    {
                        continue;
                    }
                    else if (oldValue.equals(newValue))
                    {
                        continue;
                    }
                }
                
                if (update)
                {
                    if (modifyList == null)
                    {
                        oldValues = new HashMap<String, Object>();
                        modifyList = new ArrayList<PropertyBlockModifyItem>();
                    }
                    modifyList.add(new PropertyBlockModifyItem(modifyType, key, oldValue, newValue));
                    oldValues.put(key, oldValue);
                    this.properties.put(key, newValue);
                }
            }
            
            if (modifyList != null)
            {
                this.propertiesCopy = null;
                this.matchables = null;
                this.keySet = null;
                listenerList = getModifyListenerList();
            }
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        
        if (modifyList == null)
        {
            return EMPTY_PROPERTIES;
        }
        
        if ((listenerList != null) && (!listenerList.isEmpty()))
        {
            try
            {
                for (final IPropertyBlockModifyListener listener : listenerList)
                {
                    try
                    {
                        listener.onModifySet(modifyList);
                    }
                    catch (final Exception e)
                    {
                        if (this.dispatcher != null)
                        {
                            this.dispatcher.logError("execute property modify listener (update/insert set)", e);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                if (this.dispatcher != null)
                {
                    this.dispatcher.logError("execute property modify listener list (update/insert set)", e);
                }
            }
        }
        
        return oldValues;
    }
    
    @Override
    public Object getProperty(final String key)
    {
        if (this.properties == null)
        {
            return null;
        }
        
        try
        {
            this.propertiesReadLock.lock();
            return this.properties.get(key);
        }
        finally
        {
            this.propertiesReadLock.unlock();
        }
    }
    
    @Override
    public Object removeProperty(final String key)
    {
        if (this.properties == null)
        {
            return null;
        }
        
        try
        {
            this.propertiesReadLock.lock();
            if (!this.properties.containsKey(key))
            {
                return null;
            }
        }
        finally
        {
            this.propertiesReadLock.unlock();
        }
        
        Object oldPropertyValue = null;
        List<IPropertyBlockModifyListener> listenerList = null;
        
        try
        {
            this.propertiesWriteLock.lock();
            
            if ((this.lockedProperties != null) && (this.lockedProperties.get(key) != null))
            {
                throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
            }
            
            if (!this.properties.containsKey(key))
            {
                return null;
            }
            
            oldPropertyValue = this.properties.get(key);
            
            this.properties.remove(key);
            this.propertiesCopy = null;
            this.matchables = null;
            this.keySet = null;
            listenerList = getModifyListenerList();
            
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        
        if ((listenerList != null) && (!listenerList.isEmpty()))
        {
            try
            {
                for (final IPropertyBlockModifyListener listener : listenerList)
                {
                    try
                    {
                        listener.onModify(IPropertyBlockModifyListener.ModifyType.REMOVE, key, oldPropertyValue, null);
                    }
                    catch (final Exception e)
                    {
                        if (this.dispatcher != null)
                        {
                            this.dispatcher.logError("execute property modify listener (remove)", e);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                if (this.dispatcher != null)
                {
                    this.dispatcher.logError("execute property modify listener list (remove)", e);
                }
            }
        }
        
        return oldPropertyValue;
    }
    
    @Override
    public Set<String> getPropertyKeySet()
    {
        if (this.properties == null)
        {
            return EMPTY_KEYSET;
        }
        
        try
        {
            this.propertiesReadLock.lock();
            if (this.keySet == null)
            {
                this.keySet = Collections.unmodifiableSet(this.properties.keySet());
            }
            return this.keySet;
        }
        finally
        {
            this.propertiesReadLock.unlock();
        }
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        if (this.properties == null)
        {
            return EMPTY_PROPERTIES;
        }
        
        Map<String, Object> props = this.propertiesCopy;
        if (props == null)
        {
            this.propertiesWriteLock.lock();
            try
            {
                if (this.properties == null)
                {
                    return EMPTY_PROPERTIES;
                }
                this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String, Object>(this.properties));
                props = this.propertiesCopy;
            }
            finally
            {
                this.propertiesWriteLock.unlock();
            }
        }
        return props;
    }
    
    public Map<String, IMatchable> getMatchables()
    {
        if (this.properties == null)
        {
            return null;
        }
        
        Map<String, IMatchable> props = this.matchables;
        if (props == null)
        {
            this.propertiesWriteLock.lock();
            try
            {
                if (this.properties == null)
                {
                    return null;
                }
                props = new HashMap<String, IMatchable>();
                for (final Entry<String, Object> entry : this.properties.entrySet())
                {
                    props.put(entry.getKey(), new DefaultMatchableWrapper(entry.getValue()));
                }
                this.matchables = props;
            }
            finally
            {
                this.propertiesWriteLock.unlock();
            }
        }
        return props;
    }
    
    @Override
    public Map<String, Object> clear()
    {
        Map<String, Object> oldValues = null;
        
        if (this.properties == null)
        {
            return EMPTY_PROPERTIES;
        }
        
        List<IPropertyBlockModifyListener> listenerList = null;
        List<PropertyBlockModifyItem> modifyList = null;
        
        this.propertiesWriteLock.lock();
        try
        {
            if ((this.lockedProperties != null) && (!this.lockedProperties.isEmpty()))
            {
                throw new PropertyIsLockedException("clear failed. property block as locks");
            }
            
            if (this.properties == null)
            {
                return EMPTY_PROPERTIES;
            }
            
            if (this.properties.isEmpty())
            {
                return EMPTY_PROPERTIES;
            }
            
            modifyList = new ArrayList<PropertyBlockModifyItem>();
            
            oldValues = new HashMap<>(this.properties);
            
            listenerList = getModifyListenerList();
            
            for (final Entry<String, Object> oldEntry : oldValues.entrySet())
            {
                modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null));
            }
            
            this.keySet = null;
            this.properties.clear();
            this.propertiesCopy = null;
            this.matchables = null;
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        
        if ((listenerList != null) && (!listenerList.isEmpty()))
        {
            try
            {
                for (final IPropertyBlockModifyListener listener : listenerList)
                {
                    try
                    {
                        listener.onModifySet(modifyList);
                    }
                    catch (final Exception e)
                    {
                        if (this.dispatcher != null)
                        {
                            this.dispatcher.logError("execute property modify listener (clear)", e);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                if (this.dispatcher != null)
                {
                    this.dispatcher.logError("execute property modify listener list (clear)", e);
                }
            }
        }
        return oldValues;
    }
    
    @Override
    public void addModifyListener(final IPropertyBlockModifyListener listener)
    {
        this.propertiesWriteLock.lock();
        try
        {
            if (this.modifyListenerList == null)
            {
                this.modifyListenerList = new ArrayList<>();
            }
            for (final IPropertyBlockModifyListener listenerExists : this.modifyListenerList)
            {
                if (listenerExists == listener)
                {
                    return;
                }
            }
            this.modifyListenerList.add(listener);
            this.modifyListenerList = new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);
            
            List<IPropertyBlockModifyListener> modifyListenerListCopy = this.modifyListenerListCopy;
            if ((modifyListenerListCopy != null) && (!modifyListenerListCopy.isEmpty()))
            {
                try
                {
                    // TODO doit in future -- this.modifyListenerListCopy.clear();
                }
                catch (final Exception e) { }
            }
            this.modifyListenerListCopy = null;
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
    }
    
    @Override
    public void removeModifyListener(final IPropertyBlockModifyListener listener)
    {
        this.propertiesWriteLock.lock();
        try
        {
            if (this.modifyListenerList == null)
            {
                return;
            }
            
            while (this.modifyListenerList.remove(listener)) { }
            
            this.modifyListenerList = new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);
            
            List<IPropertyBlockModifyListener> modifyListenerListCopy = this.modifyListenerListCopy;
            if ((modifyListenerListCopy != null) && (!modifyListenerListCopy.isEmpty()))
            {
                try
                {
                    // TODO doit in future -- this.modifyListenerListCopy.clear();
                }
                catch (final Exception e) { }
            }
            this.modifyListenerListCopy = null;
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
    }
    
    @Override
    public void dispose()
    {
        this.propertiesWriteLock.lock();
        try
        {
            if (this.modifyListenerList != null)
            {
                try
                {
                    if (!this.modifyListenerList.isEmpty())
                    {
                        this.modifyListenerList.clear();
                    }
                }
                catch (final Exception e) { }
                this.modifyListenerList = null;
            }
            this.modifyListenerListCopy = null;
            this.keySet = null;
            if (this.properties != null)
            {
                try
                {
                    this.properties.clear();
                }
                catch (final Exception e) { }
                this.properties = null;
            }
            this.propertiesCopy = null;
            this.matchables = null;
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
    }
    
    @Override
    public boolean isEmpty()
    {
        if (this.properties == null)
        {
            return false;
        }
        
        try
        {
            this.propertiesReadLock.lock();
            if (this.properties == null)
            {
                return false;
            }
            return this.properties.isEmpty();
        }
        finally
        {
            this.propertiesReadLock.unlock();
        }
    }
    
    @Override
    public boolean containsKey(final Object key)
    {
        if (this.properties == null)
        {
            return false;
        }
        
        try
        {
            this.propertiesReadLock.lock();
            if (this.properties == null)
            {
                return false;
            }
            return this.properties.containsKey(key);
        }
        finally
        {
            this.propertiesReadLock.unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(final String key, final Class<T> resultClass)
    {
        return (T) getProperty(key);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPropertyOrDefault(final String key, final Class<T> resultClass, final T defaultValue)
    {
        T typedValue = defaultValue;
        Object current = getProperty(key);
        if (current != null)
        {
            typedValue = (T) current;
        }
        else
        {
            typedValue = defaultValue;
        }
        return typedValue;
    }
    
    @Override
    public String getPropertyOrDefaultAsString(final String key, final String defaultValue)
    {
        String stringValue = defaultValue;
        Object current = getProperty(key);
        if (current != null)
        {
            if (!(current instanceof String))
            {
                current = current.toString();
            }
        }
        if ((current != null) && (!((String) current).isEmpty()))
        {
            stringValue = (String) current;
        }
        else
        {
            stringValue = defaultValue;
        }
        return stringValue;
    }
    
    @Override
    public IPropertyLock lockProperty(final String key)
    {
        if (key == null)
        {
            return null;
        }
        
        if (key.isEmpty())
        {
            return null;
        }
        
        this.propertiesWriteLock.lock();
        try
        {
            if (this.lockedProperties == null)
            {
                this.lockedProperties = new HashMap<String, UUID>();
            }
            else if (this.lockedProperties.get(key) != null)
            {
                return null;
            }
            
            UUID pin = UUID.randomUUID();
            this.lockedProperties.put(key, pin);
            return new PropertyLockImpl(this, key, pin);
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
    }
    
    protected void unlockAllProperties()
    {
        this.propertiesWriteLock.lock();
        try
        {
            this.lockedProperties = null;
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
    }
    
    protected boolean unlockProperty(final PropertyLockImpl lock)
    {
        if (lock == null)
        {
            return false;
        }
        if (this != lock.getBlock())
        {
            return false;
        }
        if ((lock.getKey() == null) || lock.getKey().isEmpty())
        {
            return false;
        }
        if (lock.getPin() == null)
        {
            return false;
        }
        this.propertiesWriteLock.lock();
        try
        {
            if (this.lockedProperties == null)
            {
                return true;
            }
            UUID currentPin = this.lockedProperties.get(lock.getKey());
            if ((currentPin == null) || currentPin.equals(lock.getPin()))
            {
                this.lockedProperties.remove(lock.getKey());
                return true;
            }
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        return false;
    }
    
    @Override
    public Supplier<List<PropertyBlockModifyItem>> computeProcedure(final IPropertyBlockAtomicProcedure operationHandler)
    {
        UnlockedWrapper wrapper = new UnlockedWrapper();
        List<IPropertyBlockModifyListener> listenerList = null;
        
        this.propertiesWriteLock.lock();
        try
        {
            operationHandler.accept(wrapper);
            
            if ((wrapper.modifyList != null) && (!wrapper.modifyList.isEmpty()) && (!((this.modifyListenerList == null) || this.modifyListenerList.isEmpty())))
            {
                listenerList = getModifyListenerList();
            }
            if (wrapper.modifyList != null)
            {
                wrapper.modifyList = Collections.unmodifiableList(wrapper.modifyList);
            }
        }
        finally
        {
            wrapper.valid.set(false);
            this.propertiesWriteLock.unlock();
        }
        
        if ((listenerList != null) && (!listenerList.isEmpty()))
        {
            for (final IPropertyBlockModifyListener listener : listenerList)
            {
                listener.onModifySet(wrapper.modifyList);
            }
        }
        
        return new PropertyBlockProcedureModifyAuditTrail(wrapper.modifyList);
    }
    
    private class PropertyBlockProcedureModifyAuditTrail implements Supplier<List<PropertyBlockModifyItem>>
    {
        private List<PropertyBlockModifyItem> modifyList = null;
        
        public PropertyBlockProcedureModifyAuditTrail(final List<PropertyBlockModifyItem> modifyList)
        {
            super();
            this.modifyList = modifyList;
        }
        
        @Override
        public List<PropertyBlockModifyItem> get()
        {
            return this.modifyList;
        }
        
    }
    
    private class UnlockedWrapper implements IPropertyBlock
    {
        private List<PropertyBlockModifyItem> modifyList = null;
        private AtomicBoolean valid = null;
        
        public UnlockedWrapper()
        {
            super();
            this.valid = new AtomicBoolean(true);
        }
        
        @Override
        public Object setProperty(final String key, final Object value) throws PropertyIsLockedException
        {
            checkValid();
            
            Object old = null;
            IPropertyBlockModifyListener.ModifyType modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
            
            if ((PropertyBlockImpl.this.lockedProperties != null) && (PropertyBlockImpl.this.lockedProperties.get(key) != null))
            {
                throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
            }
            if (PropertyBlockImpl.this.properties == null)
            {
                PropertyBlockImpl.this.properties = new HashMap<String, Object>();
            }
            else
            {
                if (PropertyBlockImpl.this.properties.containsKey(key))
                {
                    modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
                }
                old = PropertyBlockImpl.this.properties.get(key);
            }
            PropertyBlockImpl.this.properties.put(key, value);
            PropertyBlockImpl.this.propertiesCopy = null;
            PropertyBlockImpl.this.matchables = null;
            PropertyBlockImpl.this.keySet = null;
            if (this.modifyList == null)
            {
                this.modifyList = new ArrayList<PropertyBlockModifyItem>();
            }
            this.modifyList.add(new PropertyBlockModifyItem(modifyType, key, old, value));
            
            return old;
        }
        
        @Override
        public Map<String, Object> setPropertyEntrySet(final Set<Entry<String, Object>> propertyEntrySet, final boolean ignoreIfEquals) throws PropertyIsLockedException
        {
            checkValid();
            
            if (propertyEntrySet == null)
            {
                return EMPTY_PROPERTIES;
            }
            
            if (propertyEntrySet.isEmpty())
            {
                return EMPTY_PROPERTIES;
            }
            Map<String, Object> oldValue = new HashMap<String, Object>();
            for (final Entry<String, Object> entry : propertyEntrySet)
            {
                oldValue.put(entry.getKey(), this.setProperty(entry.getKey(), entry.getValue()));
            }
            return oldValue;
        }
        
        @Override
        public Object getProperty(final String key)
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return null;
            }
            return PropertyBlockImpl.this.properties.get(key);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(final String key, final Class<T> resultClass)
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return null;
            }
            return (T) PropertyBlockImpl.this.properties.get(key);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getPropertyOrDefault(final String key, final Class<T> resultClass, final T defaultValue)
        {
            checkValid();
            
            T typedValue = defaultValue;
            Object current = getProperty(key);
            if (current != null)
            {
                typedValue = (T) current;
            }
            else
            {
                typedValue = defaultValue;
            }
            return typedValue;
        }
        
        @Override
        public String getPropertyOrDefaultAsString(final String key, final String defaultValue)
        {
            checkValid();
            
            String stringValue = defaultValue;
            Object current = getProperty(key);
            if (current != null)
            {
                if (!(current instanceof String))
                {
                    current = current.toString();
                }
            }
            if ((current != null) && (!((String) current).isEmpty()))
            {
                stringValue = (String) current;
            }
            else
            {
                stringValue = defaultValue;
            }
            return stringValue;
        }
        
        @Override
        public Object removeProperty(final String key) throws PropertyIsLockedException
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return null;
            }
            
            if (!PropertyBlockImpl.this.properties.containsKey(key))
            {
                return null;
            }
            
            Object oldPropertyValue = null;
            
            if ((PropertyBlockImpl.this.lockedProperties != null) && (PropertyBlockImpl.this.lockedProperties.get(key) != null))
            {
                throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
            }
            
            oldPropertyValue = PropertyBlockImpl.this.properties.get(key);
            
            PropertyBlockImpl.this.properties.remove(key);
            PropertyBlockImpl.this.propertiesCopy = null;
            PropertyBlockImpl.this.matchables = null;
            PropertyBlockImpl.this.keySet = null;
            
            if (this.modifyList == null)
            {
                this.modifyList = new ArrayList<PropertyBlockModifyItem>();
            }
            this.modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, key, oldPropertyValue, null));
            
            return oldPropertyValue;
        }
        
        @Override
        public Map<String, Object> getProperties()
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return EMPTY_PROPERTIES;
            }
            
            Map<String, Object> props = PropertyBlockImpl.this.propertiesCopy;
            if (props == null)
            {
                if (PropertyBlockImpl.this.properties == null)
                {
                    return EMPTY_PROPERTIES;
                }
                PropertyBlockImpl.this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String, Object>(PropertyBlockImpl.this.properties));
                props = PropertyBlockImpl.this.propertiesCopy;
            }
            return props;
        }
        
        @Override
        public Set<String> getPropertyKeySet()
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return EMPTY_KEYSET;
            }
            
            if (PropertyBlockImpl.this.keySet == null)
            {
                PropertyBlockImpl.this.keySet = Collections.unmodifiableSet(PropertyBlockImpl.this.properties.keySet());
            }
            return PropertyBlockImpl.this.keySet;
        }
        
        @Override
        public boolean isEmpty()
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return false;
            }
            
            return PropertyBlockImpl.this.properties.isEmpty();
        }
        
        @Override
        public boolean containsKey(final Object key)
        {
            checkValid();
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return false;
            }
            
            return PropertyBlockImpl.this.properties.containsKey(key);
        }
        
        @Override
        public Map<String, Object> clear() throws PropertyIsLockedException
        {
            checkValid();
            
            Map<String, Object> oldValues = null;
            
            if (PropertyBlockImpl.this.properties == null)
            {
                return EMPTY_PROPERTIES;
            }
            if (PropertyBlockImpl.this.properties.isEmpty())
            {
                return EMPTY_PROPERTIES;
            }
            
            if ((PropertyBlockImpl.this.lockedProperties != null) && (!PropertyBlockImpl.this.lockedProperties.isEmpty()))
            {
                throw new PropertyIsLockedException("clear failed. property block as locks");
            }
            
            oldValues = new HashMap<>(PropertyBlockImpl.this.properties);
            for (final Entry<String, Object> oldEntry : oldValues.entrySet())
            {
                if (this.modifyList == null)
                {
                    this.modifyList = new ArrayList<PropertyBlockModifyItem>();
                }
                this.modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null));
                
            }
            
            PropertyBlockImpl.this.keySet = null;
            PropertyBlockImpl.this.properties.clear();
            PropertyBlockImpl.this.propertiesCopy = null;
            PropertyBlockImpl.this.matchables = null;
            
            return oldValues;
        }
        
        @Override
        public IPropertyLock lockProperty(final String key)
        {
            checkValid();
            
            if (key == null)
            {
                return null;
            }
            
            if (key.isEmpty())
            {
                return null;
            }
            
            if (PropertyBlockImpl.this.lockedProperties == null)
            {
                PropertyBlockImpl.this.lockedProperties = new HashMap<String, UUID>();
            }
            else if (PropertyBlockImpl.this.lockedProperties.get(key) != null)
            {
                return null;
            }
            
            UUID pin = UUID.randomUUID();
            PropertyBlockImpl.this.lockedProperties.put(key, pin);
            return new PropertyLockImpl(PropertyBlockImpl.this, key, pin);
            
        }
        
        @Override
        public Supplier<List<PropertyBlockModifyItem>> computeProcedure(final IPropertyBlockAtomicProcedure operationHandler)
        {
            checkValid();
            
            operationHandler.accept(this);
            
            if (this.modifyList == null)
            {
                return new PropertyBlockProcedureModifyAuditTrail(null);
            }
            return new PropertyBlockProcedureModifyAuditTrail(Collections.unmodifiableList(this.modifyList));
        }
        
        private void checkValid()
        {
            if (!this.valid.get())
            {
                throw new RuntimeException("PropertyBlock Wrapper not valid");
            }
        }
        
        @Override
        public void addModifyListener(final IPropertyBlockModifyListener listener)
        {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void removeModifyListener(final IPropertyBlockModifyListener listener)
        {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void dispose()
        {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private List<IPropertyBlockModifyListener> getModifyListenerList()
    {
        List<IPropertyBlockModifyListener> list = this.modifyListenerListCopy;
        if (list != null)
        {
            return list;
        }
        
        this.propertiesWriteLock.lock();
        try
        {
            list = this.modifyListenerListCopy;
            if (list != null)
            {
                return list;
            }
            
            list = (this.modifyListenerList == null || this.modifyListenerList.isEmpty()) ? EMPTY_MODIFY_LISTENER : new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);
        }
        finally
        {
            this.propertiesWriteLock.unlock();
        }
        this.modifyListenerListCopy = list;
        return list;
    }
}
