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

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;

import org.sodeac.common.snapdeque.DequeNode.Link;

/**
 * A threadsafe and snapshotable {@link Deque}. Iterating through deque requires creating a {@link DequeSnapshot}.
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 * 
 * @param <E> the type of elements in deque
 */
public class SnapshotableDeque<E> implements AutoCloseable,Deque<E>
{
	public enum LinkMode {APPEND,PREPEND};

	/**
	 * Constructor to create a SnapshotableDeque .
	 */
	public  SnapshotableDeque()
	{
		this(Integer.MAX_VALUE,false);
	}
	
	/**
	 * Constructor to create a SnapshotableDeque with specified capacity .
	 */
	public  SnapshotableDeque(long capacity)
	{
		this(capacity,false);
	}
	
	/**
	 * Constructor to create a SnapshotableDeque with specified capacity .
	 */
	public  SnapshotableDeque(long capacity, boolean generateMetadata)
	{
		super();
		
		this.uuid = UUID.randomUUID();
		this.rwLock = new ReentrantReadWriteLock(true);
		this.readLock = this.rwLock.readLock();
		this.writeLock = this.rwLock.writeLock();
		this.modificationVersion = new SnapshotVersion<E>(this,0L);
		this.obsoleteList = new LinkedList<Link<E>>();
		this.openSnapshotVersionList = new HashSet<SnapshotVersion<E>>();
		this.nodeSize = 0L;
		this.capacity = capacity;
		this.begin = new Bollard();
		this.end = new Bollard();
		this.generateMetadata = generateMetadata;
		this.sequence = 0L;
	}
	
	
	protected ReentrantReadWriteLock rwLock;
	protected ReadLock readLock;
	protected WriteLock writeLock;
	
	protected Bollard begin = null;
	protected Bollard end = null;
	
	protected LinkedList<Link<E>> obsoleteList = null;
	protected SnapshotVersion<E> modificationVersion = null;
	protected SnapshotVersion<E> snapshotVersion = null;
	protected Set<SnapshotVersion<E>> openSnapshotVersionList = null;
	protected volatile LinkedList<INodeEventHandler<E>> eventHandlerList = null;
	protected volatile long nodeSize;
	protected volatile long capacity;
	
	protected UUID uuid = null;
	protected boolean generateMetadata = false;
	
	protected long sequence;
	
	public long getCapacity()
	{
		return capacity;
	}

	public void setCapacity(long capacity)
	{
		this.capacity = capacity;
	}

	/**
	 * Internal helper method returns current modification version. This method must invoke with SD.writeLock !
	 * 
	 * <br> Current modification version must be higher than current snapshot version.
	 * 
	 * @return
	 */
	protected SnapshotVersion<E> getModificationVersion()
	{
		if(snapshotVersion != null)
		{
			if(modificationVersion.getSequence() <= snapshotVersion.getSequence())
			{
				if(modificationVersion.getSequence() == Long.MAX_VALUE)
				{
					throw new RuntimeException("max supported version reached: " + Long.MAX_VALUE);
				}
				modificationVersion = new SnapshotVersion<E>(this,snapshotVersion.getSequence() + 1L);
			}
			snapshotVersion = null;
		}
		return modificationVersion;
	}
	
	/**
	 * returns the remaining capacity 
	 * 
	 * @return the remaining capacity 
	 */
	public long remainingCapacity()
	{
		Lock lock = this.readLock;
		lock.lock();
		try
		{
			return this.nodeSize - capacity;
		}
		finally 
		{
			lock.unlock();
		}
	}
	

