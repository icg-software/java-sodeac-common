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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sodeac.common.snapdeque.DequeNode.Link;
import org.sodeac.common.snapdeque.SnapshotableDeque.ClearCompleteForwardBranch;
import org.sodeac.common.snapdeque.SnapshotableDeque.Eyebolt;
import org.sodeac.common.snapdeque.SnapshotableDeque.SnapshotVersion;

/**
 * A Snapshot of {@link SnapshotableDeque} implements {@link Collection}.
 * Write operations of collection ({@link Collection#add(Object) ...}) impacts the source deque, not the snapshot itself.
 * <p>
 * A snapshot is not a deep copy of deque's content at the time of creation. The overhead to create a snapshot is very slightly, regardless of deque's size.
 * Iterate through a snapshot also does not produce a deep copy and requires no locks inside of snapshot or source deque.
 * <p>
 * A Snapshot has to be closed if it is not required anymore. Otherwise the helper links of removed elements remain in source deque and the gc does not release the memory.
 *
 * @param <E>
 *
 * @author Sebastian Palarus
 */
public class DequeSnapshot<E> implements AutoCloseable, Collection<E>
{
    protected UUID uuid;
    protected SnapshotVersion<E> version;
    protected SnapshotableDeque<E> snapshotableDeque;
    protected Link<E> firstLink;
    protected Link<E> lastLink;
    protected volatile boolean closed;
    protected long size;
    protected Integer pollCapacity;
    
    // Has to be run in write lock !!!
    protected DequeSnapshot(SnapshotableDeque<E> snapshotableDeque, boolean poll, Integer pollCapacity)
    {
        super();
        this.uuid = UUID.randomUUID();
        this.closed = false;
        this.snapshotableDeque = snapshotableDeque;
        this.pollCapacity = pollCapacity;
        
        if (this.snapshotableDeque.snapshotVersion == null)
        {
            this.snapshotableDeque.snapshotVersion = this.snapshotableDeque.modificationVersion;
            this.snapshotableDeque.openSnapshotVersionList.add(this.snapshotableDeque.snapshotVersion);
        }
        this.version = this.snapshotableDeque.snapshotVersion;
        this.version.addSnapshot(this);
        
        Eyebolt<E> beginLink = this.snapshotableDeque.begin.getLink();
        if (beginLink == null)
        {
            firstLink = null;
            size = 0;
        }
        else
        {
            firstLink = beginLink.nextLink;
            this.size = beginLink.getSize();
        }
        
        Eyebolt<E> endLink = snapshotableDeque.end.getLink();
        lastLink = endLink == null ? null : endLink.previewsLink;
        
        if (poll && (this.size > 0))
        {
            SnapshotVersion<E> modificationVersion = this.snapshotableDeque.getModificationVersion();
            
            if ((pollCapacity == null) || (pollCapacity.intValue() <= this.size))
            {
                beginLink = beginLink.createNewerLink(modificationVersion, null);
                endLink.previewsLink = beginLink;
                beginLink.nextLink = endLink;
                beginLink.setSize(0);
                endLink.setSize(0);
                
                if (beginLink.olderVersion.nextLink != null)
                {
                    this.snapshotableDeque.setObsolete(new ClearCompleteForwardBranch<E>(beginLink.olderVersion.nextLink));
                    
                    Link<E> clearLink = beginLink.olderVersion.nextLink;
                    Link<E> nextLink;
                    while (clearLink != null)
                    {
                        nextLink = clearLink.nextLink;
                        
                        if (clearLink.node != null)
                        {
                            if (!clearLink.node.isPayload())
                            {
                                break;
                            }
                            
                            clearLink.node.setHead(null, null);
                        }
                        
                        clearLink = nextLink;
                    }
                    
                    beginLink.olderVersion.clear();
                }
            }
            else
            {
                NodeSnapshotIterator nodeIterator = new NodeSnapshotIterator();
                try
                {
                    while (nodeIterator.hasNext())
                    {
                        nodeIterator.next().unlink();
                    }
                }
                finally
                {
                    nodeIterator.close();
                }
            }
        }
        
        if ((pollCapacity != null) && (pollCapacity < this.size))
        {
            this.size = pollCapacity;
        }
    }
    
    protected SnapshotableDeque<E> getParent()
    {
        return snapshotableDeque;
    }
    
    @Override
    public void close()
    {
        if (closed)
        {
            return;
        }
        Lock lock = this.snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            closed = true;
            SnapshotVersion<E> version = this.version;
            if (version != null)
            {
                version.removeSnapshot(this);
            }
        }
        finally
        {
            lock.unlock();
        }
        
