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
package org.sodeac.common.typedtree.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface SQLReferencedByColumn 
{
	String name();
	boolean nullable() default true;
	SQLColumnType type() default SQLColumnType.AUTO;
	int length() default -1;
	boolean readable() default true;
	boolean insertable() default true;
	boolean updatable() default true;
}
