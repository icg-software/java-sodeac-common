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
package org.sodeac.common.model.dbschema;

import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.DBSchemaUtils;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass = DBSchemaTreeModel.class)
@GenerateBow(buildAlias = true)
public class EventConsumerNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(EventConsumerNodeType.class);
    }
    
    public static volatile LeafNodeType<EventConsumerNodeType, ExceptionCatchedConsumer<DBSchemaUtils.DBSchemaEvent>> eventConsumer;
}
