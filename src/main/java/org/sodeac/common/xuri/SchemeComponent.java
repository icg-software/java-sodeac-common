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
package org.sodeac.common.xuri;

/*
 * @see {@link https://tools.ietf.org/html/rfc3986#section-3.1}
 */

/**
 * Scheme component of URI.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class SchemeComponent extends AbstractComponent<NoSubComponent>
{
    /**
     *
     */
    private static final long serialVersionUID = 5126925115989645523L;
    
    private String value = null;
    
    /**
     * constructor for scheme component
     *
     * @param value representative scheme string
     */
    protected SchemeComponent(String value)
    {
        super(ComponentType.SCHEME);
        this.value = value;
    }
    
    /**
     * getter for representative scheme string
     *
     * @return representative scheme string
     */
    public String getValue()
    {
        return this.value;
    }
}