        this.uuid = null;
        this.version = null;
        this.snapshotableDeque = null;
        this.firstLink = null;
        this.lastLink = null;
    }
    
    /**
     * returns true if snapshot is closed, otherwise false
     *
     * @return true if snapshot is closed, otherwise false
     */
    public boolean isClosed()
    {
        return closed;
    }
    
    /**
     * Returns the version of deque the snapshot was created
     *
     * @return version of deque the snapshot was created
     */
    public long getVersion()
    {
        return this.version.getSequence();
    }
    
    /**
     * Find link by element (same object)
     *
     * @param o element
     *
     * @return matched link
     */
    protected Link<E> getLink(E o)
    {
        for (Link<E> element : this.linkIterable())
        {
            if (element.node.getElement() == o)
            {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Find node by element (same object)
     *
     * @param o element
     *
     * @return matched node
     */
    public DequeNode<E> getNode(E o)
    {
        for (Link<E> element : this.linkIterable())
        {
            if (element.node.getElement() == o)
            {
                return element.node;
            }
        }
        return null;
    }
    
    /**
     * Find unlinked node by element (same object)
     *
     * @param o element
     *
     * @return matched node
     */
    public DequeNode<E> getLinkedNode(E o)
    {
        for (Link<E> element : this.linkIterable())
        {
            if (element.node.getElement() == o)
            {
                if (element.node.isLinked())
                {
                    return element.node;
                }
            }
        }
        return null;
    }
    
    @Override
    public Iterator<E> iterator()
    {
        if (closed)
        {
            throw new RuntimeException("snapshot is closed");
        }
        return new ElementSnapshotIterator();
    }
    
    /**
     * Returns link iterable
     *
     * @return link iterable
     */
    protected Iterable<Link<E>> linkIterable()
    {
        if (closed)
        {
            throw new RuntimeException("snapshot is closed");
        }
        return new Iterable<Link<E>>()
        {
            public Iterator<Link<E>> iterator()
            {
                return new LinkSnapshotIterator();
            }
        };
    }
    
    /**
     * Returns node iterable
     *
     * @return node iterable
     */
    public Iterable<DequeNode<E>> nodeIterable()
    {
        checkClosed();
        return new Iterable<DequeNode<E>>()
        {
            public Iterator<DequeNode<E>> iterator()
            {
                return new NodeSnapshotIterator();
            }
        };
    }
    
    /**
     * Returns node stream
     *
     * @return node stream
     */
    public Stream<DequeNode<E>> nodeStream()
    {
        return StreamSupport.stream(nodeIterable().spliterator(), false);
    }
    
    @Override
    public int size()
    {
        checkClosed();
        return (int) this.size;
    }
    
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }
    
    @Override
    public boolean contains(Object o)
    {
        checkClosed();
        if (o == null)
        {
            DequeNode<E> node = null;
            NodeSnapshotIterator it = new NodeSnapshotIterator();
            try
            {
                while (it.hasNext())
                {
                    node = it.next();
                    if (!node.isLinked())
                    {
                        continue;
                    }
                    if (node.getElement() == null)
                    {
                        return true;
                    }
                }
            }
            finally
            {
                it.close();
                it = null;
                node = null;
            }
        }
        else
        {
            DequeNode<E> node = null;
            NodeSnapshotIterator it = new NodeSnapshotIterator();
            try
            {
                while (it.hasNext())
                {
                    node = it.next();
                    if (!node.isLinked())
                    {
                        continue;
                    }
                    if (o.equals(node.getElement()))
                    {
                        return true;
                    }
                }
            }
            finally
            {
                it.close();
                it = null;
                node = null;
            }
        }
        return false;
    }
    
    @Override
    public Object[] toArray()
    {
        if (this.size > ((long) Integer.MAX_VALUE))
        {
            throw new IllegalStateException("too many elements to create an array : " + this.size);
        }
        checkClosed();
        Object[] array = new Object[size()];
        int index = 0;
        for (E element : this)
        {
            array[index] = element;
            index++;
        }
        return array;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a)
    {
        if (this.size > ((long) Integer.MAX_VALUE))
        {
            throw new IllegalStateException("too many elements to create an array : " + this.size);
        }
        
        checkClosed();
        if (a.length < size())
        {
            a = (T[]) new Object[size()];
        }
        int index = 0;
        for (E element : this)
        {
            a[index] = (T) element;
            index++;
        }
        if (a.length > size())
        {
            a[size()] = null;
        }
        return a;
    }
    
    @Override
    public boolean add(E e)
    {
        return this.snapshotableDeque.add(e);
    }
    
    @Override
    public boolean remove(Object o)
    {
        checkClosed();
        Lock lock = this.snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            if (o == null)
            {
                for (DequeNode<E> node : nodeIterable())
                {
                    if (node.getElement() == null)
                    {
                        if (node.unlink())
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                for (DequeNode<E> node : nodeIterable())
                {
                    if (o.equals(node.getElement()))
                    {
                        if (node.unlink())
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        finally
        {
            lock.unlock();
        }
    }
    
    @Override
    public boolean containsAll(Collection<?> c)
    {
        Objects.requireNonNull(c);
        checkClosed();
        col:
        for (Object o : c)
        {
            if (o == null)
            {
                DequeNode<E> node = null;
                NodeSnapshotIterator it = new NodeSnapshotIterator();
                try
                {
                    while (it.hasNext())
                    {
                        node = it.next();
                        if (!node.isLinked())
                        {
                            continue;
                        }
                        if (node.getElement() == null)
                        {
                            continue col;
                        }
                    }
                }
                finally
                {
                    it.close();
                    it = null;
                    node = null;
                }
            }
            else
            {
                DequeNode<E> node = null;
                NodeSnapshotIterator it = new NodeSnapshotIterator();
                try
                {
                    while (it.hasNext())
                    {
                        node = it.next();
                        if (!node.isLinked())
                        {
                            continue;
                        }
                        if (o.equals(node.getElement()))
                        {
                            continue col;
                        }
                    }
                }
                finally
                {
                    it.close();
                    it = null;
                    node = null;
                }
            }
            return false;
        }
        return true;
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        checkClosed();
        return this.snapshotableDeque.addAll(c);
    }
    
    @Override
    public boolean removeAll(Collection<?> c)
    {
        Objects.requireNonNull(c);
        checkClosed();
        
        Lock lock = this.snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            boolean modified = false;
            for (DequeNode<E> node : nodeIterable())
            {
                if (c.contains(node.getElement()))
                {
                    if (node.unlink())
                    {
                        modified = true;
                    }
                }
            }
            return modified;
        }
        finally
        {
            lock.unlock();
        }
    }
    
    @Override
    public boolean retainAll(Collection<?> c)
    {
        checkClosed();
        Objects.requireNonNull(c);
        
        Lock lock = this.snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            boolean modified = false;
            for (DequeNode<E> node : nodeIterable())
            {
                if (!c.contains(node.getElement()))
                {
                    if (node.unlink())
                    {
                        modified = true;
                    }
                }
            }
            return modified;
        }
        finally
        {
            lock.unlock();
        }
    }
    
    @Override
    public void clear()
    {
        checkClosed();
        Lock lock = this.snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            for (DequeNode<E> node : nodeIterable())
            {
                node.unlink();
            }
        }
        finally
        {
            lock.unlock();
        }
    }
    
    /**
     * Returns first element
     *
     * @return first element
     */
    public E getFirstElement()
    {
        checkClosed();
        if (this.firstLink == null)
        {
            throw new NoSuchElementException();
        }
        return this.firstLink.element;
    }
    
    protected Link<E> getFirstLink()
    {
        checkClosed();
        return this.firstLink;
    }
    
    /**
     * Returns first node
     *
     * @return first node
     */
    public DequeNode<E> getFirstNode()
    {
        checkClosed();
        return this.firstLink == null ? null : this.firstLink.node;
    }
    
    /**
     * Returns last element
     *
     * @return last element
     */
    public E getLastElement()
    {
        checkClosed();
        if (this.lastLink == null)
        {
            throw new NoSuchElementException();
        }
        return this.lastLink.element;
    }
    
    protected Link<E> getLastLink()
    {
        checkClosed();
        return this.lastLink;
    }
    
    /**
     * Returns last node
     *
     * @return last node
     */
    public DequeNode<E> getLastNode()
    {
        checkClosed();
        return this.lastLink == null ? null : this.lastLink.node;
    }
    
    private void checkClosed()
    {
        if (this.closed)
        {
            throw new RuntimeException("snapshot is closed");
        }
    }
    
    /**
     * private helper class
     *
     * @author Sebastian Palarus
     *
     */
    protected class LinkSnapshotIterator extends SnapshotIterator implements Iterator<Link<E>>
    {
        private volatile DequeNode<E> removable = null;
        
        @Override
        public Link<E> next()
        {
            Link<E> link = super.nextLink();
            this.removable = link.node;
            return link;
        }
        
        @Override
        public void remove()
        {
            if (removable != null)
            {
                removable.unlink();
            }
        }
        
        @Override
        public void close()
        {
            super.close();
            this.removable = null;
        }
    }
    
    /**
     * private helper class
     *
     * @author Sebastian Palarus
     *
     */
    protected class NodeSnapshotIterator extends SnapshotIterator implements Iterator<DequeNode<E>>
    {
        private volatile DequeNode<E> removable = null;
        
        @Override
        public DequeNode<E> next()
        {
            Link<E> link = super.nextLink();
            this.removable = link.node;
            return link.node;
        }
        
        @Override
        public void remove()
        {
            if (removable != null)
            {
                removable.unlink();
            }
        }
        
        @Override
        public void close()
        {
            super.close();
            this.removable = null;
        }
    }
    
    /**
     * private helper class
     *
     * @author Sebastian Palarus
     *
     */
    protected class ElementSnapshotIterator extends SnapshotIterator implements Iterator<E>
    {
        private volatile DequeNode<E> removable = null;
        
        @Override
        public E next()
        {
            Link<E> link = super.nextLink();
            this.removable = link.node;
            return link.element;
        }
        
        @Override
        public void remove()
        {
            if (removable != null)
            {
                removable.unlink();
            }
        }
        
        @Override
        public void close()
        {
            super.close();
            this.removable = null;
        }
        
    }
    
    /**
     * private helper class
     *
     * @author Sebastian Palarus
     *
     */
    protected abstract class SnapshotIterator implements AutoCloseable
    {
        private Link<E> previews = null;
        private Link<E> next = null;
        private boolean nextCalculated = false;
        private int provided = 0;
        
        private SnapshotIterator()
        {
            super();
            this.next = DequeSnapshot.this.firstLink;
            nextCalculated = true;
        }
        
        public boolean hasNext()
        {
            if (DequeSnapshot.this.closed)
            {
                throw new RuntimeException("snapshot is closed");
            }
            
            nextCalculated = true;
            
            if (DequeSnapshot.this.size <= this.provided)
            {
                this.next = null;
                this.previews = null;
                return false;
            }
            
            if (next != null)
            {
                return true;
            }
            
            if (this.previews == null)
            {
                this.next = null;
                return false;
            }
            this.next = this.previews.nextLink;
            if (this.next == null)
            {
                this.previews = null;
                return false;
            }
            
            if (this.next.createOnVersion.getSequence() > DequeSnapshot.this.version.getSequence())
            {
                while (this.next.createOnVersion.getSequence() > DequeSnapshot.this.version.getSequence())
                {
                    this.next = this.next.olderVersion;
                    if (this.next == null)
                    {
                        this.previews = null;
                        throw new RuntimeException("missing link with version " + DequeSnapshot.this.version.getSequence());
                    }
                    if (this.next.createOnVersion == null)
                    {
                        this.previews = null;
                        throw new RuntimeException("missing link with version " + DequeSnapshot.this.version.getSequence() + " (older is cleared)");
                    }
                }
            }
            else if (this.next.createOnVersion.getSequence() < DequeSnapshot.this.version.getSequence())
            {
                while (this.next.newerVersion != null)
                {
                    if (this.next.newerVersion.createOnVersion == null)
                    {
                        break;
                    }
                    if (this.next.newerVersion.createOnVersion.getSequence() > DequeSnapshot.this.version.getSequence())
                    {
                        break;
                    }
                    this.next = this.next.newerVersion;
                }
            }
            if (!this.next.node.isPayload())
            {
                this.previews = null;
                this.next = null;
                return false;
            }
            return true;
        }
        
        private Link<E> nextLink()
        {
            if (DequeSnapshot.this.closed)
            {
                throw new RuntimeException("snapshot is closed");
            }
            try
            {
                if (!this.nextCalculated)
                {
                    if (!this.hasNext())
                    {
                        throw new NoSuchElementException();
                    }
                }
                if (this.next == null)
                {
                    throw new NoSuchElementException();
                }
                this.provided++;
                return this.next;
            }
            finally
            {
                this.previews = this.next;
                this.next = null;
                this.nextCalculated = false;
            }
        }
        
        @Override
        public void close()
        {
            this.previews = null;
            this.next = null;
        }
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((snapshotableDeque == null) ? 0 : snapshotableDeque.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return this == obj;
    }
    
}
