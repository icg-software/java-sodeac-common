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
package org.sodeac.common.xuri.ldapfilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.xuri.ldapfilter.DefaultMatchableWrapper;
import org.sodeac.common.xuri.ldapfilter.IMatchable;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterDecodingHandler;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultMatchableWrapperTest
{
	@Test
	public void test001EqualsString()
	{
		Map<String,IMatchable> props = new HashMap<String, IMatchable>();
		props.put("attr1", new DefaultMatchableWrapper("abcdef"));
		props.put("attr2", new DefaultMatchableWrapper("ABCDEF"));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=*)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attrX=*)").matches(props)));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=abcdef)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("( attr1 = abcdef )").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=abef)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=ABCDEF)").matches(props)));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=ABCDEF)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("( attr2 = ABCDEF )").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=ABEF)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=abcdef)").matches(props)));
		
	}
	
	@Test
	public void test002ApproxString()
	{
		Map<String,IMatchable> props = new HashMap<String, IMatchable>();
		props.put("attr1", new DefaultMatchableWrapper("abcdef"));
		props.put("attr2", new DefaultMatchableWrapper("ABCDEF"));
		
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1~=abcdef)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("( attr1 ~= abcdef )").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1~=abef)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1~=ABCDEF)").matches(props)));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2~=ABCDEF)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("( attr2 ~= ABCDEF )").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2~=ABEF)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2~=abcdef)").matches(props)));
		
	}
	
	@Test
	public void test03Long()
	{
		Map<String,IMatchable> props = new HashMap<String, IMatchable>();
		props.put("val", new DefaultMatchableWrapper(1L));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val=1)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val=2)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val>=0)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val<=2)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val>=2)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val<=0)").matches(props)));
	}
	
	@Test
	public void test04Boolean()
	{
		Map<String,IMatchable> props = new HashMap<String, IMatchable>();
		props.put("attr1", new DefaultMatchableWrapper(Boolean.FALSE));
		props.put("attr2", new DefaultMatchableWrapper(Boolean.TRUE));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=false)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=true)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=true)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=false)").matches(props)));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr1=fsdfsad)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(attr2=fasdfasd)").matches(props)));
	}
	
	@Test
	public void test05Double()
	{
		Map<String,IMatchable> props = new HashMap<String, IMatchable>();
		props.put("val", new DefaultMatchableWrapper(1.0d));
		
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val=1.0)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val=2.0)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val>=0.0)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val>=1.0)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val<=1.0)").matches(props)));
		assertTrue("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val<=2.0)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val>=2.0)").matches(props)));
		assertFalse("matcher should return expected value",(LDAPFilterDecodingHandler.getInstance().decodeFromString("(val<=0.0)").matches(props)));
	}
}
