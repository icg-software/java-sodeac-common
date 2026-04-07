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

@SQLTable(name = "OFFICE")
public class OfficeNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(OfficeNodeType.class);
    }
    
    @SQLColumn(name = "OFFICECODE")
    @SQLPrimaryKey
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICECODE;
    
    @SQLColumn(name = "ADDRESSLINE1")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICEADDRESSLINE1;
    
    @SQLColumn(name = "ADDRESSLINE2")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICEADDRESSLINE2;
    
    @SQLColumn(name = "CITY")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICECITY;
    
    @SQLColumn(name = "COUNTRY")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICECOUNTRY;
    
    @SQLColumn(name = "POSTALCODE")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICEPOSTALCODE;
    
    @SQLColumn(name = "STATE")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICESTATE;
    
    @SQLColumn(name = "TERRITORY")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICETERRITORY;
    
    @SQLColumn(name = "PHONE")
    public static volatile LeafNodeType<OfficeNodeType, String> OFFICEPHONE;
    
    @SQLReferencedByColumn(name = "OFFICECODE")
    public static volatile BranchNodeListType<OfficeNodeType, EmployeeNodeType> EMPLOYEES;
}
