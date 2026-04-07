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
package org.sodeac.common.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Function;

import org.sodeac.common.IService;
import org.sodeac.common.IService.IServiceProvider;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface ServiceSatisfiedCheck
{
    Class<? extends Function<IService.IServiceProvider<?>, Boolean>> trigger() default Optional.class;
    
    public class Optional implements Function<IService.IServiceProvider<?>, Boolean>
    {
        @Override
        public Boolean apply(IServiceProvider<?> t)
        {
            return true;
        }
    }
    
    public class MatchRequired implements Function<IService.IServiceProvider<?>, Boolean>
    {
        @Override
        public Boolean apply(IServiceProvider<?> t)
        {
            return t.isMatched();
        }
    }
    
    // TODO ReferenceRequired
}
