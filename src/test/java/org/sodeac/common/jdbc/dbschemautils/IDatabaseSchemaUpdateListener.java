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
package org.sodeac.common.jdbc.dbschemautils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;

import org.sodeac.common.jdbc.DBSchemaUtils.ActionType;
import org.sodeac.common.jdbc.DBSchemaUtils.ObjectType;
import org.sodeac.common.jdbc.DBSchemaUtils.PhaseType;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;

/**
 * SchemaUpdateListeners are informed about the progress of the schema update process.
 *
 * @author Sebastian Palarus
 *
 */
public interface IDatabaseSchemaUpdateListener
{
    /**
     * Listener-Methode to inform about the progress of the schema update process
     *
     * @param actionType action types describe the kind of process in schema update workflow
     * @param objectType object types describe the kind of object (table,column,key ...) in schema update workflow
     * @param phaseType  phase type describe the moment of handle database object in schema update workflow
     * @param connection used connection
     * @param domain     name of schema domain
     * @param objects    property objects (involved specification objects)
     * @param driver     user database schema driver
     * @param exception  possible catched exceptions for {@link PhaseType#POST} - events
     *
     * @throws SQLException
     */
    public void onAction(ActionType actionType, ObjectType objectType, PhaseType phaseType, Connection connection, String domain, Dictionary<ObjectType, Object> objects, IDBSchemaUtilsDriver driver, Exception exception) throws SQLException;
}
