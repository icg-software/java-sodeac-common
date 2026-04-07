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
package org.sodeac.common.message.dispatcher.impl;

import java.util.List;

import org.sodeac.common.message.dispatcher.api.IPropertyBlockModifyListener;
import org.sodeac.common.message.dispatcher.api.PropertyBlockModifyItem;

public class ChannelConfigurationModifyListener implements IPropertyBlockModifyListener
{
	private ChannelImpl channel;
	
	protected ChannelConfigurationModifyListener(ChannelImpl queue)
	{
		super();
		this.channel = queue;
	}

	@Override
	public void onModify(ModifyType type, String key, Object valueOld, Object valueNew)
	{
		((MessageDispatcherImpl)channel.getDispatcher()).onConfigurationModify(this.channel, key);
	}

	@Override
	public void onModifySet(List<PropertyBlockModifyItem> modifySet)
	{
		if(modifySet == null)
		{
			return;
		}
		if(modifySet.isEmpty())
		{
			return;
		}
		String[] attributes = new String[modifySet.size()];
		int index = 0;
		for(PropertyBlockModifyItem item : modifySet)
		{
			attributes[index++] = item.getKey();
		}
 		((MessageDispatcherImpl)channel.getDispatcher()).onConfigurationModify(this.channel,attributes);
	}

}
