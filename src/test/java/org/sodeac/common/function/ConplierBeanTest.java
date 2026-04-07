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
package org.sodeac.common.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.Test;

public class ConplierBeanTest
{
    @Test
    public void test1()
    {
        final ConplierBean<String> conplier = new ConplierBean<String>("val");
        final Supplier<String> supplier = () -> conplier.get();
        assertEquals("supplied value should be correct", "val", supplier.get());
    }
    
    @Test
    public void test2()
    {
        final ConplierBean<String> conplier = new ConplierBean<String>();
        conplier.accept("val");
        assertEquals("supplied value should be correct", "val", conplier.getValue());
    }
    
    @Test
    public void test3()
    {
        final ConplierBean<String> conplier = new ConplierBean<String>();
        Collections.singletonList("val").stream().forEach(conplier);
        assertEquals("supplied value should be correct", "val", conplier.getValue());
        assertTrue("test should returns correct value", conplier.test(s -> "val".equals(s)));
        assertFalse("test should returns correct value", conplier.test(s -> "xxx".equals(s)));
    }
    
    @Test
    public void test4()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
        final UnaryOperator<Integer> increment = i -> ++i;
        conplier.unaryOperate(increment);
        assertEquals("operated value should be correct", Integer.valueOf(43), conplier.getValue());
    }
    
    @Test
    public void test5()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
        assertEquals("operated value should be correct", Integer.valueOf(43), conplier.unaryOperate(i -> ++i));
        assertEquals("operated value should be correct", Integer.valueOf(44), conplier.unaryOperate(i -> ++i));
    }
    
    @Test
    public void test6()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
        final BinaryOperator<Integer> add = (i, p) -> i + p;
        conplier.binaryOperate(add, 10);
        assertEquals("value should be correct", Integer.valueOf(52), conplier.get());
    }
    
    @Test
    public void test7()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
        assertEquals("value should be correct", Integer.valueOf(52), conplier.binaryOperate((i, p) -> i + p, 10));
        assertEquals("value should be correct", Integer.valueOf(62), conplier.binaryOperate((i, p) -> i + p, 10));
    }
    
    @Test
    public void test8()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
        final ConplierBean<Boolean> consumed = new ConplierBean<Boolean>(false);
        
        conplier.supply((v) ->
                        {
                            consumed.setValue(true);
                            assertEquals("value should be correct", 42, v.intValue());
                        });
        
        assertTrue("consumed flag should be correct", consumed.get());
    }
    
    @Test
    public void test9()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
        
        assertEquals("value should be correct", 1, conplier.get().intValue());
        conplier.consume(() -> 42);
        assertEquals("value should be correct", 42, conplier.get().intValue());
    }
    
    @Test
    public void test11()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>();
        assertFalse("optional should be not present", conplier.getOptional().isPresent());
        
        conplier.accept(42);
        assertTrue("optional should be present", conplier.getOptional().isPresent());
        assertEquals("options.get should be correct", 42, conplier.getOptional().get().intValue());
        
        conplier.accept(null);
        assertFalse("optional should be not present", conplier.getOptional().isPresent());
        
        boolean throwsNSEE = false;
        try
        {
            conplier.getOptional().get();
        }
        catch (final java.util.NoSuchElementException e)
        {
            throwsNSEE = true;
        }
        
        assertTrue("optional should throws oSuchElementException", throwsNSEE);
    }
    
    @Test
    public void test100()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
        final ConplierBean<Integer> mirror = new ConplierBean<Integer>(-1);
        
        assertEquals("value should be correct", 1, conplier.get().intValue());
        assertEquals("value should be correct", -1, mirror.get().intValue());
        
        final PropertyChangeListener pcl = new PropertyChangeListener()
        {
            
            @Override
            public void propertyChange(final PropertyChangeEvent evt)
            {
                assertEquals("value should be correct", 1, ((Integer) evt.getOldValue()).intValue());
                assertEquals("value should be correct", 42, ((Integer) evt.getNewValue()).intValue());
                
                mirror.setValue((Integer) evt.getNewValue());
            }
        };
        conplier.addPropertyChangeListener(pcl);
        
        conplier.setValue(42);
        
        assertEquals("value should be correct", 42, conplier.get().intValue());
        assertEquals("value should be correct", 42, mirror.get().intValue());
        
        conplier.removePropertyChangeListener(pcl);
        
        conplier.setValue(1);
        
        assertEquals("value should be correct", 1, conplier.get().intValue());
        assertEquals("value should be correct", 42, mirror.get().intValue());
        
    }
    
    @Test
    public void test101()
    {
        final ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
        
        assertEquals("value should be correct", 1, conplier.get().intValue());
        
        final ConplierBean<Boolean> consumed = new ConplierBean<Boolean>(false);
        
        final ConplierBean<Integer>.ConsumeOnChangeListener pcl =
            conplier.consumeOnChange(new Consumer<Integer>()
                                     {
                                         @Override
                                         public void accept(final Integer t)
                                         {
                                             consumed.setValue(true);
                                             assertEquals("value should be correct", 42, t.intValue());
                                         }
                                     }
            );
        
        assertFalse("value should be not consumed", consumed.get());
        conplier.setValue(42);
        assertTrue("value should be consumed", consumed.get());
        
        consumed.setValue(false);
        pcl.unregister();
        conplier.setValue(1);
        assertFalse("value should be not consumed", consumed.get());
        
    }
    
    @Test
    public void test102()
    {
        // new Long() is deprecated
        final Long l1 = Long.valueOf(1L);
        final Long l2 = Long.valueOf(1L);
        
        final ConplierBean<Long> conplier1 = new ConplierBean<Long>(l1);
        final ConplierBean<Long> conplier2 = new ConplierBean<Long>(l2);
        
        assertEquals("equals should return correct value", conplier1, conplier2);
        
        // since new Long() is deprecated, equalsBySameValue has no meaning here (also new String, new Integer...)
        conplier1.setEqualsBySameValue(true);
        
        assertEquals("equals should return correct value", conplier1, conplier2);
        
        conplier2.setValue(l1);
        
        assertEquals("equals should return correct value", conplier1, conplier2);
        
    }
    
    @Test
    public void testEqualsBySameValue()
    {
        final ConplierBean<Person> conplier1 = new ConplierBean<>(new Person("Holger"));
        final ConplierBean<Person> conplier2 = new ConplierBean<>(new Person("Holger"));
        
        assertEquals("person1.equals(person2) must be true", conplier1, conplier2);
        
        conplier1.setEqualsBySameValue(true);
        assertNotEquals("person1 == person2 must be false", conplier1, conplier2);
        
        assertEquals("person2.equals(person1) must be true", conplier2, conplier1);
    }
    
    private record Person(String name) { }
    
}
