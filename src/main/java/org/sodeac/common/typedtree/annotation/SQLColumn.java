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
package org.sodeac.common.typedtree.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.util.Dictionary;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.IDefaultValueExpressionDriver;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.ConvertEvent;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface SQLColumn
{
    enum SQLColumnType
    {AUTO, CHAR, VARCHAR, CLOB, UUID, BOOLEAN, SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, TIMESTAMP, DATE, TIME, BINARY, BLOB}
    
    String name();
    
    boolean nullable() default true;
    
    SQLColumnType type() default SQLColumnType.AUTO;
    
    int length() default 255;
    
    boolean readable() default true;
    
    boolean insertable() default true;
    
    boolean updatable() default true;
    
    String staticDefaultValue() default "";
    
    Class<? extends IDefaultValueExpressionDriver> defaultValueExpressionDriver() default NoDefaultValueExpressionDriver.class;
    
    Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onInsert() default NoConsumer.class;
    
    Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onUpdate() default NoConsumer.class;
    
    Class<? extends Consumer<TypedTreeJDBCCruder.ConvertEvent>> onUpsert() default NoConsumer.class;
    
    Class<? extends Function<?, ?>> nodeValue2JDBC() default NoNode2JDBC.class;
    
    Class<? extends Function<?, ?>> JDBC2NodeValue() default NoJDBC2Node.class;
    
    class NoConsumer implements Consumer<TypedTreeJDBCCruder.ConvertEvent>
    {
        @Override
        public void accept(final ConvertEvent t) { }
        
    }
    
    class NoNode2JDBC implements Function<Object, Object>
    {
        
        @Override
        public Object apply(final Object t)
        {
            return t;
        }
        
    }
    
    class NoJDBC2Node implements Function<Object, Object>
    {
        
        @Override
        public Object apply(final Object t)
        {
            return t;
        }
        
    }
    
    class NoDefaultValueExpressionDriver implements IDefaultValueExpressionDriver
    {
        
        @Override
        public int driverIsApplicableFor(final Map<String, Object> properties)
        {
            return IDriver.APPLICABLE_NONE;
        }
        
        @Override
        public String createExpression(final BranchNode<?, ColumnNodeType> column, final Connection connection, final String schema, final Dictionary<String, Object> properties, final IDBSchemaUtilsDriver driver)
        {
            return null;
        }
        
    }
    
}
