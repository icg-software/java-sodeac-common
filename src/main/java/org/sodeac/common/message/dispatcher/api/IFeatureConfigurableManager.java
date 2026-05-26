/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.snapdeque.SnapshotableDeque;

public interface IFeatureConfigurableManager extends
    IDispatcherChannelManager,
    IOnTaskDone,
    IOnTaskError,
    IOnTaskTimeout,
    IOnChannelAttach,
    IOnChannelDetach,
    IOnChannelSignal,
    IOnMessageStore,
    IOnMessageRemove,
    IOnMessageStoreSnapshot,
    IOnMessageRemoveSnapshot
{
    
    default boolean implementsOnMessageStore()
    {
        return implementsControllerMethod("onMessageStore", Void.TYPE, IMessage.class);
    }
    
    default boolean implementsOnMessageStoreSnapshot()
    {
        return implementsControllerMethod("onMessageStoreSnapshot", Void.TYPE, SnapshotableDeque.class);
    }
    
    default boolean implementsOnChannelSignal()
    {
        return implementsControllerMethod("onChannelSignal", Void.TYPE, IDispatcherChannel.class, String.class);
    }
    
    default boolean implementsOnChannelDetach()
    {
        return implementsControllerMethod("onChannelDetach", Void.TYPE, IDispatcherChannel.class);
    }
    
    default boolean implementsOnChannelAttach()
    {
        return implementsControllerMethod("onChannelAttach", Void.TYPE, IDispatcherChannel.class);
    }
    
    default boolean implementsOnTaskError()
    {
        return implementsControllerMethod("onTaskError", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class, Throwable.class);
    }
    
    default boolean implementsOnTaskDone()
    {
        return implementsControllerMethod("onTaskDone", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class);
    }
    
    default boolean implementsOnTaskTimeout()
    {
        return implementsControllerMethod("onTaskTimeout", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class, Object.class, Runnable.class);
    }
    
    default boolean implementsOnMessageRemove()
    {
        return implementsControllerMethod("onMessageRemove", Void.TYPE, IMessage.class);
    }
    
    default boolean implementsOnMessageRemoveSnapshot()
    {
        return implementsControllerMethod("onMessageRemoveSnapshot", Void.TYPE, SnapshotableDeque.class);
    }
    
    @Override
    default void onMessageStore(final IMessage message) { }
    
    @Override
    default void onMessageStoreSnapshot(final DequeSnapshot messageStoreSnapshot) { }
    
    @Override
    default void onChannelSignal(final IDispatcherChannel channel, final String signal) { }
    
    @Override
    default void onChannelDetach(final IDispatcherChannel channel) { }
    
    @Override
    default void onChannelAttach(final IDispatcherChannel channel) { }
    
    @Override
    default void onTaskError(final IDispatcherChannel channel, final IDispatcherChannelTask task, final Throwable throwable) { }
    
    @Override
    default void onTaskDone(final IDispatcherChannel channel, final IDispatcherChannelTask task) { }
    
    @Override
    default void onTaskTimeout(final IDispatcherChannel channel, final IDispatcherChannelTask task, final Object taskState, final Runnable interrupter) { }
    
    @Override
    default void onMessageRemove(final IMessage message) { }
    
    @Override
    default void onMessageRemoveSnapshot(final DequeSnapshot messageRemoveSnapshot) { }
    
    default boolean implementsControllerMethod(final String name, final Class<?> returnType, final Class<?>... parameterTypes)
    {
        Class<?> clazz = this.getClass();
        while (clazz != null)
        {
            try
            {
                Method m = clazz.getDeclaredMethod(name, parameterTypes);
                if ((m != null) && (m.getReturnType() == returnType) && (m.getModifiers() == Modifier.PUBLIC))
                {
                    return true;
                }
            }
            catch (final NoSuchMethodException e) { }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
    
}
