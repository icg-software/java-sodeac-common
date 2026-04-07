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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;

public class CommonConsumer
{
	public static final BiConsumer<InputStream, OutputStream> CopyStream = (i,o) ->
	{
		try
		{
			byte[] buffer = new byte[1080];
			int len;
			
			while((len = i.read(buffer)) > 0)
			{
				o.write(buffer, 0, len);
			}
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new RuntimeWrappedException(e);
		}
	};
	
	public static final BiConsumer<InputStream, OutputStream> CopyStreamAndClose = (i,o) ->
	{
		try
		{
			try
			{
				byte[] buffer = new byte[1080];
				int len;
				
				while((len = i.read(buffer)) > 0)
				{
					o.write(buffer, 0, len);
				}
			}
			finally
			{
				try
				{
					i.close();
				}
				catch(Exception e) {}
				catch(Error e) {}
				
				try
				{
					o.close();
				}
				catch(Exception e) {}
				catch(Error e) {}
			}
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new RuntimeWrappedException(e);
		}
	};
}
