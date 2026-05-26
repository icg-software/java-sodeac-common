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

public enum LogLevel
{
    /**
     * tracing level is designated for very fine-grained log events, to help in comprehending a problem program flow
     */
    TRACE(1),
    
    /**
     * debug level is designated for fine-grained log events, to help in diagnosing a problem
     */
    DEBUG(2),
    
    /**
     * info level is designated for progressing events
     */
    INFO(3),
    
    /**
     * warn level is designated for events which indicates a problem
     */
    WARN(4),
    
    /**
     * error level is designated for events which indicates a lost of functionality,
     * but still allow the procedure to continue running
     */
    ERROR(5),
    
    /**
     * error level is designated for events which abort the procedure
     */
    FATAL(6);
    
    LogLevel(final int intValue)
    {
        this.intValue = intValue;
    }
    
    private static volatile Set<LogLevel> ALL = null;
    
    private final int intValue;
    
    public int getIntValue()
    {
        return this.intValue;
    }
    
    public static Set<LogLevel> getAll()
    {
        if (LogLevel.ALL == null)
        {
            EnumSet<LogLevel> all = EnumSet.allOf(LogLevel.class);
            LogLevel.ALL = Collections.unmodifiableSet(all);
        }
        return LogLevel.ALL;
    }
    
    public static LogLevel findByInteger(final int value)
    {
        for (final LogLevel logLevel : getAll())
        {
            if (logLevel.intValue == value)
            {
                return logLevel;
            }
        }
        return null;
    }
    
    public static LogLevel findByName(final String name)
    {
        for (final LogLevel logLevel : getAll())
        {
            if (logLevel.name().equalsIgnoreCase(name))
            {
                return logLevel;
            }
        }
        return null;
    }
}
