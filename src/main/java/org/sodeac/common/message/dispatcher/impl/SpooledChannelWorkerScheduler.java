/*******************************************************************************
 * Copyright (c) 2017, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sodeac.common.snapdeque.DequeNode;
import org.sodeac.common.snapdeque.DequeSnapshot;
import org.sodeac.common.snapdeque.SnapshotableDeque;

public class SpooledChannelWorkerScheduler extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108 * 7;
	
	protected SpooledChannelWorkerScheduler(MessageDispatcherImpl eventDispatcher)
	{
		super();
		this.eventDispatcher = eventDispatcher;
		this.scheduledChain = new SnapshotableDeque<>();
		
		super.setDaemon(true);
		super.setName(SpooledChannelWorkerScheduler.class.getSimpleName() + " " + eventDispatcher.getId());
	}
	
	private MessageDispatcherImpl eventDispatcher = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile long currentWaitTimeStamp = -1;
	
	private Object waitMonitor = new Object();
	private SnapshotableDeque<SpooledChannelWorker> scheduledChain = null;
	
	private Logger logger = LoggerFactory.getLogger(SpooledChannelWorkerScheduler.class);
	
	protected SpooledChannelWorker scheduleChannelWorker(ChannelImpl<?> channel, long wakeUpTime)
	{
		SpooledChannelWorker spooledChannelWorker = new SpooledChannelWorker(channel, wakeUpTime);
		scheduledChain.addLast(spooledChannelWorker);
		
		synchronized (this.waitMonitor)
		{
			this.isUpdateNotified = true;
			if(this.currentWaitTimeStamp > 0)
			{
				if(wakeUpTime < this.currentWaitTimeStamp)
				{
					waitMonitor.notify();
				}
			}
		}
	
		return spooledChannelWorker;
	}
	
	@Override
	public void run()
	{
		SpooledChannelWorker worker;
		long spoolCleanRun = 0;
		while(go)
		{
			long minWakeUpTimestamp = -1;
			
			long now = System.currentTimeMillis();
			
			if(spoolCleanRun < (now - DEFAULT_WAIT_TIME))
			{
				try
				{
					spoolCleanRun = now;
					eventDispatcher.checkTimeoutWorker();
				}
				catch (Exception e) 
				{
					logger.error("clean worker spooler", e);
				}
				catch (Error e) 
				{
					logger.error("clean worker spooler", e);
				}
			}
			
			
			try
			{
				DequeSnapshot<SpooledChannelWorker> snapshot = this.scheduledChain.createSnapshot();
				try
				{
					for(DequeNode<SpooledChannelWorker> workerNode : snapshot.nodeIterable())
					{
						worker = workerNode.getElement();
						if(worker == null)
						{
							workerNode.unlink();
							continue;
						}
						if(! worker.isValid())
						{
							workerNode.unlink();							
							continue;
						}
						if(now >= worker.getWakeupTime())
						{
							worker.getChannel().notifyOrCreateWorker(worker.getWakeupTime()); // LOCK CHANNEL.workerSpoolLock WORKER.waitMonitor
							workerNode.unlink();
							continue;
						}
						if(minWakeUpTimestamp < 0)
						{
							minWakeUpTimestamp = worker.getWakeupTime();
						}
						else if(minWakeUpTimestamp > worker.getWakeupTime())
						{
							minWakeUpTimestamp = worker.getWakeupTime();
						}
					}
				
				}
				finally 
				{
					snapshot.close();
				}
				
			}
			catch (Exception | Error e) 
			{
				logger.error("Exception running SpooledChannelWorkerScheduler",e);
			}
			
			try
			{
				synchronized (this.waitMonitor)
				{
					if(go)
					{
						if(isUpdateNotified)
						{
							isUpdateNotified = false;
						}
						else
						{
							long wait = DEFAULT_WAIT_TIME;
							if(minWakeUpTimestamp > 0)
							{
								wait = minWakeUpTimestamp - System.currentTimeMillis();
							}
							if(wait > DEFAULT_WAIT_TIME)
							{
								wait = DEFAULT_WAIT_TIME;
							}
							if(wait > 0)
							{
								this.currentWaitTimeStamp = wait + System.currentTimeMillis();
								waitMonitor.wait(wait);
								this.currentWaitTimeStamp = -1;
							}
						}
					}
				}
			}
			catch (InterruptedException e) {}
			catch (Exception | Error e) 
			{
				logger.error("Error running SpooledChannelWorkerScheduler",e);
			}
		}
		DequeSnapshot<SpooledChannelWorker> snapshot = this.scheduledChain.createSnapshot();
		try
		{
			for(DequeNode<SpooledChannelWorker> workerNode : snapshot.nodeIterable())
			{
				workerNode.unlink();
			}
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				logger.error("Error close snapshot",e);
			}
		}
		
	}
	public void stopScheduler()
	{
		this.go = false;
		synchronized (this.waitMonitor)
		{
			try
			{
				this.waitMonitor.notify();
			}
			catch (Exception | Error e) 
			{
				logger.error("Exception stopping Spooled Channel Worker Scheduler",e);
			}
		}
	}
	
}
