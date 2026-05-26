/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.misc;

import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SimpleShrinkableCacheTest
{
    @Test
    public void test00001SimpleTest()
    {
        SimpleShrinkableCache<String, String> ssc = new SimpleShrinkableCache<>();
        
        ssc.put("ABC", "abc");
        ssc.put("DEF", "def");
        ssc.put("GHI", "ghi");
        ssc.put("JKL", "jkl");
        
        ssc.get("ABC");
        
        assertEquals("value should be correct", 4, ssc.getView().size());
        ssc.shrink(3, null);
        assertEquals("value should be correct", 3, ssc.getView().size());
        
        int index = 0;
        for (final Entry<String, String> entry : ssc.getView().entrySet())
        {
            if (index == 0)
            {
                assertEquals("value should be correct", "ABC", entry.getKey());
                assertEquals("value should be correct", "abc", entry.getValue());
            }
            
            if (index == 1)
            {
                assertEquals("value should be correct", "GHI", entry.getKey());
                assertEquals("value should be correct", "ghi", entry.getValue());
            }
            
            if (index == 3)
            {
                assertEquals("value should be correct", "JKL", entry.getKey());
                assertEquals("value should be correct", "jkl", entry.getValue());
            }
            
            index++;
        }
        
        ssc.shrink(12, null);
        assertEquals("value should be correct", 3, ssc.getView().size());
        
        ssc.shrink(1, null);
        assertEquals("value should be correct", 1, ssc.getView().size());
        
        for (final Entry<String, String> entry : ssc.getView().entrySet())
        {
            assertEquals("value should be correct", "ABC", entry.getKey());
            assertEquals("value should be correct", "abc", entry.getValue());
        }
        
        ssc.clear();
        assertEquals("value should be correct", 0, ssc.getView().size());
    }
}
