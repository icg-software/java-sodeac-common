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
	
	public default boolean implementsOnMessageStore()
	{
		return implementsControllerMethod("onMessageStore", Void.TYPE, IMessage.class);
	}
	
	public default boolean implementsOnMessageStoreSnapshot()
	{
		return implementsControllerMethod("onMessageStoreSnapshot", Void.TYPE, SnapshotableDeque.class);
	}
	
	public default boolean implementsOnChannelSignal()
	{
		return implementsControllerMethod("onChannelSignal", Void.TYPE, IDispatcherChannel.class, String.class);
	}
	
	public default boolean implementsOnChannelDetach()
	{
		return implementsControllerMethod("onChannelDetach", Void.TYPE, IDispatcherChannel.class);
	}
	
	public default boolean implementsOnChannelAttach()
	{
		return implementsControllerMethod("onChannelAttach", Void.TYPE, IDispatcherChannel.class);
	}
	
	public default boolean implementsOnTaskError()
	{
		return implementsControllerMethod("onTaskError", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class, Throwable.class);
	}
	
	public default boolean implementsOnTaskDone()
	{
		return implementsControllerMethod("onTaskDone", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class);
	}
	
	public default boolean implementsOnTaskTimeout()
	{
		return implementsControllerMethod("onTaskTimeout", Void.TYPE, IDispatcherChannel.class, IDispatcherChannelTask.class, Object.class, Runnable.class);
	}

	public default boolean implementsOnMessageRemove()
	{
		return implementsControllerMethod("onMessageRemove", Void.TYPE, IMessage.class);
	}
	
	public default boolean implementsOnMessageRemoveSnapshot()
	{
		return implementsControllerMethod("onMessageRemoveSnapshot", Void.TYPE, SnapshotableDeque.class);
	}

	@Override
	default void onMessageStore(IMessage message){}

	@Override
	default void onMessageStoreSnapshot(DequeSnapshot messageStoreSnapshot){}

	@Override
	default void onChannelSignal(IDispatcherChannel channel, String signal){}

	@Override
	default void onChannelDetach(IDispatcherChannel channel){}

	@Override
	default void onChannelAttach(IDispatcherChannel channel){}

	@Override
	default void onTaskError(IDispatcherChannel channel, IDispatcherChannelTask task, Throwable throwable){}

	@Override
	default void onTaskDone(IDispatcherChannel channel,IDispatcherChannelTask task){}

	@Override
	default void onTaskTimeout(IDispatcherChannel channel, IDispatcherChannelTask task, Object taskState, Runnable interrupter){}

	@Override
	default void onMessageRemove(IMessage message) {};
	
	@Override
	default void onMessageRemoveSnapshot(DequeSnapshot messageRemoveSnapshot){}

	default boolean implementsControllerMethod(String name,Class<?> returnType, Class<?>... parameterTypes)
	{
		Class<?> clazz = this.getClass();
		while(clazz != null)
		{
			try
			{
				Method m = clazz.getDeclaredMethod(name,parameterTypes);
				if((m != null) && (m.getReturnType() == returnType) && (m.getModifiers() == Modifier.PUBLIC))
				{
					return true;
				}
			} 
			catch (NoSuchMethodException e){}
			clazz = clazz.getSuperclass();
		}
		return false;
	}
	
}
