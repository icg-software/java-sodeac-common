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
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;

public class OfficeTreeModel extends TypedTreeMetaModel<OfficeTreeModel>
{
    static
    {
        ModelRegistry.getTypedTreeMetaModel(OfficeTreeModel.class);
    }
    
    public static volatile BranchNodeType<OfficeTreeModel, OfficeResultSetNodeType> resultSet;
    public static volatile BranchNodeType<OfficeTreeModel, PaymentNodeType> payment;
    public static volatile BranchNodeType<OfficeTreeModel, CustomerNodeType> customer;
    
    public static class OfficeResultSetNodeType extends BranchNodeMetaModel
    {
        static
        {
            ModelRegistry.getBranchNodeMetaModel(OfficeResultSetNodeType.class);
        }
        
        public static volatile BranchNodeListType<OfficeResultSetNodeType, OfficeNodeType> officeList;
    }
    
    public static class OrderDetailResultSetNodeType extends BranchNodeMetaModel
    {
        static
        {
            ModelRegistry.getBranchNodeMetaModel(OrderDetailResultSetNodeType.class);
        }
        
        public static volatile BranchNodeListType<OrderDetailResultSetNodeType, OrderDetailNodeType> orderDetailList;
    }
}
