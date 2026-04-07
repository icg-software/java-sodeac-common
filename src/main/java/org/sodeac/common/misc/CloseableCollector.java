/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
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
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Collector to collect auto {@link AutoCloseable}s. Closing the collector closes all collected {@link AutoCloseable}s in inverted order. 
 * 
 * @author Sebastian Palarus
 *
 */
public class CloseableCollector implements AutoCloseable 
{
	private CloseableCollector()
	{
		super();
		this.closeableList = new LinkedList<AutoCloseable>();
		this.lock = new ReentrantLock();
	}
	
	private LinkedList<AutoCloseable> closeableList = null;
	private Lock lock = null;
	
	public <T extends AutoCloseable> T register(T closeable)
	{
		if(closeable == null)
		{
			return null;
		}
		
		try
		{
			lock.lock();
			try
			{
				this.closeableList.add(closeable);
			}
			finally 
			{
				lock.unlock();
			}
		}
		catch (Exception e) 
		{
			try
			{
				closeable.close();
			}
			catch (Exception e2) {}
			catch (Error e2) {}
			
			throw e;
		}
		
		return closeable;
	}
	
	/**
	 * returns new instance of {@link CloseableCollector}
	 * 
	 * @return new instance
	 */
	public static CloseableCollector newInstance()
	{
		return new CloseableCollector();
	}
	
	/**
	 * Close a collected closeable on demand and removes it from collection.
	 * 
	 * @param closeable to close
	 * 
	 * @return closable to close
	 */
	public <T extends AutoCloseable> T close(T closeable)
	{
		if(closeable == null)
		{
			return null;
		}
		
		try
		{
		
			LinkedList<Integer> positionsToRemove = new LinkedList<>();
			ListIterator<AutoCloseable> itr = this.closeableList.listIterator();
			while(itr.hasNext())
			{
				if(itr.next() == closeable)
				{
					itr.remove();
				}
			}
				
			positionsToRemove.clear();
		}
		finally 
		{
			try
			{
				closeable.close();	
			}
			catch (Exception e) {}
			catch (Error e) {}
		}
		
		return closeable;
	}

	@Override
	public void close() throws IOException 
	{
		try
		{
			while(! this.closeableList.isEmpty())
			{
				try
				{
					AutoCloseable closeable = this.closeableList.removeLast();
					closeable.close();	
				}
				catch (Exception e) {}
				catch (Error e) {}
			}
		}
		finally 
		{
			this.closeableList.clear();
		}

	}

}
