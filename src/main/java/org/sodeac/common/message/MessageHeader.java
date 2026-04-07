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
package org.sodeac.common.message;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Message Header with common properties for message driven communication.
 * 
 * @author Sebastian Palarus
 *
 */
public class MessageHeader implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2987891836819988594L;
	
	public static final String MESSAGE_HEADER_MESSAGE_ID 			= "SDC_MH_MESSAGE_ID"			;
	public static final String MESSAGE_HEADER_CORRELATION_ID 		= "SDC_MH_CORRELATION_ID"		;
	public static final String MESSAGE_HEADER_PRIORITY 				= "SDC_MH_PRIORITY"				;
	public static final String MESSAGE_HEADER_GUARANTEED_DELIVERY 	= "SDC_MH_GUARANTEED_DELIVERY"	;
	public static final String MESSAGE_HEADER_TIMESTAMP 			= "SDC_MH_TIMESTAMP"			;
	public static final String MESSAGE_HEADER_EXPIRATION 			= "SDC_MH_EXPIRATION"			;
	public static final String MESSAGE_HEADER_CONNECTION 			= "SDC_MH_CONNECTION"			;
	public static final String MESSAGE_HEADER_SESSION	 			= "SDC_MH_SESSION"				;
	public static final String MESSAGE_HEADER_WORKFLOW	 			= "SDC_MH_WORKFLOW"				;
	public static final String MESSAGE_HEADER_TOPIC 				= "SDC_MH_TOPIC"				;
	public static final String MESSAGE_HEADER_QUEUE 				= "SDC_MH_QUEUE"				;
	public static final String MESSAGE_HEADER_MESSAGE_TYPE 			= "SDC_MH_MESSAGE_TYPE"			;
	public static final String MESSAGE_HEADER_MESSAGE_FORMAT 		= "SDC_MH_MESSAGE_FORMAT"		;
	public static final String MESSAGE_HEADER_SERVICE		 		= "SDC_MH_SERVICE"				;
	public static final String MESSAGE_HEADER_DOMAIN 				= "SDC_MH_DOMAIN"				;
	public static final String MESSAGE_HEADER_BOUNDED_CONTEXT 		= "SDC_MH_BOUNDED_CONTEXT"		;
	public static final String MESSAGE_HEADER_DESTINATION 			= "SDC_MH_DESTINATION"			;
	public static final String MESSAGE_HEADER_USER		 			= "SDC_MH_USER"					;
	public static final String MESSAGE_HEADER_SOURCE 				= "SDC_MH_SOURCE"				;
	public static final String MESSAGE_HEADER_REPLY_TO 				= "SDC_MH_REPLY_TO"				;
	public static final String MESSAGE_HEADER_DELIVERY_TIME 		= "SDC_MH_REPLY_DELIVERY_TIME"	;
	public static final String MESSAGE_HEADER_REDELIVERED 			= "SDC_MH_REPLY_REDELIVERED"	;
	public static final String MESSAGE_HEADER_SEQUENCE 				= "SDC_MH_SEQUENCE"				;
	public static final String MESSAGE_HEADER_POSITION 				= "SDC_MH_POSITION"				;
	public static final String MESSAGE_HEADER_SIZE 					= "SDC_MH_SIZE"					;
	public static final String MESSAGE_HEADER_END 					= "SDC_MH_END"					;
	public static final String MESSAGE_HEADER_PROPERTIES 			= "SDC_MH_PROPERTIES"			;
	
	/**
	 * Factory to create new Message Header.
	 * 
	 * @return
	 */
	public static MessageHeader newInstance()
	{
		return new MessageHeader().generateMessageID();
	}
	
	private MessageHeader()
	{
		super();
	}
	
	private volatile UUID messageID = null;
	private volatile boolean messageIDLocked = false;
	
	private volatile UUID correlationID = null;
	private volatile boolean correlationIDLocked = false;
	
	private volatile Integer priority = null;
	private volatile boolean priorityLocked = false;
	
	private volatile Boolean guaranteedDelivery = null;
	private volatile boolean guaranteedDeliveryLocked = false;
	
	private volatile Long timestamp = null;
	private volatile boolean timestampLocked = false;
	
	private volatile Long expiration = null;
	private volatile boolean expirationLocked = false;
	
	private volatile UUID connection = null;
	private volatile boolean connectionLocked = false;
	
	private volatile UUID session = null;
	private volatile boolean sessionLocked = false;
	
	private volatile UUID workflow = null;
	private volatile boolean workflowLocked = false;
	
	private volatile String topic = null;
	private volatile boolean topicLocked = false;
	
	private volatile String queue = null;
	private volatile boolean queueLocked = false;
	
	private volatile String messageType = null;
	private volatile boolean messageTypeLocked = false;
	
	private volatile String messageFormat = null;
	private volatile boolean messageFormatLocked = false;
	
	private volatile String service =  null;
	private volatile boolean serviceLocked = false;
	
	private volatile String domain =  null;
	private volatile boolean domainLocked = false;
	
	private volatile String boundedContext = null;
	private volatile boolean boundedContextLocked = false;
	
	private volatile String accessToken = null; // TODO // grant token for permissionSet ??  
	private volatile String idToken = null; // TODO
	private volatile String refreshToken = null; // TODO
	
	
	private volatile String destination = null;
	private volatile boolean destinationLocked = false;
	
	private volatile String user = null;
	private volatile boolean userLocked = false;
	
	private volatile String source = null;
	private volatile boolean sourceLocked = false;
	
	private volatile String replyTo = null;
	private volatile boolean replyToLocked = false;
	
	private volatile Long deliveryTime = null;
	private volatile boolean deliveryTimeLocked = false;
	
	private volatile Boolean redelivered = null;
	private volatile boolean redeliveredLocked = false;
	
	private volatile Long sequence = null;
	private volatile boolean sequenceLocked = false;
	
	private volatile Long position = null;
	private volatile boolean positionLocked = false;
	
	private volatile Long size = null;
	private volatile boolean sizeLocked = false;
	
	private volatile Boolean end = null;
	private volatile boolean endLocked = false;
	
	private volatile Map<String,Object> properties;
	private volatile boolean propertiesLocked = false;
	
	/**
	 * Locks complete message header. After this call the header is immutable.
	 * 
	 * @return message header
	 */
	public MessageHeader lockAllHeader()
	{
		this.messageIDLocked = true;
		this.correlationIDLocked = true;
		this.priorityLocked = true;
		this.guaranteedDeliveryLocked = true;
		this.timestampLocked = true;
		this.expirationLocked = true;
		this.connectionLocked = true;
		this.sessionLocked = true;
		this.workflowLocked = true;
		this.topicLocked = true;
		this.queueLocked = true;
		this.messageTypeLocked = true;
		this.messageFormatLocked = true;
		this.serviceLocked = true;
		this.domainLocked = true;
		this.boundedContextLocked = true;
		this.destinationLocked = true;
		this.userLocked = true;
		this.sourceLocked = true;
		this.replyToLocked = true;
		this.deliveryTimeLocked = true;
		this.redeliveredLocked = true;
		this.sequenceLocked = true;
		this.positionLocked = true;
		this.sizeLocked = true;
		this.endLocked = true;
		this.propertiesLocked = true;
		
		return this;
	}
	
	/**
	 * Locks single property of message header. After this call the property is immutable.
	 * 
	 * @param messageHeader affected property
	 * @return message header
	 */
	public MessageHeader lockHeader(String messageHeader)
	{
		switch (messageHeader) 
		{
			case MESSAGE_HEADER_MESSAGE_ID:
			
				this.messageIDLocked = true;
				break;
			
			case MESSAGE_HEADER_CORRELATION_ID:
				
				this.correlationIDLocked = true;
				break;
			
			case MESSAGE_HEADER_PRIORITY:
				
				this.priorityLocked = true;
				break;

			case MESSAGE_HEADER_GUARANTEED_DELIVERY:
				
				this.guaranteedDeliveryLocked = true;
				break;
			
			case MESSAGE_HEADER_TIMESTAMP:
				
				this.timestampLocked = true;
				break;
				
			case MESSAGE_HEADER_EXPIRATION:
				
				this.expirationLocked = true;
				break;
				
			case MESSAGE_HEADER_CONNECTION:
				
				this.connectionLocked = true;
				break;
				
			case MESSAGE_HEADER_SESSION:
				
				this.sessionLocked = true;
				break;
				
			case MESSAGE_HEADER_WORKFLOW:
				
				this.workflowLocked = true;
				break;
			
			case MESSAGE_HEADER_TOPIC:
				
				this.topicLocked = true;
				break;
				
			case MESSAGE_HEADER_QUEUE:
				
				this.queueLocked = true;
				break;
		
			case MESSAGE_HEADER_MESSAGE_TYPE:
				
				this.messageTypeLocked = true;
				break;
			
			case MESSAGE_HEADER_MESSAGE_FORMAT:
				
				this.messageFormatLocked = true;
				break;
				
			case MESSAGE_HEADER_SERVICE:
				
				this.serviceLocked = true;
				break;
				
			case MESSAGE_HEADER_DOMAIN:
				
				this.domainLocked = true;
				break;
				
			case MESSAGE_HEADER_BOUNDED_CONTEXT:
				
				this.boundedContextLocked = true;
				break;
				
			case MESSAGE_HEADER_DESTINATION:
				
				this.destinationLocked = true;
				break;
				
			case MESSAGE_HEADER_USER:
				
				this.userLocked = true;
				break;
				
			case MESSAGE_HEADER_SOURCE:
				
				this.sourceLocked = true;
				break;
				
			case MESSAGE_HEADER_REPLY_TO:
				
				this.replyToLocked = true;
				break;
				
			case MESSAGE_HEADER_DELIVERY_TIME:
				
				this.deliveryTimeLocked = true;
				break;
				
			case MESSAGE_HEADER_REDELIVERED:
				
				this.redeliveredLocked = true;
				break;
				
			case MESSAGE_HEADER_SEQUENCE:
				
				this.sequenceLocked = true;
				break;
				
			case MESSAGE_HEADER_POSITION:
				
				this.positionLocked = true;
				break;
				
			case MESSAGE_HEADER_SIZE:
				
				this.sizeLocked = true;
				break;
				
			case MESSAGE_HEADER_END:
				
				this.endLocked = true;
				break;
				
			case MESSAGE_HEADER_PROPERTIES:
				
				this.propertiesLocked = true;
				break;
				
			default:
				break;
		}
		
		return this;
	}

	/**
	 * getter for message id
	 * 
	 * @return message id
	 */
	public UUID getMessageID()
	{
		return messageID;
	}
	
	/**
	 * Autogenerates message id.
	 * 
	 * @return message header
	 */
	public MessageHeader generateMessageID()
	{
		if(! this.messageIDLocked)
		{
			this.messageID = UUID.randomUUID();
		}
		return this;
	}

	/**
	 * getter for correlation id
	 * 
	 * @return correlation id
	 */
	public UUID getCorrelationID()
	{
		return correlationID;
	}

	/**
	 * setter for correlation id
	 * @param correlationID correlation id to set
	 * @return message headers
	 */
	public MessageHeader setCorrelationID(UUID correlationID)
	{
		if(! this.correlationIDLocked)
		{
			this.correlationID = correlationID;
		}
		return this;
	}

	/**
	 * getter for message priority
	 * 
	 * @return message header
	 */
	public Integer getPriority()
	{
		return priority;
	}

	/**
	 * setter for message priority 
	 * @param priority priority to set
	 * @return message header
	 */
	public MessageHeader setPriority(Integer priority)
	{
		if(! this.priorityLocked)
		{
			this.priority = priority;
		}
		return this;
	}

	/**
	 * getter for guaranteed delivery property
	 * @return guaranteed delivery property
	 */
	public Boolean getGuaranteedDelivery()
	{
		return guaranteedDelivery;
	}

	/**
	 * setter for guaranteed delivery property
	 * 
	 * @param guaranteedDelivery guaranteed delivery property to set
	 * @return message header
	 */
	public MessageHeader setGuaranteedDelivery(Boolean guaranteedDelivery)
	{
		if(! this.guaranteedDeliveryLocked)
		{
			this.guaranteedDelivery = guaranteedDelivery;
		}
		return this;
	}

	/**
	 * getter for timestamp
	 * 
	 * @return timestamp
	 */
	public Long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * setter for timestamp
	 * 
	 * @param timestamp timestamp to set
	 * @return message header
	 */
	public MessageHeader setTimestamp(Long timestamp)
	{
		if(! this.timestampLocked)
		{
			this.timestamp = timestamp;
		}
		return this;
	}

	/**
	 * getter for expiration
	 * 
	 * @return expiration
	 */
	public Long getExpiration()
	{
		return expiration;
	}

	/**
	 * setter for expiration
	 * 
	 * @param expiration expiration to set
	 * @return message header
	 */
	public MessageHeader setExpiration(Long expiration)
	{
		if(! this.expirationLocked)
		{
			this.expiration = expiration;
		}
		return this;
	}

	/**
	 * getter for connection id
	 * 
	 * @return connection id
	 */
	public UUID getConnection()
	{
		return connection;
	}

	/**
	 * setter for connection id
	 * 
	 * @param connection connection id to set
	 * @return message header
	 */
	public MessageHeader setConnection(UUID connection)
	{
		if(! this.connectionLocked)
		{
			this.connection = connection;
		}
		return this;
	}
	
	/**
	 * getter for session id
	 * 
	 * @return session id
	 */
	public UUID getSession()
	{
		return session;
	}

	/**
	 * setter for session id
	 * 
	 * @param session session id to set
	 * @return message header
	 */
	public MessageHeader setSession(UUID session)
	{
		if(! this.serviceLocked)
		{
			this.session = session;
		}
		return this;
	}

	/**
	 * getter for workflow id
	 * 
	 * @return workflow id
	 */
	public UUID getWorkflow()
	{
		return workflow;
	}

	/**
	 * setter for workflow id
	 * 
	 * @param workflow workflow id to set
	 * @return
	 */
	public MessageHeader setWorkflow(UUID workflow)
	{
		if(! workflowLocked)
		{
			this.workflow = workflow;
		}
		return this;
	}

	/**
	 * getter for topic
	 * 
	 * @return topic
	 */
	public String getTopic()
	{
		return topic;
	}

	/**
	 * setter for topic
	 * 
	 * @param topic topic to set
	 * @return message header
	 */
	public MessageHeader setTopic(String topic)
	{
		if(! this.topicLocked)
		{
			this.topic = topic;
		}
		return this;
	}

	/**
	 * getter for queue
	 * 
	 * @return queue
	 */
	public String getQueue()
	{
		return queue;
	}

	/**
	 * setter for queue
	 * 
	 * @param queue queue to set
	 * @return message header
	 */
	public MessageHeader setQueue(String queue)
	{
		if(! this.queueLocked)
		{
			this.queue = queue;
		}
		return this;
	}

	/**
	 * getter for message type
	 * 
	 * @return message type
	 */
	public String getMessageType()
	{
		return messageType;
	}

	/**
	 * setter for message type
	 * 
	 * @param messageType message type to set
	 * @return message header
	 */
	public MessageHeader setMessageType(String messageType)
	{
		if(! this.messageTypeLocked)
		{
			this.messageType = messageType;
		}
		return this;
	}

	/**
	 * getter for message format
	 * 
	 * @return message format
	 */
	public String getMessageFormat()
	{
		return messageFormat;
	}

	/**
	 * setter for message format
	 * 
	 * @param messageFormat message format to set
	 * @return message header
	 */
	public MessageHeader setMessageFormat(String messageFormat)
	{
		if(! this.messageFormatLocked)
		{
			this.messageFormat = messageFormat;
		}
		return this;
	}

	/**
	 * getter for service
	 * 
	 * @return service
	 */
	public String getService()
	{
		return service;
	}

	/**
	 * setter for service
	 * 
	 * @param service service to set
	 * @return message header
	 */
	public MessageHeader setService(String service)
	{
		if(! serviceLocked)
		{
			this.service = service;
		}
		return this;
	}

	/**
	 * getter for domain
	 * 
	 * @return domain
	 */
	public String getDomain()
	{
		return domain;
	}

	/**
	 * setter for domain
	 * 
	 * @param domain domain to set
	 * @return message header
	 */
	public MessageHeader setDomain(String domain)
	{
		if(! this.domainLocked)
		{
			this.domain = domain;
		}
		return this;
	}

	/**
	 * setter for bounded context
	 * 
	 * @return bounded context
	 */
	public String getBoundedContext()
	{
		return boundedContext;
	}

	/**
	 * setter for bounded context
	 * 
	 * @param boundedContext bounded context to set
	 * @return message header
	 */
	public MessageHeader setBoundedContext(String boundedContext)
	{
		if(! this.boundedContextLocked)
		{
			this.boundedContext = boundedContext;
		}
		return this;
	}

	/**
	 * getter for destination
	 * 
	 * @return destination
	 */
	public String getDestination()
	{
		return destination;
	}

	/**
	 * setter for destination
	 * 
	 * @param destination destination to set
	 * @return message header
	 */
	public MessageHeader setDestination(String destination)
	{
		if(! this.destinationLocked)
		{
			this.destination = destination;
		}
		return this;
	}

	/**
	 * getter for user
	 * 
	 * @return user
	 */
	public String getUser()
	{
		return user;
	}

	/**
	 * setter for user
	 * 
	 * @param user user to set
	 * @return message parameter
	 */
	public MessageHeader setUser(String user)
	{
		if(! this.userLocked)
		{
			this.user = user;
		}
		return this;
	}

	/**
	 * getter for source
	 * 
	 * @return source
	 */
	public String getSource()
	{
		return source;
	}

	/**
	 * setter for source
	 * 
	 * @param source source to set
	 * @return message header
	 */
	public MessageHeader setSource(String source)
	{
		if(! this.sourceLocked)
		{
			this.source = source;
		}
		return this;
	}

	/**
	 * getter for reply to property
	 * 
	 * @return reply to property
	 */
	public String getReplyTo()
	{
		return replyTo;
	}

	/**
	 * setter for reply to property
	 * 
	 * @param replyTo reply to property to set
	 * @return message header
	 */
	public MessageHeader setReplyTo(String replyTo)
	{
		if(! this.replyToLocked)
		{
			this.replyTo = replyTo;
		}
		return this;
	}

	/**
	 * getter for delivery time
	 * 
	 * @return delivery time
	 */
	public Long getDeliveryTime()
	{
		return deliveryTime;
	}

	/**
	 * setter for delivery time
	 * 
	 * @param deliveryTime delivery time to set
	 * @return message header
	 */
	public MessageHeader setDeliveryTime(Long deliveryTime)
	{
		if(! this.deliveryTimeLocked)
		{
			this.deliveryTime = deliveryTime;
		}
		return this;
	}

	/**
	 * getter for redelivered property
	 * 
	 * @return redelivered property
	 */
	public Boolean getRedelivered()
	{
		return redelivered;
	}

	/**
	 * setter for redelivered property
	 * 
	 * @param redelivered redelivered property to set
	 * @return message header
	 */
	public MessageHeader setRedelivered(Boolean redelivered)
	{
		if(! this.redeliveredLocked)
		{
			this.redelivered = redelivered;
		}
		return this;
	}

	/**
	 * getter for message sequence
	 * 
	 * @return message sequence
	 */
	public Long getSequence()
	{
		return sequence;
	}

	/**
	 * setter for message sequence
	 * 
	 * @param sequence message sequence to set
	 * @return message header
	 */
	public MessageHeader setSequence(Long sequence)
	{
		if(! this.sequenceLocked)
		{
			this.sequence = sequence;
		}
		return this;
	}

	/**
	 * getter for message position
	 * 
	 * @return message position
	 */
	public Long getPosition()
	{
		return position;
	}

	/**
	 * setter for message position
	 * 
	 * @param position message position to set
	 * @return message header
	 */
	public MessageHeader setPosition(Long position)
	{
		if(! this.positionLocked)
		{
			this.position = position;
		}
		return this;
	}

	/**
	 * getter for message size (complete message)
	 * 
	 * @return message size
	 */
	public Long getSize()
	{
		return size;
	}

	/**
	 * setter for message size (complete message)
	 * 
	 * @param size complete message size to set
	 * @return message header
	 */
	public MessageHeader setSize(Long size)
	{
		if(! this.sizeLocked)
		{
			this.size = size;
		}
		return this;
	}

	/**
	 * setter for end flag
	 * 
	 * @return end flag
	 */
	public Boolean getEnd()
	{
		return end;
	}

	/**
	 * setter for end flag
	 * 
	 * @param end end flag to set
	 * @return message header
	 */
	public MessageHeader setEnd(Boolean end)
	{
		if(! this.endLocked)
		{
			this.end = end;
		}
		return this;
	}
	
	/**
	 * getter for message property keyset
	 * 
	 * @return  message property keyset
	 */
	public Set<String> getPropertyKeySet()
	{
		return  this.properties == null  ? Collections.emptySet() : this.properties.keySet();
	}
	
	/**
	 * adds a message property
	 * 
	 * @param key key for property
	 * @param value property value
	 * @return message header
	 */
	public MessageHeader addProperty(String key, Object value)
	{
		if( this.propertiesLocked)
		{
			return this;
		}
		
		if(this.properties == null)
		{
			this.properties = new HashMap<String,Object>();
		}
		
		this.properties.put(key, value);
		
		return this;
	}
	
	/**
	 * removes a message property
	 * @param key key of message property to remove
	 * @return message header
	 */
	public MessageHeader removeProperty(String key)
	{
		if( this.propertiesLocked)
		{
			return this;
		}
		
		if(this.properties == null)
		{
			return this;
		}
		
		this.properties.remove(key);
		
		return this;
	}
	
	/**
	 * clears all message properties
	 * 
	 * @return message header
	 */
	public MessageHeader clearProperties()
	{
		if( this.propertiesLocked)
		{
			return this;
		}
		
		if(this.properties == null)
		{
			return this;
		}
		
		this.properties.clear();
		
		return this;
	}
	
	/**
	 * returns the value of message property specified by key
	 * 
	 * @param key key of property is to be returned
	 * @return property specified by key
	 */
	public Object getPropertyValue(String key)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		return this.properties.get(key);
	}
	
	/**
	 * returns the typed value of message property specified by key
	 * 
	 * @param key key of property is to be returned
	 * @param type type of property is to be returned
	 * @return typed property specified by key
	 */
	@SuppressWarnings("unchecked")
	public <T> T getPropertyValue(String key, Class<T> type)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		return (T)this.properties.get(key);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((boundedContext == null) ? 0 : boundedContext.hashCode());
		result = prime * result + (boundedContextLocked ? 1231 : 1237);
		result = prime * result + ((connection == null) ? 0 : connection.hashCode());
		result = prime * result + (connectionLocked ? 1231 : 1237);
		result = prime * result + ((correlationID == null) ? 0 : correlationID.hashCode());
		result = prime * result + (correlationIDLocked ? 1231 : 1237);
		result = prime * result + ((deliveryTime == null) ? 0 : deliveryTime.hashCode());
		result = prime * result + (deliveryTimeLocked ? 1231 : 1237);
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + (destinationLocked ? 1231 : 1237);
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + (domainLocked ? 1231 : 1237);
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + (endLocked ? 1231 : 1237);
		result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
		result = prime * result + (expirationLocked ? 1231 : 1237);
		result = prime * result + ((guaranteedDelivery == null) ? 0 : guaranteedDelivery.hashCode());
		result = prime * result + (guaranteedDeliveryLocked ? 1231 : 1237);
		result = prime * result + ((messageFormat == null) ? 0 : messageFormat.hashCode());
		result = prime * result + (messageFormatLocked ? 1231 : 1237);
		result = prime * result + ((messageID == null) ? 0 : messageID.hashCode());
		result = prime * result + (messageIDLocked ? 1231 : 1237);
		result = prime * result + ((messageType == null) ? 0 : messageType.hashCode());
		result = prime * result + (messageTypeLocked ? 1231 : 1237);
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result + (positionLocked ? 1231 : 1237);
		result = prime * result + ((priority == null) ? 0 : priority.hashCode());
		result = prime * result + (priorityLocked ? 1231 : 1237);
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + (propertiesLocked ? 1231 : 1237);
		result = prime * result + ((queue == null) ? 0 : queue.hashCode());
		result = prime * result + (queueLocked ? 1231 : 1237);
		result = prime * result + ((redelivered == null) ? 0 : redelivered.hashCode());
		result = prime * result + (redeliveredLocked ? 1231 : 1237);
		result = prime * result + ((replyTo == null) ? 0 : replyTo.hashCode());
		result = prime * result + (replyToLocked ? 1231 : 1237);
		result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
		result = prime * result + (sequenceLocked ? 1231 : 1237);
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + (serviceLocked ? 1231 : 1237);
		result = prime * result + ((session == null) ? 0 : session.hashCode());
		result = prime * result + (sessionLocked ? 1231 : 1237);
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result + (sizeLocked ? 1231 : 1237);
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + (sourceLocked ? 1231 : 1237);
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + (timestampLocked ? 1231 : 1237);
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		result = prime * result + (topicLocked ? 1231 : 1237);
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		result = prime * result + (userLocked ? 1231 : 1237);
		result = prime * result + ((workflow == null) ? 0 : workflow.hashCode());
		result = prime * result + (workflowLocked ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageHeader other = (MessageHeader) obj;
		if (boundedContext == null)
		{
			if (other.boundedContext != null)
				return false;
		} else if (!boundedContext.equals(other.boundedContext))
			return false;
		if (boundedContextLocked != other.boundedContextLocked)
			return false;
		if (connection == null)
		{
			if (other.connection != null)
				return false;
		} else if (!connection.equals(other.connection))
			return false;
		if (connectionLocked != other.connectionLocked)
			return false;
		if (correlationID == null)
		{
			if (other.correlationID != null)
				return false;
		} else if (!correlationID.equals(other.correlationID))
			return false;
		if (correlationIDLocked != other.correlationIDLocked)
			return false;
		if (deliveryTime == null)
		{
			if (other.deliveryTime != null)
				return false;
		} else if (!deliveryTime.equals(other.deliveryTime))
			return false;
		if (deliveryTimeLocked != other.deliveryTimeLocked)
			return false;
		if (destination == null)
		{
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (destinationLocked != other.destinationLocked)
			return false;
		if (domain == null)
		{
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (domainLocked != other.domainLocked)
			return false;
		if (end == null)
		{
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (endLocked != other.endLocked)
			return false;
		if (expiration == null)
		{
			if (other.expiration != null)
				return false;
		} else if (!expiration.equals(other.expiration))
			return false;
		if (expirationLocked != other.expirationLocked)
			return false;
		if (guaranteedDelivery == null)
		{
			if (other.guaranteedDelivery != null)
				return false;
		} else if (!guaranteedDelivery.equals(other.guaranteedDelivery))
			return false;
		if (guaranteedDeliveryLocked != other.guaranteedDeliveryLocked)
			return false;
		if (messageFormat == null)
		{
			if (other.messageFormat != null)
				return false;
		} else if (!messageFormat.equals(other.messageFormat))
			return false;
		if (messageFormatLocked != other.messageFormatLocked)
			return false;
		if (messageID == null)
		{
			if (other.messageID != null)
				return false;
		} else if (!messageID.equals(other.messageID))
			return false;
		if (messageIDLocked != other.messageIDLocked)
			return false;
		if (messageType == null)
		{
			if (other.messageType != null)
				return false;
		} else if (!messageType.equals(other.messageType))
			return false;
		if (messageTypeLocked != other.messageTypeLocked)
			return false;
		if (position == null)
		{
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (positionLocked != other.positionLocked)
			return false;
		if (priority == null)
		{
			if (other.priority != null)
				return false;
		} else if (!priority.equals(other.priority))
			return false;
		if (priorityLocked != other.priorityLocked)
			return false;
		if (properties == null)
		{
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (propertiesLocked != other.propertiesLocked)
			return false;
		if (queue == null)
		{
			if (other.queue != null)
				return false;
		} else if (!queue.equals(other.queue))
			return false;
		if (queueLocked != other.queueLocked)
			return false;
		if (redelivered == null)
		{
			if (other.redelivered != null)
				return false;
		} else if (!redelivered.equals(other.redelivered))
			return false;
		if (redeliveredLocked != other.redeliveredLocked)
			return false;
		if (replyTo == null)
		{
			if (other.replyTo != null)
				return false;
		} else if (!replyTo.equals(other.replyTo))
			return false;
		if (replyToLocked != other.replyToLocked)
			return false;
		if (sequence == null)
		{
			if (other.sequence != null)
				return false;
		} else if (!sequence.equals(other.sequence))
			return false;
		if (sequenceLocked != other.sequenceLocked)
			return false;
		if (service == null)
		{
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		if (serviceLocked != other.serviceLocked)
			return false;
		if (session == null)
		{
			if (other.session != null)
				return false;
		} else if (!session.equals(other.session))
			return false;
		if (sessionLocked != other.sessionLocked)
			return false;
		if (size == null)
		{
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (sizeLocked != other.sizeLocked)
			return false;
		if (source == null)
		{
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (sourceLocked != other.sourceLocked)
			return false;
		if (timestamp == null)
		{
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (timestampLocked != other.timestampLocked)
			return false;
		if (topic == null)
		{
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		if (topicLocked != other.topicLocked)
			return false;
		if (user == null)
		{
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		if (userLocked != other.userLocked)
			return false;
		if (workflow == null)
		{
			if (other.workflow != null)
				return false;
		} else if (!workflow.equals(other.workflow))
			return false;
		if (workflowLocked != other.workflowLocked)
			return false;
		return true;
	}
	
	public void dispose()
	{
		if(this.properties != null)
		{
			this.properties.clear();
		}
		
		this.messageID = null;
		this.correlationID = null;
		this.priority = null;
		this.guaranteedDelivery = null;
		this.timestamp = null;
		this.expiration = null;
		this.connection = null;
		this.session = null;
		this.workflow = null;
		this.topic = null;
		this.queue = null;
		this.messageType = null;
		this.messageFormat = null;
		this.service =  null;
		this.domain =  null;
		this.boundedContext = null;
		this.destination = null;
		this.user = null;
		this.source = null;
		this.replyTo = null;
		this.deliveryTime = null;
		this.redelivered = null;
		this.sequence = null;
		this.position = null;
		this.size = null;
		this.end = null;
		this.properties = null;
	}
	
	public static void copyLocks(MessageHeader messageHeader, MessageHeader template)
	{
		messageHeader.messageIDLocked = template.messageIDLocked;
		messageHeader.correlationIDLocked = template.correlationIDLocked;
		messageHeader.priorityLocked = template.priorityLocked;
		messageHeader.guaranteedDeliveryLocked = template.guaranteedDeliveryLocked;
		messageHeader.timestampLocked = template.timestampLocked;
		messageHeader.expirationLocked = template.expirationLocked;
		messageHeader.connectionLocked = template.connectionLocked;
		messageHeader.sessionLocked = template.sessionLocked;
		messageHeader.workflowLocked = template.workflowLocked;
		messageHeader.topicLocked = template.topicLocked;
		messageHeader.queueLocked = template.queueLocked;
		messageHeader.messageTypeLocked = template.messageTypeLocked;
		messageHeader.messageFormatLocked = template.messageFormatLocked;
		messageHeader.serviceLocked = template.serviceLocked;
		messageHeader.domainLocked = template.domainLocked;
		messageHeader.boundedContextLocked = template.boundedContextLocked;
		messageHeader.destinationLocked = template.destinationLocked;
		messageHeader.userLocked = template.userLocked;
		messageHeader.sourceLocked = template.sourceLocked;
		messageHeader.replyToLocked = template.replyToLocked;
		messageHeader.deliveryTimeLocked = template.deliveryTimeLocked;
		messageHeader.redeliveredLocked = template.redeliveredLocked;
		messageHeader.sequenceLocked = template.sequenceLocked;
		messageHeader.positionLocked = template.positionLocked;
		messageHeader.sizeLocked = template.sizeLocked;
		messageHeader.endLocked = template.endLocked;
		messageHeader.propertiesLocked = template.propertiesLocked;
	}
	public static MessageHeader createFrom(MessageHeader template, boolean copyLocks)
	{
		MessageHeader mh = MessageHeader.newInstance();
		
		mh.correlationID = template.correlationID;
		mh.priority = template.priority;
		mh.guaranteedDelivery = template.guaranteedDelivery;
		mh.timestamp = template.timestamp;
		mh.expiration = template.expiration;
		mh.connection = template.connection;
		mh.session = template.session;
		mh.workflow = template.workflow;
		mh.topic = template.topic;
		mh.queue = template.queue;
		mh.messageType = template.messageType;
		mh.messageFormat = template.messageFormat;
		mh.service =  template.service;
		mh.domain =  template.domain;
		mh.boundedContext = template.boundedContext;
		mh.destination = template.destination;
		mh.user = template.user;
		mh.source = template.source;
		mh.replyTo = template.replyTo;
		mh.deliveryTime = template.deliveryTime;
		mh.redelivered = template.redelivered;
		mh.sequence = template.sequence;
		mh.position = template.position;
		mh.size = template.size;
		mh.end = template.end;
		if(template.properties != null)
		{
			template.properties = new HashMap<>(template.properties);
		}
		
		if(copyLocks)
		{
			mh.messageIDLocked = template.messageIDLocked;
			mh.correlationIDLocked = template.correlationIDLocked;
			mh.priorityLocked = template.priorityLocked;
			mh.guaranteedDeliveryLocked = template.guaranteedDeliveryLocked;
			mh.timestampLocked = template.timestampLocked;
			mh.expirationLocked = template.expirationLocked;
			mh.connectionLocked = template.connectionLocked;
			mh.sessionLocked = template.sessionLocked;
			mh.workflowLocked = template.workflowLocked;
			mh.topicLocked = template.topicLocked;
			mh.queueLocked = template.queueLocked;
			mh.messageTypeLocked = template.messageTypeLocked;
			mh.messageFormatLocked = template.messageFormatLocked;
			mh.serviceLocked = template.serviceLocked;
			mh.domainLocked = template.domainLocked;
			mh.boundedContextLocked = template.boundedContextLocked;
			mh.destinationLocked = template.destinationLocked;
			mh.userLocked = template.userLocked;
			mh.sourceLocked = template.sourceLocked;
			mh.replyToLocked = template.replyToLocked;
			mh.deliveryTimeLocked = template.deliveryTimeLocked;
			mh.redeliveredLocked = template.redeliveredLocked;
			mh.sequenceLocked = template.sequenceLocked;
			mh.positionLocked = template.positionLocked;
			mh.sizeLocked = template.sizeLocked;
			mh.endLocked = template.endLocked;
			mh.propertiesLocked = template.propertiesLocked;
		}
		
		return mh;
	}
}
