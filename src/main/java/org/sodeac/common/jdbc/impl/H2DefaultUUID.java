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
package org.sodeac.common.jdbc.impl;

import java.sql.Connection;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.schemax.IDefaultUUID;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Component(service = IDefaultUUID.class, property = { "defaultdriver=true", "type=h2" })
public class H2DefaultUUID implements IDefaultUUID
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    @Override
    public int driverIsApplicableFor(final Map<String, Object> properties)
    {
        try
        {
            Connection connection = (Connection) properties.get(Connection.class.getCanonicalName());
            if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2"))
            {
                return IDriver.APPLICABLE_DEFAULT;
            }
        }
        catch (final Exception e) { }
        return IDriver.APPLICABLE_NONE;
    }
    
    @Override
    public String createExpression
        (
            final BranchNode<?, ColumnNodeType> column,
            final Connection connection, final String schemaName,
            final Dictionary<String, Object> properties,
            final IDBSchemaUtilsDriver driver
        )
    {
        return driver.getFunctionExpression("RANDOM_UUID");
    }
    
    @Override
    public boolean updateRequired(final BranchNode<?, ColumnNodeType> column, final Connection connection, final String schema, final Dictionary<String, Object> properties, final IDBSchemaUtilsDriver driver, final String currentValue)
    {
        String defaultValue = createExpression(column, connection, schema, properties, driver).replace("\"", "").toUpperCase().trim();
        String currentValue2 = currentValue.replace("\"", "").trim();
        return !defaultValue.equalsIgnoreCase(currentValue2);
        
    }
}
