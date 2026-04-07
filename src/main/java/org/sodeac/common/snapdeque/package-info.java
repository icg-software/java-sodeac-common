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

/**
 * A {@link org.sodeac.common.snapdeque.SnapshotableDeque} provides an implementation of {@link java.util.Deque}. Iterating through deque requires creating a {@link DequeSnapshot}. A snapshot is immutable regardless of possible changes in source deque.
 * 
 * <p>
 * 
 * <strong>Unlike {@link java.util.concurrent.CopyOnWriteArrayList} a {@link org.sodeac.common.snapdeque.SnapshotableDeque} never creates a deep copy of content, neither when modifying, nor when reading or creating a snapshot.</strong>
 * Iterating through a snapshot requires no locks inside of snapshot or source deque. The goal is to prevent a big performance slump for very large deques. This is realized by versionable linked lists with different branches inside of deque.
 * 
 * <p>
 * 
 * It is recommend to close snapshots after use to remove unneeded version-branches.
 * 
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 * 
 */
package org.sodeac.common.snapdeque;

