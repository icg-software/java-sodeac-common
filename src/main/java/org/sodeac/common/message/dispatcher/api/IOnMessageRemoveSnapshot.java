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
 * An extension interface for {@link IDispatcherChannelManager} to consume notifications if a {@link IMessage}s are removed from {@link IDispatcherChannel}
 *
 * @author Sebastian Palarus
 *
 */
public interface IOnMessageRemoveSnapshot<T> extends IDispatcherChannelManager
{
    /**
     * This is fired, if {@link IDispatcherChannelManager} removed queued {@link IMessage}s
     * <br>
     * invoked and synchronized by queue worker
     *
     * @param messageRemoveSnapshot removed message snapshot
     */
    void onMessageRemoveSnapshot(DequeSnapshot<IMessage<T>> messageRemoveSnapshot);
}