	/**
	 * register node event handler
	 * 
	 * @param eventHandler event handler to register
	 */
	public void registerEventHandler(INodeEventHandler<E> eventHandler)
	{
		if(eventHandler == null)
		{
			return;
		}
		
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			if(this.eventHandlerList == null)
			{
				this.eventHandlerList = new LinkedList<>();
			}
			else if(this.eventHandlerList.contains(eventHandler))
			{
				return;
			}
			this.eventHandlerList.addLast(eventHandler);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	/**
	 * unregister node event handler 
	 * 
	 * @param eventHandler eventHandler event handler to unregister
	 */
	public void unregisterEventHandler(INodeEventHandler<E> eventHandler)
	{
		if(eventHandler == null)
		{
			return;
		}
		
		if(this.eventHandlerList == null)
		{
			return;
		}
		
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			this.eventHandlerList.remove(eventHandler);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Internal method to remove snapshot and clean-ups
	 * 
	 * @param snapshotVersion version of deque
	 */
	protected void removeSnapshotVersion(SnapshotVersion<E> snapshotVersion)
	{
		if(snapshotVersion == null)
		{
			return;
		}
		if(snapshotVersion.getParent() != this)
		{
			return;
		}
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			this.openSnapshotVersionList.remove(snapshotVersion);
			if(this.openSnapshotVersionList.isEmpty())
			{
				this.snapshotVersion = null;
			}
			if(! this.obsoleteList.isEmpty())
			{
				long minimalSnapshotVersionToKeep = Long.MAX_VALUE -1L;
				for( SnapshotVersion<E> usedSnapshotVersion : this.openSnapshotVersionList)
				{
					if(usedSnapshotVersion.sequence < minimalSnapshotVersionToKeep)
					{
						minimalSnapshotVersionToKeep = usedSnapshotVersion.sequence;
					}
				}
				
				Link<E> obsoleteLink;
				Link<E> clearLink;
				while(! this.obsoleteList.isEmpty())
				{
					obsoleteLink = this.obsoleteList.getFirst();
					if( minimalSnapshotVersionToKeep <= obsoleteLink.obsoleteOnVersion) 
					{
						// snapshot is created after link was made obsolete
						break;
					}
					this.obsoleteList.removeFirst();
					
					if(obsoleteLink instanceof ClearCompleteForwardBranch)
					{

						clearLink = ((ClearCompleteForwardBranch<E>)obsoleteLink).wrap;
						((ClearCompleteForwardBranch<E>)obsoleteLink).wrap = null;
						obsoleteLink.clear();
						obsoleteLink  = clearLink;
						
						while(obsoleteLink != null)
						{
							if(obsoleteLink instanceof Eyebolt)
							{
								break;
							}
							
							clearLink = obsoleteLink;
							obsoleteLink = obsoleteLink.nextLink;
							clearLink.clear();
						}
					}
					else
					{
						if(obsoleteLink.olderVersion != null)
						{
							obsoleteLink.olderVersion.newerVersion = obsoleteLink.newerVersion;
						}
						if(obsoleteLink.newerVersion != null)
						{
							obsoleteLink.newerVersion.newerVersion = obsoleteLink.olderVersion;
						}
						obsoleteLink.clear();
					}
				}
			}
			if((snapshotVersion != this.modificationVersion) && (snapshotVersion != this.snapshotVersion))
			{
				snapshotVersion.clear();
			}
		}
		finally 
		{
			lock.unlock();
		}
	}

	/**
	 * Internal method to set link to obsolete
	 * 
	 * @param link link to set obsolete
	 */
	protected void setObsolete(Link<E> link)
	{
		link.obsoleteOnVersion = modificationVersion.sequence;
		if(link.node != null)
		{
			// payload-link , not eyebolt
			link.node.lastObsoleteOnVersion = link.obsoleteOnVersion;
		}
		this.obsoleteList.addLast(link);
	}
	
	/**
	 * Compute procedure undisturbed by concurrency updates.
	 * 
	 * <p>Don't create new Threads inside procedure. There is a risk of a deadlock
	 * 
	 * @param procedure
	 */
	public void computeProcedure(Consumer<SnapshotableDeque<E>> procedure)
	{
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			procedure.accept(this);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + uuid.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		return this == obj;	
	}
	
	/**
	 * Internal helper class to manage versions
	 * 
	 * @author Sebastian Palarus
	 * @since 1.0
	 * @version 1.0
	 *
	 * @param <E> the type of elements in deque
	 */
	protected static class SnapshotVersion<E> implements Comparable<SnapshotVersion<E>>
	{
		protected SnapshotVersion(SnapshotableDeque<E> deque, long sequence)
		{
			super();
			this.sequence = sequence;
			this.deque = deque;
		}
		
		private long sequence;
		private SnapshotableDeque<E> deque;
		private Set<DequeSnapshot<E>> openSnapshots;
		
		protected void addSnapshot(DequeSnapshot<E> snapshot)
		{
			if(snapshot == null)
			{
				return;
			}
			if(openSnapshots == null)
			{
				openSnapshots = new LinkedHashSet<DequeSnapshot<E>>();
			}
			openSnapshots.add(snapshot);
		}
		
		protected void removeSnapshot(DequeSnapshot<E> snapshot)
		{
			if(snapshot == null)
			{
				return;
			}
			if(openSnapshots == null)
			{
				deque.removeSnapshotVersion(this);
			}
			else
			{
				openSnapshots.remove(snapshot);
				if(openSnapshots.isEmpty())
				{
					deque.removeSnapshotVersion(this);
				}
			}
		}
		
		protected Set<DequeSnapshot<E>> getOpenSnapshots() 
		{
			return openSnapshots;
		}

		protected long getSequence()
		{
			return sequence;
		}

		protected SnapshotableDeque<E> getParent()
		{
			return deque;
		}

		@Override
		public int compareTo(SnapshotVersion<E> o)
		{
			return Long.compare(this.sequence, o.sequence);
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (sequence ^ (sequence >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			return this == obj;	
		}

		@Override
		public String toString()
		{
			return "version: " + this.sequence 
					+ " : open snapshots " + (this.openSnapshots == null ? "null" : this.openSnapshots.size()) 
			;
		}
		
		protected void clear()
		{
			deque = null;
			if(openSnapshots != null)
			{
				try {openSnapshots.clear();}catch (Exception e) {}
				openSnapshots = null;
			}
		}
	}
	
	/**
	 * Internal helper class
	 * 
	 * @author Sebastian Palarus
	 *
	 * @param <E> the type of elements in this deque
	 */
	protected static class ClearCompleteForwardBranch<E> extends Link<E>
	{
		private Link<E> wrap; 
		protected ClearCompleteForwardBranch(Link<E> wrap)
		{
			super();
			this.wrap = wrap;
		}
	}
	
	/**
	 * Helps gc to clean memory. After this the deque is not usable anymore.
	 */
	public void dispose()
	{
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			if(eventHandlerList != null)
			{
				try {eventHandlerList.clear();}catch (Exception e) {}
				eventHandlerList = null;
			}
			
			this.clear();
			
			Set<DequeSnapshot<E>> openSnapshots = new HashSet<DequeSnapshot<E>>();
			
			for(SnapshotVersion<E> snapshotVersion : this.openSnapshotVersionList )
			{
				Set<DequeSnapshot<E>> snaps = snapshotVersion.getOpenSnapshots();
				if(snaps != null)
				{
					openSnapshots.addAll(snaps);
				}
			}
			
			for(DequeSnapshot<E> openSnapshot : openSnapshots)
			{
				try
				{
					openSnapshot.close();
				}
				catch (Exception e) {}
			}
			
			openSnapshots.clear();
			openSnapshots = null;
			
			while(! this.openSnapshotVersionList.isEmpty())
			{
				this.removeSnapshotVersion(this.openSnapshotVersionList.iterator().next());
			}
			
			Eyebolt<E> eyebolt;
			if(this.begin != null)
			{
				eyebolt = this.begin.getLink();
				if(eyebolt != null)
				{
					eyebolt.clear();
				}
			}
					
			if(this.end != null)
			{
				eyebolt = this.end.getLink();
				if(eyebolt != null)
				{
					eyebolt.clear();
				}
			}
				
			this.begin = null;
			this.end = null;
			
			if(obsoleteList != null)
			{
				try{obsoleteList.clear();}catch (Exception e) {}
				obsoleteList = null;
			}
			if(modificationVersion != null)
			{
				modificationVersion.clear();
				modificationVersion = null;
			}
			if(snapshotVersion != null)
			{
				snapshotVersion.clear();
				snapshotVersion = null;
			}
			openSnapshotVersionList = null;
			

			uuid = null;
			
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	/**
	 * link (append or prepend) new element to deque. If capacity is reached a IllegalStateException is thrown.
	 * 
	 * @param linkMode append or prepend
	 * @param element item to link
	 * @return node
	 */
	public DequeNode<E> link(SnapshotableDeque.LinkMode linkMode, E element)
	{
		return link(linkMode, element, null);
	}
	
	/**
	 * link (append or prepend) new element to deque. If capacity is reached a IllegalStateException is thrown.
	 * 
	 * @param linkMode append or prepend
	 * @param element item to link
	 * @param synchronizedConsumer consumer accept new node inside inside write lock
	 * @return node
	 */
	public DequeNode<E> link(SnapshotableDeque.LinkMode linkMode, E element, Consumer<DequeNode<E>> synchronizedConsumer)
	{
		DequeNode<E> node = null;
		
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			this.getModificationVersion();
			
			if(generateMetadata)
			{
				node = new DequeNode<E>(element,this, UUID.randomUUID(), System.currentTimeMillis(), ++this.sequence);
			}
			else
			{
				node = new DequeNode<E>(element,this, null, null, null);
			}
			
			if(linkMode == SnapshotableDeque.LinkMode.PREPEND)
			{
				this.prependNode(node, this.modificationVersion);
			}
			else
			{
				this.appendNode(node, this.modificationVersion);
			}
			
			if(synchronizedConsumer != null)
			{
				synchronizedConsumer.accept(node);
			}
			
		}
		finally 
		{
			lock.unlock();
		}
		return node;
	}
	
	/**
	 * link (append or prepend) new elements to deque.
	 * 
	 * @param linkMode append or prepend
	 * @param elements items to link
	 * @return nodes
	 */
	public DequeNode<E>[] linkAll(SnapshotableDeque.LinkMode linkMode, Collection<? extends E> elements)
	{
		return linkAll(linkMode, elements, null);
	}
	
	/**
	 * link (append or prepend) new elements to deque.
	 * 
	 * @param linkMode append or prepend
	 * @param elements items to link
	 * @param synchronizedConsumer consumer accept new node inside inside write lock
	 * @return nodes
	 */
	@SuppressWarnings("unchecked")
	public DequeNode<E>[] linkAll(SnapshotableDeque.LinkMode linkMode, Collection<? extends E> elements, Consumer<DequeNode<E>> synchronizedConsumer)
	{
		
		if(elements == null)
		{
			return null;
		}
		
		DequeNode<E>[] nodes = new DequeNode[elements.size()];
		
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			this.getModificationVersion();
			
			DequeNode<E> node = null;
			int index = 0;
			for(E element : elements)
			{
				if(generateMetadata)
				{
					node = new DequeNode<E>(element,this, UUID.randomUUID(), System.currentTimeMillis(), ++this.sequence);
				}
				else
				{
					node = new DequeNode<E>(element,this, null, null, null);
				}
				
				nodes[index++] = node;
				
				if(linkMode == SnapshotableDeque.LinkMode.PREPEND)
				{
					this.prependNode(node, this.modificationVersion);
				}
				else
				{
					this.appendNode(node, this.modificationVersion);
				}
			}
			if(synchronizedConsumer != null)
			{
				for(DequeNode<E> item : nodes)
				{
					synchronizedConsumer.accept(item);
				}
			}
		}
		finally 
		{
			lock.unlock();
		}
		return nodes;
	}

	/**
	 * Internal method to append node.
	 * 
	 * @param node node to append
	 * @param currentVersion current version of deque
	 */
	private void appendNode(DequeNode<E> node, SnapshotVersion<E> currentVersion)
	{
		Link<E> link = node.getLink();
		if(link != null)
		{
			throw new IllegalStateException("Internal Error: link already exists");
		}
		Eyebolt<E> linkBegin = begin.getLink();
		if(linkBegin == null)
		{
			linkBegin = begin.createHead(currentVersion, null);
		}
		Eyebolt<E>  linkEnd = end.getLink();
		if(linkEnd == null)
		{
			linkEnd = end.createHead(currentVersion, null);
		}
		if(linkBegin.nextLink == null)
		{
			linkBegin.nextLink = linkEnd;
		}
		if(linkEnd.previewsLink == null)
		{
			linkEnd.previewsLink = linkBegin;
		}
		
		Link<E> prev = linkEnd.previewsLink;
		
		link = node.createHead(currentVersion, LinkMode.APPEND);
			Link<E> previewsOfPreviews = null;
		if((prev.createOnVersion != currentVersion) && (prev != linkBegin))
		{
			// if prev == linkBegin => does not require new link-begin-version, 
			// because snapshots links first payload link and current version has nothing to clean on snapshot.close()
			
			if(prev.createOnVersion.getSequence() < currentVersion.getSequence())
			{
				if(! this.openSnapshotVersionList.isEmpty())
				{
					previewsOfPreviews = prev.previewsLink;
					prev = prev.createNewerLink(currentVersion, null);
					prev.previewsLink = previewsOfPreviews;
				}
			}
		}
		
		// link new link with endlink
		linkEnd.previewsLink = link;
		link.nextLink = linkEnd;
		
		// link new link with previews link
		link.previewsLink = prev;
		
		// set new route
		prev.nextLink = link;
		
		if(previewsOfPreviews != null)
		{
			// set new route, if previews creates a new version
			previewsOfPreviews.nextLink = prev;
		}
		
		linkBegin.incrementSize();
		linkEnd.incrementSize();
	}
	
	/**
	 * Internal method to prepend node.
	 * 
	 * @param node node to prepend
	 * @param currentVersion current version of deque
	 */
	private void prependNode(DequeNode<E> node, SnapshotVersion<E> currentVersion)
	{
		Link<E> link = node.getLink();
		if(link != null)
		{
			throw new IllegalStateException("Internal Error: link already exists");
		}
		Eyebolt<E> linkBegin = begin.getLink();
		if(linkBegin == null)
		{
			linkBegin = begin.createHead( currentVersion, null);
		}
		Eyebolt<E> linkEnd = end.getLink();
		if(linkEnd == null)
		{
			linkEnd = end.createHead(currentVersion, null);
		}
		if(linkBegin.nextLink == null)
		{
			linkBegin.nextLink = linkEnd;
		}
		if(linkEnd.previewsLink == null)
		{
			linkEnd.previewsLink = linkBegin;
		}
		
		Link<E> next = linkBegin.nextLink;
		
		link = node.createHead(currentVersion, LinkMode.PREPEND);
		
		// save water ...
		
		// link new link with nextlink
		next.previewsLink = link;
		link.nextLink = next;
		
		// link new link with begin link
		link.previewsLink = linkBegin;
		
		// set new route
		linkBegin.nextLink = link;
		
		linkBegin.incrementSize();
		linkEnd.incrementSize();
	}
	
	/**
	 *  create a snapshot 
	 * 
	 * @return snapshot for specified
	 */
	public DequeSnapshot<E> createSnapshot()
	{
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			return new DequeSnapshot<>(this,false, null);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	/**
	 *  create a snapshot poll
	 * 
	 * @return snapshot for specified
	 */
	public DequeSnapshot<E> createSnapshotPoll()
	{
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			return new DequeSnapshot<>(this,true, null);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	/**
	 * Internal helper class to reference begin or end of branch. This is the non-payload variant of {@link DequeNode}.
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	protected class Bollard extends DequeNode<E>
	{
		protected Bollard()
		{
			super(null,SnapshotableDeque.this, null, null, null);
		}
		
		@Override
		protected boolean isPayload()
		{
			return false;
		}
		
		@Override
		protected Eyebolt<E> getLink()
		{
			return (Eyebolt<E>)super.getLink();
		}

		@Override
		protected Eyebolt<E> createHead(SnapshotVersion<E> currentVersion, LinkMode linkMode)
		{
			Link<E> link = new Eyebolt<E>(this, currentVersion);
			return (Eyebolt<E>)super.setHead( link, null);
		}
	}

	/**
	 * Internal helper class to reference begin or end of branch. This is the non-payload variant of {@link Link}.
	 * 
	 * @author Sebastian Palarus
	 *
	 * @param <E> the type of elements in this deque
	 */
	protected static class Eyebolt<E> extends Link<E>
	{
		protected Eyebolt(DequeNode<E> parent, SnapshotVersion<E> currentVersion)
		{
			super(parent, currentVersion);
		}
		
		private long size = 0;
		
		protected long getSize()
		{
			return size;
		}

		protected void setSize(long size)
		{
			this.size = size;
		}
		
		protected long incrementSize()
		{
			return ++size;
		}
		
		protected long decrementSize()
		{
			return --size;
		}
		
		protected Eyebolt<E> createNewerLink(SnapshotVersion<E> currentVersion, LinkMode linkMode)
		{
			Eyebolt<E> newVersion = new Eyebolt<>(this.node,currentVersion);
			newVersion.size = size;
			newVersion.olderVersion = this;
			this.newerVersion = newVersion;
			this.node.snapshotableDeque.setObsolete(this);
			this.node.setHead(newerVersion, null);
			return newVersion;
		}

		@Override
		public String toString()
		{
			return super.toString() + " size " + size;
		}
	}
	
	// Implements Collection

	@Override
	public boolean addAll(Collection<? extends E> c) 
	{
		Objects.requireNonNull(c);
		this.linkAll(SnapshotableDeque.LinkMode.APPEND, c, null);
		return c != null && c.size() > 0;
	}

	@Override
	public void clear() 
	{
		createSnapshotPoll().close();
	}
	
	@Override
	public boolean remove(Object o) 
	{
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			if(o == null)
			{
				DequeNode<E> node;
				Iterator<DequeNode<E>> it = snapshot.nodeIterable().iterator();
				try
				{
					while(it.hasNext())
					{
						node = it.next();
						if(! node.isLinked())
						{
							continue;
						}
						if (node.getElement() == null) 
						{
							node.unlink();
							return true;
						}
					}
				}
				finally 
				{
					if(it instanceof AutoCloseable)
					{
						try
						{
							((AutoCloseable)it).close();
						}
						catch (Exception e) {}
					}
					
					it = null;
					node = null;
				}
			}
			else
			{
				DequeNode<E> node;
				Iterator<DequeNode<E>> it = snapshot.nodeIterable().iterator();
				try
				{
					while(it.hasNext())
					{
						node = it.next();
						if(! node.isLinked())
						{
							continue;
						}
						if (o.equals(node.getElement())) 
						{
							node.unlink();
							return true;
						}
					}
				}
				finally 
				{
					if(it instanceof AutoCloseable)
					{
						try
						{
							((AutoCloseable)it).close();
						}
						catch (Exception e) {}
					}
					
					it = null;
					node = null;
				}
			}
	        return false;
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) 
	{
		Objects.requireNonNull(c);
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.containsAll(c);
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public boolean contains(Object o) 
	{
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.contains(o);
		}
		finally 
		{
			snapshot.close();
		}
	}
	
	@Override
	public int size() 
	{
		return (int)this.nodeSize;
	}
	
	@Override
	public boolean isEmpty() 
	{
		return this.size() == 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) 
	{
		Objects.requireNonNull(c);
		
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.removeAll(c);
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) 
	{
		Objects.requireNonNull(c);
		
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.retainAll(c);
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public Object[] toArray() 
	{
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.toArray();
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public <T> T[] toArray(T[] a) 
	{
		DequeSnapshot<E> snapshot = createSnapshot();
		try
		{
			return snapshot.toArray(a);
		}
		finally 
		{
			snapshot.close();
		}
	}

	@Override
	public boolean add(E e) 
	{
		addLast(e);
		return true;
	}
	
	// Implements queue
	
	@Override
	public E element() 
	{
		return getFirst();
	}
	
	@Override
	public boolean offer(E e) 
	{
		return offerLast(e);
	}
	
	@Override
	public E peek() 
	{
		return peekLast();
	}
	
	@Override
	public E poll() 
	{
		return pollFirst();
	}
	
	@Override
	public E remove() 
	{
		return removeFirst();
	}

	// Stack stuff
	
	@Override
	public E pop() 
	{
		return removeFirst();
	}

	@Override
	public void push(E e) 
	{
		addFirst(e);
	}
	
	// Implements deque inserts
	
	@Override
	public void addFirst(E e) 
	{
		link(SnapshotableDeque.LinkMode.PREPEND,e, null);
	}

	@Override
	public void addLast(E e) 
	{
		link(SnapshotableDeque.LinkMode.APPEND,e, null);
	}

	@Override
	public boolean offerFirst(E e) 
	{
		try
		{
			addFirst(e);
			return true;
		}
		catch (IllegalStateException exc) 
		{
			return false;
		}
	}

	@Override
	public boolean offerLast(E e) 
	{
		try
		{
			addLast(e);
			return true;
		}
		catch (IllegalStateException exc) 
		{
			return false;
		}
	}
	
	// Implements deque examine
	

	@Override
	public E getFirst() 
	{
		if(this.nodeSize == 0L)
		{
			throw new NoSuchElementException();
		}
		Lock lock = this.readLock;
		lock.lock();
		try
		{
			Eyebolt<E> beginLink = begin.getLink();
			if(beginLink == null)
			{
				throw new NoSuchElementException();
			}
			if(beginLink.nextLink == null)
			{
				throw new NoSuchElementException();
			}
			return beginLink.nextLink.element;
		}
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public E getLast() 
	{
		if(this.nodeSize == 0L)
		{
			throw new NoSuchElementException();
		}
		Lock lock = this.readLock;
		lock.lock();
		try
		{
			Eyebolt<E> endLink = end.getLink();
			if(endLink == null)
			{
				throw new NoSuchElementException();
			}
			if(endLink.previewsLink == null)
			{
				throw new NoSuchElementException();
			}
			return endLink.previewsLink.element;
		}
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public E peekFirst() 
	{
		if(this.nodeSize == 0L)
		{
			return null;
		}
		Lock lock = this.readLock;
		lock.lock();
		try
		{
			Eyebolt<E> beginLink = begin.getLink();
			if(beginLink == null)
			{
				return null;
			}
			if(beginLink.nextLink == null)
			{
				return null;
			}
			return beginLink.nextLink.element;
		}
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public E peekLast() 
	{
		if(this.nodeSize == 0L)
		{
			return null;
		}
		Lock lock = this.readLock;
		lock.lock();
		try
		{
			Eyebolt<E> endLink = end.getLink();
			if(endLink == null)
			{
				return null;
			}
			if(endLink.previewsLink == null)
			{
				return null;
			}
			return endLink.previewsLink.element;
		} 
		finally 
		{
			lock.unlock();
		}
	}
	
	@Override
	public Iterator<E> iterator() 
	{
		throw new UnsupportedOperationException("iterator is supported by snapshot only");
	}
	
	@Override
	public Iterator<E> descendingIterator() 
	{
		throw new UnsupportedOperationException("descendingIterator is not supported");
	}
	
	// Implements deque remove

	@Override
	public E pollFirst() 
	{
		if(this.nodeSize == 0L)
		{
			return null;
		}
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			Eyebolt<E> beginLink = begin.getLink();
			if(beginLink == null)
			{
				return null;
			}
			if(beginLink.nextLink == null)
			{
				return null;
			}
			E element = beginLink.nextLink.element;
			beginLink.nextLink.node.unlink();
			return element;
		}
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public E pollLast()
	{
		if(this.nodeSize == 0L)
		{
			return null;
		}
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			Eyebolt<E> endLink = end.getLink();
			if(endLink == null)
			{
				return null;
			}
			if(endLink.previewsLink == null)
			{
				return null;
			}
			E element = endLink.previewsLink.element;
			endLink.previewsLink.node.unlink();
			return element;
		} 
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public E removeFirst() 
	{
		if(this.nodeSize == 0L)
		{
			throw new NoSuchElementException();
		}
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			Eyebolt<E> beginLink = begin.getLink();
			if(beginLink == null)
			{
				throw new NoSuchElementException();
			}
			if(beginLink.nextLink == null)
			{
				throw new NoSuchElementException();
			}
			E element = beginLink.nextLink.element;
			beginLink.nextLink.node.unlink();
			return element;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	@Override
	public E removeLast() 
	{
		if(this.nodeSize == 0L)
		{
			throw new NoSuchElementException();
		}
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			Eyebolt<E> endLink = end.getLink();
			if(endLink == null)
			{
				throw new NoSuchElementException();
			}
			if(endLink.previewsLink == null)
			{
				throw new NoSuchElementException();
			}
			E element = endLink.previewsLink.element;
			endLink.previewsLink.node.unlink();
			return element;
		} 
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public boolean removeFirstOccurrence(Object o) 
	{
		return this.remove(o);
	}

	@Override
	public boolean removeLastOccurrence(Object o) 
	{
		Lock lock = this.writeLock;
		lock.lock();
		try
		{
			DequeSnapshot<E> snapshot = createSnapshot();
			try
			{
				DequeNode<E> last = null;
				if(o == null)
				{
					for(DequeNode<E> node : snapshot.nodeIterable())
					{
						if (node.getElement() == null) 
						{
							last = node;
						}
					}
				}
				else
				{
					for(DequeNode<E> node : snapshot.nodeIterable())
					{
						if (o.equals(node.getElement())) 
						{
							last = node;
						}
					}
				}
				if(last == null)
				{
					return false;
				}
				last.unlink();
				return true;
			}
			finally 
			{
				snapshot.close();
			}
		}
		finally 
		{
			lock.unlock();
		}
	}

	@Override
	public void close()
	{
		this.dispose();
	}

	
}
