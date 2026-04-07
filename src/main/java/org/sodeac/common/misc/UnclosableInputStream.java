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
package org.sodeac.common.misc;

import java.io.IOException;
import java.io.InputStream;

public class UnclosableInputStream extends InputStream
{
    private InputStream in = null;
    
    public UnclosableInputStream(final InputStream in)
    {
        super();
        this.in = in;
    }
    
    @Override
    public int read() throws IOException
    {
        return this.in.read();
    }
    
    @Override
    public int read(final byte[] b) throws IOException
    {
        return this.in.read(b);
    }
    
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        return this.in.read(b, off, len);
    }
    
    @Override
    public long skip(final long n) throws IOException
    {
        return this.in.skip(n);
    }
    
    @Override
    public int available() throws IOException
    {
        return this.in.available();
    }
    
    @Override
    public void close() throws IOException { }
    
    @Override
    public synchronized void mark(final int readlimit)
    {
        this.in.mark(readlimit);
    }
    
    @Override
    public synchronized void reset() throws IOException
    {
        this.in.reset();
    }
    
    @Override
    public boolean markSupported()
    {
        return this.in.markSupported();
    }
    
    @Override
    public String toString()
    {
        return "Unclosable " + this.in.toString();
    }
    
    public InputStream unwrap()
    {
        return this.in;
    }
    
}
