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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface BowMethod
{
	public enum ReturnBowMode {DEFAULT, SELF, UNDEFINED_PARENT_TYPE, NESTED_BOW};
	
	boolean convertReturnValueToBow() default false;
	boolean keepStatic() default false;
	boolean createBowFromReturnValue() default false;
	BowMethod.ReturnBowMode returnBowMode() default BowMethod.ReturnBowMode.DEFAULT;
	String name() default "";
}
