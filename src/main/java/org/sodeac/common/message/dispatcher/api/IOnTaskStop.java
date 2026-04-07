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
package org.sodeac.common.message.dispatcher.api;

/**
 * extension interface for {@link IDispatcherChannelTask} to facilitate prevention of deadlocks and inconsistent data structures in result of {@link java.lang.Thread#stop()}
 *
 * @author Sebastian Palarus
 *
 */
public interface IOnTaskStop<T> extends IDispatcherChannelTask<T>
{
    /**
     *
     * before invoke {@link java.lang.Thread#stop()} this method is invoked until the return-value is less than 1
     * <p>
     * Attention! The call is not synchronized in worker thread!
     *
     * @param requestNumber         how many times this request is invoked since {@link IDispatcherChannelTask#run(IQueue, IMetrics, IPropertyBlock, ITaskControl, java.util.List)} is invoked
     * @param totalMoreTimeUntilNow how many time in ms was requested since {@link IDispatcherChannelTask#run(IQueue, IMetrics, IPropertyBlock, ITaskControl, java.util.List)} is invoked
     * @param worker                worker thread invoked {@link IDispatcherChannelTask#run(IQueue, IMetrics, IPropertyBlock, ITaskControl, java.util.List)} currently runs in timeout
     *
     * @return time in ms tasks requires for clean up
     */
    long requestForMoreLifeTime(long requestNumber, long totalMoreTimeUntilNow, IDispatcherChannelWorker worker);
}
