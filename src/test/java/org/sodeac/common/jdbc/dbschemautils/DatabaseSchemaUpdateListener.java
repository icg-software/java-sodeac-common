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
package org.sodeac.common.jdbc.dbschemautils;

import org.sodeac.common.function.ExceptionCatchedConsumer;
import org.sodeac.common.jdbc.DBSchemaUtils.DBSchemaEvent;

public class DatabaseSchemaUpdateListener implements ExceptionCatchedConsumer<DBSchemaEvent>
{
    
    private IDatabaseSchemaUpdateListener intern = null;
    
    public DatabaseSchemaUpdateListener(IDatabaseSchemaUpdateListener intern)
    {
        super();
        this.intern = intern;
    }
    
    @Override
    public void acceptWithException(DBSchemaEvent t) throws Exception
    {
        intern.onAction(t.getActionType(), t.getObjectType(), t.getPhaseType(), t.getConnection(), t.getSchemaSpecificationName(), t.getObjects(), t.getDriver(), t.getException());
        
    }
    
}
