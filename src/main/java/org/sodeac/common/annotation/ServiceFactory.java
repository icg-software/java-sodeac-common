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
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Function;

import org.sodeac.common.IService.IFactoryEnvironment;
import org.sodeac.common.misc.DefaultServiceFactory;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ServiceFactory
{
    int lowerScalingLimit() default 1;

    int upperScalingLimit() default 1;

    int initialScaling() default 0;

    boolean shared() default true;

    Class<?> requiredConfigurationClass() default NoRequiredConfiguration.class;

    Class<? extends Function<IFactoryEnvironment<?, ?>, ?>> factoryClass() default DefaultServiceFactory.class;

    ServiceRegistration[] registrations() default {};

    StringProperty[] stringProperty() default {};

    BooleanProperty[] booleanProperty() default {};

    DecimalProperty[] decimalProperty() default {};

    IntegerProperty[] integerProperty() default {};

    class NoRequiredConfiguration { }
}
