/*******************************************************************************
 * Copyright (c) 2016, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri.ldapfilter;

import java.io.Serializable;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.xuri.IDecodingExtensionHandler;
import org.sodeac.common.xuri.IEncodingExtensionHandler;
import org.sodeac.common.xuri.IExtension;

/**
 * XURI filter extension for ldap filter.
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 */
@Component(service=IExtension.class,immediate=true)
public class LDAPFilterExtension implements IExtension<IFilterItem>, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9054192628876497162L;
	
	public static final String TYPE = "org.sodeac.xuri.ldapfilter";
	
	public LDAPFilterExtension()
	{
		super();
	}
	
	public LDAPFilterExtension(String rawString)
	{
		super();
		this.rawString = rawString;
	}
	
	private String rawString = null;

	@Override
	public String getExpression()
	{
		return rawString;
	}

	@Override
	public String getType()
	{
		return TYPE;
	}

	public IFilterItem decodeFromString(String expression)
	{
		return LDAPFilterDecodingHandler.getInstance().decodeFromString(expression);
	}

	public String encodeToString(IFilterItem extensionDataObject)
	{
		return LDAPFilterEncodingHandler.getInstance().encodeToString(extensionDataObject);
	}

	@Override
	public IDecodingExtensionHandler<IFilterItem> getDecoder()
	{
		return LDAPFilterDecodingHandler.getInstance();
	}

	@Override
	public IEncodingExtensionHandler<IFilterItem> getEncoder()
	{
		return LDAPFilterEncodingHandler.getInstance();
	}
	
	@Override
	public int driverIsApplicableFor(Map<String, Object> properties)
	{
		return IDriver.APPLICABLE_DEFAULT;
	}
}
