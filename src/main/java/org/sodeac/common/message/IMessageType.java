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
package org.sodeac.common.message;

public interface IMessageType
{
	public static String OID_PREFIX_SDC_MESSAGE_TYPE = "1.3.6.1.4.1.53777.1.";
	
	public interface Payload
	{
		public static String OID_CAT = "1.";
		
		public interface Configuration
		{
			
		}
		
		public interface Flow
		{
			// Windowing
			// Stop / Pause
			// Resume
			// Reset
			
			// AutoNext : Acks => next Packages, Send Max Without Ack
			
			// resend puffered message
			// acks
			
			// push stream dirty (client not acks pushed messages and the buffer is full)
			
		}
	}
	
	public interface Channel
	{
		public static String OID_CAT = "2.";
		
		// heartbeat ?
		
	}
	
	public interface Service
	{
		
	}
	
	
	public interface Notification
	{
		public interface Validation
		{
			
		}
		
		public interface Permission
		{
			// not allowd
			// token timeout
			// token not accepted
			// token request
		}
		
		public interface Progress
		{
			
		}
	}
	
	public interface Cluster
	{
		// wait to provide resources
		// schedule service shutdown
		// wait sync / repl
	}
	
	public interface Command
	{
		// skip command queue
		
		// heartbeat
		// stop / cancel
		// commit
		
		// view / monitors
		// update
		// insert 
		// remove
		// execute
		
	}
	
	public interface Permission
	{
		// granted
		// denied
		// requested
	}
	
	// metrics
	// scope
	// jta
	// locks - request for lock / release lock / request for release / 
}
