/*******************************************************************************
 * Copyright (c) 2018, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.snapdeque;

/**
 * An interface to consume update-notifications of nodes
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 * 
 * @param <E> the type of elements in deque
 */
public interface INodeEventHandler<E>
{
	/**
	 * Notify if node is linked to deque
	 * 
	 * @param node node
	 * @param linkMode append or prepend
	 * @param version deque version
	 */
	public void onLink(DequeNode<E> node, SnapshotableDeque.LinkMode linkMode, long version );
	
	/**
	 * Notify if node is unlinked from deque
	 * 
	 * @param node node
	 * @param version deque version
	 */
	public void onUnlink(DequeNode<E> node, long version );
	
	/**
	 * Notify if node is disposed
	 * 
	 * @param deque deque
	 * @param payload element was managed by node
	 */
	public void onDisposeNode(SnapshotableDeque<E> deque, E payload);
}
