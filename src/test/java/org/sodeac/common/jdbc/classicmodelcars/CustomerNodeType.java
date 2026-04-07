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

@SQLTable(name="CUSTOMERS")
public class CustomerNodeType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(CustomerNodeType.class);}	
	
	@SQLColumn(name="CUSTOMERNUMBER")
	@SQLPrimaryKey
	public static volatile LeafNodeType<CustomerNodeType,Integer> CUSTOMERNUMBER;

	@SQLColumn(name="CUSTOMERNAME")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERNAME;
	
	@SQLColumn(name="CONTACTLASTNAME")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERCONTACTLASTNAME;
	
	@SQLColumn(name="CREDITLIMIT")
	public static volatile LeafNodeType<CustomerNodeType,Integer> CUSTOMERCREDITLIMIT;
	
	@SQLColumn(name="CONTACTFIRSTNAME")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERCONTACTFIRSTNAME;
	
	@SQLColumn(name="PHONE")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERPHONE;
	
	@SQLColumn(name="ADDRESSLINE1")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERADDRESSLINE1;
	
	@SQLColumn(name="ADDRESSLINE2")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERADDRESSLINE2;
	
	@SQLColumn(name="CITY")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERCITY;
	
	@SQLColumn(name="STATE")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERSTATE;
	
	@SQLColumn(name="POSTALCODE")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERPOSTALCODE;
	
	@SQLColumn(name="COUNTRY")
	public static volatile LeafNodeType<CustomerNodeType,String> CUSTOMERCOUNTRY;
	
	@SQLReferencedByColumn(name="CUSTOMERNUMBER")
	public static volatile BranchNodeListType<CustomerNodeType,PaymentNodeType> PAYMENTS;
	
	@SQLReferencedByColumn(name="CUSTOMERNUMBER")
	public static volatile BranchNodeListType<CustomerNodeType,OrderNodeType> ORDERS;
}
