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

import org.sodeac.common.xuri.IEncodingExtensionHandler;

import jakarta.json.JsonObject;

/**
 * XURI encoding extension handler to encode json objects
 *
 * @author Sebastian Palarus
 *
 */
public class JsonEncodingHandler implements IEncodingExtensionHandler<JsonObject>, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 312956274744970550L;

    private static volatile JsonEncodingHandler INSTANCE = null;

    public static JsonEncodingHandler getInstance()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new JsonEncodingHandler();
        }
        return INSTANCE;
    }

    @Override
    public String getType()
    {
        return JsonExtension.TYPE;
    }

    @Override
    public String encodeToString(final JsonObject extensionDataObject)
    {
        return extensionDataObject.toString();
    }
}
