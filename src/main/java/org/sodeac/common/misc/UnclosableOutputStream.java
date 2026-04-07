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
import java.io.OutputStream;

public class UnclosableOutputStream extends OutputStream
{
    private OutputStream out = null;
    
    public UnclosableOutputStream(final OutputStream out)
    {
        super();
        this.out = out;
    }
    
    @Override
    public void write(final int b) throws IOException
    {
        this.out.write(b);
    }
    
    @Override
    public void write(final byte[] b) throws IOException
    {
        this.out.write(b);
    }
    
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException
    {
        this.out.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException
    {
        this.out.flush();
    }
    
    @Override
    public void close() throws IOException { }
    
    @Override
    public String toString()
    {
        return "Unclosable " + this.out.toString();
    }
    
    public OutputStream unwrap()
    {
        return this.out;
    }
    
}
