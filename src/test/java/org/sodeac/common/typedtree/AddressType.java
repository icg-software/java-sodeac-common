/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.typedtree;

import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.LeafNodeType;

public class AddressType extends BranchNodeMetaModel
{
	static{ModelRegistry.getBranchNodeMetaModel(AddressType.class);}
	
	public static volatile LeafNodeType<AddressType,String> street;
	public static volatile LeafNodeType<AddressType,String> number;
	public static volatile LeafNodeType<AddressType,String> city;
	public static volatile LeafNodeType<AddressType,Integer> zip;
	public static volatile BranchNodeType<AddressType,CountryType> country;
	public static volatile BranchNodeType<AddressType,UserType> parentuser;
}
