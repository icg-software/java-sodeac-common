/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.service.api;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IServiceConnection
{
	public Set<IServiceChannel.IChannelDescription> getChannelCatalog();
	
	public <T> IMessageProducerEndpoint<T> openMessageProducerEndpoint(Class<T> messageClass);
	public <T> IMessageConsumerEndpoint<T> openMessageConsumerEndpoint(Class<T> messageClass);
	
	public IServiceConnection connect();
	public IServiceConnection disconnect();
	public boolean isConnected();
	public IServiceConnection close();
	public boolean isClosed();
	
	public <A> A getAdapter(Class<A> adapterClass);
	
	public interface IMessageConsumerEndpoint<T> extends IServiceChannel<T>
	{
		public IMessageConsumerEndpoint<T> onMessageReceived(Consumer<IMessageReceive<T>> messageConsumer); 
		public IMessageConsumerEndpoint<T> setupEndpoint(Consumer<IMessageConsumerEndpoint<T>> setup);
	}
	
	public interface IMessageProducerEndpoint<T> extends IServiceChannel<T>
	{
		public IMessageProducerEndpoint<T> onMessageRequested(BiConsumer<IMessageRequest<T>,Consumer<T>> messageProducer);
		public IMessageProducerEndpoint<T> setupEndpoint(Consumer<IMessageProducerEndpoint<T>> setup);
	}
}
