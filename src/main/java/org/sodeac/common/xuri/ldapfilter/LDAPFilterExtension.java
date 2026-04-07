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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.xuri.IDecodingExtensionHandler;
import org.sodeac.common.xuri.IEncodingExtensionHandler;
import org.sodeac.common.xuri.IExtension;

/**
 * XURI filter extension for ldap filter.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 */
@Component(service = IExtension.class, property = "type=" + LDAPFilterExtension.TYPE)
public class LDAPFilterExtension implements IExtension<IFilterItem>, Serializable
{
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    /**
     *
     */
    private static final long serialVersionUID = 9054192628876497162L;
    
    public static final String TYPE = "org.sodeac.xuri.ldapfilter";
    
    public LDAPFilterExtension()
    {
        super();
    }
    
    public LDAPFilterExtension(final String rawString)
    {
        super();
        this.rawString = rawString;
    }
    
    private String rawString = null;
    
    @Override
    public String getExpression()
    {
        return this.rawString;
    }
    
    @Override
    public String getType()
    {
        return TYPE;
    }
    
    public IFilterItem decodeFromString(final String expression)
    {
        return LDAPFilterDecodingHandler.getInstance().decodeFromString(expression);
    }
    
    public String encodeToString(final IFilterItem extensionDataObject)
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
    public int driverIsApplicableFor(final Map<String, Object> properties)
    {
        return IDriver.APPLICABLE_DEFAULT;
    }
    
    public static String decodeFromHexEscaped(final String rawString)
    {
        StringBuilder builder = new StringBuilder();
        
        int pendingByteSequence = 0;
        
        int codePoint = 0;
        for (int i = 0; i < rawString.length(); i++)
        {
            char c = rawString.charAt(i);
            if ((c == IFilterItem.ESCAPE) && (i + 2 < rawString.length()))
            {
                String hexCode = rawString.substring(i + 1, i + 3);
                try
                {
                    int dec = Integer.parseInt(hexCode, 16);
                    if (pendingByteSequence == 0)
                    {
                        if ((dec >= 0x00) && (dec < 0x80))
                        {
                            i += 2;
                            builder.append((char) dec);
                            continue;
                        }
                        else if ((dec >= 0x80) && (dec < 0xC0))
                        {
                            // invalid
                            builder.append(c);
                            continue;
                        }
                        else if ((dec >= 0xC0) && (dec < 0xC2))
                        {
                            // invalid
                            builder.append(c);
                            continue;
                        }
                        else if ((dec >= 0xC2) && (dec < 0xE0))
                        {
                            // 2 byte sequence
                            pendingByteSequence = 1;
                            codePoint = 0x80 + ((dec - 0xC2) * 0x40);
                            i += 2;
                            continue;
                        }
                        else if ((dec >= 0xE0) && (dec < 0xF0))
                        {
                            // 3 byte sequence
                            pendingByteSequence = 2;
                            codePoint = (dec - 0xE0) * 0x1000;
                            i += 2;
                            
                            if (dec == 0xE0)
                            {
                                // TODO check second byte is >= a0 ???
                            }
                            
                            if (dec == 0xED)
                            {
                                // TODO check second byte is < a0  ???
                            }
                            
                            continue;
                        }
                        else if ((dec >= 0xF0) && (dec < 0xF5))
                        {
                            // 4 byte sequence
                            pendingByteSequence = 3;
                            i += 2;
                            codePoint = ((dec - 0xF0) * 0x30000);
                            continue;
                        }
                        else
                        {
                            // invalid
                            builder.append(c);
                            continue;
                        }
                    }
                    else
                    {
                        
                        if ((dec < 0x80) && (dec >= 0xC0))
                        {
                            throw new IllegalStateException("Invalid escape sequence in position " + i);
                        }
                        
                        pendingByteSequence--;
                        codePoint += (dec - 0x80) * (pendingByteSequence == 0 ? 1 : (Math.pow(0x40, pendingByteSequence)));
                        
                        if (pendingByteSequence == 0)
                        {
                            builder.append(new String(Character.toChars(codePoint)));
                        }
                        
                        i += 2;
                        continue;
                    }
                }
                catch (final NumberFormatException e) { }
                
            }
            else
            {
                if (pendingByteSequence > 0)
                {
                    throw new IllegalStateException("expect escape sequence in position " + i);
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
