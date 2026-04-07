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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.sodeac.common.snapdeque.SnapshotableDeque.Eyebolt;
import org.sodeac.common.snapdeque.SnapshotableDeque.LinkMode;
import org.sodeac.common.snapdeque.SnapshotableDeque.SnapshotVersion;

/**
 * In Snapshotable Deques a Node wraps one element.
 *
 * @param <E> the type of elements in deque
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 */
public class DequeNode<E>
{
    protected DequeNode(E element, SnapshotableDeque<E> snapshotableDeque, UUID id, Long timestamp, Long sequence)
    {
        super();
        this.snapshotableDeque = snapshotableDeque;
        this.element = element;
        this.id = id;
        this.timestamp = timestamp;
        this.sequence = sequence;
    }
    
    protected SnapshotableDeque<E> snapshotableDeque = null;
    protected E element = null;
    protected volatile Link<E> head = null;
    protected volatile long lastObsoleteOnVersion = Link.NO_OBSOLETE;
    private volatile int linkSize = 0;
    
    protected Long timestamp = null;
    protected Long sequence = null;
    protected UUID id = null;
    ;
    
    /**
     * helps gc
     */
    protected void dispose()
    {
        if (snapshotableDeque != null)
        {
            List<INodeEventHandler<E>> eventHandlerList = snapshotableDeque.eventHandlerList;
            if (eventHandlerList != null)
            {
                for (INodeEventHandler<E> eventHandler : eventHandlerList)
                {
                    try
                    {
                        eventHandler.onDisposeNode(this.snapshotableDeque, element);
                    }
                    catch (Exception e) { }
                    catch (Error e) { }
                }
            }
        }
        snapshotableDeque = null;
        element = null;
        head = null;
        
        id = null;
        timestamp = null;
        sequence = null;
    }
    
    public final boolean isLinked()
    {
        return this.head != null;
    }
    
    /**
     * Unlink node
     *
     * @return true, if node was linked to deque, otherwise false
     */
    public final boolean unlink()
    {
        if (!isPayload())
        {
            throw new IllegalStateException(new UnsupportedOperationException("node is not payload"));
        }
        Lock lock = snapshotableDeque.writeLock;
        lock.lock();
        try
        {
            Link<E> link = getLink();
            if (link == null)
            {
                return false;
            }
            return unlink(link);
        }
        finally
        {
            lock.unlock();
        }
        
    }
    
    /**
     * Internal helper method to unlink node
     *
     * @param link link
     *
     * @return true, if node was linked to deque, otherwise false
     */
    private final boolean unlink(Link<E> link)
    {
        if (!isPayload())
        {
            throw new RuntimeException(new UnsupportedOperationException("node is not payload"));
        }
        if (link == null)
        {
            return false;
        }
        
        SnapshotVersion<E> currentVersion = snapshotableDeque.getModificationVersion();
        Eyebolt<E> linkBegin = snapshotableDeque.begin.getLink();
        Eyebolt<E> linkEnd = snapshotableDeque.end.getLink();
        boolean isEndpoint;
        
        Link<E> prev = link.previewsLink;
        Link<E> next = link.nextLink;
        
        Link<E> nextOfNext = null;
        Link<E> previewsOfPreviews = null;
        if (next != linkEnd)
        {
            if (next.createOnVersion.getSequence() < currentVersion.getSequence())
            {
                if (!snapshotableDeque.openSnapshotVersionList.isEmpty())
                {
                    nextOfNext = next.nextLink;
                    next = next.createNewerLink(currentVersion, null);
                    next.nextLink = nextOfNext;
                    nextOfNext.previewsLink = next;
                }
            }
        }
        
        if (prev.createOnVersion.getSequence() < currentVersion.getSequence())
        {
            if (!snapshotableDeque.openSnapshotVersionList.isEmpty())
            {
                previewsOfPreviews = prev.previewsLink;
                if (prev.node != null)
                {
                    isEndpoint = !prev.node.isPayload();
                }
                else
                {
                    isEndpoint = prev instanceof Eyebolt;
                }
                prev = prev.createNewerLink(currentVersion, null);
                if (isEndpoint)
                {
                    linkBegin = snapshotableDeque.begin.getLink();
                }
                prev.previewsLink = previewsOfPreviews;
            }
        }
        
        // link next link to previews link
        next.previewsLink = prev;
        
        // link previews link to next link (set new route)
        prev.nextLink = next;
        
        if (previewsOfPreviews != null)
        {
            // set new route, if previews creates a new version
            previewsOfPreviews.nextLink = prev;
        }
        
        linkBegin.decrementSize();
        linkEnd.decrementSize();
        
        setHead(null, null);
        
        if (snapshotableDeque.openSnapshotVersionList.isEmpty())
        {
            link.obsoleteOnVersion = currentVersion.getSequence();
            link.node.lastObsoleteOnVersion = link.obsoleteOnVersion;
            link.clear(true);
        }
        else
        {
            snapshotableDeque.setObsolete(link);
        }
        
        return true;
    }
    
    /**
     * Internal helper method to get link object of node
     *
     * @return link object or null
     */
    protected Link<E> getLink()
    {
        return head;
    }
    
    /**
     * Internal helper method to create new link version
     *
     * @param currentVersion current version of deque
     * @param linkMode       append or prepend
     *
     * @return new link
     */
    protected Link<E> createHead(SnapshotVersion<E> currentVersion, SnapshotableDeque.LinkMode linkMode)
    {
        return setHead(new Link<>(this, currentVersion), linkMode);
    }
    
