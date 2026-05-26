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
package org.sodeac.common.model.logging;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum LogEventType
{
    /**
     * classic logging for applications
     */
    SYSTEM_LOG("sdc://identifier.specs/org.sodeac.logging/logitemtype/systemlog"),
    
    /**
     * IPC - progress
     */
    PROGRESS("sdc://identifier.specs/org.sodeac.logging/logitemtype/progress"),
    
    /**
     * IPC - state
     */
    STATE("sdc://identifier.specs/org.sodeac.logging/logitemtype/state"),
    
    /**
     * business process event
     */
    BUSINESS_EVENT("sdc://identifier.specs/org.sodeac.logging/logitemtype/businessevent"),
    
    /**
     * task
     */
    TASK("sdc://identifier.specs/org.sodeac.logging/logitemtype/task");
    
    LogEventType(final String uri)
    {
        this.uri = uri;
    }
    
    private static volatile Set<LogEventType> ALL = null;
    
    private final String uri;
    
    public static Set<LogEventType> getAll()
    {
        if (LogEventType.ALL == null)
        {
            EnumSet<LogEventType> all = EnumSet.allOf(LogEventType.class);
            LogEventType.ALL = Collections.unmodifiableSet(all);
        }
        return LogEventType.ALL;
    }
    
    public static LogEventType findByURI(final String uri)
    {
        for (final LogEventType type : getAll())
        {
            if (type.uri.equals(uri))
            {
                return type;
            }
        }
        return null;
    }
    
    public static LogEventType findByName(final String name)
    {
        for (final LogEventType type : getAll())
        {
            if (type.name().equalsIgnoreCase(name))
            {
                return type;
            }
        }
        return null;
    }
}
