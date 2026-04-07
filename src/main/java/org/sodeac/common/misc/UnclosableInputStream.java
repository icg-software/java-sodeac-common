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
	
	public UnclosableInputStream(InputStream in)
	{
		super();
		this.in = in;
	}
	
	@Override
	public int read() throws IOException
	{
		return in.read();
	}
	@Override
	public int read(byte[] b) throws IOException
	{
		return in.read(b);
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		return in.read(b, off, len);
	}
	
	@Override
	public long skip(long n) throws IOException
	{
		return in.skip(n);
	}
	
	@Override
	public int available() throws IOException
	{
		return in.available();
	}
	
	@Override
	public void close() throws IOException{}
	
	@Override
	public synchronized void mark(int readlimit)
	{
		in.mark(readlimit);
	}
	@Override
	public synchronized void reset() throws IOException
	{
		in.reset();
	}
	@Override
	public boolean markSupported()
	{
		return in.markSupported();
	}

	@Override
	public String toString()
	{
		return "Unclosable " + in.toString();
	}
	
	public InputStream unwrap()
	{
		return in;
	}

}
