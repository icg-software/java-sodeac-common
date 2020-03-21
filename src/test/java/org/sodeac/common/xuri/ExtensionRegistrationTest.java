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
package org.sodeac.common.xuri;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.xuri.ComponentType;
import org.sodeac.common.xuri.ExtensionHandleObject;
import org.sodeac.common.xuri.IDecodingExtensionHandler;
import org.sodeac.common.xuri.URI;
import org.sodeac.common.xuri.URIParser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExtensionRegistrationTest
{
	// TODO @Test
	public void test001Scheme()
	{
		URIParser parser = new URIParser();
		
		new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
		
		int sizeBefore = URIParser.getExtensionList(null, null).size();
		IExtension ext = new IExtension()
		{

			@Override
			public int driverIsApplicableFor(Map<String, Object> properties)
			{
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getType()
			{
				return "test";
			}

			@Override
			public String getExpression()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IDecodingExtensionHandler getDecoder()
			{
				return new IDecodingExtensionHandler()
				{
					
					@Override
					public String getType()
					{
						return "test";
					}

					@Override
					public int parseRawExtensionString(ExtensionHandleObject extensionHandleObject)
					{
						return -1;
					}

					@Override
					public int openerCharactersMatched(ExtensionHandleObject extensionHandleObject)
					{
						return -1;
					}

					@Override
					public ComponentType[] getApplicableComponents()
					{
						return new ComponentType[0];
					}
					
					@Override
					public String decodeFromString(String raw)
					{
						return raw;
					}

				};
			}

			@Override
			public IEncodingExtensionHandler getEncoder()
			{
				return null;
			}
		};
			
		URIParser.addExtension(ext);
		
		int sizeAfter = URIParser.getExtensionList(null, null).size();
		
		new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
		
		assertEquals("size encoding extension handler should be correct", sizeBefore + 1, sizeAfter);
		
		URIParser.removeExtension(ext);
		
		sizeAfter = URIParser.getExtensionList(null, null).size();
		
		assertEquals("size encoding extension handler should be correct", sizeBefore, sizeAfter);
	}
}
