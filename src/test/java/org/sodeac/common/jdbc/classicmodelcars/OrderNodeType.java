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

import java.util.Date;

import org.sodeac.common.typedtree.BranchNodeListType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.SQLPrimaryKey;
import org.sodeac.common.typedtree.annotation.SQLTable;

@SQLTable(name = "ORDERS")
public class OrderNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(OrderNodeType.class);
    }
    
    @SQLColumn(name = "ORDERNUMBER")
    @SQLPrimaryKey
    public static volatile LeafNodeType<OrderNodeType, Integer> ORDERNUMBER;
    
    @SQLColumn(name = "REQUIREDDATE", type = SQLColumnType.DATE)
    public static volatile LeafNodeType<OrderNodeType, Date> ORDERREQUIREDDATE;
    
    @SQLColumn(name = "ORDERDATE", type = SQLColumnType.DATE)
    public static volatile LeafNodeType<OrderNodeType, Date> ORDERORDERDATE;
    
    @SQLColumn(name = "SHIPPEDDATE", type = SQLColumnType.DATE)
    public static volatile LeafNodeType<OrderNodeType, Date> ORDERSHIPPEDDATE;
    
    @SQLColumn(name = "STATUS")
    public static volatile LeafNodeType<OrderNodeType, String> ORDERSTATUS;
    
    @SQLColumn(name = "COMMENTS", type = SQLColumnType.CLOB)
    public static volatile LeafNodeType<OrderNodeType, String> ORDERCOMMENTS;
    
    public static volatile BranchNodeListType<OrderNodeType, OrderDetailNodeType> ORDERDETAILS;
    
}
