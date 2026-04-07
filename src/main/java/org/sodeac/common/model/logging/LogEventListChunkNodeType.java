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
package org.sodeac.common.model.logging;

import org.sodeac.common.annotation.BowMethod;
import org.sodeac.common.annotation.BowMethod.ReturnBowMode;
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.model.CommonListChunkBaseBranchNodeType;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;
import org.sodeac.common.typedtree.annotation.XMLNodeList;

@TypedTreeModel(modelClass = LoggingTreeModel.class)
@GenerateBow
public class LogEventListChunkNodeType extends CommonListChunkBaseBranchNodeType
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(LogEventListChunkNodeType.class);
    }
    
    @XMLNodeList(childElementName = "LogEvent", listElement = false)
    public static volatile BranchNodeListType<LogEventListChunkNodeType, LogEventNodeType> chunk;
    
    @BowMethod(convertReturnValueToBow = true, returnBowMode = ReturnBowMode.SELF)
    public static void addLogToEventListChunk
        (
            @BowParameter(self = true) BranchNode<? extends BranchNodeMetaModel, LogEventListChunkNodeType> chunkNodeList,
            @BowParameter(convertToBow = true, name = "logEventBow") BranchNode<? extends BranchNodeMetaModel, LogEventNodeType> logEventNode
        )
    {
        Integer itemSize = chunkNodeList.getValue(LogEventListChunkNodeType.itemSize);
        if (itemSize == null)
        {
            itemSize = 0;
        }
        chunkNodeList.create(LogEventListChunkNodeType.chunk).copyFrom(logEventNode);
        chunkNodeList.setValue(LogEventListChunkNodeType.itemSize, itemSize.intValue() + 1);
    }
}
