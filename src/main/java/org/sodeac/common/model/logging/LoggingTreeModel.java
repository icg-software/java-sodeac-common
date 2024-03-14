/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.model.logging;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.annotation.Domain;
import org.sodeac.common.annotation.BowMethod;
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.GenerateBowFactory;
import org.sodeac.common.annotation.Version;
import org.sodeac.common.annotation.BowMethod.ReturnBowMode;
import org.sodeac.common.annotation.BowParameter.AutomaticConsumer;

@Domain(name="sodeac.org",module="logging")
@Version(major=0,minor=6)
@GenerateBowFactory
public class LoggingTreeModel extends TypedTreeMetaModel<LoggingTreeModel> 
{
	static{ModelRegistry.getTypedTreeMetaModel(LoggingTreeModel.class);}
	
	@XmlElement(name="LogEvent")
	public static volatile BranchNodeType<LoggingTreeModel,LogEventNodeType> logEvent;
	
	
	@XmlElement(name="LogEventListChunk")
	public static volatile BranchNodeType<LoggingTreeModel,LogEventListChunkNodeType> logEventListChunk;
	
	public static RootBranchNode<LoggingTreeModel,LogEventListChunkNodeType> createLogEventListChunk(long listSize, long chunkSize, long chunkSequence, boolean last)
	{
		return createLogEventListChunk(listSize, chunkSize, chunkSequence, last, null);
	}
	
	@BowMethod(convertReturnValueToBow=true,returnBowMode=ReturnBowMode.UNDEFINED_PARENT_TYPE)
	protected static RootBranchNode<LoggingTreeModel,LogEventListChunkNodeType> createLogEventListChunk(long listSize, long chunkSize, long chunkSequence, boolean last, @BowParameter(automaticConsumerMode=AutomaticConsumer.NEW_BOW_BY_RETURNTYPE) Consumer<RootBranchNode<LoggingTreeModel,LogEventListChunkNodeType>> onRootNodeCreated)
	{
		RootBranchNode<LoggingTreeModel,LogEventListChunkNodeType> chunk = ModelRegistry.getTypedTreeMetaModel(LoggingTreeModel.class).createRootNode(LoggingTreeModel.logEventListChunk);
		
		if(onRootNodeCreated != null)
		{
			onRootNodeCreated.accept(chunk);
		}
		
		chunk
			.setValue(LogEventListChunkNodeType.listSize, listSize)
			.setValue(LogEventListChunkNodeType.chunkSequnece, chunkSequence)
			.setValue(LogEventListChunkNodeType.chunkSize, chunkSize)
			.setValue(LogEventListChunkNodeType.itemSize, 0)
			.setValue(LogEventListChunkNodeType.last, last);
		
		return chunk;
	}
}
