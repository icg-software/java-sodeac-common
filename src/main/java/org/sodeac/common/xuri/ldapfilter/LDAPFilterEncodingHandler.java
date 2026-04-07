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
package org.sodeac.common.xuri.ldapfilter;

import java.io.Serializable;

import org.sodeac.common.xuri.IEncodingExtensionHandler;

/**
 * XURI encoding extension handler to encode ldap filter items of type {@link IFilterItem}
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class LDAPFilterEncodingHandler implements IEncodingExtensionHandler<IFilterItem>, Serializable
{
    
    /**
     *
     */
    private static final long serialVersionUID = -3779708657170015377L;
    
    private transient static volatile LDAPFilterEncodingHandler INSTANCE = null;
    
    public static LDAPFilterEncodingHandler getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new LDAPFilterEncodingHandler();
        }
        return INSTANCE;
    }
    
    @Override
    public String getType()
    {
        return LDAPFilterExtension.TYPE;
    }
    
    @Override
    public String encodeToString(IFilterItem extensionDataObject)
    {
        return extensionDataObject.toString();
    }
}
