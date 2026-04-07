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
package org.sodeac.common.xuri;

import java.io.Serializable;

/**
 * Represents a Uniform Resource Identifier (URI) reference.
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 *
 */
public class URI implements Serializable
{
    
    /**
     *
     */
    private static final long serialVersionUID = 1421043812832070455L;
    
    protected String uriString = null;
    
    protected SchemeComponent scheme = null;
    protected AuthorityComponent authority = null;
    protected PathComponent path = null;
    protected QueryComponent query = null;
    protected FragmentComponent fragment = null;
    
    /**
     * Constructor of URI.
     *
     * @param uriString complete URI string
     */
    public URI(final String uriString)
    {
        super();
        this.uriString = uriString;
        URIParser.getInstance().parse(this);
    }
    
    /**
     * getter for URI string
     *
     * @return complete URI string
     */
    public String getURIString()
    {
        return this.uriString;
    }
    
    @Override
    public String toString()
    {
        return this.uriString;
    }
    
    /**
     * getter for scheme
     *
     * @return scheme object
     */
    public SchemeComponent getScheme()
    {
        return this.scheme;
    }
    
    /**
     * getter for authority object
     *
     * @return authority object
     */
    public AuthorityComponent getAuthority()
    {
        return this.authority;
    }
    
    /**
     * getter for query object
     *
     * @return query object
     */
    public QueryComponent getQuery()
    {
        return this.query;
    }
    
    /**
     * getter for path object
     *
     * @return path object
     */
    public PathComponent getPath()
    {
        return this.path;
    }
    
    /**
     * getter for fragment object
     *
     * @return fragment object
     */
    public FragmentComponent getFragment()
    {
        return this.fragment;
    }
}
