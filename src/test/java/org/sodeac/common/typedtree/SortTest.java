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
package org.sodeac.common.typedtree;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

public class SortTest
{
    @Test
    public void test1()
    {
        RootBranchNode<TestModel, SortTestType> sortTest = new TestModel().createRootNode(TestModel.sortTest);
        
        sortTest.setComperator(SortTestType.list, new Comparator<BranchNode<SortTestType, SortTestItemType>>()
                               {
                                   
                                   @Override
                                   public int compare(BranchNode<SortTestType, SortTestItemType> o1, BranchNode<SortTestType, SortTestItemType> o2)
                                   {
                                       return o1.getValue(SortTestItemType.random).compareTo(o2.getValue(SortTestItemType.random));
                                   }
                               }
        );
        
        Random r = new Random();
        
        for (int i = 0; i < 1000; i++)
        {
            List<Integer> masterList = new ArrayList<Integer>();
            
            for (int j = 0; j < i; j++)
            {
                int random = r.nextInt();
                masterList.add(random);
                
                sortTest.create(SortTestType.list, (p, n) -> n.setValue(SortTestItemType.random, random));
            }
            
            Collections.sort(masterList);
            
            List<BranchNode<SortTestType, SortTestItemType>> list = sortTest.getUnmodifiableNodeList(SortTestType.list);
            for (int j = 0; j < i; j++)
            {
                assertEquals("master list and node list should have same order", masterList.get(j), list.get(j).getValue(SortTestItemType.random));
                // System.out.println("\t\t" + masterList.get(j) + " - " + list.get(j).getValue(SortTestItemType.random));
            }
            
            assertEquals("master list and node list should haves same size", masterList.size(), list.size());
            
            sortTest.clear(SortTestType.list);
        }
        
    }
}
