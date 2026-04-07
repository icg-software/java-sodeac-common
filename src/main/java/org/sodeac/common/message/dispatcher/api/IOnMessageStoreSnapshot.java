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
package org.sodeac.common.message.dispatcher.api;

import org.sodeac.common.snapdeque.DequeSnapshot;

/**
 *
 * An extension interface for {@link IDispatcherChannelManager} to consume a notification if {@link IDispatcherChannel} has queued {@link IMessage}s.
 * <br>
 * UseCase: (re)plan tasks
 *
 * @author Sebastian Palarus
 *
 */
public interface IOnMessageStoreSnapshot<T> extends IDispatcherChannelManager
{
    /**
     * This is fired, if {@link IDispatcherChannel} has queued {@link IMessage}s
     * <br>
     * invoked and synchronized by queue worker
     * <br>
     * UseCase: (re)plan tasks
     *
     * @param messageStoreSnapshot new messageSnapshot
     */
    void onMessageStoreSnapshot(DequeSnapshot<IMessage<T>> messageStoreSnapshot);
}
