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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum LogPropertyType
{
    /**
     * property extension for log item
     */
    PROPERTY("sdc://identifier.specs/org.sodeac.logging/logpropertytype/property"),
    
    /**
     * tag extension for log item
     */
    TAG("sdc://identifier.specs/org.sodeac.logging/logpropertytype/tag"),
    
    /**
     * stacktrace attachment for log item
     */
    STACKTRACE("sdc://identifier.specs/org.sodeac.logging/logpropertytype/stacktrace"),
    
    /**
     * exception attachment for log item
     */
    THROWABLE("sdc://identifier.specs/org.sodeac.logging/logpropertytype/throwable"),
    
    /**
     * comment for log item
     */
    COMMENT("sdc://identifier.specs/org.sodeac.logging/logpropertytype/comment"),
    
    /**
     * state of trigger, especially for business events
     */
    EVENT_TRIGGER_STATE("sdc://identifier.specs/org.sodeac.logging/logpropertytype/triggerstate"),
    
    /**
     * correlation to another log item
     */
    CORRELATION("sdc://identifier.specs/org.sodeac.logging/logpropertytype/correlation");
    
    private LogPropertyType(String uri)
    {
        this.uri = uri;
    }
    
    private static volatile Set<LogPropertyType> ALL = null;
    
    private String uri;
    
    public static Set<LogPropertyType> getAll()
    {
        if (LogPropertyType.ALL == null)
        {
            EnumSet<LogPropertyType> all = EnumSet.allOf(LogPropertyType.class);
            LogPropertyType.ALL = Collections.unmodifiableSet(all);
        }
        return LogPropertyType.ALL;
    }
    
    public static LogPropertyType findByURI(String uri)
    {
        for (LogPropertyType type : getAll())
        {
            if (type.uri.equals(uri))
            {
                return type;
            }
        }
        return null;
    }
    
    public static LogPropertyType findByName(String name)
    {
        for (LogPropertyType type : getAll())
        {
            if (type.name().equalsIgnoreCase(name))
            {
                return type;
            }
        }
        return null;
    }
}
