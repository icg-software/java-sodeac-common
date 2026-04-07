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
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.schemax.IDefaultBySequence;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.model.dbschema.SequenceNodeType;
import org.sodeac.common.model.dbschema.TableNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Component(service = IDefaultBySequence.class, property = { "defaultdriver=true", "type=h2" })
public class H2DefaultBySequence implements IDefaultBySequence
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    @Override
    public int driverIsApplicableFor(Map<String, Object> properties)
    {
        try
        {
            Connection connection = (Connection) properties.get(Connection.class.getCanonicalName());
            if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2"))
            {
                return IDriver.APPLICABLE_DEFAULT;
            }
        }
        catch (Exception e) { }
        return IDriver.APPLICABLE_NONE;
    }
    
    @Override
    public String createExpression
        (
            BranchNode<?, ColumnNodeType> column,
            Connection connection, String schemaName,
            Dictionary<String, Object> properties,
            IDBSchemaUtilsDriver driver
        )
    {
        BranchNode<ColumnNodeType, SequenceNodeType> sequence = column.get(ColumnNodeType.sequence);
        Objects.requireNonNull(sequence, "sequence not defined for " + column.getValue(ColumnNodeType.name));
        Objects.requireNonNull(schemaName);
        
        BranchNode<?, TableNodeType> table = (BranchNode<?, TableNodeType>) column.getParentNode();
        BranchNode<?, DBSchemaNodeType> schema = (BranchNode<?, DBSchemaNodeType>) table.getParentNode();
        
        String sequenceName = sequence.getValue(SequenceNodeType.name);
        if ((sequenceName == null) || sequenceName.isEmpty())
        {
            sequenceName = driver.objectNameGuidelineFormat(schema, connection, "seq_" + table.getValue(TableNodeType.name) + "_" + column.getValue(ColumnNodeType.name), "SEQUENCE");
        }
        
        return " NEXT VALUE FOR " + schemaName + "." + sequenceName + " ";
    }
    
    public boolean updateRequired(BranchNode<?, ColumnNodeType> column, Connection connection, String schema, Dictionary<String, Object> properties, IDBSchemaUtilsDriver driver, String currentValue)
    {
        String defaultValue = createExpression(column, connection, schema, properties, driver).replace("\"", "").toUpperCase().trim();
        String currentValue2 = currentValue.replace("\"", "").trim();
        return !defaultValue.equalsIgnoreCase(currentValue2);
        
    }
}
