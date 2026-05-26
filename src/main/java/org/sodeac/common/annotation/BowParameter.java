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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface BowParameter
{
    enum AutomaticConsumer
    {NONE, NEW_BOW_BY_RETURNTYPE}
    
    boolean self() default false;
    
    boolean convertToBow() default false;
    
    BowParameter.AutomaticConsumer automaticConsumerMode() default AutomaticConsumer.NONE;
    
    String name() default "";
}