    /**
     * Internal helper method to set new link as head
     *
     * @param link     new link
     * @param linkMode append or prepend
     *
     * @return new link
     */
    protected Link<E> setHead(Link<E> link, SnapshotableDeque.LinkMode linkMode)
    {
        boolean startsWithEmptyState = linkSize == 0;
        try
        {
            boolean notify = false;
            if (link == null)
            {
                try
                {
                    if (this.head != null)
                    {
                        notify = true;
                        this.linkSize--;
                    }
                    this.head = link;
                    return head;
                }
                finally
                {
                    if ((notify) && isPayload() && (snapshotableDeque.eventHandlerList != null))
                    {
                        for (INodeEventHandler<E> eventHandler : snapshotableDeque.eventHandlerList)
                        {
                            try
                            {
                                eventHandler.onUnlink(this, snapshotableDeque.modificationVersion.getSequence());
                            }
                            catch (Exception e) { }
                            catch (Error e) { }
                        }
                    }
                }
            }
            else
            {
                try
                {
                    if (this.head == null)
                    {
                        if (startsWithEmptyState && (snapshotableDeque.nodeSize >= snapshotableDeque.capacity))
                        {
                            throw new CapacityExceededException(snapshotableDeque.capacity, "Can not link node, becase max size of deque is " + snapshotableDeque.capacity);
                        }
                        notify = true;
                        linkSize++;
                    }
                    this.head = link;
                    return head;
                }
                finally
                {
                    if (notify && isPayload())
                    {
                        if ((notify) && (snapshotableDeque.eventHandlerList != null))
                        {
                            for (INodeEventHandler<E> eventHandler : snapshotableDeque.eventHandlerList)
                            {
                                try
                                {
                                    eventHandler.onLink(this, linkMode, snapshotableDeque.modificationVersion.getSequence());
                                }
                                catch (Exception e) { }
                                catch (Error e) { }
                            }
                        }
                    }
                }
            }
        }
        finally
        {
            if (isPayload())
            {
                if ((linkSize > 0L) && (startsWithEmptyState))
                {
                    snapshotableDeque.nodeSize++;
                }
                else if ((linkSize == 0L) && (!startsWithEmptyState))
                {
                    snapshotableDeque.nodeSize--;
                }
            }
        }
    }
    
    /**
     * Getter for element (payload of node)
     *
     * @return element
     */
    public E getElement()
    {
        return element;
    }
    
    /**
     * getter for link timestamp, if deque has to generate metadata
     *
     * @return link timestamp
     */
    public Long getTimestamp()
    {
        return timestamp;
    }
    
    /**
     * getter for link sequence, if deque has to generate metadata
     *
     * @return link sequence
     */
    public Long getSequence()
    {
        return sequence;
    }
    
    /**
     * getter for link id, if deque has to generate metadata
     *
     * @return link id
     */
    public UUID getId()
    {
        return id;
    }
    
    /**
     * Internal method.
     *
     * @return node contains element as payload
     */
    protected boolean isPayload()
    {
        return true;
    }
    
    @Override
    public String toString()
    {
        return "Node payload: " + isPayload();
    }
    
    /**
     * Internal helper class link the elements / nodes between themselves
     *
     * @param <E>
     *
     * @author Sebastian Palarus
     */
    protected static class Link<E>
    {
        public static final long NO_OBSOLETE = -1L;
        
        protected Link(DequeNode<E> node, SnapshotVersion<E> version)
        {
            super();
            this.node = node;
            this.element = node.element;
            this.createOnVersion = version;
        }
        
        protected Link()
        {
            super();
            this.node = null;
            this.element = null;
            this.createOnVersion = null;
        }
        
        protected volatile long obsoleteOnVersion = NO_OBSOLETE;
        protected volatile DequeNode<E> node;
        protected volatile E element;
        protected volatile SnapshotVersion<E> createOnVersion;
        protected volatile Link<E> newerVersion = null;
        protected volatile Link<E> olderVersion = null;
        protected volatile Link<E> previewsLink = null;
        protected volatile Link<E> nextLink = null;
        
        protected Link<E> createNewerLink(SnapshotVersion<E> currentVersion, LinkMode linkMode)
        {
            Link<E> newVersion = new Link<>(this.node, currentVersion);
            newVersion.olderVersion = this;
            this.newerVersion = newVersion;
            this.node.snapshotableDeque.setObsolete(this);
            this.node.setHead(newVersion, linkMode);
            return newVersion;
        }
        
        public E getElement()
        {
            return element;
        }
        
        public DequeNode<E> getNode()
        {
            return node;
        }
        
        public boolean unlink()
        {
            DequeNode<E> node = this.node;
            if (node == null)
            {
                return false;
            }
            return node.unlink();
        }
        
        protected void clear()
        {
            clear(true);
        }
        
        private void clear(boolean nodeClear)
        {
            if (this.node != null)
            {
                if (nodeClear && (this.node.linkSize == 0) && (this.node.lastObsoleteOnVersion == this.obsoleteOnVersion))
                {
                    this.node.dispose();
                }
            }
            this.createOnVersion = null;
            this.newerVersion = null;
            this.olderVersion = null;
            this.previewsLink = null;
            this.nextLink = null;
            this.node = null;
            this.element = null;
        }
        
        @Override
        public String toString()
        {
            return
                node == null ? "link-version cleared away" :
                    (
                        "lVersion " + this.createOnVersion.getSequence()
                        + " hasNewer: " + (newerVersion != null)
                        + " hasOlder: " + (olderVersion != null)
                    );
        }
        
    }
}
