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
package org.sodeac.common.xuri.ldapfilter;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPFilterEscapeSequenceTest
{
	@Test
	public void test0001NonEscapeTest()
	{
		assertEquals("value should be correct", LDAPFilterEscapeSequenceTest.class.getCanonicalName(), LDAPFilterExtension.decodeFromHexEscaped(LDAPFilterEscapeSequenceTest.class.getCanonicalName()));
	}
	
	@Test
	public void test0002ASCIISequenceTest()
	{
		String orig = "Please store the file in temp directory ( C:\\Temp )";
		String encoded = orig.replaceAll("\\\\", "\\\\5c").replaceAll("\\(", "\\\\28").replaceAll("\\)", "\\\\29");
		String decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", orig, decoded);
	}
	
	@Test
	public void test0003_2ByteSequenceTest()
	{
		String encoded = "<\\c2\\80>";
		String decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u0080>", decoded);
		
		encoded = "\\c2\\80";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "\u0080", decoded);
				
		// §
		encoded = "<\\c2\\A7>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u00A7>", decoded);
		
		// π
		encoded = "<\\cf\\80>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u03C0>", decoded);
		
		// ߐ
		encoded = "<\\df\\90>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u07D0>", decoded);
		
		// ߿
		encoded = "<\\df\\bf>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u07FF>", decoded);
		
	}
	
	@Test
	public void test0004_3ByteSequenceTest()
	{
		String encoded = "<\\e0\\a0\\80>";
		String decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u0800>", decoded);
		
		// ࿚ 	TIBETAN MARK TRAILING MCHAN RTAGS (U+0FDA) 	e0bf9a
		encoded = "<\\e0\\bf\\9a>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u0FDA>", decoded);
		
		// က 	MYANMAR LETTER KA (U+1000) 	e18080
		encoded = "<\\e1\\80\\80>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u1000>", decoded);
		
		// € 	EURO SIGN (U+20AC) 	e282ac
		encoded = "<\\e2\\82\\ac>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\u20AC>", decoded);
		
		// � 	REPLACEMENT CHARACTER (U+FFFD) 	efbfbd
		encoded = "<\\ef\\bf\\bd>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<\uFFFD>", decoded);
		
	}
	
	@Test
	public void test0005_4ByteSequenceTest()
	{
		// 𐀀 	LINEAR B SYLLABLE B008 A (U+10000) 	f0908080
		String encoded = "<\\f0\\90\\80\\80>";
		String decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<" + new String(Character.toChars(0x10000)) + ">", decoded);
		
		// 𤵞 	U+24D5E (U+24D5E) 	f0a4b59e
		
		encoded = "<\\f0\\a4\\b5\\9e>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<" + new String(Character.toChars(0x24D5E)) + ">", decoded);
		
		// 𱍉 	U+31349 (U+31349) 	f0b18d89
		encoded = "<\\f0\\b1\\8d\\89>";
		decoded = LDAPFilterExtension.decodeFromHexEscaped(encoded);
		assertEquals("value should be correct", "<" + new String(Character.toChars(0x31349)) + ">", decoded);
	}
}
