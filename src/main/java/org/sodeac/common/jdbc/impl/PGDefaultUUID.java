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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.schemax.IDefaultUUID;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.misc.RuntimeWrappedException;
import org.sodeac.common.misc.Driver.IDriver;
import org.sodeac.common.model.dbschema.ColumnNodeType;
import org.sodeac.common.typedtree.BranchNode;

@Component(service=IDefaultUUID.class,property= {"defaultdriver=true","type=postgresql"})
public class PGDefaultUUID implements IDefaultUUID
{
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile OSGiDriverRegistry internalBootstrapDep;
	
	@Override
	public int driverIsApplicableFor(Map<String, Object> properties)
	{
		try
		{
			Connection connection = (Connection)properties.get(Connection.class.getCanonicalName());
			if(connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL"))
			{
				return IDriver.APPLICABLE_DEFAULT;
			}
		}
		catch (Exception e) {}
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
		try
		{
			PreparedStatement preparedStatement = connection.prepareStatement("create extension if not exists \"uuid-ossp\"");
			try
			{
				preparedStatement.executeUpdate();
			}
			finally 
			{
				preparedStatement.close();
			}
		}
		catch (SQLException e) 
		{
			throw new RuntimeWrappedException(e);
		}
		return "public.uuid_generate_v4()";
	}
}
