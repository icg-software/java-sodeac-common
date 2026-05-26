/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri.ldapfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPFilterEnDecodingTest
{
    @Test
    public void test001SimpleEqualAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b");
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(a=b)", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertFalse("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test002SimpleNotEqualAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b").setInvert(true);
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(!(a=b))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertTrue("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test003SimpleLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.LTE).setRawValue("b");
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(a<=b)", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.LTE, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertFalse("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test004SimpleNotLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.LTE).setRawValue("b").setInvert(true);
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(!(a<=b))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.LTE, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertTrue("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test005SimpleLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.GTE).setRawValue("b");
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(a>=b)", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.GTE, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertFalse("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test006SimpleNotLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.GTE).setRawValue("b").setInvert(true);
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(!(a>=b))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.GTE, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertTrue("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test007SimpleLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.APPROX).setRawValue("b");
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(a~=b)", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertFalse("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test008SimpleNotLessAttribute()
    {
        Criteria attribute = new Criteria().setName("a").setOperator(ComparativeOperator.APPROX).setRawValue("b").setInvert(true);
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attribute);
        assertEquals("ldapfilterexpression should be correct", "(!(a~=b))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
        
        attribute = (Criteria) filterItem;
        assertEquals("attributename should be correct", "a", attribute.getName());
        assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attribute.getOperator());
        assertEquals("attributevalue should be correct", "b", attribute.getValue());
        assertTrue("invertflag should be correct", attribute.isInvert());
    }
    
    @Test
    public void test020SimpleAndLinker()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b");
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z");
        CriteriaLinker attributeLinker = new CriteriaLinker().setOperator(LogicalOperator.AND);
        attributeLinker.addItem(attr1);
        attributeLinker.addItem(attr2);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinker);
        assertEquals("ldapfilterexpression should be correct", "(&(a=b)(x~=z))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinker = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinker.getOperator());
        assertFalse("invertflag should be correct", attributeLinker.isInvert());
        
        List<IFilterItem> childs1 = attributeLinker.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
            
            if (i == 0)
            {
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertFalse("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                attr2 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "x", attr2.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                assertEquals("attributevalue should be correct", "z", attr2.getValue());
                assertFalse("invertflag should be correct", attr2.isInvert());
            }
        }
    }
    
    @Test
    public void test021SimpleAndLinkerNot1()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b").setInvert(true);
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z");
        CriteriaLinker attributeLinker = new CriteriaLinker().setOperator(LogicalOperator.AND);
        attributeLinker.addItem(attr1);
        attributeLinker.addItem(attr2);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinker);
        assertEquals("ldapfilterexpression should be correct", "(&(!(a=b))(x~=z))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinker = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinker.getOperator());
        assertFalse("invertflag should be correct", attributeLinker.isInvert());
        
        List<IFilterItem> childs1 = attributeLinker.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
            
            if (i == 0)
            {
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertTrue("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                attr2 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "x", attr2.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                assertEquals("attributevalue should be correct", "z", attr2.getValue());
                assertFalse("invertflag should be correct", attr2.isInvert());
            }
        }
    }
    
    @Test
    public void test022SimpleAndLinkerNot2()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b");
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z").setInvert(true);
        CriteriaLinker attributeLinker = new CriteriaLinker().setOperator(LogicalOperator.AND);
        attributeLinker.addItem(attr1);
        attributeLinker.addItem(attr2);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinker);
        assertEquals("ldapfilterexpression should be correct", "(&(a=b)(!(x~=z)))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinker = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinker.getOperator());
        assertFalse("invertflag should be correct", attributeLinker.isInvert());
        
        List<IFilterItem> childs1 = attributeLinker.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
            
            if (i == 0)
            {
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertFalse("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                attr2 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "x", attr2.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                assertEquals("attributevalue should be correct", "z", attr2.getValue());
                assertTrue("invertflag should be correct", attr2.isInvert());
            }
        }
    }
    
    @Test
    public void test023SimpleAndLinkerNot3()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b").setInvert(true);
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z").setInvert(true);
        CriteriaLinker attributeLinker = new CriteriaLinker().setOperator(LogicalOperator.AND);
        attributeLinker.addItem(attr1);
        attributeLinker.addItem(attr2);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinker);
        assertEquals("ldapfilterexpression should be correct", "(&(!(a=b))(!(x~=z)))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinker = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinker.getOperator());
        assertFalse("invertflag should be correct", attributeLinker.isInvert());
        
        List<IFilterItem> childs1 = attributeLinker.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
            
            if (i == 0)
            {
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertTrue("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                attr2 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "x", attr2.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                assertEquals("attributevalue should be correct", "z", attr2.getValue());
                assertTrue("invertflag should be correct", attr2.isInvert());
            }
        }
    }
    
    @Test
    public void test024SimpleAndLinkerNot4()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b");
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z");
        CriteriaLinker attributeLinker = new CriteriaLinker().setOperator(LogicalOperator.AND).setInvert(true);
        attributeLinker.addItem(attr1);
        attributeLinker.addItem(attr2);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinker);
        assertEquals("ldapfilterexpression should be correct", "(!(&(a=b)(x~=z)))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinker = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinker.getOperator());
        assertTrue("invertflag should be correct", attributeLinker.isInvert());
        
        List<IFilterItem> childs1 = attributeLinker.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
            
            if (i == 0)
            {
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertFalse("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                attr2 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "x", attr2.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                assertEquals("attributevalue should be correct", "z", attr2.getValue());
                assertFalse("invertflag should be correct", attr2.isInvert());
            }
        }
    }
    
    @Test
    public void test040Nested1()
    {
        Criteria attr1 = new Criteria().setName("a").setOperator(ComparativeOperator.EQUAL).setRawValue("b");
        Criteria attr2 = new Criteria().setName("x").setOperator(ComparativeOperator.APPROX).setRawValue("z");
        Criteria attr3 = new Criteria().setName("y").setOperator(ComparativeOperator.LTE).setRawValue("1");
        CriteriaLinker attributeLinkerNested = new CriteriaLinker().setOperator(LogicalOperator.AND).setInvert(true);
        attributeLinkerNested.addItem(attr2);
        attributeLinkerNested.addItem(attr3);
        CriteriaLinker attributeLinkerRoot = new CriteriaLinker().setOperator(LogicalOperator.OR);
        attributeLinkerRoot.addItem(attr1);
        attributeLinkerRoot.addItem(attributeLinkerNested);
        
        String filterExpression = LDAPFilterEncodingHandler.getInstance().encodeToString(attributeLinkerRoot);
        assertEquals("ldapfilterexpression should be correct", "(|(a=b)(!(&(x~=z)(y<=1))))", filterExpression);
        
        IFilterItem filterItem = LDAPFilterDecodingHandler.getInstance().decodeFromString(filterExpression);
        assertNotNull("parsed ldapfilter should not be null", filterItem);
        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
        
        attributeLinkerRoot = (CriteriaLinker) filterItem;
        assertEquals("parsed linker should contains correct operator", LogicalOperator.OR, attributeLinkerRoot.getOperator());
        assertFalse("invertflag should be correct", attributeLinkerRoot.isInvert());
        
        List<IFilterItem> childs1 = attributeLinkerRoot.getLinkedItemList();
        assertEquals("parsed linker should contains correct linked item size", 2, childs1.size());
        
        for (int i = 0; i < childs1.size(); i++)
        {
            filterItem = childs1.get(i);
            
            if (i == 0)
            {
                assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
                attr1 = (Criteria) filterItem;
                assertEquals("attributename should be correct", "a", attr1.getName());
                assertEquals("attributeoperator should be correct", ComparativeOperator.EQUAL, attr1.getOperator());
                assertEquals("attributevalue should be correct", "b", attr1.getValue());
                assertFalse("invertflag should be correct", attr1.isInvert());
            }
            
            if (i == 1)
            {
                assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof CriteriaLinker);
                attributeLinkerNested = (CriteriaLinker) filterItem;
                assertEquals("parsed linker should contains correct operator", LogicalOperator.AND, attributeLinkerNested.getOperator());
                assertTrue("invertflag should be correct", attributeLinkerNested.isInvert());
                
                List<IFilterItem> childs2 = attributeLinkerNested.getLinkedItemList();
                assertEquals("parsed linker should contains correct linked item size", 2, childs2.size());
                
                for (int j = 0; j < childs2.size(); j++)
                {
                    filterItem = childs2.get(j);
                    
                    if (j == 0)
                    {
                        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
                        attr2 = (Criteria) filterItem;
                        assertEquals("attributename should be correct", "x", attr2.getName());
                        assertEquals("attributeoperator should be correct", ComparativeOperator.APPROX, attr2.getOperator());
                        assertEquals("attributevalue should be correct", "z", attr2.getValue());
                        assertFalse("invertflag should be correct", attr2.isInvert());
                    }
                    
                    if (j == 1)
                    {
                        assertTrue("parsed ldapfilter should be correct filteritem type", filterItem instanceof Criteria);
                        attr3 = (Criteria) filterItem;
                        assertEquals("attributename should be correct", "y", attr3.getName());
                        assertEquals("attributeoperator should be correct", ComparativeOperator.LTE, attr3.getOperator());
                        assertEquals("attributevalue should be correct", "1", attr3.getValue());
                        assertFalse("invertflag should be correct", attr3.isInvert());
                    }
                }
            }
        }
    }
}
