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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sodeac.common.IService;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(value=ServiceRegistrations.class)
public @interface ServiceRegistration
{
	String name() default IService.REPLACED_BY_CLASS_NAME;
	String domain() default IService.REPLACED_BY_PACKAGE_NAME;
	Version version() default @Version(major = -1, minor = -1, service= -1);
	Class<?>[] serviceType();
}
