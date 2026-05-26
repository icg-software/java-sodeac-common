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
package org.sodeac.common.jdbc.classicmodelcars;

import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLPrimaryKey;
import org.sodeac.common.typedtree.annotation.SQLReferencedByColumn;
import org.sodeac.common.typedtree.annotation.SQLTable;

@SQLTable(name = "EMPLOYEES")
public class EmployeeNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(EmployeeNodeType.class);
    }
    
    @SQLColumn(name = "EMPLOYEENUMBER")
    @SQLPrimaryKey
    public static volatile LeafNodeType<EmployeeNodeType, Integer> EMPLOYEENUMBER;
    
    @SQLColumn(name = "EXTENSION")
    public static volatile LeafNodeType<EmployeeNodeType, String> EMPLOYEEEXTENSION;
    
    @SQLColumn(name = "FIRSTNAME")
    public static volatile LeafNodeType<EmployeeNodeType, String> EMPLOYEEFIRSTNAME;
    
    @SQLColumn(name = "LASTNAME")
    public static volatile LeafNodeType<EmployeeNodeType, String> EMPLOYEELASTNAME;
    
    @SQLColumn(name = "JOBTITLE")
    public static volatile LeafNodeType<EmployeeNodeType, String> EMPLOYEEJOBTITLE;
    
    @SQLColumn(name = "EMAIL")
    public static volatile LeafNodeType<EmployeeNodeType, String> EMPLOYEEEMAIL;
    
    @SQLColumn(name = "REPORTSTO")
    public static volatile LeafNodeType<EmployeeNodeType, Integer> EMPLOYEEREPORTSTO;
    
    @SQLReferencedByColumn(name = "SALESREPEMPLOYEENUMBER")
    public static volatile BranchNodeListType<EmployeeNodeType, CustomerNodeType> CUSTOMERS;
}
