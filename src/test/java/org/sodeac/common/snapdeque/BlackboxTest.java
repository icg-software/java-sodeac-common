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
package org.sodeac.common.snapdeque;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.snapdeque.DequeNode.Link;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BlackboxTest
{
    @Test
    public void test00001CreateDequeue()
    {
        new SnapshotableDeque<>().close();
        new SnapshotableDeque<>().close();
        new SnapshotableDeque<>().close();
        new SnapshotableDeque<>().close();
        new SnapshotableDeque<>().close();
    }
    
    @Test
    public void test00002CreateSimpleDequeue() throws Exception
    {
        SnapshotableDeque<String> deque = new SnapshotableDeque<String>();
        List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
        deque.addAll(content);
        
        assertEquals("deque size should be correct ", content.size(), deque.size());
        assertEquals("first element should be correct ", "1", deque.getFirst());
        assertEquals("last element should be correct ", "3", deque.getLast());
        
        DequeSnapshot<String> snapshot1 = deque.createSnapshot();
        assertNotNull("snapshot should not be null", snapshot1);
        assertEquals("snapshot first element should be correct ", "1", snapshot1.getFirstElement());
        assertEquals("snapshot first link should be correct ", "1", snapshot1.getFirstLink().getElement());
        assertEquals("snapshot last element should be correct ", "3", snapshot1.getLastElement());
        assertEquals("snapshot last link should be correct ", "3", snapshot1.getLastLink().getElement());
        
        assertEquals("snapshot.size() should be correct ", content.size(), snapshot1.size());
        Iterator<DequeNode<String>> nodeIterator = snapshot1.nodeIterable().iterator();
        Iterator<Link<String>> linkIterator = snapshot1.linkIterable().iterator();
        int index = 0;
        for (String str : snapshot1)
        {
            assertEquals("nextValue should be correct", content.get(index), str);
            assertTrue("hasNext node should be true", nodeIterator.hasNext());
            DequeNode<String> item = nodeIterator.next();
            assertNotNull("item should not be null", item);
            
            String valueByItem = item.getElement();
            assertEquals("element by item should be correct", str, valueByItem);
            
            assertTrue("hasNext node should be true", linkIterator.hasNext());
            Link<String> link = linkIterator.next();
            assertNotNull("link should not be null", link);
            
            index++;
        }
        assertEquals("size of snapshot should be correct", content.size(), index);
        
        deque.remove("2");
        
        assertNotNull("snapshot should not be null", snapshot1);
        assertEquals("snapshot first element should be correct ", "1", snapshot1.getFirstElement());
        assertEquals("snapshot first link should be correct ", "1", snapshot1.getFirstLink().getElement());
        assertEquals("snapshot last element should be correct ", "3", snapshot1.getLastElement());
        assertEquals("snapshot last link should be correct ", "3", snapshot1.getLastLink().getElement());
        
        assertEquals("snapshot.size() should be correct ", content.size(), snapshot1.size());
        nodeIterator = snapshot1.nodeIterable().iterator();
        linkIterator = snapshot1.linkIterable().iterator();
        index = 0;
        for (String str : snapshot1)
        {
            assertEquals("nextValue should be correct", content.get(index), str);
            assertTrue("hasNext node should be true", nodeIterator.hasNext());
            DequeNode<String> item = nodeIterator.next();
            assertNotNull("item should not be null", item);
            
            String valueByItem = item.getElement();
            assertEquals("element by item should be correct", str, valueByItem);
            
            assertTrue("hasNext node should be true", linkIterator.hasNext());
            Link<String> link = linkIterator.next();
            assertNotNull("link should not be null", link);
            
            index++;
        }
        assertEquals("size of snapshot should be correct", content.size(), index);
        assertEquals("deque size should be correct ", content.size() - 1, deque.size());
        assertEquals("first element should be correct ", "1", deque.getFirst());
        assertEquals("last element should be correct ", "3", deque.getLast());
        
        snapshot1.close();
        
        deque.close();
    }
    
    @Test
    public void test01001CollectionAdd()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            for (String element : content)
            {
                deque.add(element);
            }
            
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01051CollectionAddAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01101CollectionClear()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            content.clear();
            testEqualsNotCollection(content, deque);
            deque.clear();
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01151CollectionRemove()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            content.remove("1");
            testEqualsNotCollection(content, deque);
            deque.remove("1");
            testEqualsCollection(content, deque);
            
            content.remove("2");
            testEqualsNotCollection(content, deque);
            deque.remove("2");
            testEqualsCollection(content, deque);
            
            content.remove("3");
            testEqualsNotCollection(content, deque);
            deque.remove("3");
            testEqualsCollection(content, deque);
            
            content.remove("x");
            testEqualsCollection(content, deque);
            deque.remove("x");
            testEqualsCollection(content, deque);
            
            assertEquals("collections should be null", 0, content.size());
        }
    }
    
    @Test
    public void test01152CollectionRemove()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            content.remove("3");
            testEqualsNotCollection(content, deque);
            deque.remove("3");
            testEqualsCollection(content, deque);
            
            content.remove("2");
            testEqualsNotCollection(content, deque);
            deque.remove("2");
            testEqualsCollection(content, deque);
            
            content.remove("1");
            testEqualsNotCollection(content, deque);
            deque.remove("1");
            testEqualsCollection(content, deque);
            
            assertEquals("collections should be null", 0, content.size());
        }
    }
    
    @Test
    public void test01153CollectionRemove()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            content.remove("2");
            testEqualsNotCollection(content, deque);
            deque.remove("2");
            testEqualsCollection(content, deque);
            
            content.remove("1");
            testEqualsNotCollection(content, deque);
            deque.remove("1");
            testEqualsCollection(content, deque);
            
            content.remove("3");
            testEqualsNotCollection(content, deque);
            deque.remove("3");
            testEqualsCollection(content, deque);
            
            assertEquals("collections should be null", 0, content.size());
        }
    }
    
    @Test
    public void test01154CollectionRemove()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            content.remove("2");
            testEqualsNotCollection(content, deque);
            deque.remove("2");
            testEqualsCollection(content, deque);
            
            content.remove("3");
            testEqualsNotCollection(content, deque);
            deque.remove("3");
            testEqualsCollection(content, deque);
            
            content.remove("1");
            testEqualsNotCollection(content, deque);
            deque.remove("1");
            testEqualsCollection(content, deque);
            
            assertEquals("collections should be null", 0, content.size());
        }
    }
    
    @Test
    public void test01201CollectionContainsAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            List<String> compare = Arrays.asList(new String[] { "1", "2", "3" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] {});
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "1" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "2" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "3" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "x" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "1", "2" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "2", "1" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "1", "3" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "3", "1" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "1", "x" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            compare = Arrays.asList(new String[] { "1", "2", "3", "x" });
            assertEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(compare));
            testEqualsCollection(content, deque);
            
            assertNotEquals("collection#containsAll should return correct value", content.containsAll(compare), deque.containsAll(content));
            testEqualsCollection(content, deque);
            
            assertNotEquals("collection#containsAll should correct return value", content.containsAll(content), deque.containsAll(compare));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01251CollectionContains()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            assertEquals("collection#contains should return correct value", content.contains("1"), deque.contains("1"));
            testEqualsCollection(content, deque);
            
            assertEquals("collection#contains should return correct value", content.contains("2"), deque.contains("2"));
            testEqualsCollection(content, deque);
            
            assertEquals("collection#contains should return correct value", content.contains("3"), deque.contains("3"));
            testEqualsCollection(content, deque);
            
            assertEquals("collection#contains should return correct value", content.contains("x"), deque.contains("x"));
            testEqualsCollection(content, deque);
            
            assertNotEquals("collection#contains should return correct value", content.contains("1"), deque.contains("x"));
            testEqualsCollection(content, deque);
            
            assertNotEquals("collection#contains should return correct value", content.contains("x"), deque.contains("1"));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01301CollectionSize()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            assertEquals("collection#size should return correct value", content.size(), deque.size());
            testEqualsCollection(content, deque);
            
            content.add("4");
            deque.add("4");
            
            assertEquals("collection#size should return correct value", content.size(), deque.size());
            testEqualsCollection(content, deque);
            
            content.clear();
            deque.clear();
            
            assertEquals("collection#size should return correct value", content.size(), deque.size());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01351CollectionIsEmpty()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            assertEquals("collection#isEmpty should return correct value", content.isEmpty(), deque.isEmpty());
            testEqualsCollection(content, deque);
            
            content.add("4");
            deque.add("4");
            
            assertEquals("collection#isEmpty should return correct value", content.isEmpty(), deque.isEmpty());
            testEqualsCollection(content, deque);
            
            content.clear();
            deque.clear();
            
            assertEquals("collection#isEmpty should return correct value", content.isEmpty(), deque.isEmpty());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01401CollectionRemoveAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            deque.removeAll(content);
            testEqualsNotCollection(content, deque);
            content.removeAll(content);
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01402CollectionRemoveAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            deque.removeAll(Arrays.asList(new String[] { "1", "2" }));
            testEqualsNotCollection(content, deque);
            content.removeAll(Arrays.asList(new String[] { "1", "2" }));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01403CollectionRemoveAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            deque.removeAll(Arrays.asList(new String[] { "2", "3", "4" }));
            testEqualsNotCollection(content, deque);
            content.removeAll(Arrays.asList(new String[] { "2", "3", "4" }));
            testEqualsCollection(content, deque);
        }
    }
    
    // retainAll
    
    @Test
    public void test01451CollectionRetainAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            deque.retainAll(content);
            testEqualsCollection(content, deque);
            content.retainAll(content);
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01452CollectionRetainAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            deque.retainAll(Arrays.asList(new String[] { "1", "2" }));
            testEqualsNotCollection(content, deque);
            content.retainAll(Arrays.asList(new String[] { "1", "2" }));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01453CollectionRetainAll()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            assertEquals("collection#isEmpty should return correct value", content.isEmpty(), deque.isEmpty());
            testEqualsCollection(content, deque);
            
            deque.retainAll(Arrays.asList(new String[] { "2", "3", "4" }));
            testEqualsNotCollection(content, deque);
            content.retainAll(Arrays.asList(new String[] { "2", "3", "4" }));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01501CollectionToArray()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            
            testEqualsCollection(content, deque);
            
            assertArrayEquals("array should be correct", content.toArray(), deque.toArray());
            
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01502CollectionToArray()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertArrayEquals("array should be correct", content.toArray(new String[3]), deque.toArray(new String[3]));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test01503CollectionToArray()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            List<String> content = new ArrayList<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertArrayEquals("array should be correct", content.toArray(new String[1]), deque.toArray(new String[1]));
            testEqualsCollection(content, deque);
            
            assertArrayEquals("array should be correct", content.toArray(new String[] { "x", "x", "x", "x", "x" }), deque.toArray(new String[] { "x", "x", "x", "x", "x" }));
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02001IDequeGetFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            content.removeFirst();
            testEqualsNotCollection(content, deque);
            deque.removeFirst();
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            content.addFirst("x");
            testEqualsNotCollection(content, deque);
            deque.addFirst("x");
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            content.clear();
            testEqualsNotCollection(content, deque);
            deque.clear();
            
            Exception e1 = null;
            Exception e2 = null;
            
            try
            {
                content.getFirst();
            }
            catch (Exception e)
            {
                e1 = e;
            }
            
            try
            {
                deque.getFirst();
            }
            catch (Exception e)
            {
                e2 = e;
            }
            assertEquals("exceptions should be correct", e1.getClass(), e2.getClass());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02051IDequeGetLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            content.removeLast();
            testEqualsNotCollection(content, deque);
            deque.removeLast();
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            content.addLast("x");
            testEqualsNotCollection(content, deque);
            deque.addLast("x");
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            content.clear();
            testEqualsNotCollection(content, deque);
            deque.clear();
            
            Exception e1 = null;
            Exception e2 = null;
            
            try
            {
                content.getLast();
            }
            catch (Exception e)
            {
                e1 = e;
            }
            
            try
            {
                deque.getLast();
            }
            catch (Exception e)
            {
                e2 = e;
            }
            assertEquals("exceptions should be correct", e1.getClass(), e2.getClass());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02101IDequePeekFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.peekFirst(), deque.peekFirst());
            testEqualsCollection(content, deque);
            
            content.removeFirst();
            testEqualsNotCollection(content, deque);
            deque.removeFirst();
            
            assertEquals("element should be correct", content.peekFirst(), deque.peekFirst());
            testEqualsCollection(content, deque);
            
            content.addFirst("x");
            testEqualsNotCollection(content, deque);
            deque.addFirst("x");
            
            assertEquals("element should be correct", content.peekFirst(), deque.peekFirst());
            testEqualsCollection(content, deque);
            
            content.clear();
            testEqualsNotCollection(content, deque);
            deque.clear();
            
            assertEquals("element should be correct", content.peekFirst(), deque.peekFirst());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02151IDequePeekLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.peekLast(), deque.peekLast());
            testEqualsCollection(content, deque);
            
            content.removeLast();
            testEqualsNotCollection(content, deque);
            deque.removeLast();
            
            assertEquals("element should be correct", content.peekLast(), deque.peekLast());
            testEqualsCollection(content, deque);
            
            content.addLast("x");
            testEqualsNotCollection(content, deque);
            deque.addLast("x");
            
            assertEquals("element should be correct", content.peekLast(), deque.peekLast());
            testEqualsCollection(content, deque);
            
            content.clear();
            testEqualsNotCollection(content, deque);
            deque.clear();
            
            assertEquals("element should be correct", content.peekLast(), deque.peekLast());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02201IDequeAddFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>(4))
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            deque.addFirst("x");
            testEqualsNotCollection(content, deque);
            content.addFirst("x");
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            try
            {
                deque.addFirst("y");
            }
            catch (IllegalStateException e)
            {
                assertEquals("size should be correct", content.size(), deque.size());
                assertEquals("element should be correct", content.getFirst(), deque.getFirst());
                return;
            }
            
            assertTrue("limit shoult throws exception", false);
            
        }
    }
    
    @Test
    public void test02251IDequeAddLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>(4))
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            deque.addLast("x");
            testEqualsNotCollection(content, deque);
            content.addLast("x");
            testEqualsCollection(content, deque);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            try
            {
                deque.addLast("y");
            }
            catch (IllegalStateException e)
            {
                assertEquals("size should be correct", content.size(), deque.size());
                assertEquals("element should be correct", content.getFirst(), deque.getFirst());
                return;
            }
            
            assertTrue("limit shoult throws exception", false);
            
        }
    }
    
    @Test
    public void test02301IDequeOfferFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>(4))
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            boolean offer1 = deque.offerFirst("x");
            testEqualsNotCollection(content, deque);
            boolean offer2 = content.offerFirst("x");
            testEqualsCollection(content, deque);
            assertEquals("offer return value should be correct", offer2, offer1);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            assertFalse("offer first should skiped by capacity", deque.offerFirst("y"));
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02351IDequeOfferLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>(4))
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            boolean offer1 = deque.offerLast("x");
            testEqualsNotCollection(content, deque);
            boolean offer2 = content.offerLast("x");
            testEqualsCollection(content, deque);
            assertEquals("offer return value should be correct", offer2, offer1);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            assertFalse("offer last should skiped by capacity", deque.offerLast("y"));
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
        }
    }
    
    @Test
    public void test02401IDequeRemoveFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            String value1 = deque.removeFirst();
            testEqualsNotCollection(content, deque);
            String value2 = content.removeFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            value1 = deque.removeFirst();
            testEqualsNotCollection(content, deque);
            value2 = content.removeFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            value1 = deque.removeFirst();
            testEqualsNotCollection(content, deque);
            value2 = content.removeFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            Exception e1 = null;
            try
            {
                deque.removeFirst();
            }
            catch (NoSuchElementException e)
            {
                e1 = e;
            }
            
            Exception e2 = null;
            try
            {
                content.removeFirst();
            }
            catch (NoSuchElementException e)
            {
                e2 = e;
            }
            
            assertEquals("exceptions should be correct", e1.getClass(), e2.getClass());
            testEqualsCollection(content, deque);
            
        }
    }
    
    @Test
    public void test02451IDequeRemoveLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            String value1 = deque.removeLast();
            testEqualsNotCollection(content, deque);
            String value2 = content.removeLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            value1 = deque.removeLast();
            testEqualsNotCollection(content, deque);
            value2 = content.removeLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            value1 = deque.removeLast();
            testEqualsNotCollection(content, deque);
            value2 = content.removeLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            Exception e1 = null;
            try
            {
                deque.removeLast();
            }
            catch (NoSuchElementException e)
            {
                e1 = e;
            }
            
            Exception e2 = null;
            try
            {
                content.removeLast();
            }
            catch (NoSuchElementException e)
            {
                e2 = e;
            }
            
            assertEquals("exceptions should be correct", e1.getClass(), e2.getClass());
            testEqualsCollection(content, deque);
            
        }
    }
    
    @Test
    public void test02501IDequePollFirst()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            String value1 = deque.pollFirst();
            testEqualsNotCollection(content, deque);
            String value2 = content.pollFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            value1 = deque.pollFirst();
            testEqualsNotCollection(content, deque);
            value2 = content.pollFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getFirst(), deque.getFirst());
            testEqualsCollection(content, deque);
            
            value1 = deque.pollFirst();
            testEqualsNotCollection(content, deque);
            value2 = content.pollFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            testEqualsCollection(content, deque);
            
            value1 = deque.pollFirst();
            testEqualsCollection(content, deque);
            value2 = content.pollFirst();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            testEqualsCollection(content, deque);
            
        }
    }
    
    @Test
    public void test02551IDequePollLast()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            String value1 = deque.pollLast();
            testEqualsNotCollection(content, deque);
            String value2 = content.pollLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            value1 = deque.pollLast();
            testEqualsNotCollection(content, deque);
            value2 = content.pollLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            
            assertEquals("element should be correct", content.getLast(), deque.getLast());
            testEqualsCollection(content, deque);
            
            value1 = deque.pollLast();
            testEqualsNotCollection(content, deque);
            value2 = content.pollLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            testEqualsCollection(content, deque);
            
            value1 = deque.pollLast();
            testEqualsCollection(content, deque);
            value2 = content.pollLast();
            testEqualsCollection(content, deque);
            assertEquals("element should be correct", value1, value2);
            testEqualsCollection(content, deque);
            
        }
    }
    
    @Test
    public void test02601IRemoveLastOccurrence()
    {
        try (SnapshotableDeque<String> deque = new SnapshotableDeque<String>())
        {
            ArrayDeque<String> content = new ArrayDeque<String>(Arrays.asList(new String[] { "1", "2", "3", "2", "6", "2", "1", "3" }));
            deque.addAll(content);
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("2");
            testEqualsNotCollection(content, deque);
            content.removeLastOccurrence("2");
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("2");
            testEqualsNotCollection(content, deque);
            content.removeLastOccurrence("2");
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("2");
            testEqualsNotCollection(content, deque);
            content.removeLastOccurrence("2");
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("2");
            testEqualsCollection(content, deque);
            content.removeLastOccurrence("2");
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("6");
            testEqualsNotCollection(content, deque);
            content.removeLastOccurrence("6");
            testEqualsCollection(content, deque);
            
            deque.removeLastOccurrence("1");
            testEqualsNotCollection(content, deque);
            content.removeLastOccurrence("1");
            testEqualsCollection(content, deque);
            
        }
    }
    
    private <T> void testEqualsCollection(Collection<T> collection, SnapshotableDeque<T> deque)
    {
        try (DequeSnapshot<T> snapshot = deque.createSnapshot())
        {
            assertEquals("collections size should equal", snapshot.size, deque.size());
            Iterator<T> collectionIterator = collection.iterator();
            Iterator<T> snapshotIterator = snapshot.iterator();
            
            while (collectionIterator.hasNext())
            {
                assertTrue("iterator#hasNext should be correct", snapshotIterator.hasNext());
                assertEquals("next element should be correct", collectionIterator.next(), snapshotIterator.next());
            }
            
            assertFalse("iterator#hasNext should be correct", snapshotIterator.hasNext());
            
            try
            {
                ((AutoCloseable) snapshotIterator).close();
            }
            catch (Exception e) { }
        }
    }
    
    private <T> void testEqualsNotCollection(Collection<T> collection, SnapshotableDeque<T> deque)
    {
        try (DequeSnapshot<T> snapshot = deque.createSnapshot())
        {
            Iterator<T> collectionIterator = collection.iterator();
            Iterator<T> snapshotIterator = snapshot.iterator();
            
            try
            {
                while (collectionIterator.hasNext())
                {
                    if (!snapshotIterator.hasNext())
                    {
                        return;
                    }
                    Object collectionObject = collectionIterator.next();
                    Object dequeObject = snapshotIterator.next();
                    
                    if (collectionObject == null)
                    {
                        if (dequeObject == null)
                        {
                            continue;
                        }
                        return;
                    }
                    if (!collectionObject.equals(dequeObject))
                    {
                        return;
                    }
                }
                
                if (snapshotIterator.hasNext())
                {
                    return;
                }
            }
            finally
            {
                try
                {
                    ((AutoCloseable) snapshotIterator).close();
                }
                catch (Exception e) { }
            }
        }
        
        assertTrue("collection and deque should be different", false);
    }
}
