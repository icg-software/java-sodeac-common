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

import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.annotation.SQLColumn;
import org.sodeac.common.typedtree.annotation.SQLColumn.SQLColumnType;
import org.sodeac.common.typedtree.annotation.SQLPrimaryKey;
import org.sodeac.common.typedtree.annotation.SQLTable;

@SQLTable(name = "PRODUCTS")
public class ProductNodeType extends BranchNodeMetaModel
{
    static
    {
        ModelRegistry.getBranchNodeMetaModel(ProductNodeType.class);
    }
    
    @SQLColumn(name = "PRODUCTCODE")
    @SQLPrimaryKey
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTCODE;
    
    @SQLColumn(name = "PRODUCTNAME")
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTNAME;
    
    @SQLColumn(name = "PRODUCTLINE")
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTLINE;
    
    @SQLColumn(name = "PRODUCTSCALE")
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTSCALE;
    
    @SQLColumn(name = "PRODUCTVENDOR")
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTVENDOR;
    
    @SQLColumn(name = "PRODUCTDESCRIPTION", type = SQLColumnType.CLOB)
    public static volatile LeafNodeType<ProductNodeType, String> PRODUCTDESCRIPTION;
    
    @SQLColumn(name = "QUANTITYINSTOCK")
    public static volatile LeafNodeType<ProductNodeType, Integer> PRODUCTQUANTITYINSTOCK;
    
    @SQLColumn(name = "BUYPRICE")
    public static volatile LeafNodeType<ProductNodeType, Double> PRODUCTBUYPRICE;
    
    @SQLColumn(name = "MSRP")
    public static volatile LeafNodeType<ProductNodeType, Double> PRODUCTMSRP;
    
}
