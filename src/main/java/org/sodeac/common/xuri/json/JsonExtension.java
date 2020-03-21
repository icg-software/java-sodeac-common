/*******************************************************************************
 * Copyright (c) 2016, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri.json;

import java.io.Serializable;
import java.util.Map;

import javax.json.JsonObject;

import org.osgi.service.component.annotations.Component;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.xuri.IDecodingExtensionHandler;
import org.sodeac.common.xuri.IEncodingExtensionHandler;
import org.sodeac.common.xuri.IExtension;

/**
 * XURI filter extension for json parts
 * 
 * @author Sebastian Palarus
 */

@Component(service=IExtension.class,immediate=true)
public class JsonExtension implements IExtension<JsonObject>, Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6901232768391674155L;
	
	public static final String TYPE = "org.sodeac.xuri.json";
	
	public JsonExtension()
	{
		super();
	}
	
	public JsonExtension(String rawString)
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

	public JsonObject decodeFromString(String expression)
	{
		return JsonDecodingHandler.getInstance().decodeFromString(expression);
	}

	public String encodeToString(JsonObject extensionDataObject)
	{
		return JsonEncodingHandler.getInstance().encodeToString(extensionDataObject);
	}

	@Override
	public IDecodingExtensionHandler<JsonObject> getDecoder()
	{
		return JsonDecodingHandler.getInstance();
	}

	@Override
	public IEncodingExtensionHandler<JsonObject> getEncoder()
	{
		return JsonEncodingHandler.getInstance();
	}
	
	@Override
	public int driverIsApplicableFor(Map<String, Object> properties)
	{
		return IDriver.APPLICABLE_DEFAULT;
	}
}
