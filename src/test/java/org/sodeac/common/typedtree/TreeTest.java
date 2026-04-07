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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.ILogService;
import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.model.logging.LogEventNodeType;
import org.sodeac.common.model.logging.LogEventType;
import org.sodeac.common.model.logging.LogLevel;
import org.sodeac.common.model.logging.LogPropertyNodeType;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TreeTest
{
	@Test
	public void test0001CreateModel()
	{
		assertNotNull("test mode should not be null", TypedTreeMetaModel.getInstance(TestModel.class));
	}
	
	@Test
	public void test0002CreateRootNode()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		assertNotNull("user root instance should not be null", user);
		assertSame("nodetype should be same",UserType.name,user.getLeafNodeTypeList().get(0));
		assertSame("nodetype should be same",UserType.address,user.getBranchNodeTypeList().get(0));
		user.dispose();
	}
	
	@Test
	public void test0021CreateRootNodeWithValue()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.setValue(UserType.name, "Mike");
		assertEquals("user name should be correct", "Mike", user.getValue(UserType.name));
		assertEquals("user name should be correct", "Mike", user.get(UserType.name).getValue());
		user.applyToConsumer(UserType.name, (p,u) -> u.setValue("Mikkel"));
		assertEquals("user name should be correct", "Mikkel", user.getValue(UserType.name));
		user.get(UserType.name).setValue("Michael");
		assertEquals("user name should be correct", "Michael", user.getValue(UserType.name));
		assertSame("nodetype should be same",TestModel.user,user.getNodeType());
		assertSame("nodetype should be same",UserType.name,user.get(UserType.name).getNodeType());
		user.dispose();
	}
	
	@Test
	public void test0023CreateRootNodeWithValueSync()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setSynchronized(true);
		user.setValue(UserType.name, "Mike");
		assertEquals("user name should be correct", "Mike", user.getValue(UserType.name));
		user.dispose();
	}
	
	@Test
	public void test0024CreateRootNodeWithValueImmutable()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setImmutable();
		user.setValue(UserType.name, "Mike");
		assertNull("user name should be correct", user.getValue(UserType.name));
		user.dispose();
	}
	
	@Test
	public void test0025CreateRootNodeWithChildNode()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.create(UserType.address).setValue(AddressType.city, "Berlin");
		assertEquals("city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		assertSame("nodetype should be same",UserType.address,user.get(UserType.address).getNodeType());
		user.dispose();
	}
	
	@Test
	public void test0026CreateRootNodeWithChildNodeSync()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setSynchronized(true);
		user.create(UserType.address).setValue(AddressType.city, "Berlin");
		assertEquals("city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.dispose();
	}
	
	@Test
	public void test0027CreateRootNodeWithChildNodeImmutable()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setImmutable();
		user.create(UserType.address);
		assertNull("city should be correct", user.get(UserType.address));
		user.dispose();
	}
	
	@Test
	public void test0028CreateRootNodeWithChildNodeAndRemove()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		BranchNode<UserType,AddressType> address = user.create(UserType.address).setValue(AddressType.city, "Berlin");
		assertEquals("user city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.remove(UserType.address);
		assertNull("city should be correct", user.get(UserType.address));
		assertNotNull("child node should be disposed", address.isDisposed());
		user.dispose();
	}
	
	@Test
	public void test0029CreateRootNodeWithChildNodeAndDispose()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.setValue(UserType.name, "Mike");
		BranchNode<UserType,AddressType> address = user.create(UserType.address).setValue(AddressType.city, "Berlin");
		assertEquals("user city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.dispose();
		
		assertTrue("root node should be disposed", user.isDisposed());
		assertTrue("child node should be disposed", address.isDisposed());
		
		assertNull("read lock should be disposed", user.getReadLock());
		assertNull("write lock should be disposed", user.getWriteLock());
	}
	
	@Test
	public void test0040CreateRootAndChildNonAutoCreate()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		assertNull("address should be null", user.get(UserType.address));
		user.dispose();
	}
	
	@Test
	public void test0041CreateRootAndChildAutoCreate()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setBranchNodeGetterAutoCreate(true);
		assertNotNull("address should be not null", user.get(UserType.address));
		user.dispose();
	}
	
	@Test
	public void test0050CreateRootAndConsume()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.applyToConsumer(u -> u.setValue(UserType.name, "Mike"));
		assertEquals("user name should be correct", "Mike", user.getValue(UserType.name));
		user.dispose();
	}
	
	@Test
	public void test0051CreateRootAndConsumeReadLock()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.applyToConsumerWithReadLock(u -> u.setValue(UserType.name, "Mike"));
		assertEquals("user name should be correct", "Mike", user.getValue(UserType.name));
		user.dispose();
	}
	
	@Test
	public void test0052CreateRootAndConsumeWriteLock()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.applyToConsumerWithWriteLock(u -> u.setValue(UserType.name, "Mike"));
		assertEquals("user name should be correct", "Mike", user.getValue(UserType.name));
		user.dispose();
	}
	
	@Test
	public void test0060CreateRootAndConsumeChildNodeSimpleNull()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.applyToConsumer(UserType.address, (u,a) -> { });
		assertNull("address should be not null", user.get(UserType.address));
		user.dispose();
	}
	
	@Test
	public void test0061CreateRootAndConsumeChildNodeSimpleNotNull()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user);
		user.create(UserType.address);
		user.applyToConsumer(UserType.address, (u,a) -> a.setValue(AddressType.city, "Berlin"));
		assertEquals("city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.dispose();
	}
	
	@Test
	public void test0062CreateRootAndConsumeChildNodeAutoCreate()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setBranchNodeApplyToConsumerAutoCreate(true);
		user.applyToConsumer(UserType.address, (u,a) -> a.setValue(AddressType.city, "Berlin"));
		assertEquals("city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.dispose();
	}
	
	@Test
	public void test0063CreateRootAndConsumeAbsentAndPresentChildNodeAutoCreate()
	{
		RootBranchNode<TestModel,UserType> user = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.user).setBranchNodeApplyToConsumerAutoCreate(true);
		user.applyToConsumer(UserType.address, (u,a) -> a.setValue(AddressType.city, "Berlin"), (u,a) -> a.setValue(AddressType.city, "Tallinn"));
		assertEquals("city should be correct", "Berlin", user.get(UserType.address).getValue(AddressType.city));
		user.applyToConsumer(UserType.address, (u,a) -> a.setValue(AddressType.city, "Berlin"), (u,a) -> a.setValue(AddressType.city, "Tallinn"));
		assertEquals("city should be correct", "Tallinn", user.get(UserType.address).getValue(AddressType.city));
		user.dispose();
	}
	
	@Test
	public void test0080CreateRootAndChildNodeList()
	{
		RootBranchNode<TestModel,CountryType> country = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.country);
		country.setValue(CountryType.name, "Schweiz");
		country.create(CountryType.languageList, (m,c) -> c.setValue(LangType.code, "de").setValue(LangType.name, "Deutsch"));
		country.create(CountryType.languageList, (m,c) -> c.setValue(LangType.code, "fr").setValue(LangType.name, "Französisch"));
		country.create(CountryType.languageList, (m,c) -> c.setValue(LangType.code, "it").setValue(LangType.name, "Italienisch"));
		List<BranchNode<CountryType,LangType>> languageList = country.getUnmodifiableNodeList(CountryType.languageList);
		assertEquals("size of language list should be correct", 3, languageList.size());
		assertEquals("code should be correct", "de", languageList.get(0).getValue(LangType.code));
		assertEquals("code should be correct", "fr", languageList.get(1).getValue(LangType.code));
		assertEquals("code should be correct", "it", languageList.get(2).getValue(LangType.code));
		country.dispose();
		assertEquals("size of language list should be correct", 0, languageList.size());
	}
	
	@Test
	public void test0081CreateRootAndChildNodeListIfNot()
	{
		RootBranchNode<TestModel,CountryType> country = TypedTreeMetaModel.getInstance(TestModel.class).createRootNode(TestModel.country);
		country.setValue(CountryType.name, "Schweiz");
		country.createIfAbsent(CountryType.languageList, n -> "de".equals(n.getValue(LangType.code)), (m,c) -> c.setValue(LangType.code, "de").setValue(LangType.name, "Deutsch"));
		country.createIfAbsent(CountryType.languageList, n -> "de".equals(n.getValue(LangType.code)), (m,c) -> c.setValue(LangType.code, "de").setValue(LangType.name, "Deutsch"));
		List<BranchNode<CountryType,LangType>> languageList = country.getUnmodifiableNodeList(CountryType.languageList);
		assertEquals("size of language list should be correct", 1, languageList.size());
		assertEquals("code should be correct", "de", languageList.get(0).getValue(LangType.code));
		assertEquals("code should be correct", "Deutsch", languageList.get(0).getValue(LangType.name));
		country.dispose();
		assertEquals("size of language list should be correct", 0, languageList.size());
	}
	
	@Test
	public void test0100Logging()
	{
		ConplierBean<BranchNode<?,LogEventNodeType>> logItem = new ConplierBean<>();
		ILogService.newLogService(TreeTest.class).addLoggerBackend(logItem).setAutoDispose(false).error("testlog", new RuntimeException());
		assertEquals("value should be correct", LogLevel.ERROR.name(), logItem.get().getValue(LogEventNodeType.logLevelName));
		assertEquals("value should be correct", "testlog", logItem.get().getValue(LogEventNodeType.message));
		assertEquals("list size should be correct", 1, logItem.get().getUnmodifiableNodeList(LogEventNodeType.properties).size());
		BranchNode<LogEventNodeType,LogPropertyNodeType> property = logItem.get().getUnmodifiableNodeList(LogEventNodeType.properties).get(0);
		assertEquals("value should be correct", "THROWABLE",property.getValue(LogPropertyNodeType.type));
		assertTrue("value should be correct",property.getValue(LogPropertyNodeType.value).startsWith("<?xml "));
		assertTrue("value should be correct",property.getValue(LogPropertyNodeType.value).substring(0, 54).contains("version=\"1.0\""));
		assertTrue("value should be correct",property.getValue(LogPropertyNodeType.value).substring(0, 54).contains("encoding=\"UTF-8\""));
		assertTrue("value should be correct",property.getValue(LogPropertyNodeType.value).substring(0, 54).contains("<Throwable "));
	}
}
