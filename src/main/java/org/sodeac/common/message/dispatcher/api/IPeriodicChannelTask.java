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
 * An extenstion interface for {@link IDispatcherChannelTask}. Tasks implements this interface will not finished by default and re-run periodically by worker, until the task set done manually with state-handler {@link ITaskControl}.
 *
 * @author Sebastian Palarus
 *
 */
public interface IPeriodicChannelTask extends IDispatcherChannelTask
{
    /**
     * default periodic interval
     *
     * @return
     */
    public long getPeriodicRepetitionInterval();
}
