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

package org.sodeac.common.xuri.json;

import java.io.Serializable;
import java.io.StringReader;

import org.sodeac.common.xuri.ComponentType;
import org.sodeac.common.xuri.ExtensionHandleObject;
import org.sodeac.common.xuri.FormatException;
import org.sodeac.common.xuri.IDecodingExtensionHandler;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * XURI decoding extension handler to decode json parts
 *
 * @author Sebastian Palarus
 *
 */
public class JsonDecodingHandler implements IDecodingExtensionHandler<JsonObject>, Serializable
{
    
    /**
     *
     */
    private static final long serialVersionUID = -4612916661495917506L;
    
    private static volatile JsonDecodingHandler INSTANCE = null;
    
    public static final char OPENER = '{';
    public static final char CLOSER = '}';
    public static final char ESCAPE = '\\';
    public static final char DOUBLE_QUOTE = '"';
    public static final char[] OPENER_CHARACTERS = new char[] { OPENER };
    public static final char[] CLOSER_CHARACTERS = new char[] { CLOSER };
    
    public static JsonDecodingHandler getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new JsonDecodingHandler();
        }
        return INSTANCE;
    }
    
    @Override
    public String getType()
    {
        return JsonExtension.TYPE;
    }
    
    @Override
    public ComponentType[] getApplicableComponents()
    {
        return new ComponentType[] { ComponentType.AUTHORITY, ComponentType.PATH, ComponentType.QUERY, ComponentType.FRAGMENT };
    }
    
    @Override
    public int parseRawExtensionString(final ExtensionHandleObject extensionHandleObject)
    {
        char c;
        int openerCount = 0;
        boolean inEscape = false;
        boolean inQuote = false;
        
        for (; extensionHandleObject.position < extensionHandleObject.fullPath.length(); extensionHandleObject.position++)
        {
            c = extensionHandleObject.fullPath.charAt(extensionHandleObject.position);
            
            if (inEscape)
            {
                inEscape = false;
                extensionHandleObject.rawResult.append(c);
                continue;
            }
            
            if (c == ESCAPE)
            {
                inEscape = true;
                extensionHandleObject.rawResult.append(c);
                continue;
            }
            
            if (c == DOUBLE_QUOTE)
            {
                extensionHandleObject.rawResult.append(c);
                inQuote = !inQuote;
                continue;
            }
            
            if (!inQuote)
            {
                if (c == OPENER)
                {
                    openerCount++;
                }
                
                if (c == CLOSER)
                {
                    if (openerCount == 0)
                    {
                        final String expression = extensionHandleObject.rawResult.toString();
                        extensionHandleObject.extension = new JsonExtension("{" + expression + "}");
                        
                        return extensionHandleObject.position + 1;
                    }
                    else
                    {
                        openerCount--;
                    }
                }
            }
            
            extensionHandleObject.rawResult.append(c);
        }
        
        throw new FormatException("no closing sequence \"" + new String(getCloserCharacters(extensionHandleObject.component)) + "\" found in " + getType() + " : " + extensionHandleObject.rawResult.toString());
    }
    
    @Override
    public int openerCharactersMatched(final ExtensionHandleObject extensionHandleObject)
    {
        return extensionHandleObject.fullPath.charAt(extensionHandleObject.position) == OPENER ? extensionHandleObject.position + 1 : -1;
    }
    
    @Override
    public JsonObject decodeFromString(final String raw)
    {
        final JsonReader reader = Json.createReader(new StringReader(raw));
        final JsonObject jsonObject = reader.readObject();
        return jsonObject;
    }
    
    /**
     * getter for opener characters
     *
     * @param component applicable component
     *
     * @return opener characters
     */
    public char[] getOpenerCharacters(final ComponentType component)
    {
        return OPENER_CHARACTERS;
    }
    
    /**
     * setter for closer characters
     *
     * @param component applicable component
     *
     * @return closer characters
     */
    public char[] getCloserCharacters(final ComponentType component)
    {
        return CLOSER_CHARACTERS;
    }
}
