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


import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.sodeac.common.misc.Driver;

/*
 * 
https://tools.ietf.org/html/rfc3986

*/

public final class URIParser implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3880109488641611313L;
	
	public static final char COLON		 	= ':';
	public static final char SLASH			= '/';
	public static final char BACKSLASH		= '\\';
	public static final char QUESTION_MARK	= '?';
	public static final char FRAGMENT 		= '#';
	public static final char SINGLE_QUOTE 	= '\'';
	public static final char DOUBLE_QUOTE 	= '"';
	public static final char PERCENT_SIGN 	= '%';
	public static final char AT_SIGN	 	= '@';
	public static final char AND			= '&';
	public static final char EQUAL			= '=';
	
	private static final int SIZE_CASH_HELPER_OBJECT = 13;
	
	protected static List<IExtension<?>> getExtensionList(ComponentType componentType, URI uri)
	{
		if(!URIParser.DRIVER_UPDATE_REGISTRATION)
		{
			DRIVER_UPDATE_REGISTRATION = Driver.addUpdateListener(IExtension.class, (n,o) -> 
			{
				URIParser.CACHE_ENCODING_EXTENSION = null;
			});
		}
		List<IExtension<?>> list = CACHE_ENCODING_EXTENSION;
		if(list == null)
		{
			synchronized(URIParser.class)
			{
				list = CACHE_ENCODING_EXTENSION;
				if(list == null)
				{
					list = Collections.unmodifiableList((List)Driver.getDriverList(IExtension.class, new HashMap<String,Object>()));
					CACHE_ENCODING_EXTENSION = list;
				}
			}
		}
		return list;
	}
	
	public static void addExtension(IExtension<?> extension)
	{
		// TODO special list with user defined extensions
		
		if((extension.getType() == null) || extension.getType().isEmpty())
		{
			throw new NullPointerException("type of encoding extension is undefined");
		}
		
		synchronized(URIParser.class)
		{
			List<IExtension<?>> newList = new ArrayList<IExtension<?>>();
			for(IExtension<?> handler : getExtensionList(null, null))
			{
				if(handler.getType().equals(extension.getType()))
				{
					continue;
				}
				newList.add(handler);
			}
			newList.add(extension);
			CACHE_ENCODING_EXTENSION = Collections.unmodifiableList(newList);
		}
		
	}
	
	public static void removeExtension(IExtension<?> extension)
	{
		// TODO special list with user defined extensions
		
		if((extension.getType() == null) || extension.getType().isEmpty())
		{
			throw new NullPointerException("type of encoding extension is undefined");
		}
		
		synchronized(URIParser.class)
		{
			boolean removed = false;
			
			for(IExtension<?> handler : getExtensionList(null, null))
			{
				if(handler.getType().equals(extension.getType()))
				{
					removed = true;
					break;
				}
			}
			if(! removed)
			{
				return;
			}
			
			List<IExtension<?>> newList = new ArrayList<IExtension<?>>();
			for(IExtension<?> handler : getExtensionList(null, null))
			{
				if(handler.getType().equals(extension.getType()))
				{
					continue;
				}
				newList.add(handler);
			}
			CACHE_ENCODING_EXTENSION = Collections.unmodifiableList(newList);
		}
		
	}
	
	protected static volatile URIParser instance = null;
	protected static final LinkedList<ParserHelperContainerObject> CACHE_PARSER_HELPER_CONTAINER_OBJECT = new LinkedList<ParserHelperContainerObject>();
	protected volatile static List<IExtension<?>> CACHE_ENCODING_EXTENSION = null;
	protected volatile static boolean DRIVER_UPDATE_REGISTRATION = false;
	
	protected static URIParser getInstance()
	{
		if(instance == null)
		{
			instance = new URIParser();
		}
		return instance;
	}

	protected URI parse(URI uri)
	{
		ParserHelperContainerObject workerObject = null;
		
		synchronized (URIParser.CACHE_PARSER_HELPER_CONTAINER_OBJECT)
		{
			if(URIParser.CACHE_PARSER_HELPER_CONTAINER_OBJECT.size() > 0)
			{
				workerObject = URIParser.CACHE_PARSER_HELPER_CONTAINER_OBJECT.removeFirst();
			}
			else
			{
				workerObject = new ParserHelperContainerObject();
			}
		}
		
		try
		{
			workerObject.uri = uri;
			workerObject.fullPath = uri.getURIString();
			workerObject.maxPosition = workerObject.fullPath.length() -1;
			
			parseScheme(workerObject);
			if(workerObject.containsAuthority)
			{
				parseAuthority(workerObject);
			}
			else
			{
				workerObject.uri.authority = new AuthorityComponent();
			}
			
			if(workerObject.containsPath)
			{
				parsePath(workerObject);
			}
			else
			{
				workerObject.uri.path = new PathComponent(false);
				workerObject.uri.path.setExpression("");
			}
			if(workerObject.containsQuery)
			{
				parseQuery(workerObject);
			}
			else
			{
				workerObject.uri.query = new QueryComponent();
				workerObject.uri.query.setExpression("");
			}
			if(workerObject.containsFragment)
			{
				parseFragment(workerObject);
			}
			return uri;
		}
		finally 
		{
			if(URIParser.CACHE_PARSER_HELPER_CONTAINER_OBJECT.size() < URIParser.SIZE_CASH_HELPER_OBJECT)
			{
				workerObject.clear();
				URIParser.CACHE_PARSER_HELPER_CONTAINER_OBJECT.addLast(workerObject);
			}
		}
	}
	
	private void parseScheme(ParserHelperContainerObject workerObject )
	{
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			switch(workerObject.readNextCharactor())
			{
				case COLON:
					String scheme = decodeUrl(workerObject.mainStringBuilder.toString());
					if((workerObject.fullPath.length() > (workerObject.currentPosition + 1)) && (workerObject.fullPath.charAt(workerObject.currentPosition + 1) == SLASH) )
					{
						if((workerObject.fullPath.length() > (workerObject.currentPosition + 2)) && (workerObject.fullPath.charAt(workerObject.currentPosition + 2) == SLASH) )
						{
							workerObject.currentPosition = workerObject.currentPosition + 3;
							workerObject.containsAuthority = true;
							workerObject.pathIsRelative = false;
						}
						else
						{
							workerObject.currentPosition = workerObject.currentPosition + 2;
							workerObject.containsAuthority = false;
							workerObject.pathIsRelative = true;
						}
						workerObject.uri.scheme = new SchemeComponent(scheme);
						return;
					}
					
					workerObject.containsAuthority = false;
					workerObject.currentPosition = workerObject.currentPosition + 1;
					workerObject.pathIsRelative = true;
					workerObject.uri.scheme = new SchemeComponent(scheme);
					return ; 
				default :
					workerObject.appendCurrentCharacter();
			}
		}
		throw new FormatException("scheme not found: " + workerObject.fullPath);
	}
	
	private static void parseAuthority(ParserHelperContainerObject workerObject )
	{
		workerObject.uri.authority = new AuthorityComponent();
		workerObject.uri.authority.setExpression("");
		
		if(workerObject.currentPosition > workerObject.maxPosition )
		{
			return ;
		}
		
		List<IExtension<?>> registeredExtensionList = getExtensionList(ComponentType.AUTHORITY, workerObject.uri);
				
		workerObject.clearStringBuilder();
		workerObject.inIPv6Mode = false;
		workerObject.pathIsRelative = true;
		workerObject.containsQuery = false;
		workerObject.containsFragment = false;
		workerObject.authoritySubComponent = null;
		workerObject.containsExtension = false;
		workerObject.prefixdelimiter = SLASH;
		workerObject.backup1CurrentPosition = workerObject.currentPosition;
		workerObject.backup2CurrentPosition = workerObject.backup1CurrentPosition;
		
		if(workerObject.extensionHandleObject == null)
		{
			workerObject.extensionHandleObject = new ExtensionHandleObject();
		}
		workerObject.extensionHandleObject.fullPath = workerObject.fullPath;
		workerObject.extensionHandleObject.rawResult = workerObject.mainStringBuilder;
		workerObject.extensionHandleObject.uri = workerObject.uri;
		workerObject.extensionHandleObject.extension = null;
		workerObject.extensionHandleObject.component = ComponentType.AUTHORITY;
		
		fullpathloop :
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			
			for(IExtension<?> extension : registeredExtensionList)
			{
				if(extension.getDecoder() == null)
				{
					continue;
				}
				
				workerObject.extensionHandleObject.position = workerObject.currentPosition;
				workerObject.extensionBegin = extension.getDecoder().openerCharactersMatched(workerObject.extensionHandleObject);
				if(workerObject.extensionBegin > -1)
				{
					if(workerObject.authoritySubComponent == null)
					{
						workerObject.authoritySubComponent = new AuthoritySubComponent(null,decodeUrl(workerObject.mainStringBuilder.toString()));
						
						workerObject.clearStringBuilder();
						workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
					}
					else
					{
						workerObject.clearStringBuilder();
					}
					
					workerObject.extensionHandleObject.extension = null;
					workerObject.extensionHandleObject.position = workerObject.extensionBegin;
					workerObject.extensionEnd = extension.getDecoder().parseRawExtensionString(workerObject.extensionHandleObject);
					workerObject.currentPosition = workerObject.extensionEnd;
					
					workerObject.containsExtension = true;
					
					if(workerObject.extensionHandleObject.extension != null)
					{
						workerObject.authoritySubComponent.addExtension(workerObject.extensionHandleObject.extension);
					}
					
					if(workerObject.currentPosition > workerObject.maxPosition )
					{
						break fullpathloop;
					}
					
					workerObject.currentPosition--;
					continue fullpathloop;
				}
				
			}
			
			switch(workerObject.currentCharacter)
			{
				case AT_SIGN: // https://tools.ietf.org/html/rfc3986#section-3.2.1		
					if( workerObject.containsExtension)
					{
						workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
					}
					workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
					workerObject.clearStringBuilder();
					workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
					workerObject.authoritySubComponent.setPostfixDelimiter(workerObject.currentCharacter);
					workerObject.authoritySubComponent = null;
					workerObject.prefixdelimiter = '@';
					workerObject.containsExtension = false;
					workerObject.backup2CurrentPosition = workerObject.currentPosition + 1;
						
					break;
				case COLON:
					if(workerObject.inIPv6Mode)
					{
						workerObject.appendCurrentCharacter();
					}
					else
					{
						if( workerObject.containsExtension)
						{
							workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
						}
						else
						{
							workerObject.expression = workerObject.mainStringBuilder.toString();
							workerObject.value = decodeUrl(workerObject.expression);
							workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
						}
						workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
						workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
						workerObject.authoritySubComponent.setPostfixDelimiter(workerObject.currentCharacter);
						workerObject.clearStringBuilder();
						workerObject.authoritySubComponent = null;
						workerObject.prefixdelimiter = ':';
						workerObject.containsExtension = false;
						workerObject.backup2CurrentPosition = workerObject.currentPosition + 1;
					}
					break;
				case SLASH:
					if( workerObject.containsExtension)
					{
						workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
					}
					workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
					workerObject.uri.authority.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
					workerObject.authoritySubComponent.setPostfixDelimiter(workerObject.currentCharacter);
					workerObject.pathIsRelative = false;
					workerObject.currentPosition++;
					workerObject.authoritySubComponent = null;
					
					return;
				case QUESTION_MARK:
					if( workerObject.containsExtension)
					{
						workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
					}
					workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
					workerObject.uri.authority.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
					workerObject.authoritySubComponent.setPostfixDelimiter(workerObject.currentCharacter);
					workerObject.containsQuery = true;
					workerObject.currentPosition++;
					workerObject.authoritySubComponent = null;
					return;
				case '[': // IPv6 - first char in subcomponent - https://tools.ietf.org/html/rfc3986#section-3.2.2
					if( workerObject.containsExtension)
					{
						break;
					}
					
					if(workerObject.mainStringBuilder.length() == 0)
					{
						workerObject.inIPv6Mode = true;
						workerObject.appendCurrentCharacter();
					}
					else
					{
						throw new FormatException("vorbidden character found in authority: " + workerObject.currentCharacter + " | " + workerObject.fullPath);
					}
					break;
				case ']': // IPv6 - last char in subcomponent -https://tools.ietf.org/html/rfc3986#section-3.2.2
					if( workerObject.containsExtension)
					{
						break;
					}
					
					if
					(
						(workerObject.currentPosition == workerObject.maxPosition ) ||
						(
							(workerObject.fullPath.charAt(workerObject.currentPosition + 1) == '/') ||
							(workerObject.fullPath.charAt(workerObject.currentPosition + 1) == '?') ||
							(workerObject.fullPath.charAt(workerObject.currentPosition + 1) == ':') ||
							(workerObject.fullPath.charAt(workerObject.currentPosition + 1) == '#') 
						)
					)
					{
						workerObject.appendCurrentCharacter();
						workerObject.inIPv6Mode = false;
					}
					else
					{
						throw new FormatException("vorbidden character found in authority: " + workerObject.currentCharacter + " | " + workerObject.fullPath);
					}
					break;
				case FRAGMENT:
					if( workerObject.containsExtension)
					{
						workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
					}
					workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
					workerObject.uri.authority.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
					workerObject.authoritySubComponent.setPostfixDelimiter(workerObject.currentCharacter);
					workerObject.containsFragment = true;
					workerObject.currentPosition++;
					workerObject.authoritySubComponent = null;
					return;
				default :
					if(! workerObject.containsExtension)
					{
						workerObject.appendCurrentCharacter();
					}
			}
		}
		if( workerObject.containsExtension)
		{
			workerObject.authoritySubComponent.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
		}
		else
		{
			workerObject.expression = workerObject.mainStringBuilder.toString();
			workerObject.value = decodeUrl(workerObject.expression);
			workerObject.authoritySubComponent = new AuthoritySubComponent(workerObject.expression,workerObject.value);
		}
		workerObject.uri.authority.addSubComponent(workerObject.authoritySubComponent);
		workerObject.uri.authority.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
		workerObject.authoritySubComponent.setPrefixDelimiter(workerObject.prefixdelimiter);
		workerObject.authoritySubComponent.setPostfixDelimiter(SLASH);
		workerObject.currentPosition++;
		workerObject.authoritySubComponent = null;
	}
	
	private static void parsePath(ParserHelperContainerObject workerObject )
	{
		workerObject.uri.path = new PathComponent(! workerObject.pathIsRelative);
		workerObject.uri.path.setExpression("");
		
		if(workerObject.currentPosition > workerObject.maxPosition )
		{
			if(!workerObject.pathIsRelative)
			{
				workerObject.uri.path.setExpression("/");
			}
			return ;
		}
		
		List<IExtension<?>> registeredExtensionList = getExtensionList(ComponentType.PATH, workerObject.uri);
				
		workerObject.clearStringBuilder();
		workerObject.containsQuery = false;
		workerObject.containsFragment = false;
		workerObject.pathSegment = null;
		workerObject.containsExtension = false;
		workerObject.backup1CurrentPosition = workerObject.currentPosition;
		workerObject.backup2CurrentPosition = workerObject.backup1CurrentPosition;
		
		if(workerObject.extensionHandleObject == null)
		{
			workerObject.extensionHandleObject = new ExtensionHandleObject();
		}
		workerObject.extensionHandleObject.fullPath = workerObject.fullPath;
		workerObject.extensionHandleObject.rawResult = workerObject.mainStringBuilder;
		workerObject.extensionHandleObject.uri = workerObject.uri;
		workerObject.extensionHandleObject.extension = null;
		workerObject.extensionHandleObject.component = ComponentType.PATH;
		
		fullpathloop :
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			
			for(IExtension<?> extension : registeredExtensionList)
			{
				if(extension.getDecoder() == null)
				{
					continue;
				}
				
				workerObject.extensionHandleObject.position = workerObject.currentPosition;
				workerObject.extensionBegin = extension.getDecoder().openerCharactersMatched(workerObject.extensionHandleObject);
				if(workerObject.extensionBegin > -1)
				{
					if(workerObject.pathSegment == null)
					{
						workerObject.pathSegment = new PathSegment(workerObject.uri.path.getLast(),null,decodeUrl(workerObject.mainStringBuilder.toString()));
						
						workerObject.clearStringBuilder();
					}
					else
					{
						workerObject.clearStringBuilder();
					}
					
					workerObject.extensionHandleObject.extension = null;
					workerObject.extensionHandleObject.position = workerObject.extensionBegin;
					workerObject.extensionEnd = extension.getDecoder().parseRawExtensionString(workerObject.extensionHandleObject);
					workerObject.currentPosition = workerObject.extensionEnd;
					
					workerObject.containsExtension = true;
					
					if(workerObject.extensionHandleObject.extension != null)
					{
						workerObject.pathSegment.addExtension(workerObject.extensionHandleObject.extension);
					}
					
					if(workerObject.currentPosition > workerObject.maxPosition )
					{
						break fullpathloop;
					}
					
					workerObject.currentPosition--;
					continue fullpathloop;
				}
			}
			
			switch(workerObject.currentCharacter)
			{
				case SLASH:
					if( workerObject.containsExtension)
					{
						workerObject.pathSegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.pathSegment = new PathSegment(workerObject.uri.path.getLast(),workerObject.expression,workerObject.value);
					}
					workerObject.uri.path.addSubComponent(workerObject.pathSegment);
					workerObject.clearStringBuilder();
					workerObject.pathSegment = null;
					workerObject.containsExtension = false;
					workerObject.backup2CurrentPosition = workerObject.currentPosition + 1;
					
					break;
				case QUESTION_MARK:
					if( workerObject.containsExtension)
					{
						workerObject.pathSegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.pathSegment = new PathSegment(workerObject.uri.path.getLast(),workerObject.expression,workerObject.value);
					}
					workerObject.uri.path.addSubComponent(workerObject.pathSegment);
					workerObject.uri.path.setExpression((workerObject.pathIsRelative ? "" : "/") + workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.containsQuery = true;
					workerObject.currentPosition++;
					workerObject.pathSegment = null;
					return;
			
				case FRAGMENT:
					if( workerObject.containsExtension)
					{
						workerObject.pathSegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.mainStringBuilder.toString();
						workerObject.value = decodeUrl(workerObject.expression);
						workerObject.pathSegment = new PathSegment(workerObject.uri.path.getLast(),workerObject.expression,workerObject.value);
					}
					workerObject.uri.path.addSubComponent(workerObject.pathSegment);
					workerObject.uri.path.setExpression((workerObject.pathIsRelative ? "" : "/") +workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.containsFragment = true;
					workerObject.currentPosition++;
					workerObject.pathSegment = null;
					return;
				default :
					if(! workerObject.containsExtension)
					{
						workerObject.appendCurrentCharacter();
					}
				
			}
		}
		if( workerObject.containsExtension)
		{
			workerObject.pathSegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
		}
		else
		{
			workerObject.expression = workerObject.mainStringBuilder.toString();
			workerObject.value = decodeUrl(workerObject.expression);
			workerObject.pathSegment = new PathSegment(workerObject.uri.path.getLast(),workerObject.expression,workerObject.value);
		}
		workerObject.uri.path.addSubComponent(workerObject.pathSegment);
		workerObject.uri.path.setExpression((workerObject.pathIsRelative ? "" : "/") + workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
		workerObject.currentPosition++;
		workerObject.pathSegment = null;
	}
	
	private static void parseQuery(ParserHelperContainerObject workerObject )
	{
		workerObject.uri.query = new QueryComponent();
		workerObject.uri.query.setExpression("");
		
		List<IExtension<?>> registeredExtensionList = getExtensionList(ComponentType.QUERY, workerObject.uri);
				
		workerObject.clearStringBuilder();
		workerObject.containsFragment = false;
		workerObject.querySegment = null;
		workerObject.containsExtension = false;
		workerObject.backup1CurrentPosition = workerObject.currentPosition;
		workerObject.backup2CurrentPosition = workerObject.backup1CurrentPosition;
		
		if(workerObject.extensionHandleObject == null)
		{
			workerObject.extensionHandleObject = new ExtensionHandleObject();
		}
		workerObject.extensionHandleObject.fullPath = workerObject.fullPath;
		workerObject.extensionHandleObject.rawResult = workerObject.mainStringBuilder;
		workerObject.extensionHandleObject.uri = workerObject.uri;
		workerObject.extensionHandleObject.extension = null;
		workerObject.extensionHandleObject.component = ComponentType.QUERY;
		
		fullpathloop :
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			
			for(IExtension<?> extension : registeredExtensionList)
			{
				if(extension.getDecoder() == null)
				{
					continue;
				}
				
				workerObject.extensionHandleObject.position = workerObject.currentPosition;
				workerObject.extensionBegin = extension.getDecoder().openerCharactersMatched(workerObject.extensionHandleObject);
				if(workerObject.extensionBegin > -1)
				{
					if(workerObject.querySegment  == null)
					{
						workerObject.querySegment = new QuerySegment(workerObject.expression,workerObject.qtype,workerObject.qname,workerObject.qformat,workerObject.qvalue);
						
						workerObject.clearStringBuilder();
					}
					else
					{
						workerObject.clearStringBuilder();
					}
					
					workerObject.extensionHandleObject.extension = null;
					workerObject.extensionHandleObject.position = workerObject.extensionBegin;
					workerObject.extensionEnd = extension.getDecoder().parseRawExtensionString(workerObject.extensionHandleObject);
					workerObject.currentPosition = workerObject.extensionEnd;
					
					workerObject.containsExtension = true;
					
					if(workerObject.extensionHandleObject.extension != null)
					{
						workerObject.querySegment.addExtension(workerObject.extensionHandleObject.extension);
					}
					
					if(workerObject.currentPosition > workerObject.maxPosition )
					{
						break fullpathloop;
					}
					
					workerObject.currentPosition--;
					continue fullpathloop;
				}
			}
			
			switch(workerObject.currentCharacter)
			{
				case COLON:
					if( workerObject.containsExtension)
					{
						break;
					}
					
					if(! workerObject.qtypeParsed)
					{
						workerObject.qtype = decodeUrl(workerObject.mainStringBuilder.toString());
						workerObject.qtypeParsed = true;
						workerObject.clearStringBuilder();
						break;
					}
					if(workerObject.qnameParsed && (! workerObject.qformatParsed))
					{
						workerObject.qformat = decodeUrl(workerObject.mainStringBuilder.toString());
						workerObject.qformatParsed = true;
						workerObject.clearStringBuilder();
						
						if(workerObject.qformat.equals("string"))
						{
							workerObject.currentPosition++;
							handleString(workerObject);
						}
						if(workerObject.qformat.equals("json"))
						{
							workerObject.currentPosition++;
							handleJSON(workerObject);
						}
						break;
					}
					workerObject.appendCurrentCharacter();
					break;
				case EQUAL:
					if( workerObject.containsExtension)
					{
						break;
					}
					if(! workerObject.qnameParsed)
					{
						workerObject.qname = decodeUrl(workerObject.mainStringBuilder.toString());
						workerObject.qtypeParsed = true;
						workerObject.qnameParsed = true;
						workerObject.clearStringBuilder();
						break;
					}
					workerObject.appendCurrentCharacter();
					break;
				case AND:
					
					if( workerObject.containsExtension)
					{
						workerObject.querySegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition);
						if(workerObject.qtypeParsed && (! workerObject.qnameParsed))
						{
							workerObject.qformat = workerObject.qtype;
							workerObject.qtype = null;
						}
						workerObject.qvalue = decodeUrl(workerObject.mainStringBuilder.toString());
						
						workerObject.querySegment = new QuerySegment(workerObject.expression,workerObject.qtype,workerObject.qname,workerObject.qformat,workerObject.qvalue);
					}
					workerObject.uri.query.addSubComponent(workerObject.querySegment);
					workerObject.clearStringBuilder();
					workerObject.querySegment = null;
					workerObject.containsExtension = false;
					workerObject.resetQueryValue();
					workerObject.backup2CurrentPosition = workerObject.currentPosition + 1;
					
					break;
				case FRAGMENT:
					if( workerObject.containsExtension)
					{
						workerObject.querySegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
					}
					else
					{
						workerObject.expression = workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition);
						if(workerObject.qtypeParsed && (! workerObject.qnameParsed))
						{
							workerObject.qformat = workerObject.qtype;
							workerObject.qtype = null;
						}
						workerObject.qvalue = decodeUrl(workerObject.mainStringBuilder.toString());
						workerObject.querySegment = new QuerySegment(workerObject.expression,workerObject.qtype,workerObject.qname,workerObject.qformat,workerObject.qvalue);
					}
					workerObject.uri.query.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
					workerObject.uri.query.addSubComponent(workerObject.querySegment);
					workerObject.clearStringBuilder();
					workerObject.querySegment = null;
					workerObject.containsExtension = false;
					workerObject.resetQueryValue();
					workerObject.containsFragment = true;
					workerObject.currentPosition++;
					return;
				default :
					if(! workerObject.containsExtension)
					{
						workerObject.appendCurrentCharacter();
					}
				
			}
		}
		if( workerObject.containsExtension)
		{
			workerObject.querySegment.setExpression(workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition));
		}
		else
		{
			workerObject.expression = workerObject.fullPath.substring(workerObject.backup2CurrentPosition, workerObject.currentPosition);
			if(workerObject.qtypeParsed && (! workerObject.qnameParsed))
			{
				workerObject.qformat = workerObject.qtype;
				workerObject.qtype = null;
			}
			workerObject.qvalue = decodeUrl(workerObject.mainStringBuilder.toString());
			workerObject.querySegment = new QuerySegment(workerObject.expression,workerObject.qtype,workerObject.qname,workerObject.qformat,workerObject.qvalue);
		}
		workerObject.uri.query.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
		workerObject.uri.query.addSubComponent(workerObject.querySegment);
		workerObject.clearStringBuilder();
		workerObject.querySegment = null;
		workerObject.containsExtension = false;
		workerObject.resetQueryValue();
		workerObject.currentPosition++;
	}
	
	private static void parseFragment(ParserHelperContainerObject workerObject )
	{		
		List<IExtension<?>> registeredExtensionList = getExtensionList(ComponentType.FRAGMENT, workerObject.uri);
				
		workerObject.clearStringBuilder();
		workerObject.containsExtension = false;
		workerObject.backup1CurrentPosition = workerObject.currentPosition;
		workerObject.backup2CurrentPosition = workerObject.backup1CurrentPosition;
		
		if(workerObject.extensionHandleObject == null)
		{
			workerObject.extensionHandleObject = new ExtensionHandleObject();
		}
		workerObject.extensionHandleObject.fullPath = workerObject.fullPath;
		workerObject.extensionHandleObject.rawResult = workerObject.mainStringBuilder;
		workerObject.extensionHandleObject.uri = workerObject.uri;
		workerObject.extensionHandleObject.extension = null;
		workerObject.extensionHandleObject.component = ComponentType.FRAGMENT;
		
		fullpathloop :
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			
			for(IExtension<?> extension : registeredExtensionList)
			{
				if(extension.getDecoder() == null)
				{
					continue;
				}
				
				workerObject.extensionHandleObject.position = workerObject.currentPosition;
				workerObject.extensionBegin = extension.getDecoder().openerCharactersMatched(workerObject.extensionHandleObject);
				if(workerObject.extensionBegin > -1)
				{
					if(workerObject.uri.fragment == null)
					{
						workerObject.uri.fragment = new FragmentComponent(decodeUrl(workerObject.mainStringBuilder.toString()));
						workerObject.uri.fragment.setExpression("");
					}
					workerObject.clearStringBuilder();
					
					workerObject.clearStringBuilder();
					workerObject.extensionHandleObject.extension = null;
					workerObject.extensionHandleObject.position = workerObject.extensionBegin;
					workerObject.extensionEnd = extension.getDecoder().parseRawExtensionString(workerObject.extensionHandleObject);
					workerObject.currentPosition = workerObject.extensionEnd;
					
					workerObject.containsExtension = true;
					
					if(workerObject.extensionHandleObject.extension != null)
					{
						workerObject.uri.fragment.addExtension(workerObject.extensionHandleObject.extension);
					}
					
					if(workerObject.currentPosition > workerObject.maxPosition )
					{
						break fullpathloop;
					}
					
					workerObject.currentPosition--;
					continue fullpathloop;
				}
			}
			
			if(! workerObject.containsExtension)
			{
				workerObject.appendCurrentCharacter();
			}
		}
		
		if(workerObject.uri.fragment == null)
		{
			workerObject.uri.fragment = new FragmentComponent(decodeUrl(workerObject.mainStringBuilder.toString()));
		}
		workerObject.uri.fragment.setExpression(workerObject.fullPath.substring(workerObject.backup1CurrentPosition, workerObject.currentPosition));
		workerObject.clearStringBuilder();
		workerObject.containsExtension = false;
		workerObject.resetQueryValue();
		workerObject.currentPosition++;
	}

    @SuppressWarnings("deprecation")
	private static String decodeUrl(String raw) 
    {
    	if (raw.indexOf(PERCENT_SIGN) < 0)
    	{
    		return raw;
    	}
    	return URLDecoder.decode(raw);
    }
    
    private static void handleString(ParserHelperContainerObject workerObject)
    {
    	workerObject.quoteChar = SINGLE_QUOTE;
    	workerObject.escapeChar = BACKSLASH;
    	workerObject.inEscape = false;
    	
    	if(workerObject.readNextCharactor() == workerObject.quoteChar)
    	{
    		workerObject.currentPosition++;
    	}
    	else
    	{
    		throw new RuntimeException("encoded string parameter must start with \' | " + workerObject.currentCharacter + " : " + workerObject.fullPath);
    	}
    	
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			
			if(workerObject.inEscape)
			{
				workerObject.inEscape = false;
				if(workerObject.currentCharacter != workerObject.quoteChar)
				{
					workerObject.mainStringBuilder.append(workerObject.escapeChar);
				}
				workerObject.appendCurrentCharacter();
				continue;
			}
			
			if(workerObject.currentCharacter == workerObject.escapeChar)
			{
				workerObject.inEscape  = true;
				continue;
			}
			
			if(workerObject.currentCharacter == workerObject.quoteChar)
			{
				workerObject.clearParser();
				return;
			}
			
			workerObject.appendCurrentCharacter();
		}
		
		workerObject.clearParser();
		throw new FormatException("no closing sequence \"" + workerObject.escapeChar + "\" found in string parameter " + " : " + workerObject.fullPath);
    }
    
    private static void handleJSON(ParserHelperContainerObject workerObject)
    {
    	workerObject.quoteChar = DOUBLE_QUOTE;
    	workerObject.escapeChar = BACKSLASH;
    	workerObject.inEscape = false;
    	workerObject.nestedLevel = 0;
    	
    	if(workerObject.readNextCharactor() == '{')
    	{
    		workerObject.currentPosition++;
    		workerObject.nestedLevel++;
    		workerObject.appendCurrentCharacter();
    	}
    	else
    	{
    		throw new RuntimeException("encoded json parameter must start with { | " + workerObject.currentCharacter + " : " + workerObject.fullPath);
    	}
    	
		for(; workerObject.currentPosition <= workerObject.maxPosition; workerObject.currentPosition++)
		{
			workerObject.readNextCharactor();
			workerObject.appendCurrentCharacter();
			
			if(workerObject.currentCharacter == workerObject.quoteChar)
			{
				if(workerObject.inQuote)
				{
					if(workerObject.inEscape)
					{
						workerObject.inEscape = false;
					}
					else
					{
						workerObject.inQuote = false;
					}
				}
				else
				{
					workerObject.inQuote = true;
				}
				continue;
			}
			
			if(workerObject.currentCharacter == workerObject.escapeChar)
			{
				if(workerObject.inQuote)
				{
					workerObject.inEscape = ! workerObject.inEscape;
				}
				continue;
			}
			
			if(workerObject.inQuote)
			{
				continue;
			}
			
			if(workerObject.readNextCharactor() == '{')
	    	{
				workerObject.nestedLevel++;
	    	}
			
			if(workerObject.readNextCharactor() == '}')
	    	{
				workerObject.nestedLevel--;
				
				if(workerObject.nestedLevel == 0)
				{
					workerObject.clearParser();
					return;
				}
	    	}
		}
		workerObject.clearParser();
		throw new FormatException("no closing sequence } found in json parameter " + " : " + workerObject.fullPath);
    }
	
	private class ParserHelperContainerObject
	{
		private char currentCharacter = '.';
		private String fullPath;
		
		private boolean containsExtension = true;
		private boolean pathIsRelative = false;
		private boolean containsAuthority = true;
		private boolean containsPath = true;
		private boolean containsQuery = false;
		private boolean containsFragment = false;
		private char prefixdelimiter = ':';
		private int currentPosition = 0;
		private int backup1CurrentPosition = 0;
		private int backup2CurrentPosition = 0;
		private int maxPosition = -1;
		private int extensionBegin = -1;
		private int extensionEnd = -1;
		private String value;
		private String expression;
		private StringBuilder mainStringBuilder = new StringBuilder();
		private URI uri = null;
		private ExtensionHandleObject extensionHandleObject;
		
		private char readNextCharactor()
		{
			this.currentCharacter = fullPath.charAt(currentPosition);
			return this.currentCharacter;
		}
		
		// authority
		private boolean inIPv6Mode = false;
		private AuthoritySubComponent authoritySubComponent = null;
		
		// path
		private PathSegment pathSegment = null;
		
		// query 
		private QuerySegment querySegment = null;
		private String qtype = null;
		private String qname = null;
		private String qformat = null;
		private String qvalue = null;
		
		private boolean qtypeParsed = false;
		private boolean qnameParsed = false;
		private boolean qformatParsed = false;
		
		private void resetQueryValue()
		{
			qtype = null;
			qname = null;
			qformat = null;
			qvalue = null;
			qtypeParsed = false;
			qnameParsed = false;
			qformatParsed = false;
		}
		
		// parser
		private char quoteChar = SINGLE_QUOTE;
		private char escapeChar = BACKSLASH;
		private boolean inEscape = false;
		
		private boolean inQuote = false;
		private int nestedLevel = 0;
		
		private void clearParser()
		{
			quoteChar = SINGLE_QUOTE;
			escapeChar = BACKSLASH;
			inEscape = false;
			
			inQuote = false;
			nestedLevel = 0;
		}
		
		
		private void appendCurrentCharacter()
		{
			this.mainStringBuilder.append(currentCharacter);
		}
		
		private void clearStringBuilder()
		{
			if(mainStringBuilder.length() > 0)
			{
				mainStringBuilder.delete(0, mainStringBuilder.length() );
			}
		}
		
		private void clear()
		{
			this.currentCharacter = '.';
			this.fullPath = null;
			this.containsExtension = true;
			this.pathIsRelative = false;
			this.containsAuthority = true;
			this.containsPath = true;
			this.containsQuery = false;
			this.containsFragment = false;
			this.prefixdelimiter = ':';
			this.currentPosition = 0;
			this.backup1CurrentPosition = 0;
			this.backup2CurrentPosition = 0;
			this.maxPosition = -1;
			this.extensionBegin = -1;
			this.extensionEnd = -1;
			this.value = null;
			this.expression = null;
			this.mainStringBuilder.setLength(0);
			this.uri = null;
			this.extensionHandleObject = null;
			this.inIPv6Mode = false;
			this.authoritySubComponent = null;
			this.pathSegment = null;
			this.querySegment = null;
			this.qtype = null;
			this.qname = null;
			this.qformat = null;
			this.qvalue = null;
			this.qtypeParsed = false;
			this.qnameParsed = false;
			this.qformatParsed = false;
			this.quoteChar = SINGLE_QUOTE;
			this.escapeChar = BACKSLASH;
			this.inEscape = false;
			this.inQuote = false;
			this.nestedLevel = 0;
		}
	}
}
